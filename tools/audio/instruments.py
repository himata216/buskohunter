"""Instrument voices. Every function returns a mono float64 array
(note + its natural release). Signature convention: (f, dur, vel, seed, **kw)
where f is Hz, dur the held time in seconds, vel in 0..1."""
import math

import numpy as np

from dsp import (SR, TAU, N, tt, rng, cents, db, adsr, env, perc, expdec, fade, vibrato, phase,
                 osc, saw, square, sine, tri, supersaw, harm_sum, fm, partials, white, pink, brown,
                 crackle, lp, hp, bp, biquad, peq, lshelf, hshelf, onepole_lp, tvf, tv_lp, tv_bp,
                 resonator, ks, formant, formant_tv, sat, asym_sat, smooth_noise, glide, lfo, pad_to,
                 mix_at)


def _amp(vel, curve=1.4):
    return max(vel, 0.0) ** curve


# ==========================================================================
# winds
# ==========================================================================
def flute(f, dur, vel=0.7, seed=0, kind='flute', vib=None, breath=None, scoop=True):
    """concert flute / ocarina / shinobue (Japanese bamboo flute)"""
    cfg = {
        'flute':    dict(h=[1.0, 0.42, 0.16, 0.07, 0.035, 0.02], br=0.05, a=0.045, r=0.12, vd=14, vr=5.2, sc=25),
        'ocarina':  dict(h=[1.0, 0.07, 0.025, 0.01], br=0.06, a=0.035, r=0.10, vd=9, vr=5.6, sc=15),
        'shinobue': dict(h=[1.0, 0.55, 0.3, 0.16, 0.09, 0.05, 0.03], br=0.12, a=0.03, r=0.10, vd=22, vr=6.0, sc=90),
        'piccolo':  dict(h=[1.0, 0.25, 0.08, 0.03], br=0.04, a=0.03, r=0.08, vd=12, vr=5.8, sc=20),
    }[kind]
    r = rng(seed)
    n = N(dur + cfg['r'])
    t = tt(n)
    fa = vibrato(f, n, rate=cfg['vr'], depth_c=cfg['vd'] if vib is None else vib,
                 delay=0.22, ramp=0.35, seed=seed, jitter_c=2.5)
    if scoop:
        fa = fa * cents(-cfg['sc'] * np.exp(-t / (0.05 if kind == 'shinobue' else 0.03)))
    ph, _ = phase(fa, n, r.uniform())
    hv = np.array(cfg['h'])
    bright = 0.6 + 0.6 * vel
    amps = [hv[0]] + [h * bright ** (k + 1) for k, h in enumerate(hv[1:])]
    kmax = int(0.45 * SR / (f * 1.02))
    y = harm_sum(ph, amps, kmax=max(1, kmax))
    e = adsr(dur, cfg['a'], 0.15, 0.85, cfg['r'], n)
    # amplitude tremolo coupled with vibrato
    y = y * e * (1 + 0.04 * np.sin(TAU * cfg['vr'] * t))
    brv = cfg['br'] if breath is None else breath
    nz = bp(white(n, seed + 11), min(f * 1.5, 9000), min(f * 6 + 2500, 16000), 2)
    chiff = np.exp(-t / 0.025) * 2.5
    y += brv * nz * (e + chiff * (t < 0.12)) * (0.5 + vel)
    return y * _amp(vel, 1.1) * 0.5


def brass(f, dur, vel=0.8, kind='horn', seed=0, vib=None, scoop=True, attack=None, release=None,
          bright=1.0, fall=0.0):
    """additive brass: harmonic count follows a brightness envelope (brass 'bloom')."""
    P = {
        'horn':     dict(a=0.06, r=0.22, nb=5.5, tilt=0.55, fc=1100, pk=(450, 3), vd=6, vr=5.0, sc=35, br=0.012),
        'trumpet':  dict(a=0.025, r=0.14, nb=13.0, tilt=0.3, fc=3800, pk=(1400, 6), vd=10, vr=5.5, sc=30, br=0.01),
        'mariachi': dict(a=0.022, r=0.16, nb=14.0, tilt=0.28, fc=4200, pk=(1500, 7), vd=28, vr=6.4, sc=45, br=0.012),
        'trombone': dict(a=0.04, r=0.2, nb=9.0, tilt=0.4, fc=1900, pk=(600, 4), vd=5, vr=4.8, sc=30, br=0.012),
        'tuba':     dict(a=0.06, r=0.25, nb=4.0, tilt=0.6, fc=650, pk=(250, 3), vd=3, vr=4.5, sc=25, br=0.01),
        'muted':    dict(a=0.03, r=0.14, nb=9.0, tilt=0.35, fc=2600, pk=(1800, 9), vd=8, vr=5.0, sc=40, br=0.01),
    }[kind]
    r = rng(seed)
    a = attack if attack is not None else P['a'] * (1.35 - 0.6 * vel)
    rel = release if release is not None else P['r']
    n = N(dur + rel)
    t = tt(n)
    e = adsr(dur, a, 0.3, 0.8, rel, n)
    eb = adsr(dur, a * 1.8, 0.45, 0.72, rel * 0.7, n)
    b = (eb ** 1.3) * (0.3 + 0.7 * vel) * bright
    b = b + 0.35 * vel * np.exp(-t / 0.05) * (P['nb'] > 8)  # trumpet 'blat'
    fa = vibrato(f, n, rate=P['vr'], depth_c=P['vd'] if vib is None else vib, delay=0.28, ramp=0.4,
                 seed=seed, jitter_c=2.5)
    if scoop:
        fa = fa * cents(-P['sc'] * np.exp(-t / 0.04))
    if fall:
        fa = fa * cents(-fall * np.clip((t - dur * 0.6) / max(dur * 0.4 + rel, 1e-3), 0, 1) ** 2)
    ph, _ = phase(fa, n, r.uniform())
    K = int(min(36, 0.44 * SR / (f * 1.05)))
    c = 0.45 + P['nb'] * b
    rr = np.exp(-1.0 / c)
    th = TAU * ph
    s1 = np.sin(th)
    c2 = 2 * np.cos(th)
    sp = np.zeros(n)
    sc = s1
    cur = np.ones(n)
    y = np.zeros(n)
    pf, pg = P['pk']
    for k in range(1, K + 1):
        fk = f * k
        w = k ** (-P['tilt']) / math.sqrt(1 + (fk / P['fc']) ** 4)
        w *= 1 + (db(pg) - 1) * math.exp(-((math.log2(fk / pf)) ** 2) / 0.5)
        if k > 1:
            sn = c2 * sc - sp
            sp, sc = sc, sn
        y += w * cur * sc
        cur = cur * rr
    nz = bp(white(n, seed + 5), 900, 3500, 2)
    y = y + P['br'] * nz * e * 8 * b
    y = y * e
    return y * _amp(vel, 1.2) * 0.35


