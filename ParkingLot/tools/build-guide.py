"""Expand runnable sources into the study guide; no third-party Python packages needed."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
template = (root / "STUDY_GUIDE.template.md").read_text(encoding="utf-8")

def embed(match):
    relative = match.group(1)
    source = (root / relative).read_text(encoding="utf-8").rstrip()
    return (f'<details>\n<summary>Full runnable Java: {relative}</summary>\n\n'
            f'```java\n{source}\n```\n\n</details>\n')

guide = re.sub(r"<!-- code: ([^ ]+) -->", embed, template)
(root / "STUDY_GUIDE.md").write_text(guide, encoding="utf-8")
print(f"Built STUDY_GUIDE.md with {len(guide.split())} words including code.")
