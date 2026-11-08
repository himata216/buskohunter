#!/usr/bin/env python3
"""
ブスコハンター：大型モンスターのモデル（Java の LayerDefinition コード）とテクスチャを
1 つの定義から同時に生成するスクリプト。

  python3 gen_models.py

出力:
  buskohunter/src/client/java/com/buskohunter/client/render/model/BuskoMeshes.java
  buskohunter/src/main/resources/assets/buskohunter/textures/entity/<kind>.png

座標系（Minecraft のエンティティモデル空間）:
  1 単位 = 1/16 ブロック、Y は下向きが正、-Z がモンスターの正面、-X がモンスターの右側。
  root の原点 = 足元（地面）。
UV はボックス UV（Minecraft 標準の展開）で自動配置する。
"""
import math
import os
import random

from PIL import Image

ROOT = "/home/claude/buskohunter"
JAVA_OUT = ROOT + "/src/client/java/com/buskohunter/client/render/model/BuskoMeshes.java"
TEX_DIR = ROOT + "/src/main/resources/assets/buskohunter/textures/entity"


# ---------------------------------------------------------------------------
# モデル定義 DSL
# ---------------------------------------------------------------------------
class Box:
    def __init__(self, x, y, z, w, h, d, mat, decal=None, inflate=0.0):
        self.x, self.y, self.z = x, y, z
        self.w, self.h, self.d = int(w), int(h), int(d)
        self.mat = mat
        self.decal = decal
        self.inflate = inflate
        self.u = self.v = 0


class Part:
    def __init__(self, name, parent, pivot, rot=(0, 0, 0), boxes=()):
        self.name = name
        self.parent = parent
        self.pivot = pivot
        self.rot = rot
        self.boxes = list(boxes)


class Mesh:
    def __init__(self, name, tex_w, tex_h):
        self.name = name
        self.tex_w = tex_w
        self.tex_h = tex_h
        self.parts = []

    def part(self, name, parent, pivot, rot=(0, 0, 0), boxes=()):
        p = Part(name, parent, pivot, rot, boxes)
        self.parts.append(p)
        return p

    def pack(self):
        """シェルフ方式で UV を配置。"""
        boxes = [b for p in self.parts for b in p.boxes]
        order = sorted(boxes, key=lambda b: -(b.d + b.h))
        x = y = shelf_h = 0
        for b in order:
            bw = 2 * (b.w + b.d)
            bh = b.d + b.h
            if x + bw > self.tex_w:
                x = 0
                y += shelf_h
                shelf_h = 0
            b.u, b.v = x, y
            x += bw
            shelf_h = max(shelf_h, bh)
        if y + shelf_h > self.tex_h:
            raise SystemExit(f"{self.name}: texture too small ({y + shelf_h} > {self.tex_h})")
        return y + shelf_h


