"""Convert paired Jiangxi exam/answer PDFs to the project's question import CSV.

Usage:
    python prepare-jiangxi-papers.py manifest.json output.csv

The manifest is a JSON array containing paperPdf, answerPdf/answerText, year and exam.
PDF parsing requires pdfplumber. Questions that depend on page images or cannot be
parsed into four complete options are deliberately imported as DRAFT.
"""

import csv
import json
import re
import sys
from pathlib import Path

import pdfplumber


HEADERS = [
    "stem", "optionA", "optionB", "optionC", "optionD", "answer", "analysis", "knowledgeCode",
    "difficulty", "standardTimeSeconds", "sourceType", "sourceYear", "sourceExam", "sourceName",
    "status", "usageType", "questionType",
]

SECTION_CONFIG = {
    "常识判断": ("COMMON_SENSE", 35),
    "言语理解与表达": ("VERBAL", 55),
    "数量关系": ("QUANTITY", 90),
    "判断推理": ("JUDGEMENT", 65),
    "资料分析": ("DATA_ANALYSIS", 90),
}

IMAGE_CLUES = re.compile(
    r"如下图|下图|图中|图形|图示|表中|统计图|示意图|展开图|折叠|拼合|阴影部分|问号处|所给图形"
)
QUESTION_START = re.compile(r"(?m)^(\d{1,3})[、.]")
ANSWER_START = re.compile(r"(?m)^(\d{1,3})、正确答案：\s*([A-D])")
OPTION_START = re.compile(r"(?<![A-Za-z])([A-D])[、．.]")


def normalize(text):
    text = re.sub(r"获取试卷更新[^\n]*", " ", text or "")
    text = re.sub(r"(?m)^-\s*\d+\s*-$", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def read_pdf(path):
    pages = []
    with pdfplumber.open(path) as pdf:
        for page in pdf.pages:
            pages.append({"text": page.extract_text() or "", "hasImage": bool(page.images)})
    return pages


def build_text_with_pages(pages):
    chunks = []
    ranges = []
    offset = 0
    for index, page in enumerate(pages):
        text = page["text"] + "\n"
        chunks.append(text)
        ranges.append((offset, offset + len(text), index, page["hasImage"]))
        offset += len(text)
    return "".join(chunks), ranges


def overlapping_image(ranges, start, end):
    return any(has_image and range_start < end and range_end > start
               for range_start, range_end, _, has_image in ranges)


def section_at(text, offset):
    selected = None
    selected_at = -1
    for section in SECTION_CONFIG:
        position = text.rfind(section, 0, offset)
        if position > selected_at:
            selected = section
            selected_at = position
    return selected


def parse_options(body):
    matches = list(OPTION_START.finditer(body))
    if not matches:
        return normalize(body), {}
    stem = normalize(body[:matches[0].start()])
    options = {}
    for index, match in enumerate(matches):
        key = match.group(1)
        end = matches[index + 1].start() if index + 1 < len(matches) else len(body)
        options[key] = normalize(body[match.end():end])
    return stem, options


def parse_answers(answer_pages):
    text = "\n".join(page["text"] for page in answer_pages)
    return parse_answer_text(text)


def parse_answer_text(text):
    matches = list(ANSWER_START.finditer(text))
    answers = {}
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        block = text[match.end():end]
        analysis_match = re.search(r"解析\s*(.*)", block, re.S)
        answers[int(match.group(1))] = {
            "answer": match.group(2),
            "analysis": normalize(analysis_match.group(1) if analysis_match else block),
        }
    return answers


def parse_answer_key(answer_key):
    letters = re.sub(r"[^A-D]", "", answer_key or "")
    return {index + 1: {"answer": letter, "analysis": "来源仅提供参考答案，详细解析待补充。"}
            for index, letter in enumerate(letters)}


def sequential_question_starts(text, question_count):
    starts = []
    expected = 1
    for match in QUESTION_START.finditer(text):
        if int(match.group(1)) == expected:
            starts.append(match)
            expected += 1
            if question_count and expected > question_count:
                break
    return starts


def parse_paper(item):
    pages = read_pdf(item["paperPdf"])
    text, ranges = build_text_with_pages(pages)
    if item.get("answerText"):
        answers = parse_answer_text(Path(item["answerText"]).read_text(encoding="utf-8-sig"))
    elif item.get("answerPdf"):
        answers = parse_answers(read_pdf(item["answerPdf"]))
    else:
        answers = parse_answer_key(item.get("answerKey"))
    starts = sequential_question_starts(text, item.get("questionCount") or len(answers))
    rows = []
    stats = {"paperQuestions": len(starts), "answers": len(answers), "active": 0, "draft": 0}
    for index, match in enumerate(starts):
        number = int(match.group(1))
        end = starts[index + 1].start() if index + 1 < len(starts) else len(text)
        section = section_at(text, match.start())
        if section not in SECTION_CONFIG or number not in answers:
            continue
        body = text[match.end():end]
        stem, options = parse_options(body)
        answer = answers[number]
        has_complete_options = all(options.get(key) for key in "ABCD")
        # Many source PDFs embed page logos/QR codes as images. Treating any page
        # image as question content incorrectly downgrades every textual question.
        # Keep only questions whose stem explicitly depends on a figure as DRAFT.
        image_dependent = bool(IMAGE_CLUES.search(stem))
        has_full_analysis = answer["analysis"] != "来源仅提供参考答案，详细解析待补充。"
        active = bool(stem) and has_complete_options and not image_dependent and has_full_analysis
        status = "ACTIVE" if active else "DRAFT"
        stats[status.lower()] += 1
        knowledge_code, seconds = SECTION_CONFIG[section]
        rows.append([
            stem or f"第{number}题（题干含图片，待媒体导入）",
            options.get("A") or "选项A待人工校对",
            options.get("B") or "选项B待人工校对",
            options.get("C") or "选项C待人工校对",
            options.get("D") or "选项D待人工校对",
            answer["answer"], answer["analysis"], knowledge_code, 50, seconds, "HISTORICAL", item["year"],
            item["exam"], "GitHub AdministrativeAptitudeTest（个人学习）", status, "TRAINING", "SINGLE",
        ])
    stats["paired"] = len(rows)
    stats["missingQuestionNumbers"] = sorted(set(answers) - {int(match.group(1)) for match in starts})
    stats["missingAnswerNumbers"] = sorted({int(match.group(1)) for match in starts} - set(answers))
    return rows, stats


def main():
    if len(sys.argv) != 3:
        raise SystemExit("usage: python prepare-jiangxi-papers.py <manifest.json> <output.csv>")
    manifest = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8-sig"))
    all_rows = []
    summary = {}
    for item in manifest:
        rows, stats = parse_paper(item)
        all_rows.extend(rows)
        summary[item["exam"]] = stats
    with Path(sys.argv[2]).open("w", encoding="utf-8-sig", newline="") as output:
        writer = csv.writer(output)
        writer.writerow(HEADERS)
        writer.writerows(all_rows)
    print(json.dumps({"total": len(all_rows), "papers": summary}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
