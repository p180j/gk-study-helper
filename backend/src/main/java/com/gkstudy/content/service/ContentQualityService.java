package com.gkstudy.content.service;

import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 题目质量门禁：纯规则打分，集中且可测试，不用 AI。
 * 语义：score>=60 且无致命问题（题干 / 选项 / 答案 / 知识点）且 confidence>=70 视为通过。
 * trustLevel 由调用方传入（S/A +10、B 0、C/D -15 修正 confidence），知识点匹配结果也由调用方传入。
 */
@Service
public class ContentQualityService {
    private static final Pattern HTML_TAG = Pattern.compile("<[a-zA-Z/][^>]*>");
    private static final Pattern ALLOWED_CHAR = Pattern.compile("[\\u4e00-\\u9fa5a-zA-Z0-9"
            + "\\s，。、；：？！“”‘’（）《》〈〉【】〔〕—…·%~%+\\-*/=<>≠≤≥.,;:?!'\"()\\[\\]{}]");
    private static final double GARBAGE_RATIO_LIMIT = 0.2;

    public QualityResult evaluate(QuestionCandidate candidate, String trustLevel, boolean knowledgeMatched) {
        List<String> issues = new ArrayList<>();
        boolean fatal = false;
        int score = 100;
        int confidence = 85;

        String stem = trimToNull(candidate == null ? null : candidate.getStem());
        if (stem == null || stem.length() < 10) {
            issues.add("题干不完整");
            score -= 40;
            fatal = true;
        }

        int validOptions = 0;
        if (candidate != null && candidate.getOptions() != null) {
            for (Option option : candidate.getOptions()) {
                if (option != null && trimToNull(option.getText()) != null) validOptions++;
            }
        }
        if (validOptions >= 4) {
            // 选项完整
        } else if (validOptions >= 2) {
            issues.add("选项数量不足");
            score -= 20;
            fatal = true;
        } else {
            issues.add("选项数量不足");
            score -= 40;
            fatal = true;
        }

        String answer = candidate == null ? null : trimToNull(candidate.getAnswer());
        boolean answerValid = false;
        if (answer != null) {
            answerValid = candidate.getOptions() != null && candidate.getOptions().stream()
                    .anyMatch(option -> option != null && answer.equals(option.getKey())
                            && trimToNull(option.getText()) != null);
        }
        if (!answerValid) {
            issues.add("答案缺失或非法");
            score -= 40;
            fatal = true;
        }

        String analysis = candidate == null ? null : trimToNull(candidate.getAnalysis());
        boolean analysisPresent = analysis != null;
        boolean consistent = analysisPresent && answerValid && analysisConsistent(analysis, answer);
        if (!analysisPresent) {
            issues.add("缺少解析");
            score -= 10;
            confidence -= 5;
        } else if (answerValid && !consistent) {
            issues.add("答案与解析可能冲突");
            score -= 10;
            confidence -= 5;
        } else {
            confidence += 10;
        }

        boolean garbled = stem != null && isGarbled(stem);
        if (garbled) {
            issues.add("内容含乱码或HTML噪声");
            score -= 30;
            confidence -= 10;
        }

        if (!knowledgeMatched) {
            issues.add("知识点无法分类");
            score -= 25;
            fatal = true;
            confidence -= 10;
        } else {
            confidence += 5;
        }

        if ("S".equals(trustLevel) || "A".equals(trustLevel)) confidence += 10;
        else if ("C".equals(trustLevel) || "D".equals(trustLevel)) confidence -= 15;

        score = Math.max(0, Math.min(100, score));
        confidence = Math.max(0, Math.min(100, confidence));
        return new QualityResult(score, confidence, issues, fatal);
    }

    /** 解析包含正确答案 key 或解析>=20字视为通过（简化规则） */
    private boolean analysisConsistent(String analysis, String answer) {
        return analysis.length() >= 20 || (answer != null && analysis.contains(answer));
    }

    private boolean isGarbled(String stem) {
        if (stem.indexOf('\uFFFD') >= 0 || HTML_TAG.matcher(stem).find()) return true;
        if (stem.isEmpty()) return false;
        int garbage = 0;
        for (char current : stem.toCharArray()) {
            if (!ALLOWED_CHAR.matcher(String.valueOf(current)).matches()) garbage++;
        }
        return (double) garbage / stem.length() > GARBAGE_RATIO_LIMIT;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class QualityResult {
        public final int score;
        public final int confidence;
        public final List<String> issues;
        public final boolean fatal;

        public QualityResult(int score, int confidence, List<String> issues, boolean fatal) {
            this.score = score; this.confidence = confidence; this.issues = issues; this.fatal = fatal;
        }

        public boolean passes() { return score >= 60 && !fatal && confidence >= 70; }

        /** 中文分号串，直接存库 quality_issues */
        public String issuesText() { return String.join("；", issues); }
    }
}
