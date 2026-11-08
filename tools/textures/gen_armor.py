"""Armor: 16x16 inventory icons + 64x32 worn textures (vanilla humanoid UV layout).

Worn layout (same as vanilla iron layer_1 / layer_2, box UV unwrap):
  head  (0,0)  8x8x8   hat overlay (32,0) 8x8x8
  body  (16,16) 8x12x4
  arms  (40,16) 4x12x4
  legs  (0,16)  4x12x4
For a box at (u,v) size (w,h,d):
  top (u+d, v, w, d)   bottom (u+d+w, v, w, d)
  right (u, v+d, d, h) front (u+d, v+d, w, h) left (u+d+w, v+d, d, h) back (u+d+w+d, v+d, w, h)
"""
import numpy as np

from pixlib import MAT, C, Sprite, ramp, save

SERIES_ORDER = ['hunter', 'sakeran', 'barino', 'jangria', 'busaiber', 'buskoba']

R = {
    'leather': ramp('#24140c', '#5a3420', '#8a5632', '#b07a4a', '#d2a070'),
    'iron':    ramp('#26242a', '#5a5a66', '#8a8c96', '#b8bac2', '#e6e8ec'),
    'gold':    MAT['gold'],
    'fur':     ramp('#3c3c46', '#aeaeb8', '#d8d8de', '#f0f0f4', '#ffffff'),
    'claw':    ramp('#060608', '#16161c', '#26262e', '#3e3e48', '#62626e'),
    'pink':    ramp('#4a1a24', '#c06a7a', '#e898a4', '#ffc4cc', '#fff0f2'),
    'pearl':   ramp('#4a4458', '#c0b8d0', '#e4deee', '#f6f2ff', '#ffffff'),
    'halo':    ramp('#7a5a10', '#f0c840', '#ffe680', '#fff6c0', '#ffffff'),
    'crimson': ramp('#1e0408', '#4c0c16', '#781622', '#a02a32', '#c84a48'),
    'horn':    ramp('#060406', '#141016', '#242026', '#3a343e', '#5c5462'),
    'lava':    ramp('#3a0c02', '#c03a08', '#ff7a1a', '#ffb84a', '#fff0a0'),
    'carbon':  ramp('#060608', '#15171c', '#23262d', '#353944', '#4e5462'),
    'cyan':    ramp('#003a48', '#0096b4', '#30f0ff', '#a0fcff', '#ffffff'),
    'magenta': ramp('#3a002a', '#a8108a', '#ff40d0', '#ff9ae8', '#ffffff'),
    'skin':    ramp('#4a3424', '#b08a6a', '#dcb894', '#f0d4b4', '#fff0e0'),
    'straw':   ramp('#4a3404', '#a8801c', '#e0b840', '#f4d470', '#fff2b0'),
    'red':     ramp('#3a0808', '#901818', '#d03030', '#f05a50', '#ffa090'),
    'green':   ramp('#082a10', '#1a7028', '#30a040', '#60d060', '#b0f0a0'),
    'sole':    ramp('#0c0a0a', '#24201e', '#3a3430', '#524a44', '#6a625a'),
}

# letter -> ramp name for every series
PAL = {
    'hunter':   dict(M='leather', T='iron', A='gold', B='iron', D='sole', E='leather'),
    'sakeran':  dict(M='fur', T='claw', A='pink', B='claw', D='claw', E='fur'),
    'barino':   dict(M='pearl', T='gold', A='halo', B='gold', D='sole', E='pearl'),
    'jangria':  dict(M='crimson', T='horn', A='lava', B='horn', D='horn', E='crimson'),
    'busaiber': dict(M='carbon', T='carbon', A='cyan', B='magenta', D='sole', E='carbon'),
    'buskoba':  dict(M='skin', T='straw', A='red', B='green', D='sole', E='straw'),
}

# ---------------------------------------------------------------------------
# inventory icons (16x16)
HELMET = {
    'hunter': [
        '................',
        '................',
        '.......TT.......',
        '.....MMTTMM.....',
        '...MMMMTTMMMM...',
        '..MMMMMTTMMMMM..',
        '..MMMMMMMMMMMM..',
        '..TTTTTTTTTTTT..',
        '..MMA......AMM..',
        '..MMM......MMM..',
        '..TTT......TTT..',
        '................',
    ],
    'sakeran': [
        '................',
        '..M..........M..',
        '..MM..MMMM..MM..',
        '..MAMMMMMMMMAM..',
        '..MMMMMMMMMMMM..',
        '..MMMMMMMMMMMM..',
        '..MMMMMMMMMMMM..',
        '..TMTMTMMTMTMT..',
        '..MMM......MMM..',
        '..MMT......TMM..',
        '..MT........TM..',
        '................',
    ],
    'barino': [
        '.....HHHHHH.....',
        '....H......H....',
        '.....HHHHHH.....',
        '.......TT.......',
        '...MMMMTTMMMM...',
        '..MMMMMMMMMMMM..',
        '..MMMMMMMMMMMM..',
        '..TTTTTTTTTTTT..',
        '..MMM......MMM..',
        '..MMT......TMM..',
        '..TT........TT..',
        '................',
    ],
    'jangria': [
        '.T............T.',
        '.TT..........TT.',
        '..TT........TT..',
        '..TTMMMMMMMMTT..',
        '...MMMMMMMMMM...',
        '..MMMMMAMMMMMM..',
        '..MMMAMMMMAMMM..',
        '..TTTTTTTTTTTT..',
        '..MMM......MMM..',
        '..MMT......TMM..',
        '..TT........TT..',
        '................',
    ],
    'busaiber': [
        '................',
        '............B...',
        '............B...',
        '.....MMMMMMMT...',
        '...MMMMMMMMMM...',
        '..MMMMBMMBMMMM..',
        '..MMMMMMMMMMMM..',
        '..MAAAAAAAAAAM..',
        '..MAAAAAAAAAAM..',
        '..MM........MM..',
        '..TT........TT..',
        '................',
    ],
    'buskoba': [
        '................',
        '......TTTT......',
        '.....TTTTTT.....',
        '.....TTTTTT.....',
        '.....ABABAB.....',
        '...TTTTTTTTTT...',
        '.TTTTTTTTTTTTTT.',
        '.TABABABABABABT.',
        '...TTTTTTTTTT...',
        '................',
        '................',
        '................',
    ],
}

