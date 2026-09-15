function request(options) {
  const app = getApp()
  const showLoading = options.showLoading !== false
  if (showLoading) wx.showLoading({ title: '加载中', mask: true })
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data,
      timeout: options.timeout || 10000,
      header: Object.assign({
        'content-type': 'application/json',
        'X-User-Id': String(app.globalData.currentUserId)
      }, options.header || {}),
      success(response) {
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error('服务暂时不可用（' + response.statusCode + '）'))
          return
        }
        if (!response.data || !response.data.success) {
          reject(new Error((response.data && response.data.message) || '请求失败'))
          return
        }
        resolve(response.data.data)
      },
      fail(error) { reject(new Error(error.errMsg || '网络连接失败')) },
      complete() { if (showLoading) wx.hideLoading() }
    })
  }).catch(error => {
    if (!options.silent) wx.showToast({ title: error.message, icon: 'none', duration: 2500 })
    throw error
  })
}

module.exports = { request }
