package com.gkstudy.question.service;

import com.gkstudy.common.BusinessException;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {
    private final QuestionMapper questionMapper;

    public QuestionService(QuestionMapper questionMapper) { this.questionMapper = questionMapper; }

    public List<Question> list(String status, String questionType, String usageType, String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return questionMapper.findAll(status, questionType, usageType, keyword, (safePage - 1) * safeSize, safeSize);
    }

    public Question detail(Long id) {
        Question question = questionMapper.findById(id);
        if (question == null) throw new BusinessException("QUESTION_NOT_FOUND", "题目不存在");
        question.setOptions(questionMapper.findOptions(id));
        question.setKnowledgePoints(questionMapper.findKnowledgePoints(id));
        return question;
    }

    public Question answerableDetail(Long id) {
        Question question = detail(id);
        if (!"ACTIVE".equals(question.getStatus())) throw new BusinessException("QUESTION_NOT_ACTIVE", "题目当前不可作答");
        if ("MOCK_RESERVED".equals(question.getUsageType())) throw new BusinessException("QUESTION_NOT_PRACTICABLE", "模考专用题目不能在普通练习中作答");
        return question;
    }

    /** 自由练习只返回可作答题面；取题顺序与计划练习一致，但不关联 DailyPlanItem。 */
    public List<QuestionResponse> freePracticeQuestions(Long userId, String knowledgePointCode, int limit) {
        KnowledgePointRef point = questionMapper.findKnowledgePointByCode(knowledgePointCode);
        if (point == null) throw new BusinessException("KNOWLEDGE_POINT_NOT_FOUND", "知识点不存在或未启用");
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Question> questions = questionMapper.findForPlanItemExpanded(point.getId(), "TRAINING", userId, safeLimit);
        for (Question question : questions) {
            question.setOptions(questionMapper.findOptions(question.getId()));
            question.setKnowledgePoints(questionMapper.findKnowledgePoints(question.getId()));
        }
        return QuestionResponse.from(questions);
    }
}
