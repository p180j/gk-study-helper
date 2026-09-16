package com.gkstudy.errordiagnosis.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiRootCauseAnalyzer {
    public static final String PROMPT_VERSION = "ROOT_CAUSE_V1";
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<List<String>>() { };
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    public AiRootCauseAnalyzer(AiProvider aiProvider, ObjectMapper objectMapper) {
        this.aiProvider = aiProvider; this.objectMapper = objectMapper;
    }

    public Analysis analyze(Question question, List<QuestionOption> options, AnswerRecord current,
                            List<AnswerRecord> recent, ErrorDiagnosis existing) {
        AiResponse response = aiProvider.completeStructured(systemPrompt(), userPrompt(question, options, current, recent, existing), 1000);
        JsonNode root = response.getContent();
        String suspectedCause = requiredText(root, "suspectedCause");
        String explanation = requiredText(root, "explanation");
        List<String> evidence = convert(root.get("evidence"), LIST_TYPE, "evidence");
        double confidence = score(root, "confidence");
        String relatedAbility = requiredText(root, "relatedAbility");
        if (evidence.isEmpty()) throw new AiProviderException("AI_RESULT_INVALID", "错因 evidence 不能为空");
        return new Analysis(suspectedCause, explanation, evidence, confidence, relatedAbility, response);
    }

    private String systemPrompt() {
        return "你是公务员行测错因分析助手。根据题目、选项、解析、当前错误与最近同类作答归纳具体可验证根因。"
                + "只返回JSON：suspectedCause、explanation、evidence字符串数组、confidence(0-100)、relatedAbility。"
                + "不得确认学习问题，不得修改能力分；证据不足时confidence必须低于60。";
    }

    private String userPrompt(Question question, List<QuestionOption> options, AnswerRecord current,
                              List<AnswerRecord> recent, ErrorDiagnosis existing) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("stem", question == null ? null : question.getStem());
        input.put("analysis", question == null ? null : question.getAnalysis());
        input.put("options", options == null ? new ArrayList<>() : options);
        input.put("userAnswer", current.getUserAnswer()); input.put("correctAnswer", current.getCorrectAnswerSnapshot());
        input.put("errorType", current.getErrorType()); input.put("confidenceType", current.getConfidenceType());
        input.put("durationMs", current.getDurationMs()); input.put("knowledgeSnapshot", current.getKnowledgeSnapshot());
        List<Map<String, Object>> recentView = new ArrayList<>();
        for (AnswerRecord record : recent) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userAnswer", record.getUserAnswer()); item.put("correct", record.getCorrect());
            item.put("errorType", record.getErrorType()); item.put("confidenceType", record.getConfidenceType());
            item.put("durationMs", record.getDurationMs()); recentView.add(item);
        }
        input.put("recentSameKnowledgeRecords", recentView);
        input.put("ruleCause", existing == null ? null : existing.getSuspectedCause());
        try { return objectMapper.writeValueAsString(input); }
        catch (Exception e) { throw new AiProviderException("AI_PROMPT_INVALID", "错因分析输入无法序列化", e); }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().trim().isEmpty()) {
            throw new AiProviderException("AI_RESULT_INVALID", field + " 缺失");
        }
        return node.asText().trim();
    }

    private double score(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isNumber() || node.asDouble() < 0 || node.asDouble() > 100) {
            throw new AiProviderException("AI_RESULT_INVALID", field + " 超出0-100范围");
        }
        double value = node.asDouble();
        return value <= 1 ? value * 100 : value;
    }

    private <T> T convert(JsonNode node, TypeReference<T> type, String field) {
        if (node == null || node.isNull()) throw new AiProviderException("AI_RESULT_INVALID", field + " 缺失");
        try { return objectMapper.convertValue(node, type); }
        catch (Exception e) { throw new AiProviderException("AI_RESULT_INVALID", field + " 结构不合法", e); }
    }

    public static class Analysis {
        private final String suspectedCause;
        private final String explanation;
        private final List<String> evidence;
        private final double confidence;
        private final String relatedAbility;
        private final AiResponse response;

        public Analysis(String suspectedCause, String explanation, List<String> evidence, double confidence,
                        String relatedAbility, AiResponse response) {
            this.suspectedCause = suspectedCause; this.explanation = explanation; this.evidence = evidence;
            this.confidence = confidence; this.relatedAbility = relatedAbility; this.response = response;
        }
        public String getSuspectedCause() { return suspectedCause; }
        public String getExplanation() { return explanation; }
        public List<String> getEvidence() { return evidence; }
        public double getConfidence() { return confidence; }
        public String getRelatedAbility() { return relatedAbility; }
        public AiResponse getResponse() { return response; }
    }
}
