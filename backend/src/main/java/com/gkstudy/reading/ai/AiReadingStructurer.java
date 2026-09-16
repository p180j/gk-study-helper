package com.gkstudy.reading.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.reading.model.ReadingMaterial;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiReadingStructurer {
    public static final String PROMPT_VERSION = "READING_STRUCTURE_V1";
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<List<String>>() { };
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    public AiReadingStructurer(AiProvider aiProvider, ObjectMapper objectMapper) {
        this.aiProvider = aiProvider; this.objectMapper = objectMapper;
    }

    public Result structure(ReadingMaterial material) {
        AiResponse response = aiProvider.completeStructured(systemPrompt(), userPrompt(material), 1800);
        JsonNode root = response.getContent();
        Result result = new Result();
        result.coreView = text(root, "coreView"); result.problem = text(root, "problem");
        result.cause = text(root, "cause"); result.solution = text(root, "solution");
        result.policyLogic = text(root, "policyLogic");
        result.standardExpressions = list(root, "standardExpressions"); result.cases = list(root, "cases");
        result.applicableEssayThemes = list(root, "applicableEssayThemes"); result.topicCandidates = list(root, "topicCandidates");
        JsonNode confidence = root.get("confidence");
        if (confidence == null || !confidence.isNumber() || confidence.asDouble() < 0 || confidence.asDouble() > 100) {
            throw new AiProviderException("AI_RESULT_INVALID", "政治材料 confidence 超出0-100范围");
        }
        result.confidence = confidence.asDouble() <= 1 ? confidence.asDouble() * 100 : confidence.asDouble();
        result.response = response; return result;
    }

    private String systemPrompt() {
        return "你是公务员申论政治材料整理助手，只处理用户给出的文章。返回JSON字段coreView、problem、cause、solution、"
                + "policyLogic、standardExpressions、cases、applicableEssayThemes、topicCandidates、confidence。"
                + "confidence必须是0到100之间的数字。所有结论必须可由原文支持，不得补写不存在的政策事实。";
    }

    private String userPrompt(ReadingMaterial material) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("title", material.getTitle()); input.put("source", material.getSource()); input.put("content", material.getContent());
        try { return objectMapper.writeValueAsString(input); }
        catch (Exception e) { throw new AiProviderException("AI_PROMPT_INVALID", "政治材料输入无法序列化", e); }
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().trim().isEmpty()) {
            throw new AiProviderException("AI_RESULT_INVALID", field + " 缺失");
        }
        return node.asText().trim();
    }

    private List<String> list(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) throw new AiProviderException("AI_RESULT_INVALID", field + " 结构不合法");
        try { return objectMapper.convertValue(node, LIST_TYPE); }
        catch (Exception e) { throw new AiProviderException("AI_RESULT_INVALID", field + " 结构不合法", e); }
    }

    public static class Result {
        private String coreView; private String problem; private String cause; private String solution; private String policyLogic;
        private List<String> standardExpressions; private List<String> cases; private List<String> applicableEssayThemes;
        private List<String> topicCandidates; private double confidence; private AiResponse response;
        public String getCoreView() { return coreView; }
        public String getProblem() { return problem; }
        public String getCause() { return cause; }
        public String getSolution() { return solution; }
        public String getPolicyLogic() { return policyLogic; }
        public List<String> getStandardExpressions() { return standardExpressions; }
        public List<String> getCases() { return cases; }
        public List<String> getApplicableEssayThemes() { return applicableEssayThemes; }
        public List<String> getTopicCandidates() { return topicCandidates; }
        public double getConfidence() { return confidence; }
        public AiResponse getResponse() { return response; }
    }
}
