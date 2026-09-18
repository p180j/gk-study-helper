package com.gkstudy.content.service;

import com.gkstudy.content.dto.QuestionCandidate;
import com.gkstudy.content.dto.QuestionCandidate.Option;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从纯文本抽取单选题候选：题号（或题型标记）+ 题干 + A-D 选项 + 答案 + 可选解析。
 * 兼容多行与已规范化的单行文本；无答案的候选同样输出，由质量门禁拦截；
 * 文本无题目结构时返回空列表。
 */
@Service
public class QuestionTextExtractor {
    private static final Pattern QUESTION_NO = Pattern.compile("(?:^|\\s)(\\d{1,3})[.、．]\\s*");
    private static final Pattern TYPE_PREFIX = Pattern.compile("(?:单选题|多选题|判断题|不定项选择|不定项)\\s*[:：]?\\s*");
    private static final Pattern OPTION_MARK = Pattern.compile("(?<![A-Za-z0-9])([A-D])[.．、:：]");
    private static final Pattern ANSWER_MARK = Pattern.compile("(?:正确答案|参考答案|答案)\\s*(?:[:：]\\s*|[等为是]\\s*)([A-D])|[【〖]答案[】〗]\\s*([A-D])");
    private static final Pattern ANALYSIS_MARK = Pattern.compile("(?:试题解析|答案解析|参考解析|解析)\\s*[:：]|[【〖](?:成公)?解析[】〗]");
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_ANALYSIS_LENGTH = 2000;

    /** 块 = 一个候选题目在原文中的范围：start 起点、stemStart 前缀（题号/题型标记）之后、end 块尾 */
    private static class Block {
        final int start;
        final int stemStart;
        final int end;

        Block(int start, int stemStart, int end) {
            this.start = start; this.stemStart = stemStart; this.end = end;
        }
    }

    /** 起点候选：位置 + 前缀结束位置（题号与紧随的题型标记合并剥离） */
    private static class Mark {
        final int start;
        final int prefixEnd;

        Mark(int start, int prefixEnd) {
            this.start = start; this.prefixEnd = prefixEnd;
        }
    }

    public List<QuestionCandidate> extract(String text) {
        List<QuestionCandidate> candidates = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return candidates;
        String source = text.trim();
        for (Block block : splitBlocks(source)) {
            QuestionCandidate candidate = parseBlock(source, block);
            if (candidate != null) candidates.add(candidate);
        }
        return candidates;
    }

    /**
     * 按题干强特征词推断模块级知识点（五个模块 code），命中才返回；
     * 推断不出返回 null，由质量门禁以"知识点无法分类"进入异常处理，不冒险自动入库。
     */
    public static String guessKnowledgeCode(String stem) {
        if (stem == null || stem.trim().isEmpty()) return null;
        String text = stem.trim();
        if (containsAny(text, "依次填入", "这段文字", "意在", "最恰当的一组", "语句排序", "主旨", "加点的词", "没有语病", "接下来最可能")) return "VERBAL";
        if (containsAny(text, "削弱", "加强", "前提假设", "下列推理", "由此可以推出", "真话", "假话", "类比", "定义判断")) return "JUDGEMENT";
        if ((containsAny(text, "比重", "增长率", "增长量", "基期", "现期", "同比", "环比", "百分点") || containsAny(text, "亿元", "万人", "亿元"))
                && text.matches(".*\\d+.*")) return "DATA_ANALYSIS";
        if (containsAny(text, "概率", "排列", "甲单独", "乙单独", "天完成", "至少需要", "多少种", "每隔", "植树", "相遇", "追及")) return "QUANTITY";
        if (containsAny(text, "宪法", "民法典", "我国的基本", "下列说法", "根本政治制度", "国家机构", "行政处罚", "下列关于")) return "COMMON_SENSE";
        return null;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    /** 按题号 / 题型标记切块；无任何起点但存在选项结构时整段视为一个块 */
    private List<Block> splitBlocks(String text) {
        List<Mark> marks = new ArrayList<>();
        Matcher no = QUESTION_NO.matcher(text);
        while (no.find()) marks.add(new Mark(no.start(), no.end()));
        // 题号后紧跟的题型标记（如 "1. 单选题："）合并剥离，否则题型标记独立作为起点
        Matcher type = TYPE_PREFIX.matcher(text);
        while (type.find()) {
            Mark merged = null;
            for (Mark mark : marks) {
                if (type.start() >= mark.start && type.start() <= mark.prefixEnd + 2) { merged = mark; break; }
            }
            if (merged == null) marks.add(new Mark(type.start(), type.end()));
            else if (type.end() > merged.prefixEnd) marks.set(marks.indexOf(merged), new Mark(merged.start, type.end()));
        }
        if (marks.isEmpty()) {
            // 无题号：整段至少出现两个不同选项标记才当作一个题目块
            int distinct = 0;
            boolean[] seen = new boolean[4];
            Matcher option = OPTION_MARK.matcher(text);
            while (option.find()) {
                int index = option.group(1).charAt(0) - 'A';
                if (!seen[index]) { seen[index] = true; distinct++; }
            }
            List<Block> blocks = new ArrayList<>();
            if (distinct >= MIN_OPTIONS) {
                Matcher prefix = TYPE_PREFIX.matcher(text);
                int stemStart = prefix.find() ? prefix.end() : 0;
                blocks.add(new Block(0, stemStart, text.length()));
            }
            return blocks;
        }
        marks.sort((one, two) -> Integer.compare(one.start, two.start));
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < marks.size(); i++) {
            int end = i + 1 < marks.size() ? marks.get(i + 1).start : text.length();
            blocks.add(new Block(marks.get(i).start, marks.get(i).prefixEnd, end));
        }
        return blocks;
    }

