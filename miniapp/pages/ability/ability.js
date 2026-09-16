const { request } = require('../../utils/request')

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

Page({
  data: { loading: true, error: '', abilities: [], visibleAbilities: [], problems: [], sectionTabs: SECTION_TABS, section: 'ALL' },
  onShow() { this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [abilities, problems] = await Promise.all([
        request({ url: '/api/abilities', showLoading: false }),
        request({ url: '/api/learning-problems', showLoading: false })
      ])
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
          problemTypeText: PROBLEM_TYPES[item.problemType] || item.problemType
        })
      })
      this.setData({
        abilities, problems: normalized,
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
  }
})
