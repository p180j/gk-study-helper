package com.gkstudy.reading.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.reading.dto.MaterialDetailView;
import com.gkstudy.reading.dto.MaterialListItem;
import com.gkstudy.reading.dto.SaveReadingRecordRequest;
import com.gkstudy.reading.dto.TopicView;
import com.gkstudy.reading.service.ReadingService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reading")
public class ReadingController {
    private final ReadingService readingService;

    public ReadingController(ReadingService readingService) { this.readingService = readingService; }

    @GetMapping("/topics")
    public ApiResponse<List<TopicView>> topics(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(readingService.topicsWithProgress(userId));
    }

    @GetMapping("/materials")
    public ApiResponse<List<MaterialListItem>> materials(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                          @RequestParam(required = false) Long topicId) {
        return ApiResponse.success(readingService.materialsForUser(userId, topicId));
    }

    @GetMapping("/materials/{id}")
    public ApiResponse<MaterialDetailView> materialDetail(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                           @PathVariable Long id) {
        return ApiResponse.success(readingService.materialDetail(userId, id));
    }

    @PostMapping("/records")
    public ApiResponse<MaterialDetailView> saveRecord(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                       @Valid @RequestBody SaveReadingRecordRequest request) {
        return ApiResponse.success(readingService.saveRecord(userId, request));
    }
}
