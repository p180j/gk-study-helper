const { request } = require('../../utils/request')
const { purposeLabel, abilityLabel, taskStatusLabel, problemTypeLabel, localizedText } = require('../../utils/display')
const { cleanQuestions } = require('../../utils/question-text')

const CONFIDENCE = [
  { value: 'SURE', label: '确定' },
  { value: 'HESITANT', label: '犹豫' },
  { value: 'GUESS', label: '猜测' }
]
const ERRORS = [
  { value: 'UNKNOWN', label: '暂不标注' },
  { value: 'NOT_KNOW', label: '不会' },
  { value: 'FORMULA', label: '公式' },
  { value: 'CONDITION', label: '条件理解' },
  { value: 'CALCULATION', label: '计算' },
  { value: 'TIMEOUT', label: '超时' },
  { value: 'CARELESS', label: '粗心' }
]
const STOCK_SHORTAGE_NOTICE = '当前可用题目不足，暂无法开始本组训练'

/** 秒数转时长文本：119分52秒 / 32秒 */
function formatSeconds(seconds) {
  const total = Math.max(0, Math.floor(seconds))
  const minutes = Math.floor(total / 60)
  const rest = total % 60
  return minutes > 0 ? minutes + '分' + (rest > 0 ? rest + '秒' : '') : total + '秒'
}

function numberText(value) {
  const num = Number(value)
  if (isNaN(num)) return '0'
  return String(Math.round(num * 10) / 10)
}

/** 掌握度变化解释：纯展示层文案，不参与任何算法 */
function masteryExplain(oldValue, newValue, correct) {
  const oldNum = Number(oldValue); const newNum = Number(newValue)
  if (isNaN(oldNum) || isNaN(newNum) || oldNum === newNum) return '基本持平'
  if (newNum > oldNum) return correct ? '回答正确，掌握表现提升' : '掌握表现略有回升'
  return correct ? '掌握表现有所回落' : '回答错误，掌握表现下降'
}

/** 速度分变化解释：速度分越低越慢 */
function speedExplain(oldValue, newValue) {
  const oldNum = Number(oldValue); const newNum = Number(newValue)
  if (isNaN(oldNum) || isNaN(newNum) || oldNum === newNum) return '基本持平'
  return newNum > oldNum ? '答题速度提升' : '本题耗时高于该题型参考用时'
}

function decorateChanges(changes, correct) {
  return (changes || []).map(change => {
    const masteryExplainText = masteryExplain(change.oldMastery, change.newMastery, correct)
    const speedExplainText = speedExplain(change.oldSpeed, change.newSpeed)
    return Object.assign({}, change, {
      masteryText: numberText(change.oldMastery) + ' → ' + numberText(change.newMastery),
      masteryExplainText,
      speedText: numberText(change.oldSpeed) + ' → ' + numberText(change.newSpeed),
      speedExplainText
    })
  })
}

