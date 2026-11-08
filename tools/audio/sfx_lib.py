"""Reusable SFX building blocks (transients, whooshes, impacts, explosions,
metal, electricity, water, glass, creature roars...). All mono."""
import math

import numpy as np

import instruments as I
from dsp import (SR, TAU, N, tt, rng, cents, db, env, perc, expdec, fade, vibrato, phase, saw, square, sine,
                 tri, supersaw, fm, partials, white, pink, brown, crackle, lp, hp, bp, biquad, peq, lshelf,
                 hshelf, onepole_lp, tvf, tv_lp, tv_bp, resonator, ks, formant, formant_tv, sat, asym_sat,
                 smooth_noise, glide, lfo, pad_to, mix_at, seq_mix, crush, srhold, reverb_mono, ring, stutter,
                 freeverb, to_mono)


def tailfade(y, frac=0.25):
    """raised-cosine fade over the last `frac` of the buffer (prevents end clicks)"""
    y = np.asarray(y, dtype=float)
    k = max(1, int(len(y) * frac))
    y = y.copy()
    y[-k:] *= 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, k))
    return y


def lin_env(n, pts):
    return env(pts, n)


def click(seed=0, lo=2500, dur=0.006, amp=1.0):
    n = N(dur * 4)
    return hp(white(n, seed), lo, 2) * np.exp(-tt(n) / (dur / 2.5)) * amp


def thump(f0=120, f1=45, dur=0.35, ptau=0.03, dtau=0.12, drive=1.5, amp=1.0):
    n = N(dur)
    t = tt(n)
    f = f1 + (f0 - f1) * np.exp(-t / ptau)
    y = np.sin(TAU * np.cumsum(f) / SR) * np.exp(-t / dtau)
    y = y * np.clip(t / 0.0015, 0, 1)
    return tailfade(sat(y * drive, 1.0) * amp, 0.3)


def nburst(dur, lo, hi, attack=0.002, decay=0.05, seed=0, amp=1.0, order=2, kind='white'):
    n = N(dur)
    t = tt(n)
    src = white(n, seed) if kind == 'white' else pink(n, seed)
    y = bp(src, lo, hi, order) if lo and hi else (lp(src, hi, order) if hi else hp(src, lo, order))
    e = np.clip(t / max(attack, 1e-4), 0, 1) * np.exp(-np.maximum(t - attack, 0) / decay)
    return tailfade(y * e * amp, 0.2)


def whoosh(dur, fpts, q=1.2, seed=0, peak_at=0.45, amp=1.0, shape=1.5, tone=0.0, src='pink'):
    """band-passed noise sweeping through fpts=[(t_frac, Hz), ...] with bell-shaped amplitude"""
    n = N(dur)
    t = tt(n)
    u = t / dur
    fp = [(p[0] * dur, p[1]) for p in fpts]
    fc = env(fp, n, 'exp')
    x = pink(n, seed) if src == 'pink' else white(n, seed)
    y = tv_bp(x, fc, q, stages=1)
    y += 0.5 * tv_bp(x, fc * 1.9, q * 1.5)
    a = np.where(u < peak_at, (u / peak_at) ** shape, ((1 - u) / (1 - peak_at)) ** (shape * 0.8))
    y = y * a
    if tone:
        y += tone * tv_bp(white(n, seed + 9), fc * 2.2, 12.0) * a
    return y * amp


def metal(f0, ratios, decays, amps=None, seed=0, dur=None, jitter=0.004):
    r = rng(seed)
    dur = dur or max(decays) * 4
    n = N(dur)
    t = tt(n)
    y = np.zeros(n)
    for i, (ra, de) in enumerate(zip(ratios, decays)):
        a = 1.0 if amps is None else amps[i]
        f = f0 * ra * (1 + r.uniform(-jitter, jitter))
        if f < SR * 0.45:
            y += a * np.sin(TAU * f * t + r.uniform(0, 6)) * np.exp(-t / de)
    return tailfade(y * np.clip(t / 0.0008, 0, 1), 0.3)


def explosion(size=1.0, seed=0, dur=None, bright=1.0, crackle_amt=0.4):
    dur = dur or 0.8 + 1.4 * size
    n = N(dur)
    t = tt(n)
    boom = thump(70 + 30 * (1 - size), 26 + 10 * (1 - size), dur, 0.06, 0.25 + 0.5 * size, 2.0, 1.0)
    nz = white(n, seed)
    fc = 300 + 7000 * bright * np.exp(-t / (0.08 + 0.15 * size))
    body = tv_lp(nz, fc, 0.7, stages=2) * (np.clip(t / 0.003, 0, 1) * np.exp(-t / (0.12 + 0.35 * size)))
    rumble = lp(brown(n, seed + 1), 180, 2) * np.exp(-t / (0.4 + 0.8 * size)) * np.clip(t / 0.02, 0, 1)
    deb = crackle(n, 120 * size, seed + 2, 0.0015) * np.exp(-t / (0.25 + 0.4 * size))
    deb = bp(deb, 600, 6000)
    crack = click(seed + 3, 1500, 0.008, 1.0)
    y = 0.9 * boom + 1.1 * body + 0.8 * rumble + crackle_amt * 3 * deb
    mix_at(y, crack, 0)
    return tailfade(sat(y * 1.3, 1.2), 0.25)


