const assert = require('assert')
const { buildAbilityView } = require('./ability-view')
const { localizedText } = require('./display')

const view = buildAbilityView([
  { knowledgePointCode: 'VERBAL', examSection: 'XINGCE', status: 'ASSESSED', masteryScore: 80, sampleCount: 20, masteryTrend: 3 },
  { knowledgePointCode: 'DATA_ANALYSIS', examSection: 'XINGCE', status: 'UNASSESSED', masteryScore: 0, sampleCount: 0 },
  { knowledgePointCode: 'ESSAY_SUMMARY', examSection: 'SHENLUN', status: 'ASSESSED', masteryScore: 60, sampleCount: 20 }
])

assert.strictEqual(view.xingceEvaluatedCount, 1)
assert.strictEqual(view.details.DATA_ANALYSIS.evaluated, false)
assert.strictEqual(view.details.DATA_ANALYSIS.score, null)
assert.strictEqual(view.details.VERBAL.score, 80)
assert.strictEqual(view.details.SHENLUN.score, 60)
assert.strictEqual(localizedText('TRAINING / STABILITY / UNKNOWN'), '针对训练 / 作答稳定性需要提升 / 暂未标注')
assert.strictEqual(buildAbilityView([{ knowledgePointCode: 'VERBAL', examSection: 'XINGCE', status: 'ASSESSED', masteryScore: 184, sampleCount: 20 }]).details.VERBAL.score, 100)
const assessing = buildAbilityView([{ knowledgePointCode: 'VERBAL', examSection: 'XINGCE', status: 'ASSESSED', masteryScore: 84, sampleCount: 10 }])
assert.strictEqual(assessing.details.VERBAL.evaluated, false)
assert.strictEqual(assessing.details.VERBAL.assessing, true)
assert.strictEqual(assessing.details.VERBAL.score, null)
assert.strictEqual(assessing.xingce.evaluated, false)
assert.strictEqual(assessing.xingce.score, null)
console.log('miniapp ability view tests passed')
