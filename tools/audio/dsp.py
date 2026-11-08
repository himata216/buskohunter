"""Core DSP toolkit for the ブスコハンター sound set.

Everything is vectorised numpy/scipy. Mono signals are 1-D float arrays,
stereo signals are (n, 2) arrays. Sample rate is fixed at 44100 Hz.
"""
import math
import os
import subprocess

import numpy as np
from scipy import signal
from scipy.io import wavfile
from scipy.ndimage import minimum_filter1d, uniform_filter1d, maximum_filter1d

SR = 44100
TAU = 2.0 * np.pi


# --------------------------------------------------------------------------
# basics
# --------------------------------------------------------------------------
def N(sec):
    """seconds -> samples"""
    return max(0, int(round(sec * SR)))


def tt(n):
    return np.arange(n) / SR


def rng(seed=0):
    return np.random.default_rng(seed)


def mtof(m):
    return 440.0 * 2.0 ** ((np.asarray(m, dtype=float) - 69.0) / 12.0)


def cents(c):
    return 2.0 ** (np.asarray(c, dtype=float) / 1200.0)


def db(x):
    return 10.0 ** (x / 20.0)


def pad_to(x, n):
    if len(x) >= n:
        return x[:n]
    pad = np.zeros((n - len(x),) + x.shape[1:])
    return np.concatenate([x, pad], axis=0)


def mix_at(dst, src, pos, gain=1.0):
    """add src into dst at sample offset pos (clipped to dst bounds)"""
    if pos >= len(dst) or gain == 0:
        return dst
    s0 = 0
    if pos < 0:
        s0 = -pos
        pos = 0
    e = min(len(dst), pos + len(src) - s0)
    if e > pos:
        dst[pos:e] += gain * src[s0:s0 + (e - pos)]
    return dst


def seq_mix(parts, n=None):
    """parts: list of (time_sec, mono_array[, gain]) -> mono sum"""
    end = 0
    for p in parts:
        end = max(end, N(p[0]) + len(p[1]))
    out = np.zeros(n if n else end)
    for p in parts:
        g = p[2] if len(p) > 2 else 1.0
        mix_at(out, p[1], N(p[0]), g)
    return out


# --------------------------------------------------------------------------
# oscillators (PolyBLEP band-limited)
# --------------------------------------------------------------------------
def phase(freq, n, phase0=0.0):
    """returns (phase in cycles, per-sample increment)"""
    if np.ndim(freq) == 0:
        inc = float(freq) / SR
        ph = phase0 + np.arange(n) * inc
        dt = np.full(n, inc)
    else:
        f = np.asarray(freq, dtype=float)
        if len(f) < n:
            f = np.concatenate([f, np.full(n - len(f), f[-1])])
        f = f[:n]
        dt = f / SR
        ph = phase0 + np.concatenate(([0.0], np.cumsum(dt[:-1])))
    return ph, dt


def _blep(t, dt):
    y = np.zeros_like(t)
    dt = np.maximum(dt, 1e-9)
    m = t < dt
    if m.any():
        x = t[m] / dt[m]
        y[m] = x + x - x * x - 1.0
    m = t > 1.0 - dt
    if m.any():
        x = (t[m] - 1.0) / dt[m]
        y[m] = x * x + x + x + 1.0
    return y


def osc(kind, freq, n, phase0=0.0, pw=0.5):
    ph, dt = phase(freq, n, phase0)
    if kind == 'sine':
        return np.sin(TAU * ph)
    t = ph % 1.0
    if kind == 'saw':
        return 2.0 * t - 1.0 - _blep(t, dt)
    if kind in ('square', 'pulse'):
        y = np.where(t < pw, 1.0, -1.0)
        y += _blep(t, dt) - _blep((t - pw) % 1.0, dt)
        return y
    if kind == 'tri':
        return 2.0 * np.abs(2.0 * t - 1.0) - 1.0
    raise ValueError(kind)


def sine(freq, n, phase0=0.0):
    return osc('sine', freq, n, phase0)


def saw(freq, n, phase0=0.0):
    return osc('saw', freq, n, phase0)


def square(freq, n, phase0=0.0, pw=0.5):
    return osc('square', freq, n, phase0, pw)


def tri(freq, n, phase0=0.0):
    return osc('tri', freq, n, phase0)


def supersaw(freq, n, voices=7, detune=0.18, seed=0, side=0.6):
    """JP-8000 style detuned saw stack. detune in semitones (outer voices)."""
    r = rng(seed)
    offs = np.linspace(-1.0, 1.0, voices) * detune
    offs = offs * (1.0 + 0.15 * r.uniform(-1, 1, voices))
    y = np.zeros(n)
    for i, o in enumerate(offs):
        w = 1.0 if abs(o) < 1e-9 else side
        y += w * saw(np.asarray(freq) * 2.0 ** (o / 12.0), n, r.uniform(0, 1))
    y -= np.mean(y)
    return y / (1 + side * (voices - 1)) * 1.6


def harm_sum(ph, amps, kmax=None):
    """sum_k amps[k-1] * sin(2*pi*k*ph)   (Chebyshev recurrence, cheap)"""
    th = TAU * ph
    s1 = np.sin(th)
    c2 = 2.0 * np.cos(th)
    K = len(amps) if kmax is None else min(kmax, len(amps))
    y = amps[0] * s1
    sp = np.zeros_like(s1)
    sc = s1
    for k in range(2, K + 1):
        sn = c2 * sc - sp
        a = amps[k - 1]
        if np.ndim(a) or a != 0:
            y = y + a * sn
        sp, sc = sc, sn
    return y


