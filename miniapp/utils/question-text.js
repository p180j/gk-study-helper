function cleanText(value) {
  if (value == null) return value
  return String(value)
    .replace(/(?:&emsp;){2,}/gi, '\n')
    .replace(/&(?:emsp|nbsp);|&#8195;|&#x2003;/gi, ' ')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;|&apos;/gi, "'")
    .replace(/&amp;/gi, '&')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n[ \t]+/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function cleanQuestion(question) {
  if (!question) return question
  return Object.assign({}, question, {
    stem: cleanText(question.stem),
    options: (question.options || []).map(option => Object.assign({}, option, {
      optionText: cleanText(option.optionText)
    }))
  })
}

function cleanQuestions(questions) {
  return (questions || []).map(cleanQuestion)
}

module.exports = { cleanText, cleanQuestion, cleanQuestions }
