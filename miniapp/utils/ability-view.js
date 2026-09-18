const XINGCE_AXES = [
  { code: 'VERBAL', name: '言语理解' },
  { code: 'JUDGEMENT', name: '判断推理' },
  { code: 'QUANTITY', name: '数量关系' },
  { code: 'DATA_ANALYSIS', name: '资料分析' },
  { code: 'COMMON_SENSE', name: '常识判断' }
]
const ASSESSMENT_MIN_SAMPLE_COUNT = 20

function number(value) {
  const result = Number(value)
  return isNaN(result) ? 0 : Math.max(0, Math.min(100, result))
}

function hasSamples(item) {
  return Boolean(item) && Number(item.sampleCount) > 0
}

function evaluated(item) {
  return hasSamples(item) && item.status !== 'UNASSESSED' && Number(item.sampleCount) >= ASSESSMENT_MIN_SAMPLE_COUNT
}

function summary(code, name, members) {
  const measured = members.filter(evaluated)
  const sampled = members.filter(hasSamples)
  const sampledCount = sampled.reduce((sum, item) => sum + Math.max(1, Number(item.sampleCount) || 0), 0)
  if (!measured.length) {
    return { code, name, evaluated: false, assessing: sampled.length > 0, score: null, sampleCount: sampledCount, trend: null, members }
  }
  const totalSamples = measured.reduce((sum, item) => sum + Math.max(1, Number(item.sampleCount) || 0), 0)
  const score = measured.reduce((sum, item) => sum + number(item.masteryScore) * Math.max(1, Number(item.sampleCount) || 0), 0) / totalSamples
  const trendMembers = measured.filter(item => item.masteryTrend != null && !isNaN(Number(item.masteryTrend)))
  const trend = trendMembers.length
    ? trendMembers.reduce((sum, item) => sum + Number(item.masteryTrend), 0) / trendMembers.length : null
  return { code, name, evaluated: true, assessing: false, score: Math.round(score), sampleCount: totalSamples, trend, members }
}

/**
 * 将真实 AbilityProfile 收敛为用户能力图所需的六个轴。
 * 未测评维度明确保留为空，不把数据库中的 0 当作“能力差”。
 */
function buildAbilityView(abilities) {
  const list = abilities || []
  const byCode = {}
  list.forEach(item => { byCode[item.knowledgePointCode] = item })
  const xingceAxes = XINGCE_AXES.map(axis => summary(axis.code, axis.name, [byCode[axis.code]]))
  const essay = summary('SHENLUN', '申论', list.filter(item => item.examSection === 'SHENLUN'))
  const axes = xingceAxes.concat(essay)
  const xingce = summary('XINGCE', '行测综合能力', xingceAxes.filter(axis => axis.evaluated).map(axis => ({
    status: 'ASSESSED', masteryScore: axis.score, sampleCount: axis.sampleCount, masteryTrend: axis.trend
  })))
  // 行测综合分必须覆盖五个模块；不能把单个模块高分误当成整体能力。
  if (!xingceAxes.every(axis => axis.evaluated)) {
    xingce.evaluated = false
    xingce.assessing = xingceAxes.some(axis => axis.evaluated || axis.assessing)
    xingce.score = null
    xingce.sampleCount = xingceAxes.reduce((sum, axis) => sum + (axis.sampleCount || 0), 0)
  }
  return {
    axes,
    xingce,
    xingceEvaluatedCount: xingceAxes.filter(axis => axis.evaluated).length,
    xingceAssessingCount: xingceAxes.filter(axis => axis.assessing).length,
    xingceTotalCount: xingceAxes.length,
    evaluatedCount: axes.filter(axis => axis.evaluated).length,
    assessingCount: axes.filter(axis => axis.assessing).length,
    details: axes.reduce((result, axis) => { result[axis.code] = axis; return result }, {})
  }
}

module.exports = { XINGCE_AXES, ASSESSMENT_MIN_SAMPLE_COUNT, buildAbilityView }
