"""16x16 block textures (vanilla-consistent, tile-friendly edges)."""
import numpy as np

from pixlib import C, MAT, ramp, save

S = 16

OAK = ramp('#2e1d0e', '#6a4826', '#94683a', '#b48650', '#d0a86c')
DOAK = ramp('#1c120a', '#3e2a16', '#5a3e22', '#74522e', '#8e683c')
BLUEWOOD = ramp('#141c26', '#3a4c60', '#56708a', '#7490aa', '#9cb6cc')
REDWOOD = ramp('#1e0608', '#4e1216', '#721c22', '#962a2e', '#b84440')
SUPPLY = ramp('#081a40', '#1a4ca0', '#2a72d8', '#4c98f4', '#9ccaff')
STONE = ramp('#26262a', '#5a5a62', '#7c7c84', '#9a9aa2', '#b8b8c0')
IRON = MAT['iron']
GOLD = MAT['gold']
STRAW = ramp('#4a3606', '#9a7a1e', '#c8a238', '#e2c25a', '#f6e08e')
PAPER = ramp('#6a5a40', '#b8a888', '#d8cca8', '#ece2c4', '#fbf6e6')


def blank():
    return np.zeros((S, S, 4), np.uint8)


def put(a, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        a[y, x] = c if len(c) == 4 else tuple(c) + (255,)


def rect(a, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(a, x, y, c)


def hsh(x, y, s=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (s * 83492791)) & 0xffff


def planks(r, seed=1, plank_h=4, joints=(5, 12, 2, 9)):
    """Vanilla-like horizontal planks: dark seam row, staggered end joints, grain streaks."""
    rng = np.random.default_rng(seed)
    a = blank()
    for k in range(S // plank_h):
        y0 = k * plank_h
        jx = joints[k % len(joints)]
        grains = [(int(rng.integers(y0, y0 + plank_h - 1)), int(rng.integers(0, 16)), int(rng.integers(3, 7)),
                   int(rng.choice([1, 3]))) for _ in range(3)]
        for y in range(y0, y0 + plank_h):
            for x in range(S):
                t = 2
                if y == y0 + plank_h - 1:
                    t = 1
                elif y == y0:
                    t = 3 if x % 7 else 2
                for gy, gx, gl, gt in grains:
                    if y == gy and y != y0 + plank_h - 1 and (x - gx) % 16 < gl:
                        t = gt
                if x == jx and y != y0 + plank_h - 1:
                    t = 1
                elif x == (jx + 1) % 16 and y != y0 + plank_h - 1 and y != y0:
                    t = 3
                put(a, x, y, r[t])
    return a


def bricks(r, seed=3, mortar=None):
    a = blank()
    mortar = mortar or r[1]
    for row in range(4):
        y0 = row * 4
        off = 0 if row % 2 == 0 else 4
        for y in range(y0, y0 + 4):
            for x in range(S):
                xx = (x + off) % 8
                if y == y0 + 3 or xx == 7:
                    put(a, x, y, mortar)
                    continue
                t = 2
                if y == y0 or xx == 0:
                    t = 3
                if y == y0 + 2 and xx == 6:
                    t = 1
                n = hsh(x, y, seed) % 13
                if n == 0:
                    t = max(1, t - 1)
                elif n == 1:
                    t = min(4, t + 1)
                put(a, x, y, r[t])
    return a


def straw_fill(a, x0, y0, x1, y1, r=STRAW, seed=7):
    rng = np.random.default_rng(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(a, x, y, r[2])
    for _ in range(int((x1 - x0 + 1) * (y1 - y0 + 1) / 6)):
        x = int(rng.integers(x0, x1 + 1))
        y = int(rng.integers(y0, y1 + 1))
        t = int(rng.choice([1, 3, 3, 4]))
        ln = int(rng.integers(2, 4))
        for k in range(ln):
            if x0 <= x + k // 2 <= x1 and y0 <= y + k <= y1:
                put(a, x + k // 2, y + k, r[t])
    return a


# ---------------------------------------------------------------------------
def quest_board_side():
    return planks(OAK, seed=11)


def quest_board_front():
    a = planks(OAK, seed=12)
    # dark frame
    for i in range(S):
        put(a, i, 0, DOAK[2])
        put(a, i, 15, DOAK[1])
        put(a, 0, i, DOAK[2])
        put(a, 15, i, DOAK[1])
    put(a, 0, 0, DOAK[3])
    # pinned papers: (x0, y0, x1, y1, pin colour)
    papers = [(2, 2, 6, 7, C('#d02a2a')), (8, 2, 13, 6, C('#2a6ad0')), (2, 9, 7, 13, C('#2aa04a')),
              (9, 8, 13, 13, C('#e0b020'))]
    for k, (x0, y0, x1, y1, pin) in enumerate(papers):
        rect(a, x0 + 1, y0 + 1, x1 + 1, y1 + 1, DOAK[1])      # drop shadow
        rect(a, x0, y0, x1, y1, PAPER[3])
        for x in range(x0, x1 + 1):
            put(a, x, y1, PAPER[2])
        for y in range(y0, y1 + 1):
            put(a, x1, y, PAPER[2])
        put(a, x0, y0, PAPER[4])
        # text lines
        for y in range(y0 + 2, y1, 2):
            for x in range(x0 + 1, x1):
                if (x + y + k) % 5 != 0:
                    put(a, x, y, PAPER[1])
        put(a, (x0 + x1) // 2, y0, pin)
    # monster sketch on one notice (red circle = target)
    for (x, y) in ((10, 10), (11, 10), (9, 11), (12, 11), (10, 12), (11, 12)):
        put(a, x, y, C('#b02020'))
    return a


def canteen_top():
    a = blank()
    # stone rim (cobbled) around
    for y in range(S):
        for x in range(S):
            n = hsh(x // 2, y // 2, 5) % 4
            t = [1, 2, 2, 3][n]
            if (x % 4 == 3 and y % 2 == 0) or (y % 4 == 3):
                t = 1
            put(a, x, y, STONE[t])
    # pot
    cx, cy = 7.5, 7.5
    stew = ramp('#3a1606', '#7a3410', '#a8521c', '#c8762e', '#e8a85a')
    for y in range(S):
        for x in range(S):
            d = ((x + .5 - cx) ** 2 + (y + .5 - cy) ** 2) ** .5
            if d <= 7.0:
                if d > 5.9:
                    t = 3 if (x + y) < 13 else (1 if (x + y) > 17 else 2)
                    put(a, x, y, IRON[t] if d <= 6.6 else IRON[0])
                elif d > 5.2:
                    put(a, x, y, IRON[1] if (x + y) < 15 else IRON[2])
                else:
                    t = 2
                    n = hsh(x, y, 9) % 7
                    if n == 0:
                        t = 3
                    elif n == 1:
                        t = 1
                    put(a, x, y, stew[t])
    # chunks: meat, carrot, herb, bubbles
    for (x, y) in ((5, 6), (6, 6), (9, 9), (10, 9)):
        put(a, x, y, C('#6a2a18'))
    for (x, y) in ((9, 5), (5, 10), (6, 10)):
        put(a, x, y, C('#f08a20'))
    for (x, y) in ((7, 9), (10, 7), (4, 8)):
        put(a, x, y, C('#4aa040'))
    for (x, y) in ((7, 5), (8, 11), (11, 10)):
        put(a, x, y, stew[4])
    put(a, 6, 4, C('#fff0d0'))
    return a


def canteen_side():
    a = bricks(STONE, seed=21, mortar=STONE[0])
    # fire opening (arched) at the bottom centre
    open_rows = {9: (6, 9), 10: (5, 10), 11: (4, 11), 12: (4, 11), 13: (4, 11), 14: (4, 11)}
    for y, (x0, x1) in open_rows.items():
        for x in range(x0, x1 + 1):
            put(a, x, y, C('#140806'))
        put(a, x0 - 1, y, STONE[4] if y > 10 else STONE[3])
        put(a, x1 + 1, y, STONE[1])
    for x in range(5, 11):
        put(a, x, 8, STONE[3])
    flame = [
        '........',
        '...y....',
        '..yo..y.',
        '..oOy.o.',
        '.oOwOyOo',
        'rOwwwOOr',
    ]
    pal = {'y': C('#ffd040'), 'o': C('#ff8a1a'), 'O': C('#ff6a10'), 'w': C('#fff4b0'), 'r': C('#b02a08')}
    for yy, row in enumerate(flame):
        for xx, ch in enumerate(row):
            if ch != '.':
                put(a, 4 + xx, 9 + yy, pal[ch])
    # logs + ember floor
    for x in range(4, 12):
        put(a, x, 15, C('#5a3418') if x % 3 else C('#ff8a20'))
    for x in range(S):
        put(a, x, 15, STONE[1] if x < 3 or x > 12 else a[15, x])
    return a


def chest_side(wood, band, lock, emblem=None, seed=31):
    a = planks(wood, seed=seed)
    # lid line + bands
    for x in range(S):
        put(a, x, 0, band[3])
        put(a, x, 5, band[1])
        put(a, x, 6, band[2])
        put(a, x, 15, band[1])
    for y in range(S):
        for x in (1, 14):
            put(a, x, y, band[2] if x == 1 else band[1])
        put(a, 0, y, wood[1])
        put(a, 15, y, wood[0])
    for x in (1, 14):
        for y in (2, 9, 13):
            put(a, x, y, band[4])
    # lock plate
    rect(a, 6, 4, 9, 8, lock[2])
    for x in range(6, 10):
        put(a, x, 4, lock[3])
        put(a, x, 8, lock[1])
    put(a, 6, 4, lock[4])
    put(a, 7, 6, lock[0])
    put(a, 8, 6, lock[0])
    put(a, 7, 7, lock[0])
    if emblem:
        emblem(a)
    return a


def chest_top(wood, band, seed=41, emblem=None):
    a = planks(wood, seed=seed, joints=(3, 10, 6, 13))
    for i in range(S):
        put(a, i, 0, band[2])
        put(a, i, 15, band[1])
        put(a, 0, i, band[2])
        put(a, 15, i, band[1])
    for y in range(S):
        put(a, 4, y, band[2])
        put(a, 11, y, band[2])
        put(a, 3, y, wood[1])
        put(a, 12, y, wood[3])
    for x in (4, 11):
        for y in (2, 7, 13):
            put(a, x, y, band[4])
    put(a, 0, 0, band[4])
    if emblem:
        emblem(a)
    return a


def item_box_side():
    return chest_side(BLUEWOOD, IRON, IRON, seed=31)


def item_box_top():
    return chest_top(BLUEWOOD, IRON, seed=41)


CROSSED = [          # crossed swords, 8x8: G blade light, g blade, q guard, h grip
    'G......G',
    '.G....G.',
    '..G..G..',
    '...GG...',
    '.q.gg.q.',
    '..g..g..',
    '.h.qq.h.',
    'h......h',
]


def _weapon_emblem(a, ox=5, oy=8, rows=CROSSED):
    """Crossed swords emblem in gold."""
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == 'G':
                put(a, ox + x, oy + y, GOLD[4])
            elif ch == 'g':
                put(a, ox + x, oy + y, GOLD[3])
            elif ch == 'q':
                put(a, ox + x, oy + y, GOLD[2])
            elif ch == 'h':
                put(a, ox + x, oy + y, DOAK[3])


def equipment_box_side():
    def em(a):
        rect(a, 3, 8, 12, 14, REDWOOD[0])
        rect(a, 4, 8, 11, 14, REDWOOD[1])
        _weapon_emblem(a, 4, 8, CROSSED[1:])
    return chest_side(REDWOOD, GOLD, GOLD, emblem=em, seed=33)


def equipment_box_top():
    def em(a):
        rect(a, 4, 3, 11, 12, REDWOOD[0])
        rect(a, 5, 4, 10, 11, REDWOOD[1])
        _weapon_emblem(a, 4, 4)
    return chest_top(REDWOOD, GOLD, seed=43, emblem=em)


def departure_gate():
    a = blank()
    red = ramp('#2a0406', '#7a0c12', '#b0141c', '#d8282c', '#ff6a5a')
    for y in range(S):
        for x in range(S):
            t = 2
            if x in (1, 2):
                t = 3
            if x in (13, 14):
                t = 1
            if (x + y * 3) % 9 == 0 and 3 <= x <= 12:
                t = 3        # cloth weave sheen
            put(a, x, y, red[t])
        put(a, 0, y, red[1])
        put(a, 15, y, red[0])
    # gold trims top/bottom + fringe
    for x in range(S):
        put(a, x, 0, GOLD[3])
        put(a, x, 1, GOLD[2])
        put(a, x, 14, GOLD[2])
        put(a, x, 15, GOLD[1] if x % 2 else GOLD[3])
        put(a, x, 2, red[0])
        put(a, x, 13, red[0])
    # glowing gold emblem: horned crest with crossed blade
    em = [
        'G........G',
        'gG......Gg',
        '.gG.ww.Gg.',
        '..gGwwGg..',
        '...gWWg...',
        '..gGwwGg..',
        '.gG.ww.Gg.',
        '....ww....',
        '...gGGg...',
    ]
    pal = {'G': GOLD[3], 'g': GOLD[2], 'w': C('#fff6c0'), 'W': C('#ffffff')}
    for y, row in enumerate(em):
        for x, ch in enumerate(row):
            if ch != '.':
                put(a, 3 + x, 3 + y, pal[ch])
    # glow halo pixels around emblem
    for (x, y) in ((7, 3), (8, 3), (3, 8), (12, 8)):
        put(a, x, y, red[4])
    return a


def supply_box_side():
    a = planks(SUPPLY, seed=51)
    frame = ramp('#0c2050', '#2a5ab0', '#6aa8ff', '#a8d0ff', '#e0f0ff')
    for i in range(S):
        put(a, i, 0, frame[3])
        put(a, i, 1, frame[2])
        put(a, i, 14, frame[1])
        put(a, i, 15, frame[0])
        put(a, 0, i, frame[3])
        put(a, 1, i, frame[2])
        put(a, 14, i, frame[1])
        put(a, 15, i, frame[0])
    # diagonal brace
    for k in range(2, 14):
        put(a, k, k, frame[2])
        put(a, k + 1, k, frame[3])
        put(a, k - 1, k, frame[1]) if k > 2 else None
    # white supply stencil (cross) in a corner
    for (x, y) in ((11, 3), (10, 4), (11, 4), (12, 4), (11, 5)):
        put(a, x, y, C('#ffffff'))
    for (x, y) in ((4, 10), (3, 11), (4, 11), (5, 11), (4, 12)):
        put(a, x, y, C('#ffffff'))
    return a


def supply_box_top():
    a = planks(SUPPLY, seed=52, joints=(4, 11, 7, 1))
    frame = ramp('#0c2050', '#2a5ab0', '#6aa8ff', '#a8d0ff', '#e0f0ff')
    for i in range(S):
        put(a, i, 0, frame[3])
        put(a, i, 15, frame[0])
        put(a, 0, i, frame[3])
        put(a, 15, i, frame[0])
        put(a, i, 1, frame[2])
        put(a, 14, i, frame[1])
    # big white/yellow supply mark
    mark = ['...ww...', '...ww...', '.wwyywww'[:8], 'wwyyyyww', 'wwyyyyww', '.wwyyww.', '...ww...', '...ww...']
    mark = ['...ww...', '...ww...', '..wyyw..', 'wwyyyyww', 'wwyyyyww', '..wyyw..', '...ww...', '...ww...']
    for y, row in enumerate(mark):
        for x, ch in enumerate(row):
            if ch == 'w':
                put(a, 4 + x, 4 + y, C('#ffffff'))
            elif ch == 'y':
                put(a, 4 + x, 4 + y, C('#ffd23a'))
    return a


def hunter_bed_top():
    a = blank()
    straw_fill(a, 0, 0, 15, 15, seed=61)
    # rolled blanket across the foot end + pillow at the head end
    blanket = ramp('#2a0c08', '#6a2418', '#9a3a24', '#c05a34', '#e08a5a')
    for y in range(9, 15):
        for x in range(1, 15):
            t = 2
            if y == 9:
                t = 3
            if y == 14:
                t = 1
            if (x + y) % 4 == 0 and 10 <= y <= 13:
                t = 1     # plaid lines
            if x in (4, 11) and 10 <= y <= 13:
                t = 3
            put(a, x, y, blanket[t])
    for y in range(9, 15):
        put(a, 0, y, blanket[0])
        put(a, 15, y, blanket[0])
    pillow = ramp('#5a4a30', '#b4a27a', '#d8c8a0', '#ece2c4', '#fbf6e6')
    for y in range(2, 6):
        for x in range(3, 13):
            t = 3 if y == 2 else (1 if y == 5 else 2)
            put(a, x, y, pillow[t])
    put(a, 3, 2, pillow[2])
    put(a, 12, 2, pillow[2])
    put(a, 4, 3, pillow[4])
    for x in range(S):
        put(a, x, 15, STRAW[1])
        put(a, x, 0, STRAW[3])
    return a


def hunter_bed_side():
    a = blank()
    straw_fill(a, 0, 0, 15, 8, seed=62)
    # straw bundle ties
    for y in range(0, 9):
        put(a, 4, y, C('#6a4a1a'))
        put(a, 11, y, C('#6a4a1a'))
    # blanket edge hanging over
    blanket = ramp('#2a0c08', '#6a2418', '#9a3a24', '#c05a34', '#e08a5a')
    for x in range(S):
        put(a, x, 9, blanket[3])
        put(a, x, 10, blanket[2] if x % 4 else blanket[1])
    # low wooden frame
    for y in range(11, 16):
        for x in range(S):
            t = 2 if y in (12, 13) else (3 if y == 11 else 1)
            put(a, x, y, OAK[t])
    for y in range(11, 16):
        put(a, 0, y, OAK[1])
        put(a, 15, y, OAK[1])
        put(a, 7, y, OAK[1])
    for x in (1, 14):
        put(a, x, 13, IRON[3])
    return a


BLOCKS = {
    'quest_board_front': quest_board_front, 'quest_board_side': quest_board_side,
    'canteen_top': canteen_top, 'canteen_side': canteen_side,
    'item_box_top': item_box_top, 'item_box_side': item_box_side,
    'equipment_box_top': equipment_box_top, 'equipment_box_side': equipment_box_side,
    'departure_gate': departure_gate, 'supply_box_top': supply_box_top, 'supply_box_side': supply_box_side,
    'hunter_bed_top': hunter_bed_top, 'hunter_bed_side': hunter_bed_side,
}


def build():
    out = {k: f() for k, f in BLOCKS.items()}
    for k, a in out.items():
        assert a.shape == (S, S, 4) and (a[..., 3] == 255).all(), k   # blocks are fully opaque
    return out


def write(out):
    for k, a in out.items():
        save(a, f'block/{k}.png')


if __name__ == '__main__':
    write(build())
