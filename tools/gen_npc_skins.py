#!/usr/bin/env python3
"""集会所 NPC のスキン（64x64・プレイヤーと同じ配置）を描く。

  python3 tools/gen_npc_skins.py
"""
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "src/main/resources/assets/buskohunter/textures/entity/npc")

# 部位ごとのテクスチャ位置 (u, v, 幅, 高さ, 奥行き)
PARTS = {
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4), "rsleeve": (40, 32, 4, 12, 4),
    "larm": (32, 48, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4),
    "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4),
    "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4),
}


def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


def jitter(c, rng, amt=6):
    if c is None:
        return None
    if len(c) == 4 and c[3] == 0:
        return c
    n = rng.randint(-amt, amt)
    out = tuple(max(0, min(255, c[i] + n)) for i in range(3))
    return out + ((c[3],) if len(c) == 4 else (255,))


def paint(img, part, fn, rng, amt=6):
    """fn(face, x, y, w, h) -> 色 / None（塗らない）。"""
    u, v, w, h, d = PARTS[part]
    for face, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
        for y in range(fh):
            for x in range(fw):
                c = fn(face, x, y, fw, fh)
                if c is not None:
                    img.putpixel((fx + x, fy + y), jitter(c, rng, amt))


SKIN_LIGHT = (240, 204, 172)
SKIN_TAN = (204, 150, 104)
SKIN_DARK = (150, 100, 66)
SKIN_OLD = (226, 190, 160)
WHITE = (238, 238, 232)
BLACK = (28, 26, 30)
EYE = (40, 60, 110)


def head(img, rng, skin, hair, hair_front_rows=2, side_rows=4, eyes=EYE, beard=None, mustache=None, blush=False, long_hair=False):
    def fn(face, x, y, w, h):
        if face == "top":
            return hair
        if face == "bottom":
            return skin
        if face == "back":
            return hair if (long_hair or y < 6) else skin
        if face in ("left", "right"):
            if y < side_rows or (long_hair and (x >= 5 if face == "right" else x <= 2)):
                return hair
            return skin
        # 顔
        if y < hair_front_rows:
            return hair
        if long_hair and (x == 0 or x == 7) and y < 7:
            return hair
        if y == 4 and x in (1, 2, 5, 6):
            return WHITE if x in (1, 6) else eyes
        if y == 3 and x in (1, 2, 5, 6):
            return tuple(max(0, c - 40) for c in hair)  # 眉
        if beard and y >= 5:
            return beard
        if mustache and y == 6 and 2 <= x <= 5:
            return mustache
        if y == 6 and x in (3, 4):
            return (170, 90, 80)
        if blush and y == 5 and x in (1, 6):
            return (240, 160, 150)
        return skin
    paint(img, "head", fn, rng, 4)


def clear(img):
    for y in range(64):
        for x in range(64):
            img.putpixel((x, y), (0, 0, 0, 0))


def solid(color):
    return lambda face, x, y, w, h: color


def rows(spec, default):
    """spec: [(y0, y1, color)] で横じまに塗る。"""
    def fn(face, x, y, w, h):
        for y0, y1, c in spec:
            if y0 <= y < y1:
                return c
        return default
    return fn


def make(name, draw):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(sum(map(ord, name)))
    draw(img, rng)
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, f"{name}.png"))


def receptionist(img, rng):
    hair = (122, 70, 38)
    red, gold = (186, 40, 52), (232, 190, 70)
    head(img, rng, SKIN_LIGHT, hair, hair_front_rows=2, side_rows=6, long_hair=True, blush=True)
    # 髪飾り（帽子の層に小さなリボン）
    paint(img, "hat", lambda f, x, y, w, h: (240, 240, 250) if (f == "top" and 2 <= x <= 5 and 1 <= y <= 2) or (f == "front" and y == 0 and 2 <= x <= 5) else (0, 0, 0, 0), rng, 0)

    def body(face, x, y, w, h):
        if y <= 1:
            return WHITE
        if face == "front" and x in (3, 4) and y in (3, 5, 7):
            return gold
        if y == 8:
            return gold
        return red
    paint(img, "body", body, rng)
    arm = rows([(0, 1, WHITE), (9, 11, WHITE), (11, 12, SKIN_LIGHT)], red)
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    leg = rows([(0, 4, (120, 30, 40)), (4, 10, WHITE), (10, 12, (90, 54, 34))], WHITE)
    paint(img, "rleg", leg, rng)
    paint(img, "lleg", leg, rng)


