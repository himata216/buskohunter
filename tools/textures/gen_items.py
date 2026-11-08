"""16x16 item textures: consumables, projectile display items, trophies."""
import math

import numpy as np

from pixlib import (MAT, C, Diag, Sprite, capsule, disk, ellipse, from_map,
                    make_ramp, outline_rgba, poly_mask, pts_mask, ramp, rect,
                    save)

S = 16
OUT = C('#1a1822')

LIQ = {
    'green':  ramp('#0e3410', '#1e6a24', '#38a83a', '#74d85a', '#d4f8b0'),
    'teal':   ramp('#0a2c3a', '#12607a', '#2a9cb4', '#5cd4e0', '#c8f8f6'),
    'yellow': ramp('#4a3804', '#a07c08', '#e0bc18', '#ffe852', '#fffbc8'),
    'red':    ramp('#3a0608', '#8a1018', '#cc2430', '#f2584a', '#ffc0b0'),
    'orange': ramp('#3e1804', '#9a400a', '#e0701a', '#ffa848', '#ffe6b0'),
    'gold':   ramp('#4a3004', '#a8720c', '#e8b020', '#ffdc5a', '#fff8d0'),
}
GLASS = ramp('#2a3440', '#9cb4c4', '#c4dae6', '#e2f0f6', '#ffffff')


def blank():
    return np.zeros((S, S, 4), np.uint8)


def put(a, x, y, c):
    if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
        a[y, x] = c if len(c) == 4 else tuple(c) + (255,)


def paint_map(a, rows, pal, ox=0, oy=0):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch not in ('.', ' '):
                put(a, ox + x, oy + y, pal[ch])
    return a


# ---------------------------------------------------------------------------
# bottles
def bottle(spans, liquid_from, liq, cap=None, cap_pal=None, cap_at=(0, 0), extra=None):
    """spans: {row: (x0, x1)} glass interior. Liquid fills rows >= liquid_from."""
    a = blank()
    ys = sorted(spans)
    ybot = ys[-1]
    for y in ys:
        x0, x1 = spans[y]
        wdt = max(1, x1 - x0)
        for x in range(x0, x1 + 1):
            t = (x - x0) / wdt
            if y >= liquid_from:
                tone = 3 if t < 0.25 else (1 if t > 0.72 else 2)
                if y == liquid_from:
                    tone = 3 if t < 0.72 else 2
                if y == ybot:
                    tone = max(1, tone - 1)
                put(a, x, y, liq[tone])
            else:
                tone = 4 if t < 0.2 else (1 if t > 0.75 else 3)
                put(a, x, y, GLASS[tone])
    # specular highlight on the bulb
    lx = spans[liquid_from + 1][0] + 1 if liquid_from + 1 in spans else None
    if lx is not None:
        put(a, lx, liquid_from + 1, liq[4])
        put(a, lx, liquid_from + 2, liq[4])
        put(a, lx + 1, liquid_from + 1, (255, 255, 255, 255))
    a = outline_rgba(a, OUT)
    if cap:
        paint_map(a, cap, cap_pal, *cap_at)
    if extra:
        extra(a)
    return a


CORK = {'k': OUT, 'c': C('#c08a50'), 'C': C('#e0b070'), 'd': C('#8a5a30')}
CORK_MAP = ['.kkkk.', 'kCCcdk', 'kcccdk', '.kkkk.']


def potion():
    spans = {4: (7, 8), 5: (7, 8), 6: (6, 9), 7: (4, 11), 8: (3, 12), 9: (3, 12),
             10: (3, 12), 11: (3, 12), 12: (4, 11), 13: (5, 10)}
    return bottle(spans, 8, LIQ['green'], CORK_MAP, CORK, (5, 1))


def mega_potion():
    spans = {3: (6, 9), 4: (6, 9), 5: (4, 11), 6: (3, 12)}
    for y in range(7, 14):
        spans[y] = (2, 13)
    spans[14] = (3, 12)
    g = MAT['gold']
    pal = {'k': OUT, 'g': g[2], 'G': g[3], 'h': g[4], 'd': g[1]}
    cap = ['.kkkkkk.', 'kGhGGGdk', 'kggggddk', '.kkkkkk.']

    def extra(a):
        for x in range(3, 13):          # gold label band
            put(a, x, 10, g[3] if x < 6 else (g[1] if x > 10 else g[2]))
        put(a, 7, 10, g[4])
    return bottle(spans, 6, LIQ['green'], cap, pal, (4, 0), extra)


