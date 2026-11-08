"""ポートレートと不足テクスチャ（proj_sombrero, hunter_book, exit_gate）を作る。"""
from PIL import Image, ImageDraw, ImageEnhance

R = "/home/claude/buskohunter/src/main/resources/assets/buskohunter/textures/"
UP = "/mnt/user-data/uploads/"

# --- ポートレート（アップロード画像 1〜5 → モンスター ID） ---
crops = [
    ("sakeran", "be4359a9-image.png", (0, 0, 1254, 1254)),
    ("barino", "4873baec-image.png", (0, 0, 1024, 1024)),
    ("jangria", "fcc3827b-image.png", (0, 0, 1024, 1024)),
    ("busaiber", "94f36940-image.png", (0, 0, 1024, 1024)),
    ("buskoba", "7be9dc9a-image.jpg", (730, 0, 2170, 1440)),
]
for kid, f, box in crops:
    im = Image.open(UP + f).convert("RGB").crop(box).resize((64, 64), Image.LANCZOS)
    im = ImageEnhance.Contrast(im).enhance(1.08)
    out = im.convert("RGBA")
    # 金の縁
    d = ImageDraw.Draw(out)
    d.rectangle((0, 0, 63, 63), outline=(232, 192, 96, 255))
    d.rectangle((1, 1, 62, 62), outline=(138, 106, 40, 255))
    out.save(R + "gui/portrait_%s.png" % kid)

# --- 投擲ソンブレロ（16x16） ---
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.ellipse((0, 8, 15, 14), fill=(196, 40, 40, 255), outline=(110, 18, 18, 255))
d.ellipse((2, 9, 13, 13), fill=(222, 70, 52, 255))
d.rectangle((5, 3, 10, 10), fill=(226, 196, 120, 255))
d.ellipse((5, 1, 10, 6), fill=(236, 210, 140, 255))
d.line((5, 8, 10, 8), fill=(40, 140, 60, 255))
d.line((5, 9, 10, 9), fill=(250, 220, 60, 255))
for x in (1, 4, 7, 10, 13):
    im.putpixel((x, 11), (250, 220, 60, 255))
im.save(R + "item/proj_sombrero.png")

# --- ハンターノート（16x16） ---
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.rectangle((2, 1, 13, 14), fill=(120, 70, 36, 255), outline=(60, 32, 14, 255))
d.rectangle((3, 2, 4, 13), fill=(90, 50, 24, 255))
d.rectangle((12, 2, 13, 13), fill=(236, 226, 196, 255))
d.rectangle((5, 3, 11, 12), outline=(232, 192, 96, 255))
# 表紙のソンブレロ紋章
d.line((6, 9, 10, 9), fill=(232, 192, 96, 255))
d.rectangle((7, 6, 9, 8), fill=(232, 192, 96, 255))
d.point((8, 5), fill=(255, 230, 140, 255))
im.save(R + "item/hunter_book.png")

# --- 帰還ゲート（16x16、出発ゲートと同じ雰囲気の青版） ---
src = Image.open(R + "block/departure_gate.png").convert("RGBA")
px = src.load()
out = Image.new("RGBA", src.size)
po = out.load()
for y in range(src.size[1]):
    for x in range(src.size[0]):
        r, g, b, a = px[x, y]
        # 赤〜橙系を青〜水色系へ入れ替え
        if r > b + 30:
            po[x, y] = (b, min(255, g + 20), r, a)
        else:
            po[x, y] = (r, g, b, a)
out.save(R + "block/exit_gate.png")
print("ok")
