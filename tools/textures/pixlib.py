"""Shared pixel-art helpers for the ブスコハンター texture generators.

Everything is drawn procedurally with numpy: shapes are rasterised by testing
pixel centres (no anti-aliasing), then shaded with explicit colour ramps,
outlined with a 1px darker contour and finished with hand placed pixels.

Ramps are always 5 entries: 0 outline, 1 dark, 2 mid, 3 light, 4 highlight.
"""
import math
import os

import numpy as np
from PIL import Image

ROOT = '/home/claude/buskohunter/src/main/resources/assets/buskohunter'
TEX = os.path.join(ROOT, 'textures')
TOOLS = os.path.dirname(os.path.abspath(__file__))

CREATED = []  # (path, w, h)


def C(h, a=255):
    """'#rrggbb' -> (r, g, b, a)."""
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def ramp(*hexes):
    return [C(h) for h in hexes]


def grey(v, a=255):
    return (v, v, v, a)


# Overlay (tintable) ramp: grayscale so a multiplicative tint colours it.
OVER = [grey(52), grey(98), grey(152), grey(208), grey(250)]

# Material ramps (outline, dark, mid, light, highlight) - hue shifted:
# shadows lean cool/purple, highlights lean warm.
MAT = {
    'steel':   ramp('#22232c', '#5c5f6e', '#8f94a0', '#c4c8ce', '#eef0f2'),
    'iron':    ramp('#26242a', '#4e4d58', '#73737f', '#a2a3ac', '#d6d8dc'),
    'dark':    ramp('#141218', '#2a2832', '#403e4a', '#5c5b68', '#8a8a98'),
    'gold':    ramp('#3e2408', '#8e5a12', '#cf9a2c', '#f2cf5a', '#fff3b8'),
    'brass':   ramp('#3a2610', '#7a5520', '#b0812e', '#d8b04e', '#f6e09a'),
    'wood':    ramp('#2c1a0e', '#5e3a1e', '#8a5a30', '#b07c46', '#d2a66c'),
    'darkwood': ramp('#1c100a', '#3c2414', '#5a361e', '#7a4c2a', '#9c6a3e'),
    'leather': ramp('#1e120c', '#4a2a1a', '#6e4026', '#925a36', '#b47a4e'),
    'wrap_red': ramp('#2a0c0e', '#64161c', '#982430', '#c43c44', '#e86a6a'),
    'wrap_blue': ramp('#0e1424', '#1e2c4c', '#30467a', '#4a66a4', '#7a96cc'),
    'cloth':   ramp('#2a2420', '#8a8070', '#bdb29c', '#ddd4c0', '#f6f0e2'),
    'bone':    ramp('#3c3224', '#a29274', '#cdbf9e', '#e8dfc6', '#fbf7ea'),
    'string':  ramp('#4a4a50', '#bcbcc4', '#e0e0e6', '#f4f4f8', '#ffffff'),
    'glass':   ramp('#1c3440', '#4c8aa0', '#86c4d4', '#bfe8ee', '#f4ffff'),
    'amber':   ramp('#3a1c06', '#a0500e', '#e08a1c', '#ffbe48', '#fff0b0'),
    'red':     ramp('#2c080a', '#7a1418', '#b8242a', '#e04a42', '#ff8a78'),
    'green':   ramp('#0c2410', '#1e6a2a', '#38a040', '#6ad25a', '#c0f4a0'),
    'kinsect': ramp('#2e1404', '#8a3c0a', '#d0701a', '#f4a63a', '#ffe08a'),
}


