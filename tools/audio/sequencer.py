"""Music theory helpers + a small multitrack sequencer/mixer.

Loop-safe design: for LOOP songs every stem buffer is exactly the loop
length L and all writes wrap around (circular), reverb is a circular
convolution and every stateful effect is run with a circular pre-roll,
so the last bar flows into the first without click or gap.
"""
import math
from fractions import Fraction

import numpy as np

import dsp
from dsp import SR, N, mtof

# --------------------------------------------------------------------------
# theory
# --------------------------------------------------------------------------
PC = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}


def n2m(s):
    """'C#4' -> 61 (C4 = 60)"""
    s = s.strip()
    pc = PC[s[0].upper()]
    i = 1
    while i < len(s) and s[i] in '#b':
        pc += 1 if s[i] == '#' else -1
        i += 1
    octv = int(s[i:])
    return 12 * (octv + 1) + pc


QUAL = {
    '': [0, 4, 7], 'm': [0, 3, 7], '7': [0, 4, 7, 10], 'm7': [0, 3, 7, 10], 'maj7': [0, 4, 7, 11],
    'sus4': [0, 5, 7], 'sus2': [0, 2, 7], 'dim': [0, 3, 6], 'aug': [0, 4, 8], '5': [0, 7],
    'add9': [0, 4, 7, 14], '6': [0, 4, 7, 9], 'm6': [0, 3, 7, 9], '9': [0, 4, 7, 10, 14],
    'm9': [0, 3, 7, 10, 14], '7sus4': [0, 5, 7, 10], 'dim7': [0, 3, 6, 9], 'm7b5': [0, 3, 6, 10],
    '7b9': [0, 4, 7, 10, 13], 'mmaj7': [0, 3, 7, 11], 'madd9': [0, 3, 7, 14], 'b5': [0, 6],
    'cl': [0, 1, 6, 7],
}


def parse_chord(sym):
    """'F#m7/C#' -> (root_pc, intervals, bass_pc)"""
    bass = None
    if '/' in sym:
        sym, b = sym.split('/')
        bass = PC[b[0]] + sum(1 if c == '#' else -1 for c in b[1:])
    root = PC[sym[0]]
    i = 1
    while i < len(sym) and sym[i] in '#b' and not sym[i:].startswith('b5') and not sym[i:].startswith('b9'):
        root += 1 if sym[i] == '#' else -1
        i += 1
    q = sym[i:]
    ints = QUAL[q]
    root %= 12
    if bass is None:
        bass = root
    return root, ints, bass % 12


def chord_pcs(sym):
    r, ints, b = parse_chord(sym)
    return [(r + x) % 12 for x in ints]


def voicing(sym, lo, hi, n=4, prev=None):
    r, ints, b = parse_chord(sym)
    pcs = sorted(set((r + x) % 12 for x in ints))
    cands = []
    for start in range(lo, lo + 12):
        if start % 12 not in pcs:
            continue
        v = [start]
        while len(v) < n:
            nx = v[-1] + 1
            while nx % 12 not in pcs:
                nx += 1
            v.append(nx)
        if v[-1] <= hi:
            # make sure the third (or main colour tone) is present when possible
            cands.append(v)
    if not cands:
        cands = [[lo + ((r - lo) % 12) + 12 * k for k in range(n)]]
    if prev:
        ps = sorted(prev)

        def cost(v):
            vs = sorted(v)
            m = min(len(vs), len(ps))
            return sum(abs(vs[i] - ps[i]) for i in range(m))
        return min(cands, key=cost)
    mid = (lo + hi) / 2
    return min(cands, key=lambda v: abs(np.mean(v) - mid))


def bass_note(sym, octave=2):
    r, ints, b = parse_chord(sym)
    return 12 * (octave + 1) + b


def root_note(sym, octave=2):
    r, ints, b = parse_chord(sym)
    return 12 * (octave + 1) + r


def chord_tone(sym, degree, octave=2):
    """degree: 'R', '3', '5', '7', '8' relative to chord root"""
    r, ints, b = parse_chord(sym)
    base = 12 * (octave + 1) + r
    table = {'R': 0, '1': 0, '8': 12, '3': ints[1] if len(ints) > 1 else 4, '5': 7 if 7 in ints else ints[-1],
             '7': ints[3] if len(ints) > 3 else 10, '-5': -5, '-8': -12, 'B': (b - r) % 12, '-4': -5,
             '6': 9, '2': 2, '4': 5, '10': 12 + (ints[1] if len(ints) > 1 else 4), '12': 19}
    return base + table[degree]


