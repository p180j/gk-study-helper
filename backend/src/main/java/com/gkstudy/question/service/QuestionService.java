package com.gkstudy.question.service;

import com.gkstudy.common.BusinessException;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.Question;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {
    private final QuestionMapper questionMapper;

    public QuestionService(QuestionMapper questionMapper) { this.questionMapper = questionMapper; }

    public List<Question> list(String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return questionMapper.findAll(status, (safePage - 1) * safeSize, safeSize);
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
        return question;
    }
}
