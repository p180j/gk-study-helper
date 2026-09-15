package com.gkstudy.question;

import com.gkstudy.question.dto.ImportResult;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.service.QuestionImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuestionImportServiceTest {
    private QuestionMapper questionMapper;
    private QuestionImportService importService;

    @BeforeEach
    void setUp() {
        questionMapper = mock(QuestionMapper.class);
        KnowledgePointRef knowledge = new KnowledgePointRef(); knowledge.setId(8L); knowledge.setCode("GROWTH_RATE"); knowledge.setName("增长率");
        when(questionMapper.findKnowledgePointByCode("GROWTH_RATE")).thenReturn(knowledge);
        doAnswer(invocation -> { Question question = invocation.getArgument(0); question.setId(101L); return 1; }).when(questionMapper).insertQuestion(any());
        importService = new QuestionImportService(questionMapper, new TransactionTemplate(new TestTransactionManager()));
    }

    @Test
    void importsValidRow() throws Exception {
        ImportResult result = importService.importCsv(file(header() + validRow("2024年某地区增长率为多少？")));
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(questionMapper).insertQuestion(any());
        verify(questionMapper, times(4)).insertOption(any());
        verify(questionMapper).insertKnowledge(101L, 8L);
    }

    @Test
    void isolatesInvalidRow() throws Exception {
        String csv = header() + validRow("第一道有效题") + "错误题,A,B,C,D,B,解析,NOT_EXISTS,50,60,MANUAL,2024,国考,测试,ACTIVE,TRAINING,SINGLE\n" + validRow("第三道有效题");
        ImportResult result = importService.importCsv(file(csv));
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(3, result.getFailures().get(0).getRow());
        verify(questionMapper, times(2)).insertQuestion(any());
    }

    @Test
    void reportsDuplicateWithoutStoppingFollowingRows() throws Exception {
        when(questionMapper.countByContentHash(any())).thenReturn(1, 0);
        String csv = header() + validRow("重复题") + validRow("非重复题");
        ImportResult result = importService.importCsv(file(csv));
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    private MockMultipartFile file(String content) { return new MockMultipartFile("file", "questions.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8)); }
    private String validRow(String stem) { return stem + ",10%,20%,30%,40%,B,增长率解析,GROWTH_RATE,55,60,HISTORICAL,2024,国考,真题,ACTIVE,TRAINING,SINGLE\n"; }
    private String header() { return "stem,optionA,optionB,optionC,optionD,answer,analysis,knowledgeCode,difficulty,standardTimeSeconds,sourceType,sourceYear,sourceExam,sourceName,status,usageType,questionType\n"; }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
