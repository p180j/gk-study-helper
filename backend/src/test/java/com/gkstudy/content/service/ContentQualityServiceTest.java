package com.gkstudy.content.service;

import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import com.gkstudy.content.service.ContentQualityService.QualityResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentQualityServiceTest {
    private final ContentQualityService service = new ContentQualityService();

    private QuestionCandidate fullCandidate() {
        QuestionCandidate candidate = new QuestionCandidate();
        candidate.setStem("下列关于基层治理的说法正确的是哪一项内容呢");
        candidate.getOptions().add(new Option("A", "治理资源下沉"));
        candidate.getOptions().add(new Option("B", "仅靠上级统筹"));
        candidate.getOptions().add(new Option("C", "居民不参与"));
        candidate.getOptions().add(new Option("D", "无需制度建设"));
        candidate.setAnswer("A");
        candidate.setAnalysis("基层治理要求推动治理资源和服务下沉，完善共建共治共享的社会治理制度。");
        return candidate;
    }

    @Test
    void fullCandidatePassesWithHighScore() {
        QualityResult result = service.evaluate(fullCandidate(), "S", true);
        assertTrue(result.passes());
        assertEquals(100, result.score);
        assertEquals(100, result.confidence);
        assertTrue(result.issues.isEmpty());
    }

    @Test
    void shortStemIsFatal() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setStem("太短的题干");
        QualityResult result = service.evaluate(candidate, "A", true);
        assertFalse(result.passes());
        assertTrue(result.fatal);
        assertTrue(result.issues.contains("题干不完整"));
    }

    @Test
    void insufficientOptionsIsFatal() {
        QuestionCandidate candidate = fullCandidate();
        candidate.getOptions().remove(3);
        candidate.getOptions().remove(2);
        QualityResult result = service.evaluate(candidate, "A", true);
        assertFalse(result.passes());
        assertTrue(result.fatal);
        assertTrue(result.issues.contains("选项数量不足"));
    }

    @Test
    void emptyOptionTextCountsAsMissing() {
        QuestionCandidate candidate = fullCandidate();
        candidate.getOptions().get(3).setText("  ");
        QualityResult result = service.evaluate(candidate, "A", true);
        assertTrue(result.issues.contains("选项数量不足"));
    }

    @Test
    void missingAnswerIsFatal() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setAnswer(null);
        QualityResult result = service.evaluate(candidate, "A", true);
        assertFalse(result.passes());
        assertTrue(result.fatal);
        assertTrue(result.issues.contains("答案缺失或非法"));
    }

    @Test
    void answerNotInOptionsIsInvalid() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setAnswer("E");
        QualityResult result = service.evaluate(candidate, "A", true);
        assertTrue(result.issues.contains("答案缺失或非法"));
        assertTrue(result.fatal);
    }

    @Test
    void missingAnalysisMinorDeduction() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setAnalysis(null);
        QualityResult result = service.evaluate(candidate, "A", true);
        assertTrue(result.issues.contains("缺少解析"));
        assertFalse(result.fatal);
        assertEquals(90, result.score);
        assertTrue(result.passes());
    }

    @Test
    void shortAnalysisWithoutAnswerKeyFlagsConflict() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setAnalysis("解析太短了");
        QualityResult result = service.evaluate(candidate, "A", true);
        assertTrue(result.issues.contains("答案与解析可能冲突"));
        assertFalse(result.fatal);
        assertTrue(result.passes());
    }

    @Test
    void htmlNoiseInStemPenalizes() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setStem("下列关于<div>基层治理</div>的说法正确的是哪一项内容呢");
        QualityResult result = service.evaluate(candidate, "A", true);
        assertTrue(result.issues.contains("内容含乱码或HTML噪声"));
        assertEquals(70, result.score);
        assertTrue(result.passes());
    }

    @Test
    void replacementCharDetectedAsGarbled() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setStem("下列关于基层治理的说法正确的＂是哪一项内\uFFFD容呢");
        QualityResult result = service.evaluate(candidate, "A", true);
        assertTrue(result.issues.contains("内容含乱码或HTML噪声"));
    }

    @Test
    void unknownKnowledgeIsFatal() {
        QualityResult result = service.evaluate(fullCandidate(), "A", false);
        assertFalse(result.passes());
        assertTrue(result.fatal);
        assertTrue(result.issues.contains("知识点无法分类"));
    }

    @Test
    void trustLevelAdjustsConfidence() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setAnalysis(null); // -5，便于观察修正差值
        assertEquals(95, service.evaluate(candidate, "S", true).confidence);
        assertEquals(95, service.evaluate(candidate, "A", true).confidence);
        assertEquals(85, service.evaluate(candidate, "B", true).confidence);
        assertEquals(70, service.evaluate(candidate, "D", true).confidence);
    }

    @Test
    void issuesTextJoinsWithChineseSemicolon() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setAnswer(null);
        candidate.setAnalysis(null);
        QualityResult result = service.evaluate(candidate, "A", false);
        assertTrue(result.issuesText().contains("；"));
        assertTrue(result.issuesText().contains("答案缺失或非法"));
    }
}
