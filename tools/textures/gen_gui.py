"""GUI icons: element (16x16), weapon silhouettes (16x16), status (12x12), map markers."""
import math

import numpy as np
from PIL import Image

from pixlib import C, capsule, disk, outline_rgba, poly_mask, save

K = C('#120a10')         # outline used on every HUD icon
WHITE = C('#ffffff')


def blank(n):
    return np.zeros((n, n, 4), np.uint8)


def put(a, x, y, c):
    if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
        a[y, x] = c


def paint(a, rows, pal, ox=0, oy=0):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch not in '. ':
                put(a, ox + x, oy + y, pal[ch])
    return a


def layers(n, masks_cols):
    a = blank(n)
    for m, c in masks_cols:
        a[m] = c
    return a


def finish(a, col=K):
    return outline_rgba(a, col)


# ---------------------------------------------------------------------------
# element icons 16x16
def teardrop(cx, top, cy, r, n=16, bend=0.0):
    ys, xs = np.mgrid[0:n, 0:n]
    X, Y = xs + 0.5, ys + 0.5
    circ = (X - cx) ** 2 + (Y - cy) ** 2 <= r * r
    t = np.clip((Y - top) / max(cy - top, 1e-3), 0, 1)
    off = bend * (1 - t) ** 2
    cone = (Y >= top) & (Y <= cy) & (np.abs(X - cx - off) <= r * t ** 0.85 + 0.05)
    return circ | cone


def el_fire():
    outer = teardrop(7.6, 1.0, 10.0, 5.0, bend=2.2)
    tongue = teardrop(12.4, 4.0, 9.6, 1.6, bend=0.8)
    mid = teardrop(7.6, 4.2, 10.6, 3.6, bend=1.2)
    inner = teardrop(7.4, 7.0, 11.4, 2.2, bend=0.6)
    core = teardrop(7.3, 9.5, 12.0, 1.1)
    a = layers(16, [(outer | tongue, C('#e8361a')), (mid, C('#ff8a1e')), (inner, C('#ffd23c')),
                    (core, C('#fff8d0'))])
    put(a, 5, 9, C('#ffb040'))
    return finish(a)


def el_water():
    body = teardrop(7.5, 1.5, 9.8, 5.0)
    a = layers(16, [(body, C('#1e78e8'))])
    inner = teardrop(7.0, 4.5, 10.0, 3.6)
    a[inner & ~teardrop(8.6, 6.0, 11.0, 3.0)] = C('#4aa8ff')
    for (x, y) in ((5, 9), (5, 10), (6, 8), (5, 11)):
        put(a, x, y, C('#d8f2ff'))
    put(a, 7, 6, C('#8ccfff'))
    for (x, y) in ((9, 13), (10, 12), (11, 11)):
        put(a, x, y, C('#1458b0'))
    return finish(a)


def el_thunder():
    rows = [
        '................',
        '.......kkkkkk...',
        '......kWYYYYk...',
        '.....kWYYYYk....',
        '....kWYYYYk.....',
        '...kWYYYYkkkkk..',
        '..kWYYYYYYYYYk..',
        '..kkkkkWYYYYk...',
        '......kWYYyk....',
        '.....kWYYyk.....',
        '....kWYYyk......',
        '....kWYyk.......',
        '...kWYyk........',
        '...kWyk.........',
        '...kkk..........',
        '................',
    ]
    return paint(blank(16), rows, {'k': K, 'W': C('#fffbd8'), 'Y': C('#ffe03a'), 'y': C('#d8a810')})


def el_ice():
    rows = [
        '................',
        '.......kk.......',
        '...k..kWWk..k...',
        '..kWk.kWWk.kWk..',
        '...kWkkWWkkWk...',
        '....kWkWWkWk....',
        '.kkkkkWccWkkkkk.',
        '.kWWWWcCCcWWWWk.',
        '.kWWWWcCCcWWWWk.',
        '.kkkkkWccWkkkkk.',
        '....kWkWWkWk....',
        '...kWkkWWkkWk...',
        '..kWk.kWWk.kWk..',
        '...k..kWWk..k...',
        '.......kk.......',
        '................',
    ]
    return paint(blank(16), rows, {'k': K, 'W': C('#e6fcff'), 'C': C('#5ce0ff'), 'c': C('#2aa0d8')})


