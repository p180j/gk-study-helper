package com.gkstudy.content.service;

import com.gkstudy.common.BusinessException;
import com.gkstudy.content.dto.SourceView;
import com.gkstudy.content.mapper.ContentInventoryMapper;
import com.gkstudy.content.mapper.ContentSourceMapper;
import com.gkstudy.content.mapper.ContentStagingMapper;
import com.gkstudy.content.model.ContentSource;
import com.gkstudy.content.model.ContentStaging;
import com.gkstudy.reading.mapper.PoliticalTopicMapper;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.model.PoliticalTopic;
import com.gkstudy.reading.model.ReadingMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    private ContentAdminService service;

    @BeforeEach
    void setUp() {
        sourceMapper = mock(ContentSourceMapper.class);
        stagingMapper = mock(ContentStagingMapper.class);
        inventoryMapper = mock(ContentInventoryMapper.class);
        crawlService = mock(ContentCrawlService.class);
        topicMapper = mock(PoliticalTopicMapper.class);
        materialMapper = mock(ReadingMaterialMapper.class);
        service = new ContentAdminService(sourceMapper, stagingMapper, inventoryMapper, crawlService,
                topicMapper, materialMapper);
    }

    @Test
    void createSourceValidatesInput() {
        assertEquals("SOURCE_NAME_EMPTY", assertThrows(BusinessException.class,
                () -> service.createSource("", "https://gov.cn", "GOVERNMENT", "GK", "A", true)).getCode());
        assertEquals("SOURCE_URL_INVALID", assertThrows(BusinessException.class,
                () -> service.createSource("来源", "ftp://gov.cn", "GOVERNMENT", "GK", "A", true)).getCode());
        assertEquals("SOURCE_TRUST_INVALID", assertThrows(BusinessException.class,
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
                assertThrows(BusinessException.class, () -> service.review(1L, "DISCARD", null, "备注")).getCode());
    }

    @Test
    void reviewDiscardMarksFailedWithNote() {
        ContentStaging item = new ContentStaging();
        item.setId(1L); item.setStatus(ContentStaging.NEEDS_REVIEW);
        when(stagingMapper.findById(1L)).thenReturn(item);

        service.review(1L, "DISCARD", null, "人工判定不予入库");

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

        service.review(1L, "IMPORT_MATERIAL", 3L, "人工确认");

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
                assertThrows(BusinessException.class, () -> service.review(1L, "IMPORT_MATERIAL", 9L, null)).getCode());
    }

    @Test
    void crawlDelegatesToCrawlService() {
        when(crawlService.crawlSource(5L)).thenReturn(new ContentCrawlService.CrawlSummary());
        service.crawl(5L);
        verify(crawlService).crawlSource(5L);
    }
}
