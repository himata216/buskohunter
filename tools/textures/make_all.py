"""Generate every ブスコハンター texture + preview sheets, then verify against the spec.

    python3 /home/claude/tools/textures/make_all.py
"""
import os
import re
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import gen_armor  # noqa: E402
import gen_blocks  # noqa: E402
import gen_gui  # noqa: E402
import gen_icon  # noqa: E402
import gen_items  # noqa: E402
import gen_weapons  # noqa: E402
import preview  # noqa: E402
from pixlib import CREATED, ROOT, TEX, TOOLS  # noqa: E402

SPEC = '/home/claude/spec/textures_spec.md'

WEAPON_TYPES = ['great_sword', 'long_sword', 'hammer', 'gunlance', 'lance', 'light_bowgun', 'heavy_bowgun',
                'bow', 'insect_glaive', 'switch_axe', 'charge_blade', 'dual_blades', 'sword_and_shield',
                'hunting_horn']
CONSUMABLES = ['potion', 'mega_potion', 'max_potion', 'antidote', 'energy_drink', 'demondrug', 'armorskin',
               'ration', 'well_done_steak', 'whetstone', 'paintball', 'flash_bomb', 'shock_trap', 'pitfall_trap',
               'tranq_bomb', 'barrel_bomb', 'farcaster']
PROJ = ['proj_bullet', 'proj_arrow', 'proj_sake_bottle', 'proj_fireball', 'proj_water_orb', 'proj_missile',
        'proj_holy_orb', 'proj_glitch_cube', 'proj_kinsect', 'proj_shell']
TROPHIES = ['trophy_sakeran', 'trophy_barino', 'trophy_jangria', 'trophy_busaiber', 'trophy_buskoba']
SERIES = ['hunter', 'sakeran', 'barino', 'jangria', 'busaiber', 'buskoba']
PIECES = ['helmet', 'chestplate', 'leggings', 'boots']
BLOCKS = ['quest_board_front', 'quest_board_side', 'canteen_top', 'canteen_side', 'item_box_top', 'item_box_side',
          'equipment_box_top', 'equipment_box_side', 'departure_gate', 'supply_box_top', 'supply_box_side',
          'hunter_bed_top', 'hunter_bed_side']
ELEMENTS = ['fire', 'water', 'thunder', 'ice', 'dragon', 'poison', 'paralysis', 'sleep', 'blast', 'buskoba', 'none']
STATUS = ['fireblight', 'waterblight', 'thunderblight', 'iceblight', 'dragonblight', 'poison', 'sleep', 'paralysis',
          'stun', 'drunk', 'attack_up', 'defense_up', 'speed_up', 'heal']


def expected():
    e = {}
    for w in WEAPON_TYPES:
        e[f'item/{w}.png'] = (32, 32)
        e[f'item/{w}_overlay.png'] = (32, 32)
    for n in CONSUMABLES + PROJ + TROPHIES:
        e[f'item/{n}.png'] = (16, 16)
    for s in SERIES:
        for p in PIECES:
            e[f'item/{s}_{p}.png'] = (16, 16)
        e[f'entity/equipment/humanoid/{s}.png'] = (64, 32)
        e[f'entity/equipment/humanoid_leggings/{s}.png'] = (64, 32)
    for b in BLOCKS:
        e[f'block/{b}.png'] = (16, 16)
    for el in ELEMENTS:
        e[f'gui/element_{el}.png'] = (16, 16)
    for w in WEAPON_TYPES:
        e[f'gui/weapon_{w}.png'] = (16, 16)
    for s in STATUS:
        e[f'gui/status_{s}.png'] = (12, 12)
    e['gui/map_camp.png'] = (12, 12)
    e['gui/map_monster.png'] = (12, 12)
    e['gui/map_player.png'] = (8, 8)
    return e


def spec_names():
    """Every snake_case texture name mentioned in the spec (for a cross-check)."""
    txt = open(SPEC, encoding='utf-8').read()
    return set(re.findall(r'\b([a-z]+(?:_[a-z]+)+)\b', txt))


def main():
    # 1. weapons
    weapons = gen_weapons.build()
    gen_weapons.write(weapons)
    preview.weapon_sheet(weapons, os.path.join(TOOLS, 'preview_weapons.png'))
    # 2. items
    items = gen_items.build()
    gen_items.write(items)
    # 3. armour icons + worn layers
    icons = gen_armor.icons()
    worn = gen_armor.worn()
    gen_armor.write_all()
    preview.armor_sheet(worn, os.path.join(TOOLS, 'preview_armor.png'))
    # 4. blocks
    blocks = gen_blocks.build()
    gen_blocks.write(blocks)
    # 5. gui
    gui = gen_gui.build(weapons)
    gen_gui.write(gui)
    # 6. mod icon
    gen_icon.write()

    # extra review sheets
    preview.grid_sheet(list(items.items()) + list(icons.items()), os.path.join(TOOLS, 'preview_items.png'),
                       scale=6, cols=10)
    bl = []
    for k, a in blocks.items():
        bl.append((k, a))
        bl.append((k + ' (2x2 tiled)', np.tile(a, (2, 2, 1))))
    preview.grid_sheet(bl, os.path.join(TOOLS, 'preview_blocks.png'), scale=5, cols=6)
    preview.grid_sheet(list(gui.items()), os.path.join(TOOLS, 'preview_gui.png'), scale=6, cols=10,
                       bg=(30, 30, 36))

    # verification
    exp = expected()
    names = spec_names()
    problems = []
    for rel, (w, h) in exp.items():
        p = os.path.join(TEX, rel)
        if not os.path.exists(p):
            problems.append(f'MISSING {rel}')
            continue
        im = Image.open(p)
        if im.size != (w, h) or im.mode != 'RGBA':
            problems.append(f'BAD {rel}: {im.size} {im.mode}')
    icon = os.path.join(ROOT, 'icon.png')
    if Image.open(icon).size != (128, 128):
        problems.append('BAD icon.png')
    # names in the spec that look like texture ids should all be produced
    produced = {os.path.splitext(os.path.basename(r))[0] for r in exp}
    produced |= {f'element_{e}' for e in ELEMENTS} | {f'status_{s}' for s in STATUS}
    for n in sorted(names):
        if (n.startswith(('proj_', 'trophy_', 'map_')) or n in CONSUMABLES or n in BLOCKS or n in WEAPON_TYPES) \
                and n not in produced:
            problems.append(f'spec name not produced: {n}')
    # weapon layer rules
    for w in WEAPON_TYPES:
        b = np.asarray(Image.open(os.path.join(TEX, f'item/{w}.png')))
        o = np.asarray(Image.open(os.path.join(TEX, f'item/{w}_overlay.png')))
        if ((b[..., 3] > 0) & (o[..., 3] > 0)).any():
            problems.append(f'{w}: base/overlay overlap')
        op = o[o[..., 3] > 0]
        if not ((op[:, 0] == op[:, 1]) & (op[:, 1] == op[:, 2])).all():
            problems.append(f'{w}: overlay not grayscale')
    all_files = sorted(set(p for p, _, _ in CREATED))
    print(f'{len(all_files)} files written')
    for p in all_files:
        im = Image.open(p)
        print(f'  {os.path.relpath(p, ROOT)}  {im.size[0]}x{im.size[1]}')
    print('expected from spec:', len(exp), '+ icon.png')
    print('problems:', problems or 'none')
    return 0 if not problems else 1


if __name__ == '__main__':
    sys.exit(main())
