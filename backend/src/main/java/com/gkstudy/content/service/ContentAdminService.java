package com.gkstudy.content.service;

import com.gkstudy.common.BusinessException;
import com.gkstudy.content.ContentLabels;
import com.gkstudy.content.dto.InventoryItem;
import com.gkstudy.content.dto.InventoryOverviewView;
import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.SourceView;
import com.gkstudy.content.dto.StagingView;
import com.gkstudy.content.mapper.ContentCrawlLogMapper;
import com.gkstudy.content.mapper.ContentInventoryMapper;
import com.gkstudy.content.mapper.ContentSourceMapper;
import com.gkstudy.content.mapper.ContentStagingMapper;
import com.gkstudy.content.model.ContentCrawlLog;
import com.gkstudy.content.model.ContentSource;
import com.gkstudy.content.model.ContentStaging;
import com.gkstudy.reading.mapper.PoliticalTopicMapper;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.model.PoliticalTopic;
import com.gkstudy.reading.model.ReadingMaterial;
import com.gkstudy.question.service.QuestionInventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContentAdminService {
    private static final int PAGE_SIZE = 20;
    private static final int CRAWL_LOG_LIMIT = 20;

    private final ContentSourceMapper sourceMapper;
    private final ContentStagingMapper stagingMapper;
    private final ContentInventoryMapper inventoryMapper;
    private final ContentCrawlService crawlService;
    private final PoliticalTopicMapper topicMapper;
    private final ReadingMaterialMapper materialMapper;
    private final QuestionStagingImportService questionImporter;
    private final ContentUploadService uploadService;
    private final ContentCrawlLogMapper crawlLogMapper;
    private final QuestionInventoryService questionInventoryService;
    private final int lowStockThreshold;

    public ContentAdminService(ContentSourceMapper sourceMapper, ContentStagingMapper stagingMapper,
                               ContentInventoryMapper inventoryMapper, ContentCrawlService crawlService,
                               PoliticalTopicMapper topicMapper, ReadingMaterialMapper materialMapper,
                               QuestionStagingImportService questionImporter, ContentUploadService uploadService,
                               ContentCrawlLogMapper crawlLogMapper, QuestionInventoryService questionInventoryService,
                               @Value("${question.low-stock-threshold:10}") int lowStockThreshold) {
        this.sourceMapper = sourceMapper; this.stagingMapper = stagingMapper; this.inventoryMapper = inventoryMapper;
        this.crawlService = crawlService; this.topicMapper = topicMapper; this.materialMapper = materialMapper;
        this.questionImporter = questionImporter; this.uploadService = uploadService;
        this.crawlLogMapper = crawlLogMapper; this.questionInventoryService = questionInventoryService;
        this.lowStockThreshold = lowStockThreshold;
    }

    public List<SourceView> listSources() {
        List<SourceView> views = new ArrayList<>();
        for (ContentSource source : sourceMapper.findAll()) views.add(SourceView.of(source));
        return views;
    }

    public SourceView createSource(String name, String baseUrl, String sourceType, String examType,
                                   String trustLevel, Boolean enabled) {
        validateSourceInput(name, baseUrl, trustLevel);
        if (sourceMapper.findAll().stream().anyMatch(s -> s.getBaseUrl().equals(baseUrl.trim()))) {
            throw new BusinessException("SOURCE_DUPLICATED", "该来源地址已存在");
        }
        ContentSource source = new ContentSource();
        source.setName(name.trim()); source.setBaseUrl(baseUrl.trim());
        source.setSourceType(defaultString(sourceType, "GOVERNMENT"));
        source.setExamType(defaultString(examType, "GK"));
        source.setTrustLevel(trustLevel);
        source.setEnabled(enabled == null || enabled);
        source.setCrawlStrategy("LINK_SCAN");
        sourceMapper.insert(source);
        return SourceView.of(sourceMapper.findById(source.getId()));
    }

    public SourceView updateSource(Long id, String name, String baseUrl, String sourceType, String examType,
                                   String trustLevel, Boolean enabled) {
        ContentSource source = requireSource(id);
        validateSourceInput(name, baseUrl, trustLevel);
        source.setName(name.trim()); source.setBaseUrl(baseUrl.trim());
        source.setSourceType(defaultString(sourceType, source.getSourceType()));
        source.setExamType(defaultString(examType, source.getExamType()));
        source.setTrustLevel(trustLevel);
        if (enabled != null) source.setEnabled(enabled);
        sourceMapper.update(source);
        return SourceView.of(sourceMapper.findById(id));
    }

    /** 异步触发采集：立即返回 RUNNING 日志，进度通过采集日志查询 */
    public ContentCrawlLog crawl(Long sourceId) {
        return crawlService.triggerCrawl(sourceId);
    }

    /** 采集日志列表（新 → 旧，默认最近 20 条） */
    public List<ContentCrawlLog> crawlLogs(Long sourceId) {
        return crawlLogMapper.findList(sourceId, CRAWL_LOG_LIMIT);
    }

    public Map<String, Object> listStaging(String status, Long sourceId, String keyword, int page) {
        int offset = Math.max(0, page - 1) * PAGE_SIZE;
        List<StagingView> views = new ArrayList<>();
        for (ContentStaging item : stagingMapper.list(status, sourceId, keyword, offset, PAGE_SIZE)) {
            views.add(StagingView.of(item));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("items", views);
        result.put("total", stagingMapper.count(status, sourceId, keyword));
        result.put("page", page);
        result.put("pageSize", PAGE_SIZE);
        return result;
    }

    public Map<String, Object> stagingDetail(Long id) {
        ContentStaging item = stagingMapper.findById(id);
        if (item == null) throw new BusinessException("STAGING_NOT_FOUND", "暂存记录不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("sourceUrl", item.getSourceUrl());
        result.put("title", item.getTitle());
        result.put("status", item.getStatus());
        result.put("statusText", ContentLabels.status(item.getStatus()));
        result.put("failReason", item.getFailReason());
        result.put("reviewNote", item.getReviewNote());
        result.put("importedType", item.getImportedType());
        result.put("importedId", item.getImportedId());
        result.put("parsedText", item.getParsedText() == null ? "" : item.getParsedText());
        result.put("fileHash", item.getFileHash());
        result.put("contentHash", item.getContentHash());
        result.put("mimeType", item.getMimeType());
        result.put("fileSize", item.getFileSize());
        result.put("trustLevel", item.getTrustLevel());
        result.put("sourceYear", item.getSourceYear());
        result.put("qualityScore", item.getQualityScore());
        result.put("qualityConfidence", item.getQualityConfidence());
        result.put("qualityIssues", item.getQualityIssues());
        return result;
    }

    public void retryStaging(Long id) {
        crawlService.retry(id);
    }

    /** 人工处理：题目修正入库 / 确认重复 / 材料入库 / 丢弃 */
    public void review(Long id, String action, Long topicId, String note, QuestionCandidate question) {
        ContentStaging item = stagingMapper.findById(id);
        if (item == null) throw new BusinessException("STAGING_NOT_FOUND", "暂存记录不存在");
        if (!ContentStaging.NEEDS_REVIEW.equals(item.getStatus())) {
            throw new BusinessException("STAGING_STATUS_INVALID", "只有需要人工检查的记录才能执行人工处理");
        }
        if ("DISCARD".equals(action)) {
            stagingMapper.updateReviewNote(id, note == null ? "" : note);
            stagingMapper.markFailed(id, "人工判定不予入库");
            return;
        }
        if ("CONFIRM_DUPLICATE".equals(action)) {
            stagingMapper.updateReviewNote(id, note == null ? "" : note);
            stagingMapper.markFailed(id, "确认重复");
            return;
        }
        if ("IMPORT_QUESTION".equals(action)) {
            if (question == null) throw new BusinessException("QUESTION_BODY_REQUIRED", "请提交修正后的题目内容");
            Long questionId = questionImporter.importConfirmed(item, question);
            stagingMapper.updateReviewNote(id, note == null ? "" : note);
            stagingMapper.markImported(id, "QUESTION", questionId);
            return;
        }
        if (!"IMPORT_MATERIAL".equals(action)) throw new BusinessException("ACTION_INVALID", "不支持的处理操作");
        PoliticalTopic topic = topicMapper.findById(topicId);
        if (topic == null || !"ACTIVE".equals(topic.getStatus())) {
            throw new BusinessException("TOPIC_NOT_FOUND", "政治专题不存在或未启用");
        }
        String text = item.getParsedText() == null ? "" : item.getParsedText();
        if (text.trim().isEmpty()) throw new BusinessException("CONTENT_EMPTY", "该记录没有可入库的正文内容");
        ReadingMaterial material = new ReadingMaterial();
        material.setTopicId(topic.getId());
        material.setTitle(item.getTitle());
        material.setSource(item.getSiteName());
        material.setPublishDate(item.getPublishTime() == null ? LocalDate.now() : item.getPublishTime().toLocalDate());
        material.setContent(text);
        material.setStandardExpressionsJson("[]");
        material.setCasesJson("[]");
        material.setApplicableEssayThemesJson("[]");
        material.setStatus("DRAFT");
        materialMapper.insert(material);
        stagingMapper.updateReviewNote(id, note == null ? "" : note);
        stagingMapper.markImported(id, "READING_MATERIAL", material.getId());
    }

    /** 批量人工处理：丢弃 / 确认重复，只处理 NEEDS_REVIEW 记录，返回实际处理数 */
    public int batchReview(List<Long> ids, String action) {
        if (ids == null || ids.isEmpty()) throw new BusinessException("IDS_EMPTY", "请选择要处理的记录");
        if (!"DISCARD".equals(action) && !"CONFIRM_DUPLICATE".equals(action)) {
            throw new BusinessException("ACTION_INVALID", "批量处理仅支持丢弃或确认重复");
        }
        int processed = 0;
        for (Long id : ids) {
            ContentStaging item = stagingMapper.findById(id);
            if (item == null || !ContentStaging.NEEDS_REVIEW.equals(item.getStatus())) continue;
            stagingMapper.markFailed(id, "DISCARD".equals(action) ? "人工判定不予入库" : "确认重复");
            processed++;
        }
        return processed;
    }

    /** 内容库存聚合：总览计数 + 一级模块（含子知识点）库存 */
    public InventoryOverviewView inventoryOverview() {
        InventoryOverviewView view = new InventoryOverviewView();
        InventoryOverviewView.Overview overview = inventoryMapper.overviewCounts();
        view.setOverview(overview == null ? new InventoryOverviewView.Overview() : overview);
        view.getOverview().setTrainableTotal(questionInventoryService.totalAvailable());
        List<InventoryItem> points = inventoryMapper.pointStats();
        Map<Long, List<InventoryOverviewView.PointView>> byParent = new HashMap<>();
        for (InventoryItem point : points) {
            if (point.getParentId() == null) continue;
            byParent.computeIfAbsent(point.getParentId(), key -> new ArrayList<>())
                    .add(new InventoryOverviewView.PointView(point, point.getUnused() < lowStockThreshold));
        }
        Map<String, Integer> availableByModule = new HashMap<>();
        for (QuestionInventoryService.ModuleInventory module : questionInventoryService.moduleSummary(null)) {
            availableByModule.put(module.getModuleCode(), module.getAvailableCount());
        }
        for (InventoryItem module : inventoryMapper.moduleStats()) {
            InventoryOverviewView.ModuleView moduleView = new InventoryOverviewView.ModuleView();
            moduleView.setId(module.getId());
            moduleView.setCode(module.getCode());
            moduleView.setModuleName(module.getName());
            moduleView.setTotalQuestions(module.getTotalQuestions());
            moduleView.setTrainable(availableByModule.getOrDefault(module.getCode(), 0));
            moduleView.setUnused(module.getUnused());
            moduleView.setKnowledgePoints(byParent.getOrDefault(module.getId(), new ArrayList<>()));
            view.getModules().add(moduleView);
        }
        return view;
    }

    public List<InventoryItem> inventory() {
        return inventoryMapper.inventory();
    }

    /** 题目文件批量上传：CSV / XLSX 逐行校验分流入库 */
    public ContentUploadService.UploadResult upload(MultipartFile file, String trustLevel, String sourceName) throws IOException {
        String level = trustLevel == null || trustLevel.trim().isEmpty() ? "B" : trustLevel.trim();
        if (!ContentLabels.validTrust(level)) throw new BusinessException("SOURCE_TRUST_INVALID", "可信度必须是 S/A/B/C/D");
        return uploadService.upload(file, level, sourceName);
    }

    private ContentSource requireSource(Long id) {
        ContentSource source = sourceMapper.findById(id);
        if (source == null) throw new BusinessException("SOURCE_NOT_FOUND", "内容来源不存在");
        return source;
    }

    private void validateSourceInput(String name, String baseUrl, String trustLevel) {
        if (name == null || name.trim().isEmpty()) throw new BusinessException("SOURCE_NAME_EMPTY", "来源名称不能为空");
        if (baseUrl == null || !baseUrl.trim().startsWith("http")) throw new BusinessException("SOURCE_URL_INVALID", "来源地址必须以 http 开头");
        if (!ContentLabels.validTrust(trustLevel)) throw new BusinessException("SOURCE_TRUST_INVALID", "可信度必须是 S/A/B/C/D");
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
