(function () {
  const context = {
    baseUrl: localStorage.getItem('gk.apiBaseUrl') || 'http://127.0.0.1:8080',
    userId: Number(localStorage.getItem('gk.currentUserId') || 1)
  }

  async function request(path, options) {
    const settings = options || {}
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), settings.timeout || 10000)
    window.AdminApp.setBusy(true)
    try {
      const headers = Object.assign({ 'X-User-Id': String(context.userId) }, settings.headers || {})
      if (settings.body && !(settings.body instanceof FormData)) headers['Content-Type'] = 'application/json'
      const response = await fetch(context.baseUrl + path, {
        method: settings.method || 'GET',
        headers,
        signal: controller.signal,
        body: settings.body instanceof FormData ? settings.body : (settings.body ? JSON.stringify(settings.body) : undefined)
      })
      let payload
      try { payload = await response.json() } catch (error) { throw new Error('服务返回了无法解析的内容') }
      if (!response.ok) throw new Error(payload.message || 'HTTP ' + response.status)
      if (!payload.success) throw new Error(payload.message || '业务请求失败')
      window.AdminApp.setBusy(false)
      return payload.data
    } catch (error) {
      const actual = error.name === 'AbortError' ? new Error('请求超时，请检查后端服务') : error
      window.AdminApp.showError(actual.message)
      throw actual
    } finally {
      clearTimeout(timeout)
    }
  }

  function saveContext(baseUrl, userId) {
    context.baseUrl = baseUrl.replace(/\/$/, '')
    context.userId = Number(userId)
    localStorage.setItem('gk.apiBaseUrl', context.baseUrl)
    localStorage.setItem('gk.currentUserId', String(context.userId))
  }

  window.Api = { context, request, saveContext }
})()