def brass_section(f, dur, vel=0.8, kind='horn', seed=0, players=2, spread_c=7, **kw):
    out = None
    r = rng(seed + 99)
    for p in range(players):
        d = cents(r.uniform(-spread_c, spread_c)) if p else 1.0
        off = N(r.uniform(0.0, 0.018)) if p else 0
        y = brass(f * d, dur, vel * (1 - 0.05 * p), kind, seed + 17 * p, **kw)
        if out is None:
            out = np.zeros(len(y) + N(0.02))
        mix_at(out, y, off)
    return out / math.sqrt(players)


def vocal_src(f, n, seed=0, jitter=0.01, vib_rate=5.3, vib_c=15, delay=0.2):
    """glottal-ish source: saw softened + per-period jitter"""
    r = rng(seed)
    fa = vibrato(f, n, rate=vib_rate * r.uniform(0.92, 1.08), depth_c=vib_c, delay=delay, ramp=0.4,
                 seed=seed, jitter_c=3)
    fa = fa * (1 + jitter * smooth_noise(n, 25, seed + 3))
    y = saw(fa, n, r.uniform())
    y = onepole_lp(y, 2600)
    return y


def choir_src(f, dur, vel=0.7, seed=0, voices=3, attack=0.25, release=0.5, breath=0.04):
    """unfiltered choir source — apply formant() on the stem (LTI)"""
    n = N(dur + release)
    r = rng(seed)
    y = np.zeros(n)
    for v in range(voices):
        d = cents(r.uniform(-10, 10))
        y += vocal_src(f * d, n, seed + 31 * v, vib_c=r.uniform(10, 22))
    y /= math.sqrt(voices)
    y += breath * hp(white(n, seed + 7), 1500, 2)
    e = adsr(dur, attack, 0.3, 0.9, release, n, curve=1.5)
    return y * e * _amp(vel, 1.0)


def choir_stab(f, dur, vel=0.9, seed=0, voices=4):
    return choir_src(f, dur, vel, seed, voices, attack=0.02, release=0.18, breath=0.07)


def shout(f0, dur, path, voice='tenor', seed=0, breath=0.25, contour=(1.0, 1.1, 0.8),
          h_onset=0.0, jitter=0.03, shift=1.0, rough=0.0):
    """synthetic vocal shout (kakegoe / grunt). path = [(t, vowel), ...]"""
    n = N(dur)
    t = tt(n)
    r = rng(seed)
    c = np.interp(t, [0, dur * 0.35, dur], list(contour))
    fa = f0 * c * (1 + jitter * smooth_noise(n, 30, seed))
    src = saw(fa, n, r.uniform())
    if rough:
        src = src * (1 + rough * square(fa * 0.5, n))
    src = onepole_lp(src, 3500)
    voiced = np.clip((t - h_onset) / 0.02, 0, 1)
    asp = hp(white(n, seed + 1), 400, 2)
    x = src * voiced + asp * (breath + 0.8 * (t < h_onset + 0.02) * (h_onset > 0))
    y = formant_tv(x, path, voice, shift=shift)
    e = env([(0, 0), (0.012, 1), (dur * 0.55, 0.8), (dur, 0)], n)
    return y * e


# ==========================================================================
# strings
# ==========================================================================
def strings(f, dur, vel=0.7, seed=0, sect='vln', attack=None, release=None, tremolo=0.0,
            voices=5, bright=1.0, marc=False, vib=None, gliss=0.0):
    cfg = {'vln': (2600, 650, 3000), 'vla': (2200, 420, 2400), 'vc': (1800, 230, 1600),
           'cb': (1100, 110, 900)}[sect]
    fc0, body, fpk = cfg
    a = attack if attack is not None else (0.025 if marc else 0.14)
    rel = release if release is not None else (0.18 if marc else 0.35)
    n = N(dur + rel)
    t = tt(n)
    r = rng(seed)
    y = np.zeros(n)
    for v in range(voices):
        d = cents(r.uniform(-6, 6))
        fa = vibrato(f * d, n, rate=r.uniform(4.6, 6.0), depth_c=(r.uniform(9, 17) if vib is None else vib),
                     delay=0.12, ramp=0.35, seed=seed + 13 * v, jitter_c=3)
        if gliss:
            fa = fa * 2 ** (gliss * np.clip(t / max(dur, 1e-3), 0, 1) ** 2 / 12)
        y += saw(fa, n, r.uniform())
    y /= math.sqrt(voices)
    y += 0.05 * bp(white(n, seed + 3), 2500, 7000, 2) * (0.5 + vel)
    fc = (fc0 * (0.55 + 0.9 * vel)) * bright
    y = lp(y, min(fc, 12000), 2)
    y = lp(y, min(fc * 2.2, 16000), 1)
    y = peq(y, body, 3.0, 1.2)
    y = peq(y, fpk, 2.0, 1.4)
    e = adsr(dur, a, 0.2 if marc else 0.3, 0.7 if marc else 0.92, rel, n, curve=1.6)
    if marc:
        e = e * (1 + 0.5 * np.exp(-t / 0.06))
    if tremolo:
        rate = 13.0 + 1.5 * smooth_noise(n, 2, seed + 4)
        ph = np.cumsum(rate) / SR
        tr = 0.5 + 0.5 * np.sin(TAU * ph)
        e = e * (1 - tremolo * tr ** 2)
    return y * e * _amp(vel, 1.1) * 0.5


def pizz(f, dur, vel=0.7, seed=0, t60=None, bright=0.55):
    t60 = t60 if t60 else float(np.clip(0.9 * (220 / f) ** 0.35, 0.25, 1.2))
    ln = min(dur, t60) + 0.25
    y = ks(f, ln, t60=t60, a=0.52, bright=bright, pick=0.33, seed=seed)
    y = lp(y, 3200, 2)
    y = peq(y, 380, 3, 1.0)
    n = len(y)
    e = env([(0, 1), (ln - 0.08, 1), (ln, 0)], n)
    th = sine(f, n) * np.exp(-tt(n) / 0.06) * 0.2
    return (y + th) * e * _amp(vel, 1.3) * 0.7


