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
    private final TaskProgress taskProgress;

    public AnswerResult(Long answerRecordId, boolean correct, String correctAnswer, String analysis,
                        List<AbilityChange> abilityChanges, ErrorDiagnosis errorDiagnosis) {
        this(answerRecordId, correct, correctAnswer, analysis, abilityChanges, errorDiagnosis, null);
    }

    public AnswerResult(Long answerRecordId, boolean correct, String correctAnswer, String analysis,
                        List<AbilityChange> abilityChanges, ErrorDiagnosis errorDiagnosis, TaskProgress taskProgress) {
        this.answerRecordId = answerRecordId;
        this.correct = correct;
        this.correctAnswer = correctAnswer;
        this.analysis = analysis;
        this.abilityChanges = abilityChanges;
        this.errorDiagnosis = errorDiagnosis;
        this.taskProgress = taskProgress;
    }

    public Long getAnswerRecordId() { return answerRecordId; }
    public boolean isCorrect() { return correct; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getAnalysis() { return analysis; }
    public List<AbilityChange> getAbilityChanges() { return abilityChanges; }
    public ErrorDiagnosis getErrorDiagnosis() { return errorDiagnosis; }
    public TaskProgress getTaskProgress() { return taskProgress; }

    /** 计划任务进度：目标题量、已完成题量、是否达标、是否库存不足、是否重复提交。 */
    public static class TaskProgress {
        private final Integer targetQuestionCount;
        private final Integer completedQuestionCount;
        private final boolean completed;
        private final boolean insufficientStock;
        private final boolean duplicate;

        public TaskProgress(Integer targetQuestionCount, Integer completedQuestionCount, boolean completed,
                            boolean insufficientStock, boolean duplicate) {
            this.targetQuestionCount = targetQuestionCount;
            this.completedQuestionCount = completedQuestionCount;
            this.completed = completed;
            this.insufficientStock = insufficientStock;
            this.duplicate = duplicate;
        }

        public Integer getTargetQuestionCount() { return targetQuestionCount; }
        public Integer getCompletedQuestionCount() { return completedQuestionCount; }
        public boolean isCompleted() { return completed; }
        public boolean isInsufficientStock() { return insufficientStock; }
        public boolean isDuplicate() { return duplicate; }
    }
}
