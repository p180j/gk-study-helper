package com.gkstudy.content.service;

import com.gkstudy.content.dto.InventoryItem;
import com.gkstudy.content.dto.InventoryOverviewView;
import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import com.gkstudy.content.dto.SourceView;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContentAdminServiceTest {
    private ContentSourceMapper sourceMapper;
    private ContentStagingMapper stagingMapper;
    private ContentInventoryMapper inventoryMapper;
    private ContentCrawlService crawlService;
    private PoliticalTopicMapper topicMapper;
    private ReadingMaterialMapper materialMapper;
    private QuestionStagingImportService questionImporter;
    private ContentUploadService uploadService;
    private ContentCrawlLogMapper crawlLogMapper;
    private QuestionInventoryService questionInventoryService;
    private ContentAdminService service;

    @BeforeEach
    void setUp() {
        sourceMapper = mock(ContentSourceMapper.class);
        stagingMapper = mock(ContentStagingMapper.class);
        inventoryMapper = mock(ContentInventoryMapper.class);
        crawlService = mock(ContentCrawlService.class);
        topicMapper = mock(PoliticalTopicMapper.class);
        materialMapper = mock(ReadingMaterialMapper.class);
        questionImporter = mock(QuestionStagingImportService.class);
        uploadService = mock(ContentUploadService.class);
        crawlLogMapper = mock(ContentCrawlLogMapper.class);
        questionInventoryService = mock(QuestionInventoryService.class);
        service = new ContentAdminService(sourceMapper, stagingMapper, inventoryMapper, crawlService,
                topicMapper, materialMapper, questionImporter, uploadService, crawlLogMapper, questionInventoryService, 10);
    }

    @Test
    void createSourceValidatesInput() {
        assertEquals("SOURCE_NAME_EMPTY", assertThrows(com.gkstudy.common.BusinessException.class,
                () -> service.createSource("", "https://gov.cn", "GOVERNMENT", "GK", "A", true)).getCode());
        assertEquals("SOURCE_URL_INVALID", assertThrows(com.gkstudy.common.BusinessException.class,
                () -> service.createSource("来源", "ftp://gov.cn", "GOVERNMENT", "GK", "A", true)).getCode());
        assertEquals("SOURCE_TRUST_INVALID", assertThrows(com.gkstudy.common.BusinessException.class,
                () -> service.createSource("来源", "https://gov.cn", "GOVERNMENT", "GK", "X", true)).getCode());
    }

    @Test
    void createSourceSavesWithDefaults() {
        ContentSource saved = new ContentSource();
        saved.setId(7L); saved.setName("某省人社厅"); saved.setBaseUrl("https://gov.example.cn");
        saved.setSourceType("GOVERNMENT"); saved.setExamType("GK"); saved.setTrustLevel("A");
        saved.setEnabled(true); saved.setCrawlStrategy("LINK_SCAN"); saved.setStatus("IDLE");
        when(sourceMapper.findAll()).thenReturn(Collections.emptyList());
        when(sourceMapper.findById(7L)).thenReturn(saved);
        when(sourceMapper.insert(any(ContentSource.class))).thenAnswer(invocation -> {
            invocation.<ContentSource>getArgument(0).setId(7L);
            return 1;
        });

        SourceView view = service.createSource("某省人社厅", "https://gov.example.cn", null, null, "A", null);

        assertEquals("GOVERNMENT", view.getSourceType());
        assertEquals("政府网站", view.getSourceTypeText());
        assertEquals("A", view.getTrustLevel());
        assertTrue(view.isEnabled());
        assertEquals("A：官方附件 / 官方材料", view.getTrustText());
    }

    @Test
    void reviewRejectsNonReviewStatus() {
        ContentStaging imported = new ContentStaging();
        imported.setId(1L); imported.setStatus(ContentStaging.IMPORTED);
        when(stagingMapper.findById(1L)).thenReturn(imported);
        assertEquals("STAGING_STATUS_INVALID",
                assertThrows(com.gkstudy.common.BusinessException.class,
                        () -> service.review(1L, "DISCARD", null, "备注", null)).getCode());
    }

    @Test
    void reviewDiscardMarksFailedWithNote() {
        ContentStaging item = new ContentStaging();
        item.setId(1L); item.setStatus(ContentStaging.NEEDS_REVIEW);
        when(stagingMapper.findById(1L)).thenReturn(item);

        service.review(1L, "DISCARD", null, "人工判定不予入库", null);

        verify(stagingMapper).updateReviewNote(1L, "人工判定不予入库");
        verify(stagingMapper).markFailed(1L, "人工判定不予入库");
        verify(materialMapper, never()).insert(any());
    }

    @Test
    void reviewImportMaterialInsertsDraftAndMarksImported() {
        ContentStaging item = new ContentStaging();
        item.setId(1L); item.setStatus(ContentStaging.NEEDS_REVIEW);
        item.setSiteName("某省人社厅"); item.setTitle("基层治理公告");
        item.setParsedText("基层治理相关长文本内容");
        when(stagingMapper.findById(1L)).thenReturn(item);
        PoliticalTopic topic = new PoliticalTopic();
        topic.setId(3L); topic.setStatus("ACTIVE");
        when(topicMapper.findById(3L)).thenReturn(topic);
        when(materialMapper.insert(any(ReadingMaterial.class))).thenAnswer(invocation -> {
            invocation.<ReadingMaterial>getArgument(0).setId(88L);
            return 1;
        });

        service.review(1L, "IMPORT_MATERIAL", 3L, "人工确认", null);

        ArgumentCaptor<ReadingMaterial> captor = ArgumentCaptor.forClass(ReadingMaterial.class);
        verify(materialMapper).insert(captor.capture());
        assertEquals(3L, captor.getValue().getTopicId());
        assertEquals("DRAFT", captor.getValue().getStatus());
        verify(stagingMapper).markImported(1L, "READING_MATERIAL", 88L);
    }

    @Test
    void reviewImportRequiresActiveTopic() {
        ContentStaging item = new ContentStaging();
        item.setId(1L); item.setStatus(ContentStaging.NEEDS_REVIEW);
        item.setParsedText("有内容");
        when(stagingMapper.findById(1L)).thenReturn(item);
        when(topicMapper.findById(9L)).thenReturn(null);
        assertEquals("TOPIC_NOT_FOUND",
                assertThrows(com.gkstudy.common.BusinessException.class,
                        () -> service.review(1L, "IMPORT_MATERIAL", 9L, null, null)).getCode());
    }

    @Test
    void reviewConfirmDuplicateMarksFailed() {
        ContentStaging item = new ContentStaging();
        item.setId(1L); item.setStatus(ContentStaging.NEEDS_REVIEW);
        when(stagingMapper.findById(1L)).thenReturn(item);

        service.review(1L, "CONFIRM_DUPLICATE", null, null, null);

        verify(stagingMapper).markFailed(1L, "确认重复");
    }

    @Test
    void reviewImportQuestionInsertsAndMarksImported() {
        ContentStaging item = new ContentStaging();
        item.setId(1L); item.setStatus(ContentStaging.NEEDS_REVIEW);
        item.setTrustLevel("A");
        when(stagingMapper.findById(1L)).thenReturn(item);
        QuestionCandidate candidate = new QuestionCandidate();
        candidate.setStem("下列关于基层治理的说法正确的是哪一项内容呢");
        candidate.getOptions().add(new Option("A", "治理资源下沉"));
        candidate.getOptions().add(new Option("B", "仅靠上级统筹"));
        candidate.getOptions().add(new Option("C", "居民不参与"));
        candidate.getOptions().add(new Option("D", "无需制度建设"));
        candidate.setAnswer("A");
        candidate.setAnalysis("基层治理要求推动治理资源和服务下沉。");
        candidate.setKnowledgeCode("GK-XC-01");
        when(questionImporter.importConfirmed(eq(item), any(QuestionCandidate.class))).thenReturn(606L);

        service.review(1L, "IMPORT_QUESTION", null, "人工修正后入库", candidate);

        ArgumentCaptor<QuestionCandidate> captor = ArgumentCaptor.forClass(QuestionCandidate.class);
        verify(questionImporter).importConfirmed(eq(item), captor.capture());
        assertEquals("GK-XC-01", captor.getValue().getKnowledgeCode());
        verify(stagingMapper).markImported(1L, "QUESTION", 606L);
    }

    @Test
    void reviewImportQuestionRejectsInvalidKnowledgeCode() {
        ContentStaging item = new ContentStaging();
        item.setId(1L); item.setStatus(ContentStaging.NEEDS_REVIEW);
        when(stagingMapper.findById(1L)).thenReturn(item);
        QuestionCandidate candidate = new QuestionCandidate();
        candidate.setStem("下列关于基层治理的说法正确的是哪一项内容呢");
        candidate.getOptions().add(new Option("A", "选项一"));
        candidate.getOptions().add(new Option("B", "选项二"));
        candidate.setAnswer("A");
        candidate.setKnowledgeCode("NOT-EXIST");
        when(questionImporter.importConfirmed(eq(item), any(QuestionCandidate.class)))
                .thenThrow(new IllegalArgumentException("知识点不存在或未启用: NOT-EXIST"));

        assertThrows(IllegalArgumentException.class,
                () -> service.review(1L, "IMPORT_QUESTION", null, null, candidate));
        verify(stagingMapper, never()).markImported(any(), any(), any());
    }

    @Test
    void batchReviewProcessesOnlyNeedsReviewItems() {
        ContentStaging first = new ContentStaging();
        first.setId(1L); first.setStatus(ContentStaging.NEEDS_REVIEW);
        ContentStaging second = new ContentStaging();
        second.setId(2L); second.setStatus(ContentStaging.NEEDS_REVIEW);
        ContentStaging imported = new ContentStaging();
        imported.setId(3L); imported.setStatus(ContentStaging.IMPORTED);
        when(stagingMapper.findById(1L)).thenReturn(first);
        when(stagingMapper.findById(2L)).thenReturn(second);
        when(stagingMapper.findById(3L)).thenReturn(imported);

        int processed = service.batchReview(Arrays.asList(1L, 2L, 3L), "DISCARD");

        assertEquals(2, processed);
        verify(stagingMapper).markFailed(1L, "人工判定不予入库");
        verify(stagingMapper).markFailed(2L, "人工判定不予入库");
        verify(stagingMapper, never()).markFailed(eq(3L), any());
    }

    @Test
    void crawlDelegatesToTriggerCrawl() {
        ContentCrawlLog log = new ContentCrawlLog();
        log.setId(5L); log.setStatus("RUNNING");
        when(crawlService.triggerCrawl(5L)).thenReturn(log);

        ContentCrawlLog result = service.crawl(5L);

        assertSame(log, result);
        verify(crawlService).triggerCrawl(5L);
    }

    @Test
    void inventoryOverviewAggregatesModulesAndPoints() {
        InventoryOverviewView.Overview overview = new InventoryOverviewView.Overview();
        overview.setTrainableTotal(5);
        overview.setMockReservedTotal(10);
        overview.setNeedsReviewCount(3);
        when(inventoryMapper.overviewCounts()).thenReturn(overview);
        when(questionInventoryService.totalAvailable()).thenReturn(5);
        QuestionInventoryService.ModuleInventory available = new QuestionInventoryService.ModuleInventory();
        available.setModuleCode("XC"); available.setAvailableCount(15);
        when(questionInventoryService.moduleSummary(null)).thenReturn(Collections.singletonList(available));
        InventoryItem module = new InventoryItem();
        module.setId(1L); module.setCode("XC"); module.setName("言语理解");
        module.setTotalQuestions(20); module.setTrainable(15); module.setUnused(8);
        when(inventoryMapper.moduleStats()).thenReturn(Collections.singletonList(module));
        InventoryItem lowStockPoint = new InventoryItem();
        lowStockPoint.setId(11L); lowStockPoint.setCode("XC-01"); lowStockPoint.setName("逻辑填空");
        lowStockPoint.setParentId(1L); lowStockPoint.setTotalQuestions(6); lowStockPoint.setTrainable(5);
        lowStockPoint.setUnused(2);
        InventoryItem enoughPoint = new InventoryItem();
        enoughPoint.setId(12L); enoughPoint.setCode("XC-02"); enoughPoint.setName("片段阅读");
        enoughPoint.setParentId(1L); enoughPoint.setTotalQuestions(30); enoughPoint.setTrainable(25);
        enoughPoint.setUnused(50);
        when(inventoryMapper.pointStats()).thenReturn(Arrays.asList(lowStockPoint, enoughPoint));

        InventoryOverviewView view = service.inventoryOverview();

        assertEquals(5, view.getOverview().getTrainableTotal());
        assertEquals(10, view.getOverview().getMockReservedTotal());
        assertEquals(3, view.getOverview().getNeedsReviewCount());
        assertEquals(1, view.getModules().size());
        InventoryOverviewView.ModuleView moduleView = view.getModules().get(0);
        assertEquals("言语理解", moduleView.getModuleName());
        assertEquals(15, moduleView.getTrainable());
        assertEquals(2, moduleView.getKnowledgePoints().size());
        assertTrue(moduleView.getKnowledgePoints().get(0).isLowStock());
        assertFalse(moduleView.getKnowledgePoints().get(1).isLowStock());
    }

    @Test
    void uploadRejectsInvalidTrustLevel() {
        assertThrows(com.gkstudy.common.BusinessException.class, () -> service.upload(
                new MockMultipartFile("file", "t.csv", "text/csv", new byte[]{'a'}), "X", null));
    }

    @Test
    void uploadDelegatesToUploadService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "questions.csv", "text/csv", "stem".getBytes());
        ContentUploadService.UploadResult result = new ContentUploadService.UploadResult();
        result.imported = 1;
        when(uploadService.upload(file, "S", "官方题库")).thenReturn(result);

        ContentUploadService.UploadResult actual = service.upload(file, "S", "官方题库");

        assertSame(result, actual);
    }
}