def upright(f, dur, vel=0.75, seed=0, t60=1.2, arco=False):
    """pizzicato double bass / acoustic bass: strong fundamental + KS texture"""
    ln = min(dur, t60 * 1.5) + 0.12
    n = N(ln)
    t = tt(n)
    if arco:
        y = strings(f, dur, vel, seed, 'cb', attack=0.08, release=0.25, voices=3)
        return pad_to(y, len(y))
    fa = f * (1 + 0.012 * np.exp(-t / 0.03))
    ph = np.cumsum(fa) / SR
    y = np.zeros(n)
    for k, (a, d) in enumerate([(1.0, 1.0), (0.55, 0.6), (0.28, 0.4), (0.14, 0.25), (0.07, 0.15)]):
        if f * (k + 1) < 8000:
            y += a * np.sin(TAU * (k + 1) * ph) * np.exp(-t / (t60 * d * 0.45))
    y = y * np.clip(t / 0.004, 0, 1)
    y += 0.35 * ks(f, ln, t60=t60 * 0.6, a=0.52, bright=0.4, pick=0.25, seed=seed, n_out=n)
    y += 0.12 * lp(white(n, seed + 3), 900, 2) * np.exp(-t / 0.015)
    e = env([(0, 1), (max(ln - 0.1, 0.01), 1), (ln, 0)], n)
    return lp(y, 2500, 2) * e * _amp(vel, 1.2) * 0.55


def harp(f, dur, vel=0.7, seed=0, t60=None):
    t60 = t60 if t60 else float(np.clip(3.2 * (220 / f) ** 0.4, 0.8, 5.0))
    ln = min(max(dur, 0.5) + 1.2, t60 * 0.9)
    y = ks(f, ln, t60=t60, a=0.58, bright=0.5, pick=0.3, seed=seed)
    n = len(y)
    fund = sine(f, n) * np.exp(-tt(n) / (t60 * 0.25)) * 0.35
    y = y + fund
    y = lp(y, 5500, 2)
    y = hp(y, 60, 1)
    e = env([(0, 1), (ln - 0.25, 1), (ln, 0)], n, 'cos')
    return y * e * _amp(vel, 1.2) * 0.6


def nylon(f, dur, vel=0.7, seed=0, t60=1.6, mute=False):
    t60 = 0.18 if mute else t60
    ln = min(dur + 0.15, t60 * 1.1) if mute else min(dur + 0.6, t60 * 1.2)
    y = ks(f, ln, t60=t60, a=0.6, bright=0.62 + 0.2 * vel, pick=0.14, seed=seed)
    y = lp(y, 4800, 2)
    y = peq(y, 105, 3, 1.2)
    y = peq(y, 230, 2, 1.5)
    y = peq(y, 2800, 2, 1.0)
    n = len(y)
    e = env([(0, 1), (ln - 0.06, 1), (ln, 0)], n)
    return y * e * _amp(vel, 1.2) * 0.6


def strum(freqs, dur, vel=0.7, seed=0, spread=0.012, down=True, mute=False, inst=None):
    inst = inst or nylon
    fs = list(freqs) if down else list(freqs)[::-1]
    out = np.zeros(N(dur + 1.5))
    r = rng(seed)
    for i, f in enumerate(fs):
        v = vel * (1 - 0.06 * i) * r.uniform(0.85, 1.0)
        y = inst(f, dur, v, seed + i * 7, mute=mute)
        mix_at(out, y, N(i * spread * r.uniform(0.7, 1.3)))
    # pick noise
    nz = bp(white(N(0.05), seed), 2000, 8000) * expdec(N(0.05), 0.01) * 0.08 * vel
    mix_at(out, nz, 0)
    return out


def shamisen(f, dur, vel=0.8, seed=0, t60=0.9, slide=0.0):
    ln = min(dur + 0.3, t60 * 1.3)
    y = ks(f, ln, t60=t60, a=0.86, bright=0.9, pick=0.07, seed=seed)
    n = len(y)
    t = tt(n)
    if slide:
        # pitch slide via variable-rate resampling
        sp = cents(slide * np.exp(-t / 0.05))
        pos = np.cumsum(sp)
        y = np.interp(pos, np.arange(n), y, right=0.0)
    # sawari buzz: asymmetric non-linearity on the string, band-limited
    buzz = asym_sat(y * 3.0, 1.5, 0.35)
    buzz = bp(buzz, 1800, 6500, 2)
    y = y + 0.45 * buzz
    y = peq(y, 2600, 4, 1.0)
    y = hp(y, 120, 2)
    # bachi (plectrum) strike on the skin
    skin = sine(210 * (1 + 0.3 * np.exp(-t / 0.01)), n) * np.exp(-t / 0.035)
    click = hp(white(n, seed + 2), 2500, 2) * np.exp(-t / 0.004)
    y = y + 0.6 * skin * vel + 0.5 * click * vel
    e = env([(0, 1), (ln - 0.05, 1), (ln, 0)], n)
    return y * e * _amp(vel, 1.2) * 0.5


def guitar_chug(f, dur, vel=0.9, seed=0, palm=True, power=(0, 7, 12), drive=22.0, detune=0.0):
    ln = dur + (0.04 if palm else 0.2)
    n = N(ln)
    y = np.zeros(n)
    for i, iv in enumerate(power):
        fr = f * 2 ** (iv / 12) * cents(detune * (i - 1))
        y += ks(fr, ln, t60=0.35 if palm else 2.5, a=0.8, bright=0.9, pick=0.11, seed=seed + i, n_out=n)
    if palm:
        y = lp(y, 1100, 2)
    y = hp(y, 85, 2)
    y = peq(y, 750, 6, 0.8)
    y = sat(y * drive * vel, 1.0)
    y = asym_sat(y, 1.2, 0.1)
    # cabinet
    y = lp(y, 5200, 4)
    y = peq(y, 110, 3, 1.0)
    y = peq(y, 420, -5, 1.2)
    y = peq(y, 1900, 3, 1.2)
    y = hp(y, 65, 2)
    e = env([(0, 1), (dur, 0.9 if palm else 1.0), (ln, 0)], n)
    return y * e * 0.35


def guitar_lead(f, dur, vel=0.9, seed=0, drive=14.0, vib=18):
    """distorted sustained lead (tremolo-pick friendly)"""
    n = N(dur + 0.12)
    fa = vibrato(f, n, rate=6.0, depth_c=vib, delay=0.18, ramp=0.25, seed=seed, jitter_c=3)
    y = saw(fa, n) + 0.5 * saw(fa * cents(7), n, 0.3)
    y = y * adsr(dur, 0.005, 0.1, 0.8, 0.1, n)
    y = hp(y, 150, 1)
    y = sat(y * drive * vel, 1.0)
    y = lp(y, 4800, 4)
    y = peq(y, 1500, 4, 1.0)
    y = peq(y, 400, -4, 1.0)
    return y * 0.3


