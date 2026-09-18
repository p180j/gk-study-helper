const { request } = require('../../utils/request')
const { buildAbilityView } = require('../../utils/ability-view')
const { problemStatusLabel, problemTypeLabel, localizedText } = require('../../utils/display')

function evidenceText(problem) {
  try {
    const evidence = JSON.parse(problem.evidenceJson || '{}')
    if (evidence.recentCount != null && evidence.incorrectCount != null) {
      return '最近 ' + evidence.recentCount + ' 次真实作答中出现 ' + evidence.incorrectCount + ' 次失误'
    }
    if (evidence.recentCount != null && evidence.slowCount != null) {
      return '最近 ' + evidence.recentCount + ' 次真实作答中有 ' + evidence.slowCount + ' 次耗时偏长'
    }
  } catch (ignore) {}
  return '系统会持续根据你的真实作答更新判断'
}

function axisView(axis) {
  const trend = axis.trend == null || isNaN(Number(axis.trend)) ? '' : Math.round(Number(axis.trend))
  return Object.assign({}, axis, {
    scoreText: axis.evaluated ? axis.score + ' 分' : (axis.assessing ? '摸底中' : '未测评'),
    sampleText: axis.evaluated ? '已基于 ' + axis.sampleCount + ' 次有效作答' :
      (axis.assessing ? '已完成 ' + axis.sampleCount + ' 题，满 20 题后形成能力分' : '完成相关练习后形成能力判断'),
    trendText: trend === '' || trend === 0 ? '' : (trend > 0 ? '近期提升 ' + trend : '近期波动 ' + Math.abs(trend))
  })
}

