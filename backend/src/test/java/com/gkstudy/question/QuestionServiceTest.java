package com.gkstudy.question;

import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.service.QuestionService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuestionServiceTest {
    @Test
    void freePracticeReturnsAnswerSafeQuestionsWithoutPlanItem() throws Exception {
        QuestionMapper mapper = mock(QuestionMapper.class);
        KnowledgePointRef point = new KnowledgePointRef(); point.setId(5L); point.setCode("DATA_ANALYSIS");
        Question question = new Question(); question.setId(101L); question.setStem("资料题"); question.setAnswer("B");
        when(mapper.findKnowledgePointByCode("DATA_ANALYSIS")).thenReturn(point);
        when(mapper.findForPlanItemExpanded(5L, "TRAINING", 99L, 3)).thenReturn(Collections.singletonList(question));
        when(mapper.findOptions(101L)).thenReturn(Collections.emptyList());
        when(mapper.findKnowledgePoints(101L)).thenReturn(Collections.emptyList());

        List<QuestionResponse> questions = new QuestionService(mapper).freePracticeQuestions(99L, "DATA_ANALYSIS", 3);

        assertEquals(1, questions.size()); assertEquals(101L, questions.get(0).getId());
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(questions.get(0)).contains("answer"));
        verify(mapper).findForPlanItemExpanded(eq(5L), eq("TRAINING"), eq(99L), eq(3));
    }
}
