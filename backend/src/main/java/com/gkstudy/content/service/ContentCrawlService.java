package com.gkstudy.content.service;

import com.gkstudy.common.BusinessException;
import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.fetch.ContentFetcher;
import com.gkstudy.content.mapper.ContentCrawlLogMapper;
import com.gkstudy.content.mapper.ContentSourceMapper;
import com.gkstudy.content.mapper.ContentStagingMapper;
import com.gkstudy.content.model.ContentCrawlLog;
import com.gkstudy.content.model.ContentSource;
import com.gkstudy.content.model.ContentStaging;
import com.gkstudy.reading.mapper.PoliticalTopicMapper;
import com.gkstudy.reading.mapper.ReadingMaterialMapper;
import com.gkstudy.reading.model.PoliticalTopic;
import com.gkstudy.reading.model.ReadingMaterial;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内容自动化管线：发现 → 下载 → 解析 → 去重 → 分类 → 入库 / 人工复核。
 * 只抓取公开页面，不绕过登录 / 验证码 / 付费 / 访问控制；单条失败只标记自身，不影响批次。
 * 疑似试题内容先抽取候选再走质量门禁分流；低可信（B/C/D）非试题内容仍需人工确认。
 */
@Service
public class ContentCrawlService {
    private static final Logger log = LoggerFactory.getLogger(ContentCrawlService.class);
    private static final Pattern KEYWORD = Pattern.compile("公告|大纲|招聘|考试|试题|样题|题库|公务员|事业单位|笔试|面试|成绩|职位|录用|遴选");
    private static final Set<String> ATTACHMENT_EXTS = new HashSet<>(java.util.Arrays.asList(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar", ".jpg", ".jpeg", ".png"));
    private static final Set<String> TEXT_MIME = new HashSet<>(java.util.Arrays.asList(
            "text/html", "text/plain", "application/xhtml+xml"));
    private static final Pattern PUBLISH_TIME = Pattern.compile("(20\\d{2})\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})");
    private static final Pattern YEAR = Pattern.compile("(19|20)\\d{2}");
    private static final Pattern OPTION_LINE = Pattern.compile("[ABCD][．.、:：]");
    private static final Pattern QUESTION_LIKE = Pattern.compile("单选题|多选题|判断题|不定项|下列(说法|哪)");
    private static final int MAX_TEXT_LENGTH = 100000;
    private static final int MIN_IMPORT_TEXT_LENGTH = 200;

    private final ContentSourceMapper sourceMapper;
    private final ContentStagingMapper stagingMapper;
    private final ContentFetcher fetcher;
    private final PoliticalTopicMapper topicMapper;
    private final ReadingMaterialMapper materialMapper;
    private final QuestionTextExtractor textExtractor;
    private final QuestionStagingImportService questionImporter;
    private final ContentCrawlLogMapper crawlLogMapper;
    private final String storageDir;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "content-crawl");
        thread.setDaemon(true);
        return thread;
    });

    public ContentCrawlService(ContentSourceMapper sourceMapper, ContentStagingMapper stagingMapper,
                               ContentFetcher fetcher, PoliticalTopicMapper topicMapper,
                               ReadingMaterialMapper materialMapper, QuestionTextExtractor textExtractor,
                               QuestionStagingImportService questionImporter, ContentCrawlLogMapper crawlLogMapper,
                               @Value("${content.storage-dir:./data/content}") String storageDir) {
        this.sourceMapper = sourceMapper; this.stagingMapper = stagingMapper; this.fetcher = fetcher;
        this.topicMapper = topicMapper; this.materialMapper = materialMapper;
        this.textExtractor = textExtractor; this.questionImporter = questionImporter;
        this.crawlLogMapper = crawlLogMapper; this.storageDir = storageDir;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    public static class CrawlSummary {
        public int discovered;
        public int processed;
        public int downloaded;
        public int parsed;
        public int imported;
        public int duplicates;
        public int needsReview;
        public int failed;
        public final List<String> errors = new ArrayList<>();
    }

    /** 对单个来源执行完整管线：发现 + 逐条处理（失败隔离） */
    public CrawlSummary crawlSource(Long sourceId) {
        ContentSource source = sourceMapper.findById(sourceId);
        if (source == null) throw new IllegalArgumentException("内容来源不存在");
        if (!source.isEnabled()) throw new IllegalArgumentException("内容来源已停用");
        sourceMapper.updateCrawlState(sourceId, "RUNNING");
        CrawlSummary summary = new CrawlSummary();
        try {
            summary.discovered = discover(source);
            List<ContentStaging> items = stagingMapper.findProcessable(sourceId);
            for (ContentStaging item : items) {
                try {
                    summary.processed++;
                    processItem(item, summary);
                } catch (Exception e) {
                    summary.failed++;
                    String reason = rootMessage(e);
                    stagingMapper.markFailed(item.getId(), reason);
                    summary.errors.add(item.getSourceUrl() + " : " + reason);
                }
            }
            sourceMapper.updateCrawlState(sourceId, "IDLE");
        } catch (Exception e) {
            sourceMapper.updateCrawlState(sourceId, "FAILED");
            throw new IllegalStateException("发现阶段失败：" + rootMessage(e), e);
        }
        return summary;
    }

    /**
     * 异步触发采集：校验来源存在且启用、同来源无 RUNNING 日志后立即返回日志对象；
     * 后台单线程执行 crawlSource 并把结果回写 content_crawl_log。
     */
    public ContentCrawlLog triggerCrawl(Long sourceId) {
        ContentSource source = sourceMapper.findById(sourceId);
        if (source == null) throw new IllegalArgumentException("内容来源不存在");
        if (!source.isEnabled()) throw new IllegalArgumentException("内容来源已停用");
        ContentCrawlLog latest = crawlLogMapper.findLatestBySource(sourceId);
        if (latest != null && ContentCrawlLog.RUNNING.equals(latest.getStatus())) {
            throw new BusinessException("CRAWL_ALREADY_RUNNING", "该来源正在采集中，请稍后刷新查看");
        }
        ContentCrawlLog entry = new ContentCrawlLog();
        entry.setSourceId(sourceId);
        entry.setSourceName(source.getName());
        entry.setStatus(ContentCrawlLog.RUNNING);
        entry.setStartTime(LocalDateTime.now());
        crawlLogMapper.insert(entry);
        executor.submit(() -> runCrawlTask(entry));
        return entry;
    }

    /** 采集执行体：完成 / 失败都回写日志（包级供测试直接调用） */
    void runCrawlTask(ContentCrawlLog entry) {
        try {
            CrawlSummary summary = crawlSource(entry.getSourceId());
            entry.setStatus(ContentCrawlLog.SUCCESS);
            entry.setDiscovered(summary.discovered);
            entry.setDownloaded(summary.downloaded);
            entry.setParsed(summary.parsed);
            entry.setImported(summary.imported);
            entry.setDuplicates(summary.duplicates);
            entry.setNeedsReview(summary.needsReview);
            entry.setFailed(summary.failed);
            entry.setMessage(summary.errors.isEmpty() ? null : String.join("；", summary.errors));
        } catch (Exception e) {
            entry.setStatus(ContentCrawlLog.FAILED);
            entry.setMessage(rootMessage(e));
        }
        entry.setEndTime(LocalDateTime.now());
        try {
            crawlLogMapper.update(entry);
        } catch (Exception e) {
            log.error("采集日志回写失败 sourceId={}", entry.getSourceId(), e);
        }
    }

    /** 重试单条：重置为已发现并重新走管线 */
    public void retry(Long stagingId) {
        ContentStaging item = stagingMapper.findById(stagingId);
        if (item == null) throw new IllegalArgumentException("暂存记录不存在");
        stagingMapper.updateStatus(stagingId, ContentStaging.DISCOVERED);
        CrawlSummary summary = new CrawlSummary();
        try {
            processItem(stagingMapper.findById(stagingId), summary);
        } catch (Exception e) {
            stagingMapper.markFailed(stagingId, rootMessage(e));
        }
    }

    /** 从来源页面发现候选链接，按 URL 去重后写入 staging（已存在则跳过） */
    int discover(ContentSource source) throws Exception {
        ContentFetcher.Fetched page = fetcher.fetch(source.getBaseUrl());
        Document document = Jsoup.parse(new String(page.body, StandardCharsets.UTF_8), source.getBaseUrl());
        int created = 0;
        Set<String> seen = new HashSet<>();
        String pageText = document.body() == null ? "" : document.body().text();
        // 公开题源常直接就是一张结构化试卷页，不能只扫描其下级链接而遗漏页面自身。
        if (QUESTION_LIKE.matcher(pageText).find() || countOccurrences(OPTION_LINE, pageText) >= 4) {
            created += createDiscoveredStaging(source, source.getBaseUrl(), document.title(), seen) ? 1 : 0;
        }
        for (Element link : document.select("a[href]")) {
            String url = link.absUrl("href").trim();
            String text = link.text().trim();
            if (!isCandidate(url, text)) continue;
            created += createDiscoveredStaging(source, url, text, seen) ? 1 : 0;
        }
        return created;
    }

    private boolean createDiscoveredStaging(ContentSource source, String url, String title, Set<String> seen) {
        if (!seen.add(url) || stagingMapper.countByUrl(url) > 0) return false;
        ContentStaging staging = new ContentStaging();
        staging.setSourceId(source.getId());
        staging.setSourceUrl(url);
        staging.setSiteName(source.getName());
        staging.setTitle(title == null || title.trim().isEmpty() ? url : title.trim());
        staging.setExamType(source.getExamType());
        staging.setSourceType(guessSourceType(url, title));
        staging.setTrustLevel(source.getTrustLevel());
        staging.setStatus(ContentStaging.DISCOVERED);
        stagingMapper.insert(staging);
        return true;
    }

    /** 单条状态机推进：下载 → 解析 → 去重 → 分类入库 */
    void processItem(ContentStaging item, CrawlSummary summary) throws Exception {
        if (ContentStaging.DISCOVERED.equals(item.getStatus())) {
            item = download(item);
            summary.downloaded++;
        }
        if (ContentStaging.DOWNLOADED.equals(item.getStatus())) {
            item = parse(item);
            if (ContentStaging.PARSED.equals(item.getStatus())) summary.parsed++;
        }
        if (ContentStaging.PARSED.equals(item.getStatus())) {
            item = dedup(item, summary);
        }
        if (ContentStaging.DEDUPED.equals(item.getStatus())) {
            classifyAndImport(item, summary);
        }
    }

    private ContentStaging download(ContentStaging item) throws Exception {
        ContentFetcher.Fetched fetched = fetcher.fetch(item.getSourceUrl());
        if (fetched.body == null || fetched.body.length == 0) throw new IllegalStateException("下载内容为空");
        String fileHash = sha256(fetched.body);
        Path path = storagePath(fileHash, extensionOf(item.getSourceUrl(), fetched.fileName));
        Files.createDirectories(path.getParent());
        Files.write(path, fetched.body);
        item.setFileHash(fileHash);
        item.setMimeType(fetched.mimeType);
        item.setFileSize((long) fetched.body.length);
        item.setFilePath(path.toString());
        item.setOriginalFileName(fetched.fileName);
        stagingMapper.markDownloaded(item);
        item.setStatus(ContentStaging.DOWNLOADED);
        return item;
    }

    private ContentStaging parse(ContentStaging item) throws IOException {
        String mime = item.getMimeType() == null ? "" : item.getMimeType();
        if (!TEXT_MIME.contains(mime)) {
            stagingMapper.markNeedsReview(item.getId(), "附件类型（" + (mime.isEmpty() ? "未知" : mime)
                    + "）暂不支持自动解析，原始文件已保留，请人工处理");
            item.setStatus(ContentStaging.NEEDS_REVIEW);
            return item;
        }
        String raw = new String(Files.readAllBytes(Paths.get(item.getFilePath())), StandardCharsets.UTF_8);
        Document document = Jsoup.parse(raw, item.getSourceUrl());
        String text = normalize(document.body() == null ? "" : document.body().text());
        if (text.length() > MAX_TEXT_LENGTH) text = text.substring(0, MAX_TEXT_LENGTH);
        if (item.getTitle() == null || item.getTitle().isEmpty() || item.getTitle().startsWith("http")) {
            item.setTitle(document.title() == null || document.title().isEmpty() ? item.getOriginalFileName() : document.title());
        }
        item.setParsedText(text);
        item.setContentHash(sha256(text.getBytes(StandardCharsets.UTF_8)));
        item.setPublishTime(extractPublishTime(text));
        item.setSourceYear(extractYear(item.getTitle() + " " + text));
        stagingMapper.markParsed(item);
        item.setStatus(ContentStaging.PARSED);
        return item;
    }

    private ContentStaging dedup(ContentStaging item, CrawlSummary summary) {
        if (item.getFileHash() != null && stagingMapper.countSameFileHash(item.getFileHash(), item.getId()) > 0) {
            String reason = "文件重复（hash 相同），已跳过入库";
            stagingMapper.markFailed(item.getId(), reason);
            item.setStatus(ContentStaging.FAILED);
            summary.failed++;
            summary.duplicates++;
            return item;
        }
        if (item.getContentHash() != null && stagingMapper.countSameContentHash(item.getContentHash(), item.getId()) > 0) {
            String reason = "内容重复（规范化 hash 相同），已跳过入库";
            stagingMapper.markFailed(item.getId(), reason);
            item.setStatus(ContentStaging.FAILED);
            summary.failed++;
            summary.duplicates++;
            return item;
        }
        stagingMapper.markDeduped(item.getId());
        item.setStatus(ContentStaging.DEDUPED);
        return item;
    }

    private void classifyAndImport(ContentStaging item, CrawlSummary summary) {
        String text = item.getParsedText() == null ? "" : item.getParsedText();
        // 疑似试题：先抽取候选，非空走质量门禁分流入库（C/D 高置信也可入库，source_level 自然被验证取题排除）
        if (QUESTION_LIKE.matcher(text).find() || countOccurrences(OPTION_LINE, text) >= 4) {
            List<QuestionCandidate> candidates = textExtractor.extract(text);
            if (!candidates.isEmpty()) {
                importExtractedQuestions(item, candidates, summary);
                return;
            }
            // 抽取不出题目结构：低可信仍需人工确认，S/A 也标记人工处理
            if (item.getTrustLevel() == null || "BCD".indexOf(item.getTrustLevel()) >= 0) {
                stagingMapper.markNeedsReview(item.getId(), "来源可信度为 " + item.getTrustLevel()
                        + "，疑似试题但无法自动抽取，需人工确认（低可信内容不参与核心能力测量）");
            } else {
                stagingMapper.markNeedsReview(item.getId(), "疑似试题内容但无法自动抽取题目结构，请人工确认后通过题库导入流程处理");
            }
            summary.needsReview++;
            return;
        }
        // 低可信非试题内容不得自动入库参与核心能力测量
        if (item.getTrustLevel() == null || "BCD".indexOf(item.getTrustLevel()) >= 0) {
            stagingMapper.markNeedsReview(item.getId(), "来源可信度为 " + item.getTrustLevel()
                    + "，需人工确认后才可入库（低可信内容不参与核心能力测量）");
            summary.needsReview++;
            return;
        }
        if (text.length() < MIN_IMPORT_TEXT_LENGTH) {
            stagingMapper.markNeedsReview(item.getId(), "正文内容过短（" + text.length() + " 字），请人工确认");
            summary.needsReview++;
            return;
        }
        PoliticalTopic topic = matchTopic(item.getTitle() + " " + text);
        if (topic == null) {
            stagingMapper.markNeedsReview(item.getId(), "未能自动匹配政治专题，请人工指定专题后入库");
            summary.needsReview++;
            return;
        }
        ReadingMaterial material = new ReadingMaterial();
        material.setTopicId(topic.getId());
        material.setTitle(item.getTitle());
        material.setSource(item.getSiteName());
        material.setPublishDate(item.getPublishTime() == null ? LocalDate.now() : item.getPublishTime().toLocalDate());
        material.setContent(text);
        material.setStandardExpressionsJson("[]");
        material.setCasesJson("[]");
        material.setApplicableEssayThemesJson("[]");
        material.setStatus("DRAFT");
        materialMapper.insert(material);
        stagingMapper.markImported(item.getId(), "READING_MATERIAL", material.getId());
        item.setStatus(ContentStaging.IMPORTED);
        summary.imported++;
    }

    /** 抽取候选分流入库：有自动入库则标记 IMPORTED，全部异常 / 重复则留给人工或标记完成 */
    private void importExtractedQuestions(ContentStaging item, List<QuestionCandidate> candidates, CrawlSummary summary) {
        for (QuestionCandidate candidate : candidates) {
            if (candidate.getKnowledgeCode() == null || candidate.getKnowledgeCode().trim().isEmpty()) {
                candidate.setKnowledgeCode(QuestionTextExtractor.guessKnowledgeCode(candidate.getStem()));
            }
        }
        QuestionStagingImportService.ImportStats stats = questionImporter.importCandidates(item, candidates);
        summary.imported += stats.imported;
        summary.duplicates += stats.duplicates;
        summary.needsReview += stats.needsReview;
        summary.failed += stats.failed;
        summary.errors.addAll(stats.errors);
        if (stats.imported > 0) {
            stagingMapper.markImported(item.getId(), "QUESTION", stats.firstQuestionId);
            String note = "自动入库 " + stats.imported + " 题/重复 " + stats.duplicates + "/异常 " + stats.needsReview;
            if (stats.sampled > 0) note += "（B 级来源入库 " + stats.sampled + " 题建议抽样质检）";
            stagingMapper.updateReviewNote(item.getId(), note);
            item.setStatus(ContentStaging.IMPORTED);
        } else if (stats.needsReview > 0) {
            stagingMapper.markNeedsReview(item.getId(), "疑似试题内容：抽取 " + candidates.size()
                    + " 题均未通过质量门禁，已生成逐题人工检查记录");
            item.setStatus(ContentStaging.NEEDS_REVIEW);
        } else if (stats.duplicates > 0) {
            stagingMapper.markFailed(item.getId(), "抽取 " + candidates.size() + " 题均与已有题目重复，无新增入库");
            item.setStatus(ContentStaging.FAILED);
        } else {
            stagingMapper.markFailed(item.getId(), "抽取 " + candidates.size() + " 题入库全部失败：" 
                    + (stats.errors.isEmpty() ? "未知原因" : stats.errors.get(0)));
            item.setStatus(ContentStaging.FAILED);
        }
    }

    PoliticalTopic matchTopic(String text) {
        for (PoliticalTopic topic : topicMapper.findAllActive()) {
            if (text.contains(topic.getName())) return topic;
        }
        return null;
    }

    private boolean isCandidate(String url, String text) {
        if (url.isEmpty() || url.startsWith("javascript:") || url.startsWith("mailto:") || url.startsWith("tel:")
                || url.startsWith("#") || url.endsWith("/#")) return false;
        String lower = url.toLowerCase();
        if (lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".ico")) return false;
        for (String ext : ATTACHMENT_EXTS) {
            if (lower.endsWith(ext) || lower.contains(ext + "?")) return true;
        }
        return !text.isEmpty() && KEYWORD.matcher(text).find();
    }

    private String guessSourceType(String url, String text) {
        String combined = url + " " + text;
        if (combined.contains("大纲")) return "SYLLABUS";
        if (combined.contains("样题") || combined.contains("试题") || combined.contains("题库")) return "SAMPLE_QUESTION";
        if (ATTACHMENT_EXTS.stream().anyMatch(url.toLowerCase()::endsWith)) return "ATTACHMENT";
        if (combined.contains("公告")) return "ANNOUNCEMENT";
        return "ARTICLE";
    }

    private String extensionOf(String url, String fileName) {
        String source = fileName != null && fileName.contains(".") ? fileName : url.split("[?#]")[0];
        int dot = source.lastIndexOf('.');
        return dot >= 0 ? source.substring(dot).toLowerCase() : ".bin";
    }

    private Path storagePath(String fileHash, String extension) {
        return Paths.get(storageDir, fileHash.substring(0, 2), fileHash + extension);
    }

    static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    static LocalDateTime extractPublishTime(String text) {
        if (text == null) return null;
        Matcher matcher = PUBLISH_TIME.matcher(text);
        if (!matcher.find()) return null;
        try {
            return LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    static Integer extractYear(String text) {
        if (text == null) return null;
        Matcher matcher = YEAR.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group()) : null;
    }

    static int countOccurrences(Pattern pattern, String text) {
        int count = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) count++;
        return count;
    }

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