# ==========================================================================
# keyboards / mallets / bells
# ==========================================================================
ORGAN_REG = {
    'principal': [(1, 1.0), (2, 0.55), (3, 0.22), (4, 0.33), (6, 0.15), (8, 0.13), (10, 0.05), (12, 0.05)],
    'full':      [(0.5, 0.55), (1, 1.0), (2, 0.7), (3, 0.35), (4, 0.45), (5, 0.12), (6, 0.25), (8, 0.22),
                  (10, 0.08), (12, 0.1), (16, 0.06)],
    'flute':     [(1, 1.0), (2, 0.18), (3, 0.06), (4, 0.03)],
    'pedal':     [(0.5, 1.0), (1, 0.8), (2, 0.35), (3, 0.12), (4, 0.15)],
    'reed':      [(1, 1.0), (2, 0.5), (3, 0.6), (4, 0.3), (5, 0.42), (6, 0.2), (7, 0.3), (8, 0.15),
                  (9, 0.2), (11, 0.13), (13, 0.09)],
}


def organ(f, dur, vel=0.8, seed=0, reg='principal', detune_c=2.5, chiff=0.12, trem=0.0,
          attack=0.045, release=0.25):
    r = rng(seed)
    n = N(dur + release)
    t = tt(n)
    y = np.zeros(n)
    for ratio, amp in ORGAN_REG[reg]:
        fr = f * ratio * cents(r.uniform(-detune_c, detune_c))
        if fr > 15000:
            continue
        delay = r.uniform(0, 0.012) / max(ratio, 0.5)
        on = np.clip((t - delay) / 0.02, 0, 1)
        y += amp * sine(fr, n, r.uniform()) * on
    if chiff:
        cz = bp(white(n, seed + 1), min(f * 2.5, 9000), min(f * 5, 15000), 2)
        y += chiff * cz * np.exp(-t / 0.03) * 3
    y += 0.004 * hp(white(n, seed + 2), 3000, 1)
    e = adsr(dur, attack, 0.08, 1.0, release, n, curve=1.5)
    if trem:
        e = e * (1 - trem * (0.5 + 0.5 * np.sin(TAU * 5.8 * t)))
    return y * e * _amp(vel, 0.8) * 0.18


def bell(f, dur=3.0, vel=0.8, seed=0, kind='tubular', ring=None):
    """inharmonic additive bell. Rings for `dur`, then a damper release
    (or the natural decay when ring=True)."""
    r = rng(seed)
    specs = {
        # ratio, amp, decay
        'tubular': [(1.0, 0.7, 3.0), (2.0, 1.0, 2.2), (2.76, 0.5, 1.6), (3.0, 0.35, 1.4), (4.07, 0.4, 1.0),
                    (5.4, 0.25, 0.7), (6.8, 0.12, 0.45), (8.93, 0.08, 0.3), (0.5, 0.2, 3.5)],
        'church':  [(0.5, 0.45, 4.0), (1.0, 0.7, 3.0), (1.19, 0.5, 2.2), (1.5, 0.3, 1.8), (2.0, 1.0, 2.0),
                    (2.51, 0.3, 1.2), (3.0, 0.35, 1.0), (4.0, 0.25, 0.6), (5.95, 0.1, 0.35)],
        'glock':   [(1.0, 1.0, 1.2), (2.76, 0.35, 0.3), (5.4, 0.15, 0.12), (8.93, 0.06, 0.06)],
        'celesta': [(1.0, 1.0, 0.9), (2.0, 0.25, 0.4), (3.0, 0.08, 0.2), (4.0, 0.04, 0.1)],
        'chime':   [(1.0, 1.0, 1.5), (2.0, 0.3, 0.8), (3.01, 0.25, 0.5), (4.2, 0.12, 0.3), (5.43, 0.08, 0.2)],
    }[kind]
    maxdec = max(sp[2] for sp in specs)
    if ring is None:
        ring = kind == 'church'
    if ring:
        total = max(dur, 3.0 * maxdec)
    else:
        total = max(dur, 0.05) + min(1.5, 2.5 * maxdec)
    n = N(total)
    t = tt(n)
    y = np.zeros(n)
    for ratio, amp, dec in specs:
        fr = f * ratio
        if fr > 16000:
            continue
        for split in (-1, 1):  # doublets -> slow beating shimmer
            fr2 = fr + split * r.uniform(0.2, 1.2) * (0.6 if kind in ('tubular', 'church') else 0.2)
            y += 0.5 * amp * np.sin(TAU * fr2 * t + r.uniform(0, 6)) * np.exp(-t / dec)
    strike = hp(white(n, seed), 2000, 2) * np.exp(-t / 0.003) * 0.15
    y = y + strike
    if not ring:
        rel = total - dur
        y = y * np.where(t < dur, 1.0, np.exp(-(t - dur) / max(rel / 4, 1e-3)))
    y = y * fade(np.ones(n), 0.0005, min(0.3, total * 0.2))
    return y * _amp(vel, 1.2) * 0.35


def marimba(f, dur=0.6, vel=0.8, seed=0):
    n = N(max(dur, 0.3) + 0.2)
    t = tt(n)
    dec = float(np.clip(0.5 * (440 / f) ** 0.6, 0.15, 1.2))
    y = (np.sin(TAU * f * t) * np.exp(-t / dec) + 0.2 * np.sin(TAU * f * 3.93 * t) * np.exp(-t / (dec * 0.15))
         + 0.06 * np.sin(TAU * f * 9.2 * t) * np.exp(-t / 0.01))
    y += 0.15 * bp(white(n, seed), 1500, 6000) * np.exp(-t / 0.002)
    return fade(y, 0.0005, 0.03) * _amp(vel, 1.2) * 0.5


def fm_bell(f, dur=1.5, vel=0.8, seed=0, ratio=3.5, index=4.0, decay=0.8):
    n = N(dur)
    t = tt(n)
    ie = index * np.exp(-t / (decay * 0.5))
    y = fm(f, n, ratio, ie) * np.exp(-t / decay)
    return fade(y, 0.001, 0.03) * _amp(vel, 1.2) * 0.4