class Sprite:
    """Layered part sprite: later parts are drawn on top of earlier ones."""

    def __init__(self, w=32, h=32):
        self.w, self.h = w, h
        self.label = np.full((h, w), -1, int)
        self.parts = []
        self.tone_over = np.full((h, w), -1, int)
        self.base_px = {}   # (x, y) -> rgba override on base layer
        self.over_px = {}   # (x, y) -> rgba override on overlay layer
        self.no_outline = np.zeros((h, w), bool)
        self.extra_outline = []  # (mask, mat, tinted)

    # -- building -------------------------------------------------------
    def add(self, mask, mat, tinted=False, shade='edge', outline=True,
            sep=False):
        """sep=True: draw this part's outline colour onto lower parts it
        touches (so a sword in front of a shield keeps a contour)."""
        idx = len(self.parts)
        self.parts.append(dict(mat=mat, tinted=tinted, shade=shade,
                               outline=outline, sep=sep))
        self.label[mask] = idx
        return idx

    def erase(self, mask):
        self.label[mask] = -1

    def tone(self, mask, t):
        """Force a tone index (0..4) for every drawn pixel in mask."""
        self.tone_over[mask] = t

    def tone_px(self, pts, t):
        for x, y in pts:
            if 0 <= x < self.w and 0 <= y < self.h:
                self.tone_over[y, x] = t

    def put(self, x, y, rgba, layer='base'):
        (self.base_px if layer == 'base' else self.over_px)[(x, y)] = rgba

    def part_mask(self, idx):
        return self.label == idx

    # -- rendering ------------------------------------------------------
    def _tones(self):
        L = self.label
        h, w = L.shape
        pad = np.pad(L, 1, constant_values=-2)
        same_l = pad[1:-1, 0:-2] == L
        same_u = pad[0:-2, 1:-1] == L
        same_r = pad[1:-1, 2:] == L
        same_d = pad[2:, 1:-1] == L
        lit = (~same_l).astype(int) + (~same_u).astype(int)
        drk = (~same_r).astype(int) + (~same_d).astype(int)
        tones = np.full((h, w), 2, int)
        tones[(lit > 0) & (drk == 0)] = 3
        tones[(drk > 0) & (lit == 0)] = 1
        both = (lit > 0) & (drk > 0)
        tones[both & (lit > drk)] = 3
        tones[both & (drk > lit)] = 1
        for i, p in enumerate(self.parts):
            m = L == i
            if p['shade'] == 'flat':
                tones[m] = 2
            elif p['shade'] == 'flatlight':
                tones[m] = 3
            elif p['shade'] == 'flatdark':
                tones[m] = 1
        ov = self.tone_over >= 0
        tones[ov & (L >= 0)] = self.tone_over[ov & (L >= 0)]
        return tones

    def cleanup(self):
        """Remove stray single pixels with no 4-connected neighbour."""
        L = self.label
        pad = np.pad(L, 1, constant_values=-1)
        n = ((pad[1:-1, :-2] >= 0).astype(int) + (pad[1:-1, 2:] >= 0) +
             (pad[:-2, 1:-1] >= 0) + (pad[2:, 1:-1] >= 0))
        iso = (L >= 0) & (n == 0)
        keep = np.zeros_like(iso)
        for (x, y) in list(self.base_px) + list(self.over_px):
            keep[y, x] = True
        L[iso & ~keep] = -1

    def render(self):
        self.cleanup()
        h, w = self.h, self.w
        base = np.zeros((h, w, 4), np.uint8)
        over = np.zeros((h, w, 4), np.uint8)
        tones = self._tones()
        L = self.label
        for y in range(h):
            for x in range(w):
                i = L[y, x]
                if i < 0:
                    continue
                p = self.parts[i]
                t = tones[y, x]
                if p['tinted']:
                    over[y, x] = OVER[t]
                else:
                    base[y, x] = MAT[p['mat']][t] if isinstance(p['mat'], str) else p['mat'][t]
        # outline (4-neighbourhood)
        for y in range(h):
            for x in range(w):
                if L[y, x] >= 0 or self.no_outline[y, x]:
                    continue
                best = -1
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h:
                        j = L[ny, nx]
                        if j >= 0 and self.parts[j]['outline'] and j > best:
                            best = j
                if best >= 0:
                    p = self.parts[best]
                    if p['tinted']:
                        over[y, x] = OVER[0]
                    else:
                        m = MAT[p['mat']] if isinstance(p['mat'], str) else p['mat']
                        base[y, x] = m[0]
        # separation contours drawn onto lower parts
        for y in range(h):
            for x in range(w):
                j = L[y, x]
                if j < 0:
                    continue
                best = -1
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h:
                        k = L[ny, nx]
                        if k > j and self.parts[k]['sep'] and k > best:
                            best = k
                if best >= 0:
                    p = self.parts[best]
                    if p['tinted']:
                        over[y, x] = OVER[0]
                        base[y, x] = (0, 0, 0, 0)
                    else:
                        m = MAT[p['mat']] if isinstance(p['mat'], str) else p['mat']
                        base[y, x] = m[0]
                        over[y, x] = (0, 0, 0, 0)
        for (x, y), c in self.base_px.items():
            base[y, x] = c
            over[y, x] = (0, 0, 0, 0)
        for (x, y), c in self.over_px.items():
            over[y, x] = c
            base[y, x] = (0, 0, 0, 0)
        return base, over


