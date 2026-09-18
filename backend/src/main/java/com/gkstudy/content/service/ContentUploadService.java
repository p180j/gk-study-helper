package com.gkstudy.content.service;

import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import com.gkstudy.content.mapper.ContentSourceMapper;
import com.gkstudy.content.mapper.ContentStagingMapper;
import com.gkstudy.content.model.ContentSource;
import com.gkstudy.content.model.ContentStaging;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 题目文件批量上传：保存原始文件 → 建 file 级暂存 → CSV/XLSX 逐行转候选 → 质量门禁分流入库。
 * 上传来源自动登记为 content_source（upload:// 前缀，不参与页面采集）。
 */
@Service
public class ContentUploadService {
    private static final List<String> HEADERS = Arrays.asList("questionType", "stem", "answer", "optionA", "optionB",
            "optionC", "optionD", "analysis", "knowledgeCode", "sourceType", "usageType", "difficultyExpected",
            "standardTimeSeconds");
    private static final int MAX_ERRORS = 10;

    private final ContentSourceMapper sourceMapper;
    private final ContentStagingMapper stagingMapper;
    private final QuestionStagingImportService questionImporter;
    private final String storageDir;

    public ContentUploadService(ContentSourceMapper sourceMapper, ContentStagingMapper stagingMapper,
                                QuestionStagingImportService questionImporter,
                                @Value("${content.storage-dir:./data/content}") String storageDir) {
        this.sourceMapper = sourceMapper; this.stagingMapper = stagingMapper;
        this.questionImporter = questionImporter; this.storageDir = storageDir;
    }

    public static class UploadResult {
        public Long stagingId;
        public int total;
        public int imported;
        public int duplicates;
        public int needsReview;
        public int failed;
        public List<String> errors = new ArrayList<>();
    }

