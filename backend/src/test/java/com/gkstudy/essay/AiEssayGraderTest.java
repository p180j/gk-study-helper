package com.gkstudy.essay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.engine.AiEssayGrader;
import com.gkstudy.essay.model.EssayQuestion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AiEssayGraderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void validatesAndReturnsAuditedStructuredResult() throws Exception {
        AiEssayGrader grader = grader("{\"totalScore\":82,\"dimensionScores\":{\"ESSAY_MATERIAL_READING\":80,\"ESSAY_INFO_EXTRACTION\":81,\"ESSAY_POINT_COMPLETENESS\":79,\"ESSAY_SUMMARY\":84,\"ESSAY_EXPRESSION\":82},\"strengths\":[\"概括准确\"],\"problems\":[],\"missingPoints\":[],\"evidence\":[\"治理资源下沉\"],\"suggestions\":[\"压缩表述\"],\"confidence\":86}");
        EssayEvaluationResult result = grader.grade(question(), "应推动治理资源下沉", 60000);
        assertEquals(82, result.getTotalScore());
        assertEquals("REAL_AI", result.getEvaluator());
        assertEquals("OPENAI_COMPATIBLE", result.getProvider());
        assertEquals("model-x", result.getModel());
        assertEquals(86, result.getConfidence());
    }

    @Test
    void rejectsOutOfRangeScore() throws Exception {
        AiEssayGrader grader = grader("{\"totalScore\":101,\"dimensionScores\":{\"ESSAY_SUMMARY\":80},\"strengths\":[],\"problems\":[],\"missingPoints\":[],\"evidence\":[\"治理资源下沉\"],\"suggestions\":[],\"confidence\":80}");
        assertThrows(AiProviderException.class, () -> grader.grade(question(), "治理资源下沉", 60000));
    }

    @Test
    void rejectsEvidenceNotGroundedInMaterialOrAnswer() throws Exception {
        AiEssayGrader grader = grader("{\"totalScore\":80,\"dimensionScores\":{\"ESSAY_SUMMARY\":80},\"strengths\":[],\"problems\":[],\"missingPoints\":[],\"evidence\":[\"不存在的证据\"],\"suggestions\":[],\"confidence\":80}");
        assertThrows(AiProviderException.class, () -> grader.grade(question(), "治理资源下沉", 60000));
    }

    private AiEssayGrader grader(String json) throws Exception {
        AiResponse response = new AiResponse("OPENAI_COMPATIBLE", "model-x", mapper.readTree(json), "raw",
                LocalDateTime.now(), 12);
        AiProvider provider = new AiProvider() {
            public AiResponse completeStructured(String system, String user, int max) { return response; }
            public boolean healthCheck() { return true; }
        };
        return new AiEssayGrader(provider, mapper);
    }

    private EssayQuestion question() {
        EssayQuestion question = new EssayQuestion(); question.setQuestionType("SUMMARY");
        question.setMaterial("材料提出要推动治理资源下沉，提升基层服务能力。"); question.setPrompt("概括主要做法");
        question.setReferencePointsJson("[]"); question.setTopicName("基层治理"); question.setTopicCode("THEME_GOVERNANCE");
        return question;
    }
}
