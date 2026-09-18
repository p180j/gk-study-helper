package com.gkstudy.content.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import com.gkstudy.content.mapper.ContentStagingMapper;
import com.gkstudy.content.model.ContentStaging;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Staging → Question 自动入库：按来源等级 + 质量门禁分流。
 * S/A 质量通过自动入库；B 需 confidence>=80；C/D 需 confidence>=85（source_level=C/D 会被验证取题排除）；
 * 不满足条件的候选逐条生成 NEEDS_REVIEW 暂存记录，供人工修正后入库，原始数据不丢失。
 */
@Service
public class QuestionStagingImportService {
    private static final Set<String> SOURCE_TYPES = new HashSet<>(Arrays.asList("HISTORICAL", "MANUAL", "IMPORTED", "AI_GENERATED", "AI_VARIANT"));
    private static final Set<String> USAGE_TYPES = new HashSet<>(Arrays.asList("TRAINING", "VALIDATION", "MOCK_RESERVED"));
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private final QuestionMapper questionMapper;
    private final ContentStagingMapper stagingMapper;
    private final ContentQualityService qualityService;
    private final TransactionTemplate transactionTemplate;

    public QuestionStagingImportService(QuestionMapper questionMapper, ContentStagingMapper stagingMapper,
                                        ContentQualityService qualityService, TransactionTemplate transactionTemplate) {
        this.questionMapper = questionMapper; this.stagingMapper = stagingMapper;
        this.qualityService = qualityService; this.transactionTemplate = transactionTemplate;
    }

    public static class ImportStats {
        public int total;
        public int imported;
        public int duplicates;
        public int needsReview;
        public int failed;
        /** B 级自动入库数量（抽样质检提示用，不阻塞） */
        public int sampled;
        public Long firstQuestionId;
        public final List<String> errors = new ArrayList<>();
    }

    /** 逐候选分流：通过 → 自动入库，content_hash 撞 → 重复跳过，否则生成 NEEDS_REVIEW 记录 */
    public ImportStats importCandidates(ContentStaging staging, List<QuestionCandidate> candidates) {
        ImportStats stats = new ImportStats();
        stats.total = candidates.size();
        int index = 0;
        for (QuestionCandidate candidate : candidates) {
            index++;
            try {
                normalize(candidate);
                routeOne(staging, candidate, index, stats);
            } catch (Exception e) {
                stats.failed++;
                stats.errors.add("第 " + index + " 题入库失败：" + rootMessage(e));
            }
        }
        return stats;
    }

    private void routeOne(ContentStaging staging, QuestionCandidate candidate, int index, ImportStats stats) {
        KnowledgePointRef knowledge = candidate.getKnowledgeCode() == null || candidate.getKnowledgeCode().trim().isEmpty()
                ? null : questionMapper.findKnowledgePointByCode(candidate.getKnowledgeCode().trim());
        ContentQualityService.QualityResult quality = qualityService.evaluate(candidate, staging.getTrustLevel(), knowledge != null);
        if (!shouldAutoImport(staging.getTrustLevel(), quality)) {
            recordNeedsReview(staging, candidate, index, quality);
            stats.needsReview++;
            return;
        }
        String contentHash = contentHash(candidate);
        if (questionMapper.countByContentHash(contentHash) > 0) {
            stats.duplicates++;
            return;
        }
        Question question = buildQuestion(staging, candidate, quality, knowledge, contentHash);
        transactionTemplate.executeWithoutResult(status -> {
            questionMapper.insertQuestion(question);
            int sortNo = 1;
            for (Option each : candidate.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(question.getId());
                option.setOptionKey(each.getKey());
                option.setOptionText(each.getText());
                option.setSortNo(sortNo++);
                questionMapper.insertOption(option);
            }
            questionMapper.insertKnowledge(question.getId(), knowledge.getId());
        });
        stats.imported++;
        if (stats.firstQuestionId == null) stats.firstQuestionId = question.getId();
        if ("B".equals(staging.getTrustLevel())) stats.sampled++;
    }

