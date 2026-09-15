package com.gkstudy.practice.dto;

import com.gkstudy.ability.dto.AbilityChange;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import java.util.List;

public class AnswerResult {
    private final Long answerRecordId;
    private final boolean correct;
    private final String correctAnswer;
    private final String analysis;
    private final List<AbilityChange> abilityChanges;
    private final ErrorDiagnosis errorDiagnosis;

    public AnswerResult(Long answerRecordId, boolean correct, String correctAnswer, String analysis,
                        List<AbilityChange> abilityChanges, ErrorDiagnosis errorDiagnosis) {
        this.answerRecordId = answerRecordId;
        this.correct = correct;
        this.correctAnswer = correctAnswer;
        this.analysis = analysis;
        this.abilityChanges = abilityChanges;
        this.errorDiagnosis = errorDiagnosis;
    }

    public Long getAnswerRecordId() { return answerRecordId; }
    public boolean isCorrect() { return correct; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getAnalysis() { return analysis; }
    public List<AbilityChange> getAbilityChanges() { return abilityChanges; }
    public ErrorDiagnosis getErrorDiagnosis() { return errorDiagnosis; }
}
