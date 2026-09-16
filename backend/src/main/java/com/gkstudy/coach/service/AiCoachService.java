package com.gkstudy.coach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.coach.dto.CoachResponse;
import com.gkstudy.common.BusinessException;
import com.gkstudy.errordiagnosis.mapper.ErrorDiagnosisMapper;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.essay.mapper.EssayEvaluationMapper;
import com.gkstudy.essay.model.EssayEvaluation;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.service.DailyPlanService;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.reading.mapper.ReadingRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiCoachService {
    private static final Logger log = LoggerFactory.getLogger(AiCoachService.class);
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<List<String>>() { };
    private final AiProvider aiProvider;
    private final AbilityMapper abilityMapper;
    private final LearningProblemMapper problemMapper;
    private final ErrorDiagnosisMapper diagnosisMapper;
    private final EssayEvaluationMapper evaluationMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final DailyPlanService dailyPlanService;
    private final AnswerRecordMapper answerRecordMapper;
    private final ObjectMapper objectMapper;

    public AiCoachService(AiProvider aiProvider, AbilityMapper abilityMapper, LearningProblemMapper problemMapper,
                          ErrorDiagnosisMapper diagnosisMapper, EssayEvaluationMapper evaluationMapper,
                          ReadingRecordMapper readingRecordMapper, DailyPlanService dailyPlanService,
                          AnswerRecordMapper answerRecordMapper, ObjectMapper objectMapper) {
        this.aiProvider = aiProvider; this.abilityMapper = abilityMapper; this.problemMapper = problemMapper;
        this.diagnosisMapper = diagnosisMapper; this.evaluationMapper = evaluationMapper;
        this.readingRecordMapper = readingRecordMapper; this.dailyPlanService = dailyPlanService;
        this.answerRecordMapper = answerRecordMapper; this.objectMapper = objectMapper;
    }

    public CoachResponse ask(Long userId, String question) {
        Map<String, Object> context = context(userId);
        String contextJson;
        try { contextJson = objectMapper.writeValueAsString(context); }
        catch (Exception e) { throw new IllegalStateException("学习上下文无法序列化", e); }
        try {
            AiResponse response = aiProvider.completeStructured(systemPrompt(), userPrompt(contextJson, question), 1400);
            JsonNode root = response.getContent();
            String currentStatus = text(root, "currentStatus");
            List<String> coreProblems = list(root, "coreProblems");
            String todayReason = text(root, "todayReason");
            String answer = text(root, "answer");
            List<String> evidence = list(root, "evidence");
            for (String item : evidence) {
                if (!normalize(contextJson).contains(normalize(item))) {
                    throw new AiProviderException("AI_RESULT_INVALID", "教练证据不在真实学习上下文中");
                }
            }
            return new CoachResponse(currentStatus, coreProblems, todayReason, answer, evidence,
                    response.getProvider(), response.getModel());
        } catch (AiProviderException e) {
            log.warn("AI coach result rejected: code={}, reason={}", e.getCode(), e.getMessage());
            throw new BusinessException("AI_COACH_UNAVAILABLE", "AI学习教练暂时不可用，可稍后重试");
        }
    }

    Map<String, Object> context(Long userId) {
        DailyPlan plan = dailyPlanService.today(userId);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("abilities", abilityMapper.findByUserId(userId));
        context.put("learningProblems", problemMapper.findByUserId(userId));
        context.put("errorDiagnoses", diagnosisContext(diagnosisMapper.findRecentByUser(userId, 10)));
        context.put("essayEvaluations", evaluationContext(evaluationMapper.findRecentSuccessfulByUser(userId, 5)));
        context.put("completedReadingCount", readingRecordMapper.countCompletedByUser(userId));
        context.put("todayPlan", plan); context.put("recentAnswers", answerRecordMapper.findByUserId(userId, 0, 20));
        return context;
    }

    private List<Map<String, Object>> diagnosisContext(List<ErrorDiagnosis> diagnoses) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ErrorDiagnosis diagnosis : diagnoses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("knowledgePointId", diagnosis.getKnowledgePointId()); item.put("suspectedCause", diagnosis.getSuspectedCause());
            item.put("aiExplanation", diagnosis.getAiExplanation()); item.put("confidence", diagnosis.getConfidence());
            item.put("status", diagnosis.getStatus()); item.put("occurrenceCount", diagnosis.getOccurrenceCount()); result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> evaluationContext(List<EssayEvaluation> evaluations) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (EssayEvaluation evaluation : evaluations) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("essayAnswerId", evaluation.getEssayAnswerId()); item.put("totalScore", evaluation.getTotalScore());
            item.put("dimensionScores", evaluation.getDimensionScoresJson()); item.put("missingPoints", evaluation.getMissingPointsJson());
            item.put("problems", evaluation.getProblemsJson()); item.put("confidence", evaluation.getConfidence()); result.add(item);
        }
        return result;
    }

    private String systemPrompt() {
        return "你是学习教练，只能解释给定真实学习上下文，不得创造不存在的问题，不得修改或重排优先级。"
                + "返回JSON字段currentStatus、coreProblems数组、todayReason、answer、evidence数组。"
                + "evidence每项只能是上下文中逐字复制的单个具体值或完整文本，不要添加字段名、解释、引号或单位。";
    }

    private String userPrompt(String context, String question) {
        return "真实上下文=" + context + "\n用户问题=" + (question == null || question.trim().isEmpty()
                ? "我现在怎么样？真正的问题在哪里？今天为什么这样学？" : question.trim());
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().trim().isEmpty()) throw new AiProviderException("AI_RESULT_INVALID", field + " 缺失");
        return node.asText().trim();
    }

    private List<String> list(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) throw new AiProviderException("AI_RESULT_INVALID", field + " 结构不合法");
        try { return objectMapper.convertValue(node, LIST_TYPE); }
        catch (Exception e) { throw new AiProviderException("AI_RESULT_INVALID", field + " 结构不合法", e); }
    }

    private String normalize(String value) { return value.replaceAll("[\\s\"{}\\[\\],:]", ""); }
}
