package com.gkstudy.essay.service;

import com.gkstudy.ability.dto.AbilityChange;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.common.BusinessException;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.engine.EssayAbilityEngine;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.question.mapper.KnowledgePointMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EssayAbilityService {
    private final EssayAbilityEngine engine;
    private final AbilityMapper abilityMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    public EssayAbilityService(EssayAbilityEngine engine, AbilityMapper abilityMapper, KnowledgePointMapper knowledgePointMapper) {
        this.engine = engine; this.abilityMapper = abilityMapper; this.knowledgePointMapper = knowledgePointMapper;
    }

    @Transactional
    public List<AbilityChange> apply(Long userId, EssayQuestion question, EssayEvaluationResult result) {
        Map<String, Long> kpIdCache = new HashMap<>();
        List<AbilityChange> changes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, Double> entry : result.getDimensionScores().entrySet()) {
            changes.add(updateOne(userId, entry.getKey(), null, entry.getValue(), kpIdCache, now));
        }
        changes.add(updateOne(userId, question.getTopicCode(), question.getTopicKnowledgePointId(), result.getTotalScore(), kpIdCache, now));
        return changes;
    }

    private AbilityChange updateOne(Long userId, String kpCode, Long knownKpId, double score, Map<String, Long> kpIdCache, LocalDateTime now) {
        Long kpId = knownKpId != null ? knownKpId : resolveKpId(kpCode, kpIdCache);
        AbilityProfile current = abilityMapper.findForUpdate(userId, kpId);
        EssayAbilityEngine.AbilityValues values = engine.update(current, score);
        AbilityProfile updated = new AbilityProfile();
        updated.setId(current == null ? null : current.getId());
        updated.setUserId(userId); updated.setKnowledgePointId(kpId);
        updated.setMasteryScore(values.getMastery()); updated.setSpeedScore(values.getSpeed());
        updated.setStabilityScore(values.getStability()); updated.setConfidenceScore(values.getConfidence());
        updated.setSampleCount(values.getSampleCount()); updated.setStatus(values.getStatus());
        updated.setLastPracticeTime(now);
        if (current == null) abilityMapper.insertProfile(updated); else abilityMapper.updateProfile(updated);
        abilityMapper.insertHistory(updated);
        return new AbilityChange(kpCode, score(current == null ? null : current.getMasteryScore(), 50), updated.getMasteryScore(),
                score(current == null ? null : current.getSpeedScore(), 50), updated.getSpeedScore(), updated.getStabilityScore(),
                updated.getConfidenceScore(), updated.getStatus());
    }

    private Long resolveKpId(String code, Map<String, Long> kpIdCache) {
        Long kpId = kpIdCache.get(code);
        if (kpId == null) {
            kpId = knowledgePointMapper.findIdByCode(code);
            if (kpId == null) throw new BusinessException("KP_NOT_FOUND", "申论知识点不存在: " + code);
            kpIdCache.put(code, kpId);
        }
        return kpId;
    }

    private BigDecimal score(BigDecimal value, int fallback) { return value == null ? BigDecimal.valueOf(fallback) : value; }
}
