const { request } = require('../../utils/request')

const PURPOSE_LABELS = {
  TRAINING: '专项训练',
  REVIEW: '巩固复习',
  VALIDATION: '问题验证'
}

Page({
  data: { loading: true, error: '', planItems: [], questions: [] },
  onShow() { this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const tasks = await request({ url: '/api/essay/tasks', showLoading: false })
      const planItems = (tasks.planItems || []).map(item => Object.assign({}, item, {
        purposeLabel: PURPOSE_LABELS[item.purpose] || item.purpose || '专项训练'
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
