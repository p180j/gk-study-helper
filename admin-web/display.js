(function () {
  const maps = {
    status: { ACTIVE: '启用', DRAFT: '草稿', SUSPENDED: '暂停', ARCHIVED: '已归档', PENDING: '待开始', IN_PROGRESS: '进行中', COMPLETED: '已完成', NEEDS_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已驳回', SUCCESS: '成功', FAILED: '失败', PENDING_CONFIRMATION: '待确认', CONFIRMED: '已确认' },
    module: { VERBAL: '言语理解', JUDGEMENT: '判断推理', QUANTITY: '数量关系', DATA_ANALYSIS: '资料分析', COMMON_SENSE: '常识判断', ESSAY: '申论' },
    purpose: { ASSESSMENT: '摸底测评', TRAINING: '针对训练', VALIDATION: '效果验证', MAINTENANCE: '巩固保持', REVIEW: '复习' },
    problem: { MASTERY: '掌握不足', SPEED: '速度不足', STABILITY: '稳定性不足' },
    problemStatus: { OBSERVING: '观察中', CONFIRMED: '已确认', PROCESSING: '改善中', VERIFYING: '验证中', RESOLVED: '已解决', REOPENED: '再次出现' },
    confidence: { SURE: '确定', HESITANT: '犹豫', GUESS: '猜测', UNKNOWN: '未知' },
    error: { UNKNOWN: '未归类', NOT_KNOW: '知识盲区', FORMULA: '公式错误', CONDITION: '条件理解错误', CALCULATION: '计算错误', TIMEOUT: '超时', CARELESS: '粗心' },
    practice: { DAILY: '每日计划', EXTRA: '自由练习', SPECIAL: '专项训练', REVIEW: '错题复习', VALIDATION: '效果验证', MOCK: '模考' },
    usage: { TRAINING: '训练', VALIDATION: '验证', MOCK_RESERVED: '模考保留' },
    source: { HISTORICAL: '历年真题', MANUAL: '人工录入', IMPORTED: '批量导入', AI_GENERATED: 'AI 原创', AI_VARIANT: 'AI 变式' },
    abilityStatus: { UNASSESSED: '未测评', ASSESSING: '摸底中', ASSESSED: '已评估' },
    aiStatus: { CONFIGURED: '已配置', UNCONFIGURED: '未配置', SUCCESS: '连接正常', FAILED: '连接失败', PENDING_CONFIRMATION: '待确认', CONFIRMED: '已确认' },
    itemType: { QUESTION_SET: '题目训练', REVIEW: '复习任务' }, type: { SINGLE: '单选题', MULTIPLE: '多选题' }
  }
  function text(kind, value) { if (value === null || value === undefined || value === '') return '—'; const mapped = maps[kind] && maps[kind][value]; if (!mapped && maps[kind]) console.warn('[Display] 未登记枚举：', kind, value); return mapped || String(value) }
  function tone(value) { if (['ACTIVE', 'SUCCESS', 'COMPLETED', 'ASSESSED', 'RESOLVED', 'CONFIRMED'].includes(value)) return 'positive'; if (['FAILED', 'REJECTED', 'SUSPENDED', 'REOPENED'].includes(value)) return 'danger'; if (['PENDING', 'DRAFT', 'OBSERVING', 'PENDING_CONFIRMATION', 'NEEDS_REVIEW', 'UNASSESSED'].includes(value)) return 'warning'; return 'neutral' }
  window.Display = { text, tone }
})()
