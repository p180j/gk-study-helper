const { request } = require('../../utils/request')

Page({
  data: { loading: true, error: '', problems: [], plan: null },
  onShow() { this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [problems, plan] = await Promise.all([
        request({ url: '/api/learning-problems/core', showLoading: false }),
        request({ url: '/api/plan/today', showLoading: false })
      ])
      this.setData({ problems, plan, loading: false })
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
