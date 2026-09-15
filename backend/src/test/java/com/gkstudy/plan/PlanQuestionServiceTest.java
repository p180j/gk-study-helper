package com.gkstudy.plan;

import com.gkstudy.common.BusinessException;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.service.PlanQuestionService;
import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanQuestionServiceTest {
    @Test
    void returnsHydratedQuestionsForOwnedPlanItem() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        DailyPlanItem item = new DailyPlanItem(); item.setKnowledgePointId(12L); item.setPurpose("VALIDATION");
        Question question = new Question(); question.setId(100L); question.setStem("题干");
        QuestionOption option = new QuestionOption(); option.setOptionKey("A"); option.setOptionText("选项");
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        when(questionMapper.findForPlanItem(12L, "VALIDATION", 10)).thenReturn(Collections.singletonList(question));
        when(questionMapper.findOptions(100L)).thenReturn(Collections.singletonList(option));
        when(questionMapper.findKnowledgePoints(100L)).thenReturn(Collections.emptyList());

        List<QuestionResponse> result = new PlanQuestionService(planMapper, questionMapper).questions(7L, 8L, 10);

        assertEquals(1, result.size()); assertEquals("题干", result.get(0).getStem());
        assertEquals("A", result.get(0).getOptions().get(0).getOptionKey());
    }

    @Test
    void rejectsPlanItemOwnedByAnotherUser() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new PlanQuestionService(planMapper, questionMapper).questions(7L, 8L, 10));

        assertEquals("PLAN_ITEM_NOT_FOUND", error.getCode()); verifyNoInteractions(questionMapper);
    }

    @Test
    void capsQuestionBatchAtTwenty() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        DailyPlanItem item = new DailyPlanItem(); item.setKnowledgePointId(12L); item.setPurpose("TRAINING");
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        when(questionMapper.findForPlanItem(12L, "TRAINING", 20)).thenReturn(Collections.emptyList());

        assertTrue(new PlanQuestionService(planMapper, questionMapper).questions(7L, 8L, 999).isEmpty());
    }
}
