"""32x32 two-layer weapon textures (base + grayscale tint overlay).

item/<type>.png          untinted parts (grip, guard, frame, wood ...)
item/<type>_overlay.png  element-tinted parts in grayscale (multiplied by tint)
"""
import math

import numpy as np

from pixlib import (MAT, Diag, Sprite, capsule, disk, ellipse, poly_mask,
                    pts_mask, rect, save)

W = H = 32


def diag():
    return Diag(1, 31)


def wrap_tone(sp, D, mask, u0, step=1.0, light=3, dark=1):
    """Alternating grip-wrap stripes across the axis."""
    U = D.U
    a = mask & (np.floor((U - u0) / step) % 2 == 0)
    b = mask & (np.floor((U - u0) / step) % 2 == 1)
    sp.tone(a & (D.V >= 0), light)
    sp.tone(a & (D.V < 0), 2)
    sp.tone(b, dark)


def V_line(D, v, u0, u1):
    return (np.abs(D.V - v) < 0.1) & (D.U >= u0) & (D.U <= u1)


# ---------------------------------------------------------------------------
def great_sword():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    blade = D.poly([(9.6, 6.5), (25.6, 6.5), (29.6, 0.5), (29.6, -0.5),
                    (23.6, -6.5), (9.6, -6.5)])
    # bite-shaped notch on the spine near the guard (monster-fang look)
    blade &= ~D.band(11.6, 12.6, -6.5, -5.5)
    grip = D.band(2.4, 7.1, -1.5, 1.5)
    pommel = D.band(0.4, 2.4, -2.5, 2.5) & ~D.band(0.4, 0.9, -2.5, -1.5) & ~D.band(0.4, 0.9, 1.5, 2.5)
    guard = D.poly([(7.1, 8.5), (8.1, 9.5), (9.6, 9.5), (9.6, -9.5), (8.1, -9.5), (7.1, -8.5)])
    guard &= ~(D.band(9.0, 9.6, 8.5, 9.5) | D.band(9.0, 9.6, -9.5, -8.5))
    gem = D.band(7.6, 9.1, -1.5, 1.5)
    sp.add(blade, 'steel', tinted=True)
    sp.add(grip, 'leather')
    sp.add(pommel, 'gold')
    sp.add(guard, 'iron')
    sp.add(gem, 'gold')
    wrap_tone(sp, D, grip, 2.4)
    # blade bevels: bright cutting edge on the upper-left, dark spine bevel
    sp.tone(blade & (np.abs(V - 5) < .1) & (U < 25), 4)
    sp.tone(blade & (np.abs(V - 6) < .1), 3)
    sp.tone(blade & (np.abs(V + 5) < .1), 1)
    sp.tone(blade & (np.abs(V + 6) < .1), 1)
    # fuller groove down the middle
    sp.tone(blade & V_line(D, 0, 11, 21), 1)
    sp.tone(blade & V_line(D, 1, 11, 21), 3)
    gd = sp.part_mask(3)
    sp.tone(gd & (U < 7.9), 3)
    sp.tone(gd & (np.abs(V) > 7.6), 2)
    sp.tone(gd & (V < -2) & (U > 8.4), 1)
    sp.tone_px([D.px(8.0, 7), D.px(8.0, 6)], 4)
    g = sp.part_mask(4)
    sp.tone(g & (V > 0.5), 3)
    sp.tone_px([D.px(8.0, 1)], 4)
    # tip glint
    sp.tone(blade & (np.abs(U - 27.5) < .6) & (np.abs(V - 3) < .6), 4)
    return sp


def long_sword():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    u0, u1 = 11.0, 28.6
    t = np.clip((U - u0) / (u1 - u0), 0, 1)
    c = np.round(3.0 * t ** 1.7)            # quantised curve -> clean pixel steps
    lo = c - 1.5
    hi = c + 1.5
    hi = np.where(U > u1 - 3.2, c + 0.5, hi)  # kissaki: spine drops toward the tip
    lo = np.where(U > u1 - 1.2, c - 0.5, lo)
    blade = (U >= u0) & (U <= u1) & (V >= lo) & (V <= hi)
    grip = D.band(1.4, 8.6, -1.5, 1.5)
    kashira = D.band(0.4, 1.4, -1.5, 1.5)
    X = D.P(9.7, 0)
    tsuba = D.poly([(8.7, 3.0), (9.2, 4.6), (10.2, 4.6), (10.7, 3.0), (10.7, -3.0),
                    (10.2, -4.6), (9.2, -4.6), (8.7, -3.0)])
    habaki = D.band(10.6, 11.6, -1.5, 1.5)
    sp.add(blade, 'steel', tinted=True)
    sp.add(grip, 'wrap_blue')
    sp.add(kashira, 'gold')
    sp.add(tsuba, 'gold')
    sp.add(habaki, 'gold')
    # diamond wrap pattern (tsuka-ito over pale same-gawa)
    for y in range(H):
        for x in range(W):
            if grip[y, x]:
                u, v = U[y, x], V[y, x]
                k = (round(u * 2) + round(v)) % 4
                if v == 0 and k in (0, 2):
                    sp.put(x, y, MAT['cloth'][3])
                elif k == 1:
                    sp.tone_px([(x, y)], 3)
                elif k == 3:
                    sp.tone_px([(x, y)], 1)
    # blade: spine light, hamon (temper line) bright near the edge
    sp.tone(blade & (V >= c + 0.5), 3)
    sp.tone(blade & (np.abs(V - c) < 0.1), 2)
    sp.tone(blade & (V <= c - 0.5), 4)
    sp.tone(blade & (V <= c - 0.5) & ((np.floor(U) % 5) == 0), 3)
    sp.tone(blade & (U > u1 - 3.2) & (V >= c - 0.5), 3)
    # tsuba: lit face toward the grip, dark rim toward the blade
    ts = sp.part_mask(3)
    sp.tone(ts & (U < 9.6), 3)
    sp.tone(ts & (U < 9.6) & (V > 1.5) & (V < 3.5), 4)
    sp.tone(ts & (U >= 9.6), 1)
    sp.tone(ts & (np.abs(V) > 3.6), 2)
    return sp


