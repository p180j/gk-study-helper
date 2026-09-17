const PURPOSE_LABELS = {
  ASSESSMENT: '摸底测评',
  TRAINING: '针对训练',
  VALIDATION: '效果验证',
  MAINTENANCE: '能力保持',
  REVIEW: '错题复习'
}

const PROBLEM_STATUS_LABELS = {
  OBSERVING: '正在观察',
  CONFIRMED: '需要提升',
  PROCESSING: '正在改善',
  VERIFYING: '效果验证中',
  RESOLVED: '已改善',
  REOPENED: '再次需要关注'
}

const ABILITY_STATUS_LABELS = {
  UNASSESSED: '未测评',
  ASSESSED: '已测评'
}

const EVALUATOR_LABELS = {
  REAL_AI: 'AI 分析',
  LOCAL_RULE: '本地规则',
  LOCAL_RULE_V1: '本地规则'
}

const ABILITY_LABELS = {
  VERBAL: '言语理解',
  JUDGEMENT: '判断推理',
  QUANTITY: '数量关系',
  DATA_ANALYSIS: '资料分析',
  COMMON_SENSE: '常识判断',
  ESSAY_MATERIAL_READING: '材料阅读',
  ESSAY_INFO_EXTRACTION: '信息提取',
  ESSAY_POINT_COMPLETENESS: '要点完整性',
  ESSAY_SUMMARY: '归纳概括',
  ESSAY_ANALYSIS: '综合分析',
  ESSAY_COUNTERMEASURE: '提出对策',
  ESSAY_IMPLEMENTATION: '贯彻执行',
  ESSAY_EXPRESSION: '文字表达',
  GRASSROOTS_GOVERNANCE: '基层治理'
}

function purposeLabel(value) { return PURPOSE_LABELS[value] || '学习任务' }
function problemStatusLabel(value) { return PROBLEM_STATUS_LABELS[value] || '持续跟踪' }
function abilityStatusLabel(value) { return ABILITY_STATUS_LABELS[value] || '已测评' }
function evaluatorLabel(value) { return EVALUATOR_LABELS[value] || '智能分析' }
function abilityLabel(value) { return ABILITY_LABELS[value] || '相关能力' }

module.exports = { purposeLabel, problemStatusLabel, abilityStatusLabel, evaluatorLabel, abilityLabel }
