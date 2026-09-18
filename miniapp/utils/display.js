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

const TASK_STATUS_LABELS = {
  PENDING: '待开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  INSUFFICIENT_STOCK: '库存不足'
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

const PROBLEM_TYPE_LABELS = {
  MASTERY: '掌握程度需要提升',
  SPEED: '作答速度需要提升',
  STABILITY: '作答稳定性需要提升',
  ESSAY_MISSING_POINTS: '申论要点完整性需要提升',
  ESSAY_ANALYSIS: '申论分析能力需要提升',
  ESSAY_EXPRESSION: '申论表达能力需要提升',
  ESSAY_STRUCTURE: '申论结构需要提升',
  CONTENT_GAP: '申论素材储备需要补充',
  EXAM_TIME_MANAGEMENT: '考试时间分配需要提升',
  EXAM_COMPLETION: '考试完成率需要提升',
  EXAM_SECTION_STABILITY: '考试发挥稳定性需要提升',
  EXAM_ORDER_STRATEGY: '做题顺序需要调整',
  EXAM_PERFORMANCE_GAP: '专项训练与模考表现存在差距'
}

const CONFIDENCE_LABELS = { SURE: '有把握', HESITANT: '犹豫', GUESS: '猜测', UNKNOWN: '暂未标注' }
const ERROR_TYPE_LABELS = {
  UNKNOWN: '暂未标注', NOT_KNOW: '知识点不熟', FORMULA: '公式或方法不熟', CONDITION: '条件理解偏差',
  CALCULATION: '计算失误', TIMEOUT: '作答超时', CARELESS: '粗心失误'
}

const TOKEN_LABELS = Object.assign({}, PURPOSE_LABELS, PROBLEM_STATUS_LABELS, TASK_STATUS_LABELS,
  ABILITY_STATUS_LABELS, EVALUATOR_LABELS, ABILITY_LABELS, PROBLEM_TYPE_LABELS, CONFIDENCE_LABELS, ERROR_TYPE_LABELS)

function purposeLabel(value) { return PURPOSE_LABELS[value] || '学习任务' }
function problemStatusLabel(value) { return PROBLEM_STATUS_LABELS[value] || '持续跟踪' }
function taskStatusLabel(value) { return TASK_STATUS_LABELS[value] || '待开始' }
function abilityStatusLabel(value) { return ABILITY_STATUS_LABELS[value] || '已测评' }
function evaluatorLabel(value) { return EVALUATOR_LABELS[value] || '智能分析' }
function abilityLabel(value) { return ABILITY_LABELS[value] || '相关能力' }
function problemTypeLabel(value) { return PROBLEM_TYPE_LABELS[value] || '学习问题' }
function confidenceLabel(value) { return CONFIDENCE_LABELS[value] || '暂未标注' }
function errorTypeLabel(value) { return ERROR_TYPE_LABELS[value] || '暂未标注' }

/** 所有用户可见文本统一替换内部枚举；仅用于展示，不参与任何能力或计划计算。 */
function localizedText(value) {
  let text = String(value == null ? '' : value)
  Object.keys(TOKEN_LABELS).sort((left, right) => right.length - left.length).forEach(key => {
    text = text.replace(new RegExp('\\b' + key + '\\b', 'g'), TOKEN_LABELS[key])
  })
  return text
}

module.exports = {
  purposeLabel, problemStatusLabel, taskStatusLabel, abilityStatusLabel, evaluatorLabel, abilityLabel,
  problemTypeLabel, confidenceLabel, errorTypeLabel, localizedText
}