def hammer():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    handle = D.band(0.4, 18.6, -1.5, 1.5)
    cap = D.band(0.4, 1.6, -2.5, 2.5)
    grip = D.band(2.4, 9.6, -1.5, 1.5)
    hu0, hu1, hv = 18.6, 26.1, 9.5
    head = D.band(hu0, hu1, -hv, hv)
    head &= ~(D.band(hu0, hu0 + 0.9, hv - 1, hv) | D.band(hu1 - 0.9, hu1, hv - 1, hv)
              | D.band(hu0, hu0 + 0.9, -hv, -hv + 1) | D.band(hu1 - 0.9, hu1, -hv, -hv + 1))
    endA = head & (V >= hv - 1.6)
    endB = head & (V <= -hv + 1.6)
    socket = D.band(16.6, 18.6, -3.5, 3.5) & ~D.band(16.6, 17.1, 2.5, 3.5) & ~D.band(16.6, 17.1, -3.5, -2.5)
    sp.add(handle, 'darkwood')
    sp.add(grip, 'leather')
    sp.add(cap, 'iron')
    body = head & ~endA & ~endB
    sp.add(body, 'steel', tinted=True)
    sp.add(endA | endB, 'iron')
    sp.add(socket, 'gold')
    wrap_tone(sp, D, grip, 2.4)
    # raised centre panel with a bevel: lit upper-left rim, shaded lower-right
    sp.tone(body & (U <= hu0 + 0.9), 3)
    sp.tone(body & (U >= hu1 - 0.9), 1)
    sp.tone(body & (np.abs(V - (hv - 2)) < 0.1), 3)
    sp.tone(body & (np.abs(V + (hv - 2)) < 0.1), 1)
    panel = body & (U > hu0 + 1.4) & (U < hu1 - 1.4) & (np.abs(V) < hv - 3.4)
    sp.tone(panel, 2)
    sp.tone(panel & (U < hu0 + 2.2), 1)
    sp.tone(panel & (U > hu1 - 2.2), 3)
    # emblem: diamond stud in the centre
    sp.tone(body & (np.abs(U - 22.35) + np.abs(V) / 2 < 1.1), 4)
    sp.tone(body & (np.abs(U - 22.35) + np.abs(V) / 2 < 1.1) & (V < 0), 1)
    sp.tone(body & (np.abs(U - (hu0 + 0.5)) < 0.6) & (V > 2) & (V < 7), 4)
    # striking faces: lit (upper-left) and shaded (lower-right) ends
    ea, eb = sp.part_mask(4) & endA, sp.part_mask(4) & endB
    sp.tone(ea, 3)
    sp.tone(ea & (V >= hv - 0.6), 4)
    sp.tone(eb, 1)
    sp.tone(eb & (V >= -hv + 1.4), 2)
    return sp