Page({
  data: {
    loading: true, error: '', plan: null, taskItems: [], item: null, questions: [], questionIndex: 0,
    question: null, selectedAnswer: '', confidence: '', confidenceOptions: CONFIDENCE,
    errorIndex: 0, errorOptions: ERRORS, submitting: false, diagnosisDeciding: false, result: null,
    showPractice: false,
    // 显式三态：answering 答题 / result 结果反馈 / task_completed 本组完成
    phase: 'answering',
    completedCount: 0, targetCount: 0, taskDone: false, insufficientStock: false,
    practiceNotice: '', fetchingMore: false, restarting: false,
    extraRound: false, freeKnowledgePointCode: '', sessionSummary: null, inventory: null,
    showTaskPicker: false, coreProblems: [], selectedTaskId: null
  },
  async onShow() {
    const app = getApp()
    const pending = app.globalData.pendingPlanItemId
    const startPractice = Boolean(app.globalData.pendingStartPractice)
    const pendingExtraPractice = app.globalData.pendingExtraPractice
    app.globalData.pendingPlanItemId = null
    app.globalData.pendingStartPractice = false
    app.globalData.pendingExtraPractice = null
    await this.load(pending, startPractice)
    await this.loadInventory()
    await this.loadCoreProblems()
    if (pendingExtraPractice && pendingExtraPractice.code) {
      await this.startExtraPractice(pendingExtraPractice.code, pendingExtraPractice.name, pendingExtraPractice.purposeName)
    }
  },
  /** 自由学习模块库存：接口失败静默不展示 */
  async loadInventory() {
    try {
      const inventory = await request({ url: '/api/practice/inventory-summary', showLoading: false, silent: true })
      if (Array.isArray(inventory) && inventory.length) this.setData({ inventory })
    } catch (error) { /* 静默降级 */ }
  },
  async loadCoreProblems() {
    try {
      const problems = await request({ url: '/api/learning-problems/core', showLoading: false, silent: true })
      this.setData({ coreProblems: (problems || []).filter(item => item.status !== 'RESOLVED').slice(0, 2).map(item => Object.assign({}, item, {
        problemTypeText: problemTypeLabel(item.problemType),
        recommendation: localizedText(item.rootCause || item.description || '根据最近真实作答安排巩固')
      })) })
    } catch (error) { this.setData({ coreProblems: [] }) }
  },
  async load(preferredItemId, startPractice) {
    this.setData({ loading: true, error: '', result: null })
    this.sessionStats = null
    try {
      const plan = await request({ url: '/api/plan/today', showLoading: false })
      const items = (plan.items || []).map(candidate => Object.assign({}, candidate, {
        purposeLabel: purposeLabel(candidate.purpose),
        statusLabel: taskStatusLabel(candidate.status)
      }))
      plan.items = items
      const taskItems = items.filter(candidate => candidate.itemType !== 'ESSAY' && candidate.itemType !== 'READING')
      const item = taskItems.find(candidate => candidate.id === preferredItemId) ||
        taskItems.find(candidate => candidate.status !== 'COMPLETED' && candidate.status !== 'INSUFFICIENT_STOCK') || taskItems[0]
      if (!item) {
        this.setData({ loading: false, plan, taskItems, item: null, questions: [], question: null, showPractice: false })
        return
      }
      const targetCount = item.targetQuestionCount || 0
      const completedCount = item.completedQuestionCount || 0
      const taskDone = item.status === 'COMPLETED' || (targetCount > 0 && completedCount >= targetCount)
      const base = {
        loading: false, plan, taskItems, item,
        targetCount, completedCount, taskDone,
        insufficientStock: false, practiceNotice: '', extraRound: false,
        sessionSummary: null, result: null,
        showPractice: Boolean(startPractice), showTaskPicker: false, selectedTaskId: item.id
      }
      if (taskDone) {
        this.setData(Object.assign(base, { questions: [], questionIndex: 0, question: null, phase: 'task_completed' }))
        return
      }
      if (item.status === 'INSUFFICIENT_STOCK') {
        this.setData(Object.assign(base, {
          questions: [], questionIndex: 0, question: null, phase: 'answering',
          insufficientStock: true, practiceNotice: STOCK_SHORTAGE_NOTICE
        }))
        return
      }
      const limit = Math.min(20, Math.max(1, targetCount || 10))
      const questions = await request({ url: '/api/plan/items/' + item.id + '/questions?limit=' + limit, showLoading: false })
      const list = cleanQuestions(questions)
      const insufficient = list.length === 0
      this.setData(Object.assign(base, {
        questions: list, questionIndex: 0, question: list[0] || null, phase: 'answering',
        targetCount: targetCount || list.length, insufficientStock: insufficient,
        practiceNotice: insufficient ? STOCK_SHORTAGE_NOTICE : ''
      }))
      this.resetAnswer()
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  openTaskPicker() { this.setData({ showTaskPicker: true }) },
  closeTaskPicker() { this.setData({ showTaskPicker: false }) },
  selectTask(event) { this.load(Number(event.currentTarget.dataset.id), false) },
  reload() { this.load() },
  startCurrent() {
    if (!this.data.item) return
    // 从结果/完成态再次进入时刷新取题（已答题会被后端排除）
    if (this.data.phase !== 'answering' || this.data.result) {
      this.load(this.data.item.id)
      return
    }
    if (this.data.question) this.setData({ showPractice: true })
  },
  closePractice() {
    if (this.data.extraRound) {
      this.load(null, false)
      return
    }
    this.setData({ showPractice: false })
  },
  async startFreePractice(event) {
    const code = event.currentTarget.dataset.code
    const module = (this.data.inventory || []).find(candidate => candidate.moduleCode === code)
    if (!code || !module) return
    return this.startExtraPractice(code, module.moduleName, '专项训练')
  },
  async startProblemPractice(event) {
    const code = event.currentTarget.dataset.code
    const name = event.currentTarget.dataset.name
    if (!code) return
    return this.startExtraPractice(code, name, '问题针对训练')
  },
  async startExtraPractice(code, name, purposeName) {
    this.setData({ loading: true, error: '' })
    try {
      const questions = await request({ url: '/api/questions/practice?knowledgePointCode=' + encodeURIComponent(code) + '&limit=3', showLoading: false })
      const list = cleanQuestions(questions)
      if (!list.length) {
        this.setData({ loading: false, error: '该模块当前没有可练题目' })
        return
      }
      this.sessionStats = null
      this.setData({
        loading: false, showPractice: true, extraRound: true, freeKnowledgePointCode: code,
        item: { id: null, knowledgePointCode: code, knowledgePointName: name, purpose: 'TRAINING', purposeLabel: purposeName },
        questions: list, questionIndex: 0, question: list[0], completedCount: 0, targetCount: list.length,
        taskDone: false, insufficientStock: false, practiceNotice: '', sessionSummary: null, result: null, phase: 'answering'
      })
      this.resetAnswer()
    } catch (error) { this.setData({ loading: false, error: error.message }) }
  },
  goEssay() { wx.navigateTo({ url: '/pages/essay/essay' }) },
  goReading() { wx.navigateTo({ url: '/pages/reading/reading' }) },
  goMock() { wx.navigateTo({ url: '/pages/mock/mock' }) },
  async goReview() {
    const items = (this.data.plan && this.data.plan.items) || []
    const review = items.find(candidate => candidate.itemType === 'REVIEW' || candidate.purpose === 'REVIEW')
    if (!review) {
      try {
        const history = await request({ url: '/api/practice/answers?size=20', showLoading: false, silent: true })
        const wrong = (history || []).find(item => !item.correct)
        let knowledge = null
        try { knowledge = wrong && JSON.parse(wrong.knowledgeSnapshot || '[]')[0] } catch (ignore) {}
        if (knowledge && knowledge.code) {
          await this.startExtraPractice(knowledge.code, knowledge.name || '错题相关知识点', '错题回顾')
          return
        }
      } catch (error) { /* 无历史时走统一中文提示 */ }
      wx.showToast({ title: '暂无可复习的错题，先完成几道练习吧', icon: 'none' })
      return
    }
    this.load(review.id, false)
  },
  selectAnswer(event) {
    if (this.data.phase !== 'answering' || this.data.submitting) return
    this.setData({ selectedAnswer: event.currentTarget.dataset.key })
  },
  selectConfidence(event) {
    if (this.data.phase !== 'answering' || this.data.submitting) return
    this.setData({ confidence: event.currentTarget.dataset.value })
  },
  /** 结果卡内补选错因：仅答错后展示，变更即提交 */
  async selectError(event) {
    const errorIndex = Number(event.detail.value)
    this.setData({ errorIndex })
    const result = this.data.result
    if (!result || !result.answerRecordId || this.errorTypeSaving) return
    this.errorTypeSaving = true
    try {
      await request({
        url: '/api/practice/answer/' + result.answerRecordId + '/error-type',
        method: 'POST',
        data: { errorType: this.data.errorOptions[errorIndex].value },
        showLoading: false,
        silent: true
      })
    } catch (error) {
      wx.showToast({ title: '错因保存失败，请重试', icon: 'none' })
    }
    this.errorTypeSaving = false
  },
  async decideDiagnosis(event) {
    if (this.data.diagnosisDeciding || !this.data.result || !this.data.result.errorDiagnosis) return
    const confirmed = event.currentTarget.dataset.confirmed === true || event.currentTarget.dataset.confirmed === 'true'
    this.setData({ diagnosisDeciding: true })
    try {
      const diagnosis = await request({
        url: '/api/error-diagnoses/' + this.data.result.errorDiagnosis.id + '/decision',
        method: 'POST', data: { confirmed }
      })
      this.setData({ 'result.errorDiagnosis': diagnosis, diagnosisDeciding: false })
      wx.showToast({ title: confirmed ? '已确认错因' : '已标记不是', icon: 'success' })
    } catch (error) {
      this.setData({ diagnosisDeciding: false, error: error.message })
    }
  },
  async submit() {
    if (this.data.phase !== 'answering' || this.data.submitting || !this.data.question) return
    if (!this.data.selectedAnswer || !this.data.confidence) {
      wx.showToast({ title: '请选择答案和信心程度', icon: 'none' }); return
    }
    this.setData({ submitting: true })
    const purpose = this.data.item.purpose
    const practiceType = this.data.extraRound ? 'EXTRA' :
      (purpose === 'VALIDATION' ? 'VALIDATION' : (purpose === 'REVIEW' ? 'REVIEW' : 'DAILY'))
    const durationMs = Date.now() - this.startedAt
    const payload = {
      questionId: this.data.question.id,
      userAnswer: this.data.selectedAnswer,
      durationMs,
      practiceType,
      confidenceType: this.data.confidence
    }
    // 再练一组不关联计划任务进度，其余作答均携带 planItemId
    if (!this.data.extraRound) payload.planItemId = this.data.item.id
    try {
      const result = await request({ url: '/api/practice/answer', method: 'POST', data: payload })
      const names = {}
      ;(this.data.question.knowledgePoints || []).forEach(knowledge => { names[knowledge.code] = knowledge.name })
      result.abilityChanges = decorateChanges(
        (result.abilityChanges || []).map(change => Object.assign({}, change, {
          knowledgePointName: names[change.knowledgePointCode] || abilityLabel(change.knowledgePointCode)
        })),
        result.correct
      )
      if (result.errorDiagnosis) {
        result.errorDiagnosis = Object.assign({}, result.errorDiagnosis, {
          suspectedCause: localizedText(result.errorDiagnosis.suspectedCause),
          aiExplanation: localizedText(result.errorDiagnosis.aiExplanation)
        })
      }
      this.recordSession(result, durationMs)
      const progress = result.taskProgress
      const patch = { result, submitting: false, phase: 'result', error: '' }
      if (progress && !this.data.extraRound) {
        if (progress.targetQuestionCount) patch.targetCount = progress.targetQuestionCount
        patch.completedCount = progress.completedQuestionCount || 0
        patch.insufficientStock = Boolean(progress.insufficientStock)
        patch.taskDone = Boolean(progress.completed) ||
          (progress.targetQuestionCount > 0 && (progress.completedQuestionCount || 0) >= progress.targetQuestionCount)
      } else {
        const completedCount = this.data.completedCount + 1
        patch.completedCount = completedCount
        patch.taskDone = this.data.targetCount > 0 && completedCount >= this.data.targetCount
      }
      this.setData(patch)
    } catch (error) {
      this.setData({ submitting: false, error: error.message })
    }
  },
  /** 继续下一题：本地有题直接取，本地用尽且未达标则向后端补拉 */
  async nextQuestion() {
    if (this.data.phase !== 'result' || this.data.taskDone || this.data.fetchingMore) return
    const nextIndex = this.data.questionIndex + 1
    if (nextIndex < this.data.questions.length) {
      this.setData({ questionIndex: nextIndex, question: this.data.questions[nextIndex], phase: 'answering', taskDone: false })
      this.resetAnswer()
      return
    }
    const remaining = this.data.targetCount - this.data.completedCount
    if (remaining <= 0) return
    this.setData({ fetchingMore: true })
    try {
      const limit = Math.min(20, Math.max(1, remaining))
        const url = this.data.extraRound
          ? '/api/questions/practice?knowledgePointCode=' + encodeURIComponent(this.data.freeKnowledgePointCode || this.data.item.knowledgePointCode) + '&limit=' + limit
          : '/api/plan/items/' + this.data.item.id + '/questions?limit=' + limit
        const more = await request({ url, showLoading: false })
      const existingIds = {}
      this.data.questions.forEach(question => { existingIds[question.id] = true })
      const list = cleanQuestions(more).filter(question => !existingIds[question.id])
      if (!list.length) {
        this.setData({ fetchingMore: false, insufficientStock: true })
        return
      }
      this.setData({
        fetchingMore: false,
        questions: this.data.questions.concat(list),
        questionIndex: this.data.questions.length,
        question: list[0],
        phase: 'answering',
        taskDone: false
      })
      this.resetAnswer()
    } catch (error) {
      this.setData({ fetchingMore: false })
    }
  },
  /** 结果态查看本组总结：进入 task_completed */
  goTaskSummary() {
    if (this.data.phase !== 'result') return
    this.buildSessionSummary()
    this.setData({ phase: 'task_completed' })
  },
  /** 再练一组：重新取题并重置本地统计，不携带 planItemId */
  async restartPractice() {
    if (this.data.restarting || !this.data.item) return
    this.setData({ restarting: true })
    try {
      const limit = Math.min(20, Math.max(1, this.data.targetCount || 10))
      const url = this.data.extraRound
        ? '/api/questions/practice?knowledgePointCode=' + encodeURIComponent(this.data.freeKnowledgePointCode || this.data.item.knowledgePointCode) + '&limit=' + limit
        : '/api/plan/items/' + this.data.item.id + '/questions?limit=' + limit
      const questions = await request({ url, showLoading: false })
      const list = cleanQuestions(questions)
      if (!list.length) {
        this.setData({
          restarting: false, phase: 'answering', question: null,
          insufficientStock: true, practiceNotice: STOCK_SHORTAGE_NOTICE
        })
        return
      }
      this.sessionStats = null
      this.setData({
        restarting: false, extraRound: true,
        questions: list, questionIndex: 0, question: list[0],
        completedCount: 0, targetCount: list.length,
        taskDone: false, insufficientStock: false, practiceNotice: '',
        sessionSummary: null, result: null, phase: 'answering'
      })
      this.resetAnswer()
    } catch (error) {
      this.setData({ restarting: false })
    }
  },
  /** 本次会话累计：答完数、正确数、总耗时、知识点能力变化（首个旧值 → 最新新值） */
  recordSession(result, durationMs) {
    if (!this.sessionStats) this.sessionStats = { answered: 0, correct: 0, totalDurationMs: 0, ability: {} }
    const stats = this.sessionStats
    stats.answered += 1
    if (result.correct) stats.correct += 1
    stats.totalDurationMs += durationMs || 0
    ;(result.abilityChanges || []).forEach(change => {
      const code = change.knowledgePointCode
      const current = stats.ability[code]
      if (!current) {
        stats.ability[code] = {
          code,
          name: change.knowledgePointName,
          oldMastery: numberText(change.oldMastery), newMastery: numberText(change.newMastery),
          oldSpeed: numberText(change.oldSpeed), newSpeed: numberText(change.newSpeed)
        }
      } else {
        current.name = change.knowledgePointName || current.name
        current.newMastery = numberText(change.newMastery)
        current.newSpeed = numberText(change.newSpeed)
      }
    })
  },
  buildSessionSummary() {
    const stats = this.sessionStats
    if (!stats || !stats.answered) {
      this.setData({ sessionSummary: null })
      return
    }
    const changes = Object.keys(stats.ability).map(code => stats.ability[code])
    this.setData({
      sessionSummary: {
        answered: stats.answered,
        accuracy: Math.round(stats.correct * 100 / stats.answered),
        avgDurationText: formatSeconds(stats.totalDurationMs / stats.answered / 1000),
        changes
      }
    })
  },
  resetAnswer() {
    this.startedAt = Date.now()
    this.setData({ selectedAnswer: '', confidence: '', errorIndex: 0, result: null, submitting: false, diagnosisDeciding: false, error: '' })
  }
})