# ---------------------------------------------------------------------------
# geometry
def centres(w, h):
    ys, xs = np.mgrid[0:h, 0:w]
    return xs + 0.5, ys + 0.5


def poly_mask(pts, w=32, h=32):
    X, Y = centres(w, h)
    inside = np.zeros((h, w), bool)
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        if y1 == y2:
            continue
        cond = (y1 > Y) != (y2 > Y)
        xint = (x2 - x1) * (Y - y1) / (y2 - y1) + x1
        inside ^= cond & (X < xint)
    return inside


def disk(cx, cy, r, w=32, h=32):
    X, Y = centres(w, h)
    return (X - cx) ** 2 + (Y - cy) ** 2 <= r * r


def ellipse(cx, cy, rx, ry, w=32, h=32):
    X, Y = centres(w, h)
    return ((X - cx) / rx) ** 2 + ((Y - cy) / ry) ** 2 <= 1.0


def rect(x0, y0, x1, y1, w=32, h=32):
    """Inclusive pixel rectangle."""
    m = np.zeros((h, w), bool)
    m[max(0, y0):y1 + 1, max(0, x0):x1 + 1] = True
    return m


def capsule(x0, y0, x1, y1, r, w=32, h=32):
    X, Y = centres(w, h)
    dx, dy = x1 - x0, y1 - y0
    L2 = dx * dx + dy * dy
    t = np.clip(((X - x0) * dx + (Y - y0) * dy) / (L2 if L2 else 1), 0, 1)
    px, py = x0 + t * dx, y0 + t * dy
    return (X - px) ** 2 + (Y - py) ** 2 <= r * r


def pts_mask(pts, w=32, h=32):
    m = np.zeros((h, w), bool)
    for x, y in pts:
        if 0 <= x < w and 0 <= y < h:
            m[y, x] = True
    return m


def line_pts(x0, y0, x1, y1):
    """Bresenham line pixel list."""
    pts = []
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        pts.append((x0, y0))
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy
    return pts


class Diag:
    """45-degree weapon frame.

    u: distance along the diagonal (bottom-left -> top-right) in pixel steps.
    v: perpendicular offset in 'diagonal lines' (+ = upper-left side).
    Every integer v is exactly one 1:1 staircase line of pixels, so constant-v
    edges give perfectly clean pixel-art diagonals.
    """

    def __init__(self, ox, oy, w=32, h=32):
        self.ox, self.oy, self.w, self.h = ox + 1e-4, oy + 2e-4, w, h
        X, Y = centres(w, h)
        self.U = ((X - self.ox) - (Y - self.oy)) / 2.0
        self.V = -((X - self.ox) + (Y - self.oy))

    def P(self, u, v):
        return (self.ox + u - v / 2.0, self.oy - u - v / 2.0)

    def poly(self, uv):
        return poly_mask([self.P(u, v) for u, v in uv], self.w, self.h)

    def band(self, u0, u1, v0, v1):
        U, V = self.U, self.V
        return (U >= u0) & (U <= u1) & (V >= v0) & (V <= v1)

    def where(self, fn):
        return fn(self.U, self.V)

    def px(self, u, v):
        x, y = self.P(u, v)
        return int(math.floor(x)), int(math.floor(y))


