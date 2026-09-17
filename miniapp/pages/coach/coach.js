const { request } = require('../../utils/request')

function localize(text) {
  return String(text || '')
    .replace(/mastery=/gi, '掌握度 ')
    .replace(/speed=/gi, '速度 ')
    .replace(/stability=/gi, '稳定性 ')
    .replace(/confidence=/gi, '可信度 ')
    .replace(/sample[_ ]?count=/gi, '样本数 ')
    .replace(/未评估状态/g, '能力画像建立中')
}

function localizeResult(result) {
  return Object.assign({}, result, {
    currentStatus: localize(result.currentStatus),
    todayReason: localize(result.todayReason),
    answer: localize(result.answer),
    coreProblems: (result.coreProblems || []).map(localize),
    evidence: (result.evidence || []).map(localize)
  })
}

// 三层摘要：结论（真实数据洞察）+ 关键证据（AI 证据优先，失败时回退洞察证据）+ 下一步建议
function buildSummary(insight, result) {
  if (!insight) return null
  const evidenceSource = result && result.evidence && result.evidence.length ? result.evidence : (insight.evidence || [])
  return {
    problem: insight.problem,
    suggestion: insight.suggestion,
    evidence: evidenceSource.slice(0, 4)
  }
}

Page({
  data: { loading: true, insight: null, summary: null, result: null, error: '', question: '', asking: false, detailExpanded: false },
  onLoad() { this.loadInsight(); this.ask() },
  async loadInsight() {
    try {
      const insight = await request({ url: '/api/ai/coach/insight', showLoading: false, silent: true })
      this.setData({ insight, summary: buildSummary(insight, this.data.result), loading: false })
    } catch (error) {
      this.setData({ loading: false })
    }
  },
  onInput(event) { this.setData({ question: event.detail.value || '' }) },
  async ask() {
    if (this.data.asking) return
    this.setData({ asking: true, error: '' })
    try {
      const result = await request({
        url: '/api/ai/coach', method: 'POST', showLoading: false,
        data: { question: this.data.question.trim() }
      })
      const localized = localizeResult(result)
      this.setData({
        result: localized,
        summary: buildSummary(this.data.insight, localized),
        asking: false, question: '', detailExpanded: false
      })
    } catch (error) {
      this.setData({ asking: false, error: error.message })
    }
  },
  toggleDetail() { this.setData({ detailExpanded: !this.data.detailExpanded }) }
})
