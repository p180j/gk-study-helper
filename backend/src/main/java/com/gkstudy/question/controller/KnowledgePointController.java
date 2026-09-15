package com.gkstudy.question.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.question.model.KnowledgePointView;
import com.gkstudy.question.service.KnowledgePointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-points")
public class KnowledgePointController {
    private final KnowledgePointService service;

    public KnowledgePointController(KnowledgePointService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<KnowledgePointView>> list() { return ApiResponse.success(service.list()); }
}