def gunlance():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    grip = D.band(1.4, 6.6, -1.5, 1.5)
    pommel = D.band(0.4, 1.4, -2.5, 2.5)
    drum = D.band(6.6, 10.6, -4.5, 4.5) & ~D.band(6.6, 7.1, 3.5, 4.5) & ~D.band(6.6, 7.1, -4.5, -3.5)
    top = np.where(U < 21.0, 5.5, 5.5 - (U - 21.0) / (29.6 - 21.0) * 5.2)
    bot = np.where(U < 22.5, -2.5, -2.5 + (U - 22.5) / (29.6 - 22.5) * 2.4)
    lance = (U >= 10.6) & (U <= 29.6) & (V <= top + 0.01) & (V >= bot - 0.01)
    barrel = D.band(10.6, 21.6, -5.5, -2.5)
    muzzle = D.band(21.6, 22.9, -6.5, -1.5)
    sc = D.P(8.6, -9.5)
    shield = disk(sc[0], sc[1], 5.0)
    sp.add(shield, 'iron')
    sp.add(grip, 'leather', sep=True)
    sp.add(pommel, 'gold', sep=True)
    sp.add(drum, 'steel', sep=True)
    sp.add(barrel, 'dark', sep=True)
    sp.add(muzzle, 'iron', sep=True)
    sp.add(lance, 'steel', tinted=True)
    wrap_tone(sp, D, grip, 1.4)
    sh = sp.part_mask(0)
    rim = sh & ~disk(sc[0], sc[1], 3.9)
    sp.tone(sh, 2)
    for y in range(H):
        for x in range(W):
            if rim[y, x]:
                g = MAT['brass']
                d = (x + y) - (sc[0] + sc[1]) + 1
                sp.put(x, y, g[3] if d < -1 else (g[1] if d > 1 else g[2]))
    sp.tone(rim & (np.mgrid[0:32, 0:32][1] + np.mgrid[0:32, 0:32][0] < sc[0] + sc[1] - 1), 3)
    sp.tone(rim & (np.mgrid[0:32, 0:32][1] + np.mgrid[0:32, 0:32][0] > sc[0] + sc[1] + 1), 1)
    xi, yi = int(sc[0]), int(sc[1])
    sp.tone_px([(xi, yi), (xi - 1, yi), (xi, yi - 1)], 3)
    sp.tone_px([(xi - 1, yi - 1)], 4)
    sp.tone_px([(xi + 1, yi), (xi, yi + 1)], 1)
    # revolver drum chambers
    dr = sp.part_mask(3)
    for u in (7.5, 9.5):
        for v in (-2, 1, 3):
            x, y = D.px(u, v)
            if dr[y, x]:
                sp.tone_px([(x, y)], 0)
    sp.tone(dr & (np.abs(V - 4) < .1), 4)
    br = sp.part_mask(4)
    sp.tone(br & (np.abs(V + 3) < 0.1), 3)
    # lance: bevelled edge, central ridge
    ln = sp.part_mask(6)
    sp.tone(ln & (np.abs(V - 4) < .1) & (U < 23), 4)
    sp.tone(ln & (np.abs(V - 1) < .1) & (U < 27), 3)
    sp.tone(ln & (np.abs(V) < .1) & (U < 27), 1)
    sp.tone(ln & (V < -0.5), 1)
    x, y = D.px(22.5, -4)
    sp.put(x, y, MAT['dark'][0])
    x, y = D.px(22.0, -3)
    sp.put(x, y, MAT['dark'][0])
    return sp


def lance():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    grip = D.band(1.4, 6.6, -1.5, 1.5)
    pommel = D.band(0.4, 1.4, -2.5, 2.5)
    # vamplate cone: wide face toward the hand, tapering toward the tip
    g0, g1 = 6.6, 11.6
    tg = (U - g0) / (g1 - g0)
    guard = (U >= g0) & (U <= g1) & (np.abs(V) <= 9.5 - 5.0 * tg + 0.01)
    t = (U - g1) / (29.6 - g1)
    cone = (U >= g1) & (U <= 29.6) & (np.abs(V) <= 4.5 - 4.5 * t + 0.01)
    bands = cone & ((np.abs(U - 16.0) < 0.6) | (np.abs(U - 21.5) < 0.6))
    sp.add(grip, 'leather')
    sp.add(pommel, 'iron')
    sp.add(guard, 'steel')
    sp.add(cone & ~bands, 'steel', tinted=True)
    sp.add(bands, 'gold')
    wrap_tone(sp, D, grip, 1.4)
    gd = sp.part_mask(2)
    sp.tone(gd & (U < g0 + 0.9), 1)          # the hand-side rim
    sp.tone(gd & (U < g0 + 0.9) & (V > 3), 2)
    sp.tone(gd & (U >= g0 + 0.9) & (V > 0.5), 3)
    sp.tone(gd & (np.abs(V - 4) < 0.1) & (U > 7.5), 4)
    sp.tone(gd & (U >= g0 + 0.9) & (V < -2.5), 1)
    sp.tone(gd & (U >= g0 + 0.9) & (np.abs(V) < 2.6) & (V < 0.5), 2)
    cn = sp.part_mask(3)
    sp.tone(cn & (V > 0.5), 3)
    sp.tone(cn & (np.abs(V - 2) < .1) & (U < 24), 4)
    sp.tone(cn & (np.abs(V) < .1), 2)
    sp.tone(cn & (V < -0.5), 2)
    sp.tone(cn & (V < -2.5), 1)
    sp.tone(cn & (np.abs(V + 1) < .1) & (U > 22), 1)
    return sp


