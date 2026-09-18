package com.gkstudy.content.service;

import com.gkstudy.content.mapper.ContentSourceMapper;
import com.gkstudy.content.mapper.ContentStagingMapper;
import com.gkstudy.content.model.ContentSource;
import com.gkstudy.content.model.ContentStaging;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ContentUploadServiceTest {
    private ContentSourceMapper sourceMapper;
    private ContentStagingMapper stagingMapper;
    private QuestionMapper questionMapper;
    private ContentUploadService service;

    @TempDir
    Path tempDir;

    private static final String HEADER = "questionType,stem,answer,optionA,optionB,optionC,optionD,analysis,knowledgeCode,sourceType,usageType,difficultyExpected,standardTimeSeconds\n";
    private static final String VALID_ROW = "SINGLE,下列关于基层治理的说法正确的是哪一项内容呢,A,治理资源下沉,仅靠上级统筹,居民不参与,无需制度建设,基层治理要求推动治理资源和服务下沉、完善共建共治共享的社会治理制度。,GK-XC-01,,,,50,60\n";

    @BeforeEach
    void setUp() {
        sourceMapper = mock(ContentSourceMapper.class);
        stagingMapper = mock(ContentStagingMapper.class);
        questionMapper = mock(QuestionMapper.class);
        QuestionStagingImportService importer = new QuestionStagingImportService(questionMapper, stagingMapper,
                new ContentQualityService(), new TransactionTemplate(new TestTransactionManager()));
        service = new ContentUploadService(sourceMapper, stagingMapper, importer, tempDir.toString());
        KnowledgePointRef knowledge = new KnowledgePointRef();
        knowledge.setId(8L); knowledge.setCode("GK-XC-01"); knowledge.setName("逻辑填空");
        when(questionMapper.findKnowledgePointByCode("GK-XC-01")).thenReturn(knowledge);
        when(questionMapper.insertQuestion(any(Question.class))).thenAnswer(invocation -> {
            invocation.<Question>getArgument(0).setId(501L);
            return 1;
        });
        when(sourceMapper.findAll()).thenReturn(Collections.emptyList());
        when(sourceMapper.insert(any(ContentSource.class))).thenAnswer(invocation -> {
            invocation.<ContentSource>getArgument(0).setId(20L);
            return 1;
        });
        when(stagingMapper.insert(any(ContentStaging.class))).thenAnswer(invocation -> {
            invocation.<ContentStaging>getArgument(0).setId(30L);
            return 1;
        });
    }

    @Test
    void uploadCsvImportsNewRowAndSkipsDuplicateContentHash() throws Exception {
        when(stagingMapper.countByUrl(any())).thenReturn(0);
        when(questionMapper.countByContentHash(any())).thenReturn(0, 1);
        String csv = HEADER + VALID_ROW + VALID_ROW;
        MockMultipartFile file = new MockMultipartFile("file", "questions.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ContentUploadService.UploadResult result = service.upload(file, "S", "官方题库");

        assertEquals(2, result.total);
        assertEquals(1, result.imported);
        assertEquals(1, result.duplicates);
        assertEquals(30L, result.stagingId);
        verify(questionMapper, times(1)).insertQuestion(any(Question.class));
        // 文件级暂存：登记来源 + 创建 DOWNLOADED 记录 + 导入后标记 IMPORTED
        ArgumentCaptor<ContentStaging> captor = ArgumentCaptor.forClass(ContentStaging.class);
        verify(stagingMapper).insert(captor.capture());
        assertEquals("upload://", captor.getValue().getSourceUrl().substring(0, 9));
        assertEquals(ContentStaging.DOWNLOADED, captor.getValue().getStatus());
        assertEquals("ATTACHMENT", captor.getValue().getSourceType());
        assertEquals("S", captor.getValue().getTrustLevel());
        assertNotNull(captor.getValue().getFileHash());
        verify(stagingMapper).markImported(30L, "QUESTION", 501L);
        verify(stagingMapper).updateReviewNote(eq(30L), contains("文件导入：共 2/入库 1/重复 1"));
    }

    @Test
    void uploadCsvWithLowQualityRowGoesNeedsReview() throws Exception {
        when(stagingMapper.countByUrl(any())).thenReturn(0);
        when(questionMapper.countByContentHash(any())).thenReturn(0);
        String badRow = "SINGLE,题干太短,A,选项甲,选项乙,选项丙,选项丁,解析内容太短,GK-XC-01,,,,50,60\n";
        MockMultipartFile file = new MockMultipartFile("file", "questions.csv", "text/csv",
                (HEADER + VALID_ROW + badRow).getBytes(StandardCharsets.UTF_8));

        ContentUploadService.UploadResult result = service.upload(file, "S", null);

        assertEquals(1, result.imported);
        assertEquals(1, result.needsReview);
        // 文件级暂存 + 质量不通过候选的逐题人工检查记录
        verify(stagingMapper, times(2)).insert(any(ContentStaging.class));
        ArgumentCaptor<ContentStaging> captor = ArgumentCaptor.forClass(ContentStaging.class);
        verify(stagingMapper, times(2)).insert(captor.capture());
        assertEquals(ContentStaging.NEEDS_REVIEW, captor.getAllValues().get(1).getStatus());
        assertTrue(captor.getAllValues().get(1).getQualityIssues().contains("题干不完整"));
        verify(stagingMapper).markImported(30L, "QUESTION", 501L);
    }

    @Test
    void uploadCsvWithUnparsableRowReportsFailure() throws Exception {
        when(stagingMapper.countByUrl(any())).thenReturn(0);
        when(questionMapper.countByContentHash(any())).thenReturn(0);
        String badRow = "SINGLE,题干太短,A,只有一个选项,,,,解析内容,GK-XC-01,,,,50,60\n";
        MockMultipartFile file = new MockMultipartFile("file", "questions.csv", "text/csv",
                (HEADER + badRow).getBytes(StandardCharsets.UTF_8));

        ContentUploadService.UploadResult result = service.upload(file, "S", null);

        assertEquals(0, result.total);
        assertEquals(1, result.failed);
        assertFalse(result.errors.isEmpty());
        verify(questionMapper, never()).insertQuestion(any());
        // 无自动入库时文件级暂存仍标记 IMPORTED 完成（importedType 为空）
        verify(stagingMapper).markImported(30L, null, null);
    }

    @Test
    void uploadRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "questions.txt", "text/plain", new byte[]{'a'});

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.upload(file, "S", null));
        assertEquals("仅支持 CSV / XLSX / XLS 文件", error.getMessage());
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "questions.csv", "text/csv", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service.upload(file, "S", null));
    }

    @Test
    void uploadRejectsAlreadyProcessedFile() throws Exception {
        when(stagingMapper.countByUrl(any())).thenReturn(1);
        MockMultipartFile file = new MockMultipartFile("file", "questions.csv", "text/csv",
                (HEADER + VALID_ROW).getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.upload(file, "S", null));
        assertEquals("该文件已上传处理过，请勿重复上传", error.getMessage());
        verify(stagingMapper, never()).insert(any(ContentStaging.class));
    }

    @Test
    void uploadRejectsInvalidHeader() {
        String csv = "stem,answer\n题干,A\n";
        MockMultipartFile file = new MockMultipartFile("file", "questions.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.upload(file, "S", null));
        assertTrue(error.getMessage().contains("表头不符合模板"));
    }

    @Test
    void uploadXlsxParsesRowsAndImports() throws Exception {
        when(stagingMapper.countByUrl(any())).thenReturn(0);
        when(questionMapper.countByContentHash(any())).thenReturn(0);
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("题目");
            Row header = sheet.createRow(0);
            String[] headers = {"questionType", "stem", "answer", "optionA", "optionB", "optionC", "optionD",
                    "analysis", "knowledgeCode", "sourceType", "usageType", "difficultyExpected", "standardTimeSeconds"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            String[] values = {"SINGLE", "下列关于基层治理的说法正确的是哪一项内容呢", "A", "治理资源下沉", "仅靠上级统筹",
                    "居民不参与", "无需制度建设", "基层治理要求推动治理资源和服务下沉、完善共建共治共享的社会治理制度。",
                    "GK-XC-01", "HISTORICAL", "TRAINING", "50", "75"};
            Row row = sheet.createRow(1);
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            bytes = out.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile("file", "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        ContentUploadService.UploadResult result = service.upload(file, "A", "Excel 题库");

        assertEquals(1, result.total);
        assertEquals(1, result.imported);
        assertEquals(0, result.duplicates);
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).insertQuestion(captor.capture());
        assertEquals("A", captor.getValue().getSourceLevel());
        assertEquals("HISTORICAL", captor.getValue().getSourceType());
        assertEquals("TRAINING", captor.getValue().getUsageType());
        assertEquals(75, captor.getValue().getStandardTimeSeconds().intValue());
        assertEquals(50, captor.getValue().getDifficultyExpected().intValue());
        verify(stagingMapper).markImported(30L, "QUESTION", 501L);
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
