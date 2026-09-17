package com.gkstudy.content.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.content.dto.InventoryItem;
import com.gkstudy.content.dto.SourceView;
import com.gkstudy.content.service.ContentAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/sources/{id}/crawl")
    public ApiResponse<CrawlResultView> crawl(@PathVariable Long id) {
        return ApiResponse.success(new CrawlResultView(adminService.crawl(id)));
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

    @PostMapping("/staging/{id}/review")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long topicId = body.get("topicId") == null ? null : Long.valueOf(String.valueOf(body.get("topicId")));
        adminService.review(id, text(body.get("action")), topicId, text(body.get("note")));
        return ApiResponse.success(null);
    }

    @GetMapping("/inventory")
    public ApiResponse<List<InventoryItem>> inventory() {
        return ApiResponse.success(adminService.inventory());
    }

    /** 抓取结果视图 */
    public static class CrawlResultView {
        public final int discovered;
        public final int processed;
        public final int imported;
        public final int needsReview;
        public final int failed;
        public final List<String> errors;

        public CrawlResultView(com.gkstudy.content.service.ContentCrawlService.CrawlSummary summary) {
            this.discovered = summary.discovered; this.processed = summary.processed;
            this.imported = summary.imported; this.needsReview = summary.needsReview; this.failed = summary.failed;
            this.errors = summary.errors;
        }
    }

    private String text(Object value) { return value == null ? null : String.valueOf(value); }
    private Boolean bool(Object value) { return value == null ? null : Boolean.parseBoolean(String.valueOf(value)); }
    private String emptyToNull(String value) { return value == null || value.trim().isEmpty() ? null : value; }
}
