const fs = require('fs')

const args = process.argv.slice(2)
if (args.length < 2) throw new Error('usage: node prepare-saduck-jiangxi.js <output.csv> <paper.json>...')
const [outputPath, ...paperPaths] = args

const headers = ['stem', 'optionA', 'optionB', 'optionC', 'optionD', 'answer', 'analysis', 'knowledgeCode',
  'difficulty', 'standardTimeSeconds', 'sourceType', 'sourceYear', 'sourceExam', 'sourceName', 'status',
  'usageType', 'questionType']

const sectionConfig = {
  '政治理论': { code: 'COMMON_SENSE', seconds: 35 },
  '常识判断': { code: 'COMMON_SENSE', seconds: 35 },
  '言语理解': { code: 'VERBAL', seconds: 55 },
  '数量关系': { code: 'QUANTITY', seconds: 90 },
  '判断推理': { code: 'JUDGEMENT', seconds: 65 },
  '资料分析': { code: 'DATA_ANALYSIS', seconds: 90 }
}

function decodeHtml(value) {
  return String(value || '')
    .replace(/<br\s*\/?\s*>/gi, '\n')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&emsp;|&ensp;|&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/\s+/g, ' ')
    .trim()
}

function imagesOf(...values) {
  const urls = []
  for (const value of values) {
    for (const match of String(value || '').matchAll(/<img[^>]+src=["']([^"']+)["']/gi)) {
      const url = match[1].startsWith('//') ? 'https:' + match[1] : match[1]
      if (!urls.includes(url)) urls.push(url)
    }
  }
  return urls
}

function csv(value) {
  const text = String(value == null ? '' : value).replace(/\r?\n/g, ' ').replace(/\s+/g, ' ').trim()
  return '"' + text.replace(/"/g, '""') + '"'
}

function sectionsOf(model) {
  return JSON.parse(model || '[]').map(item => ({ name: item.name, start: item.snum, end: item.enum }))
}

function sectionAt(sections, number) {
  return sections.find(item => number >= item.start && number <= item.end)
}

const rows = [headers.map(csv).join(',')]
const summary = { total: 0, active: 0, draft: 0, papers: {} }

for (const paperPath of paperPaths) {
  const paper = JSON.parse(fs.readFileSync(paperPath, 'utf8'))
  const sections = sectionsOf(paper.model)
  const yearMatch = String(paper.source || '').match(/(20\d{2})年/)
  const paperSummary = { total: 0, active: 0, draft: 0 }
  for (let index = 0; index < (paper.questions || []).length; index++) {
    const question = paper.questions[index]
    const number = index + 1
    const section = sectionAt(sections, number)
    const config = (section && sectionConfig[section.name]) || sectionConfig[question.tag]
    if (!config) throw new Error(`${paper.source} 第${number}题无法识别模块`)
    if (question.type && question.type !== 'single') throw new Error(`${paper.source} 第${number}题不是单选题`)
    const options = Object.fromEntries((question.options || []).map(item => [item.label, decodeHtml(item.text)]))
    const optionCount = Object.keys(options).length
    const answerIndex = Number(question.correctAnswer)
    if (!Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex >= optionCount) {
      throw new Error(`${paper.source} 第${number}题答案无对应选项`)
    }
    const answer = String.fromCharCode(65 + answerIndex)
    const material = decodeHtml(question.materialHtml)
    const title = decodeHtml(question.titleHtml)
    const stem = material ? material + ' ' + title : title
    const images = imagesOf(question.materialHtml, question.titleHtml, ...(question.options || []).map(item => item.text))
    const analysis = decodeHtml(question.analysisHtml) + (images.length ? ` 原题图片：${images.join(' ')}` : '')
    const complete = stem && ['A', 'B', 'C', 'D'].every(key => options[key]) && analysis
    const active = complete && images.length === 0
    const status = active ? 'ACTIVE' : 'DRAFT'
    const difficulty = Math.max(20, Math.min(95, 100 - Number(question.globalAccuracy || 50)))
    rows.push([
      stem || `第${number}题（题干含图片，待媒体导入）`,
      options.A || '选项A待人工校对', options.B || '选项B待人工校对',
      options.C || '选项C待人工校对', options.D || '选项D待人工校对',
      answer, analysis || '解析待人工校对', config.code, difficulty, config.seconds, 'HISTORICAL',
      yearMatch ? yearMatch[1] : '', paper.source, 'GitHub mpbfx/gongkao（AGPL-3.0，个人学习）', status,
      'TRAINING', 'SINGLE'
    ].map(csv).join(','))
    summary.total++
    summary[status.toLowerCase()]++
    paperSummary.total++
    paperSummary[status.toLowerCase()]++
  }
  summary.papers[paper.source] = paperSummary
}

fs.writeFileSync(outputPath, '\uFEFF' + rows.join('\n'), 'utf8')
console.log(JSON.stringify(summary, null, 2))
