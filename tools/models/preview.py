#!/usr/bin/env python3
"""モデルの簡易プレビュー（テクスチャ付き・ペインタアルゴリズム）。gen_models の定義を読み込んで描画する。"""
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

import gen_models as G


def rot_zyx(xr, yr, zr):
    cx, sx = math.cos(xr), math.sin(xr)
    cy, sy = math.cos(yr), math.sin(yr)
    cz, sz = math.cos(zr), math.sin(zr)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


def world_quads(mesh, hidden=()):
    by = {p.name: p for p in mesh.parts}
    mats = {}

    def mat_of(name):
        if name in mats:
            return mats[name]
        p = by[name]
        r = rot_zyx(*p.rot)
        t = np.array(p.pivot, dtype=float)
        if p.parent is None:
            m = (np.eye(3), np.zeros(3))
        else:
            m = mat_of(p.parent)
        R = m[0] @ r
        T = m[0] @ t + m[1]
        mats[name] = (R, T)
        return mats[name]

    quads = []
    for p in mesh.parts:
        if any(p.name.startswith(h) for h in hidden):
            continue
        R, T = mat_of(p.name)
        for b in p.boxes:
            x0, y0, z0 = b.x, b.y, b.z
            x1, y1, z1 = x0 + b.w, y0 + b.h, z0 + b.d
            corners = {}
            for ix, X in enumerate((x0, x1)):
                for iy, Y in enumerate((y0, y1)):
                    for iz, Z in enumerate((z0, z1)):
                        corners[(ix, iy, iz)] = R @ np.array([X, Y, Z]) + T
            fc = {f[0]: f[1:] for f in G.faces(b)}
            defs = {
                "front": [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)],
                "back": [(1, 0, 1), (0, 0, 1), (0, 1, 1), (1, 1, 1)],
                "right": [(0, 0, 1), (0, 0, 0), (0, 1, 0), (0, 1, 1)],
                "left": [(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)],
                "top": [(0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)],
                "bottom": [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)],
            }
            for fname, idx in defs.items():
                pts = [corners[i] for i in idx]
                quads.append((pts, fc[fname]))
    return quads


def render(mesh, tex_path, yaw_deg, out, hidden=(), size=420, scale=4.4, pitch_deg=-12):
    tex = Image.open(tex_path).convert("RGBA")
    quads = world_quads(mesh, hidden)
    yaw = math.radians(yaw_deg)
    pitch = math.radians(pitch_deg)
    ry = np.array([[math.cos(yaw), 0, math.sin(yaw)], [0, 1, 0], [-math.sin(yaw), 0, math.cos(yaw)]])
    rx = np.array([[1, 0, 0], [0, math.cos(pitch), -math.sin(pitch)], [0, math.sin(pitch), math.cos(pitch)]])
    V = rx @ ry
    img = Image.new("RGBA", (size, size), (40, 44, 52, 255))
    dr = ImageDraw.Draw(img)
    items = []
    for pts, (fx, fy, fw, fh) in quads:
        P = [V @ p for p in pts]
        depth = sum(p[2] for p in P) / 4
        e1 = P[1] - P[0]
        e2 = P[3] - P[0]
        n = np.cross(e1, e2)
        if n[2] > 0:  # back-face
            continue
        if fw <= 0 or fh <= 0:
            continue
        crop = tex.crop((fx, fy, fx + fw, fy + fh)).convert("RGBA")
        arr = np.array(crop).reshape(-1, 4)
        alpha = arr[:, 3] > 0
        if not alpha.any():
            continue
        col = arr[alpha][:, :3].mean(axis=0)
        ln = np.linalg.norm(n) + 1e-9
        light = 0.55 + 0.45 * max(0.0, float(-(n / ln) @ np.array([0.3, -0.7, -0.6])))
        c = tuple(int(min(255, v * light)) for v in col)
        poly = [(size / 2 + p[0] * scale, size * 0.92 + (p[1] - 24 * math.cos(pitch)) * scale) for p in P]
        items.append((depth, poly, c))
    items.sort(key=lambda t: -t[0])
    for _, poly, c in items:
        dr.polygon(poly, fill=c, outline=(0, 0, 0, 255))
    img.save(out)


if __name__ == "__main__":
    busko = G.build_busko()
    busko.pack()
    sak = G.build_sakeran()
    sak.pack()
    tex = G.TEX_DIR
    hid_base = ("horn", "brim_spike", "halo", "wing", "spike", "plate", "cable", "visor", "claw")
    sheets = []
    for name, hidden in (("buskoba", hid_base),
                         ("barino", tuple(h for h in hid_base if h != "halo")),
                         ("jangria", ("halo", "plate", "cable", "visor")),
                         ("busaiber", ("halo", "brim_spike", "spike"))):
        for yaw in (200, 20):
            out = "/tmp/prev_%s_%d.png" % (name, yaw)
            render(busko, tex + "/%s.png" % name, yaw, out, hidden)
            sheets.append(out)
    for yaw in (200, 20):
        out = "/tmp/prev_sakeran_%d.png" % yaw
        render(sak, tex + "/sakeran.png", yaw, out, (), scale=4.6)
        sheets.append(out)
    ims = [Image.open(s) for s in sheets]
    W = 420 * 4
    H = 420 * ((len(ims) + 3) // 4)
    sheet = Image.new("RGBA", (W, H))
    for i, im in enumerate(ims):
        sheet.paste(im, ((i % 4) * 420, (i // 4) * 420))
    sheet.save("/home/claude/tools/models/preview_sheet.png")
    print("ok")
