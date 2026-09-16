package com.gkstudy.plan;

import com.gkstudy.common.BusinessException;
import com.gkstudy.essay.dto.EssayQuestionView;
import com.gkstudy.essay.mapper.EssayQuestionMapper;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.service.PlanQuestionService;
import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;
import com.gkstudy.reading.dto.MaterialDetailView;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.model.ReadingMaterial;
import com.gkstudy.reading.service.ReadingService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

        List<QuestionResponse> result = service(planMapper, questionMapper).questions(7L, 8L, 10);

        assertEquals(1, result.size()); assertEquals("题干", result.get(0).getStem());
        assertEquals("A", result.get(0).getOptions().get(0).getOptionKey());
    }

    @Test
    void rejectsPlanItemOwnedByAnotherUser() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service(planMapper, questionMapper).questions(7L, 8L, 10));

        assertEquals("PLAN_ITEM_NOT_FOUND", error.getCode()); verifyNoInteractions(questionMapper);
    }

    @Test
    void capsQuestionBatchAtTwenty() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        DailyPlanItem item = new DailyPlanItem(); item.setKnowledgePointId(12L); item.setPurpose("TRAINING");
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        when(questionMapper.findForPlanItem(12L, "TRAINING", 20)).thenReturn(Collections.emptyList());

        assertTrue(service(planMapper, questionMapper).questions(7L, 8L, 999).isEmpty());
    }

    @Test
    void essayQuestionReturnsFirstUnansweredForOwnedEssayItem() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        EssayQuestionMapper essayMapper = mock(EssayQuestionMapper.class);
        ReadingMaterialMapper readingMapper = mock(ReadingMaterialMapper.class); ReadingService readingService = mock(ReadingService.class);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("ESSAY"); item.setKnowledgePointId(60L);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        when(essayMapper.findFirstUnansweredByTopicKpId(7L, 60L, 1)).thenReturn(Collections.singletonList(essayQuestion()));

        EssayQuestionView view = new PlanQuestionService(planMapper, questionMapper, essayMapper, readingMapper, readingService)
                .essayQuestion(7L, 8L);

        assertEquals(100L, view.getId()); assertEquals("基层治理", view.getTopicName());
        assertEquals("归纳概括", view.getQuestionTypeName()); assertEquals("给定材料", view.getMaterial());
        verify(essayMapper, never()).findActiveByTopicKpId(anyLong(), anyInt());
    }

    @Test
    void essayQuestionFallsBackToActiveWhenAllAnswered() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        EssayQuestionMapper essayMapper = mock(EssayQuestionMapper.class);
        ReadingMaterialMapper readingMapper = mock(ReadingMaterialMapper.class); ReadingService readingService = mock(ReadingService.class);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("ESSAY"); item.setKnowledgePointId(60L);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        when(essayMapper.findFirstUnansweredByTopicKpId(7L, 60L, 1)).thenReturn(Collections.emptyList());
        when(essayMapper.findActiveByTopicKpId(60L, 1)).thenReturn(Collections.singletonList(essayQuestion()));

        EssayQuestionView view = new PlanQuestionService(planMapper, questionMapper, essayMapper, readingMapper, readingService)
                .essayQuestion(7L, 8L);

        assertEquals(100L, view.getId());
    }

    @Test
    void essayQuestionRejectsWhenTopicHasNoQuestions() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        EssayQuestionMapper essayMapper = mock(EssayQuestionMapper.class);
        ReadingMaterialMapper readingMapper = mock(ReadingMaterialMapper.class); ReadingService readingService = mock(ReadingService.class);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("ESSAY"); item.setKnowledgePointId(60L);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        when(essayMapper.findFirstUnansweredByTopicKpId(7L, 60L, 1)).thenReturn(Collections.emptyList());
        when(essayMapper.findActiveByTopicKpId(60L, 1)).thenReturn(Collections.emptyList());

        BusinessException error = assertThrows(BusinessException.class,
                () -> new PlanQuestionService(planMapper, questionMapper, essayMapper, readingMapper, readingService).essayQuestion(7L, 8L));

        assertEquals("BUSINESS_ERROR", error.getCode()); assertEquals("该主题暂无可用申论题", error.getMessage());
    }

    @Test
    void essayQuestionRejectsNonEssayPlanItem() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        EssayQuestionMapper essayMapper = mock(EssayQuestionMapper.class);
        ReadingMaterialMapper readingMapper = mock(ReadingMaterialMapper.class); ReadingService readingService = mock(ReadingService.class);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("QUESTION_SET"); item.setKnowledgePointId(12L);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new PlanQuestionService(planMapper, questionMapper, essayMapper, readingMapper, readingService).essayQuestion(7L, 8L));

        assertEquals("PLAN_ITEM_TYPE_MISMATCH", error.getCode()); verifyNoInteractions(essayMapper);
    }

    @Test
    void readingMaterialReturnsFirstUnreadForOwnedReadingItem() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        EssayQuestionMapper essayMapper = mock(EssayQuestionMapper.class);
        ReadingMaterialMapper readingMapper = mock(ReadingMaterialMapper.class); ReadingService readingService = mock(ReadingService.class);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("READING"); item.setKnowledgePointId(60L);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        ReadingMaterial material = new ReadingMaterial(); material.setId(200L);
        when(readingMapper.findFirstUnreadByTopicKpId(7L, 60L, "PUBLISHED", 1)).thenReturn(Collections.singletonList(material));
        MaterialDetailView detail = new MaterialDetailView(); detail.setId(200L); detail.setTitle("材料标题");
        when(readingService.materialDetail(7L, 200L)).thenReturn(detail);

        MaterialDetailView result = new PlanQuestionService(planMapper, questionMapper, essayMapper, readingMapper, readingService)
                .readingMaterial(7L, 8L);

        assertEquals(200L, result.getId()); assertEquals("材料标题", result.getTitle());
        verify(readingMapper, never()).findByTopicKpIdAndStatus(anyLong(), anyString(), anyInt());
    }

    @Test
    void readingMaterialFallsBackToPublishedWhenAllRead() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        EssayQuestionMapper essayMapper = mock(EssayQuestionMapper.class);
        ReadingMaterialMapper readingMapper = mock(ReadingMaterialMapper.class); ReadingService readingService = mock(ReadingService.class);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("READING"); item.setKnowledgePointId(60L);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        when(readingMapper.findFirstUnreadByTopicKpId(7L, 60L, "PUBLISHED", 1)).thenReturn(Collections.emptyList());
        ReadingMaterial material = new ReadingMaterial(); material.setId(201L);
        when(readingMapper.findByTopicKpIdAndStatus(60L, "PUBLISHED", 1)).thenReturn(Collections.singletonList(material));
        MaterialDetailView detail = new MaterialDetailView(); detail.setId(201L);
        when(readingService.materialDetail(7L, 201L)).thenReturn(detail);

        MaterialDetailView result = new PlanQuestionService(planMapper, questionMapper, essayMapper, readingMapper, readingService)
                .readingMaterial(7L, 8L);

        assertEquals(201L, result.getId());
    }

    @Test
    void readingMaterialRejectsWhenTopicHasNoMaterials() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        EssayQuestionMapper essayMapper = mock(EssayQuestionMapper.class);
        ReadingMaterialMapper readingMapper = mock(ReadingMaterialMapper.class); ReadingService readingService = mock(ReadingService.class);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("READING"); item.setKnowledgePointId(60L);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);
        when(readingMapper.findFirstUnreadByTopicKpId(7L, 60L, "PUBLISHED", 1)).thenReturn(Collections.emptyList());
        when(readingMapper.findByTopicKpIdAndStatus(60L, "PUBLISHED", 1)).thenReturn(Collections.emptyList());

        BusinessException error = assertThrows(BusinessException.class,
                () -> new PlanQuestionService(planMapper, questionMapper, essayMapper, readingMapper, readingService).readingMaterial(7L, 8L));

        assertEquals("BUSINESS_ERROR", error.getCode()); assertEquals("该主题暂无可用阅读材料", error.getMessage());
        verifyNoInteractions(readingService);
    }

    @Test
    void readingMaterialRejectsNonReadingPlanItem() {
        DailyPlanMapper planMapper = mock(DailyPlanMapper.class); QuestionMapper questionMapper = mock(QuestionMapper.class);
        EssayQuestionMapper essayMapper = mock(EssayQuestionMapper.class);
        ReadingMaterialMapper readingMapper = mock(ReadingMaterialMapper.class); ReadingService readingService = mock(ReadingService.class);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("ESSAY"); item.setKnowledgePointId(60L);
        when(planMapper.findItemForUser(8L, 7L)).thenReturn(item);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new PlanQuestionService(planMapper, questionMapper, essayMapper, readingMapper, readingService).readingMaterial(7L, 8L));

        assertEquals("PLAN_ITEM_TYPE_MISMATCH", error.getCode()); verifyNoInteractions(readingMapper, readingService);
    }

    private PlanQuestionService service(DailyPlanMapper planMapper, QuestionMapper questionMapper) {
        return new PlanQuestionService(planMapper, questionMapper, mock(EssayQuestionMapper.class),
                mock(ReadingMaterialMapper.class), mock(ReadingService.class));
    }

    private EssayQuestion essayQuestion() {
        EssayQuestion question = new EssayQuestion(); question.setId(100L); question.setTopicCode("GRASSROOTS_GOVERNANCE");
        question.setTopicName("基层治理"); question.setQuestionType("SUMMARY"); question.setMaterial("给定材料");
        question.setPrompt("请概括材料要点"); question.setWordLimitMin(100); question.setWordLimitMax(200);
        question.setStandardTimeSeconds(900);
        return question;
    }
}
