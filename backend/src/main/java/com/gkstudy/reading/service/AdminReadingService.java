package com.gkstudy.reading.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.common.BusinessException;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.question.mapper.KnowledgePointMapper;
import com.gkstudy.reading.dto.CreateTopicRequest;
import com.gkstudy.reading.dto.SaveMaterialRequest;
import com.gkstudy.reading.mapper.PoliticalTopicMapper;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.model.PoliticalTopic;
import com.gkstudy.reading.model.ReadingMaterial;
import com.gkstudy.reading.ai.AiReadingStructurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminReadingService {
    private static final BigDecimal THEME_KNOWLEDGE_IMPORTANCE = new BigDecimal("0.85");

    private final PoliticalTopicMapper topicMapper;
    private final ReadingMaterialMapper materialMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;
    private final AiReadingStructurer aiStructurer;

    @Autowired
    public AdminReadingService(PoliticalTopicMapper topicMapper, ReadingMaterialMapper materialMapper,
                               KnowledgePointMapper knowledgePointMapper, ObjectMapper objectMapper,
                               AiReadingStructurer aiStructurer) {
        this.topicMapper = topicMapper;
        this.materialMapper = materialMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.objectMapper = objectMapper;
        this.aiStructurer = aiStructurer;
    }

    public AdminReadingService(PoliticalTopicMapper topicMapper, ReadingMaterialMapper materialMapper,
                               KnowledgePointMapper knowledgePointMapper, ObjectMapper objectMapper) {
        this(topicMapper, materialMapper, knowledgePointMapper, objectMapper, null);
    }

    @Transactional(readOnly = true)
    public List<PoliticalTopic> topics() { return topicMapper.findAll(); }

    @Transactional
    public PoliticalTopic createTopic(CreateTopicRequest request) {
        if (topicMapper.findByCode(request.getCode()) != null) {
            throw new BusinessException("TOPIC_CODE_DUPLICATE", "专题编码已存在");
        }
        Long knowledgePointId = knowledgePointMapper.findIdByCode(request.getCode());
        if (knowledgePointId == null) {
            Long themesId = knowledgePointMapper.findIdByCode("ESSAY_THEMES");
            if (themesId == null) throw new BusinessException("KP_NOT_FOUND", "ESSAY_THEMES 知识点不存在，请先初始化申论主题知识点");
            knowledgePointMapper.insert(themesId, request.getCode(), request.getName(), "CIVIL_SERVICE", 3,
                    knowledgePointMapper.findNextSortNo(themesId), THEME_KNOWLEDGE_IMPORTANCE);
            knowledgePointId = knowledgePointMapper.findIdByCode(request.getCode());
        }
        PoliticalTopic topic = new PoliticalTopic();
        topic.setCode(request.getCode());
        topic.setName(request.getName());
        topic.setKnowledgePointId(knowledgePointId);
        topic.setDescription(request.getDescription());
        topic.setStatus("ACTIVE");
        topic.setSortNo(topicMapper.findNextSortNo());
        topicMapper.insert(topic);
        return topicMapper.findById(topic.getId());
    }

    @Transactional(readOnly = true)
    public List<ReadingMaterial> materials(String keyword, Long topicId, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return materialMapper.adminList(keyword, topicId, status, (safePage - 1) * safeSize, safeSize);
    }

    @Transactional(readOnly = true)
    public int countMaterials(String keyword, Long topicId, String status) {
        return materialMapper.adminCount(keyword, topicId, status);
    }

    @Transactional(readOnly = true)
    public ReadingMaterial materialDetail(Long id) {
        ReadingMaterial material = materialMapper.findById(id);
        if (material == null) throw new BusinessException("MATERIAL_NOT_FOUND", "阅读材料不存在");
        return material;
    }

    @Transactional
    public ReadingMaterial createMaterial(SaveMaterialRequest request) {
        requireTopic(request.getTopicId());
        ReadingMaterial material = new ReadingMaterial();
        applyRequest(material, request);
        material.setStatus(normalizeStatus(request.getStatus()));
        materialMapper.insert(material);
        return materialMapper.findById(material.getId());
    }

    @Transactional
    public ReadingMaterial updateMaterial(Long id, SaveMaterialRequest request) {
        ReadingMaterial existing = materialMapper.findById(id);
        if (existing == null) throw new BusinessException("MATERIAL_NOT_FOUND", "阅读材料不存在");
        requireTopic(request.getTopicId());
        ReadingMaterial material = new ReadingMaterial();
        material.setId(id);
        applyRequest(material, request);
        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            material.setStatus(existing.getStatus());
        } else {
            material.setStatus(normalizeStatus(request.getStatus()));
        }
        materialMapper.update(material);
        return materialMapper.findById(id);
    }

    @Transactional
    public ReadingMaterial changeStatus(Long id, String status) {
        ReadingMaterial existing = materialMapper.findById(id);
        if (existing == null) throw new BusinessException("MATERIAL_NOT_FOUND", "阅读材料不存在");
        if (status == null || status.trim().isEmpty()) throw new BusinessException("INVALID_ARGUMENT", "status 不能为空");
        materialMapper.updateStatus(id, normalizeStatus(status));
        return materialMapper.findById(id);
    }

    public ReadingMaterial aiStructure(Long id) {
        ReadingMaterial material = materialMapper.findById(id);
        if (material == null) throw new BusinessException("MATERIAL_NOT_FOUND", "阅读材料不存在");
        if (aiStructurer == null) throw new BusinessException("AI_NOT_AVAILABLE", "AI结构化暂时不可用，可稍后重试");
        try {
            AiReadingStructurer.Result result = aiStructurer.structure(material);
            material.setCoreView(result.getCoreView()); material.setProblem(result.getProblem()); material.setCause(result.getCause());
            material.setSolution(result.getSolution()); material.setPolicyLogic(result.getPolicyLogic());
            material.setStandardExpressionsJson(toJson(result.getStandardExpressions())); material.setCasesJson(toJson(result.getCases()));
            material.setApplicableEssayThemesJson(toJson(result.getApplicableEssayThemes()));
            material.setTopicCandidatesJson(toJson(result.getTopicCandidates()));
            material.setAiConfidence(BigDecimal.valueOf(result.getConfidence()).setScale(2, RoundingMode.HALF_UP));
            material.setAiProvider(result.getResponse().getProvider()); material.setAiModel(result.getResponse().getModel());
            material.setAiPromptVersion(AiReadingStructurer.PROMPT_VERSION); material.setAiStatus("SUCCESS");
            material.setAiRawResponse(result.getResponse().getRawResponse());
            material.setAiRequestTime(result.getResponse().getRequestTime()); material.setAiLatencyMs(result.getResponse().getLatencyMs());
            material.setStatus("DRAFT");
            materialMapper.updateAiStructure(material);
            return materialMapper.findById(id);
        } catch (AiProviderException e) {
            material.setAiPromptVersion(AiReadingStructurer.PROMPT_VERSION); material.setAiRequestTime(LocalDateTime.now());
            materialMapper.updateAiFailure(material);
            throw new BusinessException("AI_STRUCTURE_FAILED", "AI结构化暂时不可用，可稍后重试");
        }
    }

    private void requireTopic(Long topicId) {
        if (topicMapper.findById(topicId) == null) throw new BusinessException("TOPIC_NOT_FOUND", "专题不存在");
    }

    private void applyRequest(ReadingMaterial material, SaveMaterialRequest request) {
        material.setTopicId(request.getTopicId());
        material.setTitle(request.getTitle());
        material.setSource(request.getSource());
        material.setPublishDate(request.getPublishDate());
        material.setContent(request.getContent());
        material.setCoreView(request.getCoreView());
        material.setProblem(request.getProblem());
        material.setCause(request.getCause());
        material.setSolution(request.getSolution());
        material.setPolicyLogic(request.getPolicyLogic());
        material.setStandardExpressionsJson(toJson(request.getStandardExpressions()));
        material.setCasesJson(toJson(request.getCases()));
        material.setApplicableEssayThemesJson(toJson(request.getApplicableEssayThemes()));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "DRAFT";
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status) && !"ARCHIVED".equals(status)) {
            throw new BusinessException("INVALID_STATUS", "材料状态仅允许 DRAFT/PUBLISHED/ARCHIVED");
        }
        return status;
    }

    private String toJson(List<String> values) {
        if (values == null) return null;
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("阅读材料 JSON 字段序列化失败", e);
        }
    }
}