def guild_master(img, rng):
    beard = (236, 236, 236)
    purple, gold = (96, 52, 132), (226, 184, 70)
    head(img, rng, SKIN_OLD, (220, 220, 222), hair_front_rows=1, side_rows=3, beard=beard)
    paint(img, "hat", lambda f, x, y, w, h: beard if (f == "front" and y >= 6 and 1 <= x <= 6) else (0, 0, 0, 0), rng, 0)
    paint(img, "body", rows([(7, 8, gold)], purple), rng)
    paint(img, "jacket", lambda f, x, y, w, h: beard if (f == "front" and y <= 2 and 2 <= x <= 5) else (0, 0, 0, 0), rng, 0)
    arm = rows([(10, 11, gold), (11, 12, SKIN_OLD)], purple)
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    leg = rows([(10, 12, (60, 40, 30))], (80, 44, 112))
    paint(img, "rleg", leg, rng)
    paint(img, "lleg", leg, rng)


def cook(img, rng):
    head(img, rng, SKIN_LIGHT, (60, 40, 30), hair_front_rows=1, side_rows=3, mustache=BLACK)
    # コック帽
    paint(img, "hat", lambda f, x, y, w, h: WHITE if (f in ("top",) or y <= 2) else (0, 0, 0, 0), rng, 3)

    def body(face, x, y, w, h):
        if face == "front" and 1 <= x <= 6 and y >= 4:
            return (250, 250, 250) if y != 4 else (200, 60, 60)
        if face == "front" and x in (2, 5) and y in (1, 3):
            return (40, 40, 40)
        return WHITE
    paint(img, "body", body, rng)
    arm = rows([(10, 12, SKIN_LIGHT)], WHITE)
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    check = lambda f, x, y, w, h: ((40, 40, 44) if (x + y) % 2 == 0 else (200, 200, 200)) if y < 10 else (40, 30, 26)
    paint(img, "rleg", check, rng, 2)
    paint(img, "lleg", check, rng, 2)


def smith(img, rng):
    head(img, rng, SKIN_TAN, (30, 26, 24), hair_front_rows=1, side_rows=2, mustache=(30, 26, 24))
    # ゴーグル
    paint(img, "hat", lambda f, x, y, w, h: ((120, 220, 255) if f == "front" and x in (1, 2, 5, 6) else (80, 52, 32)) if (y == 2 and f != "top" and f != "bottom") else (0, 0, 0, 0), rng, 0)
    apron = (110, 70, 40)

    def body(face, x, y, w, h):
        if face == "front":
            return apron if y >= 1 else (90, 90, 96)
        if face == "back" and x in (1, 6):
            return apron
        return (90, 90, 96)
    paint(img, "body", body, rng)
    arm = rows([(0, 3, (90, 90, 96)), (9, 12, (60, 44, 30))], SKIN_TAN)
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    leg = rows([(10, 12, (40, 36, 34))], (70, 70, 76))
    paint(img, "rleg", leg, rng)
    paint(img, "lleg", leg, rng)


def clerk(img, rng):
    hair = (232, 140, 60)
    head(img, rng, SKIN_LIGHT, hair, hair_front_rows=2, side_rows=4, blush=True)
    green = (60, 150, 70)
    paint(img, "hat", lambda f, x, y, w, h: green if (f == "top" or y <= 1) else (0, 0, 0, 0), rng, 4)

    def body(face, x, y, w, h):
        if face == "front" and 3 <= x <= 4:
            return WHITE
        if face in ("front", "back") and (x + y) % 9 == 0:
            return (120, 80, 40)
        return green
    paint(img, "body", body, rng)
    arm = rows([(0, 4, green), (11, 12, SKIN_LIGHT)], WHITE)
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    leg = rows([(10, 12, (60, 40, 26))], (130, 96, 60))
    paint(img, "rleg", leg, rng)
    paint(img, "lleg", leg, rng)


