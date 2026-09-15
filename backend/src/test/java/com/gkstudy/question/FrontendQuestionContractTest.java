package com.gkstudy.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.question.dto.AdminQuestionResponse;
import com.gkstudy.question.dto.ImportResult;
import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.question.model.Question;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrontendQuestionContractTest {
    @Test
    void learningQuestionDoesNotLeakAnswerBeforeSubmission() throws Exception {
        Question question = question();

        String json = new ObjectMapper().writeValueAsString(QuestionResponse.from(question));

        assertFalse(json.contains("correctAnswer")); assertFalse(json.contains("\"answer\""));
        assertFalse(json.contains("\"analysis\"")); assertFalse(json.contains("B"));
    }

    @Test
    void adminQuestionIncludesAnswerAndAnalysis() throws Exception {
        String json = new ObjectMapper().writeValueAsString(AdminQuestionResponse.from(question()));

        assertTrue(json.contains("\"answer\":\"B\"")); assertTrue(json.contains("详细解析"));
    }

    @Test
    void importResultReportsTotalRows() {
        ImportResult result = new ImportResult(); result.success(); result.failure(3, "知识点不存在");

        assertEquals(2, result.getTotalCount()); assertEquals(1, result.getSuccessCount()); assertEquals(1, result.getFailureCount());
    }

    private Question question() {
        Question question = new Question(); question.setId(1L); question.setStem("题干");
        question.setAnswer("B"); question.setAnalysis("详细解析"); return question;
    }
}
