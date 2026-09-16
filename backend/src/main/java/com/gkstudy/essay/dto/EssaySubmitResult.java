package com.gkstudy.essay.dto;

import com.gkstudy.ability.dto.AbilityChange;

import java.util.List;

public class EssaySubmitResult {
    private final Long essayAnswerId;
    private final EvaluationView evaluation;
    private final List<AbilityChange> abilityChanges;
    private final String gradingStatus;
    private final boolean retryable;
    private final String message;

    public EssaySubmitResult(Long essayAnswerId, EvaluationView evaluation, List<AbilityChange> abilityChanges) {
        this.essayAnswerId = essayAnswerId; this.evaluation = evaluation; this.abilityChanges = abilityChanges;
        this.gradingStatus = "SUCCESS"; this.retryable = false; this.message = null;
    }

    public EssaySubmitResult(Long essayAnswerId, String gradingStatus, boolean retryable, String message) {
        this.essayAnswerId = essayAnswerId; this.evaluation = null; this.abilityChanges = java.util.Collections.emptyList();
        this.gradingStatus = gradingStatus; this.retryable = retryable; this.message = message;
    }

    public Long getEssayAnswerId() { return essayAnswerId; }
    public EvaluationView getEvaluation() { return evaluation; }
    public List<AbilityChange> getAbilityChanges() { return abilityChanges; }
    public String getGradingStatus() { return gradingStatus; }
    public boolean isRetryable() { return retryable; }
    public String getMessage() { return message; }
}
