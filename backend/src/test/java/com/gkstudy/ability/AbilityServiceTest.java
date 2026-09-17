package com.gkstudy.ability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.dto.AbilityOverview;
import com.gkstudy.ability.engine.AbilityEngine;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.ability.service.AbilityService;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityServiceTest {
    @Test
    void newUserReceivesCompleteUnassessedMapWithoutPersistingProfiles() {
        AbilityMapper mapper = mock(AbilityMapper.class);
        List<AbilityProfile> map = Arrays.asList(unassessed(1L, "DATA_ANALYSIS"), unassessed(2L, "ESSAY_SUMMARY"));
        when(mapper.findCompleteMap(9L)).thenReturn(map);
        AbilityService service = service(mapper);

        AbilityOverview overview = service.overview(9L);

        assertEquals(0, overview.getEvaluatedCount()); assertEquals(2, overview.getTotalCount());
        assertEquals(0, overview.getCoveragePercent());
        assertTrue(overview.getAbilities().stream().allMatch(item -> "UNASSESSED".equals(item.getStatus())));
        assertTrue(overview.getAbilities().stream().allMatch(item -> item.getMasteryScore().signum() == 0
                && item.getSpeedScore().signum() == 0 && item.getStabilityScore().signum() == 0
                && item.getConfidenceScore().signum() == 0 && item.getSampleCount() == 0));
        verify(mapper, never()).insertProfile(any()); verify(mapper, never()).updateProfile(any());
    }

    @Test
    void existingAbilityIsReturnedWithoutBeingOverwrittenAndCoverageIsCalculated() {
        AbilityMapper mapper = mock(AbilityMapper.class);
        AbilityProfile evaluated = unassessed(1L, "DATA_ANALYSIS"); evaluated.setId(88L); evaluated.setStatus("LEARNING");
        evaluated.setMasteryScore(BigDecimal.valueOf(62)); evaluated.setSampleCount(3);
        when(mapper.findCompleteMap(9L)).thenReturn(Arrays.asList(evaluated, unassessed(2L, "ESSAY_SUMMARY")));
        AbilityService service = service(mapper);

        AbilityOverview overview = service.overview(9L);

        assertEquals(1, overview.getEvaluatedCount()); assertEquals(2, overview.getTotalCount()); assertEquals(50, overview.getCoveragePercent());
        assertEquals(new BigDecimal("62"), overview.getAbilities().get(0).getMasteryScore());
        verify(mapper, never()).insertProfile(any()); verify(mapper, never()).updateProfile(any());
    }

    private AbilityService service(AbilityMapper mapper) {
        return new AbilityService(new AbilityEngine(), mapper, mock(AnswerRecordMapper.class), new ObjectMapper());
    }

    private AbilityProfile unassessed(long id, String code) {
        AbilityProfile profile = new AbilityProfile(); profile.setKnowledgePointId(id); profile.setKnowledgePointCode(code);
        profile.setMasteryScore(BigDecimal.ZERO); profile.setSpeedScore(BigDecimal.ZERO); profile.setStabilityScore(BigDecimal.ZERO);
        profile.setConfidenceScore(BigDecimal.ZERO); profile.setSampleCount(0); profile.setStatus("UNASSESSED"); return profile;
    }
}
