"""OCR all rendered PNG pages in a directory into one UTF-8 text file."""

import sys
from pathlib import Path

from rapidocr_onnxruntime import RapidOCR


if len(sys.argv) != 3:
    raise SystemExit("usage: python extract-pdf-ocr.py <page-directory> <output.txt>")

root = Path(sys.argv[1])
files = sorted(root.glob("page-*.png"))
engine = RapidOCR()
pages = []
for index, path in enumerate(files, 1):
    result, _ = engine(str(path))
    pages.append("\n".join(item[1] for item in (result or [])))
    print(f"{index}/{len(files)}", flush=True)

Path(sys.argv[2]).write_text("\n".join(pages), encoding="utf-8")
