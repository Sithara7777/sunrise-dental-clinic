"""
Open the report in Microsoft Word, refresh the Table of Contents so it carries
real page numbers, then export to PDF.

Word is used rather than a converter because the document contains a live TOC
field; nothing else populates it correctly.
"""
import os, sys, time

HERE = os.path.dirname(os.path.abspath(__file__))
DOCX = os.path.join(HERE, "CIS6003_WRIT1_Report.docx")
PDF = os.path.join(HERE, "CIS6003_WRIT1_Report.pdf")

WD_FORMAT_PDF = 17
WD_DO_NOT_SAVE_CHANGES = 0


def main():
    import win32com.client as win32

    if not os.path.exists(DOCX):
        sys.exit(f"missing {DOCX} - run build_report.py first")

    word = win32.DispatchEx("Word.Application")
    word.Visible = False
    word.DisplayAlerts = 0
    doc = None
    try:
        doc = word.Documents.Open(DOCX, ReadOnly=False)

        # Fields first, then the TOC, then fields again: the TOC needs the
        # page numbers to be settled before it can report them.
        doc.Fields.Update()
        for i in range(1, doc.TablesOfContents.Count + 1):
            doc.TablesOfContents(i).Update()
        doc.Repaginate()
        doc.Fields.Update()

        pages = doc.ComputeStatistics(2)     # wdStatisticPages
        words = doc.ComputeStatistics(0)     # wdStatisticWords
        print(f"Word reports: {pages} pages, {words} words")

        doc.Save()
        if os.path.exists(PDF):
            os.remove(PDF)
        doc.SaveAs(PDF, FileFormat=WD_FORMAT_PDF)
        print("wrote", PDF)
    finally:
        if doc is not None:
            doc.Close(WD_DO_NOT_SAVE_CHANGES)
        word.Quit()
        time.sleep(0.5)


if __name__ == "__main__":
    main()
