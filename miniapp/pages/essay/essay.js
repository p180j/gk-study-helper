const { request } = require('../../utils/request')
const { purposeLabel } = require('../../utils/display')

Page({
  data: { loading: true, error: '', planItems: [], questions: [] },
  onShow() { this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const tasks = await request({ url: '/api/essay/tasks', showLoading: false })
      const planItems = (tasks.planItems || []).map(item => Object.assign({}, item, {
        purposeLabel: purposeLabel(item.purpose)
      }))
      this.setData({ planItems, questions: tasks.questions || [], loading: false })
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  startPlanItem(event) {
    wx.navigateTo({ url: '/pages/essay/answer?itemId=' + event.currentTarget.dataset.id })
  },
  startQuestion(event) {
    wx.navigateTo({ url: '/pages/essay/answer?questionId=' + event.currentTarget.dataset.id })
  }
})