CHEST = [
    '................',
    '................',
    '..MMMM....MMMM..',
    '.MMMMMTTTTMMMMM.',
    '.MMMMMMMMMMMMMM.',
    '.MMMMMMMMMMMMMM.',
    '.TTTMMMMMMMMTTT.',
    '...MMMMMMMMMM...',
    '...MMMMMMMMMM...',
    '...MMMMMMMMMM...',
    '...MMMMMMMMMM...',
    '...MMMMMMMMMM...',
    '...TTTTTTTTTT...',
    '................',
]
CHEST_X = {
    'hunter': [
        '................',
        '................',
        '..TTTT....TTTT..',
        '.TTTTT....TTTTT.',
        '.TTT..........T.',
        '................',
        '................',
        '.....T....T.....',
        '.....T....T.....',
        '.....T....T.....',
        '...TTTTTTTTTT...',
        '.......AA.......',
        '................',
    ],
    'sakeran': [
        '................',
        '................',
        '................',
        '.....MMMMMM.....',
        '...........T....',
        '..........T..T..',
        '.........T..T...',
        '.......E..T.....',
        '...........E....',
        '.......E........',
        '...........E....',
        '................',
        '...TMTMTMTMTMT..',
    ],
    'barino': [
        '................',
        '................',
        '..TTTT....TTTT..',
        '................',
        '................',
        '................',
        '.......AA.......',
        '......A..A......',
        '.......AA.......',
        '.......TT.......',
        '.......TT.......',
        '.......TT.......',
        '................',
    ],
    'jangria': [
        '................',
        '.T............T.',
        '.TT..........TT.',
        '................',
        '.....A..........',
        '......A....A....',
        '......A...A.....',
        '.....A....A.....',
        '.........A......',
        '....A.....A.....',
        '.....A..........',
        '.....A..........',
        '................',
    ],
    'busaiber': [
        '................',
        '................',
        '................',
        '................',
        '.A..A......A..A.',
        '.A..A......A..A.',
        '....A..BB..A....',
        '....A.BBBB.A....',
        '....A..BB..A....',
        '....A......A....',
        '....AAAAAAAA....',
        '................',
        '...AAAAAAAAAA...',
    ],
    'buskoba': [
        '................',
        '................',
        '................',
        '................',
        '................',
        '.AAAAAAAAAAAAAA.',
        '.BBB........BBB.',
        '...BBBBBBBBBB...',
        '................',
        '...AAAAAAAAAA...',
        '...BBBBBBBBBB...',
        '................',
        '...TBTBTBTBTBT..',
    ],
}

LEGS = [
    '................',
    '................',
    '...TTTTTTTTTT...',
    '...MMMMMMMMMM...',
    '...MMMMMMMMMM...',
    '...MMMM..MMMM...',
    '...MMMM..MMMM...',
    '...MMMM..MMMM...',
    '...TTTT..TTTT...',
    '...MMMM..MMMM...',
    '...MMMM..MMMM...',
    '...MMMM..MMMM...',
    '...MMMM..MMMM...',
    '...TTTT..TTTT...',
    '................',
]
LEGS_X = {
    'hunter': ['', '', '.......AA.......', '', '....T......T....', '....T......T....', '',
               '', '...AT.....TA....'.replace('...AT.....TA....', '....A....A......'), '', '', '', ''],
    'sakeran': ['', '', '...TMTMTMTMTM...', '', '', '...E........E...', '', '.....E....E.....',
                '...TMTM..MTMT...', '', '....E......E....', '', '...TMTM..MTMT...'],
    'barino': ['', '', '', '...A........A...', '...A........A...', '...A........A...', '...A........A...',
               '', '....AA....AA....', '', '', '', ''],
    'jangria': ['', '', '', '......A..A......', '.....A....A.....', '....A......A....', '.....A.....A....',
                '', '...BTTB..BTTB...', '..B..........B..', '', '....A......A....', ''],
    'busaiber': ['', '', '...AAAAAAAAAA...', '', '...B........B...', '...A........A...', '...A........A...',
                 '...A........A...', '...AAAA..AAAA...', '...A........A...', '...A........A...',
                 '...A........A...', ''],
    'buskoba': ['', '', '...ABABABABAB...', '', '', '...A........A...', '...B........B...',
                '...A........A...', '...TTTT..TTTT...', '...B........B...', '...A........A...',
                '...B........B...', ''],
}

BOOTS = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '...TTTT..TTTT...',
    '...MMMM..MMMM...',
    '...MMMM..MMMM...',
    '...MMMM..MMMM...',
    '..MMMMM..MMMMM..',
    '.MMMMMM..MMMMMM.',
    '.DDDDDD..DDDDDD.',
    '................',
]
BOOTS_X = {
    'hunter': ['', '', '', '', '', '', '', '...A........A...', '', '...TTTT..TTTT...', '', '', ''],
    'sakeran': ['', '', '', '', '', '', '...MTMT..TMTM...', '', '', '', '', '.T.T........T.T.', ''],
    'barino': ['', '', '', '', '', '', '', '...A........A...', '...A........A...', '', '.TTTT....TTTT...'.replace('.TTTT....TTTT...', '..TTTTT..TTTTT..'), '', ''],
    'jangria': ['', '', '', '', '', '..B..........B..', '.BB..........BB.', '', '....A......A....', '...A........A...', '', '', ''],
    'busaiber': ['', '', '', '', '', '', '...AAAA..AAAA...', '', '...B........B...', '', '..AAAAA..AAAAA..', '', '.AAAAAA..AAAAAA.'],
    'buskoba': ['', '', '', '', '', '', '...ABAB..BABA...', '', '', '', '', '', ''],
}


def merge(base, extra):
    out = [list(r) for r in base]
    for y, row in enumerate(extra):
        for x, ch in enumerate(row):
            if ch != '.' and y < len(out) and x < 16:
                out[y][x] = ch
    return [''.join(r) for r in out]