# ---------------------------------------------------------------------------
# ブスコ系（ブスコバ / ブスコバリーノ王 / ジャングリア / ブサイバー / 訓練人形）共通メッシュ
# ---------------------------------------------------------------------------
def build_busko():
    m = Mesh("busko", 512, 256)
    m.part("root", None, (0, 24, 0))
    m.part("body", "root", (0, -18, 0), boxes=[
        Box(-22, -16, -17, 44, 32, 34, "skin", "belly"),
        Box(-18, -13, -20, 36, 26, 3, "skin"),
        Box(-25, -12, -13, 50, 24, 26, "skin"),
        Box(-17, -19, -13, 34, 3, 26, "skin"),
        Box(-17, 16, -13, 34, 2, 26, "skin_dark"),
        Box(-14, -23, -15, 28, 7, 20, "skin", "back"),
    ])
    # 尻（左右）
    for side, sx in (("r", -1), ("l", 1)):
        m.part("lobe_" + side, "body", (sx * 11, -1, 15), boxes=[
            Box(-11, -15, -2, 22, 30, 12, "lobe", "lobe_" + side),
            Box(-9, -12, 10, 18, 24, 3, "lobe", "lobe_back_" + side),
            Box(-12, -12, 0, 24, 24, 9, "lobe"),
        ])
    m.part("neck", "body", (0, -22, -7), boxes=[Box(-4, -5, -4, 8, 6, 8, "skin")])
    m.part("head", "neck", (0, -5, 0), boxes=[
        Box(-6, -11, -6, 12, 11, 12, "skin", "face"),
        Box(-5, -9, -7, 10, 7, 1, "skin", "face_front"),
    ])
    m.part("visor", "head", (0, -7, -7), boxes=[Box(-6.5, -2, -0.6, 13, 4, 1, "visor")])
    m.part("hat", "head", (0, -10, 0), boxes=[
        Box(-15, -1, -15, 30, 2, 30, "hat_brim", "brim"),
        Box(-7, -11, -7, 14, 10, 14, "hat_crown", "crown"),
        Box(-5, -14, -5, 10, 3, 10, "hat_crown"),
    ])
    m.part("hat_band", "hat", (0, -3, 0), boxes=[Box(-7.5, -2, -7.5, 15, 2, 15, "band", inflate=0)])
    # 角（ジャングリア・ブサイバー）
    for side, sx in (("r", -1), ("l", 1)):
        m.part("horn_" + side, "hat", (sx * 6, -8, 0), rot=(0.0, 0.0, sx * 0.55), boxes=[
            Box(-2, -10, -2, 4, 10, 4, "horn")])
        m.part("horn_tip_" + side, "horn_" + side, (0, -9, 0), rot=(-0.2, 0.0, sx * 0.55), boxes=[
            Box(-1.5, -9, -1.5, 3, 9, 3, "horn_tip")])
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        m.part("brim_spike_%d" % i, "hat", (math.cos(a) * 13, -1, math.sin(a) * 13),
               rot=(-math.sin(a) * 0.6, 0.0, math.cos(a) * 0.6), boxes=[Box(-1, -6, -1, 2, 6, 2, "horn_tip")])
    # 光輪（ブスコバリーノ王）
    m.part("halo", "body", (0, -38, 22))
    segs = 16
    radius = 30
    seg_len = 2 * math.pi * radius / segs + 1
    for i in range(segs):
        a = 2 * math.pi * i / segs
        m.part("halo_%d" % i, "halo", (math.cos(a) * radius, math.sin(a) * radius, 0), rot=(0, 0, a + math.pi / 2),
               boxes=[Box(-seg_len / 2, -1.5, -1, seg_len, 3, 2, "halo")])
    for i in range(8):
        a = 2 * math.pi * i / 8 + 0.2
        r2 = 18
        m.part("halo_in_%d" % i, "halo", (math.cos(a) * r2, math.sin(a) * r2, 0.5), rot=(0, 0, a + math.pi / 2),
               boxes=[Box(-7.5, -1, -0.5, 15, 2, 1, "halo_inner")])
    for i in range(6):
        a = 2 * math.pi * i / 6
        m.part("halo_rune_%d" % i, "halo", (math.cos(a) * 24, math.sin(a) * 24, 0.2), rot=(0, 0, a),
               boxes=[Box(-2, -2, -0.5, 4, 4, 1, "rune")])
    # 腕（細長い）
    for side, sx in (("r", -1), ("l", 1)):
        m.part("arm_" + side, "body", (sx * 20, -20, -6), rot=(-0.25, 0.0, -sx * 0.75), boxes=[
            Box(-1.5, -1.5, -1.5, 3, 26, 3, "arm")])
        m.part("forearm_" + side, "arm_" + side, (0, 25, 0), rot=(0.1, 0.0, sx * 0.9), boxes=[
            Box(-1.5, -0.5, -1.5, 3, 20, 3, "arm")])
        m.part("hand_" + side, "forearm_" + side, (0, 19, 0), boxes=[
            Box(-2.5, 0, -3, 5, 2, 6, "hand")])
        for c in range(3):
            m.part("claw_%s_%d" % (side, c), "hand_" + side, ((c - 1) * 1.6, 1, -3), rot=(-0.6, 0, 0), boxes=[
                Box(-0.5, -0.5, -4, 1, 1, 4, "claw")])
    # 翼（ジャングリア・ブサイバー）
    for side, sx in (("r", -1), ("l", 1)):
        m.part("wing_" + side, "body", (sx * 12, -20, 10), rot=(-0.3, sx * 0.3, sx * 0.9), boxes=[
            Box(-1, -20, -1, 2, 20, 2, "wing_bone")])
        m.part("wing_mem_" + side, "wing_" + side, (0, -10, 0.5), boxes=[
            Box(-sx * 0 - (16 if sx < 0 else 0), -10, -0.5, 16, 20, 1, "wing_membrane")])
        m.part("wing2_" + side, "body", (sx * 18, 4, 10), rot=(-0.4, sx * 0.4, sx * 1.4), boxes=[
            Box(-1, -14, -1, 2, 14, 2, "wing_bone")])
        m.part("wing2_mem_" + side, "wing2_" + side, (0, -7, 0.5), boxes=[
            Box(-(11 if sx < 0 else 0), -7, -0.5, 11, 14, 1, "wing_membrane")])
    # 背中のトゲ（ジャングリア）
    for i in range(4):
        x = -9 + i * 6
        m.part("spike_%d" % i, "body", (x, -24, 2 + (i % 2) * 4), rot=(-0.4, 0, 0), boxes=[
            Box(-1.5, -7, -1.5, 3, 7, 3, "horn_tip")])
    # サイバー装甲（ブサイバー）
    for side, sx in (("r", -1), ("l", 1)):
        m.part("plate_" + side, "lobe_" + side, (0, 0, 13), boxes=[
            Box(-9, -12, 0, 18, 24, 2, "plate", "plate_" + side)])
    for i in range(4):
        m.part("cable_%d" % i, "body", (-9 + i * 6, 14, 18), rot=(0.25, 0, 0), boxes=[
            Box(-0.5, 0, -0.5, 1, 10, 1, "cable")])
    return m