def prog(text, start_beat=0.0, bpb=4):
    """'D | G | Em7 A7 | D:3 A:1' -> [(beat, dur, sym)]  (chords split bar evenly unless :dur)"""
    out = []
    beat = start_beat
    for bar in text.split('|'):
        toks = bar.split()
        if not toks:
            continue
        explicit = [t for t in toks if ':' in t]
        if explicit:
            for t in toks:
                s, d = t.split(':')
                out.append((beat, float(Fraction(d)), s))
                beat += float(Fraction(d))
        else:
            d = bpb / len(toks)
            for s in toks:
                out.append((beat, d, s))
                beat += d
    return out


def chord_at(progression, beat):
    for b, d, s in progression:
        if b <= beat + 1e-6 < b + d:
            return s
    return progression[-1][2]


def mel(text, start=0.0, vel=0.8, transpose=0, legato=1.0):
    """'D5:1.5 E5:.5 F#5 r:1 A5:1!' -> [(beat, dur, midi, vel)]
    '!' accent (+0.15 vel), '^' staccato (50%), '_' soft (-0.2 vel)."""
    out = []
    beat = start
    d = 1.0
    for tok in text.split():
        if tok == '|':
            continue
        acc = 0.0
        stac = 1.0
        while tok and tok[-1] in '!^_':
            if tok[-1] == '!':
                acc += 0.15
            elif tok[-1] == '^':
                stac = 0.5
            else:
                acc -= 0.2
            tok = tok[:-1]
        if ':' in tok:
            p, ds = tok.split(':')
            d = float(Fraction(ds))
        else:
            p = tok
        if p not in ('r', '-'):
            out.append((beat, d * legato * stac, n2m(p) + transpose, float(np.clip(vel + acc, 0.05, 1.0))))
        beat += d
    return out


def shift(notes, beats=0.0, semis=0, vel_mul=1.0):
    return [(b + beats, d, m + semis, min(1.0, v * vel_mul)) for (b, d, m, v) in notes]


def harmonize(notes, progression, below=True, min_int=3, scale=None):
    """mariachi/brass style harmony: nearest chord tone a 3rd-6th below (or above)"""
    out = []
    for (b, d, m, v) in notes:
        pcs = chord_pcs(chord_at(progression, b))
        cand = []
        rng_ = range(m - min_int, m - 10, -1) if below else range(m + min_int, m + 10)
        for c in rng_:
            if c % 12 in pcs:
                cand.append(c)
                break
        if not cand and scale is not None:
            cand = [m - 4 if below else m + 4]
        if cand:
            out.append((b, d, cand[0], v * 0.92))
    return out


def merge_prog(progression):
    out = []
    for b, d, s in progression:
        if out and out[-1][2] == s and abs(out[-1][0] + out[-1][1] - b) < 1e-6:
            out[-1] = (out[-1][0], out[-1][1] + d, s)
        else:
            out.append((b, d, s))
    return out


def arp_notes(progression, degrees, step=0.5, octave=3, vel=0.6, accent=0.12, dur=None, offset=0.0):
    out = []
    for (b, d, sym) in progression:
        k = 0
        t = b + offset
        while t < b + d - 1e-6:
            deg = degrees[k % len(degrees)]
            if deg is not None:
                m = chord_tone(sym, deg, octave)
                v = vel + (accent if abs((t - b) % 1.0) < 1e-6 else 0.0)
                out.append((t, dur or step, m, v))
            t += step
            k += 1
    return out


def pad_notes(progression, lo, hi, n=4, vel=0.5, overlap=0.05, merge=True):
    out = []
    prev = None
    pr = merge_prog(progression) if merge else progression
    for (b, d, sym) in pr:
        v = voicing(sym, lo, hi, n, prev)
        prev = v
        for m in v:
            out.append((b, d + overlap, m, vel))
    return out


def bass_notes(progression, pattern, octave=2, vel=0.75, period=4.0):
    """pattern: [(offset, dur, degree, vel_mul)], degree 'B' = slash-bass"""
    out = []
    for (b, d, sym) in progression:
        t0 = b
        while t0 < b + d - 1e-6:
            for (o, du, deg, vm) in pattern:
                if t0 + o < b + d - 1e-6:
                    if deg == 'B':
                        m = bass_note(sym, octave)
                    else:
                        m = chord_tone(sym, deg, octave)
                    out.append((t0 + o, min(du, b + d - t0 - o), m, vel * vm))
            t0 += period
    return out


def chord_hits(progression, beats, lo, hi, n=3, vel=0.6, dur=0.5):
    """chord stabs at given beat offsets inside each bar"""
    out = []
    prev = None
    for (b, d, sym) in progression:
        v = voicing(sym, lo, hi, n, prev)
        prev = v
        t0 = b
        while t0 < b + d - 1e-6:
            for o in beats:
                if t0 + o < b + d - 1e-6:
                    for m in v:
                        out.append((t0 + o, dur, m, vel))
            t0 += 4.0
    return out


