#!/usr/bin/env python3
"""武器・弾・盾などの 3D アイテムモデル（JSON の立方体の組み合わせ）を生成する。

  python3 tools/gen_models3d.py

- テクスチャは 1 枚のパレット（textures/item/palette.png）。面ごとに色の区画を UV で指定する。
- 「属性色」の区画（TINT_*）は tintindex 1 で、武器の属性色（custom_model_data の色）に染まる。
- 武器は +Y 方向に長い（持ち手が原点）。手に持ったときの向き・位置は display で計算して入れる。
- 弾・猟虫は +Z 方向が前。ItemDisplayContext.NONE で描画し、レンダラーが速度の向きに回す。
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
A = os.path.join(ROOT, "src/main/resources/assets/buskohunter")
NS = "buskohunter"

# ---------------------------------------------------------------------------
# パレット
# ---------------------------------------------------------------------------
COLORS = [
    ("steel_light", (205, 210, 218)), ("steel", (150, 157, 168)), ("steel_dark", (88, 94, 106)), ("iron_black", (38, 40, 46)),
    ("gold", (232, 184, 64)), ("gold_dark", (168, 118, 32)), ("bronze", (176, 112, 62)), ("copper", (204, 122, 72)),
    ("wood", (150, 100, 56)), ("wood_dark", (92, 58, 34)), ("leather", (112, 72, 46)), ("leather_dark", (66, 42, 28)),
    ("bone", (232, 222, 196)), ("cloth_red", (178, 40, 42)), ("cloth_blue", (50, 72, 160)), ("tint", (255, 255, 255)),
    ("tint_mid", (215, 215, 215)), ("tint_dark", (150, 150, 150)), ("glow", (255, 255, 236)), ("string", (238, 236, 226)),
    ("black", (20, 20, 24)), ("gem_red", (222, 32, 62)), ("gem_blue", (44, 164, 242)), ("gem_green", (44, 204, 94)),
    ("pink", (240, 142, 190)), ("purple", (132, 72, 172)), ("orange", (242, 142, 40)), ("grey", (122, 122, 126)),
    ("bug_shell", (58, 142, 92)), ("bug_wing", (205, 242, 255)), ("bug_eye", (252, 62, 40)), ("brass", (212, 172, 82)),
    ("fire", (255, 124, 32)), ("fire_core", (255, 230, 120)), ("holy", (255, 248, 196)), ("water", (64, 152, 255)),
    ("neon_cyan", (64, 255, 240)), ("neon_pink", (255, 64, 204)), ("glass", (120, 206, 126)), ("straw", (228, 198, 122)),
    ("sombrero_red", (198, 42, 42)), ("sake_label", (240, 236, 220)), ("glitch", (40, 255, 120)), ("missile_red", (210, 50, 40)),
]
INDEX = {name: i for i, (name, _) in enumerate(COLORS)}
TINTED = {"tint", "tint_mid", "tint_dark"}
GLOWING = {"glow", "fire", "fire_core", "holy", "neon_cyan", "neon_pink", "glitch", "bug_eye", "gem_red", "gem_blue", "gem_green"}
PALETTE = f"{NS}:item/palette"


def make_palette():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rng = random.Random(7)
    for i, (name, (r, g, b)) in enumerate(COLORS):
        cx, cy = (i % 8) * 4, (i // 8) * 4
        for dy in range(4):
            for dx in range(4):
                if name in TINTED or name in GLOWING:
                    n = 0
                else:
                    n = rng.randint(-7, 7)
                img.putpixel((cx + dx, cy + dy), (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)), 255))
    os.makedirs(f"{A}/textures/item", exist_ok=True)
    img.save(f"{A}/textures/item/palette.png")


def uv(name):
    i = INDEX[name]
    cx, cy = (i % 8) * 2, (i // 8) * 2
    return [cx + 0.25, cy + 0.25, cx + 1.75, cy + 1.75]


# ---------------------------------------------------------------------------
# 立方体
# ---------------------------------------------------------------------------
class Box:
    def __init__(self, a, b, color, rot=None, faces=None, emit=None):
        self.a = [min(a[i], b[i]) for i in range(3)]
        self.b = [max(a[i], b[i]) for i in range(3)]
        self.color = color
        self.rot = rot  # (axis, angle, origin)
        self.faces = faces
        self.emit = emit


def box(x0, y0, z0, x1, y1, z1, color, rot=None, emit=None):
    return Box((x0, y0, z0), (x1, y1, z1), color, rot, emit=emit)


def cbox(cx, cy, cz, sx, sy, sz, color, rot=None, emit=None):
    """中心と大きさで指定する立方体。"""
    return Box((cx - sx / 2, cy - sy / 2, cz - sz / 2), (cx + sx / 2, cy + sy / 2, cz + sz / 2), color, rot, emit=emit)


def element(bx, off):
    a = [round(bx.a[i] + off[i], 3) for i in range(3)]
    b = [round(bx.b[i] + off[i], 3) for i in range(3)]
    for v in a + b:
        if v < -16 or v > 32:
            raise ValueError(f"out of bounds {a} {b}")
    face = {"uv": uv(bx.color), "texture": "#p"}
    if bx.color in TINTED:
        face["tintindex"] = 1
    e = {"from": a, "to": b, "faces": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")}}
    if bx.rot:
        axis, angle, origin = bx.rot
        e["rotation"] = {"origin": [round(origin[i] + off[i], 3) for i in range(3)], "axis": axis, "angle": angle}
    emit = bx.emit if bx.emit is not None else (12 if bx.color in GLOWING else None)
    if emit:
        e["light_emission"] = emit
    return e


def bounds(boxes):
    lo = [min(b.a[i] for b in boxes) for i in range(3)]
    hi = [max(b.b[i] for b in boxes) for i in range(3)]
    return lo, hi


# ---------------------------------------------------------------------------
# 回転・持ち位置の計算（ItemTransform: T → Rx·Ry·Rz → S → -0.5）
# ---------------------------------------------------------------------------
def rot_xyz(rx, ry, rz):
    def m(a):
        return math.cos(math.radians(a)), math.sin(math.radians(a))
    cx, sx = m(rx)
    cy, sy = m(ry)
    cz, sz = m(rz)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]

    def mul(A, B):
        return [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mul(mul(Rx, Ry), Rz)


def apply(R, v):
    return [sum(R[i][k] * v[k] for k in range(3)) for i in range(3)]


def hand_point(rot, trans16, scale, model_pt):
    """バニラの変換で、モデル座標の点が手のフレームのどこに来るか。"""
    R = rot_xyz(*rot)
    p = [(model_pt[i] - 8) / 16 * scale for i in range(3)]
    q = apply(R, p)
    return [trans16[i] / 16 + q[i] for i in range(3)]


# バニラの剣（handheld）の柄の位置 = 「拳」の位置
VANILLA_GRIP = (2.0, 2.0, 8.0)
H_TP = hand_point((0, -90, 55), (0, 4.0, 0.5), 0.85, VANILLA_GRIP)
H_FP = hand_point((0, -90, 25), (1.13, 3.2, 1.13), 0.68, VANILLA_GRIP)


def transform_for(rot, scale, grip_model, target):
    R = rot_xyz(*rot)
    p = [(grip_model[i] - 8) / 16 * scale for i in range(3)]
    q = apply(R, p)
    t = [(target[i] - q[i]) * 16 for i in range(3)]
    t = [max(-80, min(80, round(v, 3))) for v in t]
    return {"rotation": list(rot), "translation": t, "scale": [round(scale, 4)] * 3}


def euler_from_axes(xdir, ydir):
    """モデルの X 軸・Y 軸がフレームのどちらを向くかから、XYZ 順の回転角（度）を求める。"""
    def norm(v):
        l = math.sqrt(sum(c * c for c in v))
        return [c / l for c in v]
    x = norm(xdir)
    y = norm(ydir)
    # y を x に直交させる
    d = sum(x[i] * y[i] for i in range(3))
    y = norm([y[i] - d * x[i] for i in range(3)])
    z = [x[1] * y[2] - x[2] * y[1], x[2] * y[0] - x[0] * y[2], x[0] * y[1] - x[1] * y[0]]
    R = [[x[0], y[0], z[0]], [x[1], y[1], z[1]], [x[2], y[2], z[2]]]
    b = math.asin(max(-1.0, min(1.0, R[0][2])))
    if abs(R[0][2]) < 0.9999:
        a = math.atan2(-R[1][2], R[2][2])
        c = math.atan2(-R[0][1], R[0][0])
    else:
        a = math.atan2(R[2][1], R[1][1])
        c = 0.0
    rot = (round(math.degrees(a), 3), round(math.degrees(b), 3), round(math.degrees(c), 3))
    # 検算
    Rc = rot_xyz(*rot)
    for col, want in ((0, x), (1, y), (2, z)):
        got = apply(Rc, [1 if i == col else 0 for i in range(3)])
        assert all(abs(got[i] - want[i]) < 1e-3 for i in range(3)), (rot, col, got, want)
    return rot


def theta_rot(theta):
    """武器の長い向き（モデル Y）を、腕を下ろした時の「前」から腕の向き（下）へ theta 度倒す。モデル X は刃の上側。"""
    t = math.radians(theta)
    return euler_from_axes((0, math.sin(t), math.cos(t)), (0, math.cos(t), -math.sin(t)))


def center_transform(rot, scale, center_model):
    R = rot_xyz(*rot)
    p = [(center_model[i] - 8) / 16 * scale for i in range(3)]
    q = apply(R, p)
    t = [max(-80, min(80, round(-v * 16, 3))) for v in q]
    return {"rotation": list(rot), "translation": t, "scale": [round(scale, 4)] * 3}


def mirror(tr):
    r = tr["rotation"]
    t = tr["translation"]
    return {"rotation": [r[0], -r[1], -r[2]], "translation": [-t[0], t[1], t[2]], "scale": tr["scale"]}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)
        f.write("\n")


def fit(boxes, limit=46.0):
    """モデルの範囲（-16..32）に収まるよう、必要なら座標を縮める。戻り値は縮めた倍率。"""
    lo, hi = bounds(boxes)
    ext = max(hi[i] - lo[i] for i in range(3))
    k = min(1.0, limit / ext)
    if k < 1.0:
        for b in boxes:
            b.a = [v * k for v in b.a]
            b.b = [v * k for v in b.b]
            if b.rot:
                axis, ang, org = b.rot
                b.rot = (axis, ang, tuple(v * k for v in org))
    return k


def weapon_model(name, boxes, length_blocks, grip=(0, 0, 0), theta=10, theta_fp=-20, gui_extra=1.0):
    tp_rot = theta_rot(theta)
    fp_rot = theta_rot(theta_fp)
    lo, hi = bounds(boxes)
    ext = max(hi[i] - lo[i] for i in range(3))
    scale = length_blocks * 16 / ext
    k = fit(boxes)
    grip = [g * k for g in grip]
    scale /= k
    lo, hi = bounds(boxes)
    center = [(lo[i] + hi[i]) / 2 for i in range(3)]
    off = [round((8 - center[i]) * 2) / 2 for i in range(3)]
    elements = [element(b, off) for b in boxes]
    grip_m = [grip[i] + off[i] for i in range(3)]
    center_m = [center[i] + off[i] for i in range(3)]
    length = max(hi[i] - lo[i] for i in range(3))
    tp = transform_for(tp_rot, scale, grip_m, H_TP)
    fp = transform_for(fp_rot, scale * 0.6, grip_m, H_FP)
    gui_s = min(1.6, 15.5 / (length * 0.82)) * gui_extra
    gui = center_transform((22, 32, -45), gui_s, center_m)
    model = {
        "textures": {"p": PALETTE, "particle": PALETTE},
        "elements": elements,
        "display": {
            "thirdperson_righthand": tp,
            "thirdperson_lefthand": mirror(tp),
            "firstperson_righthand": fp,
            "firstperson_lefthand": mirror(fp),
            "gui": gui,
            "fixed": center_transform((0, 0, -45), gui_s, center_m),
            "ground": center_transform((0, 0, -45), gui_s * 0.6, center_m),
            "head": center_transform((0, 0, 0), gui_s * 0.6, center_m),
        },
    }
    write(f"{A}/models/item/{name}.json", model)
    return scale


def tinted_item(item_id, model_name):
    write(f"{A}/items/{item_id}.json", {"model": {
        "type": "minecraft:model", "model": f"{NS}:item/{model_name}",
        "tints": [{"type": "minecraft:constant", "value": -1}, {"type": "minecraft:custom_model_data", "index": 0, "default": -1}]}})


# ---------------------------------------------------------------------------
# 武器（単位: 16 = 1 ブロック。Y が長い方向、X が「上」(持ったとき)、Z が厚み）
# ---------------------------------------------------------------------------
def great_sword():
    b = []
    b += [cbox(0, -7, 0, 3, 2.5, 3, "gold")]                       # 柄頭
    b += [cbox(0, -1, 0, 2, 10, 2, "leather_dark")]                # 柄
    b += [cbox(0, -1, 0, 2.3, 1, 2.3, "leather") for _ in range(1)]
    b += [cbox(0, 5, 0, 12, 2.4, 4, "steel_dark"), cbox(0, 5, 0, 4, 3, 4.4, "tint_dark"), cbox(5.5, 5, 0, 2, 3.4, 4.6, "gold"), cbox(-5.5, 5, 0, 2, 3.4, 4.6, "gold")]
    b += [box(-4, 6.2, -0.7, 4, 30, 0.7, "steel_light")]           # 刀身
    b += [box(-1.6, 7, -0.95, 1.6, 27, 0.95, "tint")]               # 樋（属性色）
    b += [box(-4.3, 6.2, -0.35, -3.6, 30, 0.35, "steel"), box(3.6, 6.2, -0.35, 4.3, 30, 0.35, "steel")]  # 刃
    b += [box(-3, 30, -0.6, 3, 33, 0.6, "steel_light"), box(-1.6, 33, -0.5, 1.6, 35.5, 0.5, "steel_light"), box(-0.6, 35.5, -0.4, 0.6, 37, 0.4, "steel")]
    weapon_model("great_sword", b, 1.7)


def long_sword():
    b = []
    b += [cbox(0, -9, 0, 2, 1.5, 2, "gold")]
    b += [box(-0.85, -8.5, -0.85, 0.85, 4, 0.85, "leather_dark")]
    b += [cbox(0, y, 0, 2.0, 0.6, 2.0, "tint_mid") for y in (-6, -3.5, -1, 1.5)]   # 柄巻き
    b += [cbox(0, 4.7, 0, 5, 1.2, 5, "gold"), cbox(0, 5.5, 0, 2.2, 0.8, 2.2, "gold_dark")]  # 鍔
    b += [box(-1.1, 5.8, -0.35, 1.1, 36, 0.35, "steel_light")]
    b += [box(0.7, 6.5, -0.42, 1.25, 35, 0.42, "tint")]             # 刃文（属性色）
    b += [box(-0.9, 36, -0.3, 0.9, 38, 0.3, "steel_light", rot=("z", 22.5, (0, 36, 0))), box(-0.5, 37.5, -0.25, 0.5, 39.5, 0.25, "steel", rot=("z", 30, (0, 37.5, 0)))]
    weapon_model("long_sword", b, 1.8)


def sns_sword():
    b = []
    b += [cbox(0, -4.5, 0, 2, 1.5, 2, "gold"), box(-0.8, -4, -0.8, 0.8, 3, 0.8, "leather")]
    b += [cbox(0, 3.6, 0, 7, 1.4, 2, "steel_dark"), cbox(0, 3.6, 0, 2.2, 2, 2.4, "tint_dark")]
    b += [box(-1.6, 4.3, -0.45, 1.6, 19, 0.45, "steel_light"), box(-0.6, 5, -0.55, 0.6, 17, 0.55, "tint")]
    b += [box(-1.0, 19, -0.4, 1.0, 21, 0.4, "steel_light"), box(-0.4, 21, -0.3, 0.4, 22.5, 0.3, "steel")]
    weapon_model("sword_and_shield", b, 0.95)


def dual_blades():
    b = []
    b += [box(-0.8, -4, -0.8, 0.8, 3, 0.8, "leather_dark"), cbox(0, -4.4, 0, 1.8, 1, 1.8, "gold")]
    b += [cbox(0, 3.5, 0, 4.5, 1.2, 1.8, "gold")]
    b += [box(-1.4, 4, -0.35, 1.4, 12, 0.35, "steel_light"), box(-0.5, 4.5, -0.45, 0.5, 11.5, 0.45, "tint")]
    b += [box(-1.1, 11.5, -0.3, 1.5, 17, 0.3, "steel_light", rot=("z", -15, (0, 11.5, 0))), box(-0.6, 16.5, -0.25, 0.9, 19, 0.25, "steel", rot=("z", -28, (0, 16.5, 0)))]
    b += [box(1.4, 6, -0.4, 3.0, 9, 0.4, "tint_dark", rot=("z", 30, (1.4, 6, 0)))]
    weapon_model("dual_blades", b, 0.8)


def hammer():
    b = []
    b += [cbox(0, -8, 0, 2.6, 2, 2.6, "gold")]
    b += [box(-1, -7, -1, 1, 18, 1, "wood_dark"), box(-1.2, -7, -1.2, 1.2, 2, 1.2, "leather")]
    b += [cbox(0, 23, 0, 15, 10, 10, "steel_dark")]                # 頭
    b += [cbox(0, 23, 0, 15.6, 2.2, 10.6, "gold"), cbox(0, 23, 0, 9, 10.6, 10.8, "tint_mid")]
    b += [cbox(8.1, 23, 0, 1.2, 8, 8, "steel"), cbox(-8.1, 23, 0, 1.2, 8, 8, "steel")]   # 打面
    b += [cbox(0, 28.6, 0, 3, 1.4, 3, "gem_red")]
    weapon_model("hammer", b, 1.35)


def hunting_horn():
    b = []
    b += [cbox(0, -8, 0, 2.4, 2, 2.4, "brass"), box(-1, -7, -1, 1, 14, 1, "wood")]
    b += [box(-1.2, -6, -1.2, 1.2, 2, 1.2, "leather_dark")]
    b += [box(-2.5, 12, -2.5, 2.5, 24, 2.5, "brass")]
    b += [box(-4.5, 22, -4.5, 4.5, 28, 4.5, "tint"), box(-5.5, 27, -5.5, 5.5, 29, 5.5, "brass"), cbox(0, 28.4, 0, 7, 0.6, 7, "iron_black")]
    b += [box(2.5, 14, -0.8, 6.5, 15.6, 0.8, "brass"), box(5, 15, -0.8, 6.6, 21, 0.8, "brass"), box(-6.5, 16, -0.8, -2.5, 17.6, 0.8, "brass")]
    b += [cbox(0, 18, 0, 5.4, 1, 5.4, "gold"), cbox(0, 14, 0, 5.4, 1, 5.4, "gold")]
    b += [cbox(0, 20, 2.8, 1.2, 1.2, 0.4, "gem_blue"), cbox(0, 16, 2.8, 1.2, 1.2, 0.4, "gem_green")]
    weapon_model("hunting_horn", b, 1.4)


def lance():
    b = []
    b += [cbox(0, -9, 0, 2.6, 2, 2.6, "steel_dark"), box(-1, -8, -1, 1, 5, 1, "leather_dark")]
    b += [box(-4.5, 5, -4.5, 4.5, 7, 4.5, "tint_dark"), box(-3.5, 7, -3.5, 3.5, 9, 3.5, "tint"), box(-2.6, 9, -2.6, 2.6, 11.5, 2.6, "tint_mid")]
    b += [box(-1.9, 11.5, -1.9, 1.9, 22, 1.9, "steel"), box(-1.4, 22, -1.4, 1.4, 31, 1.4, "steel_light")]
    b += [box(-0.8, 31, -0.8, 0.8, 35.5, 0.8, "steel_light"), box(-0.35, 35.5, -0.35, 0.35, 37.5, 0.35, "steel")]
    b += [cbox(0, 16, 0, 4.2, 1.2, 4.2, "gold"), cbox(0, 24, 0, 3.2, 1, 3.2, "gold")]
    weapon_model("lance", b, 2.0, theta=70, theta_fp=60)


def gunlance():
    b = []
    b += [cbox(0, -9, 0, 2.6, 2, 2.6, "steel_dark"), box(-1, -8, -1, 1, 4, 1, "leather_dark")]
    b += [box(-4.5, 4, -4.5, 4.5, 6, 4.5, "tint_dark"), cbox(0, 8, 0, 6, 4, 6, "brass")]                       # 鍔とシリンダー
    b += [box(-2.4, 10, -1.5, 2.4, 31, 1.5, "steel_light"), box(-0.8, 11, -1.7, 0.8, 30, 1.7, "tint")]
    b += [box(-1.6, 31, -1.2, 1.6, 34, 1.2, "steel_light"), box(-0.7, 34, -0.8, 0.7, 36.5, 0.8, "steel")]
    b += [box(2.4, 8, -1.3, 5.0, 29, 1.3, "iron_black"), cbox(3.7, 29.5, 0, 3.2, 1.6, 3.0, "steel_dark")]       # 砲身
    b += [cbox(3.7, 12, 0, 3.4, 1, 3.2, "gold"), cbox(3.7, 22, 0, 3.4, 1, 3.2, "gold")]
    weapon_model("gunlance", b, 1.9, theta=70, theta_fp=60)


def switch_axe():
    b = []
    b += [cbox(0, -9, 0, 2.6, 2, 2.6, "gold"), box(-1, -8, -1, 1, 25, 1, "wood_dark"), box(-1.2, -7, -1.2, 1.2, 3, 1.2, "leather")]
    b += [box(-2.8, 5, -1, -1, 17, 1, "glass", emit=8), cbox(-1.9, 4.6, 0, 2.6, 1, 2.6, "brass"), cbox(-1.9, 17.4, 0, 2.6, 1, 2.6, "brass")]  # ビン
    b += [box(1, 17, -0.7, 9, 29, 0.7, "tint"), box(8, 15, -0.5, 11, 31, 0.5, "steel_light")]   # 斧刃
    b += [box(1, 21, -1.2, 3, 26, 1.2, "steel_dark")]
    b += [box(-6, 22, -0.6, -1, 24, 0.6, "steel", rot=("z", 20, (-1, 23, 0)))]          # 背のトゲ
    b += [cbox(0, 30, 0, 2.4, 3, 2.4, "steel_dark"), cbox(0, 32, 0, 1.2, 1.6, 1.2, "steel")]
    weapon_model("switch_axe", b, 1.6)


def charge_blade():
    b = []
    b += [cbox(0, -5, 0, 2.2, 1.6, 2.2, "gold"), box(-0.9, -4.5, -0.9, 0.9, 3, 0.9, "leather_dark")]
    b += [cbox(0, 4, 0, 8, 2.4, 3, "steel_dark")]
    b += [cbox(x, 4, 1.7, 1.2, 1.8, 0.8, "glass", emit=10) for x in (-3, -1.2, 1.2, 3)]  # ビン
    b += [box(-2.4, 5.2, -0.5, 2.4, 22, 0.5, "steel_light"), box(-1, 6, -0.65, 1, 20, 0.65, "tint")]
    b += [box(-1.6, 22, -0.45, 1.6, 24.5, 0.45, "steel_light"), box(-0.6, 24.5, -0.35, 0.6, 26.5, 0.35, "steel")]
    weapon_model("charge_blade", b, 1.1)


def insect_glaive():
    b = []
    b += [box(-0.8, -16, -0.8, 0.8, 26, 0.8, "steel_dark")]
    b += [box(-1.1, -4, -1.1, 1.1, 4, 1.1, "leather_dark")]
    b += [box(-1.0, 26, -0.3, 1.0, 31, 0.3, "steel_light"), box(-2.6, 23, -0.35, -0.6, 28, 0.35, "tint", rot=("z", -25, (-0.6, 23, 0))), box(0.6, 23, -0.35, 2.6, 28, 0.35, "tint", rot=("z", 25, (0.6, 23, 0)))]
    b += [box(-0.8, -16, -0.3, 0.8, -12, 0.3, "steel", rot=("z", 0, (0, -14, 0))), box(-0.5, -16, -0.25, 0.5, -19, 0.25, "steel_light")]
    b += [cbox(0, 22, 0, 2.6, 1.4, 2.6, "gold"), cbox(0, -11, 0, 2.6, 1.4, 2.6, "gold"), cbox(0, 22, 1.4, 1, 1, 0.4, "gem_green")]
    weapon_model("insect_glaive", b, 1.8)


def light_bowgun():
    b = []
    b += [box(-1.1, 4, -1.1, 1.1, 21, 1.1, "iron_black"), cbox(0, 21.3, 0, 2.8, 1, 2.8, "steel_dark")]     # 銃身
    b += [box(-2.2, -3, -1.8, 2.8, 8, 1.8, "steel_dark"), box(-1.6, -2, -2.0, 2.2, 7, 2.0, "tint_mid")]    # 機関部
    b += [box(-1.6, -12, -1.2, 2.0, -3, 1.2, "wood"), box(-2.4, -14, -1.4, 2.6, -12, 1.4, "leather_dark")]  # 銃床
    b += [box(-6, -2.5, -0.9, -2.2, -0.5, 0.9, "leather", rot=("z", -15, (-2.2, -1.5, 0)))]              # グリップ
    b += [box(2.8, 0, -0.6, 4.0, 6, 0.6, "iron_black"), cbox(3.4, 6.3, 0, 1.6, 1, 1.6, "gem_blue")]        # 照準
    b += [box(-4.5, 3, -0.9, -2.2, 6, 0.9, "steel")]                                                     # 弾倉
    weapon_model("light_bowgun", b, 1.1, grip=(-4.2, -1.4, 0), theta=90, theta_fp=90)


def heavy_bowgun():
    b = []
    b += [box(-1.8, 8, -1.8, 1.8, 30, 1.8, "iron_black"), cbox(0, 30.5, 0, 4.4, 1.6, 4.4, "steel_dark"), cbox(0, 26, 0, 4, 1, 4, "steel")]
    b += [box(-3.4, -6, -3.2, 4.6, 10, 3.2, "steel_dark"), box(-2.6, -4, -3.5, 3.8, 8, 3.5, "tint_mid")]
    b += [box(2, 9, -4.2, 6.2, 22, -3.2, "tint_dark"), box(2, 9, 3.2, 6.2, 22, 4.2, "tint_dark")]       # 側面の盾板
    b += [box(-2.4, -16, -1.8, 3.0, -6, 1.8, "wood_dark"), box(-3, -16, -2, 3.6, -14, 2, "leather_dark")]
    b += [box(-8, -4.5, -1.1, -3.4, -2, 1.1, "leather", rot=("z", -12, (-3.4, -3, 0)))]
    b += [box(4.6, -2, -0.9, 6.4, 6, 0.9, "iron_black"), cbox(5.5, 6.5, 0, 2, 1.2, 2, "gem_red")]
    weapon_model("heavy_bowgun", b, 1.5, grip=(-5.6, -3.0, 0), theta=90, theta_fp=90)


def bow():
    b = []
    b += [box(-3, -1, -0.8, 3, 1, 0.8, "leather_dark"), box(-1.5, -1.2, -1.0, 1.5, 1.2, 1.0, "leather")]        # 握り
    for sgn in (1, -1):
        b += [box(*(sorted([3 * sgn, 9 * sgn]) and (min(3 * sgn, 9 * sgn), -0.3, -0.6, max(3 * sgn, 9 * sgn), 1.4, 0.6)), "tint")]
        b += [box(min(9 * sgn, 14 * sgn), -1.6, -0.5, max(9 * sgn, 14 * sgn), 0.2, 0.5, "tint_mid", rot=("z", 18 * sgn, (9 * sgn, 0, 0)))]
        b += [box(min(13.5 * sgn, 17 * sgn), -3.6, -0.45, max(13.5 * sgn, 17 * sgn), -2.2, 0.45, "tint_dark", rot=("z", 35 * sgn, (13.5 * sgn, -2.2, 0)))]
        b += [cbox(16.6 * sgn, -3.8, 0, 1.2, 1.2, 1.2, "gold")]
    b += [box(-16.4, -4.6, -0.12, 16.4, -4.3, 0.12, "string")]                                                   # 弦
    b += [cbox(0, 1.6, 1.1, 1, 1, 0.6, "gem_green")]
    weapon_model("bow", b, 1.4, grip=(0, 0, 0), theta=90, theta_fp=80)


# ---------------------------------------------------------------------------
# 左手に付けるもの（盾・双剣の左・猟虫）
# ---------------------------------------------------------------------------
# 左腕フレーム（肩の付け根が原点、+Y 前, +Z 上, +X キャラの右）での、バニラの持ち位置の補正
LEFT_HAND_SHIFT = (-1.0, 2.0, -10.0)


def euler_for(cols):
    """モデルの X,Y,Z 軸がフレームのどの向きに行くか（cols）を満たす回転（XYZ 順の角度）を探す。"""
    for rx in range(-180, 181, 90):
        for ry in range(-180, 181, 90):
            for rz in range(-180, 181, 90):
                R = rot_xyz(rx, ry, rz)
                ok = True
                for axis in range(3):
                    e = [1 if i == axis else 0 for i in range(3)]
                    v = apply(R, e)
                    if any(abs(v[i] - cols[axis][i]) > 1e-6 for i in range(3)):
                        ok = False
                        break
                if ok:
                    return (rx, ry, rz)
    raise ValueError(cols)


def offhand_model(name, boxes, scale, anchor, target_px, xdir, ydir, gui_rot=(22, 32, 0)):
    """左腕に付ける飾り。anchor（モデル座標）を、左腕フレームの target_px（肩が原点、px）に置く。
    xdir・ydir はモデルの X 軸・Y 軸がフレーム（腕を下ろした時：+Y 前, +Z 上, +X キャラの右）のどちらを向くか。"""
    rot = euler_from_axes(xdir, ydir)
    lo, hi = bounds(boxes)
    center = [(lo[i] + hi[i]) / 2 for i in range(3)]
    off = [round((8 - center[i]) * 2) / 2 for i in range(3)]
    elements = [element(b, off) for b in boxes]
    anchor_m = [anchor[i] + off[i] for i in range(3)]
    center_m = [center[i] + off[i] for i in range(3)]
    length = max(hi[i] - lo[i] for i in range(3))
    target = [(target_px[i] - LEFT_HAND_SHIFT[i]) / 16 for i in range(3)]
    tl = transform_for(rot, scale, anchor_m, target)
    gui_s = min(1.4, 15 / (length * 0.9))
    model = {
        "textures": {"p": PALETTE, "particle": PALETTE},
        "elements": elements,
        "display": {
            # 左手の補正（x 移動と y・z 回転の反転）がかかるので、反転した値を書いておく
            "thirdperson_lefthand": mirror(tl),
            "thirdperson_righthand": tl,
            "gui": center_transform(gui_rot, gui_s, center_m),
            "fixed": center_transform((0, 0, 0), gui_s, center_m),
            "ground": center_transform((0, 0, 0), gui_s * 0.6, center_m),
        },
    }
    write(f"{A}/models/item/{name}.json", model)


def stance_axes(alpha):
    """腕を前に alpha 度上げた構えで、世界の「前」と「上」になるフレームの向き。"""
    t = math.radians(alpha)
    fwd = (0, math.cos(t), -math.sin(t))
    up = (0, math.sin(t), math.cos(t))
    return fwd, up


def shields():
    # 盾は握り手で持つ（拳の先に、構えた時に正面を向く）。板はモデルの X-Y 面、表は +Z。
    # 構え（左腕を 65〜70 度前へ上げる）で、表が前・モデル Y が上になるように向ける
    fwd, up = stance_axes(68)
    side = (-1, 0, 0)
    grip = (-1.0, 0.0, -11.0)  # 左手の先（肩から 11px 下、腕の中心）
    small = [box(-6, -6, -0.8, 6, 6, 0.8, "tint"), box(-6.6, -6.6, -1.3, 6.6, 6.6, -0.8, "steel_dark"),
             cbox(0, 0, 1.2, 3, 3, 0.8, "gold"), box(-0.6, -5.6, 0.8, 0.6, 5.6, 1.1, "tint_dark"), box(-5.6, -0.6, 0.8, 5.6, 0.6, 1.1, "tint_dark")]
    offhand_model("disp_shield_small", small, 1.0, (0, 0, -1.3), grip, side, up)
    large = [box(-8, -12, -1.2, 8, 12, 1.2, "tint"), box(-8.6, -12.6, -1.8, 8.6, 12.6, -1.2, "steel_dark"),
             box(-1, -10, 1.2, 1, 10, 1.7, "gold"), box(-6, -1, 1.2, 6, 1, 1.7, "gold"), cbox(0, 0, 2.0, 4, 4, 0.8, "tint_dark")]
    offhand_model("disp_shield_large", large, 1.0, (0, -2, -1.8), grip, side, up)
    cb = [box(-6.5, -9, -1, 6.5, 9, 1, "tint"), box(-7, -9.5, -1.5, 7, 9.5, -1, "steel_dark"), box(-6, 9, -0.8, 6, 12, 0.8, "steel_light"),
          cbox(0, 0, 1.4, 3.4, 3.4, 0.8, "glass", emit=10), box(-0.6, -8, 1, 0.6, 8, 1.3, "gold")]
    offhand_model("disp_shield_cb", cb, 1.0, (0, -1, -1.5), grip, side, up)


def bug_boxes():
    """猟虫（+Z が前、+Y が背中）。"""
    return [
        cbox(0, 0, 0, 3, 2.4, 6, "bug_shell"), cbox(0, 0.2, 3.8, 2.2, 1.8, 2, "bug_shell"), cbox(0.7, 0.6, 4.6, 0.6, 0.6, 0.6, "bug_eye"),
        cbox(-0.7, 0.6, 4.6, 0.6, 0.6, 0.6, "bug_eye"), cbox(0, 1.4, -0.5, 2.6, 0.6, 4.5, "tint"),
        box(1.5, 0.8, -1, 6.5, 1.0, 2.6, "bug_wing", rot=("z", 22.5, (1.5, 0.9, 0))), box(-6.5, 0.8, -1, -1.5, 1.0, 2.6, "bug_wing", rot=("z", -22.5, (-1.5, 0.9, 0))),
        box(0.4, 1.0, 4.5, 0.8, 1.4, 8, "black", rot=("x", -22.5, (0.6, 1.2, 4.5))), box(-0.8, 1.0, 4.5, -0.4, 1.4, 8, "black", rot=("x", -22.5, (-0.6, 1.2, 4.5))),
    ]


def kinsect_perch():
    # 腕の外側にとまる猟虫：構え（左腕を 40 度前へ）で頭（モデル +Z）が前、背中（+Y）が腕の外（フレーム -X）
    fwd, up = stance_axes(40)
    back = (-1, 0, 0)
    x = (back[1] * fwd[2] - back[2] * fwd[1], back[2] * fwd[0] - back[0] * fwd[2], back[0] * fwd[1] - back[1] * fwd[0])
    offhand_model("disp_kinsect", bug_boxes(), 1.0, (0, -1.2, 0), (-3.1, 0.0, -7.0), x, back)


# ---------------------------------------------------------------------------
# 弾・猟虫・モンスターの飛び道具（+Z が前、NONE で描画：中心 8,8,8）
# ---------------------------------------------------------------------------
def proj_model(name, boxes, tinted=False):
    lo, hi = bounds(boxes)
    center = [(lo[i] + hi[i]) / 2 for i in range(3)]
    off = [round((8 - center[i]) * 2) / 2 for i in range(3)]
    elements = [element(b, off) for b in boxes]
    length = max(hi[i] - lo[i] for i in range(3))
    s = min(1.4, 14 / (length * 0.9))
    model = {"textures": {"p": PALETTE, "particle": PALETTE}, "elements": elements,
             "display": {"gui": center_transform((25, 40, 0), s, [8, 8, 8]), "ground": center_transform((0, 0, 0), s * 0.6, [8, 8, 8]),
                         "fixed": center_transform((0, 90, 0), s, [8, 8, 8])}}
    write(f"{A}/models/item/{name}.json", model)
    if tinted:
        tinted_item(name, name)
    else:
        write(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})


def projectiles():
    proj_model("proj_arrow", [box(-0.35, -0.35, -7, 0.35, 0.35, 6, "wood"), box(-0.9, -0.9, 6, 0.9, 0.9, 8.5, "steel_light"), cbox(0, 0, 9, 0.6, 0.6, 1.2, "tint"),
                              box(-0.1, -1.6, -7.5, 0.1, 1.6, -4, "tint_mid"), box(-1.6, -0.1, -7.5, 1.6, 0.1, -4, "tint_mid")], tinted=True)
    proj_model("proj_bullet", [cbox(0, 0, 0, 1.6, 1.6, 4, "brass"), cbox(0, 0, 2.6, 1.2, 1.2, 1.4, "tint", emit=14), cbox(0, 0, -3, 1.0, 1.0, 3, "glow", emit=10)], tinted=True)
    proj_model("proj_shell", [cbox(0, 0, 0, 2.4, 2.4, 5, "steel_dark"), cbox(0, 0, 3, 1.8, 1.8, 1.6, "fire_core", emit=15), cbox(0, 0, -3.5, 1.6, 1.6, 2, "fire", emit=12)])
    proj_model("proj_kinsect", bug_boxes(), tinted=True)
    proj_model("proj_missile", [cbox(0, 0, 0, 2.4, 2.4, 9, "steel"), cbox(0, 0, 5, 1.8, 1.8, 1.4, "missile_red"), cbox(0, 0, 6.2, 1.0, 1.0, 1, "missile_red"),
                                box(-3, -0.2, -4.5, 3, 0.2, -2, "steel_dark"), box(-0.2, -3, -4.5, 0.2, 3, -2, "steel_dark"), cbox(0, 0, -5.2, 1.6, 1.6, 1.4, "fire", emit=15)])
    proj_model("proj_sake_bottle", [cbox(0, 0, -1, 3.4, 3.4, 6, "glass"), cbox(0, 0, 3, 1.6, 1.6, 2.4, "glass"), cbox(0, 0, 4.6, 1.8, 1.8, 0.8, "wood"),
                                    cbox(0, 0, -1, 3.6, 3.6, 2.4, "sake_label")])
    proj_model("proj_sombrero", [cbox(0, 0, 0, 12, 1.2, 12, "sombrero_red"), cbox(0, 0.4, 0, 10.5, 1.0, 10.5, "straw"), cbox(0, 2.6, 0, 5, 3.6, 5, "straw"),
                                 cbox(0, 4.8, 0, 3.6, 1.2, 3.6, "straw"), cbox(0, 1.4, 0, 5.4, 0.8, 5.4, "gem_green")])
    proj_model("proj_fireball", [cbox(0, 0, 0, 5, 5, 5, "fire", emit=15), cbox(0, 0, 0, 3.6, 6, 3.6, "fire_core", emit=15), cbox(0, 0, 0, 6, 3.6, 3.6, "fire_core", emit=15),
                                 cbox(0, 0, -3.5, 3, 3, 3, "fire", emit=15)])
    proj_model("proj_water_orb", [cbox(0, 0, 0, 5, 5, 5, "water", emit=10), cbox(0, 0, 0, 3.6, 6, 3.6, "water", emit=10), cbox(0, 0, 0, 6, 3.6, 3.6, "water", emit=10),
                                  cbox(0.8, 1.2, 1.6, 1.2, 1.2, 1.2, "glow", emit=15)])
    proj_model("proj_holy_orb", [cbox(0, 0, 0, 5, 5, 5, "holy", emit=15), cbox(0, 0, 0, 3.6, 6.4, 3.6, "glow", emit=15), cbox(0, 0, 0, 6.4, 3.6, 3.6, "glow", emit=15),
                                 cbox(0, 0, 0, 3.6, 3.6, 6.4, "gold", emit=15)])
    proj_model("proj_glitch_cube", [cbox(0, 0, 0, 5, 5, 5, "iron_black"), cbox(0, 0, 0, 5.4, 1, 5.4, "glitch", emit=15), cbox(0, 0, 0, 1, 5.4, 5.4, "neon_pink", emit=15),
                                    cbox(1.6, 1.6, 2.6, 1.4, 1.4, 1, "neon_cyan", emit=15)])


def main():
    make_palette()
    great_sword(); long_sword(); sns_sword(); dual_blades(); hammer(); hunting_horn(); lance(); gunlance()
    switch_axe(); charge_blade(); insect_glaive(); light_bowgun(); heavy_bowgun(); bow()
    for w in ("great_sword", "long_sword", "sword_and_shield", "dual_blades", "hammer", "hunting_horn", "lance", "gunlance",
              "switch_axe", "charge_blade", "insect_glaive", "light_bowgun", "heavy_bowgun", "bow"):
        tinted_item(w, w)
    shields()
    kinsect_perch()
    for i in ("disp_shield_small", "disp_shield_large", "disp_shield_cb", "disp_kinsect"):
        tinted_item(i, i)
    tinted_item("disp_dual_left", "dual_blades")
    projectiles()
    print("H_TP", [round(v, 3) for v in H_TP], "H_FP", [round(v, 3) for v in H_FP])


if __name__ == "__main__":
    main()
