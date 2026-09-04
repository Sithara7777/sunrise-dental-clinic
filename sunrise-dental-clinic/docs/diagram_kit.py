"""
Hand-drawn UML diagram toolkit.

Every diagram in the report is rendered through this module so they all share
one visual language: wobbly pen strokes, a handwriting face and a restrained
palette. matplotlib's xkcd() path filter is what does the actual "drawn by
hand" distortion - it perturbs every line segment, so no two edges are ever
identical, which is exactly what separates a sketch from a computer plot.
"""
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Ellipse, Rectangle, Polygon
from matplotlib.lines import Line2D
import matplotlib.patheffects as pe
import numpy as np

# A4 landscape at a generous DPI - each diagram gets a whole page.
A4_LANDSCAPE = (11.69, 8.27)
A4_PORTRAIT = (8.27, 11.69)
DPI = 220

# Handwriting faces actually installed on this machine, best first.
HAND_FONTS = ["Segoe Print", "Comic Sans MS", "Ink Free", "Bradley Hand ITC",
              "Segoe Script", "DejaVu Sans"]

INK = "#1a2733"          # pen
ACCENT = "#12626c"       # teal, matches the application
ACCENT2 = "#b3261e"      # red, for emphasis / constraints
FILL_A = "#ffffff"
FILL_B = "#e8f4f5"
FILL_C = "#fdf3d8"
FILL_D = "#eef2f6"
NOTE = "#fff9e0"


def new_canvas(size=A4_LANDSCAPE, xlim=(0, 100), ylim=(0, 70)):
    """Start a hand-drawn canvas. Returns (fig, ax)."""
    fig = plt.figure(figsize=size, dpi=DPI)
    ax = fig.add_axes([0, 0, 1, 1])
    ax.set_xlim(*xlim)
    ax.set_ylim(*ylim)
    ax.axis("off")
    ax.set_facecolor("white")
    fig.patch.set_facecolor("white")
    return fig, ax


def title(ax, text, sub=None, x=None, y=None, fs=21, sub_fs=12.5, sub_dy=3.4):
    xl = ax.get_xlim(); yl = ax.get_ylim()
    if x is None:
        x = (xl[0] + xl[1]) / 2
    if y is None:
        y = yl[1] - 3.2
    ax.text(x, y, text, ha="center", va="top", fontsize=fs, color=INK,
            weight="bold")
    if sub:
        ax.text(x, y - sub_dy, sub, ha="center", va="top", fontsize=sub_fs,
                color="#5a6b7a")


def box(ax, x, y, w, h, label="", fc=FILL_A, ec=INK, lw=2.2, fs=12,
        bold=False, align="center", radius=0.6, zorder=2, text_color=INK):
    """A hand-drawn rounded rectangle with a centred label."""
    p = FancyBboxPatch((x, y), w, h,
                       boxstyle=f"round,pad=0,rounding_size={radius}",
                       linewidth=lw, edgecolor=ec, facecolor=fc, zorder=zorder)
    ax.add_patch(p)
    if label:
        if align == "center":
            ax.text(x + w / 2, y + h / 2, label, ha="center", va="center",
                    fontsize=fs, color=text_color, zorder=zorder + 1,
                    weight="bold" if bold else "normal", linespacing=1.5)
        else:
            ax.text(x + 0.7, y + h - 0.7, label, ha="left", va="top",
                    fontsize=fs, color=text_color, zorder=zorder + 1,
                    weight="bold" if bold else "normal", linespacing=1.5)
    return p


def sharp_box(ax, x, y, w, h, fc=FILL_A, ec=INK, lw=2.2, zorder=2):
    """A square-cornered rectangle (UML classes are not rounded)."""
    p = Rectangle((x, y), w, h, linewidth=lw, edgecolor=ec,
                  facecolor=fc, zorder=zorder)
    ax.add_patch(p)
    return p


def oval(ax, cx, cy, w, h, label="", fc=FILL_B, ec=INK, lw=2.2, fs=11.5,
         zorder=2):
    """A use-case bubble."""
    e = Ellipse((cx, cy), w, h, linewidth=lw, edgecolor=ec, facecolor=fc,
                zorder=zorder)
    ax.add_patch(e)
    ax.text(cx, cy, label, ha="center", va="center", fontsize=fs, color=INK,
            zorder=zorder + 1, linespacing=1.45)
    return e


def actor(ax, x, y, label, scale=1.0, color=INK, fs=12):
    """A UML stick-figure actor, drawn wobbly."""
    s = scale
    head_r = 0.95 * s
    ax.add_patch(Ellipse((x, y), head_r * 2, head_r * 2.1, linewidth=2.2,
                         edgecolor=color, facecolor="white", zorder=3))
    ax.add_line(Line2D([x, x], [y - head_r, y - head_r - 2.6 * s],
                       lw=2.2, color=color, zorder=3))
    ax.add_line(Line2D([x - 1.5 * s, x + 1.5 * s],
                       [y - head_r - 0.9 * s, y - head_r - 0.9 * s],
                       lw=2.2, color=color, zorder=3))
    ax.add_line(Line2D([x, x - 1.3 * s], [y - head_r - 2.6 * s,
                                          y - head_r - 4.6 * s],
                       lw=2.2, color=color, zorder=3))
    ax.add_line(Line2D([x, x + 1.3 * s], [y - head_r - 2.6 * s,
                                          y - head_r - 4.6 * s],
                       lw=2.2, color=color, zorder=3))
    ax.text(x, y - head_r - 5.6 * s, label, ha="center", va="top",
            fontsize=fs, color=color, weight="bold", linespacing=1.4)


