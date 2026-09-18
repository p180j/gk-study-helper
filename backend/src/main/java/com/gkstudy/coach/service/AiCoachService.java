package com.gkstudy.coach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.coach.dto.CoachInsight;
import com.gkstudy.coach.dto.CoachResponse;
import com.gkstudy.common.BusinessException;
import com.gkstudy.errordiagnosis.mapper.ErrorDiagnosisMapper;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.essay.mapper.EssayEvaluationMapper;
import com.gkstudy.essay.model.EssayEvaluation;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.service.DailyPlanService;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.question.mapper.KnowledgePointMapper;
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
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() { };
    private final AiProvider aiProvider;
    private final AbilityMapper abilityMapper;
    private final LearningProblemMapper problemMapper;
    private final ErrorDiagnosisMapper diagnosisMapper;
    private final EssayEvaluationMapper evaluationMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final DailyPlanService dailyPlanService;
    private final AnswerRecordMapper answerRecordMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;

    public AiCoachService(AiProvider aiProvider, AbilityMapper abilityMapper, LearningProblemMapper problemMapper,
                          ErrorDiagnosisMapper diagnosisMapper, EssayEvaluationMapper evaluationMapper,
                          ReadingRecordMapper readingRecordMapper, DailyPlanService dailyPlanService,
                          AnswerRecordMapper answerRecordMapper, KnowledgePointMapper knowledgePointMapper,
                          ObjectMapper objectMapper) {
        this.aiProvider = aiProvider; this.abilityMapper = abilityMapper; this.problemMapper = problemMapper;
        this.diagnosisMapper = diagnosisMapper; this.evaluationMapper = evaluationMapper;
        this.readingRecordMapper = readingRecordMapper; this.dailyPlanService = dailyPlanService;
        this.answerRecordMapper = answerRecordMapper; this.knowledgePointMapper = knowledgePointMapper;
        this.objectMapper = objectMapper;
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

    /**
     * 首页 AI 今日洞察：不调用 AI，只按“已确认错因根因 → 学习问题 → 大类能力 → 基线”的优先级
     * 从真实数据中选取最具体的一条问题，杜绝 AI 编造根因。
     */
    public CoachInsight insight(Long userId) {
        DailyPlan plan = dailyPlanService.today(userId);
        for (ErrorDiagnosis diagnosis : diagnosisMapper.findRecentByUser(userId, 10)) {
            if (!"CONFIRMED".equals(diagnosis.getStatus())) continue;
            String pointName = knowledgePointMapper.findNameById(diagnosis.getKnowledgePointId());
            String cause = diagnosis.getAiExplanation() == null || diagnosis.getAiExplanation().trim().isEmpty()
                    ? diagnosis.getSuspectedCause() : diagnosis.getAiExplanation();
            List<String> evidence = new ArrayList<>();
            evidence.add("根因已经你确认，同类错误累计出现 " + count(diagnosis.getOccurrenceCount()) + " 次");
            return new CoachInsight("ERROR_DIAGNOSIS", pointName, pointName + "：" + cause,
                    suggestion(plan, diagnosis.getKnowledgePointId(), "今天完成一组针对训练，验证该根因是否改善"), evidence);
        }
        for (LearningProblem problem : problemMapper.findByUserId(userId)) {
            if ("RESOLVED".equals(problem.getStatus())) continue;
            String text = problem.getRootCause() != null && !problem.getRootCause().trim().isEmpty()
                    ? problem.getKnowledgePointName() + "：" + problem.getRootCause() : problem.getTitle();
            if (text == null || text.trim().isEmpty()) text = problem.getKnowledgePointName() + " 需要加强";
            return new CoachInsight("LEARNING_PROBLEM", problem.getKnowledgePointName(), text,
                    suggestion(plan, problem.getKnowledgePointId(),
                            "今天优先完成「" + problem.getKnowledgePointName() + "」的针对训练"),
                    problemEvidence(problem));
        }
        AbilityProfile weakest = null;
        for (AbilityProfile profile : abilityMapper.findCompleteMap(userId)) {
            if ("UNASSESSED".equals(profile.getStatus()) || profile.getSampleCount() == null || profile.getSampleCount() <= 0) continue;
            if (profile.getMasteryScore() == null) continue;
            if (weakest == null || profile.getMasteryScore().compareTo(weakest.getMasteryScore()) < 0) weakest = profile;
        }
        if (weakest != null) {
            List<String> evidence = new ArrayList<>();
            evidence.add("掌握度 " + weakest.getMasteryScore() + " · 样本 " + weakest.getSampleCount() + " 题");
            return new CoachInsight("ABILITY", weakest.getKnowledgePointName(),
                    weakest.getKnowledgePointName() + "掌握不足",
                    suggestion(plan, weakest.getKnowledgePointId(),
                            "今天优先补强" + weakest.getKnowledgePointName() + "相关知识点"), evidence);
        }
        List<String> evidence = new ArrayList<>();
        evidence.add("尚无足够的真实作答数据");
        return new CoachInsight("BASELINE", null, "先完成摸底测评，建立能力基线",
                baselineSuggestion(plan), evidence);
    }

    private String suggestion(DailyPlan plan, Long knowledgePointId, String fallback) {
        if (plan != null && knowledgePointId != null && plan.getItems() != null) {
            for (DailyPlanItem item : plan.getItems()) {
                if (knowledgePointId.equals(item.getKnowledgePointId())
                        && item.getPlannedMinutes() != null && item.getPlannedMinutes() > 0) {
                    return "今天完成 " + item.getPlannedMinutes() + " 分钟针对训练";
                }
            }
        }
        return fallback;
    }

    private String baselineSuggestion(DailyPlan plan) {
        if (plan != null && plan.getItems() != null) {
            for (DailyPlanItem item : plan.getItems()) {
                if ("ASSESSMENT".equals(item.getPurpose()) && item.getPlannedMinutes() != null && item.getPlannedMinutes() > 0) {
                    return "今天完成 " + item.getPlannedMinutes() + " 分钟摸底测评";
                }
            }
        }
        return "完成今日摸底，用真实作答建立能力画像";
    }

    private List<String> problemEvidence(LearningProblem problem) {
        List<String> evidence = new ArrayList<>();
        try {
            Map<String, Object> value = objectMapper.readValue(problem.getEvidenceJson(), MAP_TYPE);
            if (value.get("recentCount") != null) {
                String type = problem.getProblemType();
                String detail = "SPEED".equals(type) ? "超时 " + value.get("slowCount") + " 题"
                        : "STABILITY".equals(type) ? "对错切换 " + value.get("transitionCount") + " 次"
                        : "错 " + value.get("incorrectCount") + " 题";
                evidence.add("最近 " + value.get("recentCount") + " 题" + detail);
            } else if (value.get("dimensionName") != null && value.get("score") != null) {
                evidence.add("「" + value.get("dimensionName") + "」最近得分 " + value.get("score"));
            } else if (value.get("topicName") != null && value.get("essayMastery") != null) {
                evidence.add("主题「" + value.get("topicName") + "」掌握度 " + value.get("essayMastery"));
            }
            if (value.get("mastery") != null) evidence.add("掌握度 " + value.get("mastery"));
        } catch (Exception ignore) { }
        if (evidence.isEmpty()) evidence.add("基于真实作答数据识别");
        return evidence;
    }

    private int count(Integer value) { return value == null ? 0 : value; }

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
                + "所有面向用户的内容必须使用简体中文，禁止输出英文枚举、代码、字段名、JSON或内部状态。"
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