def fm(freq, n, ratio=1.0, index=1.0, phase0=0.0, mod_phase0=0.0):
    """2-operator FM (phase modulation). index may be an array."""
    phc, _ = phase(freq, n, phase0)
    phm, _ = phase(np.asarray(freq) * ratio, n, mod_phase0)
    return np.sin(TAU * phc + index * np.sin(TAU * phm))


def partials(f0, n, spec, t0=0.0):
    """inharmonic additive: spec = [(ratio, amp, decay_s), ...]"""
    t = tt(n) + t0
    y = np.zeros(n)
    for item in spec:
        ratio, amp, dec = item[:3]
        ph0 = item[3] if len(item) > 3 else 0.0
        f = f0 * ratio
        if f >= SR * 0.45:
            continue
        y += amp * np.sin(TAU * f * t + ph0) * np.exp(-t / dec)
    return y


# --------------------------------------------------------------------------
# envelopes / modulation
# --------------------------------------------------------------------------
def adsr(gate, a=0.01, d=0.1, s=0.7, r=0.2, n=None, curve=2.0, dcurve=3.0):
    """gate: seconds held. Returns envelope of length gate+r (or n)."""
    gate = max(gate, 0.0)
    a = max(a, 1e-4)
    d = max(d, 1e-4)
    r = max(r, 1e-3)
    n = N(gate + r) if n is None else n
    t = tt(n)

    def held(tv):
        tv = np.asarray(tv, dtype=float)
        att = 1.0 - (1.0 - np.clip(tv / a, 0, 1)) ** curve
        dec = s + (1.0 - s) * np.exp(-(tv - a) / d * dcurve)
        return np.where(tv < a, att, dec)

    e = held(t)
    lg = float(held(gate))
    x = t - gate
    rel = lg * np.exp(-3.0 * np.maximum(x, 0) / r) * np.clip(1.0 - x / r, 0.0, 1.0)
    return np.where(t < gate, e, rel)


def env(points, n, kind='lin'):
    """breakpoint envelope. points = [(t, v), ...]"""
    ts = np.array([p[0] for p in points], dtype=float)
    vs = np.array([p[1] for p in points], dtype=float)
    t = tt(n)
    if kind == 'exp':
        lv = np.log(np.maximum(vs, 1e-5))
        return np.exp(np.interp(t, ts, lv)) * (np.interp(t, ts, vs) > 0)
    if kind == 'cos':
        idx = np.clip(np.searchsorted(ts, t, side='right') - 1, 0, len(ts) - 2)
        t0, t1 = ts[idx], ts[idx + 1]
        v0, v1 = vs[idx], vs[idx + 1]
        u = np.clip((t - t0) / np.maximum(t1 - t0, 1e-9), 0, 1)
        u = 0.5 - 0.5 * np.cos(np.pi * u)
        out = v0 + (v1 - v0) * u
        out[t >= ts[-1]] = vs[-1]
        return out
    return np.interp(t, ts, vs)


def perc(n, attack=0.002, decay=0.2, curve=1.0):
    t = tt(n)
    a = np.clip(t / max(attack, 1e-5), 0, 1)
    return a * np.exp(-np.maximum(t - attack, 0) / decay) ** curve


def expdec(n, tau):
    return np.exp(-tt(n) / tau)


def fade(x, fin=0.002, fout=0.01):
    x = np.array(x, dtype=float, copy=True)
    a, b = N(fin), N(fout)
    if a > 0 and len(x) > a:
        w = np.linspace(0, 1, a)
        x[:a] *= w if x.ndim == 1 else w[:, None]
    if b > 0 and len(x) > b:
        w = np.linspace(1, 0, b)
        x[-b:] *= w if x.ndim == 1 else w[:, None]
    return x