def light_bowgun():
    sp = Sprite()
    stock = poly_mask([(3, 14), (11, 14), (11, 18), (8, 18), (5, 21), (3, 21)])
    body = rect(10, 12, 19, 17)
    jacket = rect(16, 12, 26, 15)
    muzzle = rect(27, 11, 28, 16)
    grip = poly_mask([(12, 18), (15, 18), (14, 23), (11, 23)])
    scope = rect(12, 10, 17, 11)
    tguard = pts_mask([(15, 18), (16, 19), (16, 20), (15, 21)])
    # small bow limbs swept back from the front of the barrel
    limbs = (poly_mask([(22, 12), (25, 12), (22, 7), (19, 5), (18, 6), (20, 8)])
             | poly_mask([(22, 16), (25, 16), (22, 21), (19, 23), (18, 22), (20, 20)]))
    from pixlib import line_pts
    string = pts_mask(line_pts(18, 6, 15, 11) + line_pts(18, 22, 15, 18))
    sp.add(stock, 'wood')
    sp.add(grip, 'leather')
    sp.add(tguard, 'iron', shade='flatdark')
    sp.add(string, 'string', shade='flat', outline=False)
    sp.add(body, 'iron')
    sp.add(scope, 'dark')
    sp.add(limbs, 'steel', tinted=True)
    sp.add(jacket, 'steel', tinted=True)
    sp.add(muzzle, 'iron')
    jk = sp.part_mask(7)
    sp.tone(jk & rect(0, 12, 31, 12), 3)
    sp.tone(jk & rect(0, 13, 31, 13), 4)
    sp.tone(jk & rect(0, 15, 31, 15), 1)
    for x in (18, 21, 24):          # vent slots
        sp.tone(jk & rect(x, 14, x, 14), 0)
    lm = sp.part_mask(6)
    sp.tone_px([(21, 8), (20, 7)], 4)
    sp.tone_px([(21, 20), (20, 21)], 1)
    for y in (12, 13, 14, 15):
        sp.put(28, y, MAT['dark'][0])
    sp.put(27, 12, MAT['iron'][3])
    bd = sp.part_mask(4)
    sp.tone(bd & rect(10, 16, 19, 17), 1)
    sp.tone_px([(11, 13), (12, 13)], 4)
    sp.put(16, 10, (120, 220, 255, 255))   # scope lens
    sp.put(17, 10, (60, 140, 200, 255))
    st = sp.part_mask(0)
    sp.tone(st & rect(3, 14, 4, 21), 1)
    sp.tone(st & rect(5, 15, 10, 15), 3)
    return sp


def heavy_bowgun():
    sp = Sprite()
    stock = poly_mask([(1, 12), (8, 12), (8, 19), (5, 19), (3, 23), (1, 23)])
    body = rect(7, 9, 17, 19)
    barrel = rect(16, 9, 27, 15)
    rings = rect(19, 8, 19, 16) | rect(24, 8, 24, 16)
    brake = rect(27, 7, 30, 17)
    plate = poly_mask([(16, 16), (27, 16), (27, 18), (24, 21), (16, 21)])
    grip = poly_mask([(13, 20), (16, 20), (15, 25), (12, 25)])
    scope = rect(9, 6, 15, 8)
    drum = disk(11.5, 15.0, 3.3)
    sp.add(stock, 'darkwood')
    sp.add(grip, 'leather')
    sp.add(body, 'iron')
    sp.add(scope, 'dark')
    sp.add(barrel & ~rings, 'steel', tinted=True)
    sp.add(plate, 'steel', tinted=True)
    sp.add(rings, 'iron')
    sp.add(brake, 'dark')
    sp.add(drum, 'brass', sep=True)
    bl = sp.part_mask(4)
    sp.tone(bl & rect(0, 9, 31, 9), 3)
    sp.tone(bl & rect(0, 10, 31, 10), 4)
    sp.tone(bl & rect(0, 11, 31, 12), 2)
    sp.tone(bl & rect(0, 13, 31, 13), 2)
    sp.tone(bl & rect(0, 14, 31, 15), 1)
    pl = sp.part_mask(5)
    sp.tone(pl & rect(0, 16, 31, 16), 3)
    sp.tone(pl & rect(17, 17, 26, 17), 4)
    sp.tone(pl & rect(0, 19, 31, 21), 1)
    rg = sp.part_mask(6)
    sp.tone(rg & rect(0, 8, 31, 9), 3)
    br = sp.part_mask(7)
    sp.tone(br & rect(27, 7, 30, 7), 3)
    sp.tone(br & rect(28, 8, 28, 8), 4)
    for y in (9, 11, 13, 15):       # brake vents
        sp.tone(br & rect(28, y, 29, y), 0)
    sp.tone(br & rect(30, 8, 30, 17), 1)
    bd = sp.part_mask(2)
    sp.tone(bd & rect(7, 18, 17, 19), 1)
    sp.tone_px([(8, 10), (9, 10), (16, 10), (16, 17)], 4)
    dm = sp.part_mask(8)
    sp.tone(dm & disk(11.5, 15.0, 1.6), 1)
    sp.tone_px([(11, 15)], 0)
    sp.tone_px([(10, 13), (9, 14)], 4)
    sc = sp.part_mask(3)
    sp.put(15, 7, (120, 220, 255, 255))
    sp.put(15, 6, (60, 140, 200, 255))
    sp.tone(sc & rect(9, 6, 14, 6), 3)
    st = sp.part_mask(0)
    sp.tone(st & rect(1, 12, 2, 23), 1)
    sp.tone(st & rect(3, 13, 7, 13), 3)
    return sp