# ==========================================================================
# percussion (each returns a one-shot)
# ==========================================================================
def timpani(f, vel=0.8, seed=0, dur=4.5):
    n = N(dur)
    t = tt(n)
    fa = 1 + 0.025 * vel * np.exp(-t / 0.05)
    modes = [(1.0, 1.0, 1.4), (1.504, 0.45, 0.8), (1.742, 0.22, 0.55), (2.0, 0.25, 0.6),
             (2.245, 0.12, 0.35), (2.494, 0.1, 0.3), (2.8, 0.06, 0.2), (0.62, 0.7, 0.09)]
    r = rng(seed)
    y = np.zeros(n)
    ph = np.cumsum(fa) / SR
    for ratio, amp, dec in modes:
        bright = 1.0 if ratio <= 1.0 else (0.4 + 0.6 * vel)
        y += amp * bright * np.sin(TAU * f * ratio * ph + r.uniform(0, 6)) * np.exp(-t / dec)
    y += 0.5 * lp(white(n, seed), 1600, 2) * np.exp(-t / 0.012) * vel
    return fade(y, 0.0005, 0.4) * _amp(vel, 1.5) * 0.5


def timp_roll(f, dur, v0=0.3, v1=1.0, seed=0, rate=17.0, end_hit=True):
    r = rng(seed)
    out = np.zeros(N(dur + 4.6))
    k = 0
    tpos = 0.0
    while tpos < dur:
        u = tpos / max(dur, 1e-3)
        v = (v0 + (v1 - v0) * u ** 1.5) * r.uniform(0.75, 1.0)
        mix_at(out, timpani(f, v * 0.7, seed + k, 2.0), N(tpos))
        tpos += (1.0 / rate) * r.uniform(0.85, 1.15)
        k += 1
    if end_hit:
        mix_at(out, timpani(f, v1, seed + 999), N(dur))
    return out


def taiko(f=68.0, vel=1.0, seed=0, size=1.0):
    n = N(1.5 * size)
    t = tt(n)
    fa = f * (1 + 0.45 * np.exp(-t / 0.018))
    ph = np.cumsum(fa) / SR
    y = np.sin(TAU * ph) * np.exp(-t / (0.42 * size))
    for ratio, amp, dec in [(1.58, 0.35, 0.16), (2.13, 0.22, 0.09), (2.65, 0.14, 0.05), (3.3, 0.08, 0.03)]:
        y += amp * np.sin(TAU * ratio * ph) * np.exp(-t / dec)
    y += 0.45 * bp(white(n, seed), 600, 3500) * np.exp(-t / 0.006)
    y += 0.3 * lp(white(n, seed + 1), 900, 2) * np.exp(-t / 0.05)
    y = sat(y * (0.8 + 0.6 * vel), 1.3)
    return fade(y, 0.0003, 0.1) * _amp(vel, 1.4) * 0.6


def taiko_rim(vel=0.8, seed=0):
    n = N(0.12)
    t = tt(n)
    y = (np.sin(TAU * 1150 * t) * np.exp(-t / 0.012) + 0.6 * np.sin(TAU * 1930 * t) * np.exp(-t / 0.008)
         + 0.6 * bp(white(n, seed), 2000, 6000) * np.exp(-t / 0.004))
    return fade(y, 0.0002, 0.02) * _amp(vel) * 0.45


def shime(vel=0.8, seed=0, f=430.0):
    n = N(0.25)
    t = tt(n)
    fa = f * (1 + 0.08 * np.exp(-t / 0.008))
    ph = np.cumsum(fa) / SR
    y = np.sin(TAU * ph) * np.exp(-t / 0.07) + 0.4 * np.sin(TAU * 1.6 * ph) * np.exp(-t / 0.03)
    y += 0.25 * np.sin(TAU * 2.3 * ph) * np.exp(-t / 0.015)
    y += 0.6 * bp(white(n, seed), 1500, 7000) * np.exp(-t / 0.005)
    return fade(y, 0.0002, 0.03) * _amp(vel, 1.3) * 0.4


def kane(vel=0.8, seed=0, damp=False, f=1320.0):
    n = N(0.07 if damp else 0.45)
    t = tt(n)
    spec = [(1.0, 1.0, 0.28), (1.53, 0.7, 0.2), (2.38, 0.5, 0.13), (3.3, 0.35, 0.09), (4.6, 0.2, 0.06)]
    y = np.zeros(n)
    r = rng(seed)
    for ratio, amp, dec in spec:
        d = dec * (0.12 if damp else 1.0)
        y += amp * np.sin(TAU * f * ratio * t * r.uniform(0.995, 1.005)) * np.exp(-t / d)
    y += 0.5 * hp(white(n, seed), 3000) * np.exp(-t / 0.003)
    return fade(y, 0.0002, 0.02) * _amp(vel) * 0.25


def kick(vel=0.9, seed=0, kind='909'):
    cfg = {'909': (190, 48, 0.032, 0.38, 0.35), 'metal': (240, 58, 0.018, 0.17, 0.6),
           'boom': (120, 40, 0.06, 0.9, 0.15), 'march': (110, 55, 0.04, 0.45, 0.2)}[kind]
    f1, f0, ptau, dec, click = cfg
    n = N(dec * 3.2)
    t = tt(n)
    fa = f0 + (f1 - f0) * np.exp(-t / ptau)
    ph = np.cumsum(fa) / SR
    y = np.sin(TAU * ph) * np.exp(-t / dec)
    y += click * bp(white(n, seed), 1500, 8000) * np.exp(-t / 0.0035)
    y += click * 0.5 * np.sin(TAU * 1800 * t) * np.exp(-t / 0.004)
    y = sat(y * 1.4, 1.2)
    return fade(y, 0.0002, 0.05) * _amp(vel, 1.2) * 0.8


def snare(vel=0.8, seed=0, kind='rock'):
    cfg = {'rock': (190, 0.12, 0.2, 1.0, 900), 'march': (240, 0.07, 0.12, 1.3, 1800),
           'edm': (210, 0.08, 0.16, 1.0, 1200), 'tight': (260, 0.05, 0.08, 1.2, 2000)}[kind]
    f, tdec, ndec, nmix, nlo = cfg
    n = N(ndec * 4)
    t = tt(n)
    fa = f * (1 + 0.4 * np.exp(-t / 0.01))
    ph = np.cumsum(fa) / SR
    tone = np.sin(TAU * ph) * np.exp(-t / tdec * 0.5) + 0.5 * np.sin(TAU * 1.8 * ph) * np.exp(-t / (tdec * 0.3))
    nz = bp(white(n, seed), nlo, 11000) * np.exp(-t / ndec)
    nz = nz * (1 + 0.6 * np.exp(-t / 0.01))
    y = 0.55 * tone + nmix * nz
    y = sat(y * 1.2, 1.1)
    return fade(y, 0.0002, 0.04) * _amp(vel, 1.3) * 0.55