def zap(dur, seed=0, density=60.0, buzz_f=90.0, amp=1.0, buzz=0.5):
    n = N(dur)
    t = tt(n)
    r = rng(seed)
    cr = crackle(n, density, seed, 0.0006)
    arcs = bp(cr, 1800, 9000, 2) * 3
    gate = (smooth_noise(n, 40, seed + 1) > -0.1).astype(float)
    gate = onepole_lp(gate, 200)
    b = sat(saw(buzz_f * (1 + 0.02 * smooth_noise(n, 8, seed + 2)), n) * 3, 1.0)
    b = bp(b, 120, 4000) * gate * buzz
    hiss = hp(white(n, seed + 3), 5000, 2) * 0.25 * gate
    return tailfade((arcs + b + hiss) * amp, 0.1)


def splash(dur=0.8, seed=0, size=1.0):
    n = N(dur)
    t = tt(n)
    y = bp(white(n, seed), 300, 7000, 2) * np.clip(t / 0.015, 0, 1) * np.exp(-t / (0.12 + 0.2 * size))
    gur = lp(white(n, seed + 1), 1800, 2) * (0.6 + 0.4 * smooth_noise(n, 25, seed + 2)) * np.exp(-t / (0.3 * size))
    y = y + 0.6 * gur
    r = rng(seed + 3)
    for k in range(int(8 * size)):
        p = r.uniform(0.05, dur * 0.8)
        mix_at(y, I.bubble(r.uniform(700, 2600), r.uniform(0.02, 0.05), r.uniform(0.3, 0.8), k, up=1.8) * 0.8, N(p))
    return y


def glass(dur=0.8, seed=0, count=28, amp=1.0):
    n = N(dur)
    r = rng(seed)
    y = np.zeros(n)
    y += hp(white(n, seed), 2500, 2) * np.exp(-tt(n) / 0.03) * 1.2
    for k in range(count):
        p = r.uniform(0.0, dur * 0.7) ** 1.5 / (dur * 0.7) ** 0.5
        f = r.uniform(2200, 9000)
        m = metal(f, [1.0, 1.41, 2.03], [r.uniform(0.02, 0.09), 0.03, 0.02], [1, 0.5, 0.3], seed + k, dur=0.25)
        mix_at(y, m * r.uniform(0.2, 0.7) * (1 - p / dur), N(p))
    return y * amp


def sparkle(dur=1.0, seed=0, notes=None, step=0.07, amp=1.0, decay=0.35):
    notes = notes or [84, 86, 88, 91, 93, 96, 98]
    n = N(dur)
    y = np.zeros(n)
    for i, m in enumerate(notes):
        f = 440 * 2 ** ((m - 69) / 12)
        k = N(decay * 4)
        tk = tt(k)
        ping = (np.sin(TAU * f * tk) + 0.3 * np.sin(TAU * f * 2.01 * tk)) * np.exp(-tk / decay)
        ping = tailfade(ping * np.clip(tk / 0.002, 0, 1), 0.3)
        mix_at(y, ping * (0.9 - 0.04 * i), N(i * step))
    return y * amp


def roar(dur=2.5, f0_pts=((0, 80), (0.3, 110), (1.6, 95), (2.5, 60)), vowels=((0, 'o'), (0.4, 'a'), (2.5, 'o')),
         voice='bass', shift=0.75, seed=0, growl=0.6, sub=0.5, subharm=0.6, dist=2.5, breath=0.35,
         jitter=0.04, layers=3, amp_pts=None, vib_rate=0.0, vib_c=0.0, rough_rate=70.0):
    """layered creature roar: detuned glottal saws + subharmonic + growl AM +
    breath noise -> time-varying formants -> drive, plus a sub layer."""
    n = N(dur)
    t = tt(n)
    r = rng(seed)
    f0 = env(list(f0_pts), n, 'exp')
    f0 = f0 * (1 + jitter * smooth_noise(n, 18, seed))
    if vib_c:
        f0 = f0 * cents(vib_c * np.sin(TAU * vib_rate * t))
    src = np.zeros(n)
    for k in range(layers):
        src += saw(f0 * cents(r.uniform(-18, 18)), n, r.uniform())
    src /= layers
    src += subharm * square(f0 * 0.5, n, 0.0, 0.4)
    rough = 1 + growl * smooth_noise(n, rough_rate, seed + 1)
    src = src * rough
    src += breath * pink(n, seed + 2) * (0.6 + 0.4 * rough)
    y = formant_tv(src, list(vowels), voice, shift=shift)
    y = y / (np.max(np.abs(y)) + 1e-9)
    y = sat(y * dist, 1.0)
    y += sub * np.sin(TAU * np.cumsum(f0 * 0.5) / SR) * 0.6
    amp_pts = amp_pts or [(0, 0), (0.08, 0.8), (0.4, 1.0), (dur * 0.7, 0.85), (dur, 0)]
    a = env(list(amp_pts), n, 'cos')
    return y * a


def room(x, t60=0.4, wet=0.18, seed=5):
    return reverb_mono(x, t60, wet, seed=seed)


def hall(x, t60=1.6, wet=0.3, seed=6):
    return reverb_mono(x, t60, wet, seed=seed, pre=0.015, er_span=0.06)


def note_f(name):
    from sequencer import n2m
    return 440 * 2 ** ((n2m(name) - 69) / 12)


def addl(*xs):
    """sum arrays of different lengths (zero-padded)"""
    n = max(len(x) for x in xs)
    out = np.zeros(n)
    for x in xs:
        out[:len(x)] += x
    return out