def line(ax, x1, y1, x2, y2, lw=2.0, color=INK, ls="-", zorder=1, alpha=1.0):
    ax.add_line(Line2D([x1, x2], [y1, y2], lw=lw, color=color, linestyle=ls,
                       zorder=zorder, alpha=alpha))


def arrow(ax, x1, y1, x2, y2, color=INK, lw=2.0, ls="-", head="->",
          label=None, fs=10.5, label_off=(0, 0.9), zorder=3,
          label_color=None, shrink=0.0, label_ha="center"):
    """A hand-drawn connector. head: '->', '-|>', '..>' etc via ls/style."""
    dx, dy = x2 - x1, y2 - y1
    d = np.hypot(dx, dy) or 1
    if shrink:
        x1 += dx / d * shrink; y1 += dy / d * shrink
        x2 -= dx / d * shrink; y2 -= dy / d * shrink
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=head, color=color, lw=lw,
                                linestyle=ls, shrinkA=0, shrinkB=0),
                zorder=zorder)
    if label:
        mx, my = (x1 + x2) / 2 + label_off[0], (y1 + y2) / 2 + label_off[1]
        ax.text(mx, my, label, ha=label_ha, va="center", fontsize=fs,
                color=label_color or color, zorder=zorder + 1,
                linespacing=1.4,
                bbox=dict(boxstyle="round,pad=0.22", fc="white", ec="none",
                          alpha=0.92))


def note(ax, x, y, w, h, text, fc=NOTE, fs=10.5, ec="#c8b26b"):
    """A sticky-note annotation with the classic folded corner."""
    fold = 1.5
    pts = [(x, y), (x + w, y), (x + w, y + h - fold),
           (x + w - fold, y + h), (x, y + h)]
    ax.add_patch(Polygon(pts, closed=True, facecolor=fc, edgecolor=ec,
                         linewidth=1.8, zorder=4))
    ax.add_line(Line2D([x + w - fold, x + w - fold, x + w],
                       [y + h, y + h - fold, y + h - fold],
                       lw=1.6, color=ec, zorder=5))
    ax.text(x + 0.6, y + h - 0.9, text, ha="left", va="top", fontsize=fs,
            color="#5c4b16", zorder=5, linespacing=1.55)


def diamond(ax, cx, cy, w=1.5, h=1.1, filled=True, color=INK, zorder=4):
    """Aggregation (hollow) / composition (filled) diamond."""
    pts = [(cx, cy + h), (cx + w, cy), (cx, cy - h), (cx - w, cy)]
    ax.add_patch(Polygon(pts, closed=True,
                         facecolor=color if filled else "white",
                         edgecolor=color, linewidth=2.0, zorder=zorder))


def class_box(ax, x, y, w, name, attrs, methods, fs=9.6, title_fs=11.4,
              fc=FILL_A, header=FILL_B, ec=INK, stereotype=None):
    """
    A proper UML class box: name / attributes / operations compartments.
    Height is derived from the content so boxes never overflow.
    Returns (x, y, w, h).
    """
    line_h = 1.28
    head_h = 2.5 + (1.15 if stereotype else 0)
    a_h = max(len(attrs), 1) * line_h + 0.7
    m_h = max(len(methods), 1) * line_h + 0.7
    h = head_h + a_h + m_h

    y0 = y - h
    sharp_box(ax, x, y0, w, h, fc=fc, ec=ec)
    # header compartment
    sharp_box(ax, x, y - head_h, w, head_h, fc=header, ec=ec)

    ty = y - 1.0
    if stereotype:
        ax.text(x + w / 2, ty, stereotype, ha="center", va="center",
                fontsize=fs, color="#5a6b7a", style="italic", zorder=4)
        ty -= 1.15
    ax.text(x + w / 2, ty - 0.35, name, ha="center", va="center",
            fontsize=title_fs, color=INK, weight="bold", zorder=4)

    cy = y - head_h - 0.85
    for a in attrs:
        ax.text(x + 0.55, cy, a, ha="left", va="center", fontsize=fs,
                color=INK, zorder=4)
        cy -= line_h
    sep = y - head_h - a_h
    line(ax, x, sep, x + w, sep, lw=2.0, zorder=4)

    cy = sep - 0.85
    for m in methods:
        ax.text(x + 0.55, cy, m, ha="left", va="center", fontsize=fs,
                color=INK, zorder=4)
        cy -= line_h
    return (x, y0, w, h)


