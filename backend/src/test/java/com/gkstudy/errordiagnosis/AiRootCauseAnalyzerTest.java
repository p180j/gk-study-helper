package com.gkstudy.errordiagnosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.errordiagnosis.ai.AiRootCauseAnalyzer;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.question.model.Question;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AiRootCauseAnalyzerTest {
    @Test
    void returnsSpecificCandidateWithoutConfirmingProblem() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AiResponse response = new AiResponse("OPENAI_COMPATIBLE", "model-x", mapper.readTree(
                "{\"suspectedCause\":\"年份跨度少算1年\",\"explanation\":\"跨年度计算时把间隔数当成年份数\",\"evidence\":[\"连续两题选择少一年对应选项\"],\"confidence\":72,\"relatedAbility\":\"MASTERY\"}"),
                "raw", LocalDateTime.now(), 9);
        AiProvider provider = new AiProvider() {
            public AiResponse completeStructured(String system, String user, int max) { return response; }
            public boolean healthCheck() { return true; }
        };
        AiRootCauseAnalyzer analyzer = new AiRootCauseAnalyzer(provider, mapper);
        AnswerRecord record = new AnswerRecord(); record.setUserAnswer("B"); record.setCorrectAnswerSnapshot("C");
        record.setErrorType("CONDITION"); record.setConfidenceType("SURE"); record.setKnowledgeSnapshot("[]");
        ErrorDiagnosis rule = new ErrorDiagnosis(); rule.setSuspectedCause("条件理解偏差"); rule.setStatus("PENDING_CONFIRMATION");

        AiRootCauseAnalyzer.Analysis result = analyzer.analyze(new Question(), Collections.emptyList(), record,
                Collections.singletonList(record), rule);

        assertEquals("年份跨度少算1年", result.getSuspectedCause());
        assertEquals(72, result.getConfidence());
        assertEquals("PENDING_CONFIRMATION", rule.getStatus());
    }
}
