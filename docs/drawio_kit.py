"""
Minimal draw.io (.drawio / mxGraphModel) writer.

Everything it emits carries draw.io's own sketch styling -
`sketch=1;curveFitting=1;jiggle=2` plus a handwriting font - so opening any of
these files in draw.io gives the same hand-drawn look as the rendered PNGs,
and every shape stays editable.

Coordinates are given in the same y-up units the matplotlib diagrams use; this
module flips them, because draw.io measures y downwards from the top.
"""
from xml.sax.saxutils import escape as _escape


def escape(s):
    """XML-escape for use inside a double-quoted attribute.

    saxutils.escape leaves quotes alone, which breaks any label containing
    one - and several of these labels do (slot_lock holds "date|time").
    """
    return _escape(str(s), {'"': "&quot;", "'": "&apos;", "\n": "&#10;"})

SKETCH = "sketch=1;curveFitting=1;jiggle=2;"
FONT = "fontFamily=Comic Sans MS;"
INK = "#1A2733"
ACCENT = "#12626C"
ACCENT2 = "#B3261E"
GREY = "#5A6B7A"


class Drawio:
    def __init__(self, name, width_u, height_u, scale=10.0, landscape=True):
        self.name = name
        self.scale = scale
        self.h_u = height_u
        self.w_u = width_u
        self.landscape = landscape
        self.cells = []
        self._n = 1

    # ------------------------------------------------------------------ #
    def _id(self, prefix="n"):
        self._n += 1
        return f"{prefix}{self._n}"

    def _x(self, x):
        return round(x * self.scale, 2)

    def _y(self, y):
        """Flip: our y grows up, draw.io's grows down."""
        return round((self.h_u - y) * self.scale, 2)

    # ------------------------------------------------------------------ #
    def node(self, x, y, w, h, label="", style="", fs=14, ident=None,
             valign="middle", align="center"):
        """x, y = TOP-LEFT corner in y-up units."""
        cid = ident or self._id()
        st = (style + SKETCH + FONT + f"fontSize={fs};"
              f"verticalAlign={valign};align={align};html=1;whiteSpace=wrap;")
        self.cells.append(
            f'<mxCell id="{cid}" value="{escape(label)}" style="{escape(st)}" '
            f'vertex="1" parent="1">'
            f'<mxGeometry x="{self._x(x)}" y="{self._y(y)}" '
            f'width="{self._x(w)}" height="{round(h * self.scale, 2)}" '
            f'as="geometry"/></mxCell>')
        return cid

    def box(self, x, y, w, h, label="", fill="#FFFFFF", stroke=INK, lw=2,
            rounded=1, fs=14, **kw):
        return self.node(x, y, w, h, label,
                         f"rounded={rounded};fillColor={fill};"
                         f"strokeColor={stroke};strokeWidth={lw};arcSize=8;",
                         fs=fs, **kw)

    def ellipse(self, x, y, w, h, label="", fill="#E8F4F5", stroke=INK, fs=13):
        return self.node(x, y, w, h, label,
                         f"ellipse;fillColor={fill};strokeColor={stroke};"
                         f"strokeWidth=2;", fs=fs)

    def actor(self, cx, cy, w, h, label, stroke=INK, fs=13):
        return self.node(cx - w / 2, cy, w, h, label,
                         f"shape=umlActor;verticalLabelPosition=bottom;"
                         f"verticalAlign=top;outlineConnect=0;"
                         f"strokeColor={stroke};strokeWidth=2;fillColor=none;",
                         fs=fs)

    def note(self, x, y, w, h, label, fs=12):
        return self.node(x, y, w, h, label,
                         "shape=note;size=18;fillColor=#FFF9E0;"
                         "strokeColor=#C8B26B;strokeWidth=2;",
                         fs=fs, valign="top", align="left")

    def text(self, x, y, w, h, label, fs=13, color=INK, align="center",
             bold=False, italic=False):
        st = (f"text;fillColor=none;strokeColor=none;fontColor={color};"
              f"fontStyle={(1 if bold else 0) + (2 if italic else 0)};")
        return self.node(x, y, w, h, label, st, fs=fs, align=align)

    # ------------------------------------------------------------------ #
    def uml_class(self, x, y, w, name, attrs, methods, fs=11,
                  header="#E8F4F5", stereotype=None, line_h=1.5):
        """A stacked UML class box; returns (id, height_in_units)."""
        head_h = 3.4 + (1.4 if stereotype else 0)
        h = head_h + (len(attrs) + len(methods)) * line_h + 1.0
        title = (f"&lt;&lt;{stereotype}&gt;&gt;\n{name}" if stereotype else name)
        cid = self.node(
            x, y, w, h, title,
            f"swimlane;childLayout=stackLayout;horizontal=1;startSize={head_h * self.scale};"
            f"horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;"
            f"collapsible=0;marginBottom=0;fillColor={header};strokeColor={INK};"
            f"strokeWidth=2;fontStyle=1;", fs=fs + 2)
        cy = y - head_h
        for i, txt in enumerate(attrs + methods):
            sep = ("line;strokeWidth=2;" if i == len(attrs) and attrs and methods
                   else "")
            self.child(cid, 0, (cy - y) * -1, w, line_h, txt,
                       f"text;strokeColor=none;fillColor=#FFFFFF;{sep}"
                       f"align=left;spacingLeft=6;", fs=fs)
            cy -= line_h
        return cid, h

    def child(self, parent, dx, dy, w, h, label, style, fs=11):
        cid = self._id("c")
        st = style + SKETCH + FONT + f"fontSize={fs};html=1;verticalAlign=middle;"
        self.cells.append(
            f'<mxCell id="{cid}" value="{escape(label)}" style="{escape(st)}" '
            f'vertex="1" parent="{parent}">'
            f'<mxGeometry x="{self._x(dx)}" y="{round(dy * self.scale, 2)}" '
            f'width="{self._x(w)}" height="{round(h * self.scale, 2)}" '
            f'as="geometry"/></mxCell>')
        return cid

    # ------------------------------------------------------------------ #
    def edge(self, src, dst, label="", style="", fs=11, points=None,
             exit_xy=None, entry_xy=None):
        cid = self._id("e")
        st = ("edgeStyle=orthogonalEdgeStyle;rounded=0;" + SKETCH + FONT +
              f"fontSize={fs};strokeWidth=2;strokeColor={INK};html=1;" + style)
        if exit_xy:
            st += f"exitX={exit_xy[0]};exitY={exit_xy[1]};exitDx=0;exitDy=0;"
        if entry_xy:
            st += f"entryX={entry_xy[0]};entryY={entry_xy[1]};entryDx=0;entryDy=0;"
        geo = '<mxGeometry relative="1" as="geometry">'
        if points:
            geo += "<Array as='points'>"
            for px, py in points:
                geo += f'<mxPoint x="{self._x(px)}" y="{self._y(py)}"/>'
            geo += "</Array>"
        geo += "</mxGeometry>"
        self.cells.append(
            f'<mxCell id="{cid}" value="{escape(label)}" style="{escape(st)}" '
            f'edge="1" parent="1" source="{src}" target="{dst}">{geo}</mxCell>')
        return cid

    def line_edge(self, x1, y1, x2, y2, label="", style="", fs=11):
        """A free-floating connector between two points."""
        cid = self._id("e")
        st = ("edgeStyle=none;rounded=0;" + SKETCH + FONT +
              f"fontSize={fs};strokeWidth=2;strokeColor={INK};html=1;" + style)
        self.cells.append(
            f'<mxCell id="{cid}" value="{escape(label)}" style="{escape(st)}" '
            f'edge="1" parent="1">'
            f'<mxGeometry relative="1" as="geometry">'
            f'<mxPoint x="{self._x(x1)}" y="{self._y(y1)}" as="sourcePoint"/>'
            f'<mxPoint x="{self._x(x2)}" y="{self._y(y2)}" as="targetPoint"/>'
            f'</mxGeometry></mxCell>')
        return cid

    # ------------------------------------------------------------------ #
    def xml(self):
        body = "\n        ".join(self.cells)
        page = "1" if self.landscape else "0"
        return (
            f'<mxfile host="app.diagrams.net" type="device">\n'
            f'  <diagram name="{escape(self.name)}">\n'
            f'    <mxGraphModel dx="1422" dy="800" grid="0" gridSize="10" '
            f'guides="1" tooltips="1" connect="1" arrows="1" fold="1" '
            f'page="1" pageScale="1" pageWidth="{1169 if self.landscape else 827}" '
            f'pageHeight="{827 if self.landscape else 1169}" math="0" shadow="0">\n'
            f'      <root>\n'
            f'        <mxCell id="0"/>\n'
            f'        <mxCell id="1" parent="0"/>\n'
            f'        {body}\n'
            f'      </root>\n'
            f'    </mxGraphModel>\n'
            f'  </diagram>\n'
            f'</mxfile>\n')

    def save(self, path):
        with open(path, "w", encoding="utf-8") as f:
            f.write(self.xml())
        print("  wrote", path)


# ---------------------------------------------------------------------- #
# common edge styles
# ---------------------------------------------------------------------- #
E_PLAIN = "endArrow=none;"
E_OPEN = "endArrow=open;endFill=0;endSize=8;"
E_ARROW = "endArrow=block;endFill=1;endSize=8;"
E_INCLUDE = f"dashed=1;endArrow=open;endFill=0;strokeColor={ACCENT};fontColor={ACCENT};"
E_EXTEND = f"dashed=1;endArrow=open;endFill=0;strokeColor={ACCENT2};fontColor={ACCENT2};"
E_DEP = "dashed=1;endArrow=open;endFill=0;strokeColor=#7A5A90;fontColor=#7A5A90;"
E_GENERAL = "endArrow=block;endFill=0;endSize=14;"
E_AGGREG = "startArrow=diamondThin;startFill=0;startSize=14;endArrow=none;"
E_COMPOS = "startArrow=diamondThin;startFill=1;startSize=14;endArrow=none;"
E_ONE_MANY = "startArrow=ERone;startFill=0;endArrow=ERmany;endFill=0;"
E_ONE_ONE = "startArrow=ERone;startFill=0;endArrow=ERzeroToOne;endFill=0;"
