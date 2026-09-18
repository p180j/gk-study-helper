const { request } = require('../../utils/request')
const STATUS = { CORRECT: '正确', WRONG: '错误', SKIPPED: '主动跳过', UNANSWERED: '未作答', TIMEOUT: '单题超时', COMPLETED: '已完成' }
/** 秒数转倒计时文本：01:59:52 / 59:52 */
function formatCountdown(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds)); const h = Math.floor(s / 3600); const m = Math.floor((s % 3600) / 60); const sec = s % 60
  const mm = String(m).padStart(2, '0'); const ss = String(sec).padStart(2, '0')
  return h > 0 ? String(h).padStart(2, '0') + ':' + mm + ':' + ss : mm + ':' + ss
}
/** 秒数转时长文本：119分52秒 / 32秒 */
function formatSeconds(s) {
  const total = Math.max(0, Math.floor(s)); const m = Math.floor(total / 60); const sec = total % 60
  return m > 0 ? m + '分' + (sec > 0 ? sec + '秒' : '') : total + '秒'
}
Page({
  data: { loading: true, error: '', mode: 'list', papers: [], paper: null, sessionView: null, currentIndex: 0, selectedAnswer: '', essayAnswer: '', saving: false, result: null, secondsLeft: 0 },
  onLoad(options) { if (options && options.sessionId) this.loadResult(options.sessionId); else this.loadPapers() }, onUnload() { this.stopTimer() },
  async loadPapers() { this.setData({ loading: true, error: '', mode: 'list' }); try { const papers = await request({ url: '/api/mock/papers', showLoading: false }); this.setData({ papers, loading: false }) } catch (e) { this.setData({ error: e.message, loading: false }) } },
  async loadResult(sessionId) { this.setData({ loading: true, error: '' }); try { const result = await request({ url: '/api/mock/sessions/' + sessionId + '/result', showLoading: false }); this.decorateResult(result); this.setData({ result, mode: 'result', loading: false }) } catch (e) { this.setData({ error: e.message, mode: 'list', loading: false }) } },
  decorateResult(result) {
    result.answers = (result.answers || []).map(a => Object.assign({}, a, { statusText: STATUS[a.resultStatus] || '已记录' }))
    result.sectionStats = (result.sectionStats || []).map(s => Object.assign({}, s, { avgText: formatSeconds(Math.round((s.averageDurationMs || 0) / 1000)) }))
    result.calibrations = (result.calibrations || []).map(c => Object.assign({}, c, { dailyText: Number(c.dailyAbility) > 0 ? c.dailyAbility : '未测评' }))
  },
  async showInstructions(e) { this.setData({ loading: true, error: '' }); try { const paper = await request({ url: '/api/mock/papers/' + e.currentTarget.dataset.id, showLoading: false }); this.setData({ paper, loading: false, mode: 'instructions' }) } catch (err) { this.setData({ error: err.message, loading: false }) } },
  async start() { if (this.data.saving) return; this.setData({ saving: true }); try { const view = await request({ url: '/api/mock/papers/' + this.data.paper.id + '/sessions', method: 'POST' }); this.setData({ sessionView: view, mode: 'exam', currentIndex: 0, saving: false }); this.resetCurrent(); this.startTimer() } catch (e) { this.setData({ saving: false, error: e.message }) } },
  startTimer() { this.stopTimer(); this.updateTimer(); this.timer = setInterval(() => this.updateTimer(), 1000) }, stopTimer() { if (this.timer) clearInterval(this.timer); this.timer = null },
  updateTimer() { const text = String(this.data.sessionView.deadline || '').replace(/-/g, '/').replace('T', ' '); const deadline = new Date(text).getTime(); if (isNaN(deadline)) { this.stopTimer(); this.setData({ error: '考试计时初始化失败，请返回重试' }); return } const secondsLeft = Math.max(0, Math.floor((deadline - Date.now()) / 1000)); this.setData({ secondsLeft, timeText: formatCountdown(secondsLeft) }); if (secondsLeft === 0) { this.stopTimer(); this.submit(true) } },
  selectAnswer(e) { if (!this.data.saving) this.setData({ selectedAnswer: e.currentTarget.dataset.key }) }, inputEssay(e) { this.setData({ essayAnswer: e.detail.value }) },
  async save(skip) { if (this.data.saving) return; const item = this.current(); const answer = item.itemType === 'ESSAY' ? this.data.essayAnswer : this.data.selectedAnswer; if (!skip && !answer) return wx.showToast({ title: '请先作答', icon: 'none' }); this.setData({ saving: true }); try { await request({ url: '/api/mock/sessions/' + this.data.sessionView.session.id + (skip ? '/skip' : '/answers'), method: 'POST', data: { paperItemId: item.id, userAnswer: answer, durationMs: Date.now() - this.itemStartedAt, startedAtEpochMs: this.itemStartedAt } }); this.setData({ saving: false }); this.next() } catch (e) { this.setData({ saving: false, error: e.message }) } },
  answer() { this.save(false) }, skip() { this.save(true) }, next() { if (this.data.currentIndex + 1 >= this.data.sessionView.items.length) return wx.showToast({ title: '已到最后一题', icon: 'none' }); this.setData({ currentIndex: this.data.currentIndex + 1 }); this.resetCurrent() }, previous() { if (this.data.currentIndex > 0) { this.setData({ currentIndex: this.data.currentIndex - 1 }); this.resetCurrent() } }, jump(e) { this.setData({ currentIndex: Number(e.currentTarget.dataset.index) }); this.resetCurrent() },
  resetCurrent() { this.itemStartedAt = Date.now(); this.setData({ selectedAnswer: '', essayAnswer: '' }) }, current() { return this.data.sessionView.items[this.data.currentIndex] },
  confirmSubmit() { wx.showModal({ title: '确认交卷', content: '未作答题目将记为未答，交卷后不能修改。', success: r => { if (r.confirm) this.submit(false) } }) },
  async submit(auto) { if (this.data.saving || this.data.mode !== 'exam') return; this.setData({ saving: true }); try { const result = await request({ url: '/api/mock/sessions/' + this.data.sessionView.session.id + '/submit', method: 'POST' }); this.decorateResult(result); this.stopTimer(); this.setData({ result, mode: 'result', saving: false }); if (auto) wx.showToast({ title: '时间到，已自动交卷', icon: 'none' }) } catch (e) { this.setData({ saving: false, error: e.message }) } },
  backList() { this.stopTimer(); this.loadPapers() }
})
