"""Preview sheet helpers (weapons with every element tint, armor flattened)."""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont

from pixlib import C, TOOLS, alpha_over, checker, composite, upscale

TINTS = [
    ('fire', '#FF6A2A'), ('water', '#3AA8FF'), ('thunder', '#FFE03A'), ('ice', '#A8F0FF'),
    ('dragon', '#B03040'), ('poison', '#B060E0'), ('paralysis', '#F0D020'),
    ('sleep', '#8090FF'), ('blast', '#FF9A30'), ('buskoba', '#FF70C0'),
]


def font(sz=12):
    for p in ('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
              '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'):
        if os.path.exists(p):
            return ImageFont.truetype(p, sz)
    return ImageFont.load_default()


def on_bg(arr, s, bg=None):
    h, w = arr.shape[:2]
    up = upscale(arr, s)
    b = checker(w * s, h * s, s * 4) if bg is None else bg
    return alpha_over(b, up)


def weapon_sheet(out, path, scale=4):
    cols = ['base', 'overlay', 'untinted'] + [t[0] for t in TINTS]
    cell = 32 * scale
    pad, lw, th = 6, 130, 18
    W = lw + len(cols) * (cell + pad) + pad
    H = th + len(out) * (cell + pad) + pad
    sheet = Image.new('RGBA', (W, H), (28, 28, 34, 255))
    d = ImageDraw.Draw(sheet)
    f = font(11)
    for j, c in enumerate(cols):
        d.text((lw + pad + j * (cell + pad), 3), c, fill=(230, 230, 230, 255), font=f)
    for i, (name, (base, over)) in enumerate(out.items()):
        y = th + pad + i * (cell + pad)
        d.text((6, y + cell // 2 - 6), name, fill=(240, 220, 160, 255), font=f)
        imgs = [base, over, composite(base, over)] + [composite(base, over, C(h)) for _, h in TINTS]
        for j, im in enumerate(imgs):
            x = lw + pad + j * (cell + pad)
            sheet.paste(Image.fromarray(on_bg(im, scale)), (x, y))
    sheet.save(path)
    return path


def grid_sheet(items, path, scale=8, cols=8, bg=None, label=True):
    """items: list of (name, rgba array)."""
    cw = max(a.shape[1] for _, a in items) * scale
    ch = max(a.shape[0] for _, a in items) * scale
    pad, th = 8, 16 if label else 0
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * (cw + pad) + pad, rows * (ch + th + pad) + pad), (28, 28, 34, 255))
    d = ImageDraw.Draw(sheet)
    f = font(10)
    for k, (name, a) in enumerate(items):
        r, c = divmod(k, cols)
        x = pad + c * (cw + pad)
        y = pad + r * (ch + th + pad)
        img = on_bg(a, scale, bg=None if bg is None else np.tile(np.array(bg + (255,), np.uint8), (a.shape[0] * scale, a.shape[1] * scale, 1)))
        sheet.paste(Image.fromarray(img), (x, y + th))
        if label:
            d.text((x, y), name, fill=(230, 230, 230, 255), font=f)
    sheet.save(path)
    return path


# ---------------------------------------------------------------------------
def _crop(a, rect):
    x, y, w, h = rect
    return a[y:y + h, x:x + w]


def _paste(dst, src, x, y):
    h, w = src.shape[:2]
    region = dst[y:y + h, x:x + w]
    dst[y:y + h, x:x + w] = alpha_over(region, src)


def paper_doll(l1, l2, view='front'):
    """Assemble a simple flat paper doll (20 x 32 px) from box faces."""
    from gen_armor import HEAD, HAT, BODY, ARM, LEG
    W, H = 16, 32
    out = np.zeros((H, W, 4), np.uint8)
    # mannequin underneath so transparent armour parts read as 'body'
    skin = np.array([150, 150, 160, 255], np.uint8)
    dark = np.array([110, 110, 122, 255], np.uint8)
    out[0:8, 4:12] = skin
    out[8:20, 4:12] = dark
    out[8:20, 0:4] = skin
    out[8:20, 12:16] = skin
    out[20:32, 4:12] = dark
    if view in ('front', 'back'):
        f = view
        legs = [(l2, LEG[f]), (l1, LEG[f])]
        for lay, rect in legs:
            leg = _crop(lay, rect)
            if f == 'front':
                _paste(out, leg, 4, 20)
                _paste(out, leg[:, ::-1], 8, 20)
            else:
                _paste(out, leg, 4, 20)
                _paste(out, leg[:, ::-1], 8, 20)
        _paste(out, _crop(l2, BODY[f]), 4, 8)
        _paste(out, _crop(l1, BODY[f]), 4, 8)
        arm = _crop(l1, ARM[f])
        _paste(out, arm, 0, 8)
        _paste(out, arm[:, ::-1], 12, 8)
        _paste(out, _crop(l1, HEAD[f]), 4, 0)
        _paste(out, _crop(l1, HAT[f]), 4, 0)
    return out


def armor_sheet(worn, path, scale=8):
    from gen_armor import HEAD, HAT, BODY, ARM, LEG
    f = font(12)
    fs = font(10)
    cellw = 64 * scale
    dollw = 16 * scale
    rowh = 32 * scale + 26
    W = 150 + 2 * (cellw + 12) + 2 * (dollw + 12) + 12
    Hh = 30 + len(worn) * (rowh + 10)
    sheet = Image.new('RGBA', (W, Hh), (28, 28, 34, 255))
    d = ImageDraw.Draw(sheet)
    heads = ['humanoid/<s>.png (helmet+chest+boots)', 'humanoid_leggings/<s>.png', 'front', 'back']
    xs = [150, 150 + cellw + 12, 150 + 2 * (cellw + 12), 150 + 2 * (cellw + 12) + dollw + 12]
    for x, t in zip(xs, heads):
        d.text((x, 8), t, fill=(230, 230, 230, 255), font=f)
    boxes = [('head', HEAD), ('hat', HAT), ('body', BODY), ('arm', ARM), ('leg', LEG)]
    for r, (name, (l1, l2)) in enumerate(worn.items()):
        y = 30 + r * (rowh + 10)
        d.text((10, y + rowh // 2 - 8), name, fill=(240, 220, 160, 255), font=f)
        for k, lay in enumerate((l1, l2)):
            img = Image.fromarray(on_bg(lay, scale, checker(64 * scale, 32 * scale, scale * 2)))
            dd = ImageDraw.Draw(img)
            for bname, B in boxes:
                for face, (fx, fy, fw, fh) in B.items():
                    dd.rectangle([fx * scale, fy * scale, (fx + fw) * scale - 1, (fy + fh) * scale - 1],
                                 outline=(255, 255, 0, 110))
            sheet.paste(img, (xs[k], y))
        for k, view in enumerate(('front', 'back')):
            doll = paper_doll(l1, l2, view)
            img = Image.fromarray(on_bg(doll, scale, np.tile(np.array([44, 44, 52, 255], np.uint8), (32 * scale, 16 * scale, 1))))
            sheet.paste(img, (xs[2 + k], y))
    sheet.save(path)
    return path
