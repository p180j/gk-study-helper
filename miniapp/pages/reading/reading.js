const { request } = require('../../utils/request')

Page({
  data: { loading: true, error: '', topics: [], currentTopic: null, materials: [] },
  onShow() {
    if (this.data.currentTopic) this.loadMaterials()
    else this.loadTopics()
  },
  async loadTopics() {
    this.setData({ loading: true, error: '', currentTopic: null })
    try {
      const topics = await request({ url: '/api/reading/topics', showLoading: false })
      this.setData({
        topics: (topics || []).map(topic => Object.assign({}, topic, {
          progressPercent: topic.totalMaterials ? Math.min(100, Math.round(topic.completedMaterials / topic.totalMaterials * 100)) : 0
        })),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  openTopic(event) {
    const topic = this.data.topics.find(candidate => candidate.id === Number(event.currentTarget.dataset.id))
    if (!topic) return
    this.setData({ currentTopic: topic })
    this.loadMaterials()
  },
  async loadMaterials() {
    const topic = this.data.currentTopic
    if (!topic) return
    this.setData({ loading: true, error: '' })
    try {
      const materials = await request({ url: '/api/reading/materials?topicId=' + topic.id, showLoading: false })
      this.setData({ materials: materials || [], loading: false })
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  backToTopics() { this.loadTopics() },
  retry() {
    if (this.data.currentTopic) this.loadMaterials()
    else this.loadTopics()
  },
  openMaterial(event) {
    wx.navigateTo({ url: '/pages/reading/detail?materialId=' + event.currentTarget.dataset.id })
  }
})
