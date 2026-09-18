package com.gkstudy.content.service;

import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTextExtractorTest {
    private final QuestionTextExtractor extractor = new QuestionTextExtractor();

    @Test
    void extractsTypicalFormattedQuestion() {
        String text = "1. 下列关于基层治理的说法正确的是\n"
                + "A. 治理资源下沉\nB. 仅靠上级统筹\nC. 居民不参与\nD. 无需制度建设\n"
                + "答案：A\n解析：基层治理要求推动治理资源和服务下沉，完善制度建设。";

        List<QuestionCandidate> candidates = extractor.extract(text);

        assertEquals(1, candidates.size());
        QuestionCandidate candidate = candidates.get(0);
        assertEquals("下列关于基层治理的说法正确的是", candidate.getStem());
        assertEquals(4, candidate.getOptions().size());
        assertEquals("A", candidate.getOptions().get(0).getKey());
        assertEquals("治理资源下沉", candidate.getOptions().get(0).getText());
        assertEquals("无需制度建设", candidate.getOptions().get(3).getText());
        assertEquals("A", candidate.getAnswer());
        assertEquals("基层治理要求推动治理资源和服务下沉，完善制度建设。", candidate.getAnalysis());
    }

    @Test
    void extractsMultipleNumberedQuestions() {
        String text = "1. 第一道题的题干内容是什么\nA. 甲\nB. 乙\nC. 丙\nD. 丁\n答案：B\n"
                + "2. 第二道题的题干内容是什么\nA. 东\nB. 南\nC. 西\nD. 北\n答案：C\n解析：第二题的解析内容说明。";

        List<QuestionCandidate> candidates = extractor.extract(text);

        assertEquals(2, candidates.size());
        assertEquals("B", candidates.get(0).getAnswer());
        assertNull(candidates.get(0).getAnalysis());
        assertEquals("C", candidates.get(1).getAnswer());
        assertEquals("第二题的解析内容说明。", candidates.get(1).getAnalysis());
    }

    @Test
    void candidateWithoutAnswerStillReturned() {
        String text = "1. 下列关于基层治理的说法正确的是\nA. 治理资源下沉\nB. 仅靠上级统筹\nC. 居民不参与\nD. 无需制度建设";

        List<QuestionCandidate> candidates = extractor.extract(text);

        assertEquals(1, candidates.size());
        assertNull(candidates.get(0).getAnswer());
        assertEquals(4, candidates.get(0).getOptions().size());
    }

    @Test
    void plainTextWithoutStructureReturnsEmpty() {
        String text = "基层治理是国家治理的基石，要推动治理资源和服务下沉，提升基层服务能力，完善共建共治共享的制度。";
        assertTrue(extractor.extract(text).isEmpty());
    }

    @Test
    void nullOrBlankTextReturnsEmpty() {
        assertTrue(extractor.extract(null).isEmpty());
        assertTrue(extractor.extract("   \n  ").isEmpty());
    }

    @Test
    void singleLineWithQuestionTypePrefix() {
        String text = "单选题：下列关于法治政府建设的说法正确的是 A．依法行政 B．人治为主 C．随意执法 D．选择性守法 答案：A 解析：法治政府要求依法全面履行职能。";

        List<QuestionCandidate> candidates = extractor.extract(text);

        assertEquals(1, candidates.size());
        QuestionCandidate candidate = candidates.get(0);
        assertEquals("下列关于法治政府建设的说法正确的是", candidate.getStem());
        assertEquals("A", candidate.getAnswer());
        assertEquals(4, candidate.getOptions().size());
        assertEquals("依法行政", candidate.getOptions().get(0).getText());
        assertEquals("法治政府要求依法全面履行职能。", candidate.getAnalysis());
    }

    @Test
    void answerMentionedInsideStemDoesNotConfuseExtraction() {
        String text = "1. 某考试参考答案是D选项之外的表述，下列说法正确的是\nA. 甲\nB. 乙\nC. 丙\nD. 丁\n答案：A";

        List<QuestionCandidate> candidates = extractor.extract(text);

        assertEquals(1, candidates.size());
        assertEquals("A", candidates.get(0).getAnswer());
    }

    @Test
    void optionTextWithChineseEnumerationMark() {
        String text = "3、下列关于乡村振兴的说法正确的是\nA、产业兴旺\nB、环境脏乱\nC、乡风不文明\nD、治理无效\n正确答案：A";

        List<QuestionCandidate> candidates = extractor.extract(text);

        assertEquals(1, candidates.size());
        assertEquals("A", candidates.get(0).getAnswer());
        assertEquals("产业兴旺", candidates.get(0).getOptions().get(0).getText());
    }

    @Test
    void extractsBracketedAnswerAndAnalysisUsedByPublicQuestionPages() {
        String text = "1. 下列关于基层治理的说法正确的是 A. 治理资源下沉 B. 仅靠上级统筹 C. 居民不参与 D. 无需制度建设 【答案】A 【成公解析】基层治理要求推动治理资源和服务下沉，完善制度建设。";

        List<QuestionCandidate> candidates = extractor.extract(text);

        assertEquals(1, candidates.size());
        assertEquals("A", candidates.get(0).getAnswer());
        assertEquals("基层治理要求推动治理资源和服务下沉，完善制度建设。", candidates.get(0).getAnalysis());
    }
}