def max_potion():
    spans = {4: (7, 8), 5: (6, 9), 6: (5, 10), 7: (4, 11), 8: (3, 12), 9: (3, 12),
             10: (3, 12), 11: (3, 12), 12: (4, 11), 13: (5, 10)}
    g = MAT['gold']
    pal = {'k': OUT, 'g': g[2], 'G': g[3], 'h': g[4], 'd': g[1], 'r': C('#e03040'), 'R': C('#ff8080')}
    cap = ['..k..k..', '.kGkkhk.', 'kGgRrgdk', '.kkkkkk.']

    def extra(a):
        star = (255, 252, 220, 255)
        for (x, y) in ((1, 4), (14, 6), (13, 1)):
            put(a, x, y, star)
        for (x, y) in ((0, 4), (2, 4), (1, 3), (1, 5)):
            put(a, x, y, LIQ['gold'][3])
        for x in range(4, 12):
            put(a, x, 12, g[3] if x < 6 else (g[1] if x > 9 else g[2]))
    return bottle(spans, 6, LIQ['gold'], cap, pal, (4, 0), extra)


def antidote():
    spans = {3: (7, 8), 4: (7, 8), 5: (7, 8), 6: (6, 9), 7: (6, 9), 8: (5, 10), 9: (5, 10),
             10: (4, 11), 11: (4, 11), 12: (3, 12), 13: (3, 12)}
    return bottle(spans, 9, LIQ['teal'], CORK_MAP, CORK, (5, 0))


def energy_drink():
    spans = {3: (7, 8), 4: (6, 9)}
    for y in range(5, 14):
        spans[y] = (5, 10)
    spans[14] = (6, 9)

    def extra(a):
        lab = C('#2a6ad0')
        for y in (9, 10, 11):
            for x in range(5, 11):
                put(a, x, y, lab if x < 10 else C('#1c4890'))
        for (x, y) in ((8, 9), (7, 10), (8, 10), (7, 11)):
            put(a, x, y, (255, 255, 255, 255))
    return bottle(spans, 5, LIQ['yellow'], CORK_MAP, CORK, (5, 0), extra)


def demondrug():
    spans = {3: (7, 8), 4: (7, 8), 5: (5, 10)}
    for y in range(6, 14):
        spans[y] = (4, 11)
    pal = {'k': OUT, 's': C('#3a3440'), 'S': C('#5c5468'), 'h': C('#e8e0d0')}
    cap = ['k....k', 'hk..kh', 'kSSSsk', 'kSssskk'[:6], '.kkkk.']

    def extra(a):
        # oni face mark: two eyes + fang
        for (x, y) in ((6, 9), (9, 9)):
            put(a, x, y, LIQ['red'][0])
        put(a, 7, 11, LIQ['red'][4])
        put(a, 8, 11, LIQ['red'][4])
    return bottle(spans, 6, LIQ['red'], cap, pal, (5, 0), extra)


def armorskin():
    spans = {5: (3, 12)}
    for y in range(6, 13):
        spans[y] = (2, 13)
    spans[13] = (3, 12)
    ir = MAT['iron']
    pal = {'k': OUT, 'i': ir[2], 'I': ir[3], 'h': ir[4], 'd': ir[1]}
    cap = ['...kkkk...', '.kkIhIIkk.', 'kIIiiiiddk', 'kkkkkkkkkk']

    def extra(a):
        o = LIQ['orange']
        sh = ['.....', 'ddddd', 'dllld', 'dllld', '.dld.', '..d..']
        for y, row in enumerate(sh):
            for x, ch in enumerate(row):
                if ch == 'd':
                    put(a, 6 + x, 6 + y, o[1])
                elif ch == 'l':
                    put(a, 6 + x, 6 + y, o[4])
    return bottle(spans, 6, LIQ['orange'], cap, pal, (3, 2), extra)


# ---------------------------------------------------------------------------
# shape based items (Sprite with shading)
def sphere_tones(sp, mask, cx, cy, r):
    ys, xs = np.mgrid[0:S, 0:S]
    d = np.sqrt((xs + 0.5 - (cx - r * 0.38)) ** 2 + (ys + 0.5 - (cy - r * 0.38)) ** 2) / (r * 2)
    sp.tone(mask & (d >= 0.0), 4)
    sp.tone(mask & (d >= 0.12), 3)
    sp.tone(mask & (d >= 0.36), 2)
    sp.tone(mask & (d >= 0.72), 1)


