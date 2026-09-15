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
    const app = getApp()
    app.globalData.pendingPlanItemId = Number(event.currentTarget.dataset.id)
    wx.switchTab({ url: '/pages/learn/learn' })
  },
  startFirst() {
    const items = (this.data.plan && this.data.plan.items) || []
    if (!items.length) return
    this.startItem({ currentTarget: { dataset: { id: items[0].id } } })
  }
})