# ---------------------------------------------------------------------------
# サケラン（筋骨隆々の白い獣人）
# ---------------------------------------------------------------------------
def build_sakeran():
    m = Mesh("sakeran", 256, 256)
    m.part("root", None, (0, 24, 0))
    m.part("hips", "root", (0, -26, 0), boxes=[Box(-9, -2, -6, 18, 6, 12, "fur")])
    m.part("waist", "hips", (0, -2, 0), boxes=[Box(-8, -10, -5.5, 16, 10, 11, "fur", "abs")])
    m.part("chest", "waist", (0, -9, 0), boxes=[
        Box(-12, -16, -7, 24, 16, 14, "fur", "chest"),
        Box(-11, -14, -9, 10, 8, 2, "fur", "pec"),
        Box(1, -14, -9, 10, 8, 2, "fur", "pec"),
    ])
    m.part("ruff", "chest", (0, -15, 0), boxes=[
        Box(-10, -3, -6, 20, 5, 12, "fur_light"),
        Box(-12, -2, -2, 24, 4, 8, "fur_light"),
    ])
    m.part("neck", "chest", (0, -16, -1), boxes=[Box(-4.5, -5, -4.5, 9, 6, 9, "fur")])
    m.part("head", "neck", (0, -4, 0), boxes=[
        Box(-6, -11, -6, 12, 11, 12, "fur", "dog_face"),
    ])
    m.part("muzzle", "head", (0, -5, -5), rot=(0.42, 0, 0), boxes=[
        Box(-4.5, -3, -14, 9, 7, 14, "muzzle", "muzzle"),
    ])
    m.part("nose", "muzzle", (0, 0, -14), boxes=[Box(-5, -3.5, -4, 10, 8, 4, "nose")])
    m.part("jaw", "muzzle", (0, 4, -1), boxes=[Box(-4, -1, -11, 8, 2, 11, "muzzle")])
    for side, sx in (("r", -1), ("l", 1)):
        m.part("ear_" + side, "head", (sx * 4, -10, 1), rot=(0.15, 0, -sx * 0.25), boxes=[
            Box(-1.5, -9, -1, 3, 9, 2, "fur", "ear")])
        m.part("cheek_" + side, "head", (sx * 6.5, -3, 1), boxes=[Box(-2, -3, -3, 4, 6, 6, "fur_light")])
        # 腕
        m.part("delt_" + side, "chest", (sx * 13, -12, 0), boxes=[Box(-5.5, -5, -5.5, 11, 10, 11, "fur")])
        m.part("arm_" + side, "delt_" + side, (sx * 1, 3, 0), rot=(0, 0, -sx * 0.12), boxes=[
            Box(-4.5, 0, -4.5, 9, 15, 9, "fur", "bicep")])
        m.part("forearm_" + side, "arm_" + side, (0, 14, 0), boxes=[Box(-4, 0, -4, 8, 13, 8, "fur")])
        m.part("paw_" + side, "forearm_" + side, (0, 13, 0), boxes=[Box(-4.5, 0, -4.5, 9, 7, 9, "fur", "paw")])
        m.part("finger1_" + side, "paw_" + side, (sx * -1.8, 6, -3), boxes=[Box(-1.2, 0, -1.2, 2.4, 6, 2.4, "fur")])
        m.part("finger2_" + side, "paw_" + side, (sx * 1.2, 6, -3), boxes=[Box(-1.2, 0, -1.2, 2.4, 6, 2.4, "fur")])
        m.part("claw1_" + side, "finger1_" + side, (0, 6, 0), boxes=[Box(-1, 0, -1, 2, 2, 2, "claw")])
        m.part("claw2_" + side, "finger2_" + side, (0, 6, 0), boxes=[Box(-1, 0, -1, 2, 2, 2, "claw")])
        # 脚
        m.part("thigh_" + side, "hips", (sx * 5, 3, 0), boxes=[Box(-5, 0, -5, 10, 12, 10, "fur")])
        m.part("shin_" + side, "thigh_" + side, (0, 11, 0), boxes=[Box(-4, 0, -4, 8, 11, 8, "fur")])
        m.part("foot_" + side, "shin_" + side, (0, 10, 0), boxes=[Box(-4.5, 0, -7, 9, 4, 11, "fur", "foot")])
    m.part("tail", "hips", (0, 0, 6), rot=(0.7, 0, 0), boxes=[Box(-2.5, -2.5, 0, 5, 5, 8, "fur_light")])
    return m