def el_dragon():
    a = blank(16)
    body, light, tip = C('#a8243a'), C('#e05468'), C('#f4e6d8')
    for k, bx in enumerate((3.5, 7.5, 11.5)):
        pts = []
        for i in range(12):
            t = i / 11
            x = bx + 1.2 - 3.4 * t ** 1.6
            y = 14.0 - 12.0 * t
            pts.append((x, y, 1.9 * (1 - t) + 0.45))
        for (x0, y0, r0), (x1, y1, r1) in zip(pts, pts[1:]):
            m = capsule(x0, y0, x1, y1, (r0 + r1) / 2, 16, 16)
            a[m] = body
        for (x, y, r) in pts[2:9]:
            put(a, int(x - r + 0.6), int(y), light)
        for (x, y, r) in pts[9:]:
            put(a, int(x), int(y), tip)
    palm = disk(7.0, 14.6, 5.2, 16, 16) & (np.mgrid[0:16, 0:16][0] >= 11)
    a[palm] = C('#7a1828')
    for x in range(3, 12):
        put(a, x, 11, C('#c03a50'))
    return finish(a)


def el_poison():
    rows = [
        '................',
        '....kkkkkk......',
        '...kPPPPPPk.....',
        '..kPpPPPPPPk....',
        '..kPPPPPPPPk....',
        '..kPkkPPkkPk....',
        '..kPkkPPkkPk.kk.',
        '..kPPPPPPPPkkbbk',
        '...kPPkkPPk.kbBk',
        '....kPPPPk...kk.',
        '....kPkPkPk.....',
        '.....kkkkk...kk.',
        '..........kkkbbk',
        '.........kbbkbBk',
        '.........kbBk.kk',
        '..........kk....',
    ]
    return paint(blank(16), rows, {'k': K, 'P': C('#b45ae8'), 'p': C('#e6b8ff'), 'b': C('#8a30c0'),
                                   'B': C('#e0a8ff')})


def el_paralysis():
    a = blank(16)
    ring = disk(7.5, 7.5, 7.0, 16, 16) & ~disk(7.5, 7.5, 5.0, 16, 16)
    a[ring] = C('#f0c818')
    a[disk(7.5, 7.5, 7.0, 16, 16) & ~disk(7.5, 7.5, 6.0, 16, 16) & (np.add.outer(np.arange(16), np.arange(16)) < 14)] = C('#fff080')
    bolt = ['....kkk.', '...kWYk.', '..kWYk..', '.kWYYYk.', '..kkWYk.', '..kWYk..', '.kWYk...', '.kkk....']
    paint(a, bolt, {'k': K, 'Y': C('#ffe03a'), 'W': C('#fffbd8')}, 4, 4)
    return finish(a)


def el_sleep():
    rows = [
        '................',
        '.kkkkkkkk.......',
        '.kZZZZZZk.......',
        '.kkkkkZZk.......',
        '....kZZk........',
        '...kZZk.........',
        '..kZZkkkk.......',
        '.kZZZZZZk.......',
        '.kkkkkkkk.kkkkk.',
        '..........kzzzk.',
        '..........kkkzk.',
        '...........kzk..',
        '..........kzkkk.',
        '..........kzzzk.',
        '..........kkkkk.',
        '................',
        '................',
    ]
    return paint(blank(16), rows, {'k': K, 'Z': C('#a8b8ff'), 'z': C('#d8e0ff')})


def el_blast():
    a = blank(16)
    pts = []
    for i in range(16):
        ang = math.pi * 2 * i / 16 - math.pi / 2
        r = 7.6 if i % 2 == 0 else 3.6
        pts.append((7.5 + r * math.cos(ang), 7.5 + r * math.sin(ang)))
    star = poly_mask(pts, 16, 16)
    a[star] = C('#e82a1a')
    a[star & disk(7.5, 7.5, 4.2, 16, 16)] = C('#ff8a20')
    a[disk(7.5, 7.5, 2.4, 16, 16)] = C('#ffe060')
    put(a, 7, 7, C('#fffbe0'))
    return finish(a)