def icon_from_map(rows, series, texture=None):
    sp = Sprite(16, 16)
    pal = PAL[series]
    for letter in 'METABDH':
        m = np.zeros((16, 16), bool)
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == letter:
                    m[y, x] = True
        if m.any():
            if letter == 'H':       # glowing halo: no dark outline
                sp.add(m, R['halo'], outline=False, shade='flatlight')
            else:
                sp.add(m, R[pal[letter]])
    if texture:
        texture(sp)
    return sp.render()[0]


def fur_texture(sp):
    # sakeran: fur tufts on main parts
    for i, p in enumerate(sp.parts):
        if p['mat'] is R['fur']:
            m = sp.part_mask(i)
            ys, xs = np.mgrid[0:16, 0:16]
            sp.tone(m & ((xs * 3 + ys * 5) % 7 == 0), 1)


def icons():
    out = {}
    for s in SERIES_ORDER:
        tex = fur_texture if s == 'sakeran' else None
        out[f'{s}_helmet'] = icon_from_map(HELMET[s], s, tex)
        out[f'{s}_chestplate'] = icon_from_map(merge(CHEST, CHEST_X[s]), s, tex)
        out[f'{s}_leggings'] = icon_from_map(merge(LEGS, [r or '' for r in LEGS_X[s]]), s, tex)
        out[f'{s}_boots'] = icon_from_map(merge(BOOTS, [r or '' for r in BOOTS_X[s]]), s, tex)
    return out


# ---------------------------------------------------------------------------
# worn textures (64x32)
class Tex:
    def __init__(self):
        self.a = np.zeros((32, 64, 4), np.uint8)

    @staticmethod
    def box(u, v, w, h, d):
        return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
                'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
                'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h)}

    def fill(self, face, fn):
        x0, y0, fw, fh = face
        for j in range(fh):
            for i in range(fw):
                c = fn(i, j, fw, fh)
                if c is not None:
                    self.a[y0 + j, x0 + i] = c if len(c) == 4 else tuple(c) + (255,)


HEAD = Tex.box(0, 0, 8, 8, 8)
HAT = Tex.box(32, 0, 8, 8, 8)
BODY = Tex.box(16, 16, 8, 12, 4)
ARM = Tex.box(40, 16, 4, 12, 4)
LEG = Tex.box(0, 16, 4, 12, 4)
FACES = ('top', 'bottom', 'right', 'front', 'left', 'back')


def h2(i, j, s=0):
    """Small deterministic hash for hand-made-looking texture noise."""
    return ((i * 73856093) ^ (j * 19349663) ^ (s * 83492791)) & 0xffff


def front_edge(face, i, w):
    """True for the column of a side face that touches the front face."""
    return (face == 'right' and i == w - 1) or (face == 'left' and i == 0)


def back_edge(face, i, w):
    return (face == 'right' and i == 0) or (face == 'left' and i == w - 1)


class Style:
    """Default armour painter. Series subclasses add their own details."""
    M, T, A, B, D = 'leather', 'iron', 'gold', 'iron', 'sole'

    def __init__(self):
        self.m, self.t, self.ac, self.b, self.d = (R[self.M], R[self.T], R[self.A], R[self.B], R[self.D])

    # texture of the main material
    def mat(self, i, j, face, tone=2, s=0):
        r = self.m
        if face == 'top':
            tone = min(4, tone + 1)
        elif face == 'bottom':
            tone = max(1, tone - 1)
        n = h2(i, j, s) % 9
        if n == 0:
            tone = max(1, tone - 1)
        elif n == 1:
            tone = min(4, tone + 1)
        return r[tone]

    def trim(self, tone=2):
        return self.t[tone]

    # ---------------- helmet
    def helmet(self, face, i, j, w, h):
        m, t = self.mat, self.t
        if face == 'bottom':
            return t[1] if (i in (0, w - 1) or j in (0, h - 1)) else None
        if face == 'top':
            if i in (0, w - 1) or j in (0, h - 1):
                return t[3] if j == h - 1 or i == 0 else t[2]
            return m(i, j, face, 3)
        if face == 'front':
            if j <= 1:
                return m(i, j, face, 3 if j == 0 else 2)
            if j == 2:
                return t[3] if i < w // 2 else t[2]
            if i <= 1 or i >= w - 2:
                if j == h - 1:
                    return t[1]
                return t[2] if i in (0, w - 1) else m(i, j, face, 2)
            return None
        # sides / back
        if j == h - 1:
            return t[1]
        if j == 0:
            return m(i, j, face, 3)
        if front_edge(face, i, w):
            return t[2]
        if face == 'back' and j >= h - 2:
            return t[2]
        return m(i, j, face, 2)

    def hat(self, face, i, j, w, h):
        return None

    # ---------------- chestplate
    def body(self, face, i, j, w, h):
        m, t = self.mat, self.t
        if face == 'top':
            return m(i, j, face, 3) if not (2 <= i <= 5 and j >= 1) else m(i, j, face, 2)
        if face == 'bottom':
            return m(i, j, face, 1)
        if j == 0:
            return t[3]
        if j >= h - 2:
            return t[2] if j == h - 2 else t[1]
        return m(i, j, face, 2)

    def arm(self, face, i, j, w, h):
        m, t = self.mat, self.t
        if face == 'top':
            return t[3]
        if face == 'bottom':
            return m(i, j, face, 1)
        if j <= 3:
            return t[3] if j == 0 else (t[1] if j == 3 else t[2])
        if j >= 9:
            return t[3] if j == 9 else t[2]
        return m(i, j, face, 2)

    # ---------------- boots (legs box of layer 1, lower half only)
    def boot(self, face, i, j, w, h):
        m, t, d = self.mat, self.t, self.d
        if face == 'top':
            return None
        if face == 'bottom':
            return d[1]
        if j < 6:
            return None
        if j == 6:
            return t[3]
        if j == h - 1:
            return d[2]
        if face == 'front' and j >= 9:
            return t[2] if j == 9 else t[1]
        return m(i, j, face, 2 if j > 7 else 3)

    # ---------------- leggings (layer 2)
    def waist(self, face, i, j, w, h):
        m, t, a = self.mat, self.t, self.ac
        if face == 'top':
            return None
        if face == 'bottom':
            return m(i, j, face, 1)
        if j < 7:
            return None
        if j == 7:
            return t[3]
        if j == 8:
            if face == 'front' and i in (3, 4):
                return a[3] if i == 3 else a[2]
            return t[2]
        return m(i, j, face, 2 if j < 11 else 1)

    def leg(self, face, i, j, w, h):
        m, t = self.mat, self.t
        if face == 'top':
            return m(i, j, face, 2)
        if face == 'bottom':
            return m(i, j, face, 1)
        if face == 'front' and j in (5, 6):
            return t[3] if j == 5 else t[2]
        if j == 0:
            return t[2]
        return m(i, j, face, 2 if j < 11 else 1)

    # ---------------------------------------------------------------
    def paint(self):
        l1, l2 = Tex(), Tex()
        for f in FACES:
            l1.fill(HEAD[f], lambda i, j, w, h, f=f: self.helmet(f, i, j, w, h))
            l1.fill(HAT[f], lambda i, j, w, h, f=f: self.hat(f, i, j, w, h))
            l1.fill(BODY[f], lambda i, j, w, h, f=f: self.body(f, i, j, w, h))
            l1.fill(ARM[f], lambda i, j, w, h, f=f: self.arm(f, i, j, w, h))
            l1.fill(LEG[f], lambda i, j, w, h, f=f: self.boot(f, i, j, w, h))
            l2.fill(BODY[f], lambda i, j, w, h, f=f: self.waist(f, i, j, w, h))
            l2.fill(LEG[f], lambda i, j, w, h, f=f: self.leg(f, i, j, w, h))
        return l1.a, l2.a


