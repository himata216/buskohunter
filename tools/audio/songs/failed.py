"""music.failed — クエスト失敗 (one-shot ~6 s). Sad-but-comic 'wah wah wah wahhh':
muted trombone with wah (sweeping band-pass) descending G-F#-F-E, wobbling last
note, droopy harp glissando, tuba + timpani 'bwomp' button."""
import numpy as np

import instruments as I
import dsp
from dsp import N, tt
from sequencer import Song, mel

BPM = 90


def wah_trombone(f, dur, vel=0.85, seed=0, wobble=False):
    y = I.brass(f, dur, vel, 'muted', seed, vib=0 if not wobble else 0, scoop=True, release=0.12)
    n = len(y)
    t = tt(n)
    if wobble:
        # pitch wobble: resample with a vibrato-like read rate
        rate = 1 + 0.035 * np.sin(2 * np.pi * 4.5 * t) * np.clip(t / 0.3, 0, 1)
        pos = np.cumsum(rate)
        y = np.interp(pos, np.arange(n), y, right=0.0)
        fc = 700 + 1100 * (0.5 + 0.5 * np.sin(2 * np.pi * 4.5 * t - 1.2)) * np.clip(t / 0.25, 0, 1)
        fc = fc * np.clip(1.4 - t / (dur + 0.1), 0.4, 1.0)
    else:
        fc = 450 + 1500 * (1 - np.exp(-t / 0.12))
    w = dsp.tvf(y, 'bp', fc, 1.6)
    return 1.6 * w + 0.25 * dsp.lp(y, 900)


def render():
    s = Song(BPM, 2, loop=False, tail=2.0, seed=161, name='failed')
    s.ir = dsp.make_ir(1.5, pre=0.015, hf=0.5, seed=171)
    s.stem('tbn', gain=1.0, pan=0.05, rev=0.22)
    s.stem('harp', gain=1.2, pan=-0.3, rev=0.3)
    s.stem('low', gain=0.9, pan=0.0, rev=0.2)
    s.stem('perc', gain=0.8, pan=0.0, rev=0.25)
    s.stem('strings', gain=0.45, pan=0.1, rev=0.35, width=1.4)

    # droopy harp glissando down (C major-ish to A minor)
    gl = [79, 77, 76, 74, 72, 71, 69, 67, 65, 64, 62, 60]
    s.play('harp', I.harp, [(i * 0.06, 1.0, m, 0.55 - i * 0.02) for i, m in enumerate(gl)])
    for i, (m, ln) in enumerate(((55, 1.0), (54, 1.0), (53, 1.0))):
        s.add('tbn', 0.75 + i, wah_trombone(float(dsp.mtof(m)), s.secs(ln * 0.92), 0.85, i))
    s.add('tbn', 3.75, wah_trombone(float(dsp.mtof(52)), s.secs(2.6), 0.9, 9, wobble=True))
    # soft sad string chords underneath
    for b, chord in ((0.75, (60, 64, 67)), (1.75, (60, 63, 66)), (2.75, (56, 60, 65)), (3.75, (55, 60, 64))):
        s.play('strings', I.strings, [(b, 1.0 if b < 3.7 else 2.6, m, 0.4) for m in chord], attack=0.08, release=0.4)
    # the 'bwomp' button
    T = 6.6
    s.play('low', I.brass, [(T, 0.35, 40, 0.9)], kind='tuba')
    s.play('low', I.brass, [(T, 0.35, 28, 0.8)], kind='tuba')
    s.hit('perc', I.timpani, T, 0.9, f=float(dsp.mtof(40)))
    s.play('harp', I.pizz, [(T, 0.5, 40, 0.8), (T, 0.5, 52, 0.6)])
    return s.mixdown(peak_db=-3.0, drive_db=1.0, max_sec=6.5, fade_sec=1.0)
