App({
  globalData: {
    baseUrl: 'http://127.0.0.1:8089',
    currentUserId: 1,
    pendingPlanItemId: null
  },
  onLaunch() {
    const baseUrl = wx.getStorageSync('apiBaseUrl')
    const userId = wx.getStorageSync('currentUserId')
    if (baseUrl) this.globalData.baseUrl = baseUrl
    if (userId) this.globalData.currentUserId = Number(userId)
  }
})
