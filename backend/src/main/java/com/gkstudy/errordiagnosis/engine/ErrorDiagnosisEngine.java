package com.gkstudy.errordiagnosis.engine;

import com.gkstudy.practice.model.AnswerRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ErrorDiagnosisEngine {
    public Candidate evaluate(AnswerRecord current, List<AnswerRecord> recentRecords) {
        if (Boolean.TRUE.equals(current.getCorrect())) return null;
        String cause = causeOf(current);
        if (cause == null) return null;
        int occurrences = 0;
        for (AnswerRecord record : recentRecords) {
            if (!Boolean.TRUE.equals(record.getCorrect()) && cause.equals(causeOf(record))) occurrences++;
        }
        occurrences = Math.max(occurrences, 1);
        int base = "UNKNOWN".equals(current.getErrorType()) ? 30 : 40;
        BigDecimal confidence = BigDecimal.valueOf(Math.min(85, base + (occurrences - 1) * 15L));
        return new Candidate(cause, confidence, occurrences);
    }

    private String causeOf(AnswerRecord record) {
        String errorType = record.getErrorType();
        if ("NOT_KNOW".equals(errorType)) return "知识点未掌握";
        if ("FORMULA".equals(errorType)) return "公式使用错误";
        if ("CONDITION".equals(errorType)) return "条件理解偏差";
        if ("CALCULATION".equals(errorType)) return "计算错误";
        if ("TIMEOUT".equals(errorType)) return "时间分配不足";
        if ("CARELESS".equals(errorType)) return "粗心失误";
        if (!"UNKNOWN".equals(errorType)) return null;
        if (record.getStandardTimeSecondsSnapshot() != null && record.getDurationMs() != null
                && record.getDurationMs() > record.getStandardTimeSecondsSnapshot() * 1000L) return "时间分配不足";
        if ("SURE".equals(record.getConfidenceType())) return "概念存在稳定误解";
        if ("GUESS".equals(record.getConfidenceType())) return "知识点未掌握";
        if ("HESITANT".equals(record.getConfidenceType())) return "知识掌握不稳定";
        return null;
    }

    public static class Candidate {
        private final String suspectedCause;
        private final BigDecimal confidence;
        private final int occurrenceCount;

        public Candidate(String suspectedCause, BigDecimal confidence, int occurrenceCount) {
            this.suspectedCause = suspectedCause; this.confidence = confidence; this.occurrenceCount = occurrenceCount;
        }
        public String getSuspectedCause() { return suspectedCause; }
        public BigDecimal getConfidence() { return confidence; }
        public int getOccurrenceCount() { return occurrenceCount; }
    }
}