def ball(rmp, cx=7.5, cy=8.5, r=5.6):
    sp = Sprite(S, S)
    m = disk(cx, cy, r, S, S)
    sp.add(m, rmp)
    sphere_tones(sp, m, cx, cy, r)
    return sp, m


PINK = ramp('#4a0e2c', '#b02a6a', '#e45a9c', '#ff94c4', '#ffe0f0')
PURPLE = ramp('#2a0e3a', '#6a2a90', '#a050c8', '#d088f0', '#f6dcff')
GREENB = ramp('#0c2a12', '#22702c', '#3eaa40', '#7ed660', '#dcf8c0')
WATER = ramp('#0a2448', '#1a5aa0', '#3a96e0', '#84ccff', '#e8f8ff')
FLASH = ramp('#6a5410', '#e8c84a', '#fff08a', '#fffcd8', '#ffffff')


def paintball():
    sp, m = ball(PINK)
    a = sp.render()[0]
    # paint splotches + drip
    for (x, y) in ((9, 10), (10, 10), (10, 9), (6, 12), (11, 7)):
        put(a, x, y, PINK[1])
    put(a, 8, 14, PINK[2])
    put(a, 8, 15, PINK[0])
    return a


def flash_bomb():
    sp, m = ball(FLASH, 7.5, 8.5, 4.6)
    a = sp.render()[0]
    ray = C('#fff6a0')
    ray2 = C('#ffd84a')
    for (x, y) in ((7, 1), (8, 1), (7, 2), (8, 2), (7, 15), (8, 15), (0, 8), (1, 8), (14, 8), (15, 8)):
        put(a, x, y, ray)
    for (x, y) in ((2, 3), (3, 4), (13, 3), (12, 4), (2, 13), (3, 12), (13, 13), (12, 12)):
        put(a, x, y, ray2)
    put(a, 6, 6, (255, 255, 255, 255))
    return a


def tranq_bomb():
    sp, m = ball(ramp('#2e0c30', '#8a3092', '#c460c4', '#ec9ce0', '#ffe4fa'), 7.5, 9.0, 5.4)
    fuse = rect(7, 1, 8, 3, S, S)
    sp.add(fuse, 'brass')
    a = sp.render()[0]
    w = (255, 255, 255, 255)
    for (x, y) in ((6, 8), (7, 8), (8, 8), (7, 9), (6, 10), (7, 10), (8, 10)):  # Z
        put(a, x, y, w)
    for (x, y) in ((10, 6), (11, 6), (11, 7), (10, 8), (11, 8)):  # small z
        put(a, x, y, C('#ffd0f4'))
    put(a, 8, 0, C('#ffe080'))
    return a


def farcaster():
    sp, m = ball(GREENB, 8.0, 9.0, 5.2)
    a = sp.render()[0]
    sw = C('#c8f8b0')
    for (x, y) in ((6, 7), (7, 6), (8, 6), (9, 7), (9, 8), (8, 9), (7, 9)):  # swirl
        put(a, x, y, sw)
    put(a, 8, 8, GREENB[1])
    smoke = [C('#e8f4e8'), C('#b0ccb4'), C('#5a7060')]
    puff = ['.ab.', 'abbc', '.bc.']
    pal = {'a': smoke[0], 'b': smoke[1], 'c': smoke[2]}
    paint_map(a, puff, pal, 1, 1)
    paint_map(a, ['ab', 'bc'], pal, 12, 1)
    return a


def proj_water_orb():
    sp, m = ball(WATER, 7.5, 8.0, 5.8)
    inner = disk(7.5, 8.0, 4.4, S, S)
    sp.tone(inner, 2)
    sp.tone(inner & disk(9.5, 10.0, 3.0, S, S), 1)
    a = sp.render()[0]
    for (x, y) in ((4, 5), (5, 4), (4, 6), (6, 4)):
        put(a, x, y, (255, 255, 255, 255))
    put(a, 10, 11, WATER[3])
    put(a, 12, 2, WATER[3])
    put(a, 13, 1, WATER[4])
    return a


def proj_holy_orb():
    gold = ramp('#5a3a08', '#d09a20', '#ffd24a', '#fff2a0', '#ffffff')
    sp, m = ball(gold, 7.5, 8.5, 4.2)
    a = sp.render()[0]
    ray = C('#fff4b0')
    for i in range(1, 4):
        for (x, y) in ((7, 4 - i), (8, 4 - i), (7, 12 + i), (8, 12 + i), (3 - i, 8), (3 - i, 9), (11 + i, 8), (11 + i, 9)):
            put(a, x, y, ray if i < 3 else C('#ffd24a'))
    for (x, y) in ((3, 4), (12, 4), (3, 13), (12, 13)):
        put(a, x, y, C('#ffe680'))
    return a


