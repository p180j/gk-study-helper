const { request } = require('../../utils/request')

Page({
  data: { loading: true, error: '', result: null, question: '', asking: false },
  onLoad() { this.ask() },
  onInput(event) { this.setData({ question: event.detail.value || '' }) },
  async ask() {
    if (this.data.asking) return
    this.setData({ loading: !this.data.result, asking: true, error: '' })
    try {
      const result = await request({
        url: '/api/ai/coach', method: 'POST', showLoading: false,
        data: { question: this.data.question.trim() }
      })
      this.setData({ result, loading: false, asking: false, question: '' })
    } catch (error) {
      this.setData({ loading: false, asking: false, error: error.message })
    }
  }
})
