package com.gkstudy.essay.dto;

import com.gkstudy.essay.model.EssayAnswer;
import com.gkstudy.essay.model.EssayQuestion;

public class AdminEssayAnswerView {
    private final EssayAnswer answer;
    private final EssayQuestion question;
    private final EvaluationView evaluation;

    public AdminEssayAnswerView(EssayAnswer answer, EssayQuestion question, EvaluationView evaluation) {
        this.answer = answer; this.question = question; this.evaluation = evaluation;
    }

    public EssayAnswer getAnswer() { return answer; }
    public EssayQuestion getQuestion() { return question; }
    public EvaluationView getEvaluation() { return evaluation; }
}