def lifeline(ax, cx, top, bottom, label, w=13.5, h=3.0, fc=FILL_B, fs=10.5,
             sub=None):
    """A sequence-diagram participant with its dashed lifeline.

    `sub` (the ":ClassName" line) may span several lines; the header is laid
    out from the top down so a two-line class name never sits on the title.
    """
    box(ax, cx - w / 2, top - h, w, h, "", fc=fc, lw=2.2, radius=0.4)
    if sub:
        ax.text(cx, top - 1.5, label, ha="center", va="top", fontsize=fs,
                color=INK, weight="bold", zorder=4, linespacing=1.3)
        ax.text(cx, top - 3.9, sub, ha="center", va="top", fontsize=fs - 1.9,
                color="#5a6b7a", style="italic", zorder=4, linespacing=1.3)
    else:
        ax.text(cx, top - h / 2, label, ha="center", va="center", fontsize=fs,
                color=INK, weight="bold", zorder=4, linespacing=1.3)
    line(ax, cx, top - h, cx, bottom, lw=1.7, ls=(0, (5, 5)),
         color="#7b8b99", zorder=1)


def activation(ax, cx, y_top, y_bottom, w=1.15, fc="#cfe6e9"):
    """The tall thin bar showing a participant is active."""
    ax.add_patch(Rectangle((cx - w / 2, y_bottom), w, y_top - y_bottom,
                           facecolor=fc, edgecolor=INK, linewidth=1.6,
                           zorder=3))


def msg(ax, x1, x2, y, text, ret=False, fs=10.2, color=INK, self_call=False,
        label_dy=0.95, self_side="right"):
    """A synchronous call (solid, filled head) or a return (dashed, open).

    `self_side` puts a self-call's label on the other side, which is what the
    right-most participant needs so its text does not run off the page.
    """
    if self_call:
        s = 1 if self_side == "right" else -1
        r = 3.4 * s
        ax.annotate("", xy=(x1 + 0.6 * s, y - 2.4), xytext=(x1 + 0.6 * s, y),
                    arrowprops=dict(arrowstyle="-", color=color, lw=1.9),
                    zorder=4)
        line(ax, x1 + 0.6 * s, y, x1 + r, y, lw=1.9, color=color, zorder=4)
        line(ax, x1 + r, y, x1 + r, y - 2.4, lw=1.9, color=color, zorder=4)
        ax.annotate("", xy=(x1 + 0.6 * s, y - 2.4), xytext=(x1 + r, y - 2.4),
                    arrowprops=dict(arrowstyle="-|>", color=color, lw=1.9),
                    zorder=4)
        ax.text(x1 + r + 0.8 * s, y - 1.2, text,
                ha="left" if s > 0 else "right", va="center",
                fontsize=fs, color=color, zorder=5, linespacing=1.4,
                bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="none",
                          alpha=0.93))
        return
    style = "-|>" if not ret else "->"
    ls = "-" if not ret else (0, (4, 3))
    ax.annotate("", xy=(x2, y), xytext=(x1, y),
                arrowprops=dict(arrowstyle=style, color=color, lw=1.9,
                                linestyle=ls, shrinkA=0, shrinkB=0), zorder=4)
    ax.text((x1 + x2) / 2, y + label_dy, text, ha="center", va="bottom",
            fontsize=fs, color=color, zorder=5, linespacing=1.4,
            bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="none",
                      alpha=0.93))


def frame(ax, x, y, w, h, label, sub=None, ec=ACCENT, fs=10.5):
    """An interaction/grouping frame with the folded tab (alt / loop / opt)."""
    ax.add_patch(Rectangle((x, y), w, h, facecolor="none", edgecolor=ec,
                           linewidth=1.9, linestyle=(0, (6, 4)), zorder=2))
    tw, th = 7.0, 2.1
    pts = [(x, y + h), (x + tw, y + h), (x + tw, y + h - th + 0.6),
           (x + tw - 0.9, y + h - th), (x, y + h - th)]
    ax.add_patch(Polygon(pts, closed=True, facecolor="white", edgecolor=ec,
                         linewidth=1.9, zorder=3))
    ax.text(x + 0.7, y + h - th / 2, label, ha="left", va="center",
            fontsize=fs, color=ec, weight="bold", zorder=4)
    if sub:
        ax.text(x + tw + 1.0, y + h - th / 2, sub, ha="left", va="center",
                fontsize=fs - 0.5, color="#5a6b7a", zorder=4)


def save(fig, path):
    fig.savefig(path, dpi=DPI, facecolor="white", bbox_inches=None)
    plt.close(fig)
    print("  wrote", path)


def hand(scale=1.4, length=110, randomness=3.2):
    """Context manager giving every stroke the hand-drawn wobble."""
    ctx = plt.xkcd(scale=scale, length=length, randomness=randomness)
    return ctx


def use_hand_font():
    """Apply a handwriting face on top of xkcd()'s own rcParams."""
    plt.rcParams["font.family"] = HAND_FONTS
    plt.rcParams["font.size"] = 12
    plt.rcParams["path.effects"] = [pe.withStroke(linewidth=0, foreground="w")]
