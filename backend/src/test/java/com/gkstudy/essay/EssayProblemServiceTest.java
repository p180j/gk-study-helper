package com.gkstudy.essay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.engine.EssayConstants;
import com.gkstudy.essay.engine.EssayProblemEngine;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.essay.service.EssayProblemService;
import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.question.mapper.KnowledgePointMapper;
import com.gkstudy.reading.mapper.ReadingRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EssayProblemServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 16, 10, 0);

    private LearningProblemMapper problemMapper;
    private AbilityMapper abilityMapper;
    private ReadingRecordMapper readingRecordMapper;
    private EssayProblemService service;

    @BeforeEach
    void setUp() {
        problemMapper = mock(LearningProblemMapper.class);
        abilityMapper = mock(AbilityMapper.class);
        readingRecordMapper = mock(ReadingRecordMapper.class);
        KnowledgePointMapper knowledgePointMapper = mock(KnowledgePointMapper.class);
        service = new EssayProblemService(new EssayProblemEngine(), problemMapper, abilityMapper,
                readingRecordMapper, knowledgePointMapper, new ObjectMapper());
        when(knowledgePointMapper.findIdByCode(EssayConstants.DIM_POINT_COMPLETENESS)).thenReturn(21L);
        when(knowledgePointMapper.findIdByCode(EssayConstants.DIM_ANALYSIS)).thenReturn(22L);
        when(knowledgePointMapper.findIdByCode(EssayConstants.DIM_SUMMARY)).thenReturn(23L);
        when(knowledgePointMapper.findIdByCode(EssayConstants.DIM_EXPRESSION)).thenReturn(24L);
        when(problemMapper.findForUpdate(eq(7L), anyLong(), anyString())).thenReturn(null);
        when(problemMapper.insert(any())).thenAnswer(invocation -> {
            ((LearningProblem) invocation.getArgument(0)).setId(88L); return 1;
        });
        when(abilityMapper.findForUpdate(eq(7L), anyLong())).thenReturn(dimensionProfile());
    }

    @Test
    void lowPointCompletenessCreatesMissingPointsProblem() {
        service.evaluate(7L, question("ANALYSIS"),
                result(dims(30.0, EssayConstants.DIM_ANALYSIS, 70.0), List.of("生态保护", "基层治理")), NOW);

        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).insert(captor.capture());
        LearningProblem problem = captor.getValue();
        assertEquals(LearningProblemEngine.ESSAY_MISSING_POINTS, problem.getProblemType());
        assertEquals(21L, problem.getKnowledgePointId());
        assertEquals("CONFIRMED", problem.getStatus());
        assertEquals("乡村振兴·要点完整性问题", problem.getTitle());
        assertTrue(problem.getEvidenceJson().contains("\"topicCode\":\"THEME_RURAL\""));
        assertTrue(problem.getEvidenceJson().contains("\"topicKnowledgePointId\":77"));
        assertTrue(problem.getEvidenceJson().contains("生态保护"));
        verify(problemMapper, never()).update(any());
        verify(problemMapper).insertHistory(any(), isNull(), eq(NOW));
    }

    @Test
    void lowAnalysisDimensionCreatesAnalysisProblem() {
        service.evaluate(7L, question("ANALYSIS"),
                result(dims(70.0, EssayConstants.DIM_ANALYSIS, 35.0), Collections.emptyList()), NOW);

        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).insert(captor.capture());
        assertEquals(LearningProblemEngine.ESSAY_ANALYSIS, captor.getValue().getProblemType());
        assertEquals(22L, captor.getValue().getKnowledgePointId());
        assertEquals("CONFIRMED", captor.getValue().getStatus());
        assertTrue(captor.getValue().getTitle().contains("综合分析问题"));
    }

    @Test
    void lowSummaryDimensionCreatesStructureProblem() {
        service.evaluate(7L, question("SUMMARY"),
                result(dims(70.0, EssayConstants.DIM_SUMMARY, 35.0), Collections.emptyList()), NOW);

        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).insert(captor.capture());
        assertEquals(LearningProblemEngine.ESSAY_STRUCTURE, captor.getValue().getProblemType());
        assertEquals(23L, captor.getValue().getKnowledgePointId());
        assertEquals("乡村振兴·归纳概括结构问题", captor.getValue().getTitle());
    }

    @Test
    void dimensionsAboveThresholdCreateNoProblem() {
        service.evaluate(7L, question("ANALYSIS"),
                result(dims(70.0, EssayConstants.DIM_ANALYSIS, 70.0), Collections.emptyList()), NOW);

        verify(problemMapper, never()).insert(any());
        verify(problemMapper, never()).update(any());
        verify(problemMapper, never()).insertHistory(any(), any(), any());
    }

    @Test
    void existingProblemMovesToVerifyingAndWritesHistory() {
        when(problemMapper.findForUpdate(7L, 21L, LearningProblemEngine.ESSAY_MISSING_POINTS))
                .thenReturn(problem(88L, "CONFIRMED", 0, 0));
        service.evaluate(7L, question("ANALYSIS"),
                result(dims(58.0, EssayConstants.DIM_ANALYSIS, 70.0), Collections.emptyList()), NOW);

        verify(problemMapper, never()).insert(any());
        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).update(captor.capture());
        assertEquals("VERIFYING", captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getValidationCount().intValue());
        assertEquals(0, captor.getValue().getValidationPassCount().intValue());
        verify(problemMapper).insertHistory(any(), eq("CONFIRMED"), eq(NOW));
    }

    @Test
    void secondConsecutivePassResolvesProblem() {
        when(problemMapper.findForUpdate(7L, 21L, LearningProblemEngine.ESSAY_MISSING_POINTS))
                .thenReturn(problem(88L, "VERIFYING", 1, 1));
        service.evaluate(7L, question("ANALYSIS"),
                result(dims(65.0, EssayConstants.DIM_ANALYSIS, 70.0), Collections.emptyList()), NOW);

        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).update(captor.capture());
        assertEquals("RESOLVED", captor.getValue().getStatus());
        assertEquals(2, captor.getValue().getValidationPassCount().intValue());
        assertEquals(NOW, captor.getValue().getResolvedTime());
        verify(problemMapper).insertHistory(any(), eq("VERIFYING"), eq(NOW));
    }

    @Test
    void sameProblemRowIsReusedInsteadOfDuplicating() {
        when(problemMapper.findForUpdate(7L, 21L, LearningProblemEngine.ESSAY_MISSING_POINTS))
                .thenReturn(problem(88L, "OBSERVING", 0, 0));
        service.evaluate(7L, question("ANALYSIS"),
                result(dims(30.0, EssayConstants.DIM_ANALYSIS, 70.0), Collections.emptyList()), NOW);

        verify(problemMapper, never()).insert(any());
        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).update(captor.capture());
        assertEquals(88L, captor.getValue().getId());
        assertEquals("CONFIRMED", captor.getValue().getStatus());
        verify(problemMapper).insertHistory(any(), eq("OBSERVING"), eq(NOW));
    }

    @Test
    void contentGapConfirmedWhenThemeWeakAndNothingRead() {
        when(abilityMapper.findForUpdate(7L, 77L)).thenReturn(themeProfile(1, 40.0));
        when(readingRecordMapper.countCompletedByUserAndTopicKpId(7L, 77L)).thenReturn(0);

        service.evaluateContentGap(7L, 77L, "THEME_RURAL", "乡村振兴", NOW);

        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).insert(captor.capture());
        LearningProblem problem = captor.getValue();
        assertEquals(LearningProblemEngine.CONTENT_GAP, problem.getProblemType());
        assertEquals(77L, problem.getKnowledgePointId());
        assertEquals("CONFIRMED", problem.getStatus());
        assertEquals("《乡村振兴》主题素材缺口", problem.getTitle());
        assertEquals(0, problem.getSeverity().compareTo(BigDecimal.valueOf(20)));
        assertTrue(problem.getEvidenceJson().contains("\"completedMaterials\":0"));
        assertTrue(problem.getEvidenceJson().contains("THEME_RURAL"));
        verify(problemMapper).insertHistory(any(), isNull(), eq(NOW));
    }

    @Test
    void contentGapResolvedOnceMaterialCompleted() {
        when(problemMapper.findForUpdate(7L, 77L, LearningProblemEngine.CONTENT_GAP))
                .thenReturn(contentGapProblem(89L, "CONFIRMED"));
        when(abilityMapper.findForUpdate(7L, 77L)).thenReturn(themeProfile(1, 40.0));
        when(readingRecordMapper.countCompletedByUserAndTopicKpId(7L, 77L)).thenReturn(1);

        service.evaluateContentGap(7L, 77L, "THEME_RURAL", "乡村振兴", NOW);

        verify(problemMapper, never()).insert(any());
        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).update(captor.capture());
        assertEquals("RESOLVED", captor.getValue().getStatus());
        assertEquals(NOW, captor.getValue().getResolvedTime());
        assertTrue(captor.getValue().getEvidenceJson().contains("\"completedMaterials\":1"));
        verify(problemMapper).insertHistory(any(), eq("CONFIRMED"), eq(NOW));
    }

    @Test
    void contentGapObservingWhenThemeMasteryRecovers() {
        when(problemMapper.findForUpdate(7L, 77L, LearningProblemEngine.CONTENT_GAP))
                .thenReturn(contentGapProblem(89L, "CONFIRMED"));
        when(abilityMapper.findForUpdate(7L, 77L)).thenReturn(themeProfile(2, 60.0));
        when(readingRecordMapper.countCompletedByUserAndTopicKpId(7L, 77L)).thenReturn(0);

        service.evaluateContentGap(7L, 77L, "THEME_RURAL", "乡村振兴", NOW);

        ArgumentCaptor<LearningProblem> captor = ArgumentCaptor.forClass(LearningProblem.class);
        verify(problemMapper).update(captor.capture());
        assertEquals("OBSERVING", captor.getValue().getStatus());
        verify(problemMapper).insertHistory(any(), eq("CONFIRMED"), eq(NOW));
    }

    @Test
    void contentGapSkippedWithoutThemeProfileOrSamples() {
        when(abilityMapper.findForUpdate(7L, 77L)).thenReturn(null);
        service.evaluateContentGap(7L, 77L, "THEME_RURAL", "乡村振兴", NOW);
        verify(readingRecordMapper, never()).countCompletedByUserAndTopicKpId(any(), any());
        verify(problemMapper, never()).insert(any());

        when(abilityMapper.findForUpdate(7L, 77L)).thenReturn(themeProfile(0, 40.0));
        when(readingRecordMapper.countCompletedByUserAndTopicKpId(7L, 77L)).thenReturn(0);
        service.evaluateContentGap(7L, 77L, "THEME_RURAL", "乡村振兴", NOW);
        verify(problemMapper, never()).insert(any());
        verify(problemMapper, never()).update(any());
    }

    private EssayQuestion question(String type) {
        EssayQuestion question = new EssayQuestion();
        question.setId(1001L); question.setQuestionType(type);
        question.setTopicKnowledgePointId(77L); question.setTopicCode("THEME_RURAL");
        question.setTopicName("乡村振兴"); question.setVersion(1);
        return question;
    }

    private Map<String, Double> dims(double completeness, String primaryCode, double primaryScore) {
        Map<String, Double> dims = new LinkedHashMap<>();
        dims.put(EssayConstants.DIM_POINT_COMPLETENESS, completeness);
        dims.put(primaryCode, primaryScore);
        dims.put(EssayConstants.DIM_EXPRESSION, 70.0);
        return dims;
    }

    private EssayEvaluationResult result(Map<String, Double> dimensionScores, List<String> missingPoints) {
        return new EssayEvaluationResult("LOCAL_RULE_V1", 55.0, dimensionScores, Collections.emptyList(),
                Collections.emptyList(), missingPoints, Collections.emptyList(), Collections.emptyMap());
    }

    private LearningProblem problem(Long id, String status, int validationCount, int validationPassCount) {
        LearningProblem problem = new LearningProblem();
        problem.setId(id); problem.setUserId(7L); problem.setKnowledgePointId(21L);
        problem.setProblemType(LearningProblemEngine.ESSAY_MISSING_POINTS); problem.setStatus(status);
        problem.setValidationCount(validationCount); problem.setValidationPassCount(validationPassCount);
        problem.setDiscoveredTime(NOW.minusDays(1));
        return problem;
    }

    private LearningProblem contentGapProblem(Long id, String status) {
        LearningProblem problem = new LearningProblem();
        problem.setId(id); problem.setUserId(7L); problem.setKnowledgePointId(77L);
        problem.setProblemType(LearningProblemEngine.CONTENT_GAP); problem.setStatus(status);
        problem.setValidationCount(0); problem.setValidationPassCount(0);
        problem.setDiscoveredTime(NOW.minusDays(2));
        return problem;
    }

    private AbilityProfile dimensionProfile() {
        AbilityProfile profile = new AbilityProfile();
        profile.setUserId(7L); profile.setKnowledgePointId(21L); profile.setSampleCount(2);
        profile.setMasteryScore(BigDecimal.valueOf(60));
        return profile;
    }

    private AbilityProfile themeProfile(int samples, double mastery) {
        AbilityProfile profile = new AbilityProfile();
        profile.setUserId(7L); profile.setKnowledgePointId(77L); profile.setSampleCount(samples);
        profile.setMasteryScore(BigDecimal.valueOf(mastery));
        profile.setSpeedScore(BigDecimal.valueOf(50)); profile.setStabilityScore(BigDecimal.valueOf(50));
        profile.setConfidenceScore(BigDecimal.valueOf(30));
        return profile;
    }
}