def proj_fireball():
    a = blank()
    rows = [
        '................',
        '..........r.....',
        '.......r.rOr....',
        '......rOrOyOr...',
        '....r.rOyyyOr...',
        '...rOrOyywyyOr..',
        '..rOOOyywwwyOr..',
        '..rOyyywwwwyOr..',
        '.rROOyywwwyyOr..',
        '.rROOOyyyyyOrr..',
        '..rROOOyyyOOr...',
        '..rrROOOOOOrr...',
        '...rrRROOORr....',
        '.....rrrrrr.....',
        '................',
        '................',
    ]
    pal = {'r': C('#7a1408'), 'R': C('#c42a10'), 'O': C('#ff6a1a'), 'y': C('#ffc040'), 'w': C('#fff6c8')}
    paint_map(a, rows, pal)
    return outline_rgba(a, C('#3a0804'))


def proj_shell():
    a = blank()
    rows = [
        '................',
        '.....ss..ss.....',
        '...sssSsSSSs....',
        '..sSSSoooSSSs...',
        '..sSoooyyooSs...',
        '.sSooyywyyooSs..',
        '.sSoyywwwyyoSs..',
        '.sSooywwwyoooSs.',
        '..sSooyyyyooSs..',
        '..sSSoooooSSs...',
        '...sSSSoSSSSs...',
        '....ssSSSss.s...',
        '..s...sss....s..',
        '.ss..........ss.',
        '................',
        '................',
    ]
    pal = {'s': C('#5a5660'), 'S': C('#9a96a0'), 'o': C('#ff7a20'), 'y': C('#ffc84a'), 'w': C('#fffae0')}
    paint_map(a, rows, pal)
    return a


def proj_bullet():
    sp = Sprite(S, S)
    D = Diag(1, 15, S, S)
    case = D.band(2.6, 8.6, -2.5, 2.5)
    rim = D.band(1.6, 2.6, -2.5, 2.5)
    tip = D.poly([(8.6, 2.5), (10.0, 2.5), (12.6, 0.5), (12.6, -0.5), (10.0, -2.5), (8.6, -2.5)])
    sp.add(case, 'brass')
    sp.add(rim, 'brass')
    sp.add(tip, ramp('#3a1a0c', '#8a4020', '#c06a34', '#e8985a', '#ffd0a0'))
    sp.tone(rim, 1)
    sp.tone(case & (np.abs(D.V - 1) < .1), 4)
    sp.tone(case & (np.abs(D.V - 2) < .1), 3)
    sp.tone(case & (D.V < -0.5), 1)
    sp.tone(sp.part_mask(2) & (np.abs(D.V - 1) < .1), 4)
    a = sp.render()[0]
    return a


def proj_arrow():
    sp = Sprite(S, S)
    D = Diag(0, 16, S, S)
    shaft = D.band(2.5, 11.5, -0.5, 1.5)
    head = D.poly([(10.5, 3.5), (15.4, 0.5), (10.5, -2.5)])
    fl = D.band(1.0, 4.5, 1.5, 3.5) | D.band(1.0, 4.5, -2.5, -0.5)
    glow = ramp('#0a3a4a', '#28a8c8', '#68e0f0', '#b8f8ff', '#ffffff')
    sp.add(shaft, glow)
    sp.add(fl, ramp('#3a1010', '#a02828', '#d84848', '#f88080', '#ffd0d0'))
    sp.add(head, glow)
    sp.tone(head, 3)
    sp.tone(head & (np.abs(D.V - 0.5) < 0.6), 4)
    sp.tone(shaft, 3)
    sp.tone(shaft & (D.V < 0.5), 2)
    a = sp.render()[0]
    # sparkle trail
    for (x, y) in ((1, 10), (4, 13)):
        put(a, x, y, C('#b8f8ff'))
    return a