def el_buskoba():
    rows = [
        '................',
        '................',
        '................',
        '......kkkk......',
        '.....kYYYyk.....',
        '....kYYYYYyk....',
        '....kYYYYYyk....',
        '....kPpPpPPk....',
        '..kkkYYYYYykkk..',
        '.kYYYYYYYYYYYyk.',
        'kYyyYYYYYYYYyyYk',
        '.kkPkkPkkPkkPkk.',
        '...k..k..k..k...',
        '................',
        '................',
        '................',
    ]
    return paint(blank(16), rows, {'k': K, 'Y': C('#f4cc48'), 'y': C('#c8961e'), 'P': C('#ff70c0'),
                                   'p': C('#ffc0e4')})


def el_none():
    a = blank(16)
    ring = disk(7.5, 7.5, 6.5, 16, 16) & ~disk(7.5, 7.5, 4.6, 16, 16)
    a[ring] = C('#a8a8b4')
    slash = capsule(4.0, 11.0, 11.0, 4.0, 1.0, 16, 16)
    a[slash] = C('#d8d8e2')
    return finish(a)


ELEMENTS = {'fire': el_fire, 'water': el_water, 'thunder': el_thunder, 'ice': el_ice, 'dragon': el_dragon,
            'poison': el_poison, 'paralysis': el_paralysis, 'sleep': el_sleep, 'blast': el_blast,
            'buskoba': el_buskoba, 'none': el_none}


# ---------------------------------------------------------------------------
# status icons 12x12
def st(rows, pal):
    pal = dict(pal)
    pal.setdefault('k', K)
    pal.setdefault('w', WHITE)
    return paint(blank(12), rows, pal)


UP = C('#7cff6a')


