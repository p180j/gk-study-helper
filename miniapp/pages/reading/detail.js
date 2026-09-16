const { request } = require('../../utils/request')

Page({
  data: { loading: true, error: '', material: null, acting: false },
  onLoad(options) {
    this.itemId = options.itemId ? Number(options.itemId) : null
    this.materialId = options.materialId ? Number(options.materialId) : null
    this.load()
  },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const url = this.itemId
        ? '/api/plan/items/' + this.itemId + '/reading-material'
        : '/api/reading/materials/' + this.materialId
      const material = await request({ url, showLoading: false })
      this.setData({ loading: false, material })
      this.startedAt = Date.now()
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  markRead() { this.update({ readStatus: 'COMPLETED' }) },
  toggleFavorite() {
    if (!this.data.material) return
    this.update({ favorite: !this.data.material.favorite })
  },
  toggleMastered() {
    if (!this.data.material) return
    this.update({ masteryLevel: this.data.material.masteryLevel === 'MASTERED' ? 'UNMASTERED' : 'MASTERED' })
  },
  async update(fields) {
    if (this.data.acting || !this.data.material) return
    this.setData({ acting: true })
    try {
      const material = await request({
        url: '/api/reading/records', method: 'POST',
        data: Object.assign({
          materialId: this.data.material.id,
          durationMs: Date.now() - this.startedAt
        }, fields)
      })
      this.setData({ material: material || this.data.material, acting: false })
    } catch (error) {
      this.setData({ acting: false, error: error.message })
    }
  }
})
