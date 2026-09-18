package com.gkstudy.content.service;

import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import com.gkstudy.content.mapper.ContentStagingMapper;
import com.gkstudy.content.model.ContentStaging;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class QuestionStagingImportServiceTest {
    private QuestionMapper questionMapper;
    private ContentStagingMapper stagingMapper;
    private QuestionStagingImportService service;

    @BeforeEach
    void setUp() {
        questionMapper = mock(QuestionMapper.class);
        stagingMapper = mock(ContentStagingMapper.class);
        service = new QuestionStagingImportService(questionMapper, stagingMapper,
                new ContentQualityService(), new TransactionTemplate(new TestTransactionManager()));
        KnowledgePointRef knowledge = new KnowledgePointRef();
        knowledge.setId(8L); knowledge.setCode("GK-XC-01"); knowledge.setName("逻辑填空");
        when(questionMapper.findKnowledgePointByCode("GK-XC-01")).thenReturn(knowledge);
        when(questionMapper.insertQuestion(any(Question.class))).thenAnswer(invocation -> {
            invocation.<Question>getArgument(0).setId(601L);
            return 1;
        });
    }

    private ContentStaging staging(String trustLevel) {
        ContentStaging item = new ContentStaging();
        item.setId(1L);
        item.setSourceId(10L);
        item.setSourceUrl("https://gov.example.cn/sample.html");
        item.setTrustLevel(trustLevel);
        item.setExamType("GK");
        return item;
    }

    private QuestionCandidate fullCandidate() {
        QuestionCandidate candidate = new QuestionCandidate();
        candidate.setStem("下列关于基层治理的说法正确的是哪一项内容呢");
        candidate.getOptions().add(new Option("A", "治理资源下沉"));
        candidate.getOptions().add(new Option("B", "仅靠上级统筹"));
        candidate.getOptions().add(new Option("C", "居民不参与"));
        candidate.getOptions().add(new Option("D", "无需制度建设"));
        candidate.setAnswer("A");
        candidate.setAnalysis("基层治理要求推动治理资源和服务下沉，完善共建共治共享的社会治理制度。");
        candidate.setKnowledgeCode("GK-XC-01");
        return candidate;
    }

    @Test
    void saHighQualityCandidateAutoImports() {
        when(questionMapper.countByContentHash(any())).thenReturn(0);

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging("A"), Collections.singletonList(fullCandidate()));

        assertEquals(1, stats.imported);
        assertEquals(601L, stats.firstQuestionId);
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).insertQuestion(captor.capture());
        Question question = captor.getValue();
        assertEquals("SINGLE", question.getQuestionType());
        assertEquals("TRAINING", question.getUsageType());
        assertEquals("ACTIVE", question.getStatus());
        assertEquals("IMPORTED", question.getSourceType());
        assertEquals("A", question.getSourceLevel());
        assertEquals(100, question.getQualityScore().intValue());
        assertNotNull(question.getContentHash());
        assertEquals(64, question.getContentHash().length());
        verify(questionMapper, times(4)).insertOption(any(QuestionOption.class));
        verify(questionMapper).insertKnowledge(601L, 8L);
        verify(stagingMapper, never()).insert(any(ContentStaging.class));
    }

    @Test
    void contentHashCollisionCountsDuplicateWithoutInsert() {
        when(questionMapper.countByContentHash(any())).thenReturn(1);

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging("A"), Collections.singletonList(fullCandidate()));

        assertEquals(1, stats.duplicates);
        assertEquals(0, stats.imported);
        verify(questionMapper, never()).insertQuestion(any());
        verify(stagingMapper, never()).insert(any(ContentStaging.class));
    }

    @Test
    void invalidCandidateGeneratesNeedsReviewStagingWithQualityIssues() {
        QuestionCandidate broken = fullCandidate();
        broken.setAnswer(null);

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging("A"), Collections.singletonList(broken));

        assertEquals(1, stats.needsReview);
        assertEquals(0, stats.imported);
        verify(questionMapper, never()).insertQuestion(any());
        ArgumentCaptor<ContentStaging> captor = ArgumentCaptor.forClass(ContentStaging.class);
        verify(stagingMapper).insert(captor.capture());
        ContentStaging review = captor.getValue();
        assertEquals(ContentStaging.NEEDS_REVIEW, review.getStatus());
        assertEquals("https://gov.example.cn/sample.html#idx-1", review.getSourceUrl());
        assertEquals(10L, review.getSourceId());
        assertTrue(review.getQualityIssues().contains("答案缺失或非法"));
        assertNotNull(review.getQualityScore());
        assertNotNull(review.getQualityConfidence());
        assertTrue(review.getParsedText().contains("基层治理"));
        assertTrue(review.getFailReason().contains("质量门禁未通过"));
    }

    @Test
    void bHighConfidenceAutoImportsAndCountsSampled() {
        when(questionMapper.countByContentHash(any())).thenReturn(0);

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging("B"), Collections.singletonList(fullCandidate()));

        assertEquals(1, stats.imported);
        assertEquals(1, stats.sampled);
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).insertQuestion(captor.capture());
        assertEquals("B", captor.getValue().getSourceLevel());
    }

    @Test
    void bLowConfidenceGoesNeedsReview() {
        QuestionCandidate noisy = fullCandidate();
        noisy.setAnalysis(null);
        noisy.setStem("下列关于<div>基层治理</div>的说法正确的是哪一项内容呢");

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging("B"), Collections.singletonList(noisy));

        assertEquals(1, stats.needsReview);
        assertEquals(0, stats.imported);
        verify(questionMapper, never()).insertQuestion(any());
        verify(stagingMapper).insert(any(ContentStaging.class));
    }

    @Test
    void cdHighConfidenceImportsWithSourceLevel() {
        when(questionMapper.countByContentHash(any())).thenReturn(0);

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging("C"), Collections.singletonList(fullCandidate()));

        assertEquals(1, stats.imported);
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).insertQuestion(captor.capture());
        assertEquals("C", captor.getValue().getSourceLevel());
        assertEquals("TRAINING", captor.getValue().getUsageType());
    }

    @Test
    void cdLowConfidenceGoesNeedsReview() {
        QuestionCandidate missingAnalysis = fullCandidate();
        missingAnalysis.setAnalysis(null);

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging("D"), Collections.singletonList(missingAnalysis));

        assertEquals(1, stats.needsReview);
        assertEquals(0, stats.imported);
        verify(questionMapper, never()).insertQuestion(any());
        ArgumentCaptor<ContentStaging> captor = ArgumentCaptor.forClass(ContentStaging.class);
        verify(stagingMapper).insert(captor.capture());
        assertEquals(ContentStaging.NEEDS_REVIEW, captor.getValue().getStatus());
    }

    @Test
    void nullTrustLevelNeverAutoImports() {
        when(questionMapper.countByContentHash(any())).thenReturn(0);

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging(null), Collections.singletonList(fullCandidate()));

        assertEquals(0, stats.imported);
        assertEquals(1, stats.needsReview);
        verify(questionMapper, never()).insertQuestion(any());
    }

    @Test
    void unknownKnowledgeCodeGoesNeedsReview() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setKnowledgeCode("NOT-EXIST");

        QuestionStagingImportService.ImportStats stats =
                service.importCandidates(staging("A"), Collections.singletonList(candidate));

        assertEquals(1, stats.needsReview);
        verify(questionMapper, never()).insertQuestion(any());
        ArgumentCaptor<ContentStaging> captor = ArgumentCaptor.forClass(ContentStaging.class);
        verify(stagingMapper).insert(captor.capture());
        assertTrue(captor.getValue().getQualityIssues().contains("知识点无法分类"));
    }

    @Test
    void importConfirmedInsertsQuestionWithStagingTrustLevel() {
        when(questionMapper.countByContentHash(any())).thenReturn(0);

        Long id = service.importConfirmed(staging("A"), fullCandidate());

        assertEquals(601L, id);
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).insertQuestion(captor.capture());
        assertEquals("A", captor.getValue().getSourceLevel());
        assertEquals("ACTIVE", captor.getValue().getStatus());
        verify(questionMapper, times(4)).insertOption(any(QuestionOption.class));
    }

    @Test
    void importConfirmedRejectsUnknownKnowledgeCode() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setKnowledgeCode("NOT-EXIST");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.importConfirmed(staging("A"), candidate));
        assertTrue(error.getMessage().contains("知识点不存在"));
        verify(questionMapper, never()).insertQuestion(any());
    }

    @Test
    void importConfirmedRejectsDuplicate() {
        when(questionMapper.countByContentHash(any())).thenReturn(1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.importConfirmed(staging("A"), fullCandidate()));
        assertEquals("题目重复", error.getMessage());
        verify(questionMapper, never()).insertQuestion(any());
    }

    @Test
    void importConfirmedRejectsShortStem() {
        QuestionCandidate candidate = fullCandidate();
        candidate.setStem("太短");

        assertThrows(IllegalArgumentException.class, () -> service.importConfirmed(staging("A"), candidate));
        verify(questionMapper, never()).insertQuestion(any());
    }

    @Test
    void contentHashMatchesManualImportRule() {
        QuestionCandidate candidate = fullCandidate();
        String hash = QuestionStagingImportService.contentHash(candidate);
        // sha256(stem|A:text|B:text|C:text|D:text|answer)
        assertEquals(64, hash.length());
        QuestionCandidate same = fullCandidate();
        assertEquals(hash, QuestionStagingImportService.contentHash(same));
        same.setAnswer("D");
        assertNotEquals(hash, QuestionStagingImportService.contentHash(same));
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
