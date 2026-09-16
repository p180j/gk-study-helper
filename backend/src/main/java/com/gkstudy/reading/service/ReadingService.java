package com.gkstudy.reading.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReadingService {
    private static final int USER_MATERIAL_LIMIT = 200;

    private final PoliticalTopicMapper topicMapper;
    private final ReadingMaterialMapper materialMapper;
    private final ReadingRecordMapper recordMapper;
    private final ObjectMapper objectMapper;

    public ReadingService(PoliticalTopicMapper topicMapper, ReadingMaterialMapper materialMapper,
                          ReadingRecordMapper recordMapper, ObjectMapper objectMapper) {
        this.topicMapper = topicMapper;
        this.materialMapper = materialMapper;
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TopicView> topicsWithProgress(Long userId) {
        List<TopicView> views = new ArrayList<>();
        for (PoliticalTopic topic : topicMapper.findAllActive()) {
            int total = topicMapper.countMaterialsByTopicId(topic.getId(), "PUBLISHED");
            int completed = recordMapper.countCompletedByUserAndTopicKpId(userId, topic.getKnowledgePointId());
            views.add(new TopicView(topic.getId(), topic.getCode(), topic.getName(), topic.getDescription(), total, completed));
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<MaterialListItem> materialsForUser(Long userId, Long topicId) {
        if (topicId == null || topicId <= 0) throw new BusinessException("INVALID_ARGUMENT", "topicId 不合法");
        List<MaterialListItem> items = new ArrayList<>();
        for (ReadingMaterial material : materialMapper.listPublishedByTopicId(topicId, USER_MATERIAL_LIMIT)) {
            ReadingRecord record = recordMapper.findByUserIdAndMaterialId(userId, material.getId());
            items.add(toListItem(material, record));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public MaterialDetailView materialDetail(Long userId, Long materialId) {
        ReadingMaterial material = materialMapper.findById(materialId);
        if (material == null) throw new BusinessException("MATERIAL_NOT_FOUND", "阅读材料不存在");
        if (!"PUBLISHED".equals(material.getStatus())) throw new BusinessException("MATERIAL_NOT_PUBLISHED", "阅读材料未发布");
        ReadingRecord record = recordMapper.findByUserIdAndMaterialId(userId, materialId);
        return toDetailView(material, record);
    }

    @Transactional
    public MaterialDetailView saveRecord(Long userId, SaveReadingRecordRequest request) {
        ReadingMaterial material = materialMapper.findById(request.getMaterialId());
        if (material == null || !"PUBLISHED".equals(material.getStatus())) {
            throw new BusinessException("MATERIAL_NOT_PUBLISHED", "阅读材料不存在或未发布");
        }
        ReadingRecord record = recordMapper.findForUpdate(userId, request.getMaterialId());
        LocalDateTime now = LocalDateTime.now();
        if (record == null) {
            record = new ReadingRecord();
            record.setUserId(userId);
            record.setMaterialId(request.getMaterialId());
            record.setReadStatus(request.getReadStatus() != null ? request.getReadStatus() : "READING");
            record.setFavorite(Boolean.TRUE.equals(request.getFavorite()));
            record.setMasteryLevel(request.getMasteryLevel() != null ? request.getMasteryLevel() : "UNMASTERED");
            record.setDurationMs(request.getDurationMs());
            record.setFirstReadTime(now);
            record.setLastReadTime(now);
            recordMapper.insert(record);
        } else {
            if (request.getReadStatus() != null) record.setReadStatus(request.getReadStatus());
            if (request.getFavorite() != null) record.setFavorite(request.getFavorite());
            if (request.getMasteryLevel() != null) record.setMasteryLevel(request.getMasteryLevel());
            if (request.getDurationMs() != null) record.setDurationMs(request.getDurationMs());
            record.setLastReadTime(now);
            recordMapper.update(record);
        }
        return materialDetail(userId, request.getMaterialId());
    }

    @Transactional(readOnly = true)
    public int countCompleted(Long userId, Long topicKnowledgePointId) {
        return recordMapper.countCompletedByUserAndTopicKpId(userId, topicKnowledgePointId);
    }

    private MaterialListItem toListItem(ReadingMaterial material, ReadingRecord record) {
        MaterialListItem item = new MaterialListItem();
        item.setId(material.getId());
        item.setTopicId(material.getTopicId());
        item.setTopicCode(material.getTopicCode());
        item.setTopicName(material.getTopicName());
        item.setTitle(material.getTitle());
        item.setSource(material.getSource());
        item.setPublishDate(material.getPublishDate());
        item.setCoreView(material.getCoreView());
        if (record != null) {
            item.setReadStatus(record.getReadStatus());
            item.setFavorite(record.getFavorite());
            item.setMasteryLevel(record.getMasteryLevel());
        }
        return item;
    }

    private MaterialDetailView toDetailView(ReadingMaterial material, ReadingRecord record) {
        MaterialDetailView view = new MaterialDetailView();
        view.setId(material.getId());
        view.setTopicId(material.getTopicId());
        view.setTopicCode(material.getTopicCode());
        view.setTopicName(material.getTopicName());
        view.setTitle(material.getTitle());
        view.setSource(material.getSource());
        view.setPublishDate(material.getPublishDate());
        view.setCoreView(material.getCoreView());
        view.setContent(material.getContent());
        view.setProblem(material.getProblem());
        view.setCause(material.getCause());
        view.setSolution(material.getSolution());
        view.setPolicyLogic(material.getPolicyLogic());
        view.setStandardExpressions(parseStringList(material.getStandardExpressionsJson()));
        view.setCases(parseStringList(material.getCasesJson()));
        view.setApplicableEssayThemes(parseStringList(material.getApplicableEssayThemesJson()));
        if (record != null) {
            view.setReadStatus(record.getReadStatus());
            view.setFavorite(record.getFavorite());
            view.setMasteryLevel(record.getMasteryLevel());
        }
        return view;
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("阅读材料 JSON 字段解析失败", e);
        }
    }
}
