package com.gkstudy.errordiagnosis.dto;

import javax.validation.constraints.NotNull;

public class DiagnosisDecisionRequest {
    @NotNull
    private Boolean confirmed;

    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
}
