const { request } = require('../../utils/request')
const { abilityLabel, evaluatorLabel } = require('../../utils/display')

const FIELD_LABELS = {
  MASTERY: '掌握',
  SPEED: '速度',
  STABILITY: '稳定性',
  CONFIDENCE: '可信度'
}

function normalizeChanges(changes) {
  return (changes || []).map(change => {
    let line
    if (change.field !== undefined && change.field !== null) {
      line = (FIELD_LABELS[change.field] || '能力') + ' ' +
        (change.before == null ? '-' : change.before) + ' → ' + (change.after == null ? '-' : change.after)
    } else {
      line = '掌握 ' + (change.oldMastery == null ? '-' : change.oldMastery) + ' → ' + (change.newMastery == null ? '-' : change.newMastery) +
        ' · 速度 ' + (change.oldSpeed == null ? '-' : change.oldSpeed) + ' → ' + (change.newSpeed == null ? '-' : change.newSpeed)
    }
    return {
      knowledgePointCode: change.knowledgePointCode,
      name: change.knowledgePointName || abilityLabel(change.knowledgePointCode),
      line
    }
  })
}

function normalizeResult(result) {
  const evaluation = (result && result.evaluation) || {}
  const score = Number(evaluation.totalScore)
  const strengths = evaluation.strengths || []
  const problems = evaluation.problems || []
  const missingPoints = evaluation.missingPoints || []
  const suggestions = evaluation.suggestions || []
  const evidence = (evaluation.evidence && evaluation.evidence.items) || []
  const abilityChanges = normalizeChanges(result && result.abilityChanges)
  const hiddenMore = [strengths, problems, missingPoints, suggestions]
    .reduce((sum, list) => sum + Math.max(0, list.length - 3), 0)
  return {
    answerId: result && result.essayAnswerId,
    failed: result && result.gradingStatus === 'FAILED',
    retryable: !!(result && result.retryable),
    message: result && result.message,
    localEvaluator: evaluation.evaluator === 'LOCAL_RULE_V1',
    aiEvaluator: evaluation.evaluator === 'REAL_AI',
    evaluatorLabel: evaluation.evaluator ? evaluatorLabel(evaluation.evaluator) : '',
    provider: evaluation.provider,
    model: evaluation.model,
    promptVersion: evaluation.promptVersion,
    confidence: evaluation.confidence,
    totalScore: evaluation.totalScore,
    totalScorePercent: isNaN(score) ? 0 : Math.max(0, Math.min(100, score)),
    dimensionScores: (evaluation.dimensionScores || []).map(dimension => Object.assign({}, dimension, {
      scorePercent: Math.max(0, Math.min(100, Number(dimension.score) || 0))
    })),
    strengths,
    problems,
    missingPoints,
    suggestions,
    evidence,
    abilityChanges,
    // 首屏精修：每个区块最多 3 条，其余内容进“查看完整批改”
    topStrengths: strengths.slice(0, 3),
    topProblems: problems.slice(0, 3),
    topMissingPoints: missingPoints.slice(0, 3),
    topSuggestions: suggestions.slice(0, 3),
    hiddenMore,
    hasDetail: hiddenMore > 0 || evidence.length > 0 || abilityChanges.length > 0
  }
}

Page({
  data: {
    loading: true, error: '', question: null, suggestedMinutes: null, materialExpanded: true,
    answerText: '', charCount: 0, submitting: false, result: null, detailExpanded: false
  },
  onLoad(options) {
    this.itemId = options.itemId ? Number(options.itemId) : null
    this.questionId = options.questionId ? Number(options.questionId) : null
    this.load()
  },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const url = this.itemId
        ? '/api/plan/items/' + this.itemId + '/essay-question'
        : '/api/essay/questions/' + this.questionId
      const question = await request({ url, showLoading: false })
      this.setData({
        loading: false, question,
        suggestedMinutes: question.standardTimeSeconds ? Math.max(1, Math.round(question.standardTimeSeconds / 60)) : null
      })
      this.resetAnswer()
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  toggleMaterial() { this.setData({ materialExpanded: !this.data.materialExpanded }) },
  toggleDetail() { this.setData({ detailExpanded: !this.data.detailExpanded }) },
  onInput(event) {
    const text = event.detail.value || ''
    this.setData({ answerText: text, charCount: text.replace(/\s/g, '').length })
  },
  async submit() {
    if (this.data.submitting || this.data.result) return
    if (!this.data.answerText.trim()) {
      wx.showToast({ title: '请先输入作答内容', icon: 'none' }); return
    }
    this.setData({ submitting: true })
    try {
      const result = await request({
        url: '/api/essay/submit', method: 'POST',
        data: {
          essayQuestionId: this.data.question.id,
          answerText: this.data.answerText,
          durationMs: Date.now() - this.startedAt,
          practiceType: 'DAILY'
        }
      })
      this.setData({ result: normalizeResult(result), submitting: false, detailExpanded: false })
    } catch (error) {
      this.setData({ submitting: false, error: error.message })
    }
  },
  async retryGrade() {
    if (!this.data.result || !this.data.result.answerId || this.data.submitting) return
    this.setData({ submitting: true, error: '' })
    try {
      const result = await request({ url: '/api/essay/answers/' + this.data.result.answerId + '/retry', method: 'POST' })
      this.setData({ result: normalizeResult(result), submitting: false, detailExpanded: false })
    } catch (error) {
      this.setData({ submitting: false, error: error.message })
    }
  },
  finish() { wx.navigateBack() },
  resetAnswer() {
    this.startedAt = Date.now()
    this.setData({ answerText: '', charCount: 0, result: null, submitting: false, error: '' })
  }
})
