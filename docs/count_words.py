"""Word-count the built report the way the brief counts it.

The brief includes text, tables, figures, subtitles and citations, and
excludes the reference list and appendices.  This prints both the raw total
and the figure that follows those rules, split by task so it is obvious where
any excess lives.
"""
import sys, os
from docx import Document
from docx.table import Table
from docx.text.paragraph import Paragraph

DOC = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "CIS6003_WRIT1_Report.docx")
EXCLUDED = {"FRONT", "Table of Contents", "Table of Figures",
            "Table of Tables", "References"}


def main(path=DOC):
    d = Document(path)
    cur = "FRONT"
    prose, tabs = {}, {}
    for ch in d.element.body.iterchildren():
        if ch.tag.endswith("}p"):
            p = Paragraph(ch, d)
            if p.style.name == "Heading 1":
                cur = p.text.strip()
            prose[cur] = prose.get(cur, 0) + len(p.text.split())
        elif ch.tag.endswith("}tbl"):
            t = Table(ch, d)
            tabs[cur] = tabs.get(cur, 0) + sum(
                len(c.text.split()) for r in t.rows for c in r.cells)

    keys = list(dict.fromkeys(list(prose) + list(tabs)))
    print(f"{'section':52s} {'prose':>7s} {'tables':>7s} {'total':>7s}")
    print("-" * 78)
    counted = total = 0
    for k in keys:
        pr, tb = prose.get(k, 0), tabs.get(k, 0)
        total += pr + tb
        if k not in EXCLUDED:
            counted += pr + tb
        print(f"{k[:52]:52s} {pr:7d} {tb:7d} {pr + tb:7d}")
    print("-" * 78)
    print(f"{'RAW TOTAL':52s} {'':7s} {'':7s} {total:7d}")
    print(f"{'COUNTED (excl. front matter + reference list)':52s} "
          f"{'':7s} {'':7s} {counted:7d}")
    print(f"\ntarget 4000  ->  {counted / 4000 * 100:.0f}% of target")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else DOC)