# ---------------------------------------------------------------------------
# 塗り（マテリアル）
# ---------------------------------------------------------------------------
def clamp(c):
    return max(0, min(255, int(c)))


def shade(rgb, f):
    return tuple(clamp(c * f) for c in rgb[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3))


def hexrgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


class Painter:
    """マテリアル名 → 面の塗り関数。"""

    def __init__(self, img, rng):
        self.img = img
        self.px = img.load()
        self.rng = rng

    def put(self, x, y, rgb, a=255):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.px[x, y] = (rgb[0], rgb[1], rgb[2], a)

    def get(self, x, y):
        return self.px[x, y][:3]

    def fill_face(self, fx, fy, fw, fh, fn):
        for j in range(fh):
            for i in range(fw):
                c = fn(i, j, fw, fh)
                if c is not None:
                    self.put(fx + i, fy + j, c)


def faces(b):
    """(name, x, y, w, h) の 6 面（Minecraft のボックス UV）。"""
    u, v, w, h, d = b.u, b.v, b.w, b.h, b.d
    return [
        ("top", u + d, v, w, d),
        ("bottom", u + d + w, v, w, d),
        ("right", u, v + d, d, h),
        ("front", u + d, v + d, w, h),
        ("left", u + d + w, v + d, d, h),
        ("back", u + 2 * d + w, v + d, w, h),
    ]


def noise_fn(rng, base, amount, light=1.0):
    def fn(i, j, fw, fh):
        n = rng.uniform(-amount, amount)
        return shade(base, light + n)
    return fn


def make_materials(variant, rng):
    """variant ごとの色設定。各値は (face, i, j, fw, fh) -> rgb を返す関数を作る。"""
    pal = {
        "buskoba": dict(skin=(238, 214, 196), skin_dark=(214, 186, 168), lobe=(242, 218, 202),
                        hat=(214, 178, 108), band_a=(200, 30, 40), band_b=(30, 120, 50), arm=(232, 206, 188),
                        hand=(222, 194, 176), horn=(60, 50, 50), wing=(90, 40, 50), plate=(40, 40, 46),
                        halo=(250, 220, 120), eye=(30, 20, 20)),
        "barino": dict(skin=(246, 240, 236), skin_dark=(226, 218, 222), lobe=(250, 246, 244),
                       hat=(232, 200, 120), band_a=(220, 40, 70), band_b=(40, 170, 90), arm=(244, 236, 236),
                       hand=(236, 228, 228), horn=(240, 210, 120), wing=(250, 240, 220), plate=(240, 220, 160),
                       halo=(255, 226, 120), eye=(60, 40, 30)),
        "jangria": dict(skin=(150, 48, 52), skin_dark=(110, 30, 36), lobe=(168, 70, 66),
                        hat=(196, 160, 96), band_a=(170, 20, 20), band_b=(40, 110, 50), arm=(120, 40, 44),
                        hand=(90, 30, 34), horn=(30, 24, 26), wing=(70, 22, 28), plate=(60, 20, 24),
                        halo=(255, 120, 40), eye=(255, 200, 60)),
        "busaiber": dict(skin=(120, 38, 42), skin_dark=(80, 24, 30), lobe=(140, 54, 52),
                         hat=(40, 42, 50), band_a=(255, 40, 120), band_b=(40, 230, 255), arm=(50, 52, 60),
                         hand=(40, 42, 50), horn=(30, 30, 36), wing=(30, 34, 44), plate=(36, 38, 46),
                         halo=(60, 240, 255), eye=(80, 255, 255)),
        "dummy": dict(skin=(214, 184, 110), skin_dark=(180, 150, 80), lobe=(222, 192, 118),
                      hat=(160, 120, 70), band_a=(160, 40, 40), band_b=(40, 100, 160), arm=(150, 110, 60),
                      hand=(130, 95, 50), horn=(90, 70, 40), wing=(90, 70, 40), plate=(90, 70, 40),
                      halo=(200, 180, 80), eye=(40, 30, 20)),
    }[variant]
    return pal


