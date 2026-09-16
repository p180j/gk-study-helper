package com.gkstudy.essay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.dto.AbilityChange;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.common.BusinessException;
import com.gkstudy.essay.dto.DimensionScore;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.dto.EssayQuestionSummary;
import com.gkstudy.essay.dto.EssayQuestionView;
import com.gkstudy.essay.dto.EssaySubmitRequest;
import com.gkstudy.essay.dto.EssaySubmitResult;
import com.gkstudy.essay.dto.EssayTaskView;
import com.gkstudy.essay.dto.EvaluationView;
import com.gkstudy.essay.dto.PlanEssayItem;
import com.gkstudy.essay.engine.EssayConstants;
import com.gkstudy.essay.engine.EssayGrader;
import com.gkstudy.essay.mapper.EssayAnswerMapper;
import com.gkstudy.essay.mapper.EssayEvaluationMapper;
import com.gkstudy.essay.mapper.EssayQuestionMapper;
import com.gkstudy.essay.model.EssayAnswer;
import com.gkstudy.essay.model.EssayEvaluation;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.service.DailyPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EssayService {
    private static final int TASK_QUESTION_LIMIT = 50;
    private static final int PROMPT_PREVIEW_LENGTH = 60;

    private final EssayQuestionMapper questionMapper;
    private final EssayAnswerMapper answerMapper;
    private final EssayEvaluationMapper evaluationMapper;
    private final EssayGrader grader;
    private final EssayAbilityService abilityService;
    private final EssayProblemService problemService;
    private final DailyPlanService dailyPlanService;
    private final ObjectMapper objectMapper;

    public EssayService(EssayQuestionMapper questionMapper, EssayAnswerMapper answerMapper, EssayEvaluationMapper evaluationMapper,
                        EssayGrader grader, EssayAbilityService abilityService, EssayProblemService problemService,
                        DailyPlanService dailyPlanService, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper; this.answerMapper = answerMapper; this.evaluationMapper = evaluationMapper;
        this.grader = grader; this.abilityService = abilityService; this.problemService = problemService;
        this.dailyPlanService = dailyPlanService; this.objectMapper = objectMapper;
    }

    public EssaySubmitResult submit(Long userId, EssaySubmitRequest request) {
        EssayQuestion question = questionMapper.findActiveById(request.getEssayQuestionId());
        if (question == null) throw new BusinessException("ESSAY_QUESTION_NOT_FOUND", "申论题目不存在或未发布");
        LocalDateTime now = LocalDateTime.now();
        EssayAnswer answer = new EssayAnswer();
        answer.setUserId(userId); answer.setEssayQuestionId(question.getId());
        answer.setQuestionVersion(question.getVersion()); answer.setPracticeType(request.getPracticeType());
        answer.setAnswerText(request.getAnswerText()); answer.setDurationMs(request.getDurationMs());
        answer.setWordCount(countWords(request.getAnswerText())); answer.setSubmitTime(now);
        answerMapper.insert(answer);
        return gradeSavedAnswer(userId, answer, question, now);
    }

    public EssaySubmitResult retry(Long userId, Long answerId) {
        EssayAnswer answer = answerMapper.findById(answerId);
        if (answer == null || !userId.equals(answer.getUserId())) throw new BusinessException("ESSAY_ANSWER_NOT_FOUND", "申论作答不存在");
        EssayEvaluation latest = evaluationMapper.findByAnswerId(answerId);
        if (latest != null && "SUCCESS".equals(latest.getStatus())) {
            throw new BusinessException("ESSAY_ALREADY_GRADED", "该作答已成功评分，无需重试");
        }
        EssayQuestion question = questionMapper.findById(answer.getEssayQuestionId());
        if (question == null) throw new BusinessException("ESSAY_QUESTION_NOT_FOUND", "申论题目不存在");
        return gradeSavedAnswer(userId, answer, question, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public EssayQuestionView questionView(Long userId, Long essayQuestionId) {
        EssayQuestion question = questionMapper.findActiveById(essayQuestionId);
        if (question == null) throw new BusinessException("ESSAY_QUESTION_NOT_FOUND", "申论题目不存在或未发布");
        return new EssayQuestionView(question.getId(), question.getTopicCode(), question.getTopicName(), question.getQuestionType(),
                EssayConstants.typeName(question.getQuestionType()), question.getMaterial(), question.getPrompt(),
                question.getWordLimitMin(), question.getWordLimitMax(), question.getStandardTimeSeconds());
    }

    public EssayTaskView tasks(Long userId) {
        DailyPlan plan = dailyPlanService.today(userId);
        List<PlanEssayItem> planItems = new ArrayList<>();
        for (DailyPlanItem item : plan.getItems()) {
            if (!"ESSAY".equals(item.getItemType())) continue;
            planItems.add(new PlanEssayItem(item.getId(), item.getKnowledgePointId(), item.getKnowledgePointCode(),
                    item.getKnowledgePointName(), item.getPlannedMinutes(), item.getPurpose(), item.getReason(), item.getStatus()));
        }
        List<EssayQuestionSummary> questions = new ArrayList<>();
        for (EssayQuestion question : questionMapper.listActiveAll(TASK_QUESTION_LIMIT)) {
            questions.add(new EssayQuestionSummary(question.getId(), question.getTopicCode(), question.getTopicName(),
                    question.getQuestionType(), EssayConstants.typeName(question.getQuestionType()),
                    preview(question.getPrompt()), question.getWordLimitMin(), question.getWordLimitMax(),
                    question.getStandardTimeSeconds()));
        }
        return new EssayTaskView(planItems, questions);
    }

    private EssayEvaluation toEvaluation(Long answerId, EssayEvaluationResult result) {
        EssayEvaluation evaluation = new EssayEvaluation();
        evaluation.setEssayAnswerId(answerId); evaluation.setEvaluator(result.getEvaluator());
        evaluation.setProvider(result.getProvider()); evaluation.setModel(result.getModel());
        evaluation.setPromptVersion(result.getPromptVersion()); evaluation.setRequestTime(result.getRequestTime());
        evaluation.setLatencyMs(result.getLatencyMs()); evaluation.setStatus(result.getStatus());
        evaluation.setConfidence(decimal(result.getConfidence())); evaluation.setRawResponse(result.getRawResponse());
        evaluation.setTotalScore(BigDecimal.valueOf(result.getTotalScore()).setScale(2, RoundingMode.HALF_UP));
        evaluation.setDimensionScoresJson(toJson(result.getDimensionScores()));
        evaluation.setStrengthsJson(toJson(result.getStrengths()));
        evaluation.setProblemsJson(toJson(result.getProblems()));
        evaluation.setMissingPointsJson(toJson(result.getMissingPoints()));
        evaluation.setEvidenceJson(toJson(result.getEvidence()));
        evaluation.setSuggestionsJson(toJson(result.getSuggestions()));
        return evaluation;
    }

    private EssaySubmitResult gradeSavedAnswer(Long userId, EssayAnswer answer, EssayQuestion question, LocalDateTime now) {
        try {
            EssayEvaluationResult result = grader.grade(question, answer.getAnswerText(), answer.getDurationMs());
            evaluationMapper.insert(toEvaluation(answer.getId(), result));
            List<AbilityChange> abilityChanges = abilityService.apply(userId, question, result);
            problemService.evaluate(userId, question, result, now);
            problemService.evaluateContentGap(userId, question.getTopicKnowledgePointId(), question.getTopicCode(), question.getTopicName(), now);
            return new EssaySubmitResult(answer.getId(), toEvaluationView(result), abilityChanges);
        } catch (AiProviderException e) {
            evaluationMapper.insert(failedEvaluation(answer.getId(), e));
            return new EssaySubmitResult(answer.getId(), "FAILED", true, "AI评分暂时不可用，可稍后重试");
        }
    }

    private EssayEvaluation failedEvaluation(Long answerId, AiProviderException error) {
        EssayEvaluation evaluation = new EssayEvaluation();
        evaluation.setEssayAnswerId(answerId); evaluation.setEvaluator(grader.evaluator());
        evaluation.setProvider(grader.provider()); evaluation.setModel(grader.model()); evaluation.setPromptVersion(grader.promptVersion());
        evaluation.setRequestTime(LocalDateTime.now()); evaluation.setStatus("FAILED"); evaluation.setFailureMessage(error.getMessage());
        return evaluation;
    }

    private EvaluationView toEvaluationView(EssayEvaluationResult result) {
        List<DimensionScore> dimensionScores = new ArrayList<>();
        for (Map.Entry<String, Double> entry : result.getDimensionScores().entrySet()) {
            dimensionScores.add(new DimensionScore(entry.getKey(), EssayConstants.dimensionName(entry.getKey()),
                    BigDecimal.valueOf(entry.getValue()).setScale(1, RoundingMode.HALF_UP)));
        }
        return new EvaluationView(result.getEvaluator(), BigDecimal.valueOf(result.getTotalScore()).setScale(1, RoundingMode.HALF_UP),
                dimensionScores, result.getStrengths(), result.getProblems(), result.getMissingPoints(), result.getSuggestions(),
                result.getEvidence(), result.getProvider(), result.getModel(), result.getPromptVersion(), result.getStatus(),
                decimal(result.getConfidence()));
    }

    private String preview(String prompt) {
        if (prompt == null) return null;
        return prompt.length() <= PROMPT_PREVIEW_LENGTH ? prompt : prompt.substring(0, PROMPT_PREVIEW_LENGTH);
    }

    private int countWords(String answerText) { return answerText == null ? 0 : answerText.replaceAll("\\s+", "").length(); }
    private BigDecimal decimal(Double value) { return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP); }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("申论评分 JSON 序列化失败", e); }
    }
}
