package com.gkstudy.essay.dto;

import java.util.List;

public class EssayTaskView {
    private final List<PlanEssayItem> planItems;
    private final List<EssayQuestionSummary> questions;

    public EssayTaskView(List<PlanEssayItem> planItems, List<EssayQuestionSummary> questions) {
        this.planItems = planItems; this.questions = questions;
    }

    public List<PlanEssayItem> getPlanItems() { return planItems; }
    public List<EssayQuestionSummary> getQuestions() { return questions; }
}
