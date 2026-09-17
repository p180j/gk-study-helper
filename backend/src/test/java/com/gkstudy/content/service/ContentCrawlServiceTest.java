package com.gkstudy.content.service;

import com.gkstudy.content.fetch.ContentFetcher;
import com.gkstudy.content.mapper.ContentSourceMapper;
import com.gkstudy.content.mapper.ContentStagingMapper;
import com.gkstudy.content.model.ContentSource;
import com.gkstudy.content.model.ContentStaging;
import com.gkstudy.content.service.ContentCrawlService.CrawlSummary;
import com.gkstudy.reading.mapper.PoliticalTopicMapper;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.model.PoliticalTopic;
import com.gkstudy.reading.model.ReadingMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContentCrawlServiceTest {
    private ContentSourceMapper sourceMapper;
    private ContentStagingMapper stagingMapper;
    private PoliticalTopicMapper topicMapper;
    private ReadingMaterialMapper materialMapper;
    private ContentFetcher fetcher;
    private ContentCrawlService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        sourceMapper = mock(ContentSourceMapper.class);
        stagingMapper = mock(ContentStagingMapper.class);
        topicMapper = mock(PoliticalTopicMapper.class);
        materialMapper = mock(ReadingMaterialMapper.class);
        fetcher = mock(ContentFetcher.class);
        service = new ContentCrawlService(sourceMapper, stagingMapper, fetcher, topicMapper,
                materialMapper, tempDir.toString());
    }

    private ContentStaging staging(String url, String trustLevel) {
        ContentStaging item = new ContentStaging();
        item.setId(1L);
        item.setSourceId(10L);
        item.setSourceUrl(url);
        item.setSiteName("某省人社厅");
        item.setTitle("2025年度考试录用公务员公告");
        item.setExamType("GK");
        item.setSourceType("ANNOUNCEMENT");
        item.setTrustLevel(trustLevel);
        item.setStatus(ContentStaging.DISCOVERED);
        return item;
    }

    private String longMaterialText() {
        String paragraph = "基层治理是国家治理的基石，要推动治理资源和服务中心下沉，提升基层服务能力，"
                + "完善共建共治共享的社会治理制度，建设人人有责、人人尽责、人人享有的社会治理共同体。";
        return paragraph.repeat(10);
    }

    private String html(String body) {
        return "<html><head><title>2025年度考试录用公务员公告</title></head><body>" + body + "</body></html>";
    }

    private void mockFetch(String url, String body) throws Exception {
        when(fetcher.fetch(url)).thenReturn(new ContentFetcher.Fetched(
                body.getBytes(StandardCharsets.UTF_8), "text/html", "notice.html"));
    }

    @Test
    void fullPipelineImportsHtmlMaterialAsDraft() throws Exception {
        String url = "https://gov.example.cn/notice/2025-01.html";
        mockFetch(url, html("<p>2025年1月15日</p><p>" + longMaterialText() + "</p>"));
        PoliticalTopic topic = new PoliticalTopic();
        topic.setId(5L); topic.setName("基层治理"); topic.setStatus("ACTIVE");
        when(topicMapper.findAllActive()).thenReturn(Collections.singletonList(topic));
        when(stagingMapper.countSameFileHash(any(), any())).thenReturn(0);
        when(stagingMapper.countSameContentHash(any(), any())).thenReturn(0);
        when(materialMapper.insert(any(ReadingMaterial.class))).thenAnswer(invocation -> {
            ReadingMaterial material = invocation.getArgument(0);
            material.setId(99L);
            return 1;
        });

        CrawlSummary summary = new CrawlSummary();
        service.processItem(staging(url, "A"), summary);

        ArgumentCaptor<ReadingMaterial> captor = ArgumentCaptor.forClass(ReadingMaterial.class);
        verify(materialMapper).insert(captor.capture());
        assertEquals(5L, captor.getValue().getTopicId());
        assertEquals("DRAFT", captor.getValue().getStatus());
        assertTrue(captor.getValue().getContent().contains("基层治理"));
        verify(stagingMapper).markImported(1L, "READING_MATERIAL", 99L);
        assertEquals(1, summary.imported);
    }

    @Test
    void duplicateFileHashIsMarkedFailedNotImported() throws Exception {
        String url = "https://gov.example.cn/notice/dup.html";
        mockFetch(url, html("<p>" + longMaterialText() + "</p>"));
        when(stagingMapper.countSameFileHash(any(), any())).thenReturn(1);

        CrawlSummary summary = new CrawlSummary();
        service.processItem(staging(url, "A"), summary);

        verify(stagingMapper).markFailed(eq(1L), contains("重复"));
        verify(materialMapper, never()).insert(any());
        assertEquals(1, summary.failed);
    }

    @Test
    void lowTrustContentNeverAutoImports() throws Exception {
        String url = "https://gov.example.cn/notice/low-trust.html";
        mockFetch(url, html("<p>" + longMaterialText() + "</p>"));

        CrawlSummary summary = new CrawlSummary();
        service.processItem(staging(url, "C"), summary);

        verify(stagingMapper).markNeedsReview(eq(1L), contains("可信度"));
        verify(materialMapper, never()).insert(any());
        assertEquals(1, summary.needsReview);
    }

    @Test
    void attachmentWithoutStructuredTextNeedsReview() throws Exception {
        String url = "https://gov.example.cn/files/2025-syllabus.pdf";
        when(fetcher.fetch(url)).thenReturn(new ContentFetcher.Fetched(
                new byte[]{1, 2, 3, 4}, "application/pdf", "2025-syllabus.pdf"));

        CrawlSummary summary = new CrawlSummary();
        service.processItem(staging(url, "A"), summary);

        verify(stagingMapper).markNeedsReview(eq(1L), contains("附件"));
        verify(materialMapper, never()).insert(any());
    }

    @Test
    void questionLikeContentNeedsReview() throws Exception {
        String url = "https://gov.example.cn/sample.html";
        String body = "<p>单选题：下列关于基层治理的说法正确的是</p>"
                + "<p>A．选项一 B．选项二 C．选项三 D．选项四</p><p>" + longMaterialText() + "</p>";
        mockFetch(url, html(body));

        CrawlSummary summary = new CrawlSummary();
        service.processItem(staging(url, "A"), summary);

        verify(stagingMapper).markNeedsReview(eq(1L), contains("试题"));
        verify(materialMapper, never()).insert(any());
    }

    @Test
    void shortTextNeedsReview() throws Exception {
        String url = "https://gov.example.cn/short.html";
        mockFetch(url, html("<p>基层治理 短公告</p>"));

        CrawlSummary summary = new CrawlSummary();
        service.processItem(staging(url, "A"), summary);

        verify(stagingMapper).markNeedsReview(eq(1L), contains("过短"));
        verify(materialMapper, never()).insert(any());
    }

    @Test
    void noTopicMatchNeedsReview() throws Exception {
        String url = "https://gov.example.cn/notice/no-topic.html";
        String unrelated = "本公告发布关于专业技术资格考试的报名安排与考场规则说明。".repeat(20);
        mockFetch(url, html("<p>" + unrelated + "</p>"));
        when(stagingMapper.countSameFileHash(any(), any())).thenReturn(0);
        when(stagingMapper.countSameContentHash(any(), any())).thenReturn(0);
        when(topicMapper.findAllActive()).thenReturn(Collections.emptyList());

        CrawlSummary summary = new CrawlSummary();
        service.processItem(staging(url, "A"), summary);

        verify(stagingMapper).markNeedsReview(eq(1L), contains("专题"));
        verify(materialMapper, never()).insert(any());
    }

    @Test
    void singleItemFailureDoesNotAffectOthersInBatch() throws Exception {
        ContentSource source = new ContentSource();
        source.setId(10L); source.setName("某省人社厅"); source.setEnabled(true);
        source.setBaseUrl("https://gov.example.cn/list.html"); source.setTrustLevel("A");
        source.setExamType("GK");
        when(sourceMapper.findById(10L)).thenReturn(source);

        String listHtml = "<html><body>"
                + "<a href='https://gov.example.cn/a.html'>关于2025年度考试录用公务员公告</a>"
                + "<a href='https://gov.example.cn/b.html'>事业单位招聘公告</a>"
                + "<a href='https://gov.example.cn/c.html'>面试成绩公告</a>"
                + "</body></html>";
        when(fetcher.fetch("https://gov.example.cn/list.html")).thenReturn(new ContentFetcher.Fetched(
                listHtml.getBytes(StandardCharsets.UTF_8), "text/html", "list.html"));
        when(fetcher.fetch("https://gov.example.cn/a.html")).thenReturn(new ContentFetcher.Fetched(
                html("<p>" + longMaterialText() + "</p>").getBytes(StandardCharsets.UTF_8), "text/html", "a.html"));
        when(fetcher.fetch("https://gov.example.cn/b.html")).thenThrow(new RuntimeException("连接超时"));
        when(fetcher.fetch("https://gov.example.cn/c.html")).thenReturn(new ContentFetcher.Fetched(
                html("<p>" + longMaterialText() + "</p>").getBytes(StandardCharsets.UTF_8), "text/html", "c.html"));

        ContentStaging a = staging("https://gov.example.cn/a.html", "A"); a.setId(1L);
        ContentStaging b = staging("https://gov.example.cn/b.html", "A"); b.setId(2L);
        ContentStaging c = staging("https://gov.example.cn/c.html", "A"); c.setId(3L);
        when(stagingMapper.findProcessable(10L)).thenReturn(Arrays.asList(a, b, c));
        when(stagingMapper.countByUrl(any())).thenReturn(0);
        when(stagingMapper.countSameFileHash(any(), any())).thenReturn(0);
        when(stagingMapper.countSameContentHash(any(), any())).thenReturn(0);
        when(topicMapper.findAllActive()).thenReturn(Collections.emptyList());

        CrawlSummary summary = service.crawlSource(10L);

        assertEquals(3, summary.discovered);
        assertEquals(3, summary.processed);
        verify(stagingMapper).markFailed(eq(2L), contains("连接超时"));
        verify(stagingMapper, times(2)).markDownloaded(any(ContentStaging.class));
        verify(stagingMapper, never()).markImported(any(), any(), any());
        verify(sourceMapper).updateCrawlState(10L, "IDLE");
    }

    @Test
    void discoverSkipsAlreadyKnownUrls() throws Exception {
        ContentSource source = new ContentSource();
        source.setId(10L); source.setName("某省人社厅");
        source.setBaseUrl("https://gov.example.cn/list.html");
        source.setTrustLevel("A"); source.setExamType("GK");
        String listHtml = "<html><body>"
                + "<a href='/notice1.html'>公务员考试公告</a>"
                + "<a href='/notice1.html'>公务员考试公告（重复链接）</a>"
                + "<a href='/notice2.pdf'>考试大纲附件</a>"
                + "<a href='#'>锚点</a>"
                + "</body></html>";
        when(fetcher.fetch("https://gov.example.cn/list.html")).thenReturn(new ContentFetcher.Fetched(
                listHtml.getBytes(StandardCharsets.UTF_8), "text/html", "list.html"));
        when(stagingMapper.countByUrl(any())).thenReturn(1);

        int created = service.discover(source);

        assertEquals(0, created);
        verify(stagingMapper, never()).insert(any(ContentStaging.class));
    }

    @Test
    void normalizeAndExtractHelpers() {
        assertEquals("abc def", ContentCrawlService.normalize("  abc\n\tdef  "));
        LocalDateTime time = ContentCrawlService.extractPublishTime("发布于2025年1月15日");
        assertNotNull(time);
        assertEquals(2025, time.getYear());
        assertEquals(1, time.getMonthValue());
        assertEquals(15, time.getDayOfMonth());
        assertEquals(Integer.valueOf(2025), ContentCrawlService.extractYear("2025年度考试公告"));
        assertNull(ContentCrawlService.extractYear("无年份文本"));
        assertNull(ContentCrawlService.extractPublishTime("没有日期"));
    }
}