# ---------------------------------------------------------------------------
# Hand-drawn per-series face maps.
#   M m N n k : main ramp   (mid, dark, light, highlight, outline)
#   x X       : main with the series' material texture (mid / light)
#   T t U u K : trim ramp
#   z         : trim with weave texture
#   A a B b   : accent ramp      C c E e : second accent ramp
#   D d       : dark (soles)     w : white      . : transparent
# Side maps are drawn for the 'right' face (front edge = right column);
# the 'left' face uses the mirrored map.
R['strap'] = ramp('#140a06', '#2e1a10', '#4a2c1a', '#6a4228', '#8a5c3a')
R['furgrey'] = ramp('#2a2a32', '#76768a', '#a2a2b0', '#c4c4cc', '#e0e0e6')
R['sheen'] = ramp('#3a3a5a', '#a8b0e0', '#ccd2f4', '#e6eaff', '#ffffff')
R['gunmetal'] = ramp('#0a0a0e', '#2c2f38', '#454a56', '#626876', '#8a90a0')

SERIES_RAMPS = {
    'hunter':   dict(M='leather', T='iron', A='gold', C='strap', D='sole'),
    'sakeran':  dict(M='fur', T='claw', A='pink', C='furgrey', D='claw'),
    'barino':   dict(M='pearl', T='gold', A='halo', C='sheen', D='sole'),
    'jangria':  dict(M='crimson', T='horn', A='lava', C='horn', D='horn'),
    'busaiber': dict(M='carbon', T='gunmetal', A='cyan', C='magenta', D='sole'),
    'buskoba':  dict(M='skin', T='straw', A='red', C='green', D='sole'),
}

E8 = ['........'] * 8


def _tex_hunter(i, j, s):          # leather: scuffs and stitches
    n = h2(i, j, s) % 11
    return 1 if n == 0 else (3 if n == 1 else 2)


def _tex_sakeran(i, j, s):         # fur: sparse diagonal tufts
    if (i + 2 * j + s) % 6 == 0:
        return 3
    if (2 * i + j + s) % 11 == 3:
        return 1
    return 2


def _tex_barino(i, j, s):          # pearl: smooth with rare sheen
    return 3 if h2(i, j, s) % 13 == 0 else 2


def _tex_jangria(i, j, s):         # scales
    return 1 if (i + 2 * (j % 2)) % 4 == 0 else 2