def status_icons():
    s = {}
    s['fireblight'] = st([
        '.....k......',
        '....kRk.....',
        '...kROk.k...',
        '..kROORkRk..',
        '..kROYORRk..',
        '.kROYYYOORk.',
        '.kROYWWYORk.',
        '.kROYWWYORk.',
        '..kROYYORk..',
        '...kRRRRk...',
        '....kkkk....',
        '............'], {'R': C('#e8361a'), 'O': C('#ff8a1e'), 'Y': C('#ffd23c'), 'W': C('#fff8d0')})
    s['waterblight'] = st([
        '.....kk.....',
        '....kBBk....',
        '...kBBBBk...',
        '..kBLBBBBk..',
        '..kLBBBBBk..',
        '.kBLBBBBBBk.',
        '.kBLBBBBBBk.',
        '.kBBBBBBBdk.',
        '..kBBBBBdk..',
        '...kddddk...',
        '....kkkk....',
        '............'], {'B': C('#2a8af0'), 'L': C('#cdeeff'), 'd': C('#1458b0')})
    s['thunderblight'] = st([
        '.....kkkkk..',
        '....kWYYYk..',
        '...kWYYYk...',
        '..kWYYYkkkk.',
        '.kWYYYYYYYk.',
        '.kkkkWYYYk..',
        '....kWYYk...',
        '...kWYYk....',
        '...kWYk.....',
        '..kWYk......',
        '..kkk.......',
        '............'], {'W': C('#fffbd8'), 'Y': C('#ffe03a')})
    s['iceblight'] = st([
        '.....kk.....',
        '..k.kWWk.k..',
        '.kWkkWWkkWk.',
        '..kWkWWkWk..',
        'kkkkWCCWkkkk',
        'kWWWCccCWWWk',
        'kWWWCccCWWWk',
        'kkkkWCCWkkkk',
        '..kWkWWkWk..',
        '.kWkkWWkkWk.',
        '..k.kWWk.k..',
        '.....kk.....'], {'W': C('#e6fcff'), 'C': C('#5ce0ff'), 'c': C('#2aa0d8')})
    s['dragonblight'] = st([
        '............',
        '.k...k...k..',
        'kRk.kRk.kRk.',
        'kRk.kRk.kRk.',
        'kRRkkRRkkRRk',
        '.kRRkkRRkkRk',
        '.kRRkkRRkkRk',
        '..kRRkkRRkk.',
        '..kRRkkRRk..',
        '...kRkkkRk..',
        '....k...k...',
        '............'], {'R': C('#c03048')})
    s['dragonblight'] = el_small_claws()
    s['poison'] = st([
        '...kkkkkk...',
        '..kPPPPPPk..',
        '.kPpPPPPPPk.',
        '.kPkkPPkkPk.',
        '.kPkkPPkkPk.',
        '.kPPPPPPPPk.',
        '..kPPkkPPk..',
        '...kPPPPk...',
        '...kPkPkk.kk',
        '....kkkk.kbk',
        '.........kbk',
        '..........k.'], {'P': C('#b45ae8'), 'p': C('#e6b8ff'), 'b': C('#e0a8ff')})
    s['sleep'] = st([
        'kkkkkkk.....',
        'kZZZZZk.....',
        'kkkkZZk.....',
        '...kZk......',
        '..kZk.......',
        '.kZkkkk.....',
        'kZZZZZkkkkk.',
        'kkkkkkkzzzk.',
        '......kkkzk.',
        '.......kzk..',
        '......kzkkk.',
        '......kzzzk.'], {'Z': C('#a8b8ff'), 'z': C('#d8e0ff')})
    s['paralysis'] = st([
        '...kkkkkk...',
        '..kYYYYYYk..',
        '.kYYkkkYYYk.',
        'kYYk..kYkYYk',
        'kYk..kYk.kYk',
        'kYk.kYYYk.Yk'.replace('.Yk', 'kYk'),
        'kYk..kYk.kYk',
        'kYk.kYk..kYk',
        'kYYkkk..kYYk',
        '.kYYYkkkYYk.',
        '..kYYYYYYk..',
        '...kkkkkk...'], {'Y': C('#f0d020')})
    s['stun'] = st([
        '............',
        '..k.....k...',
        '.kYk...kYk..',
        'kYWYk.kYWYk.',
        '.kYk...kYk..',
        '..k..k..k...',
        '....kYk.....',
        '...kYWYk....',
        '....kYk.....',
        '.kkkk.kkkkk.',
        'kSSSSkSSSSSk',
        '.kkkkkkkkkk.'], {'Y': C('#ffe03a'), 'W': C('#ffffff'), 'S': C('#c8c8d8')})
    s['drunk'] = st([
        '....kkk.....',
        '....kwk.....',
        '...kwwwk....',
        '..kwwwwwk.kk',
        '.kwBBBBBwkbk',
        '.kwBwwwBwkkk',
        '.kwBBBBBwk..',
        '.kwwwwwwwkk.',
        '.kwwwwwwwkbk',
        '..kwwwwwk.k.',
        '...kkkkk....',
        '............'], {'B': C('#e04a6a'), 'b': C('#ffb0c8')})
    s.update(buff_icons())
    s['heal'] = st([
        '............',
        '....kkkk....',
        '....kGGk....',
        '....kGGk....',
        '.kkkkGGkkkk.',
        '.kGGGWGGGGk.',
        '.kGGGGGGGGk.',
        '.kkkkGGkkkk.',
        '....kGGk....',
        '....kGGk....',
        '....kkkk....',
        '............'], {'G': C('#3ad84a'), 'W': C('#d8ffd0')})
    for k, a in s.items():
        assert a.shape == (12, 12, 4), k
    return s


ARROW = ['..G..', '.GLG.', 'GGGGG', '..G..', '..G..']


def _with_arrow(a, ox=6, oy=5):
    """Paste a green 'up' arrow badge (with its own outline) in the bottom-right corner."""
    m = np.zeros((12, 12), bool)
    for y, row in enumerate(ARROW):
        for x, ch in enumerate(row):
            if ch != '.':
                m[oy + y, ox + x] = True
    pad = np.pad(m, 1)
    ring = (pad[:-2, 1:-1] | pad[2:, 1:-1] | pad[1:-1, :-2] | pad[1:-1, 2:]) & ~m
    a[ring] = K
    a[m] = UP
    for y, row in enumerate(ARROW):
        for x, ch in enumerate(row):
            if ch == 'L':
                a[oy + y, ox + x] = C('#d8ffd0')
    return a


