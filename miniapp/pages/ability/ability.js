const { request } = require('../../utils/request')
const { problemStatusLabel, abilityStatusLabel } = require('../../utils/display')

const SECTION_TABS = [
  { value: 'ALL', label: '全部' },
  { value: 'XINGCE', label: '行测' },
  { value: 'SHENLUN', label: '申论' }
]

const PROBLEM_TYPES = {
  MASTERY: '掌握不足',
  SPEED: '速度偏慢',
  STABILITY: '稳定性差',
  ESSAY_MISSING_POINTS: '申论要点缺失',
  ESSAY_ANALYSIS: '申论分析不足',
  ESSAY_EXPRESSION: '申论表达薄弱',
  ESSAY_STRUCTURE: '申论结构问题',
  CONTENT_GAP: '主题素材缺口'
}

function filterAbilities(abilities, section) {
  const hasSection = abilities.some(item => item.examSection)
  if (section === 'ALL' || !hasSection) return abilities
  return abilities.filter(item => item.examSection === section)
}

// 视觉层级：薄弱（分数 <40 或显著低于平均）柔和橙色；趋势 ↑ 绿 / ↓ 红；未测评灰色
function decorateAbility(item, averageScore) {
  const unassessed = item.status === 'UNASSESSED'
  const score = unassessed ? null : Number(item.masteryScore)
  const trendValue = unassessed ? null : Number(item.masteryTrend)
  let trendText = ''
  let trendClass = ''
  if (trendValue != null && !isNaN(trendValue)) {
    const rounded = Math.round(trendValue)
    if (rounded >= 1) { trendText = '↑' + rounded; trendClass = 'up' }
    else if (rounded <= -1) { trendText = '↓' + Math.abs(rounded); trendClass = 'down' }
  }
  const weak = score != null && !isNaN(score) && (score < 40 || (averageScore != null && score <= averageScore - 10))
  return Object.assign({}, item, {
    statusText: abilityStatusLabel(item.status),
    scoreText: unassessed ? '未测评' : item.masteryScore,
    scoreClass: unassessed ? 'unassessed' : (weak ? 'weak' : (trendClass === 'up' ? 'improving' : '')),
    trendText,
    trendClass,
    rowClass: weak ? 'weak' : (trendClass === 'up' ? 'improving' : ''),
    weak
  })
}

Page({
  data: { loading: true, error: '', abilities: [], visibleAbilities: [], problems: [], overview: null, sectionTabs: SECTION_TABS, section: 'ALL' },
  onShow() { this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [overview, problems] = await Promise.all([
        request({ url: '/api/abilities/overview', showLoading: false }),
        request({ url: '/api/learning-problems', showLoading: false })
      ])
      const rawAbilities = overview.abilities || []
      const evaluatedScores = rawAbilities
        .filter(item => item.status !== 'UNASSESSED' && item.masteryScore != null)
        .map(item => Number(item.masteryScore))
        .filter(value => !isNaN(value))
      const averageScore = evaluatedScores.length
        ? evaluatedScores.reduce((sum, value) => sum + value, 0) / evaluatedScores.length
        : null
      const abilities = rawAbilities.map(item => decorateAbility(item, averageScore))
      const normalized = problems.map(item => {
        let evidence = item.evidenceJson || ''
        try {
          const value = JSON.parse(evidence)
          evidence = '最近' + (value.recentCount || 0) + '题，错' + (value.incorrectCount || 0) +
            '题；掌握 ' + (value.mastery || '-') + '，速度 ' + (value.speed || '-') +
            '，稳定性 ' + (value.stability || '-') + '，可信度 ' + (value.confidence || '-')
        } catch (ignore) {}
        return Object.assign({}, item, {
          evidenceText: evidence,
          problemTypeText: PROBLEM_TYPES[item.problemType] || '学习问题',
          statusLabel: problemStatusLabel(item.status)
        })
      })
      this.setData({
        abilities, overview, problems: normalized,
        visibleAbilities: filterAbilities(abilities, this.data.section),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message })
    }
  },
  selectSection(event) {
    const section = event.currentTarget.dataset.value
    this.setData({ section, visibleAbilities: filterAbilities(this.data.abilities, section) })
  },
  openCoach() { wx.navigateTo({ url: '/pages/coach/coach' }) },
  startTraining() { wx.switchTab({ url: '/pages/learn/learn' }) }
})