def notes_len(notes):
    return max(b + d for b, d, m, v in notes) if notes else 0.0


# --------------------------------------------------------------------------
# sequencer / mixer
# --------------------------------------------------------------------------
class Stem:
    def __init__(self, song, name, gain=1.0, pan=0.0, rev=0.15, room=0.0, dly=0.0, fx=None,
                 width=None):
        self.song = song
        self.name = name
        self.gain = gain
        self.pan = pan
        self.rev = rev
        self.room = room
        self.dly = dly
        self.fx = fx or []
        self.width = width
        self.duck = None
        self.buf = np.zeros((song.BL, 2), dtype=np.float32)


class Song:
    def __init__(self, bpm, bars, bpb=4, loop=True, tail=4.0, seed=1, name='song'):
        self.bpm = bpm
        self.bpb = bpb
        self.bars = bars
        self.spb = 60.0 / bpm * SR           # samples per beat (float)
        self.sec_per_beat = 60.0 / bpm
        self.total_beats = bars * bpb
        self.L = int(round(self.total_beats * self.spb))
        self.loop = loop
        self.BL = self.L if loop else self.L + N(tail)
        self.stems = {}
        self.order = []
        self.rng = np.random.default_rng(seed)
        self.cache = {}
        self.name = name
        self.ir = None
        self.room_ir = None
        self.delay_beats = 0.75
        self.delay_fb = 0.32

    # ---- time
    def bar(self, b, beat=0.0):
        return b * self.bpb + beat

    def b2s(self, beat):
        return int(round(beat * self.spb))

    def secs(self, beats):
        return beats * self.sec_per_beat

    # ---- stems
    def stem(self, name, **kw):
        if name not in self.stems:
            self.stems[name] = Stem(self, name, **kw)
            self.order.append(name)
        return self.stems[name]

    def add(self, st, beat, audio, pan=None, gain=1.0, jitter_ms=0.0):
        if isinstance(st, str):
            st = self.stems[st]
        pos = self.b2s(beat)
        if jitter_ms:
            pos += int(self.rng.normal(0, jitter_ms) * SR / 1000)
        self.add_at(st, pos, audio, pan, gain)

    def add_at(self, st, pos, audio, pan=None, gain=1.0):
        if audio.ndim == 1:
            audio = dsp.pan(audio, st.pan if pan is None else pan)
        audio = (audio * gain).astype(np.float32)
        buf = st.buf
        BL = len(buf)
        if self.loop:
            pos %= BL
            k = 0
            while k < len(audio):
                p = (pos + k) % BL
                m = min(len(audio) - k, BL - p)
                buf[p:p + m] += audio[k:k + m]
                k += m
        else:
            if pos < 0:
                audio = audio[-pos:]
                pos = 0
            m = min(len(audio), BL - pos)
            if m > 0:
                buf[pos:pos + m] += audio[:m]

    def _cached(self, key, fn):
        if key not in self.cache:
            self.cache[key] = fn()
        return self.cache[key]

    def play(self, st, inst, notes, pan=None, transpose=0, jitter_ms=4.0, vel_jit=0.06,
             variants=3, legato=1.0, gain=1.0, pan_fn=None, **kw):
        if isinstance(st, str):
            st = self.stems[st]
        kws = tuple(sorted((k, repr(v)) for k, v in kw.items()))
        for i, (b, d, m, v) in enumerate(notes):
            vv = float(np.clip(v * (1 + self.rng.normal(0, vel_jit)), 0.05, 1.0))
            vq = round(vv * 12) / 12
            var = int(self.rng.integers(0, variants))
            dur_s = d * self.sec_per_beat * legato
            key = (inst.__name__, m + transpose, round(dur_s, 3), vq, var, kws)
            audio = self._cached(key, lambda: inst(float(mtof(m + transpose)), dur_s, vq, seed=var * 7919 + int(m), **kw))
            p = pan if pan_fn is None else pan_fn(m, i)
            self.add(st, b, audio, p, gain * (vv / max(vq, 1e-3)) ** 0.5, jitter_ms)

    def hit(self, st, fn, beat, vel=0.8, pan=None, variants=4, jitter_ms=2.0, gain=1.0, **kw):
        if isinstance(st, str):
            st = self.stems[st]
        vq = max(0.1, round(vel * 6) / 6)
        var = int(self.rng.integers(0, variants))
        kws = tuple(sorted((k, repr(v)) for k, v in kw.items()))
        key = (fn.__name__, vq, var, kws)
        audio = self._cached(key, lambda: fn(vel=vq, seed=var * 131 + 7, **kw))
        self.add(st, beat, audio, pan, gain * vel / vq, jitter_ms)

    def pattern(self, st, fn, pat, start, step=0.25, reps=1, vels=None, pan=None, jitter_ms=2.0,
                gain=1.0, **kw):
        vels = vels or {'X': 1.0, 'x': 0.72, 'o': 0.45, 'g': 0.25}
        pat = pat.replace(' ', '').replace('|', '')
        for r in range(reps):
            for i, c in enumerate(pat):
                if c in vels:
                    self.hit(st, fn, start + (r * len(pat) + i) * step, vels[c], pan, jitter_ms=jitter_ms,
                             gain=gain, **kw)
        return start + reps * len(pat) * step

    def duck_curve(self, period_beats=1.0, depth=0.6, attack=0.004, release=0.22, offset=0.0,
                   active=None):
        """periodic sidechain gain curve (kick on every period). active: list of (b0,b1) beat ranges"""
        n = self.BL
        tpos = (np.arange(n) / self.spb - offset) % period_beats * self.sec_per_beat
        g = 1 - depth * np.where(tpos < attack, tpos / attack, np.exp(-(tpos - attack) / (release / 3)))
        if active is not None:
            mask = np.zeros(n, bool)
            for b0, b1 in active:
                mask[self.b2s(b0):self.b2s(b1)] = True
            sm = dsp.onepole_lp(mask.astype(float), 30)
            g = 1 - (1 - g) * sm
        return g.astype(np.float32)

    # ---- effects helpers
    def circ(self, fn, x, pre_s=3.0):
        if not self.loop:
            return fn(x)
        P = min(N(pre_s), len(x))
        xx = np.concatenate([x[-P:], x], axis=0)
        y = fn(xx)
        return y[P:P + len(x)]

    def reverb(self, x, ir):
        if self.loop:
            return dsp.conv_reverb(x, ir, loop_len=self.L)
        return dsp.conv_reverb(x, ir, out_len=len(x))

    # ---- mixdown
    def mixdown(self, peak_db=-3.0, drive_db=2.0, glue=None, master_fx=None, delay_s=None,
                verbose=True, max_sec=None, fade_sec=1.0):
        BL = self.BL
        master = np.zeros((BL, 2))
        hall = np.zeros((BL, 2))
        room = np.zeros((BL, 2))
        dly = np.zeros((BL, 2))
        report = []
        for name in self.order:
            st = self.stems[name]
            x = st.buf.astype(np.float64)
            if not np.any(x):
                continue
            for f in st.fx:
                x = self.circ(f, x)
            if st.width is not None:
                x = dsp.ms_width(x, st.width)
            if st.duck is not None:
                x = x * st.duck[:, None]
            x = x * st.gain
            report.append((name, 20 * math.log10(dsp.rms(x) + 1e-12), 20 * math.log10(dsp.peak(x) + 1e-12)))
            master += x
            if st.rev:
                hall += x * st.rev
            if st.room:
                room += x * st.room
            if st.dly:
                dly += x * st.dly
        if np.any(dly):
            ds = delay_s if delay_s else self.delay_beats * self.sec_per_beat
            wet = self.circ(lambda z: dsp.echo(z, ds, self.delay_fb, out_len=len(z)), dly, pre_s=4.0)
            master += wet
            hall += wet * 0.4
        if np.any(hall):
            ir = self.ir if self.ir is not None else dsp.make_ir(2.2)
            master += self.reverb(hall, ir)
        if np.any(room):
            ir = self.room_ir if self.room_ir is not None else dsp.make_ir(0.7, pre=0.004, hf=0.4, er_span=0.03)
            master += self.reverb(room, ir)

        def master_chain(z):
            z = dsp.hp(z, 24, 2)
            if master_fx:
                for f in master_fx:
                    z = f(z)
            if glue:
                z = dsp.compress(z, **glue)
            return z
        out = self.circ(master_chain, master, pre_s=3.0)
        # drive into limiter
        p = dsp.peak(out)
        out = out * (dsp.db(peak_db + drive_db) / max(p, 1e-9))
        out = self.circ(lambda z: dsp.limiter(z, peak_db - 0.1, 2.0, 40.0), out, pre_s=1.0)
        out = dsp.normalize(out, peak_db)
        if not self.loop:
            out = dsp.trim(out, -200.0, 0.0, -70.0, 60.0)
            if max_sec and len(out) > N(max_sec):
                out = out[:N(max_sec)]
                k = N(fade_sec)
                out[-k:] *= (np.linspace(1, 0, k) ** 2)[:, None]
        if verbose:
            for r in report:
                print(f'   {self.name:>10s} {r[0]:>12s} rms {r[1]:6.1f} dB  peak {r[2]:6.1f} dB')
        return out
