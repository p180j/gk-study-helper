const { request } = require('../../utils/request')
const { purposeLabel } = require('../../utils/display')

Page({
  data: { loading: true, error: '', plan: null, overview: null, completedCount: 0, progressPercent: 0, insight: null },
  onShow() { this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [plan, overview, insight] = await Promise.all([
        request({ url: '/api/plan/today', showLoading: false }),
        request({ url: '/api/abilities/overview', showLoading: false }),
        // 洞察接口失败时回退到“先建立基线”提示，不影响首页加载
        request({ url: '/api/ai/coach/insight', showLoading: false, silent: true }).catch(() => null)
      ])
      const items = (plan.items || []).map(item => Object.assign({}, item, { purposeLabel: purposeLabel(item.purpose) }))
      plan.items = items
      const completedCount = items.filter(item => item.status === 'COMPLETED').length
      this.setData({ plan, overview, insight, completedCount,
        progressPercent: items.length ? Math.round(completedCount * 100 / items.length) : 0,
        loading: false })
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  startItem(event) {
    const id = Number(event.currentTarget.dataset.id)
    const items = (this.data.plan && this.data.plan.items) || []
    const item = items.find(candidate => candidate.id === id)
    const itemType = item && item.itemType
    if (itemType === 'ESSAY') {
      wx.navigateTo({ url: '/pages/essay/answer?itemId=' + id })
      return
    }
    if (itemType === 'READING') {
      wx.navigateTo({ url: '/pages/reading/detail?itemId=' + id })
      return
    }
    const app = getApp()
    app.globalData.pendingPlanItemId = id
    wx.switchTab({ url: '/pages/learn/learn' })
  },
  startFirst() {
    const items = (this.data.plan && this.data.plan.items) || []
    if (!items.length) return
    this.startItem({ currentTarget: { dataset: { id: items[0].id } } })
  },
  openCoach() { wx.navigateTo({ url: '/pages/coach/coach' }) }
})
