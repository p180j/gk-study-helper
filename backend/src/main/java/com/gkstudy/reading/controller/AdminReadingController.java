package com.gkstudy.reading.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.reading.dto.ChangeStatusRequest;
import com.gkstudy.reading.dto.CreateTopicRequest;
import com.gkstudy.reading.dto.SaveMaterialRequest;
import com.gkstudy.reading.model.PoliticalTopic;
import com.gkstudy.reading.model.ReadingMaterial;
import com.gkstudy.reading.service.AdminReadingService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminReadingController {
    private final AdminReadingService adminReadingService;

    public AdminReadingController(AdminReadingService adminReadingService) { this.adminReadingService = adminReadingService; }

    @GetMapping("/reading-topics")
    public ApiResponse<List<PoliticalTopic>> topics() {
        return ApiResponse.success(adminReadingService.topics());
    }

    @PostMapping("/reading-topics")
    public ApiResponse<PoliticalTopic> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return ApiResponse.success(adminReadingService.createTopic(request));
    }

    @GetMapping("/reading-materials")
    public ApiResponse<Map<String, Object>> materials(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long topicId,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        List<ReadingMaterial> list = adminReadingService.materials(keyword, topicId, status, page, pageSize);
        int total = adminReadingService.countMaterials(keyword, topicId, status);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("list", list);
        return ApiResponse.success(result);
    }

    @GetMapping("/reading-materials/{id}")
    public ApiResponse<ReadingMaterial> materialDetail(@PathVariable Long id) {
        return ApiResponse.success(adminReadingService.materialDetail(id));
    }

    @PostMapping("/reading-materials")
    public ApiResponse<ReadingMaterial> createMaterial(@Valid @RequestBody SaveMaterialRequest request) {
        return ApiResponse.success(adminReadingService.createMaterial(request));
    }

    @PutMapping("/reading-materials/{id}")
    public ApiResponse<ReadingMaterial> updateMaterial(@PathVariable Long id, @Valid @RequestBody SaveMaterialRequest request) {
        return ApiResponse.success(adminReadingService.updateMaterial(id, request));
    }

    @PostMapping("/reading-materials/{id}/status")
    public ApiResponse<ReadingMaterial> changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.success(adminReadingService.changeStatus(id, request.getStatus()));
    }

    @PostMapping("/reading-materials/{id}/ai-structure")
    public ApiResponse<ReadingMaterial> aiStructure(@PathVariable Long id) {
        return ApiResponse.success(adminReadingService.aiStructure(id));
    }
}
