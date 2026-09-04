"""
Prepare the screenshots for the report.

Several captures are full-page and very tall - the audit trail is 4,664px and
the outstanding-invoices report 9,900px. Scaled down to fit a page those are
unreadable, which defeats the point of including them. Each is therefore
cropped to the top of the page, where the heading, the controls and the first
rows of data are, and given a thin border so it reads as a screenshot rather
than as part of the page.
"""
import os
from PIL import Image, ImageOps

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "screenshots")
OUT = os.path.join(SRC, "report")
os.makedirs(OUT, exist_ok=True)

MAX_ASPECT_H = 2000        # tallest we allow, in source pixels
BORDER = 6
BORDER_COLOUR = (176, 190, 200)


def prep(name, max_h=MAX_ASPECT_H):
    src = os.path.join(SRC, name)
    with Image.open(src) as im:
        im = im.convert("RGB")
        if im.height > max_h:
            im = im.crop((0, 0, im.width, max_h))
        im = ImageOps.expand(im, border=BORDER, fill=BORDER_COLOUR)
        dst = os.path.join(OUT, name)
        im.save(dst, optimize=True)
        print(f"  {name:28s} -> {im.width} x {im.height}")
    return dst


WANTED = [
    ("01_login.png", 2000),
    ("02_dashboard.png", 1900),
    ("04_appointment_new.png", 2000),
    ("05_appointment_view.png", 1900),
    ("09_invoice_view.png", 2000),
    ("10_receipt.png", 2000),
    ("12_report_revenue.png", 1900),
    ("23_swagger.png", 2000),
    ("19_admin_audit.png", 1900),
    ("20_admin_notifications.png", 2000),
]


def main():
    print("preparing screenshots for the report")
    for name, h in WANTED:
        prep(name, h)
    print("done ->", OUT)


if __name__ == "__main__":
    main()
