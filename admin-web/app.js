(function () {
  const content = document.getElementById('content')
  const status = document.getElementById('status')
  const title = document.getElementById('pageTitle')
  const dialog = document.getElementById('detailDialog')
  const formDialog = document.getElementById('formDialog')
  const titles = { questions: '题库', import: '题目导入', knowledge: '知识点', answers: '答题记录', essays: '申论题库', 'essay-answers': '申论作答', readings: '政治阅读', content: '题库采集', mock: '模考试卷', ai: 'AI 设置' }
  const ESSAY_QUESTION_TYPES = ['SUMMARY', 'ANALYSIS', 'COUNTERMEASURE', 'IMPLEMENTATION']
  const ESSAY_TOPIC_CODES = ['HQ_DEVELOPMENT', 'TECH_INNOVATION', 'NEW_QUALITY_PRODUCTIVITY', 'RURAL_REVITALIZATION', 'GRASSROOTS_GOVERNANCE', 'PEOPLE_LIVELIHOOD', 'ECO_CIVILIZATION', 'CULTURE', 'GOVERNANCE', 'TALENT']
  const ESSAY_STATUSES = ['DRAFT', 'ACTIVE', 'ARCHIVED']
  const READING_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED']
  const READING_STATUS_LABELS = { PUBLISHED: '发布', DRAFT: '下架', ARCHIVED: '归档' }
  const SOURCE_SITE_TYPES = [['GOVERNMENT', '政府网站'], ['ORGANIZATION', '组织部门'], ['HR_DEPARTMENT', '人社部门'], ['EXAM_AUTHORITY', '公务员主管部门'], ['OTHER', '其他']]
  const SOURCE_TRUST_LEVELS = [['S', 'S：官方真题 / 官方样题'], ['A', 'A：官方附件 / 官方材料'], ['B', 'B：多来源交叉验证'], ['C', 'C：单来源回忆'], ['D', 'D：未经验证']]
  const STAGING_STATUSES = [['DISCOVERED', '已发现'], ['DOWNLOADED', '已下载'], ['PARSED', '已解析'], ['DEDUPED', '已去重'], ['READY', '待入库'], ['IMPORTED', '已入库'], ['NEEDS_REVIEW', '需要人工检查'], ['FAILED', '处理失败']]

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;').replaceAll("'", '&#039;')
  }
  function value(value, fallback) { return value == null || value === '' ? (fallback || '—') : value }
  function options(list, selected) {
    return list.map(item => '<option value="' + escapeHtml(item) + '"' + (item === selected ? ' selected' : '') + '>' + escapeHtml(item) + '</option>').join('')
  }
  function labeledOptions(pairs, selected) {
    return pairs.map(pair => '<option value="' + escapeHtml(pair[0]) + '"' + (pair[0] === selected ? ' selected' : '') + '>' + escapeHtml(pair[1]) + '</option>').join('')
  }
  function trustText(level) {
    const found = SOURCE_TRUST_LEVELS.find(pair => pair[0] === level)
    return found ? found[1] : value(level)
  }
  function formatDateTime(text) {
    if (!text) return null
    const match = String(text).match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/)
    return match ? match[1] + ' ' + match[2] : String(text)
  }
  function fileSizeText(size) {
    if (size == null) return '—'
    if (size < 1024) return size + ' 字节'
    return (size / 1024).toFixed(1) + ' KB'
  }
  function toMinutes(durationMs) {
    if (durationMs == null) return '—'
    return (durationMs / 60000).toFixed(1) + ' 分钟'
  }
  function toDuration(durationMs) {
    if (durationMs == null) return '—'
    if (durationMs < 60000) return Math.round(durationMs / 1000) + ' 秒'
    return toMinutes(durationMs)
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
      if (view === 'content') await renderContent()
      if (view === 'mock') await renderMockPapers()
      if (view === 'ai') await renderAiProviders()
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

  async function renderMockPapers() {
    const [papers, sessions, reserved] = await Promise.all([
      Api.request('/api/admin/mock-papers'),
      Api.request('/api/mock/history'),
      Api.request('/api/questions?usageType=MOCK_RESERVED&status=ACTIVE&page=1&size=100')
    ])
    content.innerHTML = `
      <div class="toolbar"><button class="primary" id="createMockBtn">创建模考试卷</button><span class="table-meta">可用模考专用行测题 ${reserved.length} 道</span></div>
      <div class="table-wrap"><table><thead><tr><th>ID</th><th>试卷</th><th>类型</th><th>年份 / 来源</th><th>时长</th><th>总分</th><th>状态</th><th>操作</th></tr></thead><tbody>${papers.map(p => `<tr><td>${p.id}</td><td><button class="link" data-mock-detail="${p.id}">${escapeHtml(p.name)}</button></td><td>${p.examType === 'XINGCE' ? '行测' : '申论'}</td><td>${value(p.sourceYear)} / ${escapeHtml(value(p.source))}</td><td>${p.durationMinutes} 分钟</td><td>${p.totalScore}</td><td>${p.status === 'ACTIVE' ? '已启用' : p.status === 'SUSPENDED' ? '已停用' : p.status === 'ARCHIVED' ? '已归档' : '草稿'}</td><td><button class="secondary" data-mock-status="${p.id}" data-status="${p.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'}">${p.status === 'ACTIVE' ? '停用' : '启用'}</button></td></tr>`).join('')}</tbody></table></div>
      <h2>模考记录</h2><div class="table-wrap"><table><thead><tr><th>场次</th><th>用户</th><th>试卷</th><th>开始</th><th>交卷方式</th><th>得分</th><th>完成率</th></tr></thead><tbody>${sessions.map(s => `<tr><td>#${s.id}</td><td>${s.userId}</td><td>#${s.paperId}</td><td>${escapeHtml(value(formatDateTime(s.startTime)))}</td><td>${s.submitType === 'AUTO_TIME_LIMIT' ? '到时自动交卷' : s.submitType === 'MANUAL' ? '主动交卷' : '进行中'}</td><td>${value(s.totalScore)}</td><td>${value(s.completionRate)}%</td></tr>`).join('')}</tbody></table></div>`
    document.getElementById('createMockBtn').onclick = () => openMockForm(reserved)
    content.querySelectorAll('[data-mock-detail]').forEach(button => button.onclick = () => openMockDetail(button.dataset.mockDetail))
    content.querySelectorAll('[data-mock-status]').forEach(button => button.onclick = async () => { try { await Api.request('/api/admin/mock-papers/' + button.dataset.mockStatus + '/status', { method: 'POST', body: { status: button.dataset.status } }); await renderMockPapers() } catch (ignore) {} })
  }

  function openMockForm(reserved) {
    const hint = reserved.map(q => '#' + q.id + ' ' + q.stem.slice(0, 35)).join('\n') || '当前没有已启用的模考专用行测题，请先在题库导入 usageType=MOCK_RESERVED 的题目。'
    document.getElementById('formContent').innerHTML = `<h2>创建模考试卷</h2><form id="mockForm" class="form"><label>试卷名称<input name="name" required></label><label>类型<select name="examType"><option value="XINGCE">行测</option><option value="SHENLUN">申论</option></select></label><label>年份<input name="sourceYear" type="number"></label><label>来源<input name="source"></label><label>时长（分钟）<input name="durationMinutes" type="number" min="1" required value="120"></label><label>总分<input name="totalScore" type="number" min="1" required value="100"></label><label>Section 与题目组成（JSON）<textarea name="sections" rows="10" required placeholder='[{"name":"资料分析","sectionCode":"DATA_ANALYSIS","knowledgePointId":1,"sortNo":1,"score":20,"items":[{"itemType":"QUESTION","questionId":1001,"sortNo":1,"score":2}]}]'></textarea></label><div class="form-tip pre-wrap">可用题目：\n${escapeHtml(hint)}</div><div class="actions"><button type="button" class="secondary" data-form-cancel>取消</button><button class="primary">创建草稿</button></div></form>`
    document.querySelector('#mockForm [data-form-cancel]').onclick = () => formDialog.close()
    document.getElementById('mockForm').onsubmit = async event => { event.preventDefault(); const form = Object.fromEntries(new FormData(event.target)); try { const sections = JSON.parse(form.sections); await Api.request('/api/admin/mock-papers', { method: 'POST', body: { name: form.name, examType: form.examType, sourceYear: form.sourceYear ? Number(form.sourceYear) : null, source: form.source, durationMinutes: Number(form.durationMinutes), totalScore: Number(form.totalScore), sections } }); formDialog.close(); await renderMockPapers() } catch (error) { if (error instanceof SyntaxError) showError('Section JSON 格式不正确') } }
    formDialog.showModal()
  }

  async function openMockDetail(id) {
    const paper = await Api.request('/api/admin/mock-papers/' + id)
    document.getElementById('detailContent').innerHTML = `<div class="eyebrow">${paper.examType === 'XINGCE' ? '行测' : '申论'}模考 #${paper.id}</div><h2>${escapeHtml(paper.name)}</h2><dl><dt>来源</dt><dd>${escapeHtml(value(paper.source))}</dd><dt>时长 / 总分</dt><dd>${paper.durationMinutes} 分钟 / ${paper.totalScore} 分</dd><dt>状态</dt><dd>${escapeHtml(paper.status)}</dd></dl>${(paper.sections || []).map(section => `<h3>${escapeHtml(section.name)}</h3><div class="table-wrap"><table><thead><tr><th>顺序</th><th>类型</th><th>题目</th><th>分值</th></tr></thead><tbody>${(section.items || []).map(item => `<tr><td>${item.sortNo}</td><td>${item.itemType === 'QUESTION' ? '行测题' : '申论题'}</td><td>${escapeHtml(value(item.stem))}</td><td>${item.score}</td></tr>`).join('')}</tbody></table></div>`).join('')}`
    dialog.showModal()
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
      records.map(item => '<tr><td>#' + item.questionId + '</td><td>' + escapeHtml(item.userAnswer) + '</td><td>' + escapeHtml(item.correctAnswerSnapshot) + '</td><td><span class="pill ' + (item.correct ? 'good' : 'bad') + '">' + (item.correct ? '正确' : '错误') + '</span></td><td>' + toDuration(item.durationMs) + '</td><td>' + escapeHtml(item.practiceType) + '</td><td>' + escapeHtml(item.confidenceType) + '</td><td>' + escapeHtml(item.errorType) + '</td><td>' + escapeHtml(item.answerTime) + '</td></tr>').join('') +
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

  const IMPORTED_TYPE_LABELS = { READING_MATERIAL: '政治阅读材料' }

  function providerCard(item) {
    const lastTest = [item.lastTestStatusText,
      item.lastTestLatencyMs != null && item.lastTestLatencyMs > 0 ? '耗时 ' + item.lastTestLatencyMs + ' ms' : '',
      formatDateTime(item.lastTestTime)].filter(Boolean).join(' · ')
    return `
      <article class="provider-card" data-provider-code="${escapeHtml(item.code)}">
        <div class="provider-head">
          <h3>${escapeHtml(item.name)}</h3>
          <div class="pill-group">
            ${item.defaultProvider ? '<span class="pill good">默认服务</span>' : ''}
            <span class="pill ${item.configured ? 'good' : 'bad'}">${item.configured ? '已配置' : '未配置'}</span>
            <span class="pill">${item.enabled ? '已启用' : '已停用'}</span>
          </div>
        </div>
        <dl class="provider-meta">
          <dt>当前模型</dt><dd>${escapeHtml(value(item.effectiveModel))}</dd>
          <dt>API Key</dt><dd>${escapeHtml(value(item.maskedKey))}</dd>
          <dt>最后测试</dt><dd>${escapeHtml(value(lastTest))}</dd>
          <dt>测试详情</dt><dd>${escapeHtml(value(item.lastTestMessage))}</dd>
        </dl>
        <div class="card-hint">点击配置 API Key、模型与连接参数</div>
      </article>`
  }

  async function renderAiProviders() {
    const providers = await Api.request('/api/admin/ai-providers')
    content.innerHTML = `
      <div class="section-head"><h2>AI 服务商</h2></div>
      <div class="provider-grid">${providers.map(providerCard).join('')}</div>
      ${providers.length ? '' : '<div class="empty">暂无 AI 服务商</div>'}`
    content.querySelectorAll('[data-provider-code]').forEach(card => {
      card.onclick = () => openAiProviderForm(providers.find(provider => provider.code === card.dataset.providerCode))
    })
  }

  function openAiProviderForm(item) {
    const models = (item.defaultModels || []).slice()
    if (item.model && models.indexOf(item.model) === -1) models.push(item.model)
    const keyPlaceholder = item.maskedKey
      ? '已保存：' + item.maskedKey + '，不修改请留空'
      : '尚未配置，请输入完整 API Key'
    document.getElementById('formContent').innerHTML = `
      <div class="eyebrow">AI 设置</div>
      <h2>${escapeHtml(item.name)}</h2>
      <p class="form-tip">修改 API Key 时必须重新输入完整 Key，掩码值会被拒绝；测试连接会直接使用当前表单输入（含尚未保存的新 Key）。</p>
      <form id="aiProviderForm" class="form">
        <label>API Key<input name="apiKey" type="password" autocomplete="new-password" placeholder="${escapeHtml(keyPlaceholder)}"></label>
        <div class="row">
          <label>模型<select name="model">${options(models, item.model)}</select></label>
          <label>自定义模型名<input name="customModel" value="${escapeHtml(item.customModel || '')}" placeholder="填写后优先生效，留空使用左侧模型"></label>
        </div>
        <label>Base URL（高级选项，默认可留空）<input name="baseUrl" value="${escapeHtml(item.baseUrl || '')}" placeholder="${escapeHtml(item.defaultBaseUrl || '')}"></label>
        <label class="check"><input name="enabled" type="checkbox" ${item.enabled ? 'checked' : ''}>启用该服务</label>
        <div id="aiTestResult" class="notice" hidden></div>
        <div class="actions">
          <button type="button" class="secondary" data-form-cancel>取消</button>
          <button type="button" class="secondary" id="aiSetDefaultBtn"${item.defaultProvider ? ' disabled' : ''}>设为默认</button>
          <button type="button" class="secondary" id="aiTestBtn">测试连接</button>
          <button class="primary">保存</button>
        </div>
      </form>`
    const form = document.getElementById('aiProviderForm')
    const resultBox = document.getElementById('aiTestResult')
    const readBody = () => ({
      apiKey: form.apiKey.value.trim() || undefined,
      model: form.model.value,
      customModel: form.customModel.value.trim(),
      baseUrl: form.baseUrl.value.trim(),
      enabled: String(form.enabled.checked)
    })
    const showTestResult = (className, text) => {
      resultBox.hidden = false
      resultBox.className = 'notice ' + className
      resultBox.textContent = text
    }
    document.querySelector('#aiProviderForm [data-form-cancel]').onclick = () => formDialog.close()
    document.getElementById('aiTestBtn').onclick = async () => {
      const button = document.getElementById('aiTestBtn')
      button.disabled = true
      button.textContent = '测试中…'
      resultBox.hidden = true
      try {
        const result = await Api.request('/api/admin/ai-providers/' + item.code + '/test-request', { method: 'POST', body: readBody() })
        showTestResult(result.status === 'SUCCESS' ? 'success' : 'error',
          (result.statusText || '测试完成') + '：' + (result.message || '无详细信息') + (result.latencyMs ? '（耗时 ' + result.latencyMs + ' ms）' : ''))
      } catch (error) {
        showTestResult('error', '测试请求失败：' + error.message)
      } finally {
        button.disabled = false
        button.textContent = '测试连接'
      }
    }
    document.getElementById('aiSetDefaultBtn').onclick = async () => {
      try {
        await Api.request('/api/admin/ai-providers/' + item.code + '/default', { method: 'POST' })
        formDialog.close()
        await renderAiProviders()
      } catch (ignore) {}
    }
    form.onsubmit = async event => {
      event.preventDefault()
      try {
        await Api.request('/api/admin/ai-providers/' + item.code, { method: 'PUT', body: readBody() })
        formDialog.close()
        await renderAiProviders()
      } catch (ignore) {}
    }
    formDialog.showModal()
  }

  let contentStagingFilters = {}
  let contentTab = 'inventory'
  const contentTabs = [['inventory', '库存概览'], ['sources', '手动采集'], ['upload', '文件导入'], ['exceptions', '异常处理'], ['logs', '采集记录']]
  const contentTabBar = () => '<div class="toolbar">' + contentTabs.map(tab => '<button class="mini' + (contentTab === tab[0] ? ' primary' : '') + '" data-content-tab="' + tab[0] + '">' + tab[1] + '</button>').join('') + '</div>'
  async function renderContent(filters) {
    if (filters) contentStagingFilters = filters
    const [sources, overview] = await Promise.all([Api.request('/api/admin/content/sources'), Api.request('/api/admin/content/inventory')])
    let staging = null
    let logs = null
    if (contentTab === 'exceptions') staging = await Api.request('/api/admin/content/staging?status=NEEDS_REVIEW&page=1')
    if (contentTab === 'logs') logs = await Api.request('/api/admin/content/crawl-logs')
    const items = (staging && staging.items) || []
    const summary = overview.overview || {}
    let body = ''
    if (contentTab === 'inventory') {
      body = '<div class="summary">' + [['普通可训练题', summary.trainableTotal], ['模考保留题', summary.mockReservedTotal], ['申论题', summary.essayTotal], ['AI 训练题', summary.aiTotal], ['异常待处理', summary.needsReviewCount], ['采集失败', summary.crawlFailedCount]].map(item => '<div><b>' + (item[1] || 0) + '</b><span>' + item[0] + '</span></div>').join('') + '</div>'
        + '<div class="section-head"><h2>五大模块库存</h2><span class="table-meta">来自实时题库统计</span></div><div class="table-wrap"><table><thead><tr><th>模块</th><th>总题数</th><th>可训练</th><th>未做题</th><th>知识点</th></tr></thead><tbody>'
        + (overview.modules || []).map(module => '<tr><td>' + escapeHtml(module.moduleName) + '</td><td>' + module.totalQuestions + '</td><td>' + module.trainable + '</td><td>' + module.unused + '</td><td>' + (module.knowledgePoints || []).map(point => escapeHtml(point.name) + ' ' + point.trainable + '题').join('；') + '</td></tr>').join('') + '</tbody></table></div>'
    } else if (contentTab === 'sources') {
      body = '<div class="section-head"><h2>手动采集</h2><button id="createSourceBtn" class="primary">新增来源</button></div><p class="form-tip">仅对公开、无需登录且确实包含结构化题目的来源发起采集。抓取结果以新增 Question 数量为准。</p><div class="table-wrap"><table><thead><tr><th>名称</th><th>地址</th><th>可信度</th><th>启用</th><th>操作</th></tr></thead><tbody>'
        + sources.map(item => '<tr><td>' + escapeHtml(item.name) + '</td><td class="stem">' + escapeHtml(item.baseUrl) + '</td><td>' + escapeHtml(value(item.trustText)) + '</td><td>' + (item.enabled ? '已启用' : '已停用') + '</td><td><button class="mini" data-source-edit="' + item.id + '">编辑</button> <button class="mini" data-source-crawl="' + item.id + '">立即采集</button></td></tr>').join('') + '</tbody></table></div>'
    } else if (contentTab === 'upload') {
      body = '<div class="section-head"><h2>文件导入</h2></div><p class="form-tip">CSV、XLSX、XLS 将先进入暂存区，再依次经过解析、去重、质量门禁、自动分类和入库；异常记录进入“异常处理”。</p><form id="contentUploadForm" class="form"><label>题目文件<input name="file" type="file" accept=".csv,.xlsx,.xls" required></label><label>来源名称<input name="sourceName" required placeholder="例如：公开题目整理"></label><label>来源等级<select name="trustLevel">' + labeledOptions(SOURCE_TRUST_LEVELS, 'B') + '</select></label><div class="actions"><button class="primary">上传并处理</button></div></form><div id="uploadResult"></div>'
    } else if (contentTab === 'exceptions') {
      body = '<div class="section-head"><h2>异常处理</h2><span class="table-meta">仅异常记录需要人工处理；正常题已自动入库。</span></div><div class="table-wrap"><table><thead><tr><th>标题</th><th>来源</th><th>异常原因</th><th>质量信息</th><th>操作</th></tr></thead><tbody>'
        + items.map(item => '<tr><td class="stem">' + escapeHtml(value(item.title)) + '</td><td>' + escapeHtml(value(item.sourceName)) + '</td><td>' + escapeHtml(value(item.failReason)) + '</td><td>' + escapeHtml(value(item.qualityIssues)) + '</td><td><button class="mini" data-staging-detail="' + item.id + '">详情</button> <button class="mini" data-staging-review="' + item.id + '">修正并入库</button></td></tr>').join('') + '</tbody></table></div>' + (items.length ? '' : '<div class="empty">当前没有待处理异常。</div>')
    } else {
      body = '<div class="section-head"><h2>采集记录</h2></div><div class="table-wrap"><table><thead><tr><th>来源</th><th>状态</th><th>发现页面</th><th>已下载</th><th>解析内容</th><th>自动入库</th><th>重复</th><th>异常</th><th>失败</th><th>时间</th></tr></thead><tbody>'
        + (logs || []).map(log => '<tr><td>' + escapeHtml(value(log.sourceName, log.sourceId)) + '</td><td>' + escapeHtml(value(log.status)) + '</td><td>' + value(log.discovered) + '</td><td>' + value(log.downloaded) + '</td><td>' + value(log.parsed) + '</td><td>' + value(log.imported) + '</td><td>' + value(log.duplicates) + '</td><td>' + value(log.needsReview) + '</td><td>' + value(log.failed) + '</td><td>' + escapeHtml(value(formatDateTime(log.startTime))) + '</td></tr>').join('') + '</tbody></table></div>' + ((logs || []).length ? '' : '<div class="empty">暂无采集记录。</div>')
    }
    content.innerHTML = contentTabBar() + body
    content.querySelectorAll('[data-content-tab]').forEach(button => button.onclick = () => { contentTab = button.dataset.contentTab; renderContent() })
    const create = document.getElementById('createSourceBtn')
    if (create) create.onclick = () => openSourceForm()
    const uploadForm = document.getElementById('contentUploadForm')
    if (uploadForm) uploadForm.onsubmit = async event => { event.preventDefault(); const form = new FormData(event.target); try { const result = await Api.request('/api/admin/content/upload', { method: 'POST', body: form, timeout: 120000 }); document.getElementById('uploadResult').innerHTML = '<div class="notice success">文件共 ' + result.total + ' 题；自动入库 ' + result.imported + '；重复 ' + result.duplicates + '；异常 ' + result.needsReview + '；失败 ' + result.failed + '。<br>' + escapeHtml((result.errors || []).join('；')) + '</div>' } catch (ignore) {} }
    content.querySelectorAll('[data-source-edit]').forEach(button => button.onclick = () => openSourceForm(sources.find(source => String(source.id) === button.dataset.sourceEdit)))
    content.querySelectorAll('[data-source-crawl]').forEach(button => button.onclick = () => crawlSource(sources.find(source => String(source.id) === button.dataset.sourceCrawl), button))
    content.querySelectorAll('[data-staging-detail]').forEach(button => button.onclick = () => openStagingDetail(Number(button.dataset.stagingDetail)))
    content.querySelectorAll('[data-staging-review]').forEach(button => button.onclick = () => openStagingReview(items.find(entry => String(entry.id) === button.dataset.stagingReview)))
  }

  function openSourceForm(item) {
    const editing = !!item
    document.getElementById('formContent').innerHTML = `
      <div class="eyebrow">${editing ? '内容来源 #' + item.id : '内容管理 · 来源'}</div>
      <h2>${editing ? '编辑内容来源' : '新增内容来源'}</h2>
      <p class="form-tip">地址必须以 http 开头；可信度决定该来源入库内容的默认信任等级。</p>
      <form id="sourceForm" class="form">
        <label>名称<input name="name" required value="${escapeHtml(item ? item.name || '' : '')}"></label>
        <label>地址<input name="baseUrl" required placeholder="如 https://www.gov.cn/..." value="${escapeHtml(item ? item.baseUrl || '' : '')}"></label>
        <div class="row">
          <label>类型<select name="sourceType">${labeledOptions(SOURCE_SITE_TYPES, item ? item.sourceType : 'GOVERNMENT')}</select></label>
          <label>考试类型<input name="examType" value="${escapeHtml(item ? item.examType || 'GK' : 'GK')}"></label>
        </div>
        <div class="row">
          <label>可信度<select name="trustLevel">${labeledOptions(SOURCE_TRUST_LEVELS, item ? item.trustLevel : 'B')}</select></label>
          <label class="check"><input name="enabled" type="checkbox" ${!item || item.enabled ? 'checked' : ''}>启用该来源</label>
        </div>
        <div class="actions"><button type="button" class="secondary" data-form-cancel>取消</button><button class="primary">${editing ? '保存' : '创建'}</button></div>
      </form>`
    document.querySelector('#sourceForm [data-form-cancel]').onclick = () => formDialog.close()
    document.getElementById('sourceForm').onsubmit = async event => {
      event.preventDefault()
      const form = Object.fromEntries(new FormData(event.target))
      try {
        await Api.request(editing ? '/api/admin/content/sources/' + item.id : '/api/admin/content/sources', {
          method: editing ? 'PUT' : 'POST',
          body: {
            name: form.name,
            baseUrl: form.baseUrl,
            sourceType: form.sourceType,
            examType: form.examType,
            trustLevel: form.trustLevel,
            enabled: String(event.target.enabled.checked)
          }
        })
        formDialog.close()
        await renderContent()
      } catch (ignore) {}
    }
    formDialog.showModal()
  }

  async function crawlSource(source, button) {
    button.disabled = true
    const original = button.textContent
    button.textContent = '抓取中…'
    try {
      const trigger = await Api.request('/api/admin/content/sources/' + source.id + '/crawl', { method: 'POST', timeout: 120000 })
      const result = await waitForCrawlLog(source.id, trigger.logId)
      const errors = result.message ? [result.message] : []
      document.getElementById('detailContent').innerHTML = `
        <div class="eyebrow">内容抓取 · ${escapeHtml(source.name)}</div>
        <h2>${result.status === 'SUCCESS' ? '抓取完成' : '抓取失败'}</h2>
        <div class="summary">
          <div><b>${result.discovered}</b><span>新发现</span></div>
          <div><b>${(result.downloaded || 0)}</b><span>下载</span></div>
          <div><b>${result.imported}</b><span>入库</span></div>
          <div><b>${result.needsReview}</b><span>需人工检查</span></div>
          <div><b>${result.failed}</b><span>失败</span></div>
        </div>
        ${errors.length
          ? '<h3>失败明细（最多显示 5 条）</h3>' + listBlock(errors.slice(0, 5)) + (errors.length > 5 ? '<div class="table-meta">… 以及另外 ' + (errors.length - 5) + ' 个问题</div>' : '')
          : '<div class="notice success">本次抓取没有失败项。</div>'}
        <div class="dialog-actions"><button class="primary" id="refreshContentBtn">刷新内容数据</button></div>`
      document.getElementById('refreshContentBtn').onclick = async () => {
        dialog.close()
        await renderContent()
      }
      dialog.showModal()
    } catch (error) {
      document.getElementById('detailContent').innerHTML = `
        <div class="eyebrow">内容抓取 · ${escapeHtml(source.name)}</div>
        <h2>抓取未完成</h2>
        <div class="notice error">${escapeHtml(error.message || '抓取状态读取失败，请在采集记录中查看')}</div>`
      dialog.showModal()
    } finally {
      button.disabled = false
      button.textContent = original
    }
  }

  async function waitForCrawlLog(sourceId, logId) {
    for (let attempt = 0; attempt < 45; attempt++) {
      const logs = await Api.request('/api/admin/content/crawl-logs?sourceId=' + sourceId, { showLoading: false })
      const log = (logs || []).find(item => item.id === logId)
      if (log && log.status !== 'RUNNING') return log
      await new Promise(resolve => setTimeout(resolve, 1000))
    }
    throw new Error('采集仍在执行，请在采集记录中稍后查看')
  }

  async function openStagingDetail(id) {
    const item = await Api.request('/api/admin/content/staging/' + id)
    const importedLabel = IMPORTED_TYPE_LABELS[item.importedType] || '导入内容'
    document.getElementById('detailContent').innerHTML = `
      <div class="eyebrow">暂存记录 #${item.id} · ${escapeHtml(value(item.statusText))}</div>
      <h2>${escapeHtml(value(item.title))}</h2>
      <dl>
        <dt>来源地址</dt><dd class="pre-wrap">${escapeHtml(value(item.sourceUrl))}</dd>
        <dt>状态 / 可信度</dt><dd>${escapeHtml(value(item.statusText))} / ${escapeHtml(trustText(item.trustLevel))}</dd>
        <dt>年份</dt><dd>${value(item.sourceYear)}</dd>
        <dt>文件类型 / 大小</dt><dd>${escapeHtml(value(item.mimeType))} / ${fileSizeText(item.fileSize)}</dd>
        <dt>文件哈希</dt><dd><code>${escapeHtml(value(item.fileHash))}</code></dd>
        <dt>内容哈希</dt><dd><code>${escapeHtml(value(item.contentHash))}</code></dd>
        <dt>失败原因</dt><dd class="pre-wrap">${escapeHtml(value(item.failReason))}</dd>
        <dt>处理备注</dt><dd class="pre-wrap">${escapeHtml(value(item.reviewNote))}</dd>
        <dt>入库信息</dt><dd>${item.importedId ? '#' + item.importedId + ' · ' + escapeHtml(importedLabel) : '—'}</dd>
      </dl>
      <h3>正文内容</h3>
      <div class="pre-wrap detail-text">${escapeHtml(value(item.parsedText, '（暂无解析正文）'))}</div>`
    dialog.showModal()
  }

  async function openStagingReview(item) {
    item = await Api.request('/api/admin/content/staging/' + item.id)
    let candidate = {}
    try { candidate = JSON.parse(item.parsedText || '{}') } catch (ignore) {}
    const optionText = key => ((candidate.options || []).find(option => option.key === key) || {}).text || ''
    document.getElementById('formContent').innerHTML = `
      <div class="eyebrow">暂存记录 #${item.id} · 需要人工检查</div>
      <h2>修正题目并入库</h2>
      <p class="form-tip">仅修正异常字段后再入库；原始暂存记录会保留，便于追溯。</p>
      <form id="stagingReviewForm" class="form">
        <label>处理方式<select name="action" id="reviewAction">
          <option value="IMPORT_QUESTION">修正后入题库</option>
          <option value="CONFIRM_DUPLICATE">确认重复</option>
          <option value="DISCARD">不予入库</option>
        </select></label>
        <div id="questionFixFields">
          <label>题干<textarea name="stem" rows="3" required>${escapeHtml(candidate.stem || '')}</textarea></label>
          <div class="row"><label>A<input name="optionA" required value="${escapeHtml(optionText('A'))}"></label><label>B<input name="optionB" required value="${escapeHtml(optionText('B'))}"></label></div>
          <div class="row"><label>C<input name="optionC" required value="${escapeHtml(optionText('C'))}"></label><label>D<input name="optionD" required value="${escapeHtml(optionText('D'))}"></label></div>
          <div class="row"><label>正确答案<select name="answer">${labeledOptions([['A', 'A'], ['B', 'B'], ['C', 'C'], ['D', 'D']], candidate.answer || 'A')}</select></label><label>知识点编码<input name="knowledgeCode" required value="${escapeHtml(candidate.knowledgeCode || '')}"></label></div>
          <label>解析<textarea name="analysis" rows="3">${escapeHtml(candidate.analysis || '')}</textarea></label>
          <input name="sourceType" type="hidden" value="${escapeHtml(candidate.sourceType || 'IMPORTED')}">
        </div>
        <label>处理备注<textarea name="note" rows="3" placeholder="记录判定原因，便于后续追溯"></textarea></label>
        <div class="actions"><button type="button" class="secondary" data-form-cancel>取消</button><button class="primary">提交处理</button></div>
      </form>`
    const actionSelect = document.getElementById('reviewAction')
    const questionFields = document.getElementById('questionFixFields')
    const syncAction = () => {
      const importing = actionSelect.value === 'IMPORT_QUESTION'
      questionFields.style.display = importing ? '' : 'none'
      questionFields.querySelectorAll('input,textarea,select').forEach(field => field.disabled = !importing)
    }
    actionSelect.onchange = syncAction
    syncAction()
    document.querySelector('#stagingReviewForm [data-form-cancel]').onclick = () => formDialog.close()
    document.getElementById('stagingReviewForm').onsubmit = async event => {
      event.preventDefault()
      const form = Object.fromEntries(new FormData(event.target))
      try {
        await Api.request('/api/admin/content/staging/' + item.id + '/review', {
          method: 'POST',
          body: {
            action: form.action,
            note: form.note,
            stem: form.stem,
            optionA: form.optionA,
            optionB: form.optionB,
            optionC: form.optionC,
            optionD: form.optionD,
            answer: form.answer,
            analysis: form.analysis,
            knowledgeCode: form.knowledgeCode,
            sourceType: form.sourceType
          }
        })
        formDialog.close()
        await renderContent()
      } catch (ignore) {}
    }
    formDialog.showModal()
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
