const { request } = require('../../utils/request')

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

Page({
  data: {
    loading: true, error: '', plan: null, item: null, questions: [], questionIndex: 0,
    question: null, selectedAnswer: '', confidence: '', confidenceOptions: CONFIDENCE,
    errorIndex: 0, errorOptions: ERRORS, submitting: false, diagnosisDeciding: false, result: null
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
  onShow() {
    const pending = getApp().globalData.pendingPlanItemId
    getApp().globalData.pendingPlanItemId = null
    this.load(pending)
  },
  async load(preferredItemId) {
    this.setData({ loading: true, error: '', result: null })
    try {
      const plan = await request({ url: '/api/plan/today', showLoading: false })
      const items = plan.items || []
      const item = items.find(candidate => candidate.id === preferredItemId) || items[0]
      if (!item) {
        this.setData({ loading: false, plan, item: null, questions: [], question: null })
        return
      }
      const questions = await request({ url: '/api/plan/items/' + item.id + '/questions?limit=10', showLoading: false })
      this.setData({ loading: false, plan, item, questions, questionIndex: 0, question: questions[0] || null })
      this.resetAnswer()
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  selectTask(event) { this.load(Number(event.currentTarget.dataset.id)) },
  selectAnswer(event) {
    if (this.data.submitting || this.data.result) return
    this.setData({ selectedAnswer: event.currentTarget.dataset.key })
  },
  selectConfidence(event) {
    if (this.data.submitting || this.data.result) return
    this.setData({ confidence: event.currentTarget.dataset.value })
  },
  selectError(event) { this.setData({ errorIndex: Number(event.detail.value) }) },
  async submit() {
    if (this.data.submitting || this.data.result) return
    if (!this.data.selectedAnswer || !this.data.confidence) {
      wx.showToast({ title: '请选择答案和信心程度', icon: 'none' }); return
    }
    this.setData({ submitting: true })
    const purpose = this.data.item.purpose
    const practiceType = purpose === 'VALIDATION' ? 'VALIDATION' : (purpose === 'REVIEW' ? 'REVIEW' : 'DAILY')
    try {
      const result = await request({
        url: '/api/practice/answer', method: 'POST',
        data: {
          questionId: this.data.question.id,
          userAnswer: this.data.selectedAnswer,
          durationMs: Date.now() - this.startedAt,
          practiceType,
          confidenceType: this.data.confidence,
          errorType: this.data.errorOptions[this.data.errorIndex].value
        }
      })
      this.setData({ result, submitting: false })
    } catch (error) {
      this.setData({ submitting: false, error: error.message })
    }
  },
  nextQuestion() {
    const nextIndex = this.data.questionIndex + 1
    if (nextIndex >= this.data.questions.length) {
      wx.showToast({ title: '本任务题目已完成', icon: 'success' }); return
    }
    this.setData({ questionIndex: nextIndex, question: this.data.questions[nextIndex] })
    this.resetAnswer()
  },
  resetAnswer() {
    this.startedAt = Date.now()
    this.setData({ selectedAnswer: '', confidence: '', errorIndex: 0, result: null, submitting: false, diagnosisDeciding: false, error: '' })
  }
})
