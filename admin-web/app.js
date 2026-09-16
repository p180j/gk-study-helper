(function () {
  const content = document.getElementById('content')
  const status = document.getElementById('status')
  const title = document.getElementById('pageTitle')
  const dialog = document.getElementById('detailDialog')
  const formDialog = document.getElementById('formDialog')
  const titles = { questions: '题库', import: '题目导入', knowledge: '知识点', answers: '答题记录', essays: '申论题库', 'essay-answers': '申论作答', readings: '政治阅读' }
  const ESSAY_QUESTION_TYPES = ['SUMMARY', 'ANALYSIS', 'COUNTERMEASURE', 'IMPLEMENTATION']
  const ESSAY_TOPIC_CODES = ['HQ_DEVELOPMENT', 'TECH_INNOVATION', 'NEW_QUALITY_PRODUCTIVITY', 'RURAL_REVITALIZATION', 'GRASSROOTS_GOVERNANCE', 'PEOPLE_LIVELIHOOD', 'ECO_CIVILIZATION', 'CULTURE', 'GOVERNANCE', 'TALENT']
  const ESSAY_STATUSES = ['DRAFT', 'ACTIVE', 'ARCHIVED']
  const READING_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED']
  const READING_STATUS_LABELS = { PUBLISHED: '发布', DRAFT: '下架', ARCHIVED: '归档' }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;').replaceAll("'", '&#039;')
  }
  function value(value, fallback) { return value == null || value === '' ? (fallback || '—') : value }
  function options(list, selected) {
    return list.map(item => '<option value="' + escapeHtml(item) + '"' + (item === selected ? ' selected' : '') + '>' + escapeHtml(item) + '</option>').join('')
  }
  function toMinutes(durationMs) {
    if (durationMs == null) return '—'
    return (durationMs / 60000).toFixed(1) + ' 分钟'
  }
  function toLines(text) {
    return String(text || '').split('\n').map(line => line.trim()).filter(Boolean)
  }
  function listBlock(items) {
    const list = items || []
    return list.length ? '<ul class="detail-list">' + list.map(item => '<li>' + escapeHtml(item) + '</li>').join('') + '</ul>' : '—'
  }
  function parseReferencePoints(text) {
    return toLines(text).map(line => {
      const separator = line.indexOf('|')
      const point = (separator === -1 ? line : line.slice(0, separator)).trim()
      const keywords = separator === -1 ? '' : line.slice(separator + 1)
      return { point: point, keywords: keywords.split(/[,，、]/).map(keyword => keyword.trim()).filter(Boolean) }
    }).filter(item => item.point)
  }
  function evaluatorTag(evaluator) {
    if (evaluator === 'LOCAL_RULE_V1') return '<span class="pill">本地规则评分（测试实现）</span>'
    return escapeHtml(value(evaluator))
  }
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
      if (view === 'essays') await renderEssays()
      if (view === 'essay-answers') await renderEssayAnswers()
      if (view === 'readings') await renderReadings()
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

  let essayFilters = {}
  async function renderEssays(filters) {
    if (filters) essayFilters = filters
    const query = new URLSearchParams(Object.assign({ page: 1, pageSize: 50 }, essayFilters))
    Array.from(query.keys()).forEach(key => { if (!query.get(key)) query.delete(key) })
    const data = await Api.request('/api/admin/essay-questions?' + query)
    const list = (data && data.list) || []
    content.innerHTML = `
      <form id="essayFilters" class="toolbar">
        <input name="keyword" placeholder="题干或来源关键词" value="${escapeHtml(essayFilters.keyword || '')}">
        <select name="questionType"><option value="">全部题型</option>${options(ESSAY_QUESTION_TYPES, essayFilters.questionType)}</select>
        <select name="topicCode"><option value="">全部主题</option>${options(ESSAY_TOPIC_CODES, essayFilters.topicCode)}</select>
        <select name="status"><option value="">全部状态</option>${options(ESSAY_STATUSES, essayFilters.status)}</select>
        <button class="primary">查询</button>
        <button type="button" id="createEssayBtn" class="primary">新增申论题</button>
      </form>
      <div class="table-wrap"><table><thead><tr><th>ID</th><th>主题</th><th>题型</th><th>题目要求</th><th>字数限制</th><th>状态</th><th>来源</th><th>创建时间</th><th>操作</th></tr></thead>
      <tbody>${list.map(item => '<tr data-essay-id="' + item.id + '"><td>' + item.id + '</td><td>' + escapeHtml(value(item.topicName, item.topicCode)) + '</td><td>' + escapeHtml(value(item.questionTypeName, item.questionType)) + '</td><td class="stem">' + escapeHtml(item.promptPreview) + '</td><td>' + value(item.wordLimitMin) + ' ~ ' + value(item.wordLimitMax) + ' 字</td><td><span class="pill ' + (item.status === 'ACTIVE' ? 'good' : '') + '">' + escapeHtml(item.status) + '</span></td><td>' + escapeHtml(value(item.sourceName)) + '</td><td>' + escapeHtml(value(item.createTime)) + '</td><td><button class="mini" data-essay-detail="' + item.id + '">详情</button></td></tr>').join('')}</tbody></table></div>
      ${list.length ? '<div class="table-meta">共 ' + value(data && data.total) + ' 条</div>' : '<div class="empty">没有符合条件的申论题</div>'}`
    document.getElementById('essayFilters').onsubmit = event => {
      event.preventDefault(); renderEssays(Object.fromEntries(new FormData(event.target)))
    }
    document.getElementById('createEssayBtn').onclick = () => openEssayForm()
    content.querySelectorAll('[data-essay-id]').forEach(row => {
      row.onclick = () => openEssay(Number(row.dataset.essayId))
    })
    content.querySelectorAll('[data-essay-detail]').forEach(button => {
      button.onclick = event => { event.stopPropagation(); openEssay(Number(button.dataset.essayDetail)) }
    })
  }

  async function openEssay(id) {
    const item = await Api.request('/api/admin/essay-questions/' + id)
    const points = item.referencePoints || []
    document.getElementById('detailContent').innerHTML = `
      <div class="eyebrow">申论题 #${item.id} · ${escapeHtml(value(item.questionTypeName, item.questionType))} · ${escapeHtml(item.status)}</div>
      <h2>${escapeHtml(item.prompt || '（无题目要求）')}</h2>
      <dl>
        <dt>主题</dt><dd>${escapeHtml(value(item.topicName, item.topicCode))}</dd>
        <dt>题型</dt><dd>${escapeHtml(value(item.questionTypeName, item.questionType))}</dd>
        <dt>材料</dt><dd class="pre-wrap">${escapeHtml(value(item.material, '暂无材料'))}</dd>
        <dt>字数限制</dt><dd>${value(item.wordLimitMin)} ~ ${value(item.wordLimitMax)} 字</dd>
        <dt>标准时长</dt><dd>${value(item.standardTimeSeconds)} 秒</dd>
        <dt>参考答案</dt><dd class="pre-wrap">${escapeHtml(value(item.referenceAnswer, '暂无参考答案'))}</dd>
        <dt>参考要点</dt><dd>${points.length ? '<ul class="detail-list">' + points.map(point => '<li>' + escapeHtml(point.point) + (point.keywords && point.keywords.length ? '（关键词：' + point.keywords.map(escapeHtml).join('、') + '）' : '') + '</li>').join('') + '</ul>' : '—'}</dd>
        <dt>来源</dt><dd>${escapeHtml([item.sourceYear, item.sourceExam, item.sourceName].filter(Boolean).join(' · ')) || '—'}</dd>
        <dt>状态 / 创建</dt><dd>${escapeHtml(item.status)} / ${escapeHtml(value(item.createTime))}</dd>
      </dl>`
    dialog.showModal()
  }

  function openEssayForm() {
    document.getElementById('formContent').innerHTML = `
      <div class="eyebrow">申论题库</div>
      <h2>新增申论题</h2>
      <form id="essayForm" class="form">
        <div class="row">
          <label>主题<select name="topicCode" required>${options(ESSAY_TOPIC_CODES)}</select></label>
          <label>题型<select name="questionType" required>${options(ESSAY_QUESTION_TYPES)}</select></label>
        </div>
        <label>材料<textarea name="material" rows="6"></textarea></label>
        <label>题目要求<textarea name="prompt" rows="3" required></textarea></label>
        <div class="row">
          <label>字数下限<input name="wordLimitMin" type="number" min="0"></label>
          <label>字数上限<input name="wordLimitMax" type="number" min="0"></label>
        </div>
        <div class="row">
          <label>标准时长（秒）<input name="standardTimeSeconds" type="number" min="0"></label>
          <label>状态<select name="status">${options(ESSAY_STATUSES, 'ACTIVE')}</select></label>
        </div>
        <label>参考答案<textarea name="referenceAnswer" rows="4"></textarea></label>
        <label>参考要点（每行一条，格式：要点文字|关键词1,关键词2）<textarea name="referencePoints" rows="4" placeholder="例如：营商环境持续优化|营商环境,放管服"></textarea></label>
        <div class="row">
          <label>来源年份<input name="sourceYear" placeholder="如 2024"></label>
          <label>来源考试<input name="sourceExam" placeholder="如 国考"></label>
        </div>
        <label>来源名称<input name="sourceName" placeholder="如 2024 年国考副省级"></label>
        <div class="actions"><button type="button" class="secondary" data-form-cancel>取消</button><button class="primary">创建</button></div>
      </form>`
    document.querySelector('#essayForm [data-form-cancel]').onclick = () => formDialog.close()
    document.getElementById('essayForm').onsubmit = async event => {
      event.preventDefault()
      const form = Object.fromEntries(new FormData(event.target))
      try {
        await Api.request('/api/admin/essay-questions', {
          method: 'POST',
          body: {
            topicCode: form.topicCode,
            questionType: form.questionType,
            material: form.material,
            prompt: form.prompt,
            wordLimitMin: form.wordLimitMin === '' ? null : Number(form.wordLimitMin),
            wordLimitMax: form.wordLimitMax === '' ? null : Number(form.wordLimitMax),
            standardTimeSeconds: form.standardTimeSeconds === '' ? null : Number(form.standardTimeSeconds),
            referenceAnswer: form.referenceAnswer,
            referencePoints: parseReferencePoints(form.referencePoints),
            sourceYear: form.sourceYear,
            sourceExam: form.sourceExam,
            sourceName: form.sourceName,
            status: form.status
          }
        })
        formDialog.close()
        await renderEssays()
      } catch (ignore) {}
    }
    formDialog.showModal()
  }

  let essayAnswerFilters = {}
  async function renderEssayAnswers(filters) {
    if (filters) essayAnswerFilters = filters
    const query = new URLSearchParams(Object.assign({ page: 1, pageSize: 50 }, essayAnswerFilters))
    Array.from(query.keys()).forEach(key => { if (!query.get(key)) query.delete(key) })
    const data = await Api.request('/api/admin/essay-answers?' + query)
    const list = (data && data.list) || []
    content.innerHTML = `
      <form id="essayAnswerFilters" class="toolbar">
        <input name="userId" type="number" min="1" placeholder="用户 ID" value="${escapeHtml(essayAnswerFilters.userId || '')}">
        <input name="essayQuestionId" type="number" min="1" placeholder="申论题 ID" value="${escapeHtml(essayAnswerFilters.essayQuestionId || '')}">
        <button class="primary">查询</button>
      </form>
      <div class="table-wrap"><table><thead><tr><th>ID</th><th>用户</th><th>主题</th><th>题型</th><th>字数</th><th>耗时</th><th>提交时间</th><th>总分</th><th>评分器</th></tr></thead>
      <tbody>${list.map(item => '<tr data-essay-answer-id="' + item.id + '"><td>' + item.id + '</td><td>' + value(item.userId) + '</td><td>' + escapeHtml(value(item.topicName)) + '</td><td>' + escapeHtml(value(item.questionTypeName, item.questionType)) + '</td><td>' + value(item.wordCount) + ' 字</td><td>' + escapeHtml(toMinutes(item.durationMs)) + '</td><td>' + escapeHtml(value(item.submitTime)) + '</td><td>' + value(item.totalScore) + '</td><td>' + evaluatorTag(item.evaluator) + '</td></tr>').join('')}</tbody></table></div>
      ${list.length ? '<div class="table-meta">共 ' + value(data && data.total) + ' 条</div>' : '<div class="empty">没有符合条件的申论作答</div>'}`
    document.getElementById('essayAnswerFilters').onsubmit = event => {
      event.preventDefault(); renderEssayAnswers(Object.fromEntries(new FormData(event.target)))
    }
    content.querySelectorAll('[data-essay-answer-id]').forEach(row => {
      row.onclick = () => openEssayAnswer(Number(row.dataset.essayAnswerId))
    })
  }

  async function openEssayAnswer(id) {
    const data = await Api.request('/api/admin/essay-answers/' + id)
    const answer = data.answer || {}
    const question = data.question || {}
    const evaluation = data.evaluation || {}
    document.getElementById('detailContent').innerHTML = `
      <div class="eyebrow">申论作答 #${value(answer.id)} · ${evaluatorTag(evaluation.evaluator)}</div>
      <h2>${escapeHtml(value(question.topicName, '申论作答'))} · ${escapeHtml(value(question.questionTypeName, question.questionType))}</h2>
      <dl>
        <dt>用户 / 题号</dt><dd>#${value(answer.userId)} / #${value(answer.essayQuestionId)}（v${value(answer.questionVersion)}）</dd>
        <dt>练习场景</dt><dd>${escapeHtml(value(answer.practiceType))}</dd>
        <dt>字数 / 耗时</dt><dd>${value(answer.wordCount)} 字 / ${escapeHtml(toMinutes(answer.durationMs))}</dd>
        <dt>提交时间</dt><dd>${escapeHtml(value(answer.submitTime))}</dd>
      </dl>
      <h3>题目要求</h3><div class="pre-wrap">${escapeHtml(value(question.prompt))}</div>
      <h3>材料</h3><div class="pre-wrap">${escapeHtml(value(question.material, '暂无材料'))}</div>
      <h3>用户作答</h3><div class="pre-wrap">${escapeHtml(value(answer.answerText, '暂无作答内容'))}</div>
      <h3>评分结果</h3>
      <dl>
        <dt>评分器</dt><dd>${evaluatorTag(evaluation.evaluator)}</dd>
        <dt>AI审计</dt><dd>${escapeHtml(value(evaluation.provider))} / ${escapeHtml(value(evaluation.model))} / ${escapeHtml(value(evaluation.promptVersion))}</dd>
        <dt>状态 / 置信度</dt><dd>${escapeHtml(value(evaluation.status))} / ${value(evaluation.confidence)}</dd>
        <dt>总分</dt><dd class="answer">${value(evaluation.totalScore)}</dd>
        <dt>维度得分</dt><dd>${listBlock((evaluation.dimensionScores || []).map(dimension => (dimension.name || '未命名维度') + '：' + value(dimension.score) + ' 分'))}</dd>
        <dt>优点</dt><dd>${listBlock(evaluation.strengths)}</dd>
        <dt>问题</dt><dd>${listBlock(evaluation.problems)}</dd>
        <dt>遗漏要点</dt><dd>${listBlock(evaluation.missingPoints)}</dd>
        <dt>建议</dt><dd>${listBlock(evaluation.suggestions)}</dd>
      </dl>
      ${evaluation.status === 'FAILED' || !evaluation.totalScore ? '<div class="dialog-actions"><button class="primary" id="retryEssayAiBtn">重试 AI 评分</button></div>' : ''}`
    const retryButton = document.getElementById('retryEssayAiBtn')
    if (retryButton) retryButton.onclick = async () => {
      try {
        await Api.request('/api/essay/answers/' + id + '/retry', { method: 'POST' })
        dialog.close(); await openEssayAnswer(id)
      } catch (ignore) {}
    }
    dialog.showModal()
  }

  let readingFilters = {}
  async function renderReadings(filters) {
    if (filters) readingFilters = filters
    const topics = await Api.request('/api/admin/reading-topics')
    const query = new URLSearchParams(Object.assign({ page: 1, pageSize: 50 }, readingFilters))
    Array.from(query.keys()).forEach(key => { if (!query.get(key)) query.delete(key) })
    const data = await Api.request('/api/admin/reading-materials?' + query)
    const topicList = topics || []
    const list = (data && data.list) || []
    const topicOptions = topicList.map(topic => '<option value="' + topic.id + '"' + (String(topic.id) === String(readingFilters.topicId || '') ? ' selected' : '') + '>' + escapeHtml(topic.name) + '（' + escapeHtml(topic.code) + '）</option>').join('')
    content.innerHTML = `
      <div class="section-head"><h2>阅读专题</h2><button id="createTopicBtn" class="primary">新增专题</button></div>
      <div class="table-wrap"><table><thead><tr><th>Code</th><th>名称</th><th>描述</th><th>材料数</th><th>状态</th></tr></thead>
      <tbody>${topicList.map(topic => '<tr><td><code>' + escapeHtml(topic.code) + '</code></td><td>' + escapeHtml(topic.name) + '</td><td>' + escapeHtml(value(topic.description)) + '</td><td>' + value(topic.materialCount) + '</td><td><span class="pill ' + (topic.status === 'ACTIVE' ? 'good' : '') + '">' + escapeHtml(topic.status) + '</span></td></tr>').join('')}</tbody></table></div>
      ${topicList.length ? '' : '<div class="empty">暂无阅读专题</div>'}
      <div class="section-head"><h2>阅读材料</h2><button id="createMaterialBtn" class="primary">新增材料</button></div>
      <form id="materialFilters" class="toolbar">
        <input name="keyword" placeholder="标题或来源关键词" value="${escapeHtml(readingFilters.keyword || '')}">
        <select name="topicId"><option value="">全部专题</option>${topicOptions}</select>
        <select name="status"><option value="">全部状态</option>${options(READING_STATUSES, readingFilters.status)}</select>
        <button class="primary">查询</button>
      </form>
      <div class="table-wrap"><table><thead><tr><th>ID</th><th>专题</th><th>标题</th><th>来源</th><th>发布日期</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
      <tbody>${list.map(item => '<tr data-material-id="' + item.id + '"><td>' + item.id + '</td><td>' + escapeHtml(value(item.topicName, item.topicCode)) + '</td><td class="stem">' + escapeHtml(item.title) + '</td><td>' + escapeHtml(value(item.source)) + '</td><td>' + escapeHtml(value(item.publishDate)) + '</td><td><span class="pill ' + (item.status === 'PUBLISHED' ? 'good' : '') + '">' + escapeHtml(item.status) + '</span></td><td>' + escapeHtml(value(item.createTime)) + '</td><td><button class="mini" data-material-detail="' + item.id + '">详情</button></td></tr>').join('')}</tbody></table></div>
      ${list.length ? '<div class="table-meta">共 ' + value(data && data.total) + ' 条</div>' : '<div class="empty">没有符合条件的阅读材料</div>'}`
    document.getElementById('materialFilters').onsubmit = event => {
      event.preventDefault(); renderReadings(Object.fromEntries(new FormData(event.target)))
    }
    document.getElementById('createTopicBtn').onclick = () => openTopicForm()
    document.getElementById('createMaterialBtn').onclick = () => openMaterialForm(topicList)
    content.querySelectorAll('[data-material-id]').forEach(row => {
      row.onclick = () => openReadingMaterial(Number(row.dataset.materialId), topicList)
    })
    content.querySelectorAll('[data-material-detail]').forEach(button => {
      button.onclick = event => { event.stopPropagation(); openReadingMaterial(Number(button.dataset.materialDetail), topicList) }
    })
  }

  function openTopicForm() {
    document.getElementById('formContent').innerHTML = `
      <div class="eyebrow">政治阅读 · 专题</div>
      <h2>新增阅读专题</h2>
      <p>保存后会自动创建对应知识点。</p>
      <form id="topicForm" class="form">
        <label>专题 Code<input name="code" required placeholder="如 ECO_CIVILIZATION"></label>
        <label>名称<input name="name" required></label>
        <label>描述<textarea name="description" rows="3"></textarea></label>
        <div class="actions"><button type="button" class="secondary" data-form-cancel>取消</button><button class="primary">创建</button></div>
      </form>`
    document.querySelector('#topicForm [data-form-cancel]').onclick = () => formDialog.close()
    document.getElementById('topicForm').onsubmit = async event => {
      event.preventDefault()
      const form = Object.fromEntries(new FormData(event.target))
      try {
        await Api.request('/api/admin/reading-topics', { method: 'POST', body: { code: form.code, name: form.name, description: form.description } })
        formDialog.close()
        await renderReadings()
      } catch (ignore) {}
    }
    formDialog.showModal()
  }

  function openMaterialForm(topics, item) {
    const editing = !!item
    const topicOptions = topics.length
      ? topics.map(topic => '<option value="' + topic.id + '"' + (item && String(topic.id) === String(item.topicId) ? ' selected' : '') + '>' + escapeHtml(topic.name) + '（' + escapeHtml(topic.code) + '）</option>').join('')
      : '<option value="">（请先创建阅读专题）</option>'
    document.getElementById('formContent').innerHTML = `
      <div class="eyebrow">${editing ? '阅读材料 #' + item.id : '政治阅读 · 材料'}</div>
      <h2>${editing ? '编辑阅读材料' : '新增阅读材料'}</h2>
      <form id="materialForm" class="form">
        <div class="row">
          <label>专题<select name="topicId" required>${topicOptions}</select></label>
          <label>状态<select name="status">${options(READING_STATUSES, item ? item.status : 'PUBLISHED')}</select></label>
        </div>
        <label>标题<input name="title" required value="${escapeHtml(item ? item.title || '' : '')}"></label>
        <div class="row">
          <label>来源<input name="source" value="${escapeHtml(item ? item.source || '' : '')}"></label>
          <label>发布日期<input name="publishDate" type="date" value="${escapeHtml(item ? item.publishDate || '' : '')}"></label>
        </div>
        <label>正文<textarea name="content" rows="8" required>${escapeHtml(item ? item.content || '' : '')}</textarea></label>
        <label>核心观点<textarea name="coreView" rows="3">${escapeHtml(item ? item.coreView || '' : '')}</textarea></label>
        <div class="row">
          <label>问题<textarea name="problem" rows="2">${escapeHtml(item ? item.problem || '' : '')}</textarea></label>
          <label>原因<textarea name="cause" rows="2">${escapeHtml(item ? item.cause || '' : '')}</textarea></label>
        </div>
        <div class="row">
          <label>对策<textarea name="solution" rows="2">${escapeHtml(item ? item.solution || '' : '')}</textarea></label>
          <label>政策逻辑<textarea name="policyLogic" rows="2">${escapeHtml(item ? item.policyLogic || '' : '')}</textarea></label>
        </div>
        <label>规范表达（每行一条）<textarea name="standardExpressions" rows="3">${escapeHtml(item && item.standardExpressions ? item.standardExpressions.join('\n') : '')}</textarea></label>
        <label>案例（每行一条）<textarea name="cases" rows="3">${escapeHtml(item && item.cases ? item.cases.join('\n') : '')}</textarea></label>
        <label>适用申论主题（每行一条）<textarea name="applicableEssayThemes" rows="3">${escapeHtml(item && item.applicableEssayThemes ? item.applicableEssayThemes.join('\n') : '')}</textarea></label>
        <div class="actions"><button type="button" class="secondary" data-form-cancel>取消</button><button class="primary">${editing ? '保存' : '创建'}</button></div>
      </form>`
    document.querySelector('#materialForm [data-form-cancel]').onclick = () => formDialog.close()
    document.getElementById('materialForm').onsubmit = async event => {
      event.preventDefault()
      const form = Object.fromEntries(new FormData(event.target))
      try {
        await Api.request(editing ? '/api/admin/reading-materials/' + item.id : '/api/admin/reading-materials', {
          method: editing ? 'PUT' : 'POST',
          body: {
            topicId: Number(form.topicId),
            title: form.title,
            source: form.source,
            publishDate: form.publishDate || null,
            content: form.content,
            coreView: form.coreView,
            problem: form.problem,
            cause: form.cause,
            solution: form.solution,
            policyLogic: form.policyLogic,
            standardExpressions: toLines(form.standardExpressions),
            cases: toLines(form.cases),
            applicableEssayThemes: toLines(form.applicableEssayThemes),
            status: form.status
          }
        })
        formDialog.close()
        await renderReadings()
      } catch (ignore) {}
    }
    formDialog.showModal()
  }

  async function openReadingMaterial(id, topics) {
    const item = await Api.request('/api/admin/reading-materials/' + id)
    const statusButtons = READING_STATUSES.filter(status => status !== item.status)
      .map(status => '<button class="secondary" data-material-status="' + status + '">' + READING_STATUS_LABELS[status] + '</button>').join('')
    document.getElementById('detailContent').innerHTML = `
      <div class="eyebrow">阅读材料 #${item.id} · ${escapeHtml(item.status)}</div>
      <h2>${escapeHtml(item.title)}</h2>
      <dl>
        <dt>专题</dt><dd>${escapeHtml(value(item.topicName, item.topicCode))}${item.topicCode ? '（' + escapeHtml(item.topicCode) + '）' : ''}</dd>
        <dt>来源 / 日期</dt><dd>${escapeHtml(value(item.source))} / ${escapeHtml(value(item.publishDate))}</dd>
        <dt>状态</dt><dd><span class="pill ${item.status === 'PUBLISHED' ? 'good' : ''}">${escapeHtml(item.status)}</span></dd>
        <dt>创建 / 更新</dt><dd>${escapeHtml(value(item.createTime))} / ${escapeHtml(value(item.updateTime))}</dd>
        <dt>正文</dt><dd class="pre-wrap">${escapeHtml(value(item.content, '暂无正文'))}</dd>
        <dt>核心观点</dt><dd class="pre-wrap">${escapeHtml(value(item.coreView))}</dd>
        <dt>问题</dt><dd class="pre-wrap">${escapeHtml(value(item.problem))}</dd>
        <dt>原因</dt><dd class="pre-wrap">${escapeHtml(value(item.cause))}</dd>
        <dt>对策</dt><dd class="pre-wrap">${escapeHtml(value(item.solution))}</dd>
        <dt>政策逻辑</dt><dd class="pre-wrap">${escapeHtml(value(item.policyLogic))}</dd>
        <dt>规范表达</dt><dd>${listBlock(item.standardExpressions)}</dd>
        <dt>案例</dt><dd>${listBlock(item.cases)}</dd>
        <dt>适用主题</dt><dd>${listBlock(item.applicableEssayThemes)}</dd>
        <dt>AI状态</dt><dd>${escapeHtml(value(item.aiStatus))} · ${escapeHtml(value(item.aiProvider))} / ${escapeHtml(value(item.aiModel))}</dd>
        <dt>AI版本 / 置信度</dt><dd>${escapeHtml(value(item.aiPromptVersion))} / ${value(item.aiConfidence)}</dd>
      </dl>
      <div class="dialog-actions"><button class="primary" id="editMaterialBtn">编辑</button><button class="secondary" id="structureMaterialBtn">AI结构化</button>${statusButtons}</div>`
    document.getElementById('editMaterialBtn').onclick = () => {
      dialog.close(); openMaterialForm(topics, item)
    }
    document.getElementById('structureMaterialBtn').onclick = async () => {
      try {
        await Api.request('/api/admin/reading-materials/' + id + '/ai-structure', { method: 'POST' })
        dialog.close(); await openReadingMaterial(id, topics)
      } catch (ignore) {}
    }
    document.querySelectorAll('#detailContent [data-material-status]').forEach(button => {
      button.onclick = async () => {
        try {
          await Api.request('/api/admin/reading-materials/' + id + '/status', { method: 'POST', body: { status: button.dataset.materialStatus } })
          dialog.close()
          await renderReadings()
        } catch (ignore) {}
      }
    })
    dialog.showModal()
  }

  window.AdminApp = { setBusy, showError }
  document.querySelectorAll('nav button').forEach(button => button.onclick = () => loadView(button.dataset.view))
  document.querySelector('#detailDialog .close').onclick = () => dialog.close()
  document.querySelector('#formDialog .close').onclick = () => formDialog.close()
  document.getElementById('baseUrl').value = Api.context.baseUrl
  document.getElementById('userId').value = Api.context.userId
  document.getElementById('saveContext').onclick = () => {
    Api.saveContext(document.getElementById('baseUrl').value, document.getElementById('userId').value)
    loadView(currentView)
  }
  loadView('questions')
})()