def vibrato(f0, n, rate=5.5, depth_c=15.0, delay=0.25, ramp=0.3, seed=0, jitter_c=2.0):
    """returns frequency array with delayed vibrato + slow random drift"""
    t = tt(n)
    r = rng(seed)
    dep = depth_c * np.clip((t - delay) / max(ramp, 1e-3), 0, 1)
    rate_mod = rate * (1 + 0.04 * np.sin(TAU * 0.37 * t + r.uniform(0, 6)))
    ph = np.cumsum(rate_mod) / SR
    v = dep * np.sin(TAU * ph + r.uniform(0, 6))
    if jitter_c > 0:
        k = max(4, n // 2000)
        dr = np.interp(np.linspace(0, k, n), np.arange(k + 1), r.standard_normal(k + 1))
        v = v + jitter_c * dr
    return f0 * cents(v)


def glide(f_from, f_to, n, time, curve='exp', start=0.0):
    t = tt(n)
    u = np.clip((t - start) / max(time, 1e-4), 0, 1)
    if curve == 'exp':
        return f_from * (f_to / f_from) ** u
    u = 1 - (1 - u) ** 2
    return f_from + (f_to - f_from) * u


def lfo(n, rate, shape='sine', phase0=0.0):
    ph = np.arange(n) * rate / SR + phase0
    if shape == 'sine':
        return np.sin(TAU * ph)
    if shape == 'tri':
        return 2 * np.abs(2 * (ph % 1) - 1) - 1
    if shape == 'square':
        return np.where(ph % 1 < 0.5, 1.0, -1.0)
    if shape == 'saw':
        return 2 * (ph % 1) - 1
    raise ValueError(shape)


def smooth_noise(n, rate, seed=0):
    """band-limited random control signal (~rate Hz), range ~[-1,1]"""
    r = rng(seed)
    k = max(2, int(n / SR * rate) + 2)
    pts = r.uniform(-1, 1, k)
    x = np.linspace(0, k - 1, n)
    i = np.floor(x).astype(int)
    i = np.clip(i, 0, k - 2)
    u = x - i
    u = u * u * (3 - 2 * u)
    return pts[i] * (1 - u) + pts[i + 1] * u


# --------------------------------------------------------------------------
# noise
# --------------------------------------------------------------------------
def white(n, seed=0):
    return rng(seed).standard_normal(n)


def pink(n, seed=0):
    b = [0.049922035, -0.095993537, 0.050612699, -0.004408786]
    a = [1.0, -2.494956002, 2.017265875, -0.522189400]
    y = signal.lfilter(b, a, white(n + 2000, seed))[2000:]
    return y / (np.std(y) + 1e-12)


def brown(n, seed=0):
    y = signal.lfilter([1.0], [1.0, -0.997], white(n + 4000, seed))[4000:]
    y -= np.mean(y)
    return y / (np.std(y) + 1e-12)


def crackle(n, density=40.0, seed=0, decay=0.002, tone=None):
    """random sparse clicks (fire / electricity / debris)."""
    r = rng(seed)
    imp = np.zeros(n)
    cnt = r.poisson(density * n / SR)
    pos = r.integers(0, max(1, n), cnt)
    imp[pos] = r.uniform(-1, 1, cnt) * r.uniform(0.2, 1.0, cnt) ** 2
    k = N(decay * 6) + 1
    kern = np.exp(-np.arange(k) / max(1, N(decay)))
    y = signal.fftconvolve(imp, kern)[:n]
    if tone:
        y = bp(y, tone * 0.6, tone * 1.6)
    return y


# --------------------------------------------------------------------------
# filters
# --------------------------------------------------------------------------
def rbj(kind, f, q=0.707, gain_db=0.0):
    f = float(np.clip(f, 5.0, SR * 0.485))
    w0 = TAU * f / SR
    cw, sw = math.cos(w0), math.sin(w0)
    q = max(q, 1e-3)
    alpha = sw / (2.0 * q)
    A = 10.0 ** (gain_db / 40.0)
    if kind == 'lp':
        b = [(1 - cw) / 2, 1 - cw, (1 - cw) / 2]
        a = [1 + alpha, -2 * cw, 1 - alpha]
    elif kind == 'hp':
        b = [(1 + cw) / 2, -(1 + cw), (1 + cw) / 2]
        a = [1 + alpha, -2 * cw, 1 - alpha]
    elif kind == 'bp':
        b = [alpha, 0.0, -alpha]
        a = [1 + alpha, -2 * cw, 1 - alpha]
    elif kind == 'notch':
        b = [1.0, -2 * cw, 1.0]
        a = [1 + alpha, -2 * cw, 1 - alpha]
    elif kind == 'peak':
        b = [1 + alpha * A, -2 * cw, 1 - alpha * A]
        a = [1 + alpha / A, -2 * cw, 1 - alpha / A]
    elif kind == 'ls':
        sq = 2 * math.sqrt(A) * alpha
        b = [A * ((A + 1) - (A - 1) * cw + sq), 2 * A * ((A - 1) - (A + 1) * cw),
             A * ((A + 1) - (A - 1) * cw - sq)]
        a = [(A + 1) + (A - 1) * cw + sq, -2 * ((A - 1) + (A + 1) * cw),
             (A + 1) + (A - 1) * cw - sq]
    elif kind == 'hs':
        sq = 2 * math.sqrt(A) * alpha
        b = [A * ((A + 1) + (A - 1) * cw + sq), -2 * A * ((A - 1) + (A + 1) * cw),
             A * ((A + 1) + (A - 1) * cw - sq)]
        a = [(A + 1) - (A - 1) * cw + sq, 2 * ((A - 1) - (A + 1) * cw),
             (A + 1) - (A - 1) * cw - sq]
    elif kind == 'ap':
        b = [1 - alpha, -2 * cw, 1 + alpha]
        a = [1 + alpha, -2 * cw, 1 - alpha]
    else:
        raise ValueError(kind)
    b = np.array(b) / a[0]
    a = np.array(a) / a[0]
    return b, a


def biquad(x, kind, f, q=0.707, gain_db=0.0):
    b, a = rbj(kind, f, q, gain_db)
    return signal.lfilter(b, a, x, axis=0)


def peq(x, f, gain_db, q=1.0):
    return biquad(x, 'peak', f, q, gain_db)


def lshelf(x, f, gain_db, q=0.707):
    return biquad(x, 'ls', f, q, gain_db)


def hshelf(x, f, gain_db, q=0.707):
    return biquad(x, 'hs', f, q, gain_db)


def _butter(x, kind, f, order):
    if kind == 'bandpass':
        lo, hi = f
        lo = max(lo, 5.0)
        hi = min(hi, SR * 0.485)
        if hi <= lo * 1.01:
            hi = lo * 1.02
        sos = signal.butter(order, [lo, hi], btype='bandpass', fs=SR, output='sos')
    else:
        f = float(np.clip(f, 5.0, SR * 0.485))
        sos = signal.butter(order, f, btype=kind, fs=SR, output='sos')
    return signal.sosfilt(sos, x, axis=0)


def lp(x, f, order=2):
    return _butter(x, 'lowpass', f, order)


def hp(x, f, order=2):
    return _butter(x, 'highpass', f, order)


def bp(x, lo, hi, order=2):
    return _butter(x, 'bandpass', (lo, hi), order)


def onepole_lp(x, f):
    c = math.exp(-TAU * f / SR)
    return signal.lfilter([1 - c], [1, -c], x, axis=0)


def tvf(x, kind, freq, q=0.707, block=64, stages=1, gain_db=0.0):
    """time-varying biquad (block-wise coefficient update, state carried).
    freq / q may be scalars or per-sample arrays."""
    n = len(x)
    if n == 0:
        return x
    y = np.empty(n)
    nb = (n + block - 1) // block
    centers = np.minimum(np.arange(nb) * block + block // 2, n - 1)
    fa = np.broadcast_to(np.asarray(freq, dtype=float), (n,))[centers]
    qa = np.broadcast_to(np.asarray(q, dtype=float), (n,))[centers]
    fa = np.clip(fa, 10.0, SR * 0.48)
    # vectorised RBJ coefficients
    w0 = TAU * fa / SR
    cw, sw = np.cos(w0), np.sin(w0)
    alpha = sw / (2 * np.maximum(qa, 1e-3))
    a0 = 1 + alpha
    if kind == 'lp':
        b0 = (1 - cw) / 2; b1 = 1 - cw; b2 = (1 - cw) / 2
    elif kind == 'hp':
        b0 = (1 + cw) / 2; b1 = -(1 + cw); b2 = (1 + cw) / 2
    elif kind == 'bp':
        b0 = alpha; b1 = np.zeros_like(alpha); b2 = -alpha
    elif kind == 'notch':
        b0 = np.ones_like(alpha); b1 = -2 * cw; b2 = np.ones_like(alpha)
    elif kind == 'peak':
        A = 10 ** (gain_db / 40)
        b0 = 1 + alpha * A; b1 = -2 * cw; b2 = 1 - alpha * A
        a0 = 1 + alpha / A
        a2v = 1 - alpha / A
    else:
        raise ValueError(kind)
    a1 = -2 * cw
    a2 = (1 - alpha) if kind != 'peak' else a2v
    B = np.stack([b0, b1, b2], 1) / a0[:, None]
    Aa = np.stack([np.ones_like(a0), a1 / a0, a2 / a0], 1)
    zis = [np.zeros(2) for _ in range(stages)]
    lf = signal.lfilter
    for i in range(nb):
        s = i * block
        e = min(n, s + block)
        seg = x[s:e]
        for k in range(stages):
            seg, zis[k] = lf(B[i], Aa[i], seg, zi=zis[k])
        y[s:e] = seg
    return y


def tv_lp(x, freq, q=0.707, stages=2, block=64):
    return tvf(x, 'lp', freq, q, block, stages)


def tv_bp(x, freq, q=2.0, block=64, stages=1):
    return tvf(x, 'bp', freq, q, block, stages)


def resonator(x, freqs, q=30.0, gains=None):
    """bank of parallel bandpass resonators"""
    y = np.zeros_like(x, dtype=float)
    for i, f in enumerate(freqs):
        if f >= SR * 0.45:
            continue
        g = 1.0 if gains is None else gains[i]
        y += g * biquad(x, 'bp', f, q)
    return y


def comb_ff(x, delay_s, g=0.5):
    d = N(delay_s)
    y = np.array(x, dtype=float, copy=True)
    if d > 0:
        y[d:] += g * x[:-d]
    return y


# --------------------------------------------------------------------------
# Karplus-Strong plucked string (block vectorised, exact pitch by resampling)
# --------------------------------------------------------------------------
def ks(freq, dur, t60=1.5, a=0.5, bright=0.7, pick=0.15, seed=0, exc=None, n_out=None):
    """freq: Hz, dur: output seconds. a in [0.5, 0.99]: loop-filter weight
    (0.5 = dark/damped highs, ->1 brighter). bright: excitation lowpass amount."""
    n_out = N(dur) if n_out is None else n_out
    target_p = SR / freq
    Ni = max(2, int(math.floor(target_p - (1 - a))))
    P = Ni + (1 - a)
    ratio = P / target_p
    n_int = int(n_out * ratio) + 4
    g = 10.0 ** (-3.0 / (max(t60, 0.01) * freq))
    r = rng(seed)
    if exc is None:
        e = r.uniform(-1, 1, Ni)
        c = float(np.clip(1.0 - bright, 0.0, 0.97))
        if c > 0:
            e = signal.lfilter([1 - c], [1, -c], np.concatenate([e, e]))[Ni:]
        sh = int(round(pick * Ni))
        if 0 < sh < Ni:
            e = e - np.roll(e, sh) * 0.9
    else:
        e = np.asarray(exc, float)[:Ni]
        if len(e) < Ni:
            e = np.concatenate([e, np.zeros(Ni - len(e))])
    e = e - np.mean(e)
    e /= (np.max(np.abs(e)) + 1e-12)
    total = n_int + Ni + 2
    y = np.zeros(total)
    off = 1
    y[off:off + Ni] = e
    k = off + Ni
    while k < total:
        en = min(total, k + Ni)
        L = en - k
        y[k:en] = g * (a * y[k - Ni:k - Ni + L] + (1 - a) * y[k - Ni - 1:k - Ni - 1 + L])
        k = en
    y = y[off:]
    pos = np.arange(n_out) * ratio
    out = np.interp(pos, np.arange(len(y)), y)
    k = min(len(out), 24)
    out[:k] *= np.linspace(0, 1, k)  # avoid a DC step at the onset
    return out


# --------------------------------------------------------------------------
# formants (vowels) — Csound formant table (approx.)
# --------------------------------------------------------------------------
FORMANTS = {
    'soprano': {
        'a': ([800, 1150, 2900, 3900, 4950], [0, -6, -32, -20, -50], [80, 90, 120, 130, 140]),
        'e': ([350, 2000, 2800, 3600, 4950], [0, -20, -15, -40, -56], [60, 100, 120, 150, 200]),
        'i': ([270, 2140, 2950, 3900, 4950], [0, -12, -26, -26, -44], [60, 90, 100, 120, 120]),
        'o': ([450, 800, 2830, 3800, 4950], [0, -11, -22, -22, -50], [40, 80, 100, 120, 120]),
        'u': ([325, 700, 2700, 3800, 4950], [0, -16, -35, -40, -60], [50, 60, 170, 180, 200]),
    },
    'alto': {
        'a': ([800, 1150, 2800, 3500, 4950], [0, -4, -20, -36, -60], [80, 90, 120, 130, 140]),
        'e': ([400, 1600, 2700, 3300, 4950], [0, -24, -30, -35, -60], [60, 80, 120, 150, 200]),
        'i': ([350, 1700, 2700, 3700, 4950], [0, -20, -30, -36, -60], [50, 100, 120, 150, 200]),
        'o': ([450, 800, 2830, 3500, 4950], [0, -9, -16, -28, -55], [70, 80, 100, 130, 135]),
        'u': ([325, 700, 2530, 3500, 4950], [0, -12, -30, -40, -64], [50, 60, 170, 180, 200]),
    },
    'tenor': {
        'a': ([650, 1080, 2650, 2900, 3250], [0, -6, -7, -8, -22], [80, 90, 120, 130, 140]),
        'e': ([400, 1700, 2600, 3200, 3580], [0, -14, -12, -14, -20], [70, 80, 100, 120, 120]),
        'i': ([290, 1870, 2800, 3250, 3540], [0, -15, -18, -20, -30], [40, 90, 100, 120, 120]),
        'o': ([400, 800, 2600, 2800, 3000], [0, -10, -12, -12, -26], [40, 80, 100, 120, 120]),
        'u': ([350, 600, 2700, 2900, 3300], [0, -20, -17, -14, -26], [40, 60, 100, 120, 120]),
    },
    'bass': {
        'a': ([600, 1040, 2250, 2450, 2750], [0, -7, -9, -9, -20], [60, 70, 110, 120, 130]),
        'e': ([400, 1620, 2400, 2800, 3100], [0, -12, -9, -12, -18], [40, 80, 100, 120, 120]),
        'i': ([250, 1750, 2600, 3050, 3340], [0, -30, -16, -22, -28], [60, 90, 100, 120, 120]),
        'o': ([400, 750, 2400, 2600, 2900], [0, -11, -21, -20, -40], [40, 80, 100, 120, 120]),
        'u': ([350, 600, 2400, 2675, 2950], [0, -20, -32, -28, -36], [40, 80, 100, 120, 120]),
    },
}


def formant(x, voice='bass', vowel='a', shift=1.0, bw_scale=1.0, nform=5):
    fr, gains, bws = FORMANTS[voice][vowel]
    y = np.zeros_like(x, dtype=float)
    for f, g, bw in list(zip(fr, gains, bws))[:nform]:
        fc = f * shift
        if fc > SR * 0.45:
            continue
        q = fc / (bw * bw_scale * shift ** 0.5)
        y += db(g) * biquad(x, 'bp', fc, q)
    return y


def vowel_tracks(path, n, voice='bass', shift=1.0):
    """path = [(t_sec, vowel), ...] -> per-formant freq / gain / bw arrays"""
    ts = [p[0] for p in path]
    t = tt(n)
    fr = []
    gn = []
    bw = []
    for k in range(5):
        fv = [FORMANTS[voice][p[1]][0][k] for p in path]
        gv = [FORMANTS[voice][p[1]][1][k] for p in path]
        bv = [FORMANTS[voice][p[1]][2][k] for p in path]
        fr.append(np.interp(t, ts, fv) * (shift if np.ndim(shift) == 0 else shift))
        gn.append(db(np.interp(t, ts, gv)))
        bw.append(np.interp(t, ts, bv))
    return fr, gn, bw


def formant_tv(x, path, voice='bass', shift=1.0, bw_scale=1.0, nform=5, block=64):
    """formant filter following a vowel path [(t, vowel), ...]; shift may be an array"""
    n = len(x)
    fr, gn, bw = vowel_tracks(path, n, voice, 1.0)
    sh = np.broadcast_to(np.asarray(shift, dtype=float), (n,))
    y = np.zeros(n)
    for k in range(nform):
        fc = np.clip(fr[k] * sh, 30, SR * 0.45)
        q = fc / (bw[k] * bw_scale * np.sqrt(sh))
        y += gn[k] * tvf(x, 'bp', fc, q, block=block)
    return y


# --------------------------------------------------------------------------
# dynamics / saturation
# --------------------------------------------------------------------------
def sat(x, drive=2.0):
    return np.tanh(drive * x) / math.tanh(drive)


def asym_sat(x, drive=2.0, bias=0.2):
    y = np.tanh(drive * (x + bias)) - math.tanh(drive * bias)
    return y / (math.tanh(drive) + 1e-9)


def hardclip(x, t=1.0):
    return np.clip(x, -t, t)


def fold(x, amount=1.5):
    return np.sin(np.pi / 2 * amount * x)


def crush(x, bits=6):
    q = 2.0 ** (bits - 1)
    return np.round(x * q) / q


def srhold(x, factor):
    """sample-rate reduction (sample & hold)"""
    factor = max(1, int(factor))
    idx = (np.arange(len(x)) // factor) * factor
    return x[idx]


def env_follow(x, attack=0.005, release=0.08):
    m = np.abs(x) if x.ndim == 1 else np.max(np.abs(x), axis=1)
    w = max(1, N(attack))
    m = maximum_filter1d(m, size=2 * w + 1)
    c = math.exp(-1.0 / max(1, N(release)))
    return signal.lfilter([1 - c], [1, -c], m)


def compress(x, thresh_db=-18.0, ratio=3.0, attack=0.01, release=0.15, makeup_db=0.0,
             knee_db=6.0, sidechain=None):
    det = x if sidechain is None else sidechain
    e = env_follow(det, attack, release)
    edb = 20 * np.log10(e + 1e-9)
    over = edb - thresh_db
    gr = np.where(over <= -knee_db / 2, 0.0,
                  np.where(over >= knee_db / 2, over * (1 - 1 / ratio),
                           (1 - 1 / ratio) * (over + knee_db / 2) ** 2 / (2 * knee_db)))
    g = db(-gr + makeup_db)
    return x * (g if x.ndim == 1 else g[:, None])


def limiter(x, ceiling_db=-1.0, look_ms=2.5, rel_ms=60.0):
    """look-ahead brickwall: min-filter + box-smoothing (guaranteed <= ceiling)"""
    c = db(ceiling_db)
    m = np.abs(x) if x.ndim == 1 else np.max(np.abs(x), axis=1)
    graw = np.minimum(1.0, c / np.maximum(m, 1e-12))
    g = graw
    for ms in (look_ms, rel_ms):
        L = max(1, int(SR * ms / 1000.0))
        g = minimum_filter1d(g, size=2 * L + 1, mode='nearest')
        g = uniform_filter1d(g, size=2 * L + 1, mode='nearest')
    g = np.minimum(g, graw)
    return x * (g if x.ndim == 1 else g[:, None])


def peak(x):
    return float(np.max(np.abs(x))) if len(x) else 0.0


def normalize(x, peak_db=-1.0):
    p = peak(x)
    if p < 1e-12:
        return x
    return x * (db(peak_db) / p)


def rms(x):
    return float(np.sqrt(np.mean(np.square(x)))) if len(x) else 0.0


# --------------------------------------------------------------------------
# stereo helpers
# --------------------------------------------------------------------------
def pan(x, p=0.0):
    """constant-power pan, p in [-1, 1]"""
    th = (np.clip(p, -1, 1) + 1) * np.pi / 4
    if np.ndim(p):
        return np.stack([x * np.cos(th), x * np.sin(th)], 1)
    return np.stack([x * math.cos(th), x * math.sin(th)], 1)


def to_stereo(x):
    return x if x.ndim == 2 else np.stack([x, x], 1) * 0.7071


def to_mono(x):
    return x if x.ndim == 1 else x.mean(axis=1)


def widen(x, amount=0.5, delay_ms=11.0):
    """mono -> wide stereo via complementary comb (mono-compatible)"""
    d = N(delay_ms / 1000.0)
    m = x if x.ndim == 1 else x.mean(axis=1)
    dl = np.concatenate([np.zeros(d), m[:-d]]) if d else m
    s = amount * (m - dl) * 0.5
    return np.stack([m + s, m - s], 1) * 0.7071


def ms_width(x, w=1.2):
    m = (x[:, 0] + x[:, 1]) * 0.5
    s = (x[:, 0] - x[:, 1]) * 0.5 * w
    return np.stack([m + s, m - s], 1)


# --------------------------------------------------------------------------
# time-based effects
# --------------------------------------------------------------------------
def make_ir(t60=2.0, pre=0.012, hf=0.45, lf=1.15, length=None, er=12, er_span=0.07,
            seed=7, build=0.01, bright=1.0):
    """Synthesised stereo room/hall IR: band-wise exponentially decaying
    noise (frequency-dependent T60) + sparse early reflections."""
    n = N(length if length else t60 * 1.25 + pre + 0.05)
    t = tt(n)
    edges = [180, 700, 2200, 5500, 11000]
    t60s = [t60 * lf, t60 * 1.0, t60 * (0.5 + 0.5 * hf) * 1.0, t60 * hf, t60 * hf * 0.6,
            t60 * hf * 0.35]
    ir = np.zeros((n, 2))
    r = rng(seed)
    for ch in range(2):
        w = r.standard_normal(n)
        acc = np.zeros(n)
        bands = []
        bands.append(lp(w, edges[0], 4))
        for i in range(len(edges) - 1):
            bands.append(bp(w, edges[i], edges[i + 1], 3))
        bands.append(hp(w, edges[-1], 4))
        for b, T in zip(bands, t60s):
            acc += b * np.exp(-6.9078 * np.maximum(t - pre, 0) / max(T, 0.05))
        if bright != 1.0:
            acc = hshelf(acc, 4000, 20 * math.log10(bright))
        acc *= (1 - np.exp(-np.maximum(t - pre, 0) / max(build, 1e-3))) * (t >= pre)
        # early reflections
        if er:
            et = pre * 0.5 + r.uniform(0.002, er_span, er)
            ea = r.uniform(0.3, 1.0, er) * np.exp(-et / (er_span * 0.8)) * r.choice([-1, 1], er)
            erv = np.zeros(n)
            for a_, t_ in zip(ea, et):
                i = N(t_)
                if i < n:
                    erv[i] += a_
            erv = lp(erv, 7000, 2)
            acc = acc / (np.sqrt(np.sum(acc ** 2)) + 1e-12)
            erv = erv / (np.sqrt(np.sum(erv ** 2)) + 1e-12)
            acc = acc + 0.45 * erv
        ir[:, ch] = acc
    ir /= np.sqrt(np.sum(ir ** 2) / 2) + 1e-12
    return ir


def conv_reverb(x, ir, loop_len=None, out_len=None):
    """stereo convolution; if loop_len, the tail is folded circularly."""
    xs = to_stereo(x) if x.ndim == 1 else x
    L = xs[:, 0] * 0.65 + xs[:, 1] * 0.35
    R = xs[:, 0] * 0.35 + xs[:, 1] * 0.65
    yl = signal.oaconvolve(L, ir[:, 0])
    yr = signal.oaconvolve(R, ir[:, 1])
    y = np.stack([yl, yr], 1)
    if loop_len:
        return fold_loop(y, loop_len)
    n = out_len if out_len else len(y)
    return pad_to(y, n)


def fold_loop(y, L):
    out = np.zeros((L,) + y.shape[1:])
    for s in range(0, len(y), L):
        seg = y[s:s + L]
        out[:len(seg)] += seg
    return out


def reverb_mono(x, t60=0.6, wet=0.25, seed=3, pre=0.005, hf=0.4, er=8, er_span=0.03):
    """short room for SFX (mono in, mono out)"""
    ir = make_ir(t60=t60, pre=pre, hf=hf, er=er, er_span=er_span, seed=seed)
    y = signal.oaconvolve(x, ir[:, 0])
    out = np.zeros(len(y))
    out[:len(x)] += x
    return out + wet * y


def _fv_comb(x, D, fb, damp):
    n = len(x)
    out = np.zeros(n + D)
    w = np.zeros(n + D)
    zi = np.zeros(1)
    b, a = [1 - damp], [1, -damp]
    k = 0
    while k < n:  # blocks start at multiples of D, so out[k:e] only needs w[< k]
        e = min(n, k + D)
        L = e - k
        o = w[k - D:k - D + L] if k >= D else np.zeros(L)
        out[k:e] = o
        fs, zi = signal.lfilter(b, a, o, zi=zi)
        w[k:e] = x[k:e] + fb * fs
        k = e
    return out[:n]


def _fv_allpass(x, D, g=0.5):
    n = len(x)
    v = np.zeros(n)
    y = np.zeros(n)
    k = 0
    while k < n:
        e = min(n, k + D)
        L = e - k
        prev = v[k - D:k - D + L] if k >= D else np.zeros(L)
        v[k:e] = x[k:e] + g * prev
        y[k:e] = prev - x[k:e]
        k = e
    return y


def freeverb(x, room=0.8, damp=0.35, width=1.0, wet=0.3, dry=1.0):
    """Classic Freeverb (8 lowpass-feedback combs + 4 allpasses per channel)."""
    m = to_mono(x)
    combs = [1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617]
    aps = [556, 441, 341, 225]
    fb = room * 0.28 + 0.7
    outs = []
    for spread in (0, 23):
        acc = np.zeros(len(m))
        inp = m * 0.015
        for d in combs:
            acc += _fv_comb(inp, d + spread, fb, damp)
        for d in aps:
            acc = _fv_allpass(acc, d + spread, 0.5)
        outs.append(acc)
    wl, wr = outs
    w1 = wet * (width / 2 + 0.5)
    w2 = wet * ((1 - width) / 2)
    st = to_stereo(x) if x.ndim == 1 else x
    L = wl * w1 + wr * w2 + st[:, 0] * dry
    R = wr * w1 + wl * w2 + st[:, 1] * dry
    return np.stack([L, R], 1)


def echo(x, time_s, fb=0.35, lp_hz=4500.0, hp_hz=200.0, pingpong=True, n_echo=None, out_len=None):
    """feedback delay rendered as explicit echo series; returns WET stereo signal"""
    m = to_mono(x)
    D = N(time_s)
    if n_echo is None:
        n_echo = max(1, int(math.ceil(math.log(0.003) / math.log(max(fb, 1e-3)))))
        n_echo = min(n_echo, 12)
    n = out_len if out_len else len(m) + D * n_echo
    out = np.zeros((n, 2))
    cur = hp(m, hp_hz, 1)
    for k in range(1, n_echo + 1):
        cur = onepole_lp(cur, lp_hz)
        g = fb ** (k - 1)
        ch_l = 1.0 if (not pingpong or k % 2 == 1) else 0.25
        ch_r = 1.0 if (not pingpong or k % 2 == 0) else 0.25
        pos = D * k
        if pos >= n:
            break
        seg = cur[:n - pos]
        out[pos:pos + len(seg), 0] += g * ch_l * seg
        out[pos:pos + len(seg), 1] += g * ch_r * seg
    return out


def chorus(x, rate=0.8, depth_ms=3.0, base_ms=12.0, mix=0.5, seed=0):
    m = to_mono(x)
    n = len(m)
    r = rng(seed)
    idx = np.arange(n)
    outs = []
    for ch in range(2):
        d = (base_ms + depth_ms * np.sin(TAU * rate * idx / SR + r.uniform(0, 6) + ch * 1.7)) * SR / 1000
        pos = idx - d
        wet = np.interp(pos, idx, m, left=0.0)
        outs.append(m * (1 - mix) + wet * mix)
    return np.stack(outs, 1)


def stutter(x, start_s, slice_s, repeats, gain_decay=1.0):
    """repeat a slice (glitch)"""
    s = N(start_s)
    L = N(slice_s)
    sl = fade(x[s:s + L], 0.001, 0.002)
    out = np.array(x, dtype=float, copy=True)
    for k in range(repeats):
        p = s + k * L
        if p + L > len(out):
            break
        out[p:p + L] = sl * (gain_decay ** k)
    return out


def tape_stop(x, dur_s):
    n = len(x)
    m = N(dur_s)
    m = min(m, n)
    tail = x[n - m:]
    sp = np.linspace(1.0, 0.0, m) ** 1.5
    pos = np.cumsum(sp)
    pos = pos / max(pos[-1], 1) * (m - 1) * 0.5
    y = np.interp(pos, np.arange(m), tail)
    out = np.array(x, dtype=float, copy=True)
    out[n - m:] = y * np.linspace(1, 0, m) ** 0.5
    return out


def resample_pitch(x, ratio):
    """pitch/time change by resampling (ratio>1 -> higher & shorter)"""
    n = int(len(x) / ratio)
    return np.interp(np.arange(n) * ratio, np.arange(len(x)), x)


def ring(x, f, n=None):
    return x * sine(f, len(x))


def doppler(x, depth=0.02, rate=1.0):
    n = len(x)
    idx = np.arange(n)
    pos = idx + depth * SR * np.sin(TAU * rate * idx / SR) / (TAU * rate)
    return np.interp(pos, idx, x)


# --------------------------------------------------------------------------
# finishing / IO
# --------------------------------------------------------------------------
def trim(x, thresh_db=-60.0, pre_ms=1.0, tail_db=-75.0, fade_ms=8.0):
    m = to_mono(np.abs(x)) if x.ndim == 2 else np.abs(x)
    p = m.max() if len(m) else 0
    if p <= 0:
        return x
    th = p * db(thresh_db)
    idx = np.nonzero(m > th)[0]
    start = max(0, idx[0] - int(SR * pre_ms / 1000)) if len(idx) else 0
    tth = p * db(tail_db)
    idx2 = np.nonzero(m > tth)[0]
    end = idx2[-1] + 1 if len(idx2) else len(x)
    y = x[start:end]
    return fade(y, 0.0005, fade_ms / 1000.0)


def dc_block(x, f=18.0):
    return hp(x, f, 2)


def finish_sfx(x, peak_db=-1.0, hp_hz=30.0, lp_hz=None, trim_lead=True, tail_db=-72.0):
    y = to_mono(np.asarray(x, dtype=float))
    y = dc_block(y, hp_hz)
    if lp_hz:
        y = lp(y, lp_hz, 2)
    if trim_lead:
        y = trim(y, -55.0, 0.5, tail_db)
    else:
        y = trim(y, -200.0, 0.0, tail_db)
    y = limiter(normalize(y, peak_db + 0.8), peak_db - 0.05, 1.0, 20.0)
    y = normalize(y, peak_db)
    return y


def write_wav(path, x):
    d = os.path.dirname(path)
    if d:
        os.makedirs(d, exist_ok=True)
    x = np.asarray(x, dtype=np.float32)
    wavfile.write(path, SR, x)


def encode_ogg(wav, ogg, q=5, channels=1):
    d = os.path.dirname(ogg)
    if d:
        os.makedirs(d, exist_ok=True)
    cmd = ['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-ac', str(channels), '-ar', str(SR),
           '-c:a', 'libvorbis', '-q:a', str(q), '-map_metadata', '-1', ogg]
    subprocess.run(cmd, check=True)


def decoded_peak_db(ogg_path, channels):
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', ogg_path, '-f', 'f32le', '-ac', str(channels), '-'],
                         capture_output=True, check=True).stdout
    y = np.frombuffer(raw, np.float32)
    return 20 * math.log10(float(np.max(np.abs(y))) + 1e-12)


def save_ogg(x, ogg_path, q=5, channels=None, tmpdir='/tmp/bh_audio', target_peak_db=None, tol_db=0.4):
    """encode to Ogg Vorbis; if target_peak_db is given, the *decoded* peak is
    checked and the gain corrected (lossy coding can overshoot on transients)."""
    x = np.asarray(x, dtype=float)
    if channels is None:
        channels = 1 if x.ndim == 1 else 2
    os.makedirs(tmpdir, exist_ok=True)
    base = os.path.basename(ogg_path).replace('.ogg', '')
    sub = os.path.basename(os.path.dirname(ogg_path))
    wav = os.path.join(tmpdir, f'{sub}_{base}.wav')
    g = 1.0
    for _ in range(4):
        write_wav(wav, x * g)
        encode_ogg(wav, ogg_path, q, channels)
        if target_peak_db is None:
            break
        pk = decoded_peak_db(ogg_path, channels)
        if pk <= target_peak_db + tol_db:
            break
        g *= db(target_peak_db - pk)
    return wav
