const fs = require('fs')

const sourcePath = process.argv[2]
const outputPath = process.argv[3]
if (!sourcePath || !outputPath) throw new Error('usage: node prepare-fei98-question-bank.js <questions.json> <output.csv>')

const categoryConfig = {
  '言语理解': { code: 'VERBAL', seconds: 55 },
  '判断推理': { code: 'JUDGEMENT', seconds: 65 },
  '数量关系': { code: 'QUANTITY', seconds: 90 },
  '资料分析': { code: 'DATA_ANALYSIS', seconds: 90 },
  '常识判断': { code: 'COMMON_SENSE', seconds: 35 }
}
const headers = ['stem', 'optionA', 'optionB', 'optionC', 'optionD', 'answer', 'analysis', 'knowledgeCode',
  'difficulty', 'standardTimeSeconds', 'sourceType', 'sourceYear', 'sourceExam', 'sourceName', 'status',
  'usageType', 'questionType']

function csv(value) {
  const text = String(value == null ? '' : value)
    .replace(/&(?:emsp|nbsp);|&#8195;|&#x2003;/gi, ' ')
    .replace(/&lt;/gi, '<').replace(/&gt;/gi, '>').replace(/&quot;/gi, '"')
    .replace(/&#39;|&apos;/gi, "'").replace(/&amp;/gi, '&')
    .replace(/\r?\n/g, ' ').replace(/\s+/g, ' ').trim()
  return '"' + text.replace(/"/g, '""') + '"'
}

const questions = JSON.parse(fs.readFileSync(sourcePath, 'utf8'))
const rows = [headers.map(csv).join(',')]
const summary = { total: questions.length, active: 0, draft: 0, categories: {}, years: {}, provinces: {} }

for (const question of questions) {
  const config = categoryConfig[question.category]
  if (!config) throw new Error('未知模块: ' + question.category)
  const images = Array.isArray(question.images) ? question.images.filter(Boolean) : []
  const options = question.options || {}
  const completeOptions = Object.keys(options).length >= 2
  const active = completeOptions && images.length === 0
  const optionText = key => options[key] || (!completeOptions && images[Number(key.charCodeAt(0) - 65)]
    ? '图片选项' + key + '（待媒体导入）' : '')
  const imageNote = images.length ? ' 原题图片：' + images.join(' ') : ''
  const year = question.sourceMeta && question.sourceMeta.year ? String(question.sourceMeta.year) : ''
  const province = question.sourceMeta && question.sourceMeta.province ? question.sourceMeta.province : '未知地区'
  const difficulty = Math.min(95, Math.max(20, 20 + Number(question.difficulty || 2) * 15))
  const row = [
    question.stem, optionText('A'), optionText('B'), optionText('C'), optionText('D'), question.answer,
    (question.explanation || '') + imageNote, config.code, difficulty, config.seconds, 'IMPORTED', year,
    province + '公务员考试《行测》', 'GitHub fei98/civil-service-exam-prep（个人学习）',
    active ? 'ACTIVE' : 'DRAFT', 'TRAINING', 'SINGLE'
  ]
  rows.push(row.map(csv).join(','))
  summary[active ? 'active' : 'draft']++
  summary.categories[question.category] = (summary.categories[question.category] || 0) + 1
  summary.years[year || 'UNKNOWN'] = (summary.years[year || 'UNKNOWN'] || 0) + 1
  summary.provinces[province] = (summary.provinces[province] || 0) + 1
}

fs.writeFileSync(outputPath, '\uFEFF' + rows.join('\n'), 'utf8')
console.log(JSON.stringify(summary, null, 2))
