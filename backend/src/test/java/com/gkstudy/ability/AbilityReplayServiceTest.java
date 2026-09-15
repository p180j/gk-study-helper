package com.gkstudy.ability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.engine.AbilityEngine;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.ability.service.AbilityReplayService;
import com.gkstudy.ability.service.AbilityService;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AbilityReplayServiceTest {
    @Test
    void replaysAnswerRecordsInTimeOrderAndReturnsRebuiltProfile() {
        AbilityMapper abilityMapper = mock(AbilityMapper.class); AnswerRecordMapper answerMapper = mock(AnswerRecordMapper.class); AbilityService abilityService = mock(AbilityService.class);
        AnswerRecord first = record(1L, LocalDateTime.of(2026, 1, 1, 9, 0)); AnswerRecord second = record(2L, LocalDateTime.of(2026, 1, 1, 10, 0));
        AbilityProfile rebuilt = new AbilityProfile();
        when(answerMapper.findAllByUserId(7L)).thenReturn(Arrays.asList(first, second)); when(abilityMapper.findByUserId(7L)).thenReturn(Collections.singletonList(rebuilt));
        AbilityReplayService replayService = new AbilityReplayService(abilityMapper, answerMapper, abilityService);
        assertSame(rebuilt, replayService.replay(7L).get(0));
        verify(abilityMapper).deleteProfilesByUserId(7L);
        org.mockito.InOrder order = inOrder(abilityService); order.verify(abilityService).replayUpdate(first); order.verify(abilityService).replayUpdate(second);
        verifyNoMoreInteractions(abilityService);
    }

    @Test
    void replayDoesNotDuplicateHistoryAndKeepsProfileIdentical() {
        AbilityMapper abilityMapper = mock(AbilityMapper.class); AnswerRecordMapper answerMapper = mock(AnswerRecordMapper.class);
        List<AnswerRecord> records = Arrays.asList(abilityRecord(1, false), abilityRecord(2, false), abilityRecord(3, true),
                abilityRecord(4, true), abilityRecord(5, true), abilityRecord(6, true));
        AtomicReference<AbilityProfile> storedProfile = new AtomicReference<>(); AtomicInteger historyCount = new AtomicInteger();
        when(answerMapper.findAllByUserId(7L)).thenReturn(records);
        when(abilityMapper.findForUpdate(7L, 12L)).thenAnswer(invocation -> storedProfile.get());
        when(abilityMapper.insertProfile(any())).thenAnswer(invocation -> { storedProfile.set(invocation.getArgument(0)); return 1; });
        when(abilityMapper.updateProfile(any())).thenAnswer(invocation -> { storedProfile.set(invocation.getArgument(0)); return 1; });
        when(abilityMapper.insertHistory(any())).thenAnswer(invocation -> { historyCount.incrementAndGet(); return 1; });
        when(abilityMapper.deleteProfilesByUserId(7L)).thenAnswer(invocation -> { storedProfile.set(null); return 1; });
        when(abilityMapper.findByUserId(7L)).thenAnswer(invocation -> storedProfile.get() == null ? Collections.emptyList() : Collections.singletonList(storedProfile.get()));
        AbilityService abilityService = new AbilityService(new AbilityEngine(), abilityMapper, answerMapper, new ObjectMapper());
        AbilityReplayService replayService = new AbilityReplayService(abilityMapper, answerMapper, abilityService);

        for (AnswerRecord record : records) abilityService.update(record);
        assertEquals(6, historyCount.get());
        AbilityProfile expected = copy(storedProfile.get());

        AbilityProfile firstReplay = replayService.replay(7L).get(0);
        assertEquals(6, historyCount.get());
        assertProfileEquals(expected, firstReplay);

        AbilityProfile secondReplay = replayService.replay(7L).get(0);
        assertEquals(6, historyCount.get());
        assertProfileEquals(expected, secondReplay);
        assertEquals(6, records.size());
    }

    private AnswerRecord record(Long id, LocalDateTime time) { AnswerRecord record = new AnswerRecord(); record.setId(id); record.setAnswerTime(time); return record; }

    private AnswerRecord abilityRecord(long id, boolean correct) {
        AnswerRecord record = record(id, LocalDateTime.of(2026, 1, 1, 9, 0).plusMinutes(id));
        record.setUserId(7L); record.setCorrect(correct); record.setDifficultySnapshot(BigDecimal.valueOf(50 + id));
        record.setDurationMs(id == 3 ? 180000L : 60000L); record.setStandardTimeSecondsSnapshot(60);
        record.setConfidenceType(correct ? "SURE" : "HESITANT"); record.setPracticeType(id == 6 ? "VALIDATION" : "DAILY");
        record.setKnowledgeSnapshot("[{\"id\":12,\"code\":\"AVG_GROWTH_RATE\",\"name\":\"年均增长率\",\"relationType\":\"PRIMARY\",\"weight\":1.0}]");
        return record;
    }

    private AbilityProfile copy(AbilityProfile source) {
        AbilityProfile copy = new AbilityProfile(); copy.setUserId(source.getUserId()); copy.setKnowledgePointId(source.getKnowledgePointId());
        copy.setMasteryScore(source.getMasteryScore()); copy.setSpeedScore(source.getSpeedScore());
        copy.setStabilityScore(source.getStabilityScore()); copy.setConfidenceScore(source.getConfidenceScore()); copy.setSampleCount(source.getSampleCount());
        copy.setStatus(source.getStatus()); copy.setLastPracticeTime(source.getLastPracticeTime()); return copy;
    }

    private void assertProfileEquals(AbilityProfile expected, AbilityProfile actual) {
        assertEquals(expected.getUserId(), actual.getUserId()); assertEquals(expected.getKnowledgePointId(), actual.getKnowledgePointId());
        assertEquals(expected.getMasteryScore(), actual.getMasteryScore()); assertEquals(expected.getSpeedScore(), actual.getSpeedScore());
        assertEquals(expected.getStabilityScore(), actual.getStabilityScore()); assertEquals(expected.getConfidenceScore(), actual.getConfidenceScore());
        assertEquals(expected.getSampleCount(), actual.getSampleCount()); assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getLastPracticeTime(), actual.getLastPracticeTime());
    }
}