def buff_icons():
    from pixlib import Diag
    out = {}
    D = Diag(0, 12, 12, 12)
    blade = D.band(3.6, 10.6, -0.5, 1.5)
    guard = D.band(2.6, 3.6, -2.5, 3.5)
    grip = D.band(0.6, 2.6, -0.5, 1.5)
    a = blank(12)
    a[blade] = C('#ff3a2a')
    a[blade & (D.V > 0.5)] = C('#ffd0c8')
    a[guard] = C('#ffd040')
    a[grip] = C('#8a5a30')
    a = outline_rgba(a, K)
    out['attack_up'] = _with_arrow(a)
    a = paint(blank(12), [
        'kkkkkkkkk...',
        'kBBBBBBBk...',
        'kBWWBBBBk...',
        'kBWBBBBBk...',
        'kBBBBBBBk...',
        'kBBBBBBdk...',
        '.kBBBBdk....',
        '..kBBdk.....',
        '...kdk......',
        '....k.......'], {'k': K, 'B': C('#3a8aff'), 'W': C('#d8ecff'), 'd': C('#1e5ac0')})
    out['defense_up'] = _with_arrow(a)
    a = paint(blank(12), [
        '............',
        'kk..kk......',
        'kCk.kCk.....',
        '.kCk.kCk....',
        '..kCk.kCk...',
        '..kCk.kCk...',
        '.kCk.kCk....',
        'kCk.kCk.....',
        'kk..kk......',
        '............'], {'k': K, 'C': C('#4ae0ff')})
    out['speed_up'] = _with_arrow(a)
    return out


def el_small_claws():
    a = blank(12)
    body, light = C('#c03048'), C('#ff7088')
    for bx in (2.4, 5.8, 9.2):
        pts = []
        for i in range(9):
            t = i / 8
            pts.append((bx + 0.9 - 2.6 * t ** 1.6, 10.6 - 9.2 * t, 1.4 * (1 - t) + 0.4))
        for (x0, y0, r0), (x1, y1, r1) in zip(pts, pts[1:]):
            a[capsule(x0, y0, x1, y1, (r0 + r1) / 2, 12, 12)] = body
        for (x, y, r) in pts[2:6]:
            put(a, int(x - r + 0.6), int(y), light)
    return outline_rgba(a, K)


# fix up row widths in hand maps that were adjusted inline
def _norm(rows, n):
    return [(r + '.' * n)[:n] for r in rows]


# ---------------------------------------------------------------------------
def map_icons():
    camp = paint(blank(12), [
        '.....kk.....',
        '....kRRk....',
        '...kRRRRk...',
        '...kRRrRk...',
        '..kRRrrRRk..',
        '..kRrrrrRk..',
        '.kRRrkkrRRk.',
        '.kRrkddkrRk.',
        'kRRrkddkrRRk',
        'kkkkkkkkkkkk',
        '.kBBBBBBBBk.',
        '..kkkkkkkk..'], {'k': K, 'R': C('#f0d8a0'), 'r': C('#c8a868'), 'd': C('#4a2a14'), 'B': C('#8a5a30')})
    monster = paint(blank(12), [
        '..kkkkkkkk..',
        '.kRRRRRRRRk.',
        'kRRrRRRRRRRk',
        'kRkkRRRRkkRk',
        'kRkWkRRkWkRk',
        'kRRkRRRRkRRk',
        '.kRRRkkRRRk.',
        '..kRRRRRRk..',
        '..kRkRkRRk..',
        '...kRkRkk...',
        '....kRRk....',
        '.....kk.....'], {'k': K, 'R': C('#e02828'), 'r': C('#ff8a8a'), 'W': C('#ffe040')})
    player = paint(blank(8), [
        '...kk...',
        '..kWWk..',
        '.kWWWWk.',
        'kWWWWWWk',
        'kkkWWkkk',
        '..kWWk..',
        '..kWWk..',
        '..kkkk..'], {'k': K, 'W': WHITE})
    return {'map_camp': camp, 'map_monster': monster, 'map_player': player}


# ---------------------------------------------------------------------------
# weapon silhouettes 16x16 derived from the 32x32 weapon shapes
GREY = C('#aeb4c4')


