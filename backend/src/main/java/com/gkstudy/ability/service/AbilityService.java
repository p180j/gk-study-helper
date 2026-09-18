package com.gkstudy.ability.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.dto.AbilityChange;
import com.gkstudy.ability.dto.AbilityOverview;
import com.gkstudy.ability.engine.AbilityConstants;
import com.gkstudy.ability.engine.AbilityEngine;
import com.gkstudy.ability.engine.AbilityEvent;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.question.model.KnowledgePointRef;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class AbilityService {
    private static final TypeReference<List<KnowledgePointRef>> KNOWLEDGE_TYPE = new TypeReference<List<KnowledgePointRef>>() { };
    private final AbilityEngine abilityEngine;
    private final AbilityMapper abilityMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final ObjectMapper objectMapper;

    public AbilityService(AbilityEngine abilityEngine, AbilityMapper abilityMapper, AnswerRecordMapper answerRecordMapper, ObjectMapper objectMapper) {
        this.abilityEngine = abilityEngine; this.abilityMapper = abilityMapper; this.answerRecordMapper = answerRecordMapper; this.objectMapper = objectMapper;
    }

    public List<AbilityChange> update(AnswerRecord record) {
        return update(record, true);
    }

    public List<AbilityChange> replayUpdate(AnswerRecord record) {
        return update(record, false);
    }

    private List<AbilityChange> update(AnswerRecord record, boolean writeHistory) {
        List<KnowledgePointRef> knowledgePoints = parseKnowledge(record.getKnowledgeSnapshot());
        List<AnswerRecord> records = answerRecordMapper.findAllByUserId(record.getUserId());
        List<AbilityChange> changes = new ArrayList<>();
        for (KnowledgePointRef knowledge : knowledgePoints) {
            AbilityProfile oldProfile = abilityMapper.findForUpdate(record.getUserId(), knowledge.getId());
            AbilityEvent event = buildEvent(record, knowledge, records);
            AbilityProfile newProfile = abilityEngine.calculate(oldProfile, event);
            newProfile.setUserId(record.getUserId()); newProfile.setKnowledgePointId(knowledge.getId());
            if (oldProfile == null) abilityMapper.insertProfile(newProfile); else abilityMapper.updateProfile(newProfile);
            if (writeHistory) abilityMapper.insertHistory(newProfile);
            changes.add(new AbilityChange(knowledge.getCode(), score(oldProfile == null ? null : oldProfile.getMasteryScore(), 50), newProfile.getMasteryScore(),
                    score(oldProfile == null ? null : oldProfile.getSpeedScore(), 50), newProfile.getSpeedScore(), newProfile.getStabilityScore(),
                    newProfile.getConfidenceScore(), newProfile.getStatus()));
        }
        return changes;
    }

    public List<AbilityProfile> profiles(Long userId) { return abilityMapper.findCompleteMap(userId); }

    public AbilityOverview overview(Long userId) {
        List<AbilityProfile> abilities = profiles(userId);
        int evaluated = 0;
        int assessing = 0;
        for (AbilityProfile profile : abilities) {
            if (isFullyAssessed(profile)) evaluated++;
            else if (profile.getSampleCount() != null && profile.getSampleCount() > 0) assessing++;
        }
        applyTrends(abilities, abilityMapper.findMasteryTrends(userId));
        return new AbilityOverview(evaluated, assessing, abilities.size(), abilities);
    }

    private boolean isFullyAssessed(AbilityProfile profile) {
        return profile.getSampleCount() != null && profile.getSampleCount() >= AbilityConstants.ASSESSMENT_MIN_SAMPLE_COUNT
                && !"UNASSESSED".equals(profile.getStatus());
    }

    private void applyTrends(List<AbilityProfile> abilities, List<AbilityProfile> trends) {
        if (trends == null || trends.isEmpty()) return;
        Map<Long, BigDecimal> trendByPoint = new HashMap<>();
        for (AbilityProfile trend : trends) trendByPoint.put(trend.getKnowledgePointId(), trend.getMasteryTrend());
        for (AbilityProfile profile : abilities) {
            BigDecimal trend = trendByPoint.get(profile.getKnowledgePointId());
            if (trend != null) profile.setMasteryTrend(trend.setScale(1, RoundingMode.HALF_UP));
        }
    }

    private AbilityEvent buildEvent(AnswerRecord current, KnowledgePointRef knowledge, List<AnswerRecord> allRecords) {
        List<AnswerRecord> matching = new ArrayList<>();
        for (AnswerRecord record : allRecords) {
            if (after(record, current)) break;
            if (containsKnowledge(record, knowledge.getId())) matching.add(record);
        }
        int from = Math.max(0, matching.size() - AbilityConstants.RECENT_WINDOW_SIZE);
        List<AnswerRecord> recent = matching.subList(from, matching.size());
        AbilityEvent event = new AbilityEvent();
        event.setCorrect(Boolean.TRUE.equals(current.getCorrect())); event.setDifficulty(current.getDifficultySnapshot());
        event.setDurationMs(current.getDurationMs()); event.setStandardTimeSeconds(current.getStandardTimeSecondsSnapshot());
        event.setAnswerConfidenceType(current.getConfidenceType()); event.setPracticeType(current.getPracticeType());
        event.setKnowledgeWeight(knowledge.getWeight() == null ? BigDecimal.ONE : knowledge.getWeight()); event.setAnswerTime(current.getAnswerTime());
        List<Boolean> correctness = new ArrayList<>(); List<BigDecimal> difficulties = new ArrayList<>(); List<String> practices = new ArrayList<>();
        for (AnswerRecord record : recent) { correctness.add(record.getCorrect()); difficulties.add(record.getDifficultySnapshot()); practices.add(record.getPracticeType()); }
        event.setRecentCorrectness(correctness); event.setRecentDifficulties(difficulties); event.setRecentPracticeTypes(practices);
        return event;
    }

    private boolean after(AnswerRecord candidate, AnswerRecord current) {
        int time = candidate.getAnswerTime().compareTo(current.getAnswerTime());
        return time > 0 || (time == 0 && candidate.getId() > current.getId());
    }

    private boolean containsKnowledge(AnswerRecord record, Long knowledgePointId) {
        for (KnowledgePointRef knowledge : parseKnowledge(record.getKnowledgeSnapshot())) if (knowledgePointId.equals(knowledge.getId())) return true;
        return false;
    }

    private List<KnowledgePointRef> parseKnowledge(String snapshot) {
        try { return objectMapper.readValue(snapshot, KNOWLEDGE_TYPE); }
        catch (Exception e) { throw new IllegalStateException("知识点快照无法回放", e); }
    }

    private BigDecimal score(BigDecimal value, int fallback) { return value == null ? BigDecimal.valueOf(fallback) : value; }
}