    /** 人工确认入库：校验同手工导入规则（选项 / 答案 / 知识点 / 重复），通过即入库，不走自动分流 */
    public Long importConfirmed(ContentStaging staging, QuestionCandidate candidate) {
        normalize(candidate);
        List<QuestionOption> options = new ArrayList<>();
        int sortNo = 1;
        for (Option each : candidate.getOptions()) {
            if (each.getText() == null || each.getText().trim().isEmpty()) continue;
            QuestionOption option = new QuestionOption();
            option.setOptionKey(each.getKey());
            option.setOptionText(each.getText().trim());
            option.setSortNo(sortNo++);
            options.add(option);
        }
        if (candidate.getStem() == null || candidate.getStem().length() < 10) throw new IllegalArgumentException("题干不完整（至少 10 字）");
        if (options.size() < 2) throw new IllegalArgumentException("至少需要两个选项");
        if (candidate.getAnswer() == null) throw new IllegalArgumentException("答案不能为空");
        if (options.stream().noneMatch(option -> option.getOptionKey().equals(candidate.getAnswer()))) throw new IllegalArgumentException("答案没有对应选项");
        String sourceType = candidate.getSourceType() == null || candidate.getSourceType().trim().isEmpty()
                ? "IMPORTED" : candidate.getSourceType().trim().toUpperCase();
        if (!SOURCE_TYPES.contains(sourceType)) throw new IllegalArgumentException("来源类型不支持: " + sourceType);
        KnowledgePointRef knowledge = candidate.getKnowledgeCode() == null ? null
                : questionMapper.findKnowledgePointByCode(candidate.getKnowledgeCode().trim());
        if (knowledge == null) throw new IllegalArgumentException("知识点不存在或未启用: " + candidate.getKnowledgeCode());
        ContentQualityService.QualityResult quality = qualityService.evaluate(candidate, staging.getTrustLevel(), true);
        String contentHash = contentHash(candidate);
        if (questionMapper.countByContentHash(contentHash) > 0) throw new IllegalArgumentException("题目重复");
        Question question = new Question();
        question.setQuestionType("SINGLE");
        question.setStem(candidate.getStem());
        question.setAnswer(candidate.getAnswer());
        question.setAnalysis(candidate.getAnalysis());
        question.setDifficultyExpected(BigDecimal.valueOf(50));
        question.setStandardTimeSeconds(60);
        question.setSourceType(sourceType);
        question.setSourceName(staging.getSiteName());
        question.setSourceLevel(staging.getTrustLevel());
        question.setUsageType("TRAINING");
        question.setStatus("ACTIVE");
        question.setVersion(1);
        question.setContentHash(contentHash);
        question.setQualityScore(BigDecimal.valueOf(quality.score));
        transactionTemplate.executeWithoutResult(status -> {
            questionMapper.insertQuestion(question);
            for (QuestionOption option : options) {
                option.setQuestionId(question.getId());
                questionMapper.insertOption(option);
            }
            questionMapper.insertKnowledge(question.getId(), knowledge.getId());
        });
        return question.getId();
    }

    /** S/A 通过即入库；B 需 confidence>=80；C/D 需 confidence>=85；null 保守不自动入库 */
    private boolean shouldAutoImport(String trustLevel, ContentQualityService.QualityResult quality) {
        if (!quality.passes()) return false;
        if (trustLevel == null) return false;
        if ("S".equals(trustLevel) || "A".equals(trustLevel)) return true;
        if ("B".equals(trustLevel)) return quality.confidence >= 80;
        if ("C".equals(trustLevel) || "D".equals(trustLevel)) return quality.confidence >= 85;
        return false;
    }

