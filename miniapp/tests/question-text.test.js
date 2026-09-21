const assert = require('assert')
const { cleanText, cleanQuestion } = require('../utils/question-text')

assert.strictEqual(
  cleanText('&emsp;&emsp;第一段。&emsp;&emsp;第二段。'),
  '第一段。\n第二段。'
)
assert.strictEqual(cleanText('A&nbsp;&amp;&nbsp;B'), 'A & B')

const question = cleanQuestion({
  stem: '&emsp;&emsp;2024年材料',
  options: [{ optionKey: 'A', optionText: '&lt;44万亿元' }]
})
assert.strictEqual(question.stem, '2024年材料')
assert.strictEqual(question.options[0].optionText, '<44万亿元')

console.log('question-text tests passed')