def bow():
    sp = Sprite()
    th = math.radians(9)
    ax = (math.sin(th), -math.cos(th))      # along the bow, toward the top
    fw = (math.cos(th), math.sin(th))       # forward (bulge direction)
    cx, cy = 9.5, 16.0

    def P(s, w):
        return (cx + s * ax[0] + w * fw[0], cy + s * ax[1] + w * fw[1])

    S = 14.3

    def wfun(s):
        a = abs(s)
        if a < 12.0:
            return 9.4 - 8.4 * (a / 12.0) ** 1.8
        return 1.0 + 2.4 * ((a - 12.0) / (S - 12.0)) ** 1.5   # recurve tips

    def rad(s):
        a = abs(s)
        return 1.65 if a < 4 else (1.4 if a < 9 else (1.1 if a < 12.5 else 0.85))

    limbs = np.zeros((H, W), bool)
    n = 90
    for i in range(n):
        s0 = -S + 2 * S * i / n
        s1 = -S + 2 * S * (i + 1) / n
        x0, y0 = P(s0, wfun(s0))
        x1, y1 = P(s1, wfun(s1))
        limbs |= capsule(x0, y0, x1, y1, rad((s0 + s1) / 2))
    # decorative fins on the back of each limb (monster-hunter style)
    fins = np.zeros((H, W), bool)
    for sg in (-1, 1):
        pts = [P(sg * 3.6, wfun(3.6) + 1.0), P(sg * 6.0, wfun(6.0) + 3.6),
               P(sg * 8.0, wfun(8.0) + 3.0), P(sg * 9.8, wfun(9.8) + 0.8)]
        fins |= poly_mask(pts)
    grip = capsule(*P(-2.8, 9.3), *P(2.8, 9.3), 1.8)
    tips = np.zeros((H, W), bool)
    for sg in (-1, 1):
        tips |= disk(*P(sg * (S - 0.1), wfun(S)), 1.1)
    from pixlib import line_pts
    xa, ya = P(-12.4, 0.2)
    xb, yb = P(12.4, 0.2)
    spts = line_pts(int(xa), int(ya), int(xb), int(yb))
    string = pts_mask(spts) & ~limbs
    sp.add(string, 'string', shade='flatlight', outline=False)
    sp.add((limbs | fins) & ~grip, 'steel', tinted=True)
    sp.add(grip, 'leather')
    sp.add(tips, 'gold')
    gp = sp.part_mask(2)
    for y in range(H):
        for x in range(W):
            if gp[y, x] and y % 2 == 0:
                sp.tone_px([(x, y)], 1)
    lm = sp.part_mask(1)
    for sg in (-1, 1):
        for k in np.arange(3.0, 12.5, 0.5):
            x, y = P(sg * k, wfun(k) - 0.6)
            xi, yi = int(x), int(y)
            if lm[yi, xi]:
                sp.tone_px([(xi, yi)], 4 if 4.5 <= k <= 6.5 else 3)
    sp.tone(lm & fins & ~limbs, 2)
    for sg in (-1, 1):
        x, y = P(sg * 6.8, wfun(6.8) + 2.0)
        sp.tone_px([(int(x), int(y))], 3)
    ax_, ay_ = P(0, 9.3 - 2.2)
    sp.put(int(ax_), int(ay_), MAT['gold'][3])
    sp.put(int(ax_), int(ay_) + 1, MAT['gold'][1])
    return sp


BUG = [
    '.k.k.',
    '..h..',
    'wohow',
    'woOow',
    '.ooo.',
    '..o..',
]


def insect_glaive():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    staff = D.band(4.6, 22.6, -0.5, 1.5)
    # top-right leaf blade with a side barb
    t = (U - 21.6) / (29.6 - 21.6)
    top = (U >= 21.6) & (U <= 29.6) & (V <= 0.5 + 4 * np.sin(np.clip(t, 0, 1) * math.pi) ** 0.8 + 0.01) \
        & (V >= 0.5 - 3 * np.sin(np.clip(t, 0, 1) * math.pi) ** 0.8 - 0.01)
    top &= ~((U > 29.0) & (np.abs(V - 0.5) > 0.6))
    barb = D.poly([(21.0, -1.0), (23.5, -1.0), (22.6, -5.5)])
    # bottom-left blade (smaller, points to the corner)
    t2 = (5.4 - U) / 5.0
    bot = (U >= 0.4) & (U <= 5.4) & (np.abs(V - 0.5) <= 2.6 * np.sin(np.clip(t2, 0, 1) * math.pi) ** 0.7 + 0.01)
    bot &= ~((U < 1.0) & (np.abs(V - 0.5) > 0.6))
    collarA = D.band(4.6, 5.9, -1.5, 2.5)
    collarB = D.band(20.6, 21.9, -1.5, 2.5)
    grip = D.band(10.6, 15.4, -0.5, 1.5)
    sp.add(staff, 'darkwood')
    sp.add(grip, 'wrap_red')
    sp.add(top | barb, 'steel', tinted=True)
    sp.add(bot, 'steel', tinted=True)
    sp.add(collarA | collarB, 'gold')
    wrap_tone(sp, D, grip, 10.6)
    tp = sp.part_mask(2)
    sp.tone(tp & (np.abs(V - 0.5) < 0.6) & (U < 28), 3)
    sp.tone(tp & (np.abs(V - 1.5) < 0.6) & (U > 23) & (U < 26), 4)
    bt = sp.part_mask(3)
    sp.tone(bt & (np.abs(V - 0.5) < 0.6), 3)
    # kinsect-shaped charm tied to the staff on a gold cord
    k = MAT['kinsect']
    pal = {'k': MAT['dark'][0], 'h': MAT['dark'][1], 'w': (255, 226, 150, 255),
           'o': k[2], 'O': k[4]}
    bx, by = 12, 11
    bugmask = np.zeros((H, W), bool)
    for yy, row in enumerate(BUG):
        for xx, ch in enumerate(row):
            if ch != '.':
                bugmask[by + yy, bx + xx] = True
    sp.add(bugmask, 'kinsect', sep=True)
    for yy, row in enumerate(BUG):
        for xx, ch in enumerate(row):
            if ch != '.':
                sp.put(bx + xx, by + yy, pal[ch])
    sp.put(bx + 3, by + 6, MAT['gold'][2])
    return sp


