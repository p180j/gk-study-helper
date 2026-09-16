package com.gkstudy.essay;

import com.gkstudy.ability.dto.AbilityChange;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.engine.EssayAbilityEngine;
import com.gkstudy.essay.engine.EssayConstants;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.essay.service.EssayAbilityService;
import com.gkstudy.question.mapper.KnowledgePointMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EssayAbilityServiceTest {
    private AbilityMapper abilityMapper;
    private KnowledgePointMapper knowledgePointMapper;
    private EssayAbilityService service;

    @BeforeEach
    void setUp() {
        abilityMapper = mock(AbilityMapper.class);
        knowledgePointMapper = mock(KnowledgePointMapper.class);
        service = new EssayAbilityService(new EssayAbilityEngine(), abilityMapper, knowledgePointMapper);
        when(knowledgePointMapper.findIdByCode(EssayConstants.DIM_MATERIAL_READING)).thenReturn(11L);
        when(knowledgePointMapper.findIdByCode(EssayConstants.DIM_EXPRESSION)).thenReturn(12L);
    }

    @Test
    void applyUpdatesEveryDimensionAndTopicKnowledgePoint() {
        when(abilityMapper.findForUpdate(7L, 11L)).thenReturn(null);
        when(abilityMapper.findForUpdate(7L, 12L)).thenReturn(existingProfile());
        when(abilityMapper.findForUpdate(7L, 77L)).thenReturn(null);

        List<AbilityChange> changes = service.apply(7L, question(), graded());

        assertEquals(3, changes.size());
        assertEquals(EssayConstants.DIM_MATERIAL_READING, changes.get(0).getKnowledgePointCode());
        assertEquals(EssayConstants.DIM_EXPRESSION, changes.get(1).getKnowledgePointCode());
        assertEquals("THEME_RURAL", changes.get(2).getKnowledgePointCode());
        verify(abilityMapper).findForUpdate(7L, 11L);
        verify(abilityMapper).findForUpdate(7L, 12L);
        verify(abilityMapper).findForUpdate(7L, 77L);
        verify(abilityMapper, times(2)).insertProfile(any());
        verify(abilityMapper, times(1)).updateProfile(any());
        verify(abilityMapper, times(3)).insertHistory(any());
    }

    @Test
    void newAbilityIsInsertedAndExistingAbilityIsUpdated() {
        when(abilityMapper.findForUpdate(7L, 11L)).thenReturn(null);
        when(abilityMapper.findForUpdate(7L, 12L)).thenReturn(existingProfile());
        when(abilityMapper.findForUpdate(7L, 77L)).thenReturn(null);

        service.apply(7L, question(), graded());

        ArgumentCaptor<AbilityProfile> insertCaptor = ArgumentCaptor.forClass(AbilityProfile.class);
        verify(abilityMapper, times(2)).insertProfile(insertCaptor.capture());
        for (AbilityProfile inserted : insertCaptor.getAllValues()) {
            assertNull(inserted.getId());
            assertEquals(7L, inserted.getUserId());
            assertEquals(1, inserted.getSampleCount());
            assertNotNull(inserted.getLastPracticeTime());
        }
        // 维度知识点 id 通过 code 解析，主题知识点 id 直接来自题目
        assertTrue(insertCaptor.getAllValues().stream().anyMatch(p -> p.getKnowledgePointId().equals(11L)));
        assertTrue(insertCaptor.getAllValues().stream().anyMatch(p -> p.getKnowledgePointId().equals(77L)));

        ArgumentCaptor<AbilityProfile> updateCaptor = ArgumentCaptor.forClass(AbilityProfile.class);
        verify(abilityMapper).updateProfile(updateCaptor.capture());
        AbilityProfile updated = updateCaptor.getValue();
        assertEquals(5L, updated.getId());
        assertEquals(12L, updated.getKnowledgePointId());
        assertEquals(2, updated.getSampleCount());
    }

    @Test
    void abilityChangeCarriesBeforeAndAfterValues() {
        when(abilityMapper.findForUpdate(7L, 11L)).thenReturn(null);
        when(abilityMapper.findForUpdate(7L, 12L)).thenReturn(existingProfile());
        when(abilityMapper.findForUpdate(7L, 77L)).thenReturn(null);

        List<AbilityChange> changes = service.apply(7L, question(), graded());

        AbilityChange fresh = changes.get(0); // 无历史能力 → 前值取 50 兜底
        assertEquals(0, fresh.getOldMastery().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, fresh.getNewMastery().compareTo(BigDecimal.valueOf(80)));
        assertEquals("PROFICIENT", fresh.getStatus());
        AbilityChange existing = changes.get(1); // 已有能力 → 平滑更新
        assertEquals(0, existing.getOldMastery().compareTo(BigDecimal.valueOf(50)));
        assertTrue(existing.getNewMastery().doubleValue() > 50 && existing.getNewMastery().doubleValue() < 60,
                "新 mastery 应介于 50 与 60 之间，实际 " + existing.getNewMastery());
        assertEquals(0, existing.getOldSpeed().compareTo(BigDecimal.valueOf(55)));
        AbilityChange theme = changes.get(2); // 主题知识点首样本
        assertEquals(0, theme.getOldMastery().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, theme.getNewMastery().compareTo(BigDecimal.valueOf(75)));
    }

    private EssayQuestion question() {
        EssayQuestion question = new EssayQuestion();
        question.setId(1001L); question.setQuestionType("SUMMARY");
        question.setTopicCode("THEME_RURAL"); question.setTopicName("乡村振兴");
        question.setTopicKnowledgePointId(77L);
        return question;
    }

    private EssayEvaluationResult graded() {
        Map<String, Double> dims = new LinkedHashMap<>();
        dims.put(EssayConstants.DIM_MATERIAL_READING, 80.0);
        dims.put(EssayConstants.DIM_EXPRESSION, 60.0);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("totalScore", 75.0);
        return new EssayEvaluationResult("LOCAL_RULE_V1", 75.0, dims, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), evidence);
    }

    private AbilityProfile existingProfile() {
        AbilityProfile profile = new AbilityProfile();
        profile.setId(5L); profile.setUserId(7L); profile.setKnowledgePointId(12L);
        profile.setMasteryScore(BigDecimal.valueOf(50)); profile.setSpeedScore(BigDecimal.valueOf(55));
        profile.setStabilityScore(BigDecimal.valueOf(55)); profile.setConfidenceScore(BigDecimal.valueOf(30));
        profile.setSampleCount(1); profile.setStatus("LEARNING");
        return profile;
    }
}