def clap(vel=0.8, seed=0):
    n = N(0.4)
    t = tt(n)
    nz = bp(white(n, seed), 900, 6000)
    e = np.zeros(n)
    for k, d in enumerate([0.0, 0.009, 0.019, 0.027]):
        e += (t >= d) * np.exp(-np.maximum(t - d, 0) / (0.006 if k < 3 else 0.12)) * (0.8 if k < 3 else 1.0)
    y = nz * e
    y = peq(y, 1300, 4, 1.0)
    return fade(y, 0.0002, 0.05) * _amp(vel) * 0.4


def hat(vel=0.6, seed=0, open_=False, decay=None):
    n = N(0.9 if open_ else 0.14)
    t = tt(n)
    freqs = [205.3, 304.4, 369.6, 522.7, 540.0, 800.0]
    m = np.zeros(n)
    for fr in freqs:
        m += square(fr * 1.7, n, rng(seed).uniform())
    m = m + 1.5 * white(n, seed)
    y = bp(m, 7000, 15000, 2)
    y = hp(y, 6500, 2)
    d = decay if decay else (0.22 if open_ else 0.035)
    y = y * np.exp(-t / d)
    return fade(y, 0.0002, 0.02) * _amp(vel) * 0.25


def cymbal(vel=0.8, seed=0, decay=2.0, kind='crash', swell=0.0):
    n = N(decay * 4.0 + swell)
    t = tt(n)
    r = rng(seed)
    m = np.zeros(n)
    for k in range(14):
        m += square(r.uniform(300, 9000), n, r.uniform()) * r.uniform(0.3, 1.0)
    m = m / 6 + white(n, seed)
    if kind == 'china':
        m = m * (1 + sine(1340, n))
        y = bp(m, 2000, 9000, 2)
    else:
        y = hp(m, 3200 if kind == 'crash' else 5000, 2)
    e = np.exp(-np.maximum(t - swell, 0) / decay)
    if swell:
        e = np.where(t < swell, (t / swell) ** 2.5, e)
        y = y * e
    else:
        y = y * e
        y = y + 0.8 * hp(white(n, seed + 1), 5000) * np.exp(-t / 0.02)
    y = lp(y, 14000, 2)
    return fade(y, 0.0005, min(0.5, decay)) * _amp(vel) * 0.25


def tom(f=110, vel=0.8, seed=0, dec=0.35):
    n = N(dec * 3)
    t = tt(n)
    fa = f * (1 + 0.5 * np.exp(-t / 0.03))
    ph = np.cumsum(fa) / SR
    y = np.sin(TAU * ph) * np.exp(-t / dec) + 0.3 * np.sin(TAU * 1.5 * ph) * np.exp(-t / (dec * 0.4))
    y += 0.3 * bp(white(n, seed), 400, 4000) * np.exp(-t / 0.008)
    y = sat(y * 1.2, 1.2)
    return fade(y, 0.0002, 0.05) * _amp(vel, 1.3) * 0.6


def gran_cassa(vel=0.9, seed=0):
    n = N(3.6)
    t = tt(n)
    fa = 46 * (1 + 0.2 * np.exp(-t / 0.05))
    ph = np.cumsum(fa) / SR
    y = np.sin(TAU * ph) * np.exp(-t / 0.9) + 0.4 * np.sin(TAU * 1.7 * ph) * np.exp(-t / 0.25)
    y += 0.5 * lp(white(n, seed), 500, 2) * np.exp(-t / 0.08)
    return fade(y, 0.0005, 0.4) * _amp(vel, 1.3) * 0.8


def frame_drum(vel=0.7, seed=0, edge=False):
    f = 360 if edge else 105
    n = N(0.15 if edge else 0.45)
    t = tt(n)
    fa = f * (1 + 0.25 * np.exp(-t / 0.015))
    ph = np.cumsum(fa) / SR
    y = np.sin(TAU * ph) * np.exp(-t / (0.05 if edge else 0.16))
    y += 0.4 * np.sin(TAU * 1.7 * ph) * np.exp(-t / 0.03)
    y += (0.5 if edge else 0.25) * bp(white(n, seed), 500, 4000) * np.exp(-t / 0.006)
    return fade(y, 0.0002, 0.03) * _amp(vel, 1.3) * 0.5


def tambourine(vel=0.6, seed=0, shake=False):
    n = N(0.35)
    t = tt(n)
    r = rng(seed)
    y = np.zeros(n)
    for k in range(7):
        d = r.uniform(0, 0.012 if not shake else 0.04)
        fr = r.uniform(5200, 11000)
        y += np.sin(TAU * fr * t) * np.exp(-np.maximum(t - d, 0) / 0.07) * (t >= d) * r.uniform(0.4, 1.0)
    y = y / 3 + hp(white(n, seed), 6000) * np.exp(-t / (0.05 if not shake else 0.09))
    if shake:
        y = y * np.clip(t / 0.015, 0, 1)
    return fade(y, 0.0002, 0.03) * _amp(vel) * 0.2


def shaker(vel=0.5, seed=0, length=0.07):
    n = N(length + 0.05)
    t = tt(n)
    y = bp(white(n, seed), 3500, 11000, 2)
    e = np.clip(t / 0.012, 0, 1) * np.exp(-np.maximum(t - 0.012, 0) / (length * 0.45))
    return fade(y * e, 0.0002, 0.02) * _amp(vel) * 0.25


def maracas(vel=0.6, seed=0):
    n = N(0.12)
    t = tt(n)
    y = bp(white(n, seed), 4000, 12000, 2)
    e = np.exp(-t / 0.018) + 0.5 * (t > 0.03) * np.exp(-np.maximum(t - 0.03, 0) / 0.02)
    return fade(y * e, 0.0002, 0.02) * _amp(vel) * 0.3


def bongo(vel=0.8, seed=0, high=True, slap=False):
    f = 520 if high else 350
    n = N(0.3)
    t = tt(n)
    fa = f * (1 + 0.12 * np.exp(-t / 0.01))
    ph = np.cumsum(fa) / SR
    dec = 0.09 if high else 0.13
    y = np.sin(TAU * ph) * np.exp(-t / dec) + 0.4 * np.sin(TAU * 1.59 * ph) * np.exp(-t / (dec * 0.5))
    y += 0.25 * np.sin(TAU * 2.14 * ph) * np.exp(-t / (dec * 0.3))
    y += (0.9 if slap else 0.3) * bp(white(n, seed), 1200, 7000) * np.exp(-t / (0.01 if slap else 0.004))
    return fade(y, 0.0002, 0.03) * _amp(vel, 1.3) * 0.45