def switch_axe():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    handle = D.band(0.4, 25.6, -1.5, 1.5)
    cap = D.band(0.4, 1.6, -2.5, 2.5)
    grip = D.band(2.4, 8.6, -1.5, 1.5)
    axe = D.poly([(17.6, 1.5), (17.9, 7.0), (19.0, 11.5), (21.0, 14.6), (23.2, 12.4),
                  (25.0, 9.0), (26.6, 5.0), (27.6, 1.5)])
    # folded sword: base at the head, tip pointing back toward the grip
    sword = D.poly([(10.1, -1.5), (27.6, -1.5), (28.6, -4.5), (13.6, -4.5)])
    spike = D.poly([(25.6, 1.5), (29.6, 0.0), (29.6, -0.6), (27.6, -1.5), (25.6, -1.5)])
    socket = D.band(16.6, 18.1, -5.5, 2.5) | D.band(25.6, 26.9, -5.5, 2.5)
    phial = D.band(10.6, 14.6, 1.5, 4.5)
    phcap = phial & ((U < 11.4) | (U > 13.8))
    sp.add(handle, 'darkwood')
    sp.add(grip, 'leather')
    sp.add(cap, 'iron')
    sp.add(axe & ~socket, 'steel', tinted=True)
    sp.add(sword & ~socket, 'steel', tinted=True)
    sp.add(spike, 'steel', tinted=True)
    sp.add(socket, 'iron')
    sp.add(phial & ~phcap, 'glass')
    sp.add(phcap, 'brass')
    wrap_tone(sp, D, grip, 2.4)
    ax_ = sp.part_mask(3)
    # cutting edge band along the crescent
    edge = ax_ & ~D.poly([(17.0, 0.0), (17.4, 6.0), (19.0, 10.0), (21.0, 12.6), (22.6, 10.5),
                          (24.4, 7.4), (26.0, 4.0), (28.0, 0.0)])
    sp.tone(edge, 3)
    sp.tone(edge & (U > 19.6) & (U < 22.6), 4)
    sp.tone(ax_ & (U > 25.4) & ~edge, 1)
    sw = sp.part_mask(4)
    sp.tone(sw & (np.abs(V + 2) < .1), 2)
    sp.tone(sw & (np.abs(V + 3) < .1), 3)
    sp.tone(sw & (np.abs(V + 4) < .1), 4)
    sp.tone(sw & (np.abs(V + 4) < .1) & (U > 22), 3)
    sp.tone(sw & (np.abs(V + 2) < .1) & (U < 16), 1)
    gl = sp.part_mask(7)
    sp.tone(gl & (np.abs(V - 4) < 0.1), 3)
    sp.tone(gl & (np.abs(V - 3) < 0.1), 2)
    sp.tone_px([D.px(12.5, 4)], 4)
    return sp