def gatekeeper(img, rng):
    head(img, rng, SKIN_TAN, (90, 60, 40), hair_front_rows=1, side_rows=3)
    steel = (168, 174, 184)
    paint(img, "hat", lambda f, x, y, w, h: steel if (f == "top" or y <= 1 or (f != "front" and y <= 5) or (f == "front" and x in (0, 7) and y <= 5)) else (0, 0, 0, 0), rng, 5)
    blue, gold = (50, 76, 168), (230, 190, 70)

    def body(face, x, y, w, h):
        if face == "front" and 2 <= x <= 5 and y >= 2:
            return gold if y == 6 else blue
        return steel
    paint(img, "body", body, rng)
    arm = rows([(11, 12, (90, 90, 96))], steel)
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    leg = rows([(0, 3, blue), (10, 12, (60, 60, 66))], steel)
    paint(img, "rleg", leg, rng)
    paint(img, "lleg", leg, rng)


def instructor(img, rng):
    head(img, rng, SKIN_DARK, (24, 20, 18), hair_front_rows=1, side_rows=2)
    red = (200, 40, 40)
    paint(img, "hat", lambda f, x, y, w, h: red if (f != "top" and f != "bottom" and 1 <= y <= 2) or (f == "back" and 2 < y <= 4 and 3 <= x <= 4) else (0, 0, 0, 0), rng, 3)
    leather = (120, 78, 44)

    def body(face, x, y, w, h):
        if y == 7:
            return (60, 40, 24)
        if face == "front" and (x + y) % 5 == 0:
            return (96, 60, 34)
        return leather
    paint(img, "body", body, rng)
    arm = rows([(0, 2, leather), (8, 10, (60, 40, 24))], SKIN_DARK)
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    leg = rows([(10, 12, (40, 30, 22))], (54, 80, 50))
    paint(img, "rleg", leg, rng)
    paint(img, "lleg", leg, rng)


def rookie(img, rng):
    head(img, rng, SKIN_LIGHT, (236, 204, 96), hair_front_rows=2, side_rows=3)
    armor, dark = (110, 130, 70), (90, 62, 36)
    paint(img, "body", rows([(6, 7, dark)], armor), rng)
    arm = rows([(0, 3, armor), (11, 12, SKIN_LIGHT)], dark)
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    leg = rows([(9, 12, (70, 50, 30))], (120, 90, 56))
    paint(img, "rleg", leg, rng)
    paint(img, "lleg", leg, rng)


def bard(img, rng):
    head(img, rng, SKIN_LIGHT, (90, 50, 110), hair_front_rows=2, side_rows=4, blush=True)
    hatc, feather = (120, 40, 140), (236, 60, 60)

    def hat(f, x, y, w, h):
        if f == "top":
            return feather if x == 6 and y <= 4 else hatc
        if y <= 1:
            return hatc
        if f == "left" and y <= 3 and x >= 2:
            return feather
        return (0, 0, 0, 0)
    paint(img, "hat", hat, rng, 3)
    stripe = lambda f, x, y, w, h: (236, 200, 60) if (y // 2) % 2 == 0 else (60, 180, 180)
    paint(img, "body", stripe, rng)
    arm = rows([(11, 12, SKIN_LIGHT)], (236, 200, 60))
    paint(img, "rarm", arm, rng)
    paint(img, "larm", arm, rng)
    leg = rows([(10, 12, (90, 54, 34))], (70, 140, 80))
    paint(img, "rleg", leg, rng)
    paint(img, "lleg", leg, rng)


def main():
    for name, fn in [("receptionist", receptionist), ("guild_master", guild_master), ("cook", cook), ("smith", smith),
                     ("clerk", clerk), ("gatekeeper", gatekeeper), ("instructor", instructor), ("rookie", rookie), ("bard", bard)]:
        make(name, fn)
    print("ok")


if __name__ == "__main__":
    main()