def claves(vel=0.7, seed=0):
    n = N(0.12)
    t = tt(n)
    y = np.sin(TAU * 2480 * t) * np.exp(-t / 0.025) + 0.2 * np.sin(TAU * 4120 * t) * np.exp(-t / 0.01)
    y += 0.2 * hp(white(n, seed), 3000) * np.exp(-t / 0.002)
    return fade(y, 0.0002, 0.02) * _amp(vel) * 0.3


def woodblock(vel=0.7, seed=0, f=900):
    n = N(0.12)
    t = tt(n)
    y = np.sin(TAU * f * t) * np.exp(-t / 0.03) + 0.5 * np.sin(TAU * f * 2.7 * t) * np.exp(-t / 0.012)
    y += 0.3 * bp(white(n, seed), 1500, 6000) * np.exp(-t / 0.003)
    return fade(y, 0.0002, 0.02) * _amp(vel) * 0.4


# ==========================================================================
# synths
# ==========================================================================
def syn_pluck(f, dur, vel=0.8, seed=0, cut=(250, 6000), decay=0.16, res=2.0, wave='sawsq'):
    rel = 0.12
    n = N(dur + rel)
    t = tt(n)
    r = rng(seed)
    if wave == 'sawsq':
        x = saw(f * cents(-6), n, r.uniform()) + 0.6 * square(f * cents(6), n, r.uniform(), 0.4)
    elif wave == 'square':
        x = square(f, n, r.uniform(), 0.3)
    else:
        x = saw(f, n, r.uniform())
    fc = cut[0] + (cut[1] - cut[0]) * vel * np.exp(-t / decay)
    y = tv_lp(x, fc, q=res, stages=1, block=32)
    e = adsr(dur, 0.002, decay * 2.0, 0.25, rel, n)
    return y * e * _amp(vel, 1.0) * 0.35


def syn_bass(f, dur, vel=0.9, seed=0, cut=900, env_amt=1500, decay=0.12, drive=2.0, sub=0.6):
    rel = 0.05
    n = N(dur + rel)
    t = tt(n)
    x = saw(f, n, 0.0) + 0.5 * saw(f * cents(9), n, 0.37) + sub * sine(f * 0.5, n)
    fc = cut + env_amt * vel * np.exp(-t / decay)
    y = tv_lp(x, fc, q=1.4, stages=2, block=32)
    y = sat(y * drive, 1.0)
    e = adsr(dur, 0.003, 0.2, 0.85, rel, n)
    return y * e * _amp(vel, 0.9) * 0.4


def pad_saw(f, dur, vel=0.6, seed=0, attack=0.4, release=0.8, cut=2600, voices=7, detune=0.16):
    n = N(dur + release)
    x = supersaw(f, n, voices, detune, seed)
    x = lp(x, cut, 2)
    x = hp(x, f * 0.7, 1)
    e = adsr(dur, attack, 0.5, 0.9, release, n, curve=1.5)
    return x * e * _amp(vel) * 0.3


def lead_line(notes, spb, total_n, seed=0, glide_t=0.045, kind='supersaw', cut=5500, vib_c=18,
              res=1.2, sub=0.25, start_beat_offset=0.0):
    """monophonic legato synth line. notes: [(beat, dur, midi, vel)] -> mono buffer of total_n"""
    out = np.zeros(total_n)
    if not notes:
        return out
    notes = sorted(notes)
    # group into phrases (gap > 0.6 beat -> new phrase)
    phrases = [[notes[0]]]
    for nt in notes[1:]:
        pb, pd = phrases[-1][-1][0], phrases[-1][-1][1]
        if nt[0] - (pb + pd) > 0.6:
            phrases.append([nt])
        else:
            phrases[-1].append(nt)
    for pi, ph in enumerate(phrases):
        b0 = ph[0][0]
        b1 = ph[-1][0] + ph[-1][1]
        rel = 0.25
        n = int((b1 - b0) * spb) + N(rel)
        t = tt(n)
        lf = np.zeros(n)
        amp = np.zeros(n)
        prev = None
        for (b, d, m, v) in ph:
            s = int((b - b0) * spb)
            e_ = int((b - b0 + d) * spb)
            seg = np.arange(s, n)
            if prev is None:
                lf[s:] = m
            else:
                u = np.clip((seg - s) / max(1, N(glide_t)), 0, 1)
                lf[s:] = prev + (m - prev) * (1 - (1 - u) ** 2)
            prev = m
            L = e_ - s
            g = np.ones(L)
            g[:min(L, N(0.004))] = np.linspace(0.75, 1, min(L, N(0.004)))
            amp[s:e_] = np.maximum(amp[s:e_], g * (0.6 + 0.4 * v))
        last_end = int((b1 - b0) * spb)
        amp[last_end:] = amp[max(0, last_end - 1)] * np.exp(-tt(n - last_end) / (rel / 4))
        # smooth gate
        amp = onepole_lp(amp, 120)
        fq = 440 * 2 ** ((lf - 69) / 12)
        fq = vibrato(1.0, n, rate=5.8, depth_c=vib_c, delay=0.35, ramp=0.3, seed=seed + pi, jitter_c=1) * fq
        if kind == 'supersaw':
            x = supersaw(fq, n, 5, 0.12, seed + pi) + sub * square(fq * 0.5, n, 0.0)
        else:
            x = saw(fq, n) + 0.5 * square(fq * cents(5), n, 0.2, 0.45)
        y = tv_lp(x, cut * (0.7 + 0.3 * amp), q=res, stages=1)
        y = y * amp
        mix_at(out, y, int((b0 - start_beat_offset) * spb))
    return out * 0.3


def bubble(f0=600.0, dur=0.09, vel=0.7, seed=0, up=2.6):
    n = N(dur + 0.02)
    t = tt(n)
    fa = f0 * up ** np.clip(t / dur, 0, 1.2)
    e = np.clip(t / 0.003, 0, 1) * np.exp(-t / (dur * 0.45))
    y = sine(fa, n) * e
    return fade(y, 0.0002, 0.01) * _amp(vel) * 0.5


def riser(dur, seed=0, f0=300, f1=9000, tone=True):
    n = N(dur)
    t = tt(n)
    u = t / dur
    fc = f0 * (f1 / f0) ** u
    y = tv_bp(white(n, seed), fc, q=3.0)
    if tone:
        y = y + 0.3 * saw(fc * 0.25, n) * u
    e = u ** 2
    return y * e * 0.5


def impact(vel=1.0, seed=0, dur=2.0):
    n = N(dur)
    t = tt(n)
    fa = 35 + 90 * np.exp(-t / 0.05)
    ph = np.cumsum(fa) / SR
    y = np.sin(TAU * ph) * np.exp(-t / 0.6)
    y += 0.6 * lp(white(n, seed), 2500, 2) * np.exp(-t / 0.12)
    y = sat(y * 1.5, 1.2)
    return fade(y, 0.0003, 0.2) * vel * 0.7


