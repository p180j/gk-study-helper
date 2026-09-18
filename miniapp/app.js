App({
  globalData: {
    baseUrl: 'http://127.0.0.1:8089',
    currentUserId: 1,
    pendingPlanItemId: null,
    // 首页“继续今日学习”可直接进做题；任务列表进入学习工作台时保持 false。
    pendingStartPractice: false,
    // 能力页“去针对训练”使用真实自由练习链路，不污染当天计划任务。
    pendingExtraPractice: null
  },
  onLaunch() {
    const baseUrl = wx.getStorageSync('apiBaseUrl')
    const userId = wx.getStorageSync('currentUserId')
    if (baseUrl) this.globalData.baseUrl = baseUrl
    if (userId) this.globalData.currentUserId = Number(userId)
  }
})
