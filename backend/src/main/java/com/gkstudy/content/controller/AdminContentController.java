package com.gkstudy.content.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.content.dto.InventoryOverviewView;
import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import com.gkstudy.content.dto.SourceView;
import com.gkstudy.content.model.ContentCrawlLog;
import com.gkstudy.content.service.ContentAdminService;
import com.gkstudy.content.service.ContentUploadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {
    private final ContentAdminService adminService;

    public AdminContentController(ContentAdminService adminService) { this.adminService = adminService; }

    @GetMapping("/sources")
    public ApiResponse<List<SourceView>> listSources() {
        return ApiResponse.success(adminService.listSources());
    }

    @PostMapping("/sources")
    public ApiResponse<SourceView> createSource(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.createSource(
                text(body.get("name")), text(body.get("baseUrl")), text(body.get("sourceType")),
                text(body.get("examType")), text(body.get("trustLevel")), bool(body.get("enabled"))));
    }

    @PutMapping("/sources/{id}")
    public ApiResponse<SourceView> updateSource(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(adminService.updateSource(id,
                text(body.get("name")), text(body.get("baseUrl")), text(body.get("sourceType")),
                text(body.get("examType")), text(body.get("trustLevel")), bool(body.get("enabled"))));
    }

    /** 异步触发采集：立即返回日志 ID 与状态，进度通过 /crawl-logs 查询 */
    @PostMapping("/sources/{id}/crawl")
    public ApiResponse<CrawlTriggerView> crawl(@PathVariable Long id) {
        ContentCrawlLog log = adminService.crawl(id);
        return ApiResponse.success(new CrawlTriggerView(log.getId(), log.getStatus()));
    }

    /** 采集日志列表（原始枚举，中文映射由前端完成） */
    @GetMapping("/crawl-logs")
    public ApiResponse<List<ContentCrawlLog>> crawlLogs(@RequestParam(required = false) Long sourceId) {
        return ApiResponse.success(adminService.crawlLogs(sourceId));
    }

    /** 采集触发响应：{logId, status} */
    public static class CrawlTriggerView {
        public final Long logId;
        public final String status;

        public CrawlTriggerView(Long logId, String status) {
            this.logId = logId; this.status = status;
        }
    }

    @GetMapping("/staging")
    public ApiResponse<Map<String, Object>> listStaging(@RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Long sourceId,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") int page) {
        return ApiResponse.success(adminService.listStaging(emptyToNull(status), sourceId, emptyToNull(keyword), page));
    }

    @GetMapping("/staging/{id}")
    public ApiResponse<Map<String, Object>> stagingDetail(@PathVariable Long id) {
        return ApiResponse.success(adminService.stagingDetail(id));
    }

    @PostMapping("/staging/{id}/retry")
    public ApiResponse<Void> retry(@PathVariable Long id) {
        adminService.retryStaging(id);
        return ApiResponse.success(null);
    }

    /**
     * 人工处理。
     * IMPORT_QUESTION：平铺携带修正题目 {stem, optionA..optionD, answer, analysis, knowledgeCode, sourceType}
     * （也兼容嵌套 question 对象）；其余 action 用 {topicId, note}。
     */
    @PostMapping("/staging/{id}/review")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long topicId = body.get("topicId") == null ? null : Long.valueOf(String.valueOf(body.get("topicId")));
        adminService.review(id, text(body.get("action")), topicId, text(body.get("note")), question(body));
        return ApiResponse.success(null);
    }

    /** 批量人工处理：{ids: [1,2], action: 'DISCARD' | 'CONFIRM_DUPLICATE'}，返回处理数 */
    @PostMapping("/staging/batch-review")
    public ApiResponse<BatchReviewView> batchReview(@RequestBody Map<String, Object> body) {
        List<Long> ids = new ArrayList<>();
        Object raw = body.get("ids");
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) ids.add(Long.valueOf(String.valueOf(item)));
        }
        int processed = adminService.batchReview(ids, text(body.get("action")));
        return ApiResponse.success(new BatchReviewView(processed));
    }

    public static class BatchReviewView {
        public final int processed;

        public BatchReviewView(int processed) { this.processed = processed; }
    }

    /** 内容库存聚合：总览计数 + 模块（含子知识点）库存 */
    @GetMapping("/inventory")
    public ApiResponse<InventoryOverviewView> inventory() {
        return ApiResponse.success(adminService.inventoryOverview());
    }

    /** 题目文件批量上传：multipart file + 表单 trustLevel（S/A/B/C/D，默认 B）、sourceName（默认文件名） */
    @PostMapping("/upload")
    public ApiResponse<ContentUploadService.UploadResult> upload(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam(value = "trustLevel", required = false) String trustLevel,
                                                                 @RequestParam(value = "sourceName", required = false) String sourceName) {
        try {
            return ApiResponse.success(adminService.upload(file, trustLevel, sourceName));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("文件读取失败：" + e.getMessage());
        }
    }

    /** IMPORT_QUESTION 请求体 → QuestionCandidate（平铺或嵌套 question 均支持） */
    private QuestionCandidate question(Map<String, Object> body) {
        Map<String, Object> source = body;
        if (body.get("question") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nested = (Map<String, Object>) body.get("question");
            source = nested;
        }
        if (source.get("stem") == null && source.get("optionA") == null) return null;
        QuestionCandidate candidate = new QuestionCandidate();
        candidate.setStem(text(source.get("stem")));
        candidate.setAnswer(text(source.get("answer")));
        candidate.setAnalysis(text(source.get("analysis")));
        candidate.setKnowledgeCode(text(source.get("knowledgeCode")));
        candidate.setSourceType(text(source.get("sourceType")));
        String[] keys = {"A", "B", "C", "D"};
        for (String key : keys) {
            String optionText = text(source.get("option" + key));
            if (optionText == null || optionText.trim().isEmpty()) continue;
            candidate.getOptions().add(new Option(key, optionText.trim()));
        }
        return candidate;
    }

    private String text(Object value) { return value == null ? null : String.valueOf(value); }
    private Boolean bool(Object value) { return value == null ? null : Boolean.parseBoolean(String.valueOf(value)); }
    private String emptyToNull(String value) { return value == null || value.trim().isEmpty() ? null : value; }
}
