#!/usr/bin/env python3
"""Verify the built sound set against /home/claude/spec/sounds_spec.md:
every spec key present in sounds.json, every referenced .ogg exists and
decodes, channel count / sample rate / duration sane, loop seams continuous,
subtitle maps complete."""
import json
import os
import re
import subprocess
import sys

import numpy as np

SPEC = '/home/claude/spec/sounds_spec.md'
ROOT = '/home/claude/buskohunter/src/main/resources/assets/buskohunter'
SOUNDS = os.path.join(ROOT, 'sounds')
SUB = {'ja': '/home/claude/spec/subtitles_ja.json', 'en': '/home/claude/spec/subtitles_en.json'}


def parse_spec():
    rows = []
    section = None
    for line in open(SPEC, encoding='utf-8'):
        if line.startswith('## MUSIC'):
            section = 'music'
        elif line.startswith('## SFX'):
            section = 'sfx'
        m = re.match(r'\|\s*([a-z_]+\.[a-z0-9_]+)\s*\|\s*([a-z0-9_/]+)\s*\|\s*(.*?)\|', line)
        if m and section:
            rows.append((section, m.group(1), m.group(2), m.group(3)))
    return rows


def probe(path):
    out = subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'stream=codec_name,channels,sample_rate',
                          '-show_entries', 'format=duration', '-of', 'json', path], capture_output=True, text=True)
    if out.returncode != 0:
        return None
    j = json.loads(out.stdout)
    st = j['streams'][0]
    return st['codec_name'], int(st['channels']), int(st['sample_rate']), float(j['format']['duration'])


def decode(path, ch):
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', path, '-f', 'f32le', '-ac', str(ch), '-'],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, np.float32).reshape(-1, ch)


def main():
    rows = parse_spec()
    errors = []
    try:
        sj = json.load(open(os.path.join(ROOT, 'sounds.json'), encoding='utf-8'))
    except Exception as e:  # noqa
        print('sounds.json INVALID:', e)
        return 1
    subs = {k: json.load(open(p, encoding='utf-8')) for k, p in SUB.items()}
    files = 0
    size = 0
    music_lines = []
    spec_keys = set()
    for section, key, path, desc in rows:
        spec_keys.add(key)
        if key not in sj:
            errors.append(f'missing in sounds.json: {key}')
            continue
        entry = sj[key]
        names = [s['name'] if isinstance(s, dict) else s for s in entry['sounds']]
        expect_base = f'buskohunter:{path}'
        if not all(n == expect_base or re.fullmatch(re.escape(expect_base) + r'_\d+', n) for n in names):
            errors.append(f'{key}: unexpected sound names {names}')
        if section == 'music':
            if 'subtitle' in entry:
                errors.append(f'{key}: music should have no subtitle')
            if not all(isinstance(s, dict) and s.get('stream') for s in entry['sounds']):
                errors.append(f'{key}: music must be stream:true')
        else:
            sub = entry.get('subtitle')
            if sub != f'subtitles.buskohunter.{key}':
                errors.append(f'{key}: bad subtitle {sub}')
            for lang, d in subs.items():
                if sub not in d or not d[sub]:
                    errors.append(f'{key}: subtitle missing in {lang}')
        for n in names:
            p = os.path.join(SOUNDS, n.split(':', 1)[1] + '.ogg')
            if not os.path.exists(p):
                errors.append(f'{key}: file missing {p}')
                continue
            files += 1
            size += os.path.getsize(p)
            pr = probe(p)
            if pr is None:
                errors.append(f'{key}: ffprobe failed {p}')
                continue
            codec, ch, sr, dur = pr
            want_ch = 2 if section == 'music' else 1
            if codec != 'vorbis' or ch != want_ch or sr != 44100:
                errors.append(f'{key}: {codec} {ch}ch {sr}Hz (want vorbis {want_ch}ch 44100)')
            x = decode(p, ch)
            pk = 20 * np.log10(np.max(np.abs(x)) + 1e-12)
            if section == 'music':
                rng = re.findall(r'(\d+)-(\d+)s', desc)
                lo, hi = map(float, rng[0]) if rng else (0, 1e9)
                loop = 'LOOP' in desc
                ok = lo - 0.5 <= dur <= hi + 0.5
                seam = ''
                if loop:
                    a, b = x[-64:], x[:64]
                    jump = float(np.max(np.abs(b[0] - a[-1])))
                    typ = float(np.percentile(np.abs(np.diff(x, axis=0)), 99.9))
                    seam = f'seam jump {jump:.4f} (p99.9 step {typ:.4f})'
                    if jump > typ * 1.5 + 1e-3:
                        errors.append(f'{key}: loop seam discontinuity {jump:.4f}')
                if not ok:
                    errors.append(f'{key}: duration {dur:.1f}s outside {lo}-{hi}s')
                music_lines.append(f'{key:24s} {dur:6.1f}s  {"LOOP" if loop else "one-shot":8s} peak {pk:5.1f} dBFS  {seam}')
            else:
                if pk > -0.2:
                    errors.append(f'{key}: peak {pk:.2f} dBFS too hot')
                m = re.search(r'約\s*([\d.]+)\s*s', desc)
                if m and abs(dur - float(m.group(1))) > 0.6:
                    errors.append(f'{key}: duration {dur:.2f}s vs spec ~{m.group(1)}s')
    extra = set(sj) - spec_keys
    if extra:
        errors.append(f'keys in sounds.json not in spec: {sorted(extra)}')
    on_disk = []
    for dp, dn, fn in os.walk(SOUNDS):
        on_disk += [os.path.join(dp, f) for f in fn if f.endswith('.ogg')]
    print(f'spec keys: {len(spec_keys)} ({sum(1 for r in rows if r[0] == "music")} music, '
          f'{sum(1 for r in rows if r[0] == "sfx")} sfx)')
    print(f'referenced files: {files}, on disk: {len(on_disk)}, total size {size / 1e6:.2f} MB')
    print('\n'.join(music_lines))
    if errors:
        print('\nERRORS:')
        print('\n'.join(errors))
        return 1
    print('\nALL CHECKS PASSED')
    return 0


if __name__ == '__main__':
    sys.exit(main())