    public UploadResult upload(MultipartFile file, String trustLevel, String sourceName) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("上传文件不能为空");
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".csv") && !lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new IllegalArgumentException("仅支持 CSV / XLSX / XLS 文件");
        }
        byte[] bytes = file.getBytes();
        String fileHash = sha256(bytes);
        String extension = lower.endsWith(".csv") ? ".csv" : (lower.endsWith(".xls") ? ".xls" : ".xlsx");
        String mimeType = lower.endsWith(".csv") ? "text/csv"
                : (lower.endsWith(".xls") ? "application/vnd.ms-excel"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String uploadSourceName = sourceName == null || sourceName.trim().isEmpty() ? fileName : sourceName.trim();
        ContentSource source = ensureUploadSource(fileHash, uploadSourceName, trustLevel);
        String sourceUrl = "upload://" + fileHash;
        if (stagingMapper.countByUrl(sourceUrl) > 0) {
            throw new IllegalArgumentException("该文件已上传处理过，请勿重复上传");
        }
        Path path = Paths.get(storageDir, fileHash.substring(0, 2), fileHash + extension);
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);

        ContentStaging staging = new ContentStaging();
        staging.setSourceId(source.getId());
        staging.setSourceUrl(sourceUrl);
        staging.setSiteName(uploadSourceName);
        staging.setTitle(uploadSourceName);
        staging.setSourceType("ATTACHMENT");
        staging.setTrustLevel(trustLevel);
        staging.setMimeType(mimeType);
        staging.setFileSize((long) bytes.length);
        staging.setFileHash(fileHash);
        staging.setFilePath(path.toString());
        staging.setOriginalFileName(fileName);
        staging.setStatus(ContentStaging.DOWNLOADED);
        stagingMapper.insert(staging);

        UploadResult result = new UploadResult();
        result.stagingId = staging.getId();
        List<QuestionCandidate> candidates = new ArrayList<>();
        List<String> rowErrors = new ArrayList<>();
        List<Map<String, String>> rows = lower.endsWith(".csv") ? parseCsv(bytes) : parseExcel(bytes, rowErrors);
        int rowNumber = 1;
        for (Map<String, String> row : rows) {
            rowNumber++;
            try {
                candidates.add(toCandidate(row));
            } catch (Exception e) {
                result.failed++;
                rowErrors.add("第 " + rowNumber + " 行解析失败：" + rootMessage(e));
            }
        }
        QuestionStagingImportService.ImportStats stats = questionImporter.importCandidates(staging, candidates);
        result.total = candidates.size();
        result.imported = stats.imported;
        result.duplicates = stats.duplicates;
        result.needsReview = stats.needsReview;
        result.failed += stats.failed;
        List<String> allErrors = new ArrayList<>(rowErrors);
        allErrors.addAll(stats.errors);
        result.errors = allErrors.size() > MAX_ERRORS ? new ArrayList<>(allErrors.subList(0, MAX_ERRORS)) : allErrors;

        if (stats.imported > 0) {
            stagingMapper.markImported(staging.getId(), "QUESTION", stats.firstQuestionId);
        } else {
            stagingMapper.markImported(staging.getId(), null, null);
        }
        stagingMapper.updateReviewNote(staging.getId(), "文件导入：共 " + result.total + "/入库 " + result.imported
                + "/重复 " + result.duplicates + "/异常 " + result.needsReview + "/失败 " + result.failed);
        return result;
    }

    /** 上传来源：同文件复用（base_url=upload://+hash前16），否则登记新来源 */
    private ContentSource ensureUploadSource(String fileHash, String sourceName, String trustLevel) {
        String baseUrl = "upload://" + fileHash.substring(0, 16);
        for (ContentSource existing : sourceMapper.findAll()) {
            if (baseUrl.equals(existing.getBaseUrl())) {
                existing.setTrustLevel(trustLevel);
                sourceMapper.update(existing);
                return existing;
            }
        }
        ContentSource source = new ContentSource();
        source.setName(sourceName);
        source.setBaseUrl(baseUrl);
        source.setSourceType("OTHER");
        source.setExamType("GK");
        source.setTrustLevel(trustLevel);
        source.setEnabled(false);
        source.setCrawlStrategy("LINK_SCAN");
        sourceMapper.insert(source);
        return source;
    }

    /** CSV 解析：UTF-8 + BOM 处理 + 引号转义（与手工导入一致） */
    private List<Map<String, String>> parseCsv(byte[] bytes) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("上传文件为空");
            List<String> headers = parseLine(stripBom(headerLine));
            validateHeaders(headers);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                rows.add(toRow(headers, parseLine(line)));
            }
        }
        return rows;
    }

    /** Excel 解析：第一个 sheet，首行表头，列同 CSV 模板 */
    private List<Map<String, String>> parseExcel(byte[] bytes, List<String> rowErrors) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            if (sheet == null || sheet.getRow(0) == null) throw new IllegalArgumentException("上传文件为空");
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                headers.add(formatter.formatCellValue(sheet.getRow(0).getCell(i)).trim());
            }
            validateHeaders(headers);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                List<String> values = new ArrayList<>();
                boolean hasValue = false;
                for (int i = 0; i < headers.size(); i++) {
                    String value = formatter.formatCellValue(row.getCell(i)).trim();
                    values.add(value);
                    if (!value.isEmpty()) hasValue = true;
                }
                if (!hasValue) continue;
                rows.add(toRow(headers, values));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Excel 文件解析失败：" + e.getMessage());
        }
        return rows;
    }

    private void validateHeaders(List<String> headers) {
        for (String required : new String[]{"stem", "answer", "optionA", "optionB", "knowledgeCode"}) {
            if (!headers.contains(required)) throw new IllegalArgumentException("上传文件表头不符合模板（缺少列 " + required + "）");
        }
    }

    private Map<String, String> toRow(List<String> headers, List<String> values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.size() && i < values.size(); i++) row.put(headers.get(i), values.get(i));
        return row;
    }

    private QuestionCandidate toCandidate(Map<String, String> row) {
        String questionType = value(row.get("questionType"), "SINGLE").toUpperCase();
        if (!"SINGLE".equals(questionType)) throw new IllegalArgumentException("导入场景仅支持单选题（SINGLE）");
        String stem = value(row.get("stem"), null);
        if (stem == null) throw new IllegalArgumentException("题干不能为空");
        QuestionCandidate candidate = new QuestionCandidate();
        candidate.setStem(stem);
        candidate.setAnswer(value(row.get("answer"), null));
        candidate.setAnalysis(emptyToNull(row.get("analysis")));
        candidate.setKnowledgeCode(value(row.get("knowledgeCode"), null));
        candidate.setSourceType(emptyToNull(row.get("sourceType")));
        candidate.setUsageType(emptyToNull(row.get("usageType")));
        candidate.setDifficultyExpected(emptyToNull(row.get("difficultyExpected")));
        String standardTime = emptyToNull(row.get("standardTimeSeconds"));
        if (standardTime != null) {
            try {
                candidate.setStandardTimeSeconds(Integer.valueOf(standardTime));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("参考耗时段格式错误: " + standardTime);
            }
        }
        String[] keys = {"A", "B", "C", "D"};
        for (String key : keys) {
            String text = emptyToNull(row.get("option" + key));
            if (text == null) continue;
            candidate.getOptions().add(new Option(key, text));
        }
        if (candidate.getOptions().size() < 2) throw new IllegalArgumentException("至少需要两个选项");
        return candidate;
    }

    static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append('"'); i++; }
                else quoted = !quoted;
            } else if (current == ',' && !quoted) { values.add(value.toString()); value.setLength(0); }
            else value.append(current);
        }
        if (quoted) throw new IllegalArgumentException("引号未闭合");
        values.add(value.toString());
        return values;
    }

    private String stripBom(String value) { return value.startsWith("\uFEFF") ? value.substring(1) : value; }
    private String value(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
    private String emptyToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }

    static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private String rootMessage(Exception error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