def proj_sake_bottle():
    a = blank()
    rows = [
        '................',
        '......kkkk......',
        '......kWwk......',
        '.......kwk......',
        '.......kWk......',
        '......kWwwk.....',
        '.....kWWwwgk....',
        '....kWWwwwggk...',
        '....kbbbbbbBk...',
        '...kWbWbbbbBgk..',
        '...kWbbbbbbBgk..',
        '...kWWwwwwwggk..',
        '...kWWwwwwgggk..',
        '....kWwwwwggk...',
        '.....kkkkkkk....',
        '................',
    ]
    pal = {'k': C('#2a2a38'), 'W': C('#ffffff'), 'w': C('#e8e8f0'), 'g': C('#b8b8c8'),
           'b': C('#2a5ab0'), 'B': C('#1a3a80')}
    paint_map(a, rows, pal)
    put(a, 8, 1, C('#c08a50'))
    put(a, 7, 1, C('#e0b070'))
    return a


def proj_missile():
    sp = Sprite(S, S)
    D = Diag(0, 16, S, S)
    body = D.band(4.6, 10.6, -2.5, 2.5)
    nose = D.poly([(10.6, 2.5), (12.0, 2.0), (13.6, 0.5), (13.6, -0.5), (12.0, -2.0), (10.6, -2.5)])
    fins = (D.poly([(4.6, 2.5), (7.1, 2.5), (5.1, 5.5), (3.6, 5.5)])
            | D.poly([(4.6, -2.5), (7.1, -2.5), (5.1, -5.5), (3.6, -5.5)]))
    flame = D.poly([(4.6, 2.0), (4.6, -2.0), (2.6, -1.5), (0.6, 0.0), (2.6, 1.5)])
    carbon = ramp('#0c0c12', '#262832', '#3a3e4a', '#5a6070', '#8a92a6')
    mag = ramp('#3a0630', '#a01890', '#f040d0', '#ff8ae8', '#ffd8f8')
    sp.add(flame, ramp('#6a1a04', '#e05010', '#ff9a30', '#ffe080', '#ffffff'), outline=False)
    sp.add(body, carbon)
    sp.add(nose, mag)
    sp.add(fins, mag)
    fl = sp.part_mask(0)
    sp.tone(fl, 2)
    sp.tone(fl & (np.abs(D.V) < 0.6) & (D.U > 2.0), 3)
    sp.tone(fl & (np.abs(D.V) < 0.6) & (D.U > 3.4), 4)
    bd = sp.part_mask(1)
    sp.tone(bd & (np.abs(D.V - 1) < .1), 3)
    a = sp.render()[0]
    cy = C('#30f0ff')
    for u in (6.5, 9.0):           # neon rings
        for v in range(-2, 3):
            x, y = D.px(u, v)
            if a[y, x, 3]:
                put(a, x, y, cy if v != 1 else C('#b8fcff'))
    return a


def proj_glitch_cube():
    a = blank()
    rows = [
        '................',
        '.......kk.......',
        '.....kkTTkk.....',
        '...kkTTttTTkk...',
        '..kTTttttttTTk..',
        '..kMMTTttTTCCk..',
        '..kMMmmTTCCccc..',
        '..kMmmmmkCCcck..',
        '..kMmmmmkCcccck.',
        '..kMmmmmkCccck..',
        '..kMmmmmkCccck..',
        '...kkmmmkCckk...',
        '.....kkmkkk.....',
        '.......kk.......',
        '................',
        '................',
    ]
    pal = {'k': C('#120818'), 'T': C('#ffe8fc'), 't': C('#f6b8ee'), 'M': C('#ff70e4'),
           'm': C('#d82cb8'), 'C': C('#7ef8ff'), 'c': C('#18b4cc')}
    paint_map(a, rows, pal)
    # glitch: displaced scanlines + RGB split pixels
    a[6] = np.roll(a[6], 1, axis=0)
    a[9] = np.roll(a[9], -1, axis=0)
    for (x, y, c) in ((14, 8, '#ff40d0'), (15, 8, '#40f0ff'), (0, 9, '#40f0ff'), (1, 9, '#ff40d0'),
                      (12, 2, '#40f0ff'), (3, 13, '#ff40d0'), (13, 13, '#40f0ff'), (2, 3, '#ff40d0')):
        put(a, x, y, C(c))
    return a


def proj_kinsect():
    a = blank()
    rows = [
        '................',
        '.....k....k.....',
        '......k..k......',
        '......kddk......',
        '.....kdhhdk.....',
        '..kk.kOOOOk.kk..',
        '.kWWkOoOOoOkWWk.',
        '.kWwkOoOOoOkwWk.',
        '..kwkOooooOkwk..',
        '...kkOoOOoOkk...',
        '..k.kOooooOk.k..',
        '.k..kdOooOdk..k.',
        '.....kdOOdk.....',
        '......kddk......',
        '.......kk.......',
        '................',
    ]
    k = MAT['kinsect']
    pal = {'k': C('#24140a'), 'd': k[1], 'o': k[2], 'O': k[3], 'h': C('#3a2a20'),
           'W': C('#e8f8ff'), 'w': C('#b0d8f0')}
    paint_map(a, rows, pal)
    put(a, 7, 6, k[4])
    return a