def charge_blade():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    shield = poly_mask([(1, 12), (15, 12), (15, 23), (8, 30.5), (1, 23)])
    shield &= ~(rect(1, 12, 1, 12) | rect(14, 12, 14, 12))
    face = poly_mask([(3, 14), (13, 14), (13, 22), (8, 27.6), (3, 22)])
    window = rect(6, 17, 9, 19) & ~rect(6, 17, 6, 17)
    grip = D.band(1.4, 6.6, -1.5, 1.5)
    pommel = D.band(0.4, 1.4, -2.5, 2.5)
    hilt = D.band(6.6, 9.6, -4.5, 4.5) & ~D.band(6.6, 7.1, 3.5, 4.5) & ~D.band(6.6, 7.1, -4.5, -3.5)
    phials = (D.band(7.4, 9.0, 2.5, 3.5) | D.band(7.4, 9.0, -0.5, 0.5) | D.band(7.4, 9.0, -3.5, -2.5))
    blade = D.poly([(9.6, 3.5), (25.6, 3.5), (29.6, 0.0), (29.6, -0.6), (26.6, -3.5), (9.6, -3.5)])
    sp.add(shield, 'iron')
    sp.add(face & ~window, 'wrap_red')
    sp.add(window, 'steel', tinted=True, shade='flatlight')
    sp.add(grip, 'leather', sep=True)
    sp.add(pommel, 'gold', sep=True)
    sp.add(hilt & ~phials, 'steel', sep=True)
    sp.add(phials & hilt, 'steel', tinted=True, shade='flatlight')
    sp.add(blade, 'steel', tinted=True, sep=True)
    wrap_tone(sp, D, grip, 1.4)
    sh = sp.part_mask(0)
    sp.tone(sh & rect(0, 12, 31, 12), 3)
    sp.tone_px([(2, 13), (3, 13)], 4)
    fc = sp.part_mask(1)
    sp.tone(fc & rect(0, 14, 31, 14), 3)
    win = sp.part_mask(2)
    sp.tone(win & rect(7, 17, 8, 17), 4)
    sp.tone(win & rect(6, 19, 9, 19), 2)
    ph = sp.part_mask(6)
    sp.tone(ph, 4)
    sp.tone(ph & (U > 8.4), 3)
    bl = sp.part_mask(7)
    sp.tone(bl & (np.abs(V - 2) < .1), 4)
    sp.tone(bl & (np.abs(V - 3) < .1), 3)
    sp.tone(bl & (V < -1.5), 1)
    sp.tone(bl & V_line(D, -1, 12, 22), 3)
    return sp


def dual_blades():
    sp = Sprite()

    def one(D):
        U, V = D.U, D.V
        grip = D.band(1.4, 5.6, -1.5, 1.5)
        pommel = D.band(0.4, 1.4, -1.5, 1.5)
        guard = D.band(5.6, 6.9, -3.5, 3.5)
        blade = D.poly([(6.9, 2.5), (19.6, 2.5), (23.6, -0.5), (21.6, -1.6), (6.9, -2.5)])
        # serrated back (upper-left side)
        teeth = (V > 1.6) & (np.floor(U) % 3 == 0) & (U > 9) & (U < 17)
        blade &= ~teeth
        return grip, pommel, guard, blade

    Dfront = Diag(1, 31)
    g1, p1, gd1, b1 = one(Dfront)
    g2, p2, gd2, b2 = [np.fliplr(m) for m in one(Dfront)]
    sp.add(g2, 'leather')
    sp.add(p2, 'gold')
    sp.add(gd2, 'iron')
    sp.add(b2, 'steel', tinted=True)
    sp.add(g1, 'leather', sep=True)
    sp.add(p1, 'gold', sep=True)
    sp.add(gd1, 'iron', sep=True)
    sp.add(b1, 'steel', tinted=True, sep=True)
    D = Dfront
    for gm, idx in ((g1, 4), (g2, 0)):
        wrap_tone(sp, D if idx == 4 else Diag(1, 31), gm if idx == 4 else np.zeros_like(gm), 1.4)
    wrap_tone(sp, D, g1, 1.4)
    # mirrored wrap
    U2 = np.fliplr(D.U)
    sp.tone(g2 & (np.floor(U2 - 1.4) % 2 == 1), 1)
    sp.tone(b1 & (np.abs(D.V - 1) < .1), 3)
    sp.tone(b1 & (np.abs(D.V + 1) < .1) & (D.U < 20), 4)
    V2 = np.fliplr(D.V)
    sp.tone(b2 & (np.abs(V2 + 1) < .1) & (U2 < 20), 3)
    sp.tone(b2 & (np.abs(V2 - 1) < .1), 2)
    return sp


def sword_and_shield():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    cx, cy = 20.0, 20.0
    shield = disk(cx, cy, 8.2)
    rim = shield & ~disk(cx, cy, 6.9)
    boss = disk(cx, cy, 2.0)
    grip = D.band(1.4, 5.6, -1.5, 1.5)
    pommel = D.band(0.4, 1.4, -2.5, 2.5) & ~D.band(0.4, 0.9, 1.5, 2.5) & ~D.band(0.4, 0.9, -2.5, -1.5)
    guard = D.band(5.6, 7.1, -5.5, 5.5) & ~D.band(5.6, 6.0, 4.5, 5.5) & ~D.band(5.6, 6.0, -5.5, -4.5)
    blade = D.poly([(7.1, 2.5), (21.6, 2.5), (24.6, 0.0), (24.6, -0.6), (21.6, -2.5), (7.1, -2.5)])
    sp.add(shield & ~rim & ~boss, 'wood')
    sp.add(rim, 'iron')
    sp.add(boss, 'gold')
    sp.add(grip, 'leather', sep=True)
    sp.add(pommel, 'gold', sep=True)
    sp.add(guard, 'iron', sep=True)
    sp.add(blade, 'steel', tinted=True, sep=True)
    wrap_tone(sp, D, grip, 1.4)
    wd = sp.part_mask(0)
    # plank seams on the wooden shield face
    for k in range(-12, 14, 4):
        sp.tone(wd & (np.abs(D.V - (31 - cx - cy) - k) < 0.1), 1)
    rm = sp.part_mask(1)
    for ang in range(0, 360, 45):
        x = int(cx + 7.4 * math.cos(math.radians(ang)))
        y = int(cy + 7.4 * math.sin(math.radians(ang)))
        if rm[y, x]:
            sp.tone_px([(x, y)], 4 if ang in (180, 225, 270) else 3)
    bl = sp.part_mask(6)
    sp.tone(bl & (np.abs(V - 1) < .1), 3)
    sp.tone(bl & (np.abs(V - 2) < .1), 4)
    sp.tone(bl & (np.abs(V) < .1) & (U > 8) & (U < 20), 2)
    sp.tone(bl & (V < -0.5), 1)
    gd = sp.part_mask(5)
    sp.tone_px([D.px(6.3, 4), D.px(6.3, -4)], 4)
    return sp


