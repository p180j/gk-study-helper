package com.gkstudy.practice.dto;

import com.gkstudy.ability.dto.AbilityChange;
import java.util.List;

public class AnswerResult {
    private final Long answerRecordId;
    private final boolean correct;
    private final String correctAnswer;
    private final String analysis;
    private final List<AbilityChange> abilityChanges;

    public AnswerResult(Long answerRecordId, boolean correct, String correctAnswer, String analysis, List<AbilityChange> abilityChanges) {
        this.answerRecordId = answerRecordId;
        this.correct = correct;
        this.correctAnswer = correctAnswer;
        this.analysis = analysis;
        this.abilityChanges = abilityChanges;
    }

    public Long getAnswerRecordId() { return answerRecordId; }
    public boolean isCorrect() { return correct; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getAnalysis() { return analysis; }
    public List<AbilityChange> getAbilityChanges() { return abilityChanges; }
}