# ---------------------------------------------------------------------------
def ration():
    sp = Sprite(S, S)
    D = Diag(1, 15, S, S)
    bar = D.band(2.6, 11.6, -3.5, 3.5)
    for (uu, vv) in ((2.6, 3.5), (2.6, -3.5), (11.6, 3.5), (11.6, -3.5)):
        bar &= ~((np.abs(D.U - uu) < 0.6) & (np.abs(D.V - vv) < 1.6))
    wrap = bar & (D.U > 5.4) & (D.U < 8.9)
    meat = ramp('#2e0e08', '#6e2416', '#a0402a', '#c86a44', '#eca070')
    sp.add(bar & ~wrap, meat)
    sp.add(wrap, ramp('#3a2e1c', '#8a7448', '#c2a874', '#e2cf9c', '#fff6e0'))
    tw = wrap & (np.abs(D.U - 7.1) < 0.6)
    sp.tone(tw, 1)
    sp.tone(bar & ~wrap & (np.abs(D.V - 2) < .1), 3)
    sp.tone(bar & ~wrap & (np.abs(D.V - 2) < .1) & (np.abs(D.U - 4.0) < .6), 4)
    sp.tone(wrap & (np.abs(D.V - 2) < .1) & ~tw, 4)
    a = sp.render()[0]
    # fat marbling + twine knot
    for u, v in ((3.5, -1), (10.0, 0), (10.5, -2), (4.5, 1)):
        x, y = D.px(u, v)
        put(a, x, y, C('#f0c8a8'))
    x, y = D.px(7.1, 4.6)
    put(a, x, y, C('#6a5430'))
    return a


def well_done_steak():
    sp = Sprite(S, S)
    D = Diag(0.5, 15.5, S, S)
    bone = D.band(0.6, 14.6, -0.5, 1.5)
    knob1 = disk(*D.P(1.2, 1.6), 1.25, S, S) | disk(*D.P(1.2, -0.6), 1.25, S, S)
    knob2 = disk(*D.P(14.2, 1.6), 1.25, S, S) | disk(*D.P(14.2, -0.6), 1.25, S, S)
    meat = D.poly([(3.6, 3.5), (5.0, 5.5), (10.0, 5.5), (11.6, 3.5), (11.6, -2.5), (10.0, -4.5),
                   (5.0, -4.5), (3.6, -2.5)])
    roast = ramp('#2a0e04', '#6a2a0c', '#a44e18', '#d47e2c', '#f6b860')
    sp.add(bone | knob1 | knob2, 'bone')
    sp.add(meat, roast)
    m = sp.part_mask(1)
    # grill marks
    sp.tone(m & ((np.abs(D.U - 6.0 - D.V * 0.0) < 0.3) | (np.abs(D.U - 8.5) < 0.3)) & (D.V < 3), 1)
    sp.tone(m & (D.V > 3.5), 3)
    sp.tone(m & (np.abs(D.V - 4) < .1) & (D.U > 6) & (D.U < 9), 4)
    a = sp.render()[0]
    x, y = D.px(5.5, 3)
    put(a, x, y, roast[4])
    return a


def whetstone():
    a = blank()
    rows = [
        '................',
        '................',
        '................',
        '.........kkkk...',
        '......kkkLLLLk..',
        '...kkkLLLLhLLLk.',
        '..kLLLLhLLLLLMMk',
        '..kMLLLLLLLMMMDk',
        '.kMMMMMMMMMMDDDk',
        '.kMMmMMMMMMMDDk.',
        '.kDMMMMMmMMDDk..',
        '.kDDDDDDDDDDk...',
        '..kkkkkkkkkk....',
        '................',
        '................',
        '................',
    ]
    pal = {'k': C('#1e2026'), 'L': C('#b8c0c8'), 'h': C('#e8eef2'), 'M': C('#848c98'),
           'm': C('#a0a8b4'), 'D': C('#5a606c')}
    paint_map(a, rows, pal)
    return a