def _compose(parts):
    """parts: [(mask, colour, sep)] drawn in order; sep draws a dark contour on what is under it."""
    a = blank(16)
    lab = np.full((16, 16), -1)
    for i, (m, c, sep) in enumerate(parts):
        a[m] = c
        lab[m] = i
    for i, (m, c, sep) in enumerate(parts):
        if not sep:
            continue
        pad = np.pad(m, 1)
        near = pad[:-2, 1:-1] | pad[2:, 1:-1] | pad[1:-1, :-2] | pad[1:-1, 2:]
        under = near & ~m & (lab >= 0) & (lab < i)
        a[under] = K
    return outline_rgba(a, K)


def _ascii(rows):
    a = blank(16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == 'W':
                a[y, x] = WHITE
            elif ch == 'g':
                a[y, x] = GREY
    return outline_rgba(a, K)


def weapon_silhouettes(weapons=None):
    from pixlib import Diag
    D = Diag(1, 15, 16, 16)
    U, V = D.U, D.V
    band = D.band
    sil = {}
    sil['great_sword'] = _compose([
        (band(0.5, 3.6, -0.5, 1.5), GREY, False),
        (band(3.6, 4.6, -4.5, 4.5), GREY, False),
        (D.poly([(4.6, 3.5), (11.6, 3.5), (13.6, 0.5), (13.6, -0.5), (10.6, -3.5), (4.6, -3.5)]), WHITE, False)])
    sil['long_sword'] = _compose([
        (band(0.5, 4.1, -0.5, 1.5), GREY, False),
        (band(4.1, 5.1, -2.5, 3.5), GREY, False),
        (band(5.1, 9.6, -0.5, 1.5) | band(9.6, 13.0, 0.5, 2.5) | band(13.0, 13.8, 1.5, 2.5), WHITE, False)])
    sil['hammer'] = _compose([
        (band(0.5, 9.6, -0.5, 1.5), GREY, False),
        (band(9.6, 13.1, -5.5, 5.5) & ~(band(9.6, 10.1, 4.5, 5.5) | band(12.6, 13.1, 4.5, 5.5)
                                         | band(9.6, 10.1, -5.5, -4.5) | band(12.6, 13.1, -5.5, -4.5)), WHITE, False)])
    sh = disk(*D.P(4.6, -5.0), 2.6, 16, 16)
    sil['gunlance'] = _compose([
        (sh, GREY, False),
        (band(0.5, 3.1, -0.5, 1.5), GREY, True),
        (band(3.1, 5.1, -2.5, 2.5), GREY, True),
        (D.where(lambda U, V: (U >= 5.1) & (U <= 14.0) & (V <= np.where(U < 10, 3.5, 3.5 - (U - 10) / 4 * 3.4) + .01)
                 & (V >= np.where(U < 11, -1.5, -1.5 + (U - 11) / 3 * 1.5) - .01)), WHITE, False)])
    sil['lance'] = _compose([
        (band(0.5, 3.1, -0.5, 1.5), GREY, False),
        (D.where(lambda U, V: (U >= 3.1) & (U <= 5.6) & (np.abs(V - 0.5) <= 5.0 - (U - 3.1) * 1.0 + .01)), GREY, False),
        (D.where(lambda U, V: (U >= 5.6) & (U <= 14.0) & (np.abs(V - 0.5) <= 2.5 - (U - 5.6) / 8.4 * 2.5 + .01)), WHITE, False)])
    sil['insect_glaive'] = _compose([
        (band(2.6, 11.1, -0.5, 1.5), GREY, False),
        (D.where(lambda U, V: (U >= 10.6) & (U <= 14.0) & (np.abs(V - 0.5) <= 2.0 * np.sin(np.clip((U - 10.6) / 3.4, 0, 1) * math.pi) ** 0.7 + 0.6)), WHITE, False),
        (D.where(lambda U, V: (U >= 0.4) & (U <= 3.1) & (np.abs(V - 0.5) <= 1.6 * np.sin(np.clip((3.1 - U) / 2.7, 0, 1) * math.pi) ** 0.7 + 0.6)), WHITE, False)])
    sil['switch_axe'] = _compose([
        (band(0.5, 12.1, -0.5, 1.5), GREY, False),
        (D.poly([(8.1, 1.5), (8.6, 4.5), (9.6, 6.8), (11.1, 5.4), (12.1, 3.5), (12.6, 1.5)]), WHITE, False),
        (band(6.6, 12.6, -2.5, -0.5) & ~band(6.6, 7.6, -2.5, -1.5), WHITE, False),
        (band(12.1, 13.8, -0.5, 1.5), WHITE, False)])
    shield_cb = poly_mask([(1, 6), (8, 6), (8, 11), (4.5, 15), (1, 11)], 16, 16)
    sil['charge_blade'] = _compose([
        (shield_cb, GREY, False),
        (band(0.5, 3.1, -0.5, 1.5), GREY, True),
        (band(3.1, 4.6, -2.5, 3.5), GREY, True),
        (D.poly([(4.6, 2.5), (12.1, 2.5), (14.0, 0.5), (14.0, 0.0), (12.6, -1.5), (4.6, -1.5)]), WHITE, True)])

    def blade(D):
        return [(D.band(0.5, 3.1, -0.5, 1.5), GREY), (D.band(3.1, 4.1, -1.5, 2.5), GREY),
                (D.poly([(4.1, 1.5), (10.6, 1.5), (12.0, 0.5), (11.0, -0.5), (4.1, -0.5)]), WHITE)]
    A = blade(D)
    B = [(np.fliplr(m), c) for m, c in A]
    sil['dual_blades'] = _compose([(m, c, False) for m, c in B] + [(m, c, True) for m, c in A])
    sil['sword_and_shield'] = _compose([
        (disk(10.5, 10.5, 4.6, 16, 16), GREY, False),
        (band(0.5, 2.6, -0.5, 1.5), GREY, True),
        (band(2.6, 3.6, -2.5, 3.5), GREY, True),
        (D.poly([(3.6, 1.5), (10.6, 1.5), (12.1, 0.5), (12.1, 0.0), (10.6, -0.5), (3.6, -0.5)]), WHITE, True)])
    sil['hunting_horn'] = _compose([
        (band(0.5, 6.6, -0.5, 1.5), GREY, False),
        (band(7.1, 8.1, 2.5, 5.5) | band(8.6, 9.6, 2.5, 4.5), GREY, False),
        (D.where(lambda U, V: (U >= 6.1) & (U <= 12.6) & (np.abs(V - 0.5) <= np.where(U < 9.6, 2.0, 2.0 + (U - 9.6) / 3 * 3.0) + .01)), WHITE, False),
        (D.where(lambda U, V: (U >= 12.1) & (U <= 12.6) & (np.abs(V - 0.5) <= 3.5)), K, False)])
    sil['light_bowgun'] = _ascii([
        '................',
        '................',
        '................',
        '..........WW....',
        '.....ggg...W....',
        '..ggggggggWWWWW.',
        '.gggggggggWWWWW.',
        '.gggg.gg..WW....',
        '.gg...gg.WW.....',
        '.g....gg........',
        '................',
    ])
    sil['heavy_bowgun'] = _ascii([
        '................',
        '................',
        '................',
        '.....gggg.......',
        '.ggggggggWWWWgg.',
        '.ggggggggWWWWgg.',
        '.gggggggWWWWWgg.',
        '.ggggggggWWWWgg.',
        '..ggggggWWWW....',
        '..g..gg.........',
        '.....gg.........',
        '................',
    ])
    sil['bow'] = _ascii([
        '................',
        '....gWW.........',
        '....g.WWW.......',
        '....g...WW......',
        '....g....WW.....',
        '....g.....WW....',
        '....g.....WW....',
        '....g.....ggg...',
        '....g.....ggg...',
        '....g.....WW....',
        '....g.....WW....',
        '....g....WW.....',
        '....g...WW......',
        '....g.WWW.......',
        '....gWW.........',
        '................',
    ])
    return sil


def build(weapons):
    out = {}
    for k, f in ELEMENTS.items():
        out[f'element_{k}'] = f()
    for k, a in weapon_silhouettes().items():
        out[f'weapon_{k}'] = a
    for k, a in status_icons().items():
        out[f'status_{k}'] = a
    out.update(map_icons())
    return out


def write(out):
    for k, a in out.items():
        save(a, f'gui/{k}.png')
