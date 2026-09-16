package com.gkstudy.reading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.common.BusinessException;
import com.gkstudy.reading.dto.MaterialDetailView;
import com.gkstudy.reading.dto.MaterialListItem;
import com.gkstudy.reading.dto.SaveReadingRecordRequest;
import com.gkstudy.reading.dto.TopicView;
import com.gkstudy.reading.mapper.PoliticalTopicMapper;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.mapper.ReadingRecordMapper;
import com.gkstudy.reading.model.PoliticalTopic;
import com.gkstudy.reading.model.ReadingMaterial;
import com.gkstudy.reading.model.ReadingRecord;
import com.gkstudy.reading.service.ReadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReadingServiceTest {
    private PoliticalTopicMapper topicMapper;
    private ReadingMaterialMapper materialMapper;
    private ReadingRecordMapper recordMapper;
    private ReadingService service;

    @BeforeEach
    void setUp() {
        topicMapper = mock(PoliticalTopicMapper.class);
        materialMapper = mock(ReadingMaterialMapper.class);
        recordMapper = mock(ReadingRecordMapper.class);
        service = new ReadingService(topicMapper, materialMapper, recordMapper, new ObjectMapper());
    }

    @Test
    void topicsWithProgressAggregatesPublishedAndCompletedCounts() {
        PoliticalTopic rural = topic(5L, "THEME_RURAL", "乡村振兴", 77L);
        PoliticalTopic green = topic(6L, "THEME_GREEN", "绿色发展", 78L);
        when(topicMapper.findAllActive()).thenReturn(Arrays.asList(rural, green));
        when(topicMapper.countMaterialsByTopicId(5L, "PUBLISHED")).thenReturn(3);
        when(topicMapper.countMaterialsByTopicId(6L, "PUBLISHED")).thenReturn(0);
        when(recordMapper.countCompletedByUserAndTopicKpId(7L, 77L)).thenReturn(2);
        when(recordMapper.countCompletedByUserAndTopicKpId(7L, 78L)).thenReturn(0);

        List<TopicView> views = service.topicsWithProgress(7L);

        assertEquals(2, views.size());
        TopicView view = views.get(0);
        assertEquals(5L, view.getId());
        assertEquals("THEME_RURAL", view.getCode());
        assertEquals("乡村振兴", view.getName());
        assertEquals(3, view.getTotalMaterials());
        assertEquals(2, view.getCompletedMaterials());
        assertEquals(0, views.get(1).getTotalMaterials());
        assertEquals(0, views.get(1).getCompletedMaterials());
    }

    @Test
    void firstSaveInsertsRecordWithDefaults() {
        AtomicReference<ReadingRecord> stored = new AtomicReference<>();
        when(materialMapper.findById(301L)).thenReturn(publishedMaterial());
        when(recordMapper.findForUpdate(7L, 301L)).thenReturn(null);
        when(recordMapper.insert(any())).thenAnswer(invocation -> {
            ReadingRecord record = invocation.getArgument(0); record.setId(90L); stored.set(record); return 1;
        });
        when(recordMapper.findByUserIdAndMaterialId(7L, 301L)).thenAnswer(invocation -> stored.get());

        SaveReadingRecordRequest request = new SaveReadingRecordRequest();
        request.setMaterialId(301L); request.setFavorite(true); request.setDurationMs(60000L); // readStatus/masteryLevel 为 null
        MaterialDetailView view = service.saveRecord(7L, request);

        ArgumentCaptor<ReadingRecord> captor = ArgumentCaptor.forClass(ReadingRecord.class);
        verify(recordMapper).insert(captor.capture());
        ReadingRecord record = captor.getValue();
        assertEquals(7L, record.getUserId());
        assertEquals(301L, record.getMaterialId());
        assertEquals("READING", record.getReadStatus());
        assertEquals(true, record.getFavorite());
        assertEquals("UNMASTERED", record.getMasteryLevel());
        assertEquals(60000L, record.getDurationMs());
        assertNotNull(record.getFirstReadTime());
        assertNotNull(record.getLastReadTime());
        verify(recordMapper, never()).update(any());
        assertEquals("READING", view.getReadStatus());
        assertEquals(true, view.getFavorite());
    }

    @Test
    void secondSaveUpdatesOnlyNonNullFields() {
        ReadingRecord existing = new ReadingRecord();
        existing.setId(90L); existing.setUserId(7L); existing.setMaterialId(301L);
        existing.setReadStatus("COMPLETED"); existing.setFavorite(false); existing.setMasteryLevel("UNMASTERED");
        existing.setFirstReadTime(LocalDateTime.of(2026, 1, 1, 9, 0));
        existing.setLastReadTime(LocalDateTime.of(2026, 1, 1, 9, 0));
        when(materialMapper.findById(301L)).thenReturn(publishedMaterial());
        when(recordMapper.findForUpdate(7L, 301L)).thenReturn(existing);
        when(recordMapper.findByUserIdAndMaterialId(7L, 301L)).thenReturn(existing);

        SaveReadingRecordRequest request = new SaveReadingRecordRequest();
        request.setMaterialId(301L); request.setFavorite(true); // 其余字段为 null
        MaterialDetailView view = service.saveRecord(7L, request);

        ArgumentCaptor<ReadingRecord> captor = ArgumentCaptor.forClass(ReadingRecord.class);
        verify(recordMapper).update(captor.capture());
        ReadingRecord updated = captor.getValue();
        assertEquals("COMPLETED", updated.getReadStatus()); // readStatus 不被 null 覆盖
        assertEquals(true, updated.getFavorite());
        assertEquals("UNMASTERED", updated.getMasteryLevel());
        assertTrue(updated.getLastReadTime().isAfter(updated.getFirstReadTime()));
        verify(recordMapper, never()).insert(any());
        assertEquals("COMPLETED", view.getReadStatus());
        assertEquals(true, view.getFavorite());
    }

    @Test
    void countCompletedDelegatesWithCorrectParameters() {
        when(recordMapper.countCompletedByUserAndTopicKpId(7L, 77L)).thenReturn(4);
        assertEquals(4, service.countCompleted(7L, 77L));
        verify(recordMapper).countCompletedByUserAndTopicKpId(7L, 77L);
    }

    @Test
    void materialDetailRejectsMissingOrUnpublished() {
        when(materialMapper.findById(404L)).thenReturn(null);
        BusinessException missing = assertThrows(BusinessException.class, () -> service.materialDetail(7L, 404L));
        assertEquals("MATERIAL_NOT_FOUND", missing.getCode());

        ReadingMaterial draft = publishedMaterial();
        draft.setStatus("DRAFT");
        when(materialMapper.findById(301L)).thenReturn(draft);
        BusinessException unpublished = assertThrows(BusinessException.class, () -> service.materialDetail(7L, 301L));
        assertEquals("MATERIAL_NOT_PUBLISHED", unpublished.getCode());
    }

    @Test
    void materialDetailParsesJsonFieldsIntoLists() {
        ReadingMaterial material = publishedMaterial();
        material.setStandardExpressionsJson("[\"绿水青山就是金山银山\",\"坚持绿色发展\"]");
        material.setCasesJson("[\"浙江安吉余村\"]");
        material.setApplicableEssayThemesJson(null); // null JSON → 空列表
        when(materialMapper.findById(301L)).thenReturn(material);
        ReadingRecord record = new ReadingRecord();
        record.setReadStatus("COMPLETED"); record.setFavorite(true); record.setMasteryLevel("MASTERED");
        when(recordMapper.findByUserIdAndMaterialId(7L, 301L)).thenReturn(record);

        MaterialDetailView view = service.materialDetail(7L, 301L);

        assertEquals(2, view.getStandardExpressions().size());
        assertEquals("绿水青山就是金山银山", view.getStandardExpressions().get(0));
        assertEquals(1, view.getCases().size());
        assertEquals("浙江安吉余村", view.getCases().get(0));
        assertTrue(view.getApplicableEssayThemes().isEmpty());
        assertEquals("COMPLETED", view.getReadStatus());
        assertEquals(true, view.getFavorite());
        assertEquals("MASTERED", view.getMasteryLevel());
    }

    @Test
    void materialsForUserQueriesByTopicWithReadingStatus() {
        ReadingMaterial first = publishedMaterial();
        ReadingMaterial second = publishedMaterial();
        second.setId(302L); second.setTitle("乡村振兴材料二");
        when(materialMapper.listPublishedByTopicId(5L, 200)).thenReturn(Arrays.asList(first, second));
        ReadingRecord record = new ReadingRecord();
        record.setReadStatus("COMPLETED"); record.setFavorite(true); record.setMasteryLevel("MASTERED");
        when(recordMapper.findByUserIdAndMaterialId(7L, 301L)).thenReturn(record);
        when(recordMapper.findByUserIdAndMaterialId(7L, 302L)).thenReturn(null);

        List<MaterialListItem> items = service.materialsForUser(7L, 5L);

        assertEquals(2, items.size());
        assertEquals("COMPLETED", items.get(0).getReadStatus());
        assertEquals(true, items.get(0).getFavorite());
        assertEquals("MASTERED", items.get(0).getMasteryLevel());
        assertNull(items.get(1).getReadStatus());
        assertNull(items.get(1).getFavorite());
        verify(materialMapper).listPublishedByTopicId(5L, 200);
        verify(recordMapper).findByUserIdAndMaterialId(7L, 301L);
        verify(recordMapper).findByUserIdAndMaterialId(7L, 302L);

        BusinessException invalid = assertThrows(BusinessException.class, () -> service.materialsForUser(7L, null));
        assertEquals("INVALID_ARGUMENT", invalid.getCode());
        assertThrows(BusinessException.class, () -> service.materialsForUser(7L, 0L));
        verifyNoMoreInteractions(materialMapper);
    }

    private PoliticalTopic topic(Long id, String code, String name, Long knowledgePointId) {
        PoliticalTopic topic = new PoliticalTopic();
        topic.setId(id); topic.setCode(code); topic.setName(name);
        topic.setKnowledgePointId(knowledgePointId); topic.setStatus("ACTIVE"); topic.setSortNo(1);
        return topic;
    }

    private ReadingMaterial publishedMaterial() {
        ReadingMaterial material = new ReadingMaterial();
        material.setId(301L); material.setTopicId(5L);
        material.setTopicCode("THEME_RURAL"); material.setTopicName("乡村振兴");
        material.setTitle("乡村振兴材料一"); material.setSource("人民日报");
        material.setStatus("PUBLISHED"); material.setContent("全文内容");
        return material;
    }
}