def shock_trap():
    a = blank()
    rows = [
        '................',
        '................',
        '.......y........',
        '......yy..y.....',
        '.....kyykky.....',
        '...kkiyyyIIkk...',
        '..kiIIIyyIIhik..',
        '.kiIIIyyyyyIiik.',
        '.kddIIIIyyIiidk.',
        '.kkddiiiyiiddkk.',
        '.kmkkdddydddkmk.',
        '..kmmkkkkkkmmk..',
        '...kkmmmmmmkk...',
        '.....kkkkkk.....',
        '................',
        '................',
    ]
    ir = MAT['iron']
    pal = {'k': C('#16141c'), 'i': ir[2], 'I': ir[3], 'h': ir[4], 'd': ir[1], 'm': C('#7a6a3a'),
           'y': C('#ffe23a')}
    paint_map(a, rows, pal)
    for (x, y) in ((8, 6), (7, 8), (9, 9)):
        put(a, x, y, C('#fffbd0'))
    return a


def pitfall_trap():
    a = blank()
    rows = [
        '................',
        '................',
        '................',
        '................',
        '....kkkkkkkk....',
        '..kknbbnbbnbkk..',
        '.knbbnnbbnnbbnk.',
        'knbnbbnnbbnbbnbk',
        'kbnnbbnbbnnbnnbk',
        'knbbnnbbnnbbnbnk',
        '.knbnbbnbbnbbnk.',
        '.kkrrrrrrrrrrkk.',
        '..kddddddddddk..',
        '...kkkkkkkkkk...',
        '................',
        '................',
    ]
    pal = {'k': C('#1c140c'), 'b': C('#3a2a1a'), 'n': C('#d2b67a'), 'r': C('#8a6a3c'), 'd': C('#5a3e22')}
    paint_map(a, rows, pal)
    # metal stakes + spring
    for (x, y) in ((2, 5), (13, 5)):
        put(a, x, y - 1, MAT['iron'][3])
        put(a, x, y - 2, MAT['iron'][4])
        put(a, x, y - 3, MAT['iron'][0])
    return a


def barrel_bomb():
    a = blank()
    rows = [
        '..........f.....',
        '.........f.y....',
        '.....kkkfkkk....',
        '...kkRRRfRRRkk..',
        '..kRrRRRRRRRrDk.',
        '..kbbbbbbbbbbbk.',
        '.kRRrRRRRRRRRDDk',
        '.kRrRRwwwwRRRRDk',
        '.kRrRwkwwkwRRRDk',
        '.kRrRwwwwwwRRRDk',
        '.kRrRRwkkwRRRRDk',
        '.kRRrRRRRRRRRDDk',
        '..kbbbbbbbbbbbk.',
        '..kRrRRRRRRRrDk.',
        '...kkRRRRRRRkk..',
        '.....kkkkkkk....',
    ]
    pal = {'k': C('#2a0a0a'), 'R': C('#c82828'), 'r': C('#f05a4a'), 'D': C('#841818'),
           'b': C('#4a3020'), 'w': C('#f4e8d8'), 'f': C('#a08860'), 'y': C('#ffe040')}
    paint_map(a, rows, pal)
    put(a, 12, 0, C('#ff8a20'))
    put(a, 11, 0, C('#ffe040'))
    return a


# ---------------------------------------------------------------------------
# trophies (討伐証): ribbon + medal with series emblem
def medal(face_ramp, rim_ramp, ribbon, emblem, emblem_pal):
    a = blank()
    rib = ['..abca....acba..',
           '...abca..acba...',
           '....abcaacba....',
           '.....abccba.....',
           '......abba......']
    pal = {'a': C('#1a1418'), 'b': ribbon[0], 'c': ribbon[1]}
    paint_map(a, rib, pal)
    sp = Sprite(S, S)
    cx, cy, r = 7.5, 10.0, 5.6
    outer = disk(cx, cy, r, S, S)
    inner = disk(cx, cy, r - 1.3, S, S)
    sp.add(outer & ~inner, rim_ramp)
    sp.add(inner, face_ramp)
    sp.tone(inner, 2)
    sp.tone(inner & disk(cx - 1.2, cy - 1.2, 2.6, S, S), 3)
    rm = outer & ~inner
    ys, xs = np.mgrid[0:S, 0:S]
    sp.tone(rm & (xs + ys < cx + cy - 2), 3)
    sp.tone(rm & (xs + ys > cx + cy + 2), 1)
    m = sp.render()[0]
    alpha = m[..., 3] > 0
    a[alpha] = m[alpha]
    paint_map(a, emblem, emblem_pal, 4, 7)
    put(a, 4, 6, rim_ramp[4])
    return a


