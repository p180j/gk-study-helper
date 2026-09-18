const { request } = require('../../utils/request')
const { localizedText } = require('../../utils/display')

function localize(text) {
  return localizedText(String(text || ''))
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

// 三层摘要：结论（真实数据洞察）+ 关键证据（最多 3 条，AI 证据优先，失败时回退洞察证据）+ 下一步建议
function buildSummary(insight, result) {
  if (!insight) return null
  const evidenceSource = result && result.evidence && result.evidence.length ? result.evidence : (insight.evidence || [])
  return {
    problem: insight.problem,
    suggestion: insight.suggestion,
    evidence: evidenceSource.slice(0, 3)
  }
}

Page({
  data: { loading: true, insight: null, summary: null, result: null, error: '', question: '', asking: false, detailExpanded: false },
  // 先展示基于真实数据的洞察；用户点击后才调用 AI，避免页面一打开就因外部服务短暂失败而不可用。
  onLoad() { this.loadInsight() },
  async loadInsight() {
    try {
      const insight = await request({ url: '/api/ai/coach/insight', showLoading: false, silent: true })
      const localizedInsight = Object.assign({}, insight, {
        problem: localize(insight.problem), suggestion: localize(insight.suggestion), evidence: (insight.evidence || []).map(localize)
      })
      this.setData({ insight: localizedInsight, summary: buildSummary(localizedInsight, this.data.result), loading: false })
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
