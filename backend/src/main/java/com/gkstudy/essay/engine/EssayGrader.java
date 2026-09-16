package com.gkstudy.essay.engine;

import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.model.EssayQuestion;

public interface EssayGrader {
    String evaluator();
    default String provider() { return evaluator().startsWith("LOCAL_RULE") ? "LOCAL_RULE" : "OPENAI_COMPATIBLE"; }
    default String model() { return null; }
    default String promptVersion() { return evaluator(); }

    EssayEvaluationResult grade(EssayQuestion question, String answerText, long durationMs);
}
