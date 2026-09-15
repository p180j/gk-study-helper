package com.gkstudy.ability.service;

import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AbilityReplayService {
    private final AbilityMapper abilityMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final AbilityService abilityService;

    public AbilityReplayService(AbilityMapper abilityMapper, AnswerRecordMapper answerRecordMapper, AbilityService abilityService) {
        this.abilityMapper = abilityMapper; this.answerRecordMapper = answerRecordMapper; this.abilityService = abilityService;
    }

    @Transactional
    public List<AbilityProfile> replay(Long userId) {
        List<AnswerRecord> records = answerRecordMapper.findAllByUserId(userId);
        abilityMapper.deleteProfilesByUserId(userId);
        for (AnswerRecord record : records) abilityService.replayUpdate(record);
        return abilityMapper.findByUserId(userId);
    }
}