def paint_busko(mesh, variant, out_path, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (mesh.tex_w, mesh.tex_h), (0, 0, 0, 0))
    p = Painter(img, rng)
    pal = make_materials(variant, rng)

    def skinlike(base, amount=0.04):
        def fn(face, i, j, fw, fh):
            light = {"top": 1.08, "bottom": 0.82, "front": 1.0, "back": 0.97, "left": 0.93, "right": 0.93}[face]
            # なめらかなグラデーション
            g = 1.0 - 0.10 * (j / max(1, fh - 1)) if face not in ("top", "bottom") else 1.0
            c = shade(base, light * g + rng.uniform(-amount, amount))
            if variant == "barino":
                # 真珠の虹色光沢
                t = (math.sin(i * 0.35 + j * 0.22) + 1) / 2
                sheen = mix((255, 220, 240), (210, 240, 255), t)
                c = mix(c, sheen, 0.18)
            if variant == "jangria":
                # 血管とマグマのひび
                if rng.random() < 0.035:
                    c = (255, 120 + rng.randint(0, 60), 30)
                elif (i * 7 + j * 3) % 23 == 0:
                    c = shade(c, 0.7)
            if variant == "busaiber":
                if (i + j) % 11 == 0 and rng.random() < 0.5:
                    c = shade(c, 0.75)
            if variant == "dummy":
                # 藁
                if (j + (i // 2)) % 3 == 0:
                    c = shade(c, 0.85)
                if rng.random() < 0.06:
                    c = shade(c, 1.15)
            return c
        return fn

    def straw(base):
        def fn(face, i, j, fw, fh):
            weave = ((i // 2) + (j // 2)) % 2
            c = shade(base, 1.0 + (0.08 if weave else -0.06) + rng.uniform(-0.03, 0.03))
            if variant == "barino" and rng.random() < 0.05:
                c = rng.choice([(255, 80, 120), (80, 220, 140), (120, 200, 255), (255, 240, 160)])
            if variant == "busaiber":
                c = shade((40, 42, 52), 1.0 + (0.1 if weave else -0.05))
                if j % 4 == 0:
                    c = (60, 230, 255) if (i // 3) % 2 == 0 else (255, 60, 150)
            return c
        return fn

    def flat(base, amount=0.05):
        def fn(face, i, j, fw, fh):
            light = {"top": 1.1, "bottom": 0.8}.get(face, 1.0)
            return shade(base, light + rng.uniform(-amount, amount))
        return fn

    mats = {
        "skin": skinlike(pal["skin"]),
        "skin_dark": skinlike(pal["skin_dark"]),
        "lobe": skinlike(pal["lobe"], 0.03),
        "hat_brim": straw(pal["hat"]),
        "hat_crown": straw(pal["hat"]),
        "band": None,
        "arm": skinlike(pal["arm"]),
        "hand": flat(pal["hand"]),
        "horn": flat(pal["horn"], 0.06),
        "horn_tip": flat(shade(pal["horn"], 0.8), 0.06),
        "halo": flat(pal["halo"], 0.04),
        "halo_inner": flat(shade(pal["halo"], 1.05), 0.04),
        "rune": flat((255, 255, 230)),
        "claw": flat((40, 36, 36) if variant != "barino" else (250, 230, 150)),
        "wing_bone": flat(pal["horn"]),
        "wing_membrane": skinlike(pal["wing"], 0.06),
        "plate": flat(pal["plate"], 0.04),
        "cable": flat((40, 40, 46)),
        "visor": flat((20, 20, 26)),
    }

    def band_fn(face, i, j, fw, fh):
        return pal["band_a"] if j == 0 else pal["band_b"]
    mats["band"] = band_fn

    for part in mesh.parts:
        for b in part.boxes:
            fnm = mats[b.mat]
            for (fname, fx, fy, fw, fh) in faces(b):
                p.fill_face(fx, fy, fw, fh, lambda i, j, w, h, fname=fname: fnm(fname, i, j, w, h))
            decal_busko(p, b, variant, pal, rng)
    # 未使用パーツの見た目整え（ダミー）
    img.save(out_path)


def decal_busko(p, b, variant, pal, rng):
    fcs = {f[0]: f[1:] for f in faces(b)}
    d = b.decal
    if d is None:
        return
    if d in ("face", "face_front"):
        fx, fy, fw, fh = fcs["front"]
        eye = pal["eye"]
        # 目（小さく離れた点目）と口
        ey = fy + max(1, fh // 3)
        for ex in (fx + fw // 2 - 3, fx + fw // 2 + 2):
            p.put(ex, ey, eye)
            p.put(ex, ey + 1, eye)
            if variant in ("jangria", "busaiber"):
                p.put(ex, ey, (255, 230, 80) if variant == "jangria" else (80, 255, 255))
        my = fy + fh - 3
        for mx in range(fx + fw // 2 - 2, fx + fw // 2 + 2):
            p.put(mx, my, shade(pal["skin"], 0.55))
        p.put(fx + fw // 2 - 3, my - 1, shade(pal["skin"], 0.6))
        p.put(fx + fw // 2 + 2, my - 1, shade(pal["skin"], 0.6))
    elif d.startswith("lobe_back_") or d.startswith("lobe_"):
        # 尻の丸みのハイライトと、ジャングリアの尻の眼
        fx, fy, fw, fh = fcs["back"]
        cx, cy = fx + fw // 2, fy + fh // 2
        for j in range(fh):
            for i in range(fw):
                dx = (i - fw / 2) / (fw / 2)
                dy = (j - fh / 2) / (fh / 2)
                r = dx * dx + dy * dy
                c = p.get(fx + i, fy + j)
                if r < 0.25:
                    c = shade(c, 1.07)
                elif r > 0.85:
                    c = shade(c, 0.9)
                p.put(fx + i, fy + j, c)
        if variant == "jangria" and d.startswith("lobe_back_"):
            for (ox, oy) in ((-4, -6), (3, -2), (-2, 5), (4, 8)):
                for j in range(-2, 3):
                    for i in range(-3, 4):
                        if i * i / 9 + j * j / 4 <= 1:
                            p.put(cx + ox + i, cy + oy + j, (250, 230, 200))
                p.put(cx + ox, cy + oy, (20, 10, 10))
                p.put(cx + ox - 1, cy + oy, (255, 140, 30))
                p.put(cx + ox + 1, cy + oy, (255, 140, 30))
    elif d.startswith("plate_"):
        fx, fy, fw, fh = fcs["back"]
        # 六角形のネオンライト
        for (ox, oy, col) in ((fw // 2, fh // 3, (180, 80, 255)), (fw // 2, 2 * fh // 3, (60, 220, 255))):
            for j in range(-3, 4):
                for i in range(-3, 4):
                    if abs(i) + abs(j) * 0.6 <= 3:
                        edge = abs(i) + abs(j) * 0.6 > 2
                        p.put(fx + ox + i, fy + oy + j, (220, 240, 255) if edge else col)
        for i in range(fw):
            p.put(fx + i, fy, (60, 240, 255))
            p.put(fx + i, fy + fh - 1, (255, 60, 160))
    elif d == "brim":
        fx, fy, fw, fh = fcs["top"]
        cx, cy = fw / 2, fh / 2
        for j in range(fh):
            for i in range(fw):
                r = math.hypot(i - cx, j - cy)
                if 12.5 < r < 14:
                    p.put(fx + i, fy + j, pal["band_a"])
                elif 10.5 < r < 11.5:
                    p.put(fx + i, fy + j, pal["band_b"])
                elif r > 15:
                    p.put(fx + i, fy + j, (0, 0, 0), 0)
        fx, fy, fw, fh = fcs["bottom"]
        for j in range(fh):
            for i in range(fw):
                if math.hypot(i - cx, j - cy) > 15:
                    p.put(fx + i, fy + j, (0, 0, 0), 0)
    elif d == "crown":
        for fname in ("front", "back", "left", "right"):
            fx, fy, fw, fh = fcs[fname]
            for i in range(fw):
                zig = (i // 2) % 2
                p.put(fx + i, fy + fh - 3 + zig, pal["band_b"])
                p.put(fx + i, fy + 2 + zig, pal["band_a"])
    elif d == "belly":
        fx, fy, fw, fh = fcs["front"]
        p.put(fx + fw // 2, fy + fh // 2 + 3, shade(pal["skin"], 0.75))
    elif d == "back":
        fx, fy, fw, fh = fcs["back"]
        for j in range(fh):
            p.put(fx + fw // 2, fy + j, shade(pal["skin"], 0.88))


def paint_sakeran(mesh, out_path, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (mesh.tex_w, mesh.tex_h), (0, 0, 0, 0))
    p = Painter(img, rng)
    white = (240, 240, 238)

    def fur(base, amount=0.05):
        def fn(face, i, j, fw, fh):
            light = {"top": 1.05, "bottom": 0.80, "front": 1.0, "back": 0.95, "left": 0.92, "right": 0.92}[face]
            strand = 0.05 if (i * 3 + j * 5 + (j // 3)) % 7 == 0 else 0
            c = shade(base, light - strand + rng.uniform(-amount, amount))
            if face in ("front", "back", "left", "right") and j > fh - 2:
                c = shade(c, 0.9)
            return c
        return fn

    mats = {
        "fur": fur(white),
        "fur_light": fur((250, 250, 250), 0.06),
        "muzzle": fur((246, 244, 242), 0.03),
        "nose": lambda face, i, j, fw, fh: (40, 36, 38) if not (face == "front" and i in (2, fw - 3) and j == 1) else (90, 90, 96),
        "claw": lambda face, i, j, fw, fh: (40, 38, 40),
    }
    for part in mesh.parts:
        for b in part.boxes:
            fnm = mats[b.mat]
            for (fname, fx, fy, fw, fh) in faces(b):
                p.fill_face(fx, fy, fw, fh, lambda i, j, w, h, fname=fname: fnm(fname, i, j, w, h))
            fcs = {f[0]: f[1:] for f in faces(b)}
            d = b.decal
            if d == "dog_face":
                fx, fy, fw, fh = fcs["front"]
                # 琥珀色の目
                for ex in (fx + 2, fx + fw - 5):
                    for (i, j, c) in ((0, 0, (60, 30, 10)), (1, 0, (240, 160, 30)), (2, 0, (250, 190, 50)),
                                      (0, 1, (200, 110, 20)), (1, 1, (40, 20, 10)), (2, 1, (230, 140, 30))):
                        p.put(ex + i, fy + 4 + j, c)
                    p.put(ex, fy + 3, (180, 180, 180))
                    p.put(ex + 1, fy + 3, (170, 170, 170))
                    p.put(ex + 2, fy + 3, (180, 180, 180))
            elif d == "muzzle":
                fx, fy, fw, fh = fcs["front"]
                for i in range(fw):
                    p.put(fx + i, fy + fh - 1, (200, 196, 196))
                # 鼻先へ向かって黒くグラデーション（下面・側面の先端）
                for fname in ("left", "right", "top", "bottom"):
                    gx, gy, gw, gh = fcs[fname]
                    for j in range(gh):
                        for i in range(gw):
                            t = 0
                            if fname in ("left", "right"):
                                t = (i if fname == "left" else (gw - 1 - i)) / max(1, gw - 1)
                                t = 1 - t
                            else:
                                t = 1 - j / max(1, gh - 1)
                            if t > 0.75:
                                p.put(gx + i, gy + j, mix(p.get(gx + i, gy + j), (50, 46, 48), (t - 0.75) * 4))
            elif d == "abs":
                fx, fy, fw, fh = fcs["front"]
                for j in range(fh):
                    for i in range(fw):
                        if i == fw // 2 or j in (fh // 3, 2 * fh // 3):
                            p.put(fx + i, fy + j, (205, 205, 205))
            elif d == "chest":
                fx, fy, fw, fh = fcs["front"]
                for i in range(fw):
                    p.put(fx + i, fy + fh - 1, (210, 210, 210))
            elif d == "pec":
                fx, fy, fw, fh = fcs["front"]
                for i in range(fw):
                    p.put(fx + i, fy + fh - 1, (200, 200, 200))
                p.put(fx + fw // 2, fy + fh // 2 + 1, (230, 200, 200))
            elif d == "ear":
                fx, fy, fw, fh = fcs["front"]
                for j in range(2, fh - 1):
                    p.put(fx + 1, fy + j, (238, 200, 200))
            elif d == "bicep":
                fx, fy, fw, fh = fcs["front"]
                for j in range(fh // 2):
                    p.put(fx + fw // 2, fy + j + 2, (215, 215, 215))
            elif d == "paw":
                fx, fy, fw, fh = fcs["bottom"]
                for (i, j) in ((2, 2), (5, 2), (3, 5), (4, 5)):
                    p.put(fx + i, fy + j, (50, 48, 50))
            elif d == "foot":
                fx, fy, fw, fh = fcs["front"]
                for i in range(1, fw - 1, 2):
                    p.put(fx + i, fy + fh - 1, (60, 58, 60))
    img.save(out_path)


# ---------------------------------------------------------------------------
# Java コード出力
# ---------------------------------------------------------------------------
def fnum(x):
    if abs(x - round(x)) < 1e-6:
        return "%d.0F" % round(x)
    return ("%.4f" % x).rstrip("0") + "F"


def java_mesh_method(mesh, method):
    lines = []
    lines.append("\tpublic static LayerDefinition %s() {" % method)
    lines.append("\t\tMeshDefinition mesh = new MeshDefinition();")
    lines.append("\t\tPartDefinition p_ = mesh.getRoot();")
    var = {}
    for part in mesh.parts:
        v = "p_" + part.name.replace("-", "_")
        parent = "p_" if part.parent is None else var[part.parent]
        if part.parent is None:
            # root はメッシュの root の子として作る（アニメーションで全身を動かすため）
            pass
        cb = "CubeListBuilder.create()"
        for b in part.boxes:
            cb += ".texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s%s)" % (
                b.u, b.v, fnum(b.x), fnum(b.y), fnum(b.z), fnum(b.w), fnum(b.h), fnum(b.d),
                (", new CubeDeformation(%s)" % fnum(b.inflate)) if b.inflate else "")
        px, py, pz = part.pivot
        rx, ry, rz = part.rot
        if rx or ry or rz:
            pose = "PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)" % (
                fnum(px), fnum(py), fnum(pz), fnum(rx), fnum(ry), fnum(rz))
        else:
            pose = "PartPose.offset(%s, %s, %s)" % (fnum(px), fnum(py), fnum(pz))
        lines.append("\t\tPartDefinition %s = %s.addOrReplaceChild(\"%s\", %s, %s);" % (v, parent, part.name, cb, pose))
        var[part.name] = v
    lines.append("\t\treturn LayerDefinition.create(mesh, %d, %d);" % (mesh.tex_w, mesh.tex_h))
    lines.append("\t}")
    return "\n".join(lines)


def java_names(mesh, const):
    by = {p.name: p for p in mesh.parts}
    rows = []
    for p in mesh.parts:
        path = []
        q = p
        while q is not None:
            path.append(q.name)
            q = by.get(q.parent) if q.parent else None
        path.reverse()
        rows.append("{" + ", ".join('"%s"' % n for n in path) + "}")
    return "\t/** 各パーツの root からのパス。 */\n\tpublic static final String[][] %s = {\n\t\t%s\n\t};" % (const, ",\n\t\t".join(rows))


def write_java(busko, sakeran):
    os.makedirs(os.path.dirname(JAVA_OUT), exist_ok=True)
    src = []
    src.append("package com.buskohunter.client.render.model;")
    src.append("")
    src.append("import net.minecraft.client.model.geom.PartPose;")
    src.append("import net.minecraft.client.model.geom.builders.CubeDeformation;")
    src.append("import net.minecraft.client.model.geom.builders.CubeListBuilder;")
    src.append("import net.minecraft.client.model.geom.builders.LayerDefinition;")
    src.append("import net.minecraft.client.model.geom.builders.MeshDefinition;")
    src.append("import net.minecraft.client.model.geom.builders.PartDefinition;")
    src.append("")
    src.append("/**")
    src.append(" * 自動生成ファイル（tools/models/gen_models.py）。手で編集せず、スクリプトを直して再生成してください。")
    src.append(" */")
    src.append("@SuppressWarnings(\"unused\")")
    src.append("public final class BuskoMeshes {")
    src.append("\tprivate BuskoMeshes() {")
    src.append("\t}")
    src.append("")
    src.append(java_names(busko, "BUSKO_PARTS"))
    src.append(java_names(sakeran, "SAKERAN_PARTS"))
    src.append("")
    src.append(java_mesh_method(busko, "createBuskoLayer"))
    src.append("")
    src.append(java_mesh_method(sakeran, "createSakeranLayer"))
    src.append("}")
    src.append("")
    with open(JAVA_OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(src))


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    busko = build_busko()
    used = busko.pack()
    sak = build_sakeran()
    used2 = sak.pack()
    print("busko atlas rows used:", used, "sakeran:", used2)
    for i, variant in enumerate(("buskoba", "barino", "jangria", "busaiber", "dummy")):
        name = "training_dummy" if variant == "dummy" else variant
        paint_busko(busko, variant, os.path.join(TEX_DIR, name + ".png"), 100 + i)
    paint_sakeran(sak, os.path.join(TEX_DIR, "sakeran.png"), 7)
    write_java(busko, sak)
    print("parts busko:", len(busko.parts), "sakeran:", len(sak.parts))


if __name__ == "__main__":
    main()
