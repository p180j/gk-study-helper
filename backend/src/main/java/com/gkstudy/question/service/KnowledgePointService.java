package com.gkstudy.question.service;

import com.gkstudy.question.mapper.KnowledgePointMapper;
import com.gkstudy.question.model.KnowledgePointView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgePointService {
    private final KnowledgePointMapper mapper;

    public KnowledgePointService(KnowledgePointMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public List<KnowledgePointView> list() { return mapper.findAll(); }
}
