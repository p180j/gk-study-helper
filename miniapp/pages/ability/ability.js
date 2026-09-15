const { request } = require('../../utils/request')

Page({
  data: { loading: true, error: '', abilities: [], problems: [] },
  onShow() { this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [abilities, problems] = await Promise.all([
        request({ url: '/api/abilities', showLoading: false }),
        request({ url: '/api/learning-problems', showLoading: false })
      ])
      const normalized = problems.map(item => {
        let evidence = item.evidenceJson || ''
        try {
          const value = JSON.parse(evidence)
          evidence = '最近' + (value.recentCount || 0) + '题，错' + (value.incorrectCount || 0) +
            '题；掌握 ' + (value.mastery || '-') + '，速度 ' + (value.speed || '-') +
            '，稳定性 ' + (value.stability || '-') + '，可信度 ' + (value.confidence || '-')
        } catch (ignore) {}
        return Object.assign({}, item, { evidenceText: evidence })
      })
      this.setData({ abilities, problems: normalized, loading: false })
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  }
})
