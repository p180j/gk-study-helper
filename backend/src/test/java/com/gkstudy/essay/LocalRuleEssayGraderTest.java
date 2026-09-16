package com.gkstudy.essay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.engine.EssayConstants;
import com.gkstudy.essay.engine.LocalRuleEssayGrader;
import com.gkstudy.essay.model.EssayQuestion;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalRuleEssayGraderTest {
    private final LocalRuleEssayGrader grader = new LocalRuleEssayGrader(new ObjectMapper());

    private static final String POINTS_JSON = "[{\"point\":\"产业兴旺\",\"keywords\":[\"产业\",\"兴旺\"]},"
            + "{\"point\":\"生态宜居\",\"keywords\":[\"生态\",\"宜居\"]},"
            + "{\"point\":\"治理有效\",\"keywords\":[\"治理\"]}]";

    @Test
    void fullHitWithinWordLimitScoresHigh() {
        EssayQuestion question = question("SUMMARY", 100, 400, POINTS_JSON);
        EssayEvaluationResult result = grader.grade(question, fullAnswer(), 600000L);
        assertTrue(result.getTotalScore() >= 70);
        for (Map.Entry<String, Double> entry : result.getDimensionScores().entrySet()) {
            assertTrue(entry.getValue() >= 70, entry.getKey() + " 应达标，实际 " + entry.getValue());
        }
        assertFalse(result.getStrengths().isEmpty());
        assertTrue(result.getProblems().isEmpty());
        assertTrue(result.getMissingPoints().isEmpty());
    }

    @Test
    void hollowAnswerBelowWordLimitScoresLow() {
        EssayQuestion question = question("SUMMARY", 100, 400, POINTS_JSON);
        EssayEvaluationResult result = grader.grade(question, "不会写。", 60000L);
        for (Map.Entry<String, Double> entry : result.getDimensionScores().entrySet()) {
            assertTrue(entry.getValue() < 50, entry.getKey() + " 应低于 50，实际 " + entry.getValue());
        }
        assertEquals(Arrays.asList("产业兴旺", "生态宜居", "治理有效"), result.getMissingPoints());
        assertFalse(result.getProblems().isEmpty());
        assertFalse(result.getSuggestions().isEmpty());
    }

    @Test
    void partialHitReportsOnlyMissingPoints() {
        String pointsJson = "[{\"point\":\"产业兴旺\",\"keywords\":[\"产业\"]},{\"point\":\"生态宜居\",\"keywords\":[\"生态\"]},"
                + "{\"point\":\"治理有效\",\"keywords\":[\"治理\"]},{\"point\":\"生活富裕\",\"keywords\":[\"富裕\"]}]";
        EssayQuestion question = question("SUMMARY", 100, 400, pointsJson);
        String answer = "产业与生态协同发展。" + "继续推动城乡融合发展，".repeat(10);
        EssayEvaluationResult result = grader.grade(question, answer, 600000L);
        assertEquals(0.5, (double) result.getEvidence().get("hitRatio"), 0.0001);
        assertEquals(2, result.getEvidence().get("hitCount"));
        assertEquals(4, result.getEvidence().get("totalPoints"));
        assertEquals(Arrays.asList("治理有效", "生活富裕"), result.getMissingPoints());
    }

    @Test
    void missingReferencePointsFallsBackToDefaultHitRatio() {
        EssayQuestion question = question("ANALYSIS", 100, 400, null);
        String answer = "推进乡村治理体系建设。" + "加强基层治理能力，".repeat(10);
        EssayEvaluationResult result = grader.grade(question, answer, 600000L);
        assertEquals(0.6, (double) result.getEvidence().get("hitRatio"), 0.0001);
        assertEquals(0, result.getEvidence().get("totalPoints"));
        assertEquals(0, result.getEvidence().get("hitCount"));
        assertTrue(result.getMissingPoints().isEmpty());
        for (Double score : result.getDimensionScores().values()) {
            assertTrue(score >= 0 && score <= 100, "评分应在 [0,100] 内，实际 " + score);
        }
        EssayEvaluationResult emptyJson = grader.grade(question("ANALYSIS", 100, 400, ""), answer, 600000L);
        assertEquals(0.6, (double) emptyJson.getEvidence().get("hitRatio"), 0.0001);
        assertEquals(result.getTotalScore(), emptyJson.getTotalScore(), 0.0001);
    }

    @Test
    void overWordLimitCompressesWordRatioAndLowersExpression() {
        EssayQuestion question = question("SUMMARY", 50, 100, null);
        EssayEvaluationResult inRange = grader.grade(question, "产业兴旺生态宜居治理有效".repeat(8), 600000L);
        EssayEvaluationResult over = grader.grade(question, "产业兴旺生态宜居治理有效".repeat(25), 600000L);
        double inRangeExpression = inRange.getDimensionScores().get(EssayConstants.DIM_EXPRESSION);
        double overExpression = over.getDimensionScores().get(EssayConstants.DIM_EXPRESSION);
        assertTrue(overExpression < inRangeExpression, "超字数后文字表达应下降：" + overExpression + " vs " + inRangeExpression);
        assertTrue(overExpression < 70);
        assertEquals(300, over.getEvidence().get("wordCount"));
        assertTrue(over.getTotalScore() < inRange.getTotalScore());
    }

    @Test
    void marksItselfAsLocalRuleEvaluator() {
        assertEquals("LOCAL_RULE_V1", grader.evaluator());
        EssayEvaluationResult result = grader.grade(question("SUMMARY", 100, 400, POINTS_JSON), fullAnswer(), 600000L);
        assertEquals("LOCAL_RULE_V1", result.getEvaluator());
    }

    @Test
    void evidenceCarriesExplainableFields() {
        EssayEvaluationResult result = grader.grade(question("SUMMARY", 100, 400, POINTS_JSON), fullAnswer(), 600000L);
        Map<String, Object> evidence = result.getEvidence();
        assertEquals(125, evidence.get("wordCount"));
        assertEquals(3, evidence.get("hitCount"));
        assertEquals(3, evidence.get("totalPoints"));
        assertEquals(1.0, (double) evidence.get("hitRatio"), 0.0001);
        assertEquals("SUMMARY", evidence.get("questionType"));
        assertEquals("THEME_RURAL", evidence.get("topicCode"));
        assertEquals("乡村振兴", evidence.get("topicName"));
        assertEquals(77L, evidence.get("topicKnowledgePointId"));
        assertEquals(EssayConstants.DIM_SUMMARY, evidence.get("primaryDimension"));
        assertEquals(600000L, evidence.get("durationMs"));
        assertEquals(100, evidence.get("wordLimitMin"));
        assertEquals(400, evidence.get("wordLimitMax"));
    }

    private EssayQuestion question(String type, int min, int max, String referencePointsJson) {
        EssayQuestion question = new EssayQuestion();
        question.setId(1001L); question.setQuestionType(type);
        question.setWordLimitMin(min); question.setWordLimitMax(max);
        question.setStandardTimeSeconds(1800); question.setReferencePointsJson(referencePointsJson);
        question.setTopicCode("THEME_RURAL"); question.setTopicName("乡村振兴");
        question.setTopicKnowledgePointId(77L); question.setStatus("ACTIVE"); question.setVersion(1);
        return question;
    }

    /** 125 个非空白字符（15 + 11×10），处于 [100,400] 字数区间内，三个参考要点全部命中。 */
    private String fullAnswer() {
        return "产业兴旺、生态宜居、治理有效。" + "推进乡村振兴战略实施，".repeat(10);
    }
}