Page({
  data: {
    loading: true, error: '', overview: null, abilityView: null, radarAxes: [], problems: [],
    selectedAxisCode: '', selectedAxis: null, radarCanvasWidth: 320, radarCanvasHeight: 282
  },
  onLoad() {
    try {
      const windowWidth = wx.getSystemInfoSync().windowWidth
      const width = Math.round(windowWidth * 640 / 750)
      this.setData({ radarCanvasWidth: width, radarCanvasHeight: Math.round(width * 564 / 640) })
    } catch (ignore) {}
  },
  onShow() { this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [overview, problems] = await Promise.all([
        request({ url: '/api/abilities/overview', showLoading: false }),
        request({ url: '/api/learning-problems/core', showLoading: false })
      ])
      const abilityView = buildAbilityView(overview.abilities || [])
      const radarAxes = abilityView.axes.map(axisView)
      const normalizedProblems = (problems || []).filter(item => item.status !== 'RESOLVED').slice(0, 3).map(item => Object.assign({}, item, {
        problemTypeText: problemTypeLabel(item.problemType),
        statusText: problemStatusLabel(item.status),
        rootCauseText: item.rootCause ? localizedText(item.rootCause) : '',
        evidenceText: evidenceText(item)
      }))
      const selectedAxis = radarAxes.find(axis => axis.code === this.data.selectedAxisCode) || radarAxes[0] || null
      this.setData({ overview, abilityView, radarAxes, problems: normalizedProblems, selectedAxis, loading: false }, () => this.drawRadar())
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  /** 六轴图只绘制真实已测能力；未测评轴保留为空并在卡片中明确标注。 */
  drawRadar() {
    const axes = this.data.radarAxes || []
    if (!axes.length) return
    const ctx = wx.createCanvasContext('ability-radar', this)
    const width = this.data.radarCanvasWidth; const height = this.data.radarCanvasHeight
    const centerX = width / 2; const centerY = height / 2; const radius = Math.min(width * 0.28, height * 0.32)
    const point = (index, scale) => {
      const angle = -Math.PI / 2 + index * Math.PI / 3
      return { x: centerX + Math.cos(angle) * radius * scale, y: centerY + Math.sin(angle) * radius * scale }
    }
    ctx.clearRect(0, 0, width, height)
    ;[1 / 3, 2 / 3, 1].forEach((scale, ringIndex) => {
      ctx.beginPath()
      for (let index = 0; index < 6; index++) {
        const position = point(index, scale)
        if (index === 0) ctx.moveTo(position.x, position.y); else ctx.lineTo(position.x, position.y)
      }
      ctx.closePath(); ctx.setStrokeStyle(ringIndex === 2 ? '#cddced' : '#e5edf7'); ctx.setLineWidth(1); ctx.stroke()
    })
    axes.forEach((axis, index) => {
      const end = point(index, 1)
      ctx.beginPath(); ctx.moveTo(centerX, centerY); ctx.lineTo(end.x, end.y)
      ctx.setStrokeStyle('#dce7f4'); ctx.setLineWidth(1); ctx.stroke()
      if (!axis.evaluated) return
      const value = point(index, Math.max(0.12, Number(axis.score) / 100))
      ctx.beginPath(); ctx.moveTo(centerX, centerY); ctx.lineTo(value.x, value.y)
      ctx.setStrokeStyle('#18b99e'); ctx.setLineWidth(4); ctx.setLineCap('round'); ctx.stroke()
      ctx.beginPath(); ctx.arc(value.x, value.y, 4, 0, Math.PI * 2)
      ctx.setFillStyle('#1677ff'); ctx.fill()
    })
    if (axes.every(axis => axis.evaluated)) {
      ctx.beginPath()
      axes.forEach((axis, index) => {
        const value = point(index, Math.max(0.12, Number(axis.score) / 100))
        if (index === 0) ctx.moveTo(value.x, value.y); else ctx.lineTo(value.x, value.y)
      })
      ctx.closePath(); ctx.setFillStyle('rgba(24,185,158,0.15)'); ctx.fill()
      ctx.setStrokeStyle('#1677ff'); ctx.setLineWidth(2); ctx.stroke()
    } else {
      // 只连接相邻的已测维度，不以 0 分补齐未测维度。
      axes.forEach((axis, index) => {
        const next = axes[(index + 1) % axes.length]
        if (!axis.evaluated || !next.evaluated) return
        const current = point(index, Math.max(0.12, Number(axis.score) / 100))
        const nextPoint = point((index + 1) % axes.length, Math.max(0.12, Number(next.score) / 100))
        ctx.beginPath(); ctx.moveTo(current.x, current.y); ctx.lineTo(nextPoint.x, nextPoint.y)
        ctx.setStrokeStyle('#1677ff'); ctx.setLineWidth(2); ctx.stroke()
      })
    }
    axes.forEach((axis, index) => {
      const label = point(index, 1.2)
      ctx.setTextAlign('center'); ctx.setFillStyle(axis.evaluated ? '#365b6b' : '#8b99ad'); ctx.setFontSize(Math.max(11, Math.round(width * 0.04)))
      ctx.fillText(axis.name, label.x, label.y)
      ctx.setFillStyle(axis.evaluated ? '#1677ff' : '#8b99ad'); ctx.setFontSize(Math.max(10, Math.round(width * 0.035)))
      ctx.fillText(axis.evaluated ? axis.score + '分' : (axis.assessing ? '摸底中' : '待测评'), label.x, label.y + Math.max(15, Math.round(width * 0.07)))
    })
    ctx.draw()
  },
  selectAxis(event) {
    const code = event.currentTarget.dataset.code
    const axis = (this.data.radarAxes || []).find(item => item.code === code)
    this.setData({ selectedAxisCode: code, selectedAxis: axis || null })
  },
  startTraining(event) {
    const code = event.currentTarget.dataset.code
    const axis = (this.data.radarAxes || []).find(item => item.code === code)
    if (!axis) return
    if (code === 'SHENLUN') {
      wx.navigateTo({ url: '/pages/essay/essay' })
      return
    }
    const app = getApp()
    app.globalData.pendingExtraPractice = { code, name: axis.name, purposeName: '模块针对训练' }
    wx.switchTab({ url: '/pages/learn/learn' })
  },
  openCoach() { wx.navigateTo({ url: '/pages/coach/coach' }) }
})