def hunting_horn():
    sp, D = Sprite(), diag()
    U, V = D.U, D.V
    handle = D.band(0.4, 12.6, -1.5, 1.5)
    knob = D.band(0.4, 1.6, -2.5, 2.5)
    grip = D.band(2.4, 8.6, -1.5, 1.5)
    ring = D.band(11.6, 12.8, -3.5, 3.5)
    # horn body: tube that flares into a bell
    b0, b1, b2 = 12.6, 19.6, 25.6
    tf = np.clip((U - b1) / (b2 - b1), 0, 1)
    half = np.where(U < b1, 3.5, 3.5 + 4.5 * tf ** 1.7)
    body = (U >= b0) & (U <= b2) & (np.abs(V) <= half + 0.01)
    mouth_c = D.P(b2 - 0.3, 0)
    mouth = body & (U >= b2 - 2.4) & (np.abs(V) <= 6.5)
    lip = body & (U >= b2 - 0.9)
    # drone pipes sticking out of the body (bagpipe-like)
    pipe1 = D.band(13.6, 15.1, 3.5, 9.5)
    pipe2 = D.band(16.6, 18.1, 3.5, 7.5)
    pipe_tips = D.band(13.4, 15.3, 9.5, 11.5) | D.band(16.4, 18.3, 7.5, 9.5)
    bandA = body & (np.abs(U - 19.6) < 0.6)
    sp.add(handle, 'darkwood')
    sp.add(grip, 'leather')
    sp.add(knob, 'gold')
    sp.add(pipe1 | pipe2, 'bone')
    sp.add(pipe_tips, 'gold')
    sp.add(body & ~bandA & ~(mouth & ~lip), 'steel', tinted=True)
    sp.add(bandA, 'gold')
    sp.add(ring, 'gold')
    sp.add(mouth & ~lip, 'dark', shade='flatdark')
    wrap_tone(sp, D, grip, 2.4)
    bd = sp.part_mask(5)
    sp.tone(bd & lip, 3)
    sp.tone(bd & lip & (V > 2), 4)
    sp.tone(bd & lip & (V < -3), 2)
    sp.tone(bd & (np.abs(V - 2) < .1) & (U < 19), 3)
    sp.tone(bd & (np.abs(V - 1) < .1) & (U > 13) & (U < 16), 4)
    sp.tone(bd & (V < -1.5) & (U < 21), 1)
    mt = sp.part_mask(8)
    sp.tone(mt & (V > 3.5), 2)
    sp.tone(mt & (U > b2 - 1.3) & (V < 0), 0)
    pp = sp.part_mask(3)
    sp.tone(pp & (U < 14.4), 3)
    sp.tone(pp & (U > 16.4) & (U < 17.4), 3)
    return sp


WEAPONS = [
    ('great_sword', great_sword), ('long_sword', long_sword), ('hammer', hammer),
    ('gunlance', gunlance), ('lance', lance), ('light_bowgun', light_bowgun),
    ('heavy_bowgun', heavy_bowgun), ('bow', bow), ('insect_glaive', insect_glaive),
    ('switch_axe', switch_axe), ('charge_blade', charge_blade), ('dual_blades', dual_blades),
    ('sword_and_shield', sword_and_shield), ('hunting_horn', hunting_horn),
]


def build():
    out = {}
    for name, fn in WEAPONS:
        base, over = fn().render()
        # sanity: layers must be disjoint
        both = (base[:, :, 3] > 0) & (over[:, :, 3] > 0)
        assert not both.any(), name
        # overlay must be pure grayscale
        o = over[over[:, :, 3] > 0]
        assert (o[:, 0] == o[:, 1]).all() and (o[:, 1] == o[:, 2]).all(), name
        out[name] = (base, over)
    return out


def write(out):
    for name, (base, over) in out.items():
        save(base, f'item/{name}.png')
        save(over, f'item/{name}_overlay.png')


if __name__ == '__main__':
    write(build())
