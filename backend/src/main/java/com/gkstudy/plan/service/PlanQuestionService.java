package com.gkstudy.plan.service;

import com.gkstudy.common.BusinessException;
import com.gkstudy.essay.dto.EssayQuestionView;
import com.gkstudy.essay.engine.EssayConstants;
import com.gkstudy.essay.mapper.EssayQuestionMapper;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.Question;
import com.gkstudy.reading.dto.MaterialDetailView;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.model.ReadingMaterial;
import com.gkstudy.reading.service.ReadingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanQuestionService {
    private final DailyPlanMapper planMapper;
    private final QuestionMapper questionMapper;
    private final EssayQuestionMapper essayQuestionMapper;
    private final ReadingMaterialMapper readingMaterialMapper;
    private final ReadingService readingService;

    public PlanQuestionService(DailyPlanMapper planMapper, QuestionMapper questionMapper, EssayQuestionMapper essayQuestionMapper,
                               ReadingMaterialMapper readingMaterialMapper, ReadingService readingService) {
        this.planMapper = planMapper; this.questionMapper = questionMapper;
        this.essayQuestionMapper = essayQuestionMapper; this.readingMaterialMapper = readingMaterialMapper;
        this.readingService = readingService;
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> questions(Long userId, Long itemId, int limit) {
        DailyPlanItem item = planMapper.findItemForUser(itemId, userId);
        if (item == null) throw new BusinessException("PLAN_ITEM_NOT_FOUND", "计划任务不存在");
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Question> questions = questionMapper.findForPlanItem(item.getKnowledgePointId(), item.getPurpose(), safeLimit);
        for (Question question : questions) {
            question.setOptions(questionMapper.findOptions(question.getId()));
            question.setKnowledgePoints(questionMapper.findKnowledgePoints(question.getId()));
        }
        return QuestionResponse.from(questions);
    }

    @Transactional(readOnly = true)
    public EssayQuestionView essayQuestion(Long userId, Long itemId) {
        DailyPlanItem item = ownedItem(userId, itemId, "ESSAY", "计划任务不是申论训练");
        List<EssayQuestion> questions = essayQuestionMapper.findFirstUnansweredByTopicKpId(userId, item.getKnowledgePointId(), 1);
        if (questions.isEmpty()) questions = essayQuestionMapper.findActiveByTopicKpId(item.getKnowledgePointId(), 1);
        if (questions.isEmpty()) throw new BusinessException("BUSINESS_ERROR", "该主题暂无可用申论题");
        EssayQuestion question = questions.get(0);
        return new EssayQuestionView(question.getId(), question.getTopicCode(), question.getTopicName(), question.getQuestionType(),
                EssayConstants.typeName(question.getQuestionType()), question.getMaterial(), question.getPrompt(),
                question.getWordLimitMin(), question.getWordLimitMax(), question.getStandardTimeSeconds());
    }

    @Transactional(readOnly = true)
    public MaterialDetailView readingMaterial(Long userId, Long itemId) {
        DailyPlanItem item = ownedItem(userId, itemId, "READING", "计划任务不是政治阅读");
        List<ReadingMaterial> materials = readingMaterialMapper.findFirstUnreadByTopicKpId(userId, item.getKnowledgePointId(), "PUBLISHED", 1);
        if (materials.isEmpty()) materials = readingMaterialMapper.findByTopicKpIdAndStatus(item.getKnowledgePointId(), "PUBLISHED", 1);
        if (materials.isEmpty()) throw new BusinessException("BUSINESS_ERROR", "该主题暂无可用阅读材料");
        return readingService.materialDetail(userId, materials.get(0).getId());
    }

    private DailyPlanItem ownedItem(Long userId, Long itemId, String expectedItemType, String typeMismatchMessage) {
        DailyPlanItem item = planMapper.findItemForUser(itemId, userId);
        if (item == null) throw new BusinessException("PLAN_ITEM_NOT_FOUND", "计划任务不存在");
        if (!expectedItemType.equals(item.getItemType())) throw new BusinessException("PLAN_ITEM_TYPE_MISMATCH", typeMismatchMessage);
        return item;
    }
}