def glitch_burst(dur=0.12, seed=0, vel=0.6):
    r = rng(seed)
    n = N(dur)
    y = np.zeros(n)
    pos = 0
    while pos < n:
        L = int(r.uniform(0.006, 0.03) * SR)
        kind = r.integers(0, 3)
        seg_n = min(L, n - pos)
        if kind == 0:
            seg = square(r.uniform(300, 4000), seg_n, 0, r.uniform(0.1, 0.5))
        elif kind == 1:
            seg = np.sign(white(seg_n, r.integers(1e6))) * 0.7
            seg = seg[(np.arange(seg_n) // int(r.integers(2, 24))) * int(1)]
        else:
            seg = sine(r.uniform(100, 2000) * (1 + 3 * np.linspace(0, 1, seg_n)), seg_n)
        y[pos:pos + seg_n] = seg * r.uniform(0.3, 1.0) * (r.uniform() > 0.15)
        pos += seg_n
    y = fade(y, 0.001, 0.005)
    return y * vel * 0.3


def bird(seed=0, vel=0.5):
    """procedural songbird call: a few FM-trilled chirps"""
    r = rng(seed)
    kind = r.integers(0, 4)
    parts = []
    tpos = 0.0
    count = int(r.integers(2, 6))
    base = r.uniform(2600, 5200)
    for k in range(count):
        d = r.uniform(0.03, 0.09) if kind != 2 else r.uniform(0.12, 0.25)
        n = N(d)
        t = tt(n)
        if kind == 0:      # down-sweep chirps
            f = base * (1.35 - 0.5 * t / d)
        elif kind == 1:    # up-sweep
            f = base * (0.8 + 0.6 * t / d)
        elif kind == 2:    # trill
            f = base * (1 + 0.08 * np.sin(TAU * r.uniform(28, 45) * t))
        else:              # warble
            f = base * (1 + 0.25 * np.sin(np.pi * t / d)) * (1 + 0.04 * np.sin(TAU * 60 * t))
        y = sine(f, n) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 1.5
        y += 0.15 * sine(f * 2, n) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 2
        parts.append((tpos, y))
        tpos += d + r.uniform(0.02, 0.09)
        base *= r.uniform(0.9, 1.12)
    out = np.zeros(N(tpos + 0.1))
    for p0, y in parts:
        mix_at(out, y, N(p0))
    return out * _amp(vel) * 0.25


def kakegoe(word='yo', seed=0, vel=0.9, people=4, base=190.0):
    """Japanese festival shouts: 'yo', 'ha', 'sore', 'iyoo', 'hu'"""
    r = rng(seed)
    out = np.zeros(N(1.4))
    for p in range(people):
        f0 = base * r.uniform(0.85, 1.25)
        off = r.uniform(0, 0.03)
        sh = r.uniform(0.92, 1.08)
        if word == 'yo':
            y = shout(f0, 0.32, [(0, 'i'), (0.05, 'o'), (0.32, 'o')], 'tenor', seed + p, 0.15, (1.0, 1.15, 0.8), shift=sh)
        elif word == 'ha':
            y = shout(f0 * 1.05, 0.3, [(0, 'a'), (0.3, 'a')], 'tenor', seed + p, 0.2, (1.1, 1.0, 0.75),
                      h_onset=0.05, shift=sh)
        elif word == 'sore':
            s1 = hp(white(N(0.09), seed + p), 3500, 2) * env([(0, 0), (0.02, 1), (0.09, 0.2)], N(0.09)) * 0.25
            y2 = shout(f0, 0.38, [(0, 'o'), (0.14, 'o'), (0.2, 'e'), (0.38, 'e')], 'tenor', seed + p, 0.15,
                       (1.0, 1.2, 0.85), shift=sh)
            y = np.concatenate([s1, y2])
        elif word == 'iyoo':
            y = shout(f0 * 0.9, 1.1, [(0, 'i'), (0.18, 'i'), (0.32, 'o'), (1.1, 'o')], 'tenor', seed + p, 0.12,
                      (0.85, 1.45, 1.3), shift=sh)
        else:
            y = shout(f0, 0.25, [(0, 'u'), (0.25, 'u')], 'tenor', seed + p, 0.3, (1.0, 1.1, 0.8),
                      h_onset=0.04, shift=sh)
        mix_at(out, y * r.uniform(0.7, 1.0), N(off))
    return fade(out, 0.001, 0.05) * vel * 0.9


def robot_words(words, f0=150.0, seed=0, syl=0.24, gap=0.06):
    """crude robotic formant speech. words: list of syllable specs
    (consonant, vowel_path, coda) e.g. ('p', ['o','u'], 'z')"""
    out = []
    for i, (cons, vp, coda) in enumerate(words):
        seg = []
        if cons in ('p', 'g', 'k', 't'):
            n = N(0.018)
            lo, hi = {'p': (300, 2500), 'g': (200, 1500), 'k': (1500, 5000), 't': (2500, 8000)}[cons]
            seg.append(bp(white(n, seed + i), lo, hi) * np.exp(-tt(n) / 0.006) * 0.9)
        n = N(syl)
        t = tt(n)
        src = square(f0, n, 0.0, 0.35) + 0.4 * saw(f0 * 2, n)
        path = [(0, vp[0])] + [((k + 1) * syl / len(vp), v) for k, v in enumerate(vp)]
        v = formant_tv(src, path, 'bass', shift=1.0) * env([(0, 0), (0.01, 1), (syl * 0.85, 1), (syl, 0.4)], n)
        seg.append(v)
        if coda == 'z':
            m = N(0.12)
            seg.append(hp(white(m, seed + 50 + i), 4000, 2) * 0.25 * env([(0, 0.5), (0.03, 1), (0.12, 0)], m)
                       + 0.2 * formant(square(f0, m), 'bass', 'i') * env([(0, 1), (0.12, 0)], m))
        elif coda == 'm':
            m = N(0.13)
            seg.append(lp(square(f0, m, 0, 0.35), 400, 2) * 0.6 * env([(0, 1), (0.1, 0.8), (0.13, 0)], m))
        elif coda == 'r':
            m = N(0.08)
            seg.append(formant(square(f0, m), 'bass', 'u') * env([(0, 1), (0.08, 0)], m))
        out.append(np.concatenate(seg))
        out.append(np.zeros(N(gap)))
    y = np.concatenate(out)
    return fade(y, 0.001, 0.02)
