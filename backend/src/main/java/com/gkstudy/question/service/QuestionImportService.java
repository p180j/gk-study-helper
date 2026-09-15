package com.gkstudy.question.service;

import com.gkstudy.question.dto.ImportResult;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class QuestionImportService {
    private static final List<String> HEADERS = Arrays.asList("stem", "optionA", "optionB", "optionC", "optionD", "answer", "analysis", "knowledgeCode", "difficulty", "standardTimeSeconds", "sourceType", "sourceYear", "sourceExam", "sourceName", "status", "usageType", "questionType");
    private static final Set<String> QUESTION_TYPES = new HashSet<>(Collections.singletonList("SINGLE"));
    private static final Set<String> STATUSES = new HashSet<>(Arrays.asList("ACTIVE", "DRAFT", "SUSPENDED", "ARCHIVED"));
    private static final Set<String> SOURCE_TYPES = new HashSet<>(Arrays.asList("HISTORICAL", "MANUAL", "IMPORTED", "AI_GENERATED", "AI_VARIANT"));
    private static final Set<String> USAGE_TYPES = new HashSet<>(Arrays.asList("TRAINING", "VALIDATION", "MOCK_RESERVED"));

    private final QuestionMapper questionMapper;
    private final TransactionTemplate transactionTemplate;

    public QuestionImportService(QuestionMapper questionMapper, TransactionTemplate transactionTemplate) {
        this.questionMapper = questionMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public ImportResult importCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("CSV 文件不能为空");
        ImportResult result = new ImportResult();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || !HEADERS.equals(parseLine(stripBom(headerLine)))) throw new IllegalArgumentException("CSV 表头不符合模板");
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.trim().isEmpty()) continue;
                try {
                    List<String> values = parseLine(line);
                    transactionTemplate.executeWithoutResult(status -> importRow(values));
                    result.success();
                } catch (Exception e) {
                    result.failure(row, rootMessage(e));
                }
            }
        }
        return result;
    }

    private void importRow(List<String> values) {
        if (values.size() != HEADERS.size()) throw new IllegalArgumentException("列数应为 " + HEADERS.size());
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < HEADERS.size(); i++) row.put(HEADERS.get(i), values.get(i).trim());
        require(row, "stem", "answer", "knowledgeCode", "sourceType");
        String type = valueOrDefault(row.get("questionType"), "SINGLE").toUpperCase();
        String status = valueOrDefault(row.get("status"), "DRAFT").toUpperCase();
        String sourceType = row.get("sourceType").toUpperCase();
        String usageType = valueOrDefault(row.get("usageType"), "TRAINING").toUpperCase();
        validateEnum("题型", type, QUESTION_TYPES);
        validateEnum("状态", status, STATUSES);
        validateEnum("来源类型", sourceType, SOURCE_TYPES);
        validateEnum("用途", usageType, USAGE_TYPES);
        List<QuestionOption> options = buildOptions(row);
        String answer = row.get("answer").toUpperCase();
        if (options.stream().noneMatch(option -> option.getOptionKey().equals(answer))) throw new IllegalArgumentException("答案没有对应选项");
        KnowledgePointRef knowledge = questionMapper.findKnowledgePointByCode(row.get("knowledgeCode"));
        if (knowledge == null) throw new IllegalArgumentException("知识点不存在或未启用: " + row.get("knowledgeCode"));
        Question question = buildQuestion(row, type, status, sourceType, usageType, answer, options);
        if (questionMapper.countByContentHash(question.getContentHash()) > 0) throw new IllegalArgumentException("题目重复");
        questionMapper.insertQuestion(question);
        for (QuestionOption option : options) { option.setQuestionId(question.getId()); questionMapper.insertOption(option); }
        questionMapper.insertKnowledge(question.getId(), knowledge.getId());
    }

    private Question buildQuestion(Map<String, String> row, String type, String status, String sourceType, String usageType, String answer, List<QuestionOption> options) {
        Question question = new Question();
        question.setQuestionType(type);
        question.setStem(row.get("stem"));
        question.setAnswer(answer);
        question.setAnalysis(emptyToNull(row.get("analysis")));
        question.setDifficultyExpected(decimalOrDefault(row.get("difficulty"), "50"));
        if (question.getDifficultyExpected().compareTo(BigDecimal.ZERO) < 0 || question.getDifficultyExpected().compareTo(new BigDecimal("100")) > 0) throw new IllegalArgumentException("难度必须在 0 到 100 之间");
        question.setStandardTimeSeconds(integerOrDefault(row.get("standardTimeSeconds"), 60));
        if (question.getStandardTimeSeconds() <= 0) throw new IllegalArgumentException("参考耗时必须大于 0");
        question.setSourceType(sourceType);
        question.setSourceYear(integerOrNull(row.get("sourceYear")));
        question.setSourceExam(emptyToNull(row.get("sourceExam")));
        question.setSourceName(emptyToNull(row.get("sourceName")));
        question.setUsageType(usageType);
        question.setStatus(status);
        question.setVersion(1);
        question.setContentHash(hash(question.getStem() + "|" + optionsText(options) + "|" + answer));
        return question;
    }

    private List<QuestionOption> buildOptions(Map<String, String> row) {
        List<QuestionOption> options = new ArrayList<>();
        String[] keys = {"A", "B", "C", "D"};
        for (int i = 0; i < keys.length; i++) {
            String text = row.get("option" + keys[i]);
            if (text == null || text.isEmpty()) continue;
            QuestionOption option = new QuestionOption(); option.setOptionKey(keys[i]); option.setOptionText(text); option.setSortNo(i + 1); options.add(option);
        }
        if (options.size() < 2) throw new IllegalArgumentException("至少需要两个选项");
        return options;
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

    private void require(Map<String, String> row, String... fields) { for (String field : fields) if (row.get(field) == null || row.get(field).isEmpty()) throw new IllegalArgumentException(field + " 不能为空"); }
    private void validateEnum(String name, String value, Set<String> values) { if (!values.contains(value)) throw new IllegalArgumentException(name + "不支持: " + value); }
    private String optionsText(List<QuestionOption> options) { StringBuilder text = new StringBuilder(); for (QuestionOption option : options) text.append(option.getOptionKey()).append(':').append(option.getOptionText()).append('|'); return text.toString(); }
    private String valueOrDefault(String value, String defaultValue) { return value == null || value.isEmpty() ? defaultValue : value; }
    private String emptyToNull(String value) { return value == null || value.isEmpty() ? null : value; }
    private BigDecimal decimalOrDefault(String value, String defaultValue) { try { return new BigDecimal(valueOrDefault(value, defaultValue)); } catch (NumberFormatException e) { throw new IllegalArgumentException("难度格式错误"); } }
    private Integer integerOrDefault(String value, int defaultValue) { Integer parsed = integerOrNull(value); return parsed == null ? defaultValue : parsed; }
    private Integer integerOrNull(String value) { if (value == null || value.isEmpty()) return null; try { return Integer.valueOf(value); } catch (NumberFormatException e) { throw new IllegalArgumentException("整数格式错误: " + value); } }
    private String stripBom(String value) { return value.startsWith("\uFEFF") ? value.substring(1) : value; }
    private String rootMessage(Exception error) { Throwable current = error; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 不可用", e); }
    }
}
