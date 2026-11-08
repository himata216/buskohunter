#!/usr/bin/env python3
"""Build every ブスコハンター sound: renders music (songs/*.py) and SFX (sfx.py),
encodes Ogg Vorbis, writes sounds.json and the subtitle maps.

usage:
  python3 build.py              # everything
  python3 build.py music        # only music
  python3 build.py sfx          # only SFX (+ json files)
  python3 build.py json         # only sounds.json / subtitles
  python3 build.py music.hub weapon.guard   # selected keys
"""
import importlib
import json
import os
import sys
import time
from collections import OrderedDict
from multiprocessing import Pool

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, 'songs'))

import dsp  # noqa: E402

ROOT = '/home/claude/buskohunter/src/main/resources/assets/buskohunter'
SOUNDS = os.path.join(ROOT, 'sounds')
SOUNDS_JSON = os.path.join(ROOT, 'sounds.json')
SUB_JA = '/home/claude/spec/subtitles_ja.json'
SUB_EN = '/home/claude/spec/subtitles_en.json'
NS = 'buskohunter'
MUSIC_Q = 5
SFX_Q = 5

# key, path, module, loop
MUSIC = [
    ('music.hub', 'music/hub', 'hub', True),
    ('music.prepare', 'music/prepare', 'prepare', True),
    ('music.depart', 'music/depart', 'depart', False),
    ('music.field', 'music/field', 'field', True),
    ('music.battle_sakeran', 'music/battle_sakeran', 'battle_sakeran', True),
    ('music.battle_barino', 'music/battle_barino', 'battle_barino', True),
    ('music.battle_jangria', 'music/battle_jangria', 'battle_jangria', True),
    ('music.battle_busaiber', 'music/battle_busaiber', 'battle_busaiber', True),
    ('music.battle_buskoba', 'music/battle_buskoba', 'battle_buskoba', True),
    ('music.clear', 'music/clear', 'clear', False),
    ('music.failed', 'music/failed', 'failed', False),
    ('music.result', 'music/result', 'result', True),
]
# heaviest first so the two workers finish together
MUSIC_ORDER = ['battle_barino', 'battle_jangria', 'battle_sakeran', 'field', 'battle_buskoba', 'battle_busaiber',
               'prepare', 'hub', 'result', 'clear', 'depart', 'failed']


def ogg_path(path):
    return os.path.join(SOUNDS, path + '.ogg')


def render_music(item):
    key, path, mod, loop = item
    t = time.time()
    m = importlib.import_module(mod)
    y = m.render()
    dsp.save_ogg(y, ogg_path(path), q=MUSIC_Q, channels=2, target_peak_db=-3.0, tol_db=0.3)
    return f'{key:26s} {len(y) / dsp.SR:6.1f}s  ({time.time() - t:4.1f}s render)'


def sfx_files(path, nv):
    return [path] if nv == 1 else [f'{path}_{i + 1}' for i in range(nv)]


def render_sfx(idx):
    import sfx
    key, path, fn, nv, ja, en = sfx.REGISTRY[idx]
    out = []
    for v, p in enumerate(sfx_files(path, nv)):
        y = dsp.finish_sfx(fn(v), peak_db=-1.0)
        dsp.save_ogg(y, ogg_path(p), q=SFX_Q, channels=1, target_peak_db=-1.0, tol_db=0.3)
        out.append(f'{p} {len(y) / dsp.SR:.2f}s')
    return f'{key:30s} ' + ', '.join(out)


def write_json():
    import sfx
    data = OrderedDict()
    for key, path, mod, loop in MUSIC:
        data[key] = {'sounds': [{'name': f'{NS}:{path}', 'stream': True}]}
    ja, en = OrderedDict(), OrderedDict()
    for key, path, fn, nv, sja, sen in sfx.REGISTRY:
        sub = f'subtitles.{NS}.{key}'
        data[key] = {'subtitle': sub, 'sounds': [f'{NS}:{p}' for p in sfx_files(path, nv)]}
        ja[sub] = sja
        en[sub] = sen
    os.makedirs(ROOT, exist_ok=True)
    with open(SOUNDS_JSON, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write('\n')
    for p, d in ((SUB_JA, ja), (SUB_EN, en)):
        with open(p, 'w', encoding='utf-8') as f:
            json.dump(d, f, ensure_ascii=False, indent=2)
            f.write('\n')
    print(f'wrote {SOUNDS_JSON} ({len(data)} events), {SUB_JA}, {SUB_EN}')


def main(argv):
    import sfx
    os.makedirs('/tmp/bh_audio', exist_ok=True)
    want = set(argv)
    do_all = not want
    t0 = time.time()
    music_items = [m for m in MUSIC if do_all or 'music' in want or m[0] in want]
    music_items.sort(key=lambda m: MUSIC_ORDER.index(m[2]))
    sfx_items = [i for i, s in enumerate(sfx.REGISTRY) if do_all or 'sfx' in want or s[0] in want]
    if want == {'json'}:
        music_items, sfx_items = [], []
    if music_items:
        with Pool(2) as pool:
            for line in pool.imap_unordered(render_music, music_items):
                print('  ' + line, flush=True)
    if sfx_items:
        with Pool(2) as pool:
            for line in pool.imap(render_sfx, sfx_items):
                print('  ' + line, flush=True)
    write_json()
    print(f'done in {time.time() - t0:.1f}s')


if __name__ == '__main__':
    main(sys.argv[1:])
