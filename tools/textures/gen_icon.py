"""Mod icon (128x128): crossed great sword + hammer on a heater shield, MH-like orange/red palette.

Drawn as 64x64 pixel art then scaled 2x with nearest neighbour so it stays crisp.
"""
import numpy as np

from pixlib import MAT, ROOT, Diag, Sprite, poly_mask, ramp, save, upscale

N = 64
RED = ramp('#2a0604', '#6e140c', '#a8241a', '#d0402a', '#f0704a')
DEEPRED = ramp('#1a0302', '#3e0a06', '#5c120c', '#7a1a12', '#98281a')
ORANGE = ramp('#3a1404', '#a8480c', '#ec8a1c', '#ffb840', '#fff0a8')
BLADE = ramp('#20222c', '#7a808e', '#b4bac4', '#dde2e8', '#ffffff')
HEAD = ramp('#1a1418', '#463a3e', '#6a5a5a', '#8e7c76', '#b8a49a')
WOOD = MAT['darkwood']
WRAP = ramp('#1a0806', '#4a140e', '#7a2416', '#a43a22', '#c85a36')


def shield_pts(inset=0.0):
    i = inset
    return [(11 + i, 7 + i), (32, 9 + i), (53 - i, 7 + i), (53 - i, 30), (51 - i, 40 - i * 0.2),
            (46 - i * 0.8, 49 - i * 0.5), (39 - i * 0.6, 55 - i * 0.6), (32, 60 - i * 1.3),
            (25 + i * 0.6, 55 - i * 0.6), (18 + i * 0.8, 49 - i * 0.5), (13 + i, 40 - i * 0.2), (11 + i, 30)]


def build():
    sp = Sprite(N, N)
    outer = poly_mask(shield_pts(0), N, N)
    rim_in = poly_mask(shield_pts(3.2), N, N)
    field = poly_mask(shield_pts(4.6), N, N)
    sp.add(outer & ~rim_in, ORANGE)
    sp.add(rim_in & ~field, DEEPRED)
    sp.add(field, RED)
    ys, xs = np.mgrid[0:N, 0:N]
    # field: radial light from the upper-left + quartered banding
    d = np.sqrt((xs - 24) ** 2 + (ys - 20) ** 2)
    sp.tone(field & (d < 30), 2)
    sp.tone(field & (d < 16), 3)
    sp.tone(field & (d >= 30), 1)
    rim = outer & ~rim_in
    sp.tone(rim & (xs + ys < 52), 3)
    sp.tone(rim & (ys <= 9) & (xs < 40), 4)
    sp.tone(rim & (xs + ys > 76), 1)

    # crossed weapons in front of the shield
    D = Diag(5, 59, N, N)
    U, V = D.U, D.V
    # hammer (built in the same frame, mirrored to run bottom-right -> top-left)
    h_handle = D.band(1.0, 37.0, -2.5, 2.5)
    h_grip = D.band(3.0, 15.0, -2.5, 2.5)
    h_cap = D.band(0.0, 2.6, -4.5, 4.5)
    h_head = D.band(35.0, 49.0, -14.5, 14.5)
    for (uu, vv) in ((35.0, 14.5), (49.0, 14.5), (35.0, -14.5), (49.0, -14.5)):
        h_head &= ~((np.abs(U - uu) < 1.6) & (np.abs(V - vv) < 3.2))
    h_band = h_head & ((np.abs(V - 9.5) < 1.6) | (np.abs(V + 9.5) < 1.6))
    flip = np.fliplr
    sp.add(flip(h_handle), WOOD)
    sp.add(flip(h_grip), WRAP)
    sp.add(flip(h_cap), ORANGE)
    sp.add(flip(h_head & ~h_band), HEAD, sep=True)
    sp.add(flip(h_band), ORANGE, sep=True)
    # great sword
    g_grip = D.band(3.0, 13.0, -2.5, 2.5)
    g_pommel = D.band(0.0, 3.0, -4.5, 4.5)
    g_guard = D.band(13.0, 17.0, -12.5, 12.5) & ~(D.band(13.0, 14.0, 10.5, 12.5) | D.band(13.0, 14.0, -12.5, -10.5))
    g_blade = D.poly([(17.0, 8.5), (48.0, 8.5), (55.6, 0.6), (55.6, -0.6), (46.0, -8.5), (17.0, -8.5)])
    sp.add(g_grip, WRAP, sep=True)
    sp.add(g_pommel, ORANGE, sep=True)
    sp.add(g_blade, BLADE, sep=True)
    sp.add(g_guard, ORANGE, sep=True)

    # shading details
    wrap_a = g_grip & (np.floor(U) % 3 == 0)
    sp.tone(wrap_a, 1)
    sp.tone(flip(h_grip & (np.floor(U) % 3 == 0)), 1)
    bl = g_blade
    sp.tone(bl & (V > 6.5), 3)
    sp.tone(bl & (np.abs(V - 6) < 0.6) & (U < 46), 4)
    sp.tone(bl & (V < -5.5), 1)
    sp.tone(bl & (np.abs(V) < 0.6) & (U > 20) & (U < 42), 1)
    sp.tone(bl & (np.abs(V - 1) < 0.6) & (U > 20) & (U < 42), 3)
    gd = g_guard
    sp.tone(gd & (U < 14.2), 3)
    sp.tone(gd & (U > 16.0), 1)
    sp.tone(gd & (np.abs(U - 15) < 1.2) & (np.abs(V) < 2.6), 4)
    hh = flip(h_head & ~h_band)
    hU, hV = flip(U), flip(V)
    sp.tone(hh & (hU < 36.6), 3)
    sp.tone(hh & (hU > 47.4), 1)
    sp.tone(hh & (hV > 12.4), 3)
    sp.tone(hh & (hV < -12.4), 1)
    sp.tone(hh & (np.abs(hU - 42) + np.abs(hV) / 2 < 2.2), 2)
    sp.tone(hh & (np.abs(hU - 42) + np.abs(hV) / 2 < 1.2), 4)
    base, _ = sp.render()
    return base


def write():
    a = build()
    big = upscale(a, 2)
    return save(big, f'{ROOT}/icon.png')


if __name__ == '__main__':
    write()
