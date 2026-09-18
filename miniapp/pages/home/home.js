const { request } = require('../../utils/request')
const { purposeLabel, taskStatusLabel } = require('../../utils/display')

Page({
  data: {
    loading: true, error: '', plan: null, overview: null, insight: null,
    completedCount: 0, progressPercent: 0, currentItem: null, remainingCount: 0
  },
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
      const items = (plan.items || []).map(item => Object.assign({}, item, {
        purposeLabel: purposeLabel(item.purpose),
        statusLabel: taskStatusLabel(item.status)
      }))
      plan.items = items
      const completedCount = items.filter(item => item.status === 'COMPLETED').length
      const currentItem = items.find(item => item.status !== 'COMPLETED' && item.status !== 'INSUFFICIENT_STOCK') ||
        items.find(item => item.status !== 'COMPLETED') || items[0] || null
      this.setData({ plan, overview, insight, completedCount,
        currentItem, remainingCount: Math.max(0, items.length - completedCount),
        progressPercent: items.length ? Math.round(completedCount * 100 / items.length) : 0,
        loading: false })
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  openItem(item, startPractice) {
    if (!item) return
    const id = Number(item.id)
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
    app.globalData.pendingStartPractice = Boolean(startPractice)
    wx.switchTab({ url: '/pages/learn/learn' })
  },
  startItem(event) {
    const items = (this.data.plan && this.data.plan.items) || []
    this.openItem(items.find(candidate => candidate.id === Number(event.currentTarget.dataset.id)), true)
  },
  startFirst() {
    this.openItem(this.data.currentItem, true)
  },
  viewTodayTasks() {
    const app = getApp()
    app.globalData.pendingPlanItemId = this.data.currentItem ? this.data.currentItem.id : null
    app.globalData.pendingStartPractice = false
    wx.switchTab({ url: '/pages/learn/learn' })
  },
  openCoach() { wx.navigateTo({ url: '/pages/coach/coach' }) }
})
