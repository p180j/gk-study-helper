package com.gkstudy.plan.service;

import com.gkstudy.common.BusinessException;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.Question;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanQuestionService {
    private final DailyPlanMapper planMapper;
    private final QuestionMapper questionMapper;

    public PlanQuestionService(DailyPlanMapper planMapper, QuestionMapper questionMapper) {
        this.planMapper = planMapper; this.questionMapper = questionMapper;
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
}