    private void recordNeedsReview(ContentStaging staging, QuestionCandidate candidate, int index,
                                   ContentQualityService.QualityResult quality) {
        ContentStaging review = new ContentStaging();
        review.setSourceId(staging.getSourceId());
        review.setSourceUrl(staging.getSourceUrl() + "#idx-" + index);
        review.setSiteName(staging.getSiteName());
        review.setTitle(staging.getTitle());
        review.setExamType(staging.getExamType());
        review.setSourceType(staging.getSourceType());
        review.setTrustLevel(staging.getTrustLevel());
        review.setParsedText(toJson(candidate));
        review.setStatus(ContentStaging.NEEDS_REVIEW);
        review.setFailReason("质量门禁未通过：" + quality.issuesText());
        review.setQualityScore(BigDecimal.valueOf(quality.score));
        review.setQualityConfidence(BigDecimal.valueOf(quality.confidence));
        review.setQualityIssues(quality.issuesText());
        if (stagingMapper.countByUrl(review.getSourceUrl()) == 0) stagingMapper.insert(review);
    }

    private Question buildQuestion(ContentStaging staging, QuestionCandidate candidate,
                                   ContentQualityService.QualityResult quality, KnowledgePointRef knowledge,
                                   String contentHash) {
        Question question = new Question();
        question.setQuestionType("SINGLE");
        question.setStem(candidate.getStem());
        question.setAnswer(candidate.getAnswer());
        question.setAnalysis(candidate.getAnalysis());
        question.setDifficultyExpected(parseDifficulty(candidate.getDifficultyExpected()));
        question.setStandardTimeSeconds(candidate.getStandardTimeSeconds() == null || candidate.getStandardTimeSeconds() <= 0
                ? 60 : candidate.getStandardTimeSeconds());
        String sourceType = candidate.getSourceType() == null || candidate.getSourceType().trim().isEmpty()
                ? "IMPORTED" : candidate.getSourceType().trim().toUpperCase();
        if (!SOURCE_TYPES.contains(sourceType)) throw new IllegalArgumentException("来源类型不支持: " + sourceType);
        question.setSourceType(sourceType);
        question.setSourceName(staging.getSiteName());
        question.setSourceLevel(staging.getTrustLevel());
        String usageType = candidate.getUsageType() == null || candidate.getUsageType().trim().isEmpty()
                ? "TRAINING" : candidate.getUsageType().trim().toUpperCase();
        if (!USAGE_TYPES.contains(usageType)) throw new IllegalArgumentException("用途不支持: " + usageType);
        question.setUsageType(usageType);
        question.setStatus("ACTIVE");
        question.setVersion(1);
        question.setContentHash(contentHash);
        question.setQualityScore(BigDecimal.valueOf(quality.score));
        return question;
    }

    private BigDecimal parseDifficulty(String value) {
        try {
            BigDecimal difficulty = value == null || value.trim().isEmpty()
                    ? BigDecimal.valueOf(50) : new BigDecimal(value.trim());
            if (difficulty.compareTo(BigDecimal.ZERO) < 0 || difficulty.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("难度必须在 0 到 100 之间");
            }
            return difficulty;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("难度格式错误");
        }
    }

    /** 与 QuestionImportService 完全一致：sha256(stem|A:text|B:text|...|answer) */
    static String contentHash(QuestionCandidate candidate) {
        StringBuilder text = new StringBuilder(candidate.getStem());
        text.append('|');
        for (Option option : candidate.getOptions()) text.append(option.getKey()).append(':').append(option.getText()).append('|');
        text.append(candidate.getAnswer());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private void normalize(QuestionCandidate candidate) {
        if (candidate.getStem() != null) candidate.setStem(candidate.getStem().trim());
        if (candidate.getAnswer() != null) candidate.setAnswer(candidate.getAnswer().trim().toUpperCase());
        if (candidate.getKnowledgeCode() != null) candidate.setKnowledgeCode(candidate.getKnowledgeCode().trim());
        if (candidate.getAnalysis() != null) candidate.setAnalysis(candidate.getAnalysis().trim());
    }

    private String toJson(QuestionCandidate candidate) {
        try {
            return JSON.writeValueAsString(candidate);
        } catch (Exception e) {
            return "{\"stem\":\"" + (candidate.getStem() == null ? "" : candidate.getStem()) + "\"}";
        }
    }

    private String rootMessage(Exception error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