GOLDR = MAT['gold']


def trophy_sakeran():
    fur = ramp('#4a4a54', '#b8b8c0', '#e4e4e8', '#f8f8fa', '#ffffff')
    em = ['........', '.k..k..k', '.k..k..k', '..k..k..', '..k..k..', '...k..k.', '........']
    em = ['...k.k.k', '..k.k.k.', '..k.k.k.', '.k.k.k..', '.k.k.k..', '........', '........']
    return medal(fur, ramp('#202028', '#3a3a44', '#5a5a66', '#80808c', '#b0b0bc'),
                 (C('#2a2a34'), C('#e8e8ee')), em, {'k': C('#18181e')})


def trophy_barino():
    pearl = ramp('#5a5060', '#c8c0d4', '#ece6f2', '#fbf6ff', '#ffffff')
    em = ['........', '..gggg..', '.g....g.', '..gggg..', '...pp...', '..pPPp..', '...pp...']
    return medal(pearl, GOLDR, (C('#f4f0ff'), C('#e0b030')), em,
                 {'g': C('#f2c840'), 'p': C('#d8d0e8'), 'P': C('#ffffff')})


def trophy_jangria():
    crim = ramp('#2a060a', '#6a1420', '#98202c', '#c03a40', '#e86a60')
    em = ['.....kk.', '.....kKk', '....kKk.', '...kKKk.', '..kKKk..', '.kkkk...', '........']
    return medal(crim, ramp('#140a0a', '#2a1618', '#40282a', '#5a3a3a', '#7a5450'),
                 (C('#1a1012'), C('#ff7a1a')), em, {'k': C('#120a0c'), 'K': C('#ff8a2a')})


def trophy_busaiber():
    carbon = ramp('#08080c', '#18181e', '#26282e', '#3a3c46', '#5a5e6c')
    em = ['........', '.c.cc.c.', '.cccccc.', 'cc.mm.cc', '.cmmmmc.', '.cccccc.', '.c.cc.c.']
    return medal(carbon, ramp('#2a0a2a', '#7a1a7a', '#c030c0', '#ff60e8', '#ffc8f8'),
                 (C('#30f0ff'), C('#ff40d0')), em, {'c': C('#30f0ff'), 'm': C('#ff60e8')})


def trophy_buskoba():
    straw = ramp('#4a3404', '#a87c18', '#e0b840', '#f4d870', '#fff4b8')
    em = ['...yy...', '..yyyy..', '..rgrg..', 'yyyyyyyy', '.YYYYYY.', '........', '........']
    return medal(ramp('#3a2a1a', '#a88a6a', '#e8cca8', '#f6e2c8', '#fff6ea'), straw,
                 (C('#d03030'), C('#30a040')), em, {'y': C('#f0c840'), 'Y': C('#b08820'),
                                                     'r': C('#d03030'), 'g': C('#30a040')})


ITEMS = {
    'potion': potion, 'mega_potion': mega_potion, 'max_potion': max_potion, 'antidote': antidote,
    'energy_drink': energy_drink, 'demondrug': demondrug, 'armorskin': armorskin,
    'ration': ration, 'well_done_steak': well_done_steak, 'whetstone': whetstone,
    'paintball': paintball, 'flash_bomb': flash_bomb, 'shock_trap': shock_trap,
    'pitfall_trap': pitfall_trap, 'tranq_bomb': tranq_bomb, 'barrel_bomb': barrel_bomb,
    'farcaster': farcaster,
    'proj_bullet': proj_bullet, 'proj_arrow': proj_arrow, 'proj_sake_bottle': proj_sake_bottle,
    'proj_fireball': proj_fireball, 'proj_water_orb': proj_water_orb, 'proj_missile': proj_missile,
    'proj_holy_orb': proj_holy_orb, 'proj_glitch_cube': proj_glitch_cube,
    'proj_kinsect': proj_kinsect, 'proj_shell': proj_shell,
    'trophy_sakeran': trophy_sakeran, 'trophy_barino': trophy_barino,
    'trophy_jangria': trophy_jangria, 'trophy_busaiber': trophy_busaiber,
    'trophy_buskoba': trophy_buskoba,
}


def build():
    out = {}
    for k, fn in ITEMS.items():
        a = fn()
        if isinstance(a, tuple):
            a = a[0]
        assert a.shape == (S, S, 4), k
        out[k] = a
    return out


def write(out):
    for k, a in out.items():
        save(a, f'item/{k}.png')


if __name__ == '__main__':
    write(build())
