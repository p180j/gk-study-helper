(function () {
  const content = document.getElementById('content')
  const status = document.getElementById('status')
  const title = document.getElementById('pageTitle')
  const dialog = document.getElementById('detailDialog')
  const titles = { questions: '题库', import: '题目导入', knowledge: '知识点', answers: '答题记录' }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;').replaceAll("'", '&#039;')
  }
  function value(value, fallback) { return value == null || value === '' ? (fallback || '—') : value }
  function setBusy(busy) { status.textContent = busy ? '正在读取真实数据…' : '就绪'; status.className = 'status ' + (busy ? 'busy' : '') }
  function showError(message) {
    status.textContent = message; status.className = 'status error'
    content.innerHTML = '<div class="notice error">请求失败：' + escapeHtml(message) + ' <button id="retry">重试</button></div>'
    document.getElementById('retry').onclick = () => loadView(currentView)
  }
  function showEmpty(text) { content.innerHTML = '<div class="empty">' + escapeHtml(text) + '</div>' }

  let currentView = 'questions'
  async function loadView(view) {
    currentView = view; title.textContent = titles[view]
    document.querySelectorAll('nav button').forEach(button => button.classList.toggle('active', button.dataset.view === view))
    if (view !== 'import') content.innerHTML = '<div class="empty">正在读取真实数据…</div>'
    try {
      if (view === 'questions') await renderQuestions()
      if (view === 'import') renderImport()
      if (view === 'knowledge') await renderKnowledge()
      if (view === 'answers') await renderAnswers()
    } catch (ignore) {}
  }

  async function renderQuestions(filters) {
    const query = new URLSearchParams(Object.assign({ page: 1, size: 50 }, filters || {}))
    Array.from(query.keys()).forEach(key => { if (!query.get(key)) query.delete(key) })
    const questions = await Api.request('/api/questions?' + query)
    content.innerHTML = `
      <form id="questionFilters" class="toolbar">
        <input name="keyword" placeholder="题干或来源关键词" value="${escapeHtml((filters || {}).keyword || '')}">
        <select name="questionType"><option value="">全部题型</option><option>SINGLE</option></select>
        <select name="usageType"><option value="">全部用途</option><option>TRAINING</option><option>VALIDATION</option><option>MOCK_RESERVED</option></select>
        <select name="status"><option value="">全部状态</option><option>ACTIVE</option><option>DRAFT</option><option>SUSPENDED</option><option>ARCHIVED</option></select>
        <button class="primary">查询</button>
      </form>
      <div class="table-wrap"><table><thead><tr><th>ID</th><th>题型</th><th>题干摘要</th><th>来源</th><th>用途</th><th>难度</th><th>状态</th><th>版本</th><th>创建时间</th></tr></thead>
      <tbody>${questions.map(item => `<tr data-question-id="${item.id}"><td>${item.id}</td><td>${escapeHtml(item.questionType)}</td><td class="stem">${escapeHtml(item.stem)}</td><td>${escapeHtml(value(item.sourceName, item.sourceType))}</td><td>${escapeHtml(item.usageType)}</td><td>${item.difficulty}</td><td><span class="pill">${escapeHtml(item.status)}</span></td><td>v${item.version}</td><td>${escapeHtml(value(item.createTime))}</td></tr>`).join('')}</tbody></table></div>
      ${questions.length ? '' : '<div class="empty">没有符合条件的题目</div>'}`
    document.getElementById('questionFilters').onsubmit = event => {
      event.preventDefault(); renderQuestions(Object.fromEntries(new FormData(event.target)))
    }
    content.querySelectorAll('[data-question-id]').forEach(row => {
      row.onclick = () => openQuestion(Number(row.dataset.questionId))
    })
  }

  async function openQuestion(id) {
    const item = await Api.request('/api/admin/questions/' + id)
    document.getElementById('detailContent').innerHTML = `
      <div class="eyebrow">题目 #${item.id} · ${escapeHtml(item.questionType)} · v${item.version}</div>
      <h2>${escapeHtml(item.stem)}</h2>
      <div class="options">${item.options.map(option => '<div><b>' + escapeHtml(option.optionKey) + '</b>' + escapeHtml(option.optionText) + '</div>').join('')}</div>
      <dl>
        <dt>正确答案</dt><dd class="answer">${escapeHtml(item.answer)}</dd>
        <dt>解析</dt><dd>${escapeHtml(value(item.analysis, '暂无解析'))}</dd>
        <dt>知识点</dt><dd>${item.knowledgePoints.map(point => escapeHtml(point.name) + '（' + escapeHtml(point.code) + '）').join('、') || '—'}</dd>
        <dt>来源</dt><dd>${escapeHtml([item.sourceType, item.sourceYear, item.sourceExam, item.sourceName].filter(Boolean).join(' · '))}</dd>
        <dt>难度 / 参考耗时</dt><dd>${item.difficulty} / ${item.standardTimeSeconds} 秒</dd>
        <dt>用途 / 状态</dt><dd>${escapeHtml(item.usageType)} / ${escapeHtml(item.status)}</dd>
      </dl>`
    dialog.showModal()
  }

  function renderImport() {
    content.innerHTML = `
      <div class="panel narrow"><h2>CSV 批量导入</h2>
      <p>选择 UTF-8 CSV。单行失败不会回滚其他成功题目。</p>
      <code>stem,optionA,optionB,optionC,optionD,answer,analysis,knowledgeCode,difficulty,standardTimeSeconds,sourceType,sourceYear,sourceExam,sourceName,status,usageType,questionType</code>
      <form id="importForm"><label class="drop"><input name="file" type="file" accept=".csv,text/csv" required><span>选择 CSV 文件</span></label><button class="primary">开始导入</button></form>
      <div id="importResult"></div></div>`
    document.getElementById('importForm').onsubmit = async event => {
      event.preventDefault()
      const form = new FormData(event.target)
      try {
        const result = await Api.request('/api/questions/import', { method: 'POST', body: form })
        document.getElementById('importResult').innerHTML = `
          <div class="summary"><div><b>${result.totalCount}</b><span>总行数</span></div><div><b>${result.successCount}</b><span>成功</span></div><div><b>${result.failureCount}</b><span>失败</span></div></div>
          ${result.failures.length ? '<table><thead><tr><th>失败行</th><th>原因</th></tr></thead><tbody>' + result.failures.map(item => '<tr><td>' + item.row + '</td><td>' + escapeHtml(item.reason) + '</td></tr>').join('') + '</tbody></table>' : '<div class="notice success">全部导入成功，题目已写入数据库。</div>'}`
      } catch (ignore) {}
    }
  }

  async function renderKnowledge() {
    const points = await Api.request('/api/knowledge-points')
    if (!points.length) { showEmpty('暂无知识点'); return }
    content.innerHTML = '<div class="table-wrap"><table><thead><tr><th>层级</th><th>名称</th><th>Code</th><th>父级</th><th>重要度</th><th>提升收益</th><th>迁移价值</th><th>状态</th></tr></thead><tbody>' +
      points.map(item => '<tr><td>L' + item.level + '</td><td><span class="indent-' + item.level + '">' + escapeHtml(item.name) + '</span></td><td><code>' + escapeHtml(item.code) + '</code></td><td>' + escapeHtml(value(item.parentName)) + '</td><td>' + item.importance + '</td><td>' + item.improvementPotential + '</td><td>' + item.transferValue + '</td><td><span class="pill">' + escapeHtml(item.status) + '</span></td></tr>').join('') +
      '</tbody></table></div>'
  }

  async function renderAnswers() {
    const records = await Api.request('/api/practice/answers?page=1&size=100')
    content.innerHTML = '<div class="table-wrap"><table><thead><tr><th>题目</th><th>用户答案</th><th>答案快照</th><th>结果</th><th>耗时</th><th>场景</th><th>信心</th><th>错因</th><th>时间</th></tr></thead><tbody>' +
      records.map(item => '<tr><td>#' + item.questionId + '</td><td>' + escapeHtml(item.userAnswer) + '</td><td>' + escapeHtml(item.correctAnswerSnapshot) + '</td><td><span class="pill ' + (item.correct ? 'good' : 'bad') + '">' + (item.correct ? '正确' : '错误') + '</span></td><td>' + item.durationMs + ' ms</td><td>' + escapeHtml(item.practiceType) + '</td><td>' + escapeHtml(item.confidenceType) + '</td><td>' + escapeHtml(item.errorType) + '</td><td>' + escapeHtml(item.answerTime) + '</td></tr>').join('') +
      '</tbody></table></div>' + (records.length ? '' : '<div class="empty">当前用户暂无答题记录</div>')
  }

  window.AdminApp = { setBusy, showError }
  document.querySelectorAll('nav button').forEach(button => button.onclick = () => loadView(button.dataset.view))
  document.querySelector('#detailDialog .close').onclick = () => dialog.close()
  document.getElementById('baseUrl').value = Api.context.baseUrl
  document.getElementById('userId').value = Api.context.userId
  document.getElementById('saveContext').onclick = () => {
    Api.saveContext(document.getElementById('baseUrl').value, document.getElementById('userId').value)
    loadView(currentView)
  }
  loadView('questions')
})()