def _tex_busaiber(i, j, s):        # carbon-fibre weave
    return 2 if ((i // 2) + j) % 2 == 0 else 1


def _tex_buskoba(i, j, s):         # woven cloth
    return 3 if (i + j) % 4 == 0 else 2


TEXF = {'hunter': _tex_hunter, 'sakeran': _tex_sakeran, 'barino': _tex_barino,
        'jangria': _tex_jangria, 'busaiber': _tex_busaiber, 'buskoba': _tex_buskoba}

MAPS = {}

MAPS['hunter'] = dict(
    head_front=['NNNUUNNN', 'xxxTTxxx', 'UUUUUUUU', 'TT.TT.TT', 'xm.Tt.mx', 'xm....mx', 'xx....xx', 'mm....mm'],
    head_side=['NNNNNNNN', 'xxxxxxxx', 'UUUUUUUU', 'xxxxxUUT', 'xmxxxTuT', 'xxxxxTTt', 'xxxxxttt', 'mmmmmmmm'],
    head_back=['NNNUUNNN', 'xxxTTxxx', 'UUUUUUUU', 'xxxxxxxx', 'xmxxxxmx', 'UUUUUUUU', 'TuTTTTuT', 'tttttttt'],
    head_top=['NNNUUNNN', 'NXNUUNXN', 'NNNUuNNN', 'NXNUUNNN', 'NNNUUNXN', 'NNNUuNNN', 'NXNUUNNN', 'NNNUUNNN'],
    hat_front=['...UU...', '...Tt...'] + ['........'] * 6,
    hat_back=['...UU...', '...Tt...'] + ['........'] * 6,
    hat_top=['...Uu...'] * 8,
    body_front=['NUNNNNUN', 'xUuUUuUx', 'xUTTTTUx', 'xTTuTTTx', 'xTTTTTtx', 'xtTTTTtx', 'xxttttxx',
                'xxxxxxxx', 'xmxxxxmx', 'CCCAACCC', 'CCCaaCCC', 'mxmxxmxm'],
    body_back=['NNNNNNNN', 'CxxxxxxC', 'xCxxxxCx', 'xxCxxCxx', 'xxxUUxxx', 'xxxUTxxx', 'xxCxxCxx',
               'xCxxxxCx', 'Cxxxxxxm', 'CCCCCCCC', 'CcCCCCcC', 'mxmxxmxm'],
    body_side=['NNNN', 'xxxx', 'xCxx', 'xxCx', 'xCxx', 'xxCx', 'xCxx', 'xxxx', 'xxxm', 'CCCC', 'CcCC', 'mxmx'],
    body_top=['NUNNNNUN', 'NUNNNNUN', 'NUNNNNUN', 'NUNNNNUN'],
    arm=['uUUU', 'UTTT', 'TTTt', 'tttt', 'xxxx', 'xmxx', 'xxxx', 'xxmx', 'xxxx', 'CUCC', 'UUUU', 'CCCC'],
    arm_top=['uUUU', 'UUUU', 'UUTU', 'TTTT'],
    arm_bottom=['CCCC'] * 4,
    boot=['NNNN', 'xxxx', 'xmxx', 'xxxx', 'CCCC', 'dddd'],
    boot_front=['NNNN', 'xxxx', 'xUUx', 'UUUU', 'TTTT', 'dddd'],
    waist_front=['CCCCCCCC', 'CCCAACCC', 'NNxxxxNN', 'NNxxxxNN', 'mmxxxxmm'],
    waist_back=['CCCCCCCC', 'CCCCCCCC', 'xxxxxxxx', 'xmxxxxmx', 'mxmxxmxm'],
    waist_side=['CCCC', 'CCCC', 'xxxx', 'xxmx', 'mxmx'],
    leg=['CCCC', 'xxxx', 'xxxx', 'xmxx', 'xxxx', 'CCCC', 'xxxx', 'xxmx', 'xxxx', 'xxxx', 'xmxx', 'mmmm'],
    leg_front=['CCCC', 'xxxx', 'xNxx', 'xxxx', 'UUUU', 'UuTT', 'TTTt', 'tttt', 'xxxx', 'xmxx', 'xxxx', 'mmmm'],
)

MAPS['sakeran'] = dict(
    head_front=['NNNNNNNN', 'xxxxxxxx', 'xxxxxxxx', 'xtxUtxtx', 'xT....Tx', 'xx....xx', 'Nx....xN', 'NC....CN'],
    head_side=['NNNNNNNN', 'xxxxxxxx', 'xxxxxxxx', 'xxxxxxxT', 'xxxxxxxt', 'xxxxxxxx', 'CxCxxCxx', 'cCcCCcCc'],
    head_back=['NNNNNNNN', 'xxxxxxxx', 'xxxxxxxx', 'xxxCCxxx', 'xxCxxCxx', 'xxxxxxxx', 'CxCxxCxC', 'cCcCCcCc'],
    head_top=['xxxxxxxx'] * 8,
    hat_front=['NN....NN', 'NA....AN', 'x......x'] + ['........'] * 4 + ['N.N..N.N'],
    hat_side=['......NN', '......xN', '.......x'] + ['........'] * 4 + ['N.N.N.N.'],
    hat_back=['........'] * 7 + ['N.N..N.N'],
    hat_top=['........'] * 6 + ['NN....NN', 'NN....NN'],
    body_front=['XXNNNNXX', 'XtXtTXtX', 'xxXXXXxx', 'xxxXXxxx', 'xCxXXxCx', 'xxCxxCxx', 'xxxxxxxx',
                'xxxCCxxx', 'xxxxxxxx', 'TtTUTtTt', 'tTtTTtTt', 'xCxCxCxC'],
    body_back=['XXNNNNXX', 'xxxxxxxx', 'xxtxtxtx', 'xxtxtxtx', 'xtxtxtxx', 'xtxtxtxx', 'txtxtxxx',
               'xxxxxxxx', 'xCxxxxCx', 'TtTtTtTt', 'tTtTtTtT', 'xCxCxCxC'],
    body_side=['XXXX', 'xxxx', 'xxxx', 'xxxx', 'xxCx', 'xxxx', 'xCxx', 'xxxx', 'xxxx', 'TtTt', 'tTtT', 'xCxC'],
    body_top=['XXXXXXXX'] * 4,
    body_bottom=['CCCCCCCC'] * 4,
    arm=['XXXX', 'XNXN', 'xxxx', 'CxCx', 'xxxx', 'xNxx', 'xxCx', 'xxxx', 'xxxx', 'TTTT', 'tUtU', 'wTwT'],
    arm_top=['XXXX'] * 4,
    arm_bottom=['tttt'] * 4,
    boot=['XXXX', 'xxxx', 'xCxx', 'xxxx', 'CxCx', 'tttt'],
    boot_front=['XXXX', 'xxxx', 'xxxx', 'xxxx', 'wtwt', 'tTtT'],
    boot_bottom=['tttt'] * 4,
    waist_front=['TtTtTtTt', 'tTtwwtTt', 'xxXXXXxx', 'xxXxxXxx', 'x.XxxX.x'],
    waist_back=['TtTtTtTt', 'tTtTtTtT', 'xxxxxxxx', 'xCxxxxCx', 'xCxCxCxC'],
    waist_side=['TtTt', 'tTtT', 'xxxx', 'xCxx', 'CxCx'],
    leg=['TtTt', 'xxxx', 'xxxx', 'xCxx', 'xxxx', 'xxxx', 'xxCx', 'xxxx', 'xxxx', 'xCxx', 'xxxx', 'CxCx'],
    leg_front=['TtTt', 'xxxx', 'XxxX', 'xxxx', 'tTTt', 'TUUT', 'tTTt', 'xxxx', 'xxxx', 'xCxx', 'xxxx', 'CxCx'],
)

MAPS['barino'] = dict(
    head_front=['TUUUUUUT', 'NnNUUNnN', 'MNUbbUNM', 'UT.TT.TU', 'NM....MN', 'MM....MM', 'MM....MM', 'UT....TU'],
    head_side=['TUUUUUUU', 'NNNNNNNN', 'MUUUNNMM', 'MMUUUNMU', 'MMMUUMMU', 'MMMMMMMT', 'mMMMMMmT', 'TTTTTTTT'],
    head_back=['TUUUUUUT', 'NNNNNNNN', 'NMMMMMMN', 'MMMUUMMM', 'MMUbbUMM', 'MMMUUMMM', 'mMMMMMMm', 'TTTTTTTT'],
    head_top=['NNNNNNNN', 'NnNNNNnN', 'NNNUUNNN', 'NNUNNUNN', 'NNUNNUNN', 'NNNUUNNN', 'NnNNNNnN', 'NNNNNNNN'],
    hat_top=['........', '..BbbB..', '.B....B.', '.b....b.', '.b....b.', '.B....B.', '..BbbB..', '........'],
    body_front=['UUUUUUUU', 'TNNNNNNT', 'TNnUUnNT', 'TNUbbUNT', 'TNUBBUNT', 'TNNUUNNT', 'TMNNNNMT',
                'TMMEMMMT', 'TmMMMMmT', 'UUUBBUUU', 'TTTTTTTT', 'NMNMMNMN'],
    body_back=['UUUUUUUU', 'NNNNNNNN', 'UNNNNNNU', 'NUNNNNUN', 'UNUNNUNU', 'NUNUUNUN', 'MNUMMUNM',
               'MMMUUMMM', 'mMMMMMMm', 'UUUUUUUU', 'TTTTTTTT', 'NMNMMNMN'],
    body_side=['UUUU', 'NNNN', 'NNNN', 'NMEN', 'MMMM', 'MMMM', 'MEMM', 'MMMM', 'mMMm', 'UUUU', 'TTTT', 'NMNM'],
    body_top=['UUUUUUUU', 'NNNNNNNN', 'NNNNNNNN', 'UUUUUUUU'],
    arm=['uUUU', 'NNNN', 'NnNN', 'UUUU', 'MMMM', 'MNEM', 'MMMM', 'MMMM', 'mMMm', 'UUUU', 'TBTT', 'TTTT'],
    arm_top=['uUUU', 'UNNU', 'UNNU', 'UUUU'],
    arm_bottom=['TTTT'] * 4,
    boot=['UUUU', 'NNUN', 'MUUN', 'MMUM', 'MMMM', 'TTTT'],
    boot_front=['UUUU', 'NNNN', 'NUUN', 'MMMM', 'UUUU', 'TTTT'],
    waist_front=['UUUUUUUU', 'TTTBBTTT', 'NMNNNNMN', 'NMNMMNMN', 'mMmMMmMm'],
    waist_back=['UUUUUUUU', 'TTTTTTTT', 'NNNNNNNN', 'NMNMMNMN', 'mMmMMmMm'],
    waist_side=['UUUU', 'TTTT', 'NNNN', 'NMNM', 'mMmM'],
    leg=['TTTT', 'NNNN', 'NNNN', 'NMEN', 'MMMM', 'MMMM', 'UUUU', 'MMMM', 'MMMM', 'MNEM', 'MMMM', 'mmmm'],
    leg_front=['TTTT', 'NNNN', 'NnNN', 'NNNN', 'UUUU', 'UbBU', 'UUUU', 'MMMM', 'MNMM', 'MMMM', 'MMMM', 'mmmm'],
)

MAPS['jangria'] = dict(
    head_front=['TtTTTTtT', 'xxxAxxxx', 'xxAaAxxx', 'UTTTTTTU', 'Tt....tT', 'xA....Ax', 'xa....ax', 'tt....tt'],
    head_side=['tTTTTTTT', 'xxxxxxxx', 'xxxaxxxx', 'xxaAaxxT', 'xxxAxxTt', 'xxAxxxxT', 'xxaxxxxt', 'TtTtTtTt'],
    head_back=['TtTUUTtT', 'xxxTTxxx', 'xxxTTxxx', 'xaxTTxax', 'xAxTTxAx', 'xaxTTxax', 'xxxtTxxx', 'TtTtTtTt'],
    head_top=['xxxTTxxx', 'xxxTTxxx', 'xAxUTxxx', 'xxxTTxxx', 'xxxUTxAx', 'xxxTTxxx', 'xaxTTxxx', 'xxxTTxxx'],
    hat_side=['U.......', 'UT......', '.UT.....', '..UTT...', '...TTTt.', '....Tttt', '........', '........'],
    hat_front=['U......U', 'T......T', 'T......T', 't......t'] + ['........'] * 4,
    hat_top=['T......T', 'T......T', 'T......T', 'T......T', 't......t', 't......t', '........', '........'],
    hat_back=['U......U', 'T......T', '........', '........'] + ['........'] * 4,
    body_front=['TtTTTTtT', 'xxxxxxxx', 'xAaxxxxx', 'xxAxxaAx', 'xxaAxAxx', 'xxxTTxxx', 'xxTbBTxx',
                'xaxTTxxx', 'xAxxxxAx', 'TUTTTTUT', 'tTtAAtTt', 'TxTxxTxT'],
    body_back=['TtTTTTtT', 'xxxUTxxx', 'xxxTtxxx', 'xaxUTxax', 'xAxTtxAx', 'xaxUTxax', 'xxxTtxxx',
               'xxxUTxxx', 'xxxTtxxx', 'TUTTTTUT', 'tTtTTtTt', 'TxTxxTxT'],
    body_side=['TtTt', 'xxxx', 'xaxx', 'xAxx', 'xxAx', 'xxax', 'xxxx', 'xaxx', 'xAxx', 'TUTT', 'tTtT', 'TxTx'],
    body_top=['TxxxxxxT', 'xxxxxxxx', 'xxxxxxxx', 'TxxxxxxT'],
    arm=['UtUt', 'TUTU', 'TTTT', 'tttt', 'xxxx', 'xaxx', 'xAax', 'xxAx', 'xxxx', 'TUTU', 'TTTT', 'tAtA'],
    arm_top=['UTTU', 'TUUT', 'TUUT', 'UTTU'],
    arm_bottom=['tttt'] * 4,
    boot=['TUTU', 'TTTT', 'xxxx', 'xaxx', 'xxAx', 'tttt'],
    boot_front=['TUTU', 'TTTT', 'xAxx', 'xxax', 'TTTT', 'tUtU'],
    boot_bottom=['tttt'] * 4,
    waist_front=['TUTTTTUT', 'tTtAAtTt', 'xxxxxxxx', 'xaxxxxax', 'TxTxxTxT'],
    waist_back=['TUTTTTUT', 'tTtTTtTt', 'xxxxxxxx', 'xxaxxaxx', 'TxTxxTxT'],
    waist_side=['TUTT', 'tTtT', 'xxxx', 'xaxx', 'TxTx'],
    leg=['TtTt', 'xxxx', 'xaxx', 'xAxx', 'xxAx', 'xxxx', 'xxxx', 'xaxx', 'xAxx', 'xxax', 'xxxx', 'mmmm'],
    leg_front=['TtTt', 'xxxx', 'xxax', 'xAxx', 'UTTU', 'TUUT', 'tTTt', 'xxxx', 'xaxx', 'xxAx', 'xxxx', 'mmmm'],
)

MAPS['busaiber'] = dict(
    head_front=['UUUUUUUU', 'xxxxxxxx', 'TTTTTTTT', 'AABbbBAA', 'aAAAAAAa', 'T......T', 'xC....Cx', 'tt....tt'],
    head_side=['UUUUUUUU', 'xxxxxxxx', 'TTTTTTTT', 'xxxxxxAA', 'xAAAAxaA', 'xxxxAxxx', 'xxCxAxxT', 'tttttttt'],
    head_back=['UUUUUUUU', 'xxxxxxxx', 'TTTTTTTT', 'xxAAAAxx', 'xxAxxAxx', 'xCAxxACx', 'xxxxxxxx', 'tttttttt'],
    head_top=['xxxAAxxx', 'xxxAAxxx', 'xxxBAxxx', 'xxxAAxxx', 'xxxAAxxx', 'xxxABxxx', 'xxxAAxxx', 'xxxAAxxx'],
    hat_side=['.b......', '.A......', '.A......', '.a......'] + ['........'] * 4,
    hat_front=['........', '........', '........', 'ABBbbBBA'] + ['........'] * 4,
    body_front=['UUUUUUUU', 'xTxxxxTx', 'xAxxxxAx', 'xAxCExAx', 'xAxcCxAx', 'xAAxxAAx', 'xxAxxAxx',
                'xxAAAAxx', 'xxxxxxxx', 'TTTTTTTT', 'AAAACAAA', 'tttttttt'],
    body_back=['UUUUUUUU', 'xxxAAxxx', 'xxxAAxxx', 'CCxAAxCC', 'ccxAAxcc', 'xxxAAxxx', 'xxxAAxxx',
               'xxxAAxxx', 'xxxxxxxx', 'TTTTTTTT', 'AAAAAAAA', 'tttttttt'],
    body_side=['UUUU', 'xxxx', 'xAxx', 'xAxx', 'xAxx', 'xAAx', 'xxAx', 'xxAx', 'xxxx', 'TTTT', 'AAAA', 'tttt'],
    body_top=['UUUUUUUU', 'UxxxxxxU', 'UxxxxxxU', 'UUUUUUUU'],
    body_bottom=['tttttttt'] * 4,
    arm=['UUUU', 'TTTT', 'TATT', 'tttt', 'xxxx', 'xAxx', 'xAxx', 'xAxx', 'xxxx', 'CCCC', 'TTTT', 'tAtA'],
    arm_top=['UUUU', 'UAAU', 'UAAU', 'UUUU'],
    arm_bottom=['tttt'] * 4,
    boot=['UUUU', 'xxxx', 'xAxx', 'xAxx', 'xAAA', 'AAAA'],
    boot_front=['UUUU', 'xxxx', 'xCCx', 'xxxx', 'TTTT', 'AAAA'],
    waist_front=['TTTTTTTT', 'AAACCAAA', 'xxxxxxxx', 'xAxxxxAx', 'tttttttt'],
    waist_back=['TTTTTTTT', 'AAAAAAAA', 'xxxxxxxx', 'xxAxxAxx', 'tttttttt'],
    waist_side=['TTTT', 'AAAA', 'xxxx', 'xAxx', 'tttt'],
    leg=['TTTT', 'xxxx', 'xAxx', 'xAxx', 'xAxx', 'xAxx', 'xAxx', 'xAxx', 'xAxx', 'xAxx', 'xxxx', 'tttt'],
    leg_front=['TTTT', 'xxxx', 'xxxx', 'xxxx', 'TTTT', 'TCCT', 'TTTT', 'xxxx', 'xAxx', 'xAxx', 'xxxx', 'tttt'],
)

MAPS['buskoba'] = dict(
    head_front=['UUUUUUUU', 'zzzzzzzz', 'ACACACAC', 'TTTTTTTT', '........', '........', '........', '........'],
    head_side=['UUUUUUUU', 'zzzzzzzz', 'ACACACAC', 'TTTTTTTT', '........', '........', '........', '........'],
    head_back=['UUUUUUUU', 'zzzzzzzz', 'ACACACAC', 'TTTTTTTT', '........', '........', '........', '........'],
    head_top=['zzzzzzzz', 'zUUUUUUz', 'zUzzzzUz', 'zUzuuzUz', 'zUzuuzUz', 'zUzzzzUz', 'zUUUUUUz', 'zzzzzzzz'],
    head_bottom=E8,
    hat_front=['........', '........', '........', 'UUUUUUUU', 'tTtTtTtT', 'A......A', '........', '........'],
    hat_side=['........', '........', '........', 'UUUUUUUU', 'tTtTtTtT', '.A..C..A', '........', '........'],
    hat_back=['........', '........', '........', 'UUUUUUUU', 'tTtTtTtT', 'A.C..C.A', '........', '........'],
    body_front=['NNNmmNNN', 'xxxNNxxx', 'AAAAAAAA', 'CCCCCCCC', 'xxxxxxxx', 'zzzzzzzz', 'xxxxxxxx',
                'CCCCCCCC', 'AAAAAAAA', 'xxxxxxxx', 'aAaAaAaA', 'A.C.A.C.'],
    body_back=['NNNNNNNN', 'xxxxxxxx', 'AAAAAAAA', 'CCCCCCCC', 'xxxxxxxx', 'zzzzzzzz', 'xxxxxxxx',
               'CCCCCCCC', 'AAAAAAAA', 'xxxxxxxx', 'aAaAaAaA', 'A.C.A.C.'],
    body_side=['NNNN', 'xxxx', 'AAAA', 'CCCC', 'xxxx', 'zzzz', 'xxxx', 'CCCC', 'AAAA', 'xxxx', 'aAaA', 'A.C.'],
    body_top=['NNNNNNNN', 'NAAAAAAN', 'NCCCCCCN', 'NNNNNNNN'],
    arm=['NNNN', 'AAAA', 'CCCC', 'xxxx', 'aAaA', 'xxxx', 'xxxx', 'xNxx', 'xxxx', 'CCCC', 'AAAA', 'mmmm'],
    arm_top=['NNNN', 'NAAN', 'NCCN', 'NNNN'],
    arm_bottom=['mmmm'] * 4,
    boot=['UUUU', 'xxxx', 'xAxx', 'xxCx', 'xxxx', 'dddd'],
    boot_front=['UUUU', 'xxxx', 'xACx', 'xxxx', 'zzzz', 'dddd'],
    waist_front=['AAAAAAAA', 'aaACCAaa', 'xxxAAxxx', 'xxxCAxxx', 'mxxAaxxm'],
    waist_back=['AAAAAAAA', 'aaaaaaaa', 'xxxxxxxx', 'xxxxxxxx', 'mmmmmmmm'],
    waist_side=['AAAA', 'aaaa', 'xxxx', 'xxxx', 'mmmm'],
    leg=['aaaa', 'xxxx', 'xAxx', 'xCxx', 'xAxx', 'xCxx', 'xAxx', 'xCxx', 'xAxx', 'xCxx', 'xAxx', 'mmmm'],
    leg_front=['aaaa', 'xxxx', 'xxxx', 'xxxx', 'xxxx', 'zzzz', 'xxxx', 'xxxx', 'xxxx', 'xxxx', 'xxxx', 'mmmm'],
)


class MapStyle:
    def __init__(self, series):
        self.s = series
        rr = SERIES_RAMPS[series]
        self.r = {k: R[v] for k, v in rr.items()}
        self.maps = MAPS[series]
        self.tex = TEXF[series]
        self.seed = SERIES_ORDER.index(series) + 1

    def col(self, ch, i, j, face, gi=0, gj=0):
        if ch in ('.', ' '):
            return None
        r = self.r
        adj = 1 if face == 'top' else (-1 if face == 'bottom' else 0)
        if ch in 'xX':
            t = self.tex(gi, gj, self.seed) + (1 if ch == 'X' else 0) + adj
            return r['M'][max(1, min(4, t))]
        if ch == 'z':
            t = (3 if (gi + gj) % 2 == 0 else 2) + adj
            return r['T'][max(1, min(4, t))]
        if ch == 'w':
            return (246, 246, 240, 255)
        table = {'M': ('M', 2), 'm': ('M', 1), 'N': ('M', 3), 'n': ('M', 4), 'k': ('M', 0),
                 'T': ('T', 2), 't': ('T', 1), 'U': ('T', 3), 'u': ('T', 4), 'K': ('T', 0),
                 'A': ('A', 2), 'a': ('A', 1), 'B': ('A', 3), 'b': ('A', 4),
                 'C': ('C', 2), 'c': ('C', 1), 'E': ('C', 3), 'e': ('C', 4),
                 'D': ('D', 2), 'd': ('D', 1)}
        k, t = table[ch]
        return r[k][t]

    def _get(self, key, default=None):
        return self.maps.get(key, default)

    def face_map(self, piece, face):
        """Return (rows, mirrored, row_offset) for a box face."""
        g = self._get
        if piece in ('head', 'hat'):
            if face in ('right', 'left'):
                return g(piece + '_side'), face == 'left', 0
            return g(piece + '_' + face), False, 0
        if piece in ('body', 'arm', 'leg'):
            if face in ('right', 'left'):
                m = g(piece + '_side') or g(piece)
                return m, face == 'left', 0
            if face in ('front', 'back'):
                return g(piece + '_' + face) or g(piece), False, 0
            return g(piece + '_' + face), False, 0
        if piece == 'boot':
            if face in ('right', 'left', 'back'):
                return g('boot'), face == 'left', 6
            if face == 'front':
                return g('boot_front') or g('boot'), False, 6
            if face == 'bottom':
                return g('boot_bottom') or ['dddd'] * 4, False, 0
            return None, False, 0
        if piece == 'waist':
            if face in ('right', 'left'):
                return g('waist_side'), face == 'left', 7
            if face in ('front', 'back'):
                return g('waist_' + face), False, 7
            if face == 'bottom':
                return ['mmmmmmmm'] * 4, False, 0
            return None, False, 0
        return None, False, 0

    def default(self, piece, face, w, h):
        if piece == 'head' and face == 'bottom':
            return ['TTTTTTTT'] + ['T......T'] * 6 + ['TTTTTTTT']
        if face == 'bottom':
            return ['m' * w] * h
        if face == 'top' and piece in ('body', 'arm', 'leg', 'head'):
            return ['x' * w] * h
        return None

    def paint(self):
        l1, l2 = Tex(), Tex()
        jobs = [(l1, HEAD, 'head'), (l1, HAT, 'hat'), (l1, BODY, 'body'), (l1, ARM, 'arm'),
                (l1, LEG, 'boot'), (l2, BODY, 'waist'), (l2, LEG, 'leg')]
        for tex, box, piece in jobs:
            for face in FACES:
                x0, y0, fw, fh = box[face]
                rows, mir, off = self.face_map(piece, face)
                if rows is None and piece not in ('hat', 'boot', 'waist'):
                    rows = self.default(piece, face, fw, fh)
                if rows is None:
                    continue
                assert len(rows) + off <= fh and all(len(r) == fw for r in rows), (self.s, piece, face)
                for jj, row in enumerate(rows):
                    j = jj + off
                    for ii in range(fw):
                        ch = row[fw - 1 - ii] if mir else row[ii]
                        c = self.col(ch, ii, j, face, x0 + ii, y0 + j)
                        if c is not None:
                            tex.a[y0 + j, x0 + ii] = c
        return l1.a, l2.a


def worn():
    return {s: MapStyle(s).paint() for s in SERIES_ORDER}


def write_all():
    for k, a in icons().items():
        save(a, f'item/{k}.png')
    for s, (l1, l2) in worn().items():
        save(l1, f'entity/equipment/humanoid/{s}.png')
        save(l2, f'entity/equipment/humanoid_leggings/{s}.png')


if __name__ == '__main__':
    write_all()