# ---------------------------------------------------------------------------
# ascii pixel maps
def from_map(rows, pal, w=None, h=None):
    h = h or len(rows)
    w = w or max(len(r) for r in rows)
    a = np.zeros((h, w, 4), np.uint8)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ('.', ' '):
                continue
            c = pal[ch]
            if len(c) == 3:
                c = tuple(c) + (255,)
            a[y, x] = c
    return a


def outline_rgba(a, col, diag=False, only_empty=True):
    """Add a 1px outline of colour col around opaque pixels."""
    h, w = a.shape[:2]
    alpha = a[:, :, 3] > 0
    out = a.copy()
    nbrs = [(1, 0), (-1, 0), (0, 1), (0, -1)]
    if diag:
        nbrs += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
    for y in range(h):
        for x in range(w):
            if alpha[y, x]:
                continue
            for dx, dy in nbrs:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and alpha[ny, nx]:
                    out[y, x] = col
                    break
    return out


def save(arr, rel, root=TEX):
    path = os.path.join(root, rel) if not os.path.isabs(rel) else rel
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if isinstance(arr, Image.Image):
        img = arr
    else:
        img = Image.fromarray(np.asarray(arr, np.uint8), 'RGBA')
    img.save(path)
    CREATED.append((path, img.size[0], img.size[1]))
    return path


def tint(over, col):
    o = over.astype(float)
    t = np.array(col[:3], float) / 255.0
    o[:, :, :3] = o[:, :, :3] * t
    return o.astype(np.uint8)


def alpha_over(dst, src):
    """src over dst (both uint8 RGBA arrays, same size)."""
    d = dst.astype(float) / 255.0
    s = src.astype(float) / 255.0
    sa = s[:, :, 3:4]
    da = d[:, :, 3:4]
    oa = sa + da * (1 - sa)
    rgb = np.where(oa > 0, (s[:, :, :3] * sa + d[:, :, :3] * da * (1 - sa)) / np.maximum(oa, 1e-6), 0)
    out = np.concatenate([rgb, oa], axis=2)
    return (out * 255 + 0.5).astype(np.uint8)


def composite(base, over, col=None):
    o = tint(over, col) if col is not None else over
    return alpha_over(base, o)


def upscale(arr, s):
    return np.repeat(np.repeat(arr, s, axis=0), s, axis=1)


def checker(w, h, s=8, c1=(54, 54, 62), c2=(66, 66, 76)):
    ys, xs = np.mgrid[0:h, 0:w]
    m = ((xs // s) + (ys // s)) % 2 == 0
    a = np.zeros((h, w, 4), np.uint8)
    a[m] = c1 + (255,)
    a[~m] = c2 + (255,)
    return a


def shade_hsv(rgb, dl=0.0, ds=0.0, dh=0.0):
    import colorsys
    r, g, b = [c / 255.0 for c in rgb[:3]]
    hh, ll, ss = colorsys.rgb_to_hls(r, g, b)
    hh = (hh + dh) % 1.0
    ll = min(1, max(0, ll + dl))
    ss = min(1, max(0, ss + ds))
    r, g, b = colorsys.hls_to_rgb(hh, ll, ss)
    return (int(r * 255 + .5), int(g * 255 + .5), int(b * 255 + .5), 255)


def make_ramp(hexmid):
    """Auto 5-tone ramp from a mid colour with hue shifting."""
    m = C(hexmid)
    return [shade_hsv(m, -0.36, -0.1, 0.03), shade_hsv(m, -0.17, 0.0, 0.02), m,
            shade_hsv(m, 0.13, 0.0, -0.02), shade_hsv(m, 0.27, -0.05, -0.04)]