    /** 解析单个块；选项少于 2 个视为非题目结构返回 null */
    private QuestionCandidate parseBlock(String text, Block block) {
        String body = text.substring(block.stemStart, block.end);
        if (body.isEmpty()) return null;

        List<int[]> optionMarks = new ArrayList<>();
        List<String> optionKeys = new ArrayList<>();
        Matcher optionM = OPTION_MARK.matcher(body);
        while (optionM.find()) {
            optionMarks.add(new int[]{optionM.start(), optionM.end()});
            optionKeys.add(optionM.group(1));
        }
        if (optionMarks.size() < MIN_OPTIONS) return null;

        int firstOption = optionMarks.get(0)[0];
        int lastOption = optionMarks.get(optionMarks.size() - 1)[0];
        // 答案 / 解析标记必须位于选项区之后；题干中的同字样跳过并继续向后找真正的标记
        int answerPos = -1;
        String answer = null;
        Matcher answerM = ANSWER_MARK.matcher(body);
        while (answerM.find()) {
            if (answerM.start() >= firstOption) {
                answerPos = answerM.start();
                answer = answerM.group(1) == null ? answerM.group(2) : answerM.group(1);
                break;
            }
        }

        int analysisPos = -1;
        Matcher analysisM = ANALYSIS_MARK.matcher(body);
        while (analysisM.find()) {
            if (analysisM.start() >= lastOption) { analysisPos = analysisM.start(); break; }
        }

        int contentEnd = body.length();
        if (answerPos >= 0) contentEnd = Math.min(contentEnd, answerPos);
        if (analysisPos >= 0) contentEnd = Math.min(contentEnd, analysisPos);

        QuestionCandidate candidate = new QuestionCandidate();
        candidate.setStem(body.substring(0, firstOption).trim());
        List<Option> options = new ArrayList<>();
        for (int i = 0; i < optionMarks.size(); i++) {
            int[] mark = optionMarks.get(i);
            if (mark[0] >= contentEnd) break;
            String key = optionKeys.get(i);
            if (options.stream().anyMatch(option -> option.getKey().equals(key))) continue;
            int optionEnd = i + 1 < optionMarks.size() ? Math.min(optionMarks.get(i + 1)[0], contentEnd) : contentEnd;
            String optionText = body.substring(mark[1], optionEnd).trim();
            if (optionText.isEmpty()) continue;
            options.add(new Option(key, optionText));
        }
        if (options.size() < MIN_OPTIONS) return null;
        candidate.setOptions(options);
        candidate.setAnswer(answer);
        if (analysisPos >= 0) {
            String analysis = body.substring(analysisPos, body.length());
            Matcher mark = ANALYSIS_MARK.matcher(analysis);
            analysis = mark.find() ? analysis.substring(mark.end()) : analysis;
            analysis = analysis.trim();
            if (analysis.length() > MAX_ANALYSIS_LENGTH) analysis = analysis.substring(0, MAX_ANALYSIS_LENGTH);
            if (!analysis.isEmpty()) candidate.setAnalysis(analysis);
        }
        return candidate;
    }
}
