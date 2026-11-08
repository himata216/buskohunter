"""All ブスコハンター sound effects (mono). Each design is a function(v) -> mono
array (v = variation index). Layering convention: transient + body + tail.
The REGISTRY at the bottom maps sound-event keys to file paths, variation
counts and subtitles (ja / en)."""
import numpy as np

import instruments as I
from sfx_lib import *  # noqa
from dsp import adsr, reverb_mono


# ==========================================================================
# WEAPON
# ==========================================================================
def swing_light(v=0):
    r = rng(100 + v)
    d = 0.24 + 0.03 * v
    k = [1.0, 1.12, 0.9][v % 3]
    y = whoosh(d, [(0, 900 * k), (0.45, 3600 * k), (1, 1500 * k)], 1.3, 100 + v, 0.42, tone=0.35)
    mix_at(y, 0.4 * whoosh(d * 0.8, [(0, 2500 * k), (0.5, 6500 * k), (1, 3500 * k)], 2.0, 200 + v, 0.5), N(d * 0.08))
    return room(y, 0.25, 0.08)


def swing_heavy(v=0):
    d = 0.48 + 0.04 * v
    k = [1.0, 0.9, 1.1][v % 3]
    y = whoosh(d, [(0, 220 * k), (0.5, 1100 * k), (1, 380 * k)], 0.9, 300 + v, 0.5, shape=1.8)
    y += 0.5 * whoosh(d, [(0, 700 * k), (0.5, 2400 * k), (1, 900 * k)], 1.4, 310 + v, 0.5, shape=2.0)
    n = N(d)
    sub = sine(env([(0, 50), (d * 0.5, 75), (d, 45)], n), n) * env([(0, 0), (d * 0.5, 1), (d, 0)], n, 'cos')
    y = pad_to(y, n) + 0.6 * sub
    return room(y, 0.3, 0.1)


def swing_blunt(v=0):
    d = 0.42
    y = whoosh(d, [(0, 300), (0.5, 1300), (1, 450)], 1.0, 400, 0.5, shape=1.6)
    n = len(y)
    y = y * (1 + 0.35 * np.sin(TAU * 22 * tt(n)))
    sub = sine(env([(0, 70), (d * 0.5, 95), (d, 60)], n), n) * env([(0, 0), (d * 0.5, 1), (d, 0)], n, 'cos')
    return room(y + 0.45 * sub, 0.3, 0.1)


def hit_slash(v=0):
    s = 500 + v * 7
    k = [1.0, 1.08, 0.93][v % 3]
    n = N(0.45)
    t = tt(n)
    tr = click(s, 2500, 0.004, 1.0)
    slice_ = tv_bp(white(n, s + 1), env([(0, 6000 * k), (0.08, 1800 * k), (0.45, 1200)], n, 'exp'), 1.5)
    slice_ *= np.exp(-t / 0.07) * np.clip(t / 0.001, 0, 1)
    wet = bp(white(n, s + 2), 500 * k, 2200 * k) * (0.5 + 0.5 * smooth_noise(n, 70, s + 3)) * np.exp(-t / 0.11)
    wet *= np.clip((t - 0.008) / 0.01, 0, 1)
    body = thump(150 * k, 65, 0.25, 0.02, 0.06, 1.5)
    y = 1.3 * slice_ + 0.7 * wet
    mix_at(y, tr, 0)
    mix_at(y, body * 0.9, 0)
    return room(y, 0.3, 0.12, seed=s)


def hit_blunt(v=0):
    s = 600 + v * 11
    k = [1.0, 0.9, 1.12][v % 3]
    n = N(0.5)
    t = tt(n)
    body = thump(120 * k, 42 * k, 0.5, 0.035, 0.16, 2.5)
    crunch = bp(white(n, s), 250, 1600) * np.exp(-t / 0.05) * (1 + crackle(n, 300, s + 1, 0.001) * 3)
    knock = resonator(click(s + 2, 200, 0.004), [420 * k, 950 * k], 12) * 6
    y = body * 1.1 + 0.7 * crunch
    mix_at(y, knock, 0)
    mix_at(y, click(s + 3, 1500, 0.003, 0.6), 0)
    return room(sat(y * 1.2, 1.1), 0.35, 0.14, seed=s)


def hit_pierce(v=0):
    n = N(0.4)
    t = tt(n)
    tick = click(701, 3500, 0.003, 0.9)
    zip_ = tv_bp(white(n, 702), env([(0, 4200), (0.09, 850), (0.4, 600)], n, 'exp'), 3.0) * np.exp(-t / 0.08)
    squish = lp(white(n, 703), 1400) * (0.5 + 0.5 * smooth_noise(n, 55, 704)) * np.exp(-t / 0.1)
    squish *= np.clip((t - 0.02) / 0.02, 0, 1)
    y = 1.4 * zip_ + 0.7 * squish + 0.8 * thump(170, 85, 0.4, 0.015, 0.05, 1.3)
    mix_at(y, tick, 0)
    return room(y, 0.3, 0.12)


def hit_crit(v=0):
    base = hit_slash(1)
    n = N(0.9)
    t = tt(n)
    kira = fm(2637, n, 1.41, 3.0 * np.exp(-t / 0.12)) * np.exp(-t / 0.25) * 0.5
    kira += fm(3951, n, 2.0, 1.5 * np.exp(-t / 0.1)) * np.exp(-t / 0.18) * 0.3
    sp = sparkle(0.9, 801, [91, 95, 98, 103], 0.04, 0.4, 0.2)
    shimmer = hp(white(n, 802), 7000) * np.exp(-t / 0.15) * 0.25
    y = pad_to(base, n) * 1.1 + kira + pad_to(sp, n) + shimmer
    return y


def hit_weak(v=0):
    n = N(0.45)
    m = metal(1480, [1.0, 2.32, 3.81, 5.43, 7.1], [0.25, 0.16, 0.1, 0.07, 0.045], [1, 0.8, 0.6, 0.4, 0.25], 901,
              dur=0.45)
    m2 = metal(1520, [1.0, 2.32, 3.81], [0.08, 0.06, 0.04], [1, 0.6, 0.4], 902, dur=0.2) * 0.35
    y = m
    mix_at(y, m2, N(0.055))
    mix_at(y, click(903, 3000, 0.003, 1.2), 0)
    mix_at(y, thump(260, 180, 0.08, 0.01, 0.02, 1.0) * 0.4, 0)
    return room(y, 0.35, 0.15)


def charge(v=0):
    d = 0.75
    n = N(d)
    t = tt(n)
    f = env([(0, 650), (0.62, 2600), (d, 2700)], n, 'exp')
    tone = (sine(f, n) + 0.35 * fm(f, n, 2.01, 0.8)) * env([(0, 0), (0.5, 0.6), (0.62, 1.0), (d, 0)], n)
    swell = hp(white(n, 1001), 4000) * env([(0, 0), (0.6, 0.35), (0.65, 0)], n)
    ping = I.bell(3951, 0.4, 0.8, 3, kind='glock')
    y = tone * 0.6 + swell
    mix_at(y, ping * 1.2, N(0.6))
    return y


def charge_release(v=0):
    w = swing_heavy(1)
    ex = explosion(0.45, 1101, 1.1, 0.8, 0.5)
    clang = metal(310, [1, 1.52, 2.27, 2.9], [0.3, 0.2, 0.12, 0.08], [1, 0.7, 0.5, 0.3], 1102, dur=0.6) * 0.5
    y = np.zeros(N(1.4))
    mix_at(y, w * 0.9, 0)
    mix_at(y, ex, N(0.2))
    mix_at(y, clang, N(0.2))
    mix_at(y, thump(90, 30, 0.9, 0.05, 0.35, 2.5) * 0.9, N(0.2))
    return y


def guard(v=0):
    n = N(0.8)
    t = tt(n)
    plate = metal(520, [1, 1.47, 2.09, 2.56, 3.37, 4.2, 5.3], [0.5, 0.35, 0.25, 0.2, 0.12, 0.08, 0.05],
                  [1, 0.8, 0.7, 0.5, 0.4, 0.3, 0.2], 1201, dur=0.8)
    plate = plate * (1 + 0.3 * sine(37, n))
    strike = bp(white(n, 1202), 1000, 7000) * np.exp(-t / 0.008)
    scrape = bp(white(n, 1203), 2000, 6000) * (0.5 + 0.5 * smooth_noise(n, 90, 1204)) * np.exp(-t / 0.12) * 0.4
    y = addl(plate * 0.9, strike, scrape, 0.7 * thump(140, 70, 0.3, 0.01, 0.06, 1.5))
    return room(y, 0.4, 0.15)


def morph(v=0):
    y = np.zeros(N(0.7))
    clank = metal(330, [1, 1.7, 2.6, 3.9, 5.2], [0.15, 0.1, 0.07, 0.05, 0.04], [1, 0.8, 0.6, 0.4, 0.3], 1301,
                  dur=0.3)
    mix_at(y, clank + 0.6 * thump(130, 70, 0.3, 0.01, 0.05, 1.5)[:len(clank)], 0)
    mix_at(y, click(1302, 1500, 0.006, 1.0), 0)
    n = N(0.18)
    slide = bp(white(n, 1303), 1200, 4500) * (0.5 + 0.5 * square(55, n)) * env([(0, 0), (0.03, 1), (0.18, 0)], n)
    mix_at(y, slide * 0.5, N(0.06))
    for k, p in enumerate((0.27, 0.295)):
        c = addl(click(1304 + k, 2500, 0.003, 1.0), resonator(click(1306 + k, 1000, 0.002), [3200, 5100], 25) * 4)
        mix_at(y, c, N(p))
    mix_at(y, metal(1900, [1, 2.7], [0.12, 0.06], [0.4, 0.2], 1308, dur=0.3), N(0.3))
    return room(y, 0.3, 0.12)


def phial_burst(v=0):
    y = np.zeros(N(1.4))
    mix_at(y, explosion(0.5, 1401, 1.2, 1.0, 0.6), 0)
    n = N(0.7)
    t = tt(n)
    energy = sine(env([(0, 2200), (0.4, 180)], n, 'exp'), n) * np.exp(-t / 0.2) * 0.5
    energy += fm(env([(0, 1500), (0.4, 120)], n, 'exp'), n, 1.5, 3 * np.exp(-t / 0.15)) * np.exp(-t / 0.2) * 0.3
    mix_at(y, energy, 0)
    mix_at(y, glass(0.6, 1402, 14, 0.5), 0)
    return y


def spirit(v=0):
    n = N(0.95)
    t = tt(n)
    sweep = sine(env([(0, 1400), (0.14, 4200), (0.95, 4300)], n, 'exp'), n) * env([(0, 0), (0.1, 0.5), (0.95, 0)], n)
    ring_ = metal(2210, [1, 1.5, 2.21, 2.98], [0.45, 0.35, 0.25, 0.18], [1, 0.7, 0.6, 0.4], 1501, dur=0.95)
    sh = hp(white(n, 1502), 5000) * np.exp(-t / 0.09) * np.clip(t / 0.01, 0, 1)
    sp = pad_to(sparkle(0.95, 1503, [93, 98, 100, 105], 0.05, 0.35, 0.18), n)
    return sweep * 0.4 + ring_ * 0.6 + sh * 0.9 + sp


def demon_mode(v=0):
    d = 0.95
    n = N(d)
    t = tt(n)
    swell = tv_lp(white(n, 1601), env([(0, 250), (0.12, 6000), (d, 400)], n, 'exp'), 0.9)
    swell *= env([(0, 0), (0.06, 1), (0.25, 0.6), (d, 0)], n)
    growl = sat(saw(55, n) + saw(82.5, n, 0.3) + 0.5 * saw(41, n), 3.0)
    growl = lp(growl, 900) * env([(0, 0), (0.08, 1), (d, 0)], n) * (1 + 0.4 * sine(13, n))
    fire = bp(crackle(n, 120, 1602, 0.001), 1500, 7000) * env([(0, 0), (0.1, 1), (d, 0)], n) * 3
    y = swell * 1.2 + 0.5 * growl + 0.5 * fire
    mix_at(y, thump(80, 35, 0.6, 0.04, 0.2, 2.0) * 0.8, 0)
    return room(y, 0.4, 0.15)


def gunlance_shell(v=0):
    y = explosion(0.3, 1701, 0.8, 1.0, 0.5)
    mix_at(y, metal(1500, [1, 2.3, 3.5], [0.05, 0.03, 0.02], [1, 0.6, 0.4], 1702, dur=0.15) * 0.4, 0)
    return room(y, 0.4, 0.15)


def gunlance_wyvernfire(v=0):
    total = 2.6
    y = np.zeros(N(total))
    cd = 1.45
    n = N(cd)
    t = tt(n)
    ramp = (t / cd) ** 1.6
    flame = tv_lp(white(n, 1801), 400 + 3500 * ramp, 0.8) * (0.3 + 0.7 * ramp)
    flame *= 1 + 0.4 * smooth_noise(n, 30, 1802)
    crk = bp(crackle(n, 40 + 260 * float(np.mean(ramp)), 1803, 0.0012), 1200, 8000) * ramp * 3
    rumble = sine(40 + 35 * ramp, n) * ramp * 0.7
    rise = lp(saw(80 * 2 ** (2 * ramp), n), 1800) * ramp ** 2 * 0.25
    mix_at(y, (flame + crk + rumble + rise) * env([(0, 0), (0.1, 0.5), (cd - 0.02, 1), (cd, 0.3)], n), 0)
    mix_at(y, explosion(1.0, 1804, 1.15, 1.0, 0.5) * 1.3, N(cd))
    y = y * env([(0, 1), (total - 0.25, 1), (total, 0)], len(y))
    return y


def reload(v=0):
    y = np.zeros(N(0.5))
    clack = addl(resonator(click(1901, 300, 0.004), [1800, 3500, 650], 18) * 5, click(1902, 2000, 0.003, 0.6))
    mix_at(y, clack, 0)
    mix_at(y, thump(250, 160, 0.08, 0.01, 0.02, 1.0) * 0.5, 0)
    n = N(0.1)
    mix_at(y, bp(white(n, 1903), 1500, 5000) * env([(0, 0), (0.02, 0.4), (0.1, 0)], n), N(0.06))
    ck = addl(click(1904, 4000, 0.003, 1.0), metal(5200, [1, 1.6], [0.03, 0.02], [0.5, 0.3], 1905, dur=0.1))
    mix_at(y, ck, N(0.28))
    return room(y, 0.25, 0.1)


def bowgun_light(v=0):
    n = N(0.4)
    t = tt(n)
    burst = hp(white(n, 2001), 1500) * np.exp(-t / 0.035)
    zipt = sine(env([(0, 2400), (0.09, 700)], n, 'exp'), n) * np.exp(-t / 0.06) * 0.5
    tw = ks(180, 0.3, 0.2, 0.7, 0.8, 0.1, 2002)
    y = burst + zipt + 0.4 * pad_to(tw, n)
    mix_at(y, click(2003, 3000, 0.003, 1.0), 0)
    return room(y, 0.3, 0.12)


def bowgun_heavy(v=0):
    y = explosion(0.45, 2101, 0.95, 1.0, 0.3)
    mix_at(y, click(2102, 2500, 0.006, 1.5), 0)
    n = N(0.25)
    rattle = bp(crackle(n, 90, 2103, 0.002), 1500, 6000) * 2 * env([(0, 0), (0.05, 1), (0.25, 0)], n)
    mix_at(y, rattle, N(0.12))
    return room(y, 0.5, 0.18)


def bowgun_gatling(v=0):
    y = np.zeros(N(0.22))
    for k, p in enumerate((0.0, 0.055)):
        n = N(0.12)
        t = tt(n)
        pop = hp(white(n, 2201 + k), 1200) * np.exp(-t / 0.012) + thump(140, 80, 0.12, 0.01, 0.03, 1.5) * 0.7
        mix_at(y, pop * (1 - 0.15 * k), N(p))
        mix_at(y, click(2203 + k, 3000, 0.002, 0.7), N(p))
    return y


def bow_draw(v=0):
    d = 0.7
    n = N(d)
    t = tt(n)
    y = np.zeros(n)
    r = rng(2301)
    tp = 0.02
    while tp < d - 0.02:
        rate = 22 + 45 * (tp / d)
        c = resonator(click(int(tp * 1e4), 500, 0.002), [r.uniform(850, 1400), r.uniform(2100, 2600)], 20) * 3
        mix_at(y, c * (0.3 + 0.7 * tp / d) * r.uniform(0.6, 1.0), N(tp))
        tp += 1 / rate * r.uniform(0.7, 1.3)
    tension = tv_bp(white(n, 2302), 380 + 700 * t / d, 4.0) * (t / d) * 0.4
    return y + tension


def bow_release(v=0):
    n = N(0.5)
    t = tt(n)
    tw = ks(110, 0.35, 0.22, 0.8, 0.9, 0.08, 2401)
    tw = np.interp(np.cumsum(1 + 0.15 * np.exp(-tt(len(tw)) / 0.05)), np.arange(len(tw)), tw, right=0)
    w = whoosh(0.3, [(0, 1500), (0.3, 4200), (1, 2000)], 1.6, 2402, 0.3)
    y = pad_to(tw, n) * 0.9
    mix_at(y, w * 0.8, N(0.01))
    mix_at(y, thump(160, 90, 0.1, 0.01, 0.025, 1.0) * 0.5, 0)
    return room(y, 0.3, 0.1)


def bow_piercer(v=0):
    y = np.zeros(N(1.5))
    cd = 0.55
    n = N(cd)
    t = tt(n)
    f = env([(0, 600), (cd, 3200)], n, 'exp')
    whine = (saw(f, n) * 0.3 + fm(f, n, 2.0, 1.2)) * (t / cd) ** 1.5
    whine = lp(whine, 9000) + hp(white(n, 2501), 3000) * (t / cd) ** 2 * 0.3
    mix_at(y, whine * 0.7, 0)
    mix_at(y, explosion(0.35, 2502, 0.8, 1.2, 0.2), N(cd))
    m = N(0.85)
    tm = tt(m)
    beam = tv_bp(white(m, 2503), env([(0, 3200), (0.85, 600)], m, 'exp'), 2.0) * np.exp(-tm / 0.3)
    beam += sine(1600 * (1 - 0.3 * tm), m) * np.exp(-tm / 0.25) * 0.4
    mix_at(y, beam, N(cd))
    return y


def kinsect(v=0):
    d = 1.1
    n = N(d)
    t = tt(n)
    f0 = 205 * (1 + 0.07 * np.tanh((0.5 - t / d) / 0.15)) * (1 + 0.02 * smooth_noise(n, 12, 2601))
    b = saw(f0, n) + 0.6 * square(f0 * 1.002, n, 0.3, 0.3)
    b = bp(b, 250, 4000) * (1 + 0.3 * sine(f0 * 0.5, n))
    a = np.exp(-((t - d * 0.5) / (d * 0.28)) ** 2)
    a = a * np.clip(t / 0.05, 0, 1) * np.clip((d - t) / 0.05, 0, 1)
    b = tv_lp(b, env([(0, 1800), (d * 0.5, 4500), (d, 1500)], n, 'exp'), 0.8)  # brighter when close
    return b * a


def horn_note(name):
    def f(v=0):
        fr = note_f(name)
        y = I.brass(fr, 0.65, 0.85, 'horn', 7, vib=6)
        y += 0.5 * pad_to(I.brass(fr, 0.65, 0.75, 'trumpet', 8, vib=4), len(y))
        return room(y, 0.6, 0.18)
    return f


def horn_song(v=0):
    y = np.zeros(N(2.0))
    for i, nm in enumerate(('C5', 'E5', 'G5')):
        mix_at(y, I.brass_section(note_f(nm), 0.12, 0.8, 'horn', 10 + i, players=2), N(i * 0.1))
    for i, nm in enumerate(('C5', 'E5', 'G5', 'C6')):
        mix_at(y, I.brass_section(note_f(nm), 0.85, 0.9, 'trumpet' if i == 3 else 'horn', 20 + i, players=2),
               N(0.32))
    mix_at(y, sparkle(1.2, 2701, [84, 88, 91, 96], 0.05, 0.25, 0.3), N(0.32))
    return room(y, 0.9, 0.2)


def vault(v=0):
    y = np.zeros(N(0.65))
    thunk = resonator(click(2801, 200, 0.005), [190, 470, 1100], 10) * 4
    mix_at(y, thunk + 0.5 * thump(180, 120, 0.12, 0.01, 0.03, 1.0)[:len(thunk)], 0)
    mix_at(y, whoosh(0.55, [(0, 500), (0.6, 3200), (1, 2500)], 1.3, 2802, 0.55, shape=1.3) * 1.1, N(0.03))
    n = N(0.45)
    mix_at(y, sine(env([(0, 700), (0.45, 1700)], n, 'exp'), n) * env([(0, 0), (0.25, 0.15), (0.45, 0)], n), N(0.08))
    return y


# ==========================================================================
# PLAYER
# ==========================================================================
def dodge(v=0):
    y = np.zeros(N(0.6))
    n = N(0.32)
    rustle = bp(white(n, 2901), 900, 5500) * (0.4 + 0.6 * np.abs(smooth_noise(n, 45, 2902)))
    rustle *= env([(0, 0), (0.04, 1), (0.32, 0)], n)
    mix_at(y, rustle * 0.7, 0)
    for k, (p, a) in enumerate(((0.12, 1.0), (0.3, 0.6))):
        roll = thump(95, 55, 0.2, 0.02, 0.05, 1.2) + nburst(0.2, None, 400, 0.003, 0.04, 2903 + k, 0.8)
        mix_at(y, roll * a, N(p))
    return room(y, 0.25, 0.08)


def hurt(v=0):
    y = np.zeros(N(0.55))
    mix_at(y, thump(110, 45, 0.4, 0.02, 0.13, 2.0), 0)
    mix_at(y, nburst(0.2, 300, 2500, 0.001, 0.04, 3001, 0.8), 0)
    mix_at(y, click(3002, 1500, 0.004, 0.8), 0)
    g = I.shout(150, 0.26, [(0, 'u'), (0.06, 'a'), (0.26, 'a')], 'tenor', 3003, 0.35, (1.15, 1.0, 0.8))
    mix_at(y, g * 1.2, N(0.02))
    return room(y, 0.3, 0.1)


def sharpen(v=0):
    y = np.zeros(N(1.55))
    for k, p in enumerate((0.0, 0.48, 0.96)):
        d = 0.36
        n = N(d)
        t = tt(n)
        fc = env([(0, 5200), (d, 3200)] if k % 2 == 0 else [(0, 3400), (d, 5600)], n, 'exp')
        grind = tv_bp(white(n, 3101 + k), fc, 1.2) * (1 + 2.5 * np.abs(crackle(n, 500, 3110 + k, 0.0008)))
        grind *= env([(0, 0), (0.03, 1), (d * 0.7, 0.8), (d, 0)], n)
        ringv = metal(3100, [1, 1.52, 2.0], [0.15, 0.1, 0.08], [1, 0.6, 0.4], 3120 + k, dur=0.4) * 0.12
        mix_at(y, grind + tailfade(pad_to(ringv, n), 0.4), N(p))
    mix_at(y, metal(4200, [1, 1.5, 2.2], [0.25, 0.18, 0.1], [1, 0.6, 0.4], 3130, dur=0.5) * 0.25, N(1.3))
    return y


def drink(v=0):
    y = np.zeros(N(1.15))
    for k, p in enumerate((0.0, 0.27, 0.54)):
        n = N(0.16)
        t = tt(n)
        gulp = sine(env([(0, 190), (0.16, 115)], n, 'exp'), n) * np.exp(-t / 0.05) * np.clip(t / 0.01, 0, 1)
        gulp = gulp + bp(white(n, 3201 + k), 250, 900) * np.exp(-t / 0.04) * 0.6
        gulp = formant(gulp, 'bass', 'o') * 3 + gulp * 0.5
        mix_at(y, gulp, N(p))
        mix_at(y, I.bubble(380 + 60 * k, 0.05, 0.6, k, up=1.6), N(p + 0.07))
    n = N(0.3)
    ah = I.shout(170, 0.28, [(0, 'a'), (0.28, 'a')], 'tenor', 3210, 0.6, (1.1, 1.0, 0.9), h_onset=0.06) * 0.5
    mix_at(y, ah, N(0.82))
    return room(y, 0.25, 0.08)


def heal(v=0):
    y = np.zeros(N(1.2))
    mix_at(y, sparkle(1.1, 3301, [84, 86, 88, 91, 93, 96, 98], 0.07, 0.8, 0.35), 0)
    n = N(1.1)
    pad = (sine(523.25, n) + sine(783.99, n) * 0.6 + sine(1046.5, n) * 0.4) * env([(0, 0), (0.25, 0.3), (1.1, 0)], n,
                                                                                    'cos')
    mix_at(y, pad * 0.5, 0)
    mix_at(y, hp(white(n, 3302), 6000) * env([(0, 0), (0.2, 0.15), (1.1, 0)], n), 0)
    return y


def eat(v=0):
    y = np.zeros(N(2.1))
    r = rng(3401)
    p = 0.0
    for k in range(5):
        n = N(0.1)
        t = tt(n)
        chew = bp(white(n, 3402 + k), 500, 3500) * (1 + 3 * np.abs(crackle(n, 400, 3410 + k, 0.0008)))
        chew = chew * env([(0, 0), (0.01, 1), (0.1, 0)], n) + thump(150, 110, 0.1, 0.01, 0.03, 1.0) * 0.5
        mix_at(y, chew * r.uniform(0.6, 1.0), N(p))
        p += r.uniform(0.17, 0.25)
    for i, nm in enumerate(('C5', 'E5', 'G5', 'C6')):
        mix_at(y, I.marimba(note_f(nm), 0.4, 0.8, i) * 1.4, N(1.2 + i * 0.09))
    for nm in ('E6', 'G6'):
        mix_at(y, I.bell(note_f(nm), 0.3, 0.6, 3, kind='glock'), N(1.2 + 0.36))
    return room(tailfade(y, 0.2), 0.35, 0.12)


def buff(v=0):
    y = np.zeros(N(1.0))
    for i, nm in enumerate(('C5', 'E5', 'G5', 'C6', 'E6')):
        f = note_f(nm)
        n = N(0.5)
        t = tt(n)
        tone = (square(f, n, 0, 0.3) * 0.3 + fm(f, n, 2.0, 1.5 * np.exp(-t / 0.1))) * np.exp(-t / 0.18)
        mix_at(y, lp(tone, 7000) * 0.6, N(i * 0.07))
    mix_at(y, whoosh(0.6, [(0, 800), (1, 6000)], 1.5, 3501, 0.8) * 0.6, 0)
    mix_at(y, sparkle(0.8, 3502, [96, 100, 103], 0.06, 0.4, 0.25), N(0.3))
    return room(tailfade(y, 0.2), 0.5, 0.15)


def faint(v=0):
    y = np.zeros(N(2.0))
    mix_at(y, thump(85, 40, 0.5, 0.03, 0.15, 2.0), 0)
    mix_at(y, nburst(0.3, 200, 2000, 0.003, 0.06, 3601, 0.7), 0)
    for i, (nm, p, d) in enumerate((('B4', 0.3, 0.25), ('G4', 0.58, 0.25), ('E4', 0.86, 0.25))):
        mix_at(y, I.flute(note_f(nm), d, 0.7, i, kind='ocarina') * 1.4, N(p))
    n = N(0.85)
    t = tt(n)
    f = note_f('D#4') * 2 ** (-np.clip((t - 0.25) / 0.6, 0, 1) * 5 / 12)
    last = (sine(f, n) + 0.08 * sine(f * 2, n)) * env([(0, 0), (0.04, 0.7), (0.5, 0.6), (0.85, 0)], n)
    mix_at(y, last, N(1.14))
    return room(y, 0.5, 0.15)


def cart(v=0):
    y = np.zeros(N(2.2))
    r = rng(3701)
    p = 0.0
    while p < 1.25:
        c = resonator(click(int(p * 1e4) + 3, 300, 0.004), [r.uniform(700, 900), r.uniform(1700, 2300)], 9) * 3
        mix_at(y, c * r.uniform(0.5, 1.0) * min(1, p / 0.1 + 0.3) * (1.1 - p / 1.4), N(p))
        p += 1 / 17 * r.uniform(0.8, 1.2)
    n = N(1.3)
    rumble = lp(brown(n, 3702), 160) * env([(0, 0), (0.1, 1), (1.2, 0.6), (1.3, 0)], n) * 0.6
    mix_at(y, rumble, 0)
    sq = sine(1900 * (1 + 0.06 * sine(7, N(0.25))), N(0.25)) * env([(0, 0), (0.05, 0.12), (0.25, 0)], N(0.25))
    mix_at(y, sq, N(0.5))
    for i, nm in enumerate(('G5', 'C6', 'E6', 'G6')):
        mix_at(y, I.bell(note_f(nm), 0.25, 0.7, i, kind='glock') * 1.6, N(1.28 + i * 0.06))
    return tailfade(y, 0.25)


def carve(v=0):
    n = N(0.4)
    t = tt(n)
    scrape = tv_bp(white(n, 3801), env([(0, 6500), (0.12, 2200)], n, 'exp'), 1.5) * np.exp(-t / 0.06)
    wet = lp(white(n, 3802), 1600) * (0.5 + 0.5 * smooth_noise(n, 60, 3803)) * np.exp(-t / 0.09)
    y = scrape * 1.1 + wet * 0.6 + thump(160, 90, 0.4, 0.01, 0.04, 1.2) * 0.7
    mix_at(y, click(3804, 3000, 0.003, 0.8), 0)
    return room(y, 0.25, 0.1)


# ==========================================================================
# ITEMS
# ==========================================================================
def paintball(v=0):
    n = N(0.42)
    t = tt(n)
    splat = lp(white(n, 3901), 2600) * np.exp(-t / 0.05) * np.clip(t / 0.002, 0, 1)
    squish = bp(white(n, 3902), 350, 1300) * (0.5 + 0.5 * smooth_noise(n, 40, 3903)) * np.exp(-t / 0.12)
    y = splat * 1.2 + squish * 0.8
    mix_at(y, sine(900, N(0.015)) * np.exp(-tt(N(0.015)) / 0.004) * 0.6, 0)
    return room(y, 0.25, 0.1)


def flash(v=0):
    n = N(1.5)
    t = tt(n)
    crack = hp(white(n, 4001), 2500) * np.exp(-t / 0.025) * 1.2
    shards = metal(3300, [1, 1.38, 1.83, 2.2], [0.06, 0.05, 0.04, 0.03], [1, 0.8, 0.6, 0.4], 4002, dur=0.3)
    tinn = (sine(3600, n) + sine(3613, n) + 0.3 * sine(7210, n)) * env([(0, 0), (0.03, 0.9), (0.3, 0.6), (1.5, 0)],
                                                                         n, 'cos')
    flashw = hp(white(n, 4003), 5000) * env([(0, 0), (0.01, 0.6), (0.12, 0)], n)
    y = crack * 0.6 + pad_to(shards, n) * 0.6 + tinn * 0.8 + flashw
    return y


def trap_set(v=0):
    y = np.zeros(N(0.55))
    clank = metal(620, [1, 1.83, 2.71, 3.9, 5.1], [0.12, 0.08, 0.06, 0.04, 0.03], [1, 0.8, 0.6, 0.4, 0.3], 4101,
                  dur=0.3)
    mix_at(y, clank + 0.5 * thump(140, 90, 0.3, 0.01, 0.04, 1.2)[:len(clank)], 0)
    n = N(0.4)
    t = tt(n)
    spring = sine(220 * (1 + 0.15 * np.sin(TAU * 18 * t) * np.exp(-t / 0.12)), n) * np.exp(-t / 0.12) * 0.3
    mix_at(y, spring, N(0.04))
    for k, p in enumerate((0.0, 0.16)):
        mix_at(y, click(4102 + k, 2500, 0.003, 1.0), N(p))
    return room(y, 0.3, 0.1)


def trap_shock(v=0):
    d = 2.0
    y = zap(d, 4201, 45, 60, 1.0, 0.7)
    n = len(y)
    hum = sat(saw(120, n) * 2, 1.0)
    hum = bp(hum, 150, 2500) * (0.6 + 0.4 * smooth_noise(n, 10, 4202))
    y = y + 0.4 * hum
    return y * env([(0, 0), (0.02, 1), (d - 0.35, 0.8), (d, 0)], n)


def pitfall(v=0):
    y = np.zeros(N(1.0))
    mix_at(y, thump(75, 32, 0.7, 0.04, 0.22, 2.0), N(0.05))
    n = N(0.9)
    t = tt(n)
    dirt = lp(white(n, 4301), 900) * (0.3 + np.abs(crackle(n, 250, 4302, 0.002)) * 4) * env([(0, 0), (0.02, 1), (0.9, 0)],
                                                                                            n)
    mix_at(y, dirt * 0.8, 0)
    suck = formant(lp(white(N(0.25), 4303), 1200), 'bass', 'o') * env([(0, 0), (0.05, 1), (0.25, 0)], N(0.25)) * 3
    mix_at(y, suck, 0)
    return room(y, 0.35, 0.12)


def tranq(v=0):
    n = N(0.5)
    t = tt(n)
    puff = lp(white(n, 4401), 1300) * np.clip(t / 0.006, 0, 1) * np.exp(-t / 0.09)
    hiss = hp(white(n, 4402), 4000) * env([(0, 0), (0.03, 0.2), (0.5, 0)], n)
    y = tailfade(puff * 1.3 + hiss, 0.3)
    mix_at(y, metal(4100, [1, 1.6], [0.03, 0.02], [0.4, 0.2], 4403, dur=0.1), 0)
    return room(y, 0.3, 0.12)


def bomb(v=0):
    y = explosion(1.25, 4501, 2.3, 1.0, 0.7)
    mix_at(y, thump(60, 22, 1.6, 0.08, 0.6, 2.5) * 0.7, 0)
    return y * env([(0, 1), (2.0, 1), (2.3, 0)], len(y))


def farcaster(v=0):
    y = np.zeros(N(1.0))
    n = N(0.3)
    puff = lp(white(n, 4601), 1100) * np.clip(tt(n) / 0.008, 0, 1) * np.exp(-tt(n) / 0.1)
    mix_at(y, puff * 1.3, 0)
    mix_at(y, whoosh(0.6, [(0, 500), (0.8, 4500), (1, 5500)], 1.5, 4602, 0.75, shape=1.2), N(0.18))
    m = N(0.5)
    zipt = sine(env([(0, 400), (0.5, 2200)], m, 'exp'), m) * env([(0, 0), (0.3, 0.35), (0.5, 0)], m)
    mix_at(y, zipt, N(0.25))
    return y


# ==========================================================================
# MONSTERS
# ==========================================================================
def roar_sakeran(v=0):
    d = 2.5
    y = roar(d, [(0, 62), (0.35, 92), (1.4, 84), (2.0, 70), (2.5, 52)],
             [(0, 'o'), (0.3, 'a'), (1.6, 'a'), (2.5, 'o')], 'bass', 0.72, 4701, growl=0.7, sub=0.7, subharm=0.8,
             dist=3.0, breath=0.3, vib_rate=4.0, vib_c=35)
    n = len(y)
    chest = thump(70, 40, 0.4, 0.03, 0.15, 2.0)
    mix_at(y, chest * 0.6, 0)
    return room(y, 0.6, 0.15)


def roar_barino(v=0):
    d = 2.5
    n = N(d)
    base = roar(d, [(0, 105), (0.4, 150), (1.6, 135), (2.5, 95)], [(0, 'o'), (0.3, 'a'), (2.5, 'a')], 'tenor', 0.85,
                4801, growl=0.35, sub=0.4, subharm=0.3, dist=1.8, breath=0.25)
    ch = np.zeros(n)
    for i, (nm, voice) in enumerate((('C4', 'tenor'), ('G4', 'alto'), ('C5', 'soprano'), ('E5', 'soprano'),
                                     ('G5', 'soprano'))):
        src = I.choir_src(note_f(nm), d - 0.5, 0.8, 4810 + i, voices=3, attack=0.3, release=0.45)
        ch += pad_to(formant(src, voice, 'a', bw_scale=1.3), n)
    ch = ch / (np.max(np.abs(ch)) + 1e-9)
    bellv = pad_to(I.bell(note_f('C4'), d, 0.9, 4820, kind='church', ring=False), n)
    shimmer = hp(white(n, 4821), 6000) * env([(0, 0), (0.3, 0.12), (d, 0)], n)
    y = 0.8 * base + 0.7 * ch + 0.6 * bellv + shimmer
    y = hall(y, 1.6, 0.35)[:n]
    return y * env([(0, 1), (d - 0.3, 1), (d, 0)], n)


def roar_jangria(v=0):
    d = 2.5
    n = N(d)
    t = tt(n)
    a = roar(d, [(0, 50), (0.4, 62), (1.7, 58), (2.5, 38)], [(0, 'a'), (0.6, 'a'), (1.5, 'u'), (2.5, 'u')], 'bass',
             0.68, 4901, growl=0.9, sub=0.6, subharm=0.9, dist=5.0, breath=0.4)
    b = roar(d, [(0, 71), (0.4, 88), (1.7, 82), (2.5, 54)], [(0, 'e'), (0.6, 'a'), (2.5, 'o')], 'bass', 0.8, 4902,
             growl=0.8, sub=0.0, subharm=0.5, dist=4.0, breath=0.3)
    y = a + 0.7 * b
    y = y * (0.7 + 0.3 * sine(31, n))
    rev_swell = tv_lp(white(n, 4903), 300 + 5000 * np.clip(t / 0.35, 0, 1), 0.8) * env([(0, 0), (0.33, 0.6), (0.4, 0)],
                                                                                       n)
    fire = bp(crackle(n, 90, 4904, 0.0012), 1500, 7000) * env([(0, 0), (0.3, 1), (d, 0)], n) * 2
    y = sat(y * 1.5, 1.0) + rev_swell + 0.4 * fire
    return room(y, 0.7, 0.18)[:n] * env([(0, 1), (d - 0.15, 1), (d, 0)], n)


def roar_busaiber(v=0):
    d = 2.5
    n = N(d)
    t = tt(n)
    base = roar(d, [(0, 95), (0.4, 135), (1.6, 120), (2.5, 85)], [(0, 'o'), (0.35, 'a'), (1.8, 'a'), (2.5, 'o')],
                'bass', 0.8, 5001, growl=0.5, sub=0.5, subharm=0.5, dist=2.5, breath=0.25)
    # robot layer: staircase-quantised pitch square voice through formants (vocoder-ish)
    steps = np.floor(t * 14) / 14
    f = 110 * 2 ** (np.round(env([(0, 0), (0.4, 5), (1.6, 3), (2.5, -2)], n) + 0 * steps) / 12)
    rob = formant_tv(square(f, n, 0, 0.3), [(0, 'o'), (0.35, 'a'), (2.5, 'o')], 'bass', 0.9)
    rob = rob / (np.max(np.abs(rob)) + 1e-9)
    y = 0.7 * base + 0.6 * rob
    y = crush(y * 0.9, 5) * 0.7 + 0.3 * y
    hold = srhold(y, 6)
    y = np.where(smooth_noise(n, 6, 5002) > 0.2, hold, y)
    y = y * (0.75 + 0.25 * square(440, n, 0, 0.5) * (smooth_noise(n, 5, 5003) > 0.4))
    y = stutter(y, 0.62, 0.045, 5)
    y = stutter(y, 1.75, 0.03, 6, 0.9)
    r = rng(5004)
    for k in range(7):
        mix_at(y, I.glitch_burst(r.uniform(0.03, 0.08), 5010 + k, 1.2), N(r.uniform(0.1, 2.3)))
    return room(y, 0.4, 0.12)[:n] * env([(0, 1), (d - 0.12, 1), (d, 0)], n)


def roar_buskoba(v=0):
    d = 2.5
    n = N(d)
    t = tt(n)
    y = roar(d, [(0, 120), (0.4, 165), (1.6, 140), (2.5, 95)], [(0, 'o'), (0.5, 'u'), (1.0, 'a'), (1.6, 'o'),
                                                                (2.0, 'u'), (2.5, 'o')], 'bass', 0.85, 5101,
             growl=0.3, sub=0.6, subharm=0.4, dist=2.0, breath=0.2, vib_rate=6.0, vib_c=140)
    y = y * (0.75 + 0.25 * np.sin(TAU * 6.0 * t))
    r = rng(5102)
    for k in range(26):
        p = r.uniform(0.05, d - 0.15)
        mix_at(y, I.bubble(r.uniform(250, 900), r.uniform(0.05, 0.12), r.uniform(0.5, 1.0), k,
                           up=r.uniform(1.5, 2.4)) * 1.4, N(p))
    blub = lp(white(n, 5103), 500) * (0.5 + 0.5 * np.sin(TAU * 11 * t)) * env([(0, 0), (0.3, 0.6), (d, 0)], n)
    y = y + 0.6 * blub + 0.5 * sine(48 * (1 + 0.05 * np.sin(TAU * 6 * t)), n) * env([(0, 0), (0.2, 1), (d, 0)], n)
    return room(y, 0.5, 0.15)[:n] * env([(0, 1), (d - 0.12, 1), (d, 0)], n)


def monster_step(v=0):
    s = 5200 + 13 * v
    k = [1.0, 0.9, 1.1][v % 3]
    y = np.zeros(N(0.8))
    mix_at(y, thump(62 * k, 30 * k, 0.75, 0.03, 0.2, 2.2), 0)
    mix_at(y, nburst(0.15, None, 320, 0.002, 0.05, s, 1.0), 0)
    n = N(0.3)
    crunch = bp(crackle(n, 350, s + 1, 0.0012), 700, 3500) * 4 * env([(0, 0), (0.01, 1), (0.3, 0)], n)
    mix_at(y, crunch * 0.6, N(0.005))
    m = N(0.7)
    mix_at(y, lp(brown(m, s + 2), 120) * env([(0, 0), (0.05, 0.5), (0.7, 0)], m), 0)
    return y


def slam(v=0):
    y = np.zeros(N(1.9))
    mix_at(y, thump(55, 22, 1.8, 0.05, 0.55, 3.0), 0)
    mix_at(y, click(5301, 1200, 0.01, 1.4), 0)
    n = N(1.2)
    deb = bp(crackle(n, 220, 5302, 0.0018), 400, 4000) * 4 * env([(0, 0), (0.02, 1), (1.2, 0)], n)
    mix_at(y, deb * 0.7, N(0.01))
    m = N(1.9)
    mix_at(y, lp(brown(m, 5303), 150) * env([(0, 0), (0.06, 1), (1.9, 0)], m) * 1.2, 0)
    mix_at(y, nburst(0.5, None, 1500, 0.002, 0.12, 5304, 1.0), 0)
    return sat(y * 1.2, 1.1)


def swipe(v=0):
    d = 0.65
    y = whoosh(d, [(0, 180), (0.5, 900), (1, 300)], 0.8, 5401, 0.5, shape=1.6) * 1.2
    y += whoosh(d, [(0, 900), (0.5, 3000), (1, 1100)], 1.5, 5402, 0.5, shape=2.0) * 0.4
    n = len(y)
    y += sine(env([(0, 45), (d * 0.5, 70), (d, 40)], n), n) * env([(0, 0), (d * 0.5, 1), (d, 0)], n, 'cos') * 0.7
    return room(y, 0.4, 0.1)


def monster_hurt(v=0):
    d = 0.9
    y = roar(d, [(0, 150), (0.15, 175), (0.9, 95)], [(0, 'a'), (0.4, 'a'), (0.9, 'u')], 'bass', 0.8, 5501,
             growl=0.6, sub=0.4, subharm=0.6, dist=2.5, breath=0.35,
             amp_pts=[(0, 0), (0.03, 1), (0.25, 0.8), (0.9, 0)])
    return room(y, 0.5, 0.15)


def monster_down(v=0):
    y = np.zeros(N(1.5))
    mix_at(y, thump(70, 30, 1.0, 0.04, 0.3, 2.5), 0)
    mix_at(y, nburst(0.6, 150, 2500, 0.003, 0.12, 5601, 1.0), 0)
    mix_at(y, thump(60, 35, 0.6, 0.03, 0.15, 2.0) * 0.6, N(0.19))
    n = N(1.2)
    dust = bp(crackle(n, 160, 5602, 0.002), 500, 3000) * 3 * env([(0, 0), (0.02, 1), (1.2, 0)], n)
    mix_at(y, dust * 0.5, 0)
    g = roar(0.7, [(0, 110), (0.7, 70)], [(0, 'o'), (0.7, 'u')], 'bass', 0.75, 5603, growl=0.5, dist=2.0,
             amp_pts=[(0, 0), (0.1, 0.5), (0.7, 0)])
    mix_at(y, g * 0.5, N(0.45))
    return room(y, 0.5, 0.12)


def monster_death(v=0):
    d = 3.0
    n = N(d)
    t = tt(n)
    y = roar(d, [(0, 150), (0.3, 185), (1.0, 165), (2.2, 90), (3.0, 55)],
             [(0, 'a'), (0.8, 'a'), (1.6, 'o'), (2.4, 'u'), (3.0, 'u')], 'bass', 0.78, 5701, growl=0.7, sub=0.5,
             subharm=0.7, dist=3.0, breath=0.35, amp_pts=[(0, 0), (0.06, 0.9), (0.6, 1.0), (1.8, 0.7), (2.6, 0.25),
                                                          (3.0, 0)])
    y = y * (1 - 0.35 * np.clip((t - 1.6) / 0.8, 0, 1) * (0.5 + 0.5 * np.sin(TAU * 7 * t)))
    breath_ = formant(pink(n, 5702), 'bass', 'u') * env([(0, 0), (2.2, 0), (2.5, 0.4), (3.0, 0)], n) * 2
    return room(y + breath_, 0.7, 0.15)[:n] * env([(0, 1), (d - 0.1, 1), (d, 0)], n)


def monster_fire(v=0):
    d = 1.5
    n = N(d)
    t = tt(n)
    flame = tv_lp(white(n, 5801), 1400 + 900 * smooth_noise(n, 9, 5802), 0.8) * (1 + 0.35 * smooth_noise(n, 35, 5803))
    rum = lp(brown(n, 5804), 220) * 1.2
    crk = hp(crackle(n, 160, 5805, 0.0008), 2000) * 3
    a = env([(0, 0), (0.08, 1), (d - 0.4, 0.85), (d, 0)], n, 'cos')
    y = (flame * 1.2 + rum + crk * 0.5) * a
    mix_at(y, whoosh(0.35, [(0, 300), (0.5, 2500), (1, 1200)], 1.0, 5806, 0.4) * 0.7, 0)
    return y


def monster_beam(v=0):
    d = 1.5
    n = N(d)
    t = tt(n)
    f = 420 * (1 + 0.08 * np.clip(t / 0.25, 0, 1)) * (1 + 0.004 * smooth_noise(n, 30, 5901))
    tone = saw(f, n) + saw(f * 1.004, n, 0.3) + 0.6 * saw(f * 1.5, n, 0.6)
    tone = bp(tone, 300, 6000) + 0.5 * fm(f * 2, n, 2.5, 1.5)
    whine = sine(3200 * (1 + 0.01 * np.sin(TAU * 7 * t)), n) * 0.25
    hiss = hp(white(n, 5902), 5000) * 0.25
    a = env([(0, 0), (0.06, 1), (d - 0.3, 0.9), (d, 0)], n, 'cos')
    y = (tone * 0.45 + whine + hiss) * a
    y = y * (1 + 0.15 * np.sin(TAU * 31 * t))
    mix_at(y, nburst(0.2, 800, 8000, 0.002, 0.05, 5903, 0.8), 0)
    return y


def monster_water(v=0):
    y = splash(1.1, 6001, 1.4)
    mix_at(y, whoosh(0.4, [(0, 400), (0.5, 2000), (1, 900)], 0.9, 6002, 0.3) * 0.8, 0)
    return room(y, 0.4, 0.12)


def monster_electric(v=0):
    y = zap(0.95, 6101, 85, 100, 1.0, 0.6)
    mix_at(y, click(6102, 1500, 0.006, 1.5), 0)
    return y * env([(0, 1), (0.6, 0.8), (0.95, 0)], len(y))


def monster_holy(v=0):
    d = 1.9
    n = N(d)
    t = tt(n)
    shine = (sine(1760, n) + sine(1762.5, n) + 0.6 * sine(2640, n) + 0.4 * sine(3520, n) + 0.2 * sine(5280, n))
    shine = shine * env([(0, 0), (0.05, 0.6), (0.5, 0.45), (d, 0)], n, 'cos')
    bellv = pad_to(I.bell(note_f('C5'), d, 0.9, 6201, kind='church', ring=False), n)
    burst = hp(white(n, 6202), 3500) * env([(0, 0), (0.02, 0.8), (0.25, 0)], n)
    ch = formant(I.choir_src(note_f('C5'), 0.9, 0.6, 6203, attack=0.05), 'soprano', 'a')
    y = shine * 0.5 + bellv + burst + 0.4 * pad_to(ch, n) / (np.max(np.abs(ch)) + 1e-9)
    return hall(y, 1.4, 0.3)[:n] * env([(0, 1), (d - 0.2, 1), (d, 0)], n)


def wing(v=0):
    y = np.zeros(N(0.6))
    for k, (p, a) in enumerate(((0.0, 1.0), (0.09, 0.6))):
        n = N(0.35)
        t = tt(n)
        f = bp(pink(n, 6301 + k), 120, 1400) * env([(0, 0), (0.06, 1), (0.35, 0)], n) * (1 + 0.5 * sine(32, n))
        mix_at(y, f * a, N(p))
    mix_at(y, thump(70, 50, 0.25, 0.02, 0.07, 1.0) * 0.5, 0)
    return room(y, 0.3, 0.1)


def tired(v=0):
    y = np.zeros(N(1.75))
    for k, p in enumerate((0.0, 0.55)):
        n = N(0.42)
        src = pink(n, 6401 + k) + 0.15 * saw(95, n)
        haa = formant_tv(src, [(0, 'a'), (0.3, 'a'), (0.42, 'o')], 'bass', 0.8) * env([(0, 0), (0.05, 1), (0.42, 0)], n)
        mix_at(y, haa * 2.2, N(p))
    n = N(0.4)
    slurp = bp(white(n, 6403), 400, 1800) * (0.4 + 0.6 * np.abs(smooth_noise(n, 30, 6404))) * env([(0, 0), (0.05, 1),
                                                                                                   (0.4, 0)], n)
    mix_at(y, slurp * 0.6, N(1.05))
    mix_at(y, I.bubble(1200, 0.04, 0.6, 1, up=1.5), N(1.5))
    return room(y, 0.4, 0.12)


def snore(v=0):
    d = 2.3
    y = np.zeros(N(d))
    n = N(1.15)
    t = tt(n)
    flutter = 0.5 + 0.5 * np.sin(TAU * 27 * t)
    inh = (lp(white(n, 6501), 700) * 1.2 + 0.4 * lp(saw(58, n), 500)) * flutter
    inh *= env([(0, 0), (0.35, 0.8), (0.9, 1.0), (1.15, 0)], n, 'cos')
    mix_at(y, inh, 0)
    m = N(0.95)
    ex = formant(pink(m, 6502), 'bass', 'u') * 2.5 + 0.2 * lp(saw(110, m), 600)
    ex *= env([(0, 0), (0.15, 0.6), (0.95, 0)], m, 'cos')
    mix_at(y, ex, N(1.25))
    return y


def part_break(v=0):
    y = np.zeros(N(1.5))
    crack = addl(click(6601, 900, 0.012, 1.6), bp(crackle(N(0.25), 400, 6602, 0.001), 800, 7000) * 4 * expdec(N(0.25), 0.06))
    mix_at(y, crack, 0)
    mix_at(y, thump(100, 45, 0.4, 0.02, 0.1, 2.0), 0)
    stab = np.zeros(N(1.3))
    for i, nm in enumerate(('C4', 'E4', 'G4', 'C5')):
        mix_at(stab, I.brass_section(note_f(nm), 0.35, 0.95, 'trumpet' if i > 1 else 'horn', 6610 + i, players=2), 0)
    mix_at(stab, I.timpani(note_f('C2'), 0.9, 6620, 1.3), 0)
    mix_at(stab, I.cymbal(0.9, 6621, decay=0.8), 0)
    mix_at(y, tailfade(stab, 0.35) * 1.2, N(0.1))
    return room(y, 0.6, 0.15)


def monster_glitch(v=0):
    y = np.zeros(N(0.6))
    r = rng(6701)
    p = 0.0
    while p < 0.45:
        g = I.glitch_burst(r.uniform(0.02, 0.07), int(p * 1e4) + 7, 1.0)
        mix_at(y, g * 3, N(p))
        p += r.uniform(0.03, 0.08)
    n = N(0.5)
    sw = crush(tv_bp(white(n, 6702), env([(0, 6000), (0.5, 300)], n, 'exp'), 3.0), 4) * 0.6
    mix_at(y, srhold(sw, 8), 0)
    y = stutter(y, 0.2, 0.025, 4)
    return y * env([(0, 1), (0.5, 0.8), (0.6, 0)], len(y))


def bottle(v=0):
    y = np.zeros(N(1.0))
    mix_at(y, glass(0.9, 6801, 34, 1.0), 0)
    mix_at(y, thump(180, 100, 0.15, 0.01, 0.03, 1.0) * 0.5, 0)
    n = N(0.5)
    liquid = lp(white(n, 6802), 1500) * (0.5 + 0.5 * smooth_noise(n, 30, 6803)) * env([(0, 0), (0.03, 0.6), (0.5, 0)], n)
    mix_at(y, liquid * 0.5, N(0.02))
    return room(y, 0.35, 0.12)


# ==========================================================================
# STATUS
# ==========================================================================
def st_poison(v=0):
    y = np.zeros(N(1.15))
    r = rng(6901)
    p = 0.0
    while p < 0.95:
        mix_at(y, I.bubble(r.uniform(140, 380), r.uniform(0.07, 0.14), r.uniform(0.6, 1.0), int(p * 1e3),
                           up=r.uniform(1.6, 2.4)) * 1.8, N(p))
        p += r.uniform(0.07, 0.17)
    n = N(1.15)
    murk = lp(white(n, 6902), 400) * (0.5 + 0.5 * smooth_noise(n, 15, 6903)) * env([(0, 0), (0.1, 0.4), (1.15, 0)], n)
    return formant(y, 'bass', 'o') * 2.0 + y * 0.6 + murk


def st_sleep(v=0):
    y = np.zeros(N(1.9))
    n = N(1.0)
    src = 0.5 * pink(n, 7001) + 0.6 * saw(env([(0, 230), (1.0, 150)], n, 'exp'), n)
    yawn = formant_tv(src, [(0, 'a'), (0.5, 'a'), (1.0, 'o')], 'alto', 1.0) * env([(0, 0), (0.15, 0.8), (0.6, 0.7),
                                                                                   (1.0, 0)], n, 'cos')
    mix_at(y, yawn * 1.5, 0)
    for i, nm in enumerate(('E6', 'B5', 'G5')):
        mix_at(y, I.bell(note_f(nm), 0.4, 0.5, i, kind='celesta') * 1.2, N(0.25 + i * 0.2))
    return room(tailfade(y, 0.3), 0.6, 0.15)


def st_paralysis(v=0):
    d = 0.85
    n = N(d)
    t = tt(n)
    gate = (np.sin(TAU * 24 * t) > -0.2).astype(float)
    gate = onepole_lp(gate, 300)
    b = sat(square(72, n, 0, 0.3) * 2, 1.0) * gate
    b = bp(b, 100, 5000)
    y = b * 0.6 + zap(d, 7101, 70, 140, 0.8, 0.3)
    return y * env([(0, 0), (0.01, 1), (d - 0.2, 0.8), (d, 0)], n)


def st_blast(v=0):
    return room(explosion(0.4, 7201, 0.75, 1.1, 0.6), 0.4, 0.12)


def st_buskoba(v=0):
    d = 1.1
    n = N(d)
    t = tt(n)
    f = 200 * (1 + 0.15 * t) * (1 + 0.35 * np.exp(-t / 0.35) * np.sin(TAU * 9 * t))
    boing = (sine(f, n) + 0.3 * sine(f * 2, n) + 0.1 * tri(f * 3, n)) * env([(0, 0), (0.01, 1), (d, 0)], n)
    y = boing * 0.8
    r = rng(7301)
    for k in range(9):
        mix_at(y, I.bubble(r.uniform(400, 1200), 0.06, 0.6, k, up=2.0) * 0.8, N(r.uniform(0.15, 0.9)))
    return room(y, 0.3, 0.1)


def st_stun(v=0):
    y = np.zeros(N(1.35))
    for k, p in enumerate((0.0, 0.3, 0.62, 0.92)):
        hi = 1 + 0.06 * (k % 2)
        n1 = N(0.07)
        pi_ = sine(env([(0, 2100 * hi), (0.07, 2900 * hi)], n1, 'exp'), n1) * np.sin(np.pi * tt(n1) / 0.07)
        n2 = N(0.11)
        yo = sine(env([(0, 2900 * hi), (0.11, 2200 * hi)], n2, 'exp'), n2) * np.sin(np.pi * tt(n2) / 0.11)
        mix_at(y, pi_ * 0.8, N(p))
        mix_at(y, yo * 0.7, N(p + 0.09))
    mix_at(y, sparkle(1.2, 7401, [96, 100, 103, 100, 96], 0.22, 0.25, 0.12), 0)
    return room(y, 0.3, 0.1)


# ==========================================================================
# ELEMENTS (short add-on layers)
# ==========================================================================
def el_fire(v=0):
    n = N(0.45)
    t = tt(n)
    b = bp(white(n, 7501), 500, 4500) * np.clip(t / 0.01, 0, 1) * np.exp(-t / 0.13)
    crk = hp(crackle(n, 200, 7502, 0.0008), 2000) * 4 * np.exp(-t / 0.15)
    rum = lp(brown(n, 7503), 250) * np.exp(-t / 0.15)
    return b + crk * 0.6 + rum * 0.8


def el_water(v=0):
    return splash(0.45, 7601, 0.6)


def el_thunder(v=0):
    y = zap(0.4, 7701, 120, 130, 1.0, 0.4)
    mix_at(y, click(7702, 1200, 0.008, 2.0), 0)
    return y * env([(0, 1), (0.25, 0.6), (0.4, 0)], len(y))


def el_ice(v=0):
    y = np.zeros(N(0.55))
    mix_at(y, hp(white(N(0.05), 7801), 3000) * expdec(N(0.05), 0.008) * 0.6, 0)
    mix_at(y, glass(0.5, 7802, 14, 0.8), 0)
    mix_at(y, metal(4700, [1, 1.33, 1.9], [0.2, 0.15, 0.1], [1, 0.6, 0.4], 7803, dur=0.5) * 0.4, 0)
    return y


def el_dragon(v=0):
    d = 0.55
    n = N(d)
    t = tt(n)
    g = sat(saw(70, n) * sine(113, n) * 3, 1.0)
    g = lp(g, 1600) * env([(0, 0), (0.05, 1), (d, 0)], n)
    sw = tv_lp(white(n, 7901), env([(0, 300), (0.12, 3500), (d, 500)], n, 'exp'), 1.0) * env([(0, 0), (0.1, 1), (d, 0)],
                                                                                                  n)
    shimmer = fm(220, n, 3.03, 2.0) * env([(0, 0), (0.2, 0.3), (d, 0)], n)
    return g * 0.6 + sw * 0.8 + shimmer * 0.3


# ==========================================================================
# UI
# ==========================================================================
def ui_select(v=0):
    n = N(0.07)
    t = tt(n)
    return (sine(1850, n) + 0.25 * square(3700, n, 0, 0.5)) * np.clip(t / 0.001, 0, 1) * np.exp(-t / 0.018)


def ui_confirm(v=0):
    n = N(0.28)
    t = tt(n)
    f = 1046.5 * (1 + 0.02 * np.exp(-t / 0.01))
    y = sine(f, n) * np.exp(-t / 0.08) + 0.35 * sine(f * 2, n) * np.exp(-t / 0.04) + 0.3 * sine(523.25, n) * np.exp(
        -t / 0.1)
    return y * np.clip(t / 0.001, 0, 1)


def ui_back(v=0):
    n = N(0.18)
    t = tt(n)
    f = env([(0, 560), (0.08, 430)], n, 'exp')
    return (sine(f, n) + 0.2 * sine(f * 2, n)) * np.clip(t / 0.001, 0, 1) * np.exp(-t / 0.05)


def ui_open(v=0):
    y = np.zeros(N(0.45))
    n = N(0.18)
    flip = bp(white(n, 8001), 900, 6000) * (0.4 + 0.6 * np.abs(np.sin(TAU * 28 * tt(n)))) * env([(0, 0), (0.02, 1),
                                                                                                 (0.18, 0)], n)
    mix_at(y, flip * 0.7, 0)
    knock = resonator(click(8002, 150, 0.004), [260, 610, 1350], 10) * 5
    mix_at(y, knock, N(0.03))
    mix_at(y, click(8003, 3000, 0.002, 0.5), N(0.16))
    return room(y, 0.25, 0.1)


def ui_accept(v=0):
    y = np.zeros(N(1.2))
    mix_at(y, thump(115, 55, 0.3, 0.015, 0.07, 2.0), 0)
    mix_at(y, nburst(0.05, 500, 3500, 0.001, 0.012, 8101, 1.0), 0)
    stab = np.zeros(N(1.0))
    for i, nm in enumerate(('C4', 'G4', 'C5', 'E5', 'G5')):
        mix_at(stab, I.brass_section(note_f(nm), 0.32, 0.95, 'trumpet' if i > 2 else 'horn', 8110 + i, players=2), 0)
    mix_at(stab, I.cymbal(0.8, 8120, decay=0.6), 0)
    mix_at(stab, I.timpani(note_f('C2'), 0.8, 8121, 1.0), 0)
    mix_at(y, tailfade(stab, 0.35) * 1.1, N(0.12))
    return room(y, 0.5, 0.15)


def ui_error(v=0):
    y = np.zeros(N(0.3))
    for p in (0.0, 0.14):
        n = N(0.1)
        b = lp(square(155, n, 0, 0.5) + 0.5 * saw(156, n), 1600) * env([(0, 0), (0.004, 1), (0.03, 0.75), (0.08, 0.5),
                                                                       (0.1, 0)], n)
        mix_at(y, b * 0.6, N(p))
    return y


def ui_reward(v=0):
    y = np.zeros(N(0.8))
    for k, p in enumerate((0.0, 0.075)):
        c = metal(3150 * (1 + 0.06 * k), [1, 1.46, 2.18, 2.9], [0.3, 0.2, 0.14, 0.1], [1, 0.7, 0.5, 0.35], 8201 + k,
                  dur=0.7)
        mix_at(y, c * (1 - 0.2 * k), N(p))
        mix_at(y, click(8203 + k, 4000, 0.002, 0.5), N(p))
    mix_at(y, sparkle(0.6, 8205, [100, 103, 108], 0.05, 0.3, 0.15), N(0.1))
    return y


# ==========================================================================
# QUEST
# ==========================================================================
def q_start(v=0):
    d = 2.2
    n = N(d)
    t = tt(n)
    f = 98 * cents(-180 * np.exp(-t / 0.12) + 15 * np.clip((t - 1.5) / 0.4, 0, 1))
    # cow-horn: band-limited saw with dynamic low-pass + throat formant
    src = saw(f, n) + 0.5 * saw(f * 1.003, n, 0.4) + 0.3 * square(f * 0.5, n, 0.2, 0.45)
    a = env([(0, 0), (0.25, 0.9), (1.3, 1.0), (1.75, 0.8), (d, 0)], n, 'cos')
    y = tv_lp(src, 300 + 2600 * a, 1.2, stages=2)
    y = y + 0.6 * formant(y, 'bass', 'o', 0.9)
    y = y * a * (1 + 0.04 * np.sin(TAU * 5 * t))
    y = y + 0.08 * bp(white(n, 8310), 800, 3000) * a
    return hall(y, 1.8, 0.35, seed=8311)[:N(d + 0.6)]


def q_area(v=0):
    y = np.zeros(N(1.5))
    mix_at(y, I.bell(note_f('E6'), 0.3, 0.75, 1, kind='chime'), 0)
    mix_at(y, I.bell(note_f('B6'), 0.4, 0.7, 2, kind='chime'), N(0.13))
    return room(tailfade(y, 0.3), 0.6, 0.15)


def q_alert(v=0):
    y = np.zeros(N(1.4))
    for i, nm in enumerate(('D3', 'A3', 'D4', 'Eb4', 'A4')):
        mix_at(y, I.brass_section(note_f(nm), 0.55, 1.0, 'trombone' if i < 2 else 'horn', 8400 + i, players=2), 0)
    for nm in ('D6', 'Eb6'):
        mix_at(y, I.strings(note_f(nm), 0.6, 0.9, 8410, tremolo=0.7, attack=0.01), 0)
    mix_at(y, I.timpani(note_f('D2'), 1.0, 8420, 1.4), 0)
    mix_at(y, I.gran_cassa(0.9, 8421)[:N(1.4)], 0)
    mix_at(y, I.cymbal(0.9, 8422, decay=0.9), 0)
    return room(tailfade(y, 0.35), 0.6, 0.15)


def q_timer(v=0):
    y = np.zeros(N(1.25))
    for k, p in enumerate((0.0, 0.3, 0.6, 0.9)):
        mix_at(y, I.woodblock(0.9 if k % 2 == 0 else 0.75, k, f=1750 if k % 2 == 0 else 1250) * 1.5, N(p))
    n = N(1.2)
    pulse = sine(880, n) * (0.5 + 0.5 * np.sin(TAU * 3.33 * tt(n) - np.pi / 2)) * 0.06
    mix_at(y, pulse, 0)
    return room(y, 0.25, 0.08)


# ==========================================================================
# REGISTRY: key -> (path, fn, variations, ja, en)
# ==========================================================================
REGISTRY = [
    ('weapon.swing_light', 'sfx/weapon/swing_light', swing_light, 3, '武器を振る', 'Weapon swings'),
    ('weapon.swing_heavy', 'sfx/weapon/swing_heavy', swing_heavy, 3, '重い武器を振る', 'Heavy weapon swings'),
    ('weapon.swing_blunt', 'sfx/weapon/swing_blunt', swing_blunt, 1, '打撃武器を振る', 'Blunt weapon swings'),
    ('weapon.hit_slash', 'sfx/weapon/hit_slash', hit_slash, 3, '斬撃が命中', 'Slash hits'),
    ('weapon.hit_blunt', 'sfx/weapon/hit_blunt', hit_blunt, 3, '打撃が命中', 'Blunt hit'),
    ('weapon.hit_pierce', 'sfx/weapon/hit_pierce', hit_pierce, 1, '突きが命中', 'Pierce hits'),
    ('weapon.hit_crit', 'sfx/weapon/hit_crit', hit_crit, 1, '会心の一撃', 'Critical hit'),
    ('weapon.hit_weak', 'sfx/weapon/hit_weak', hit_weak, 1, '攻撃が弾かれる', 'Attack bounces off'),
    ('weapon.charge', 'sfx/weapon/charge', charge, 1, '溜め段階アップ', 'Charge level up'),
    ('weapon.charge_release', 'sfx/weapon/charge_release', charge_release, 1, '溜め攻撃', 'Charged attack'),
    ('weapon.guard', 'sfx/weapon/guard', guard, 1, 'ガード', 'Guard'),
    ('weapon.morph', 'sfx/weapon/morph', morph, 1, '武器変形', 'Weapon morphs'),
    ('weapon.phial_burst', 'sfx/weapon/phial_burst', phial_burst, 1, 'ビン解放', 'Phial burst'),
    ('weapon.spirit', 'sfx/weapon/spirit', spirit, 1, '練気ゲージ上昇', 'Spirit gauge rises'),
    ('weapon.demon_mode', 'sfx/weapon/demon_mode', demon_mode, 1, '鬼人化', 'Demon mode'),
    ('weapon.gunlance_shell', 'sfx/weapon/gunlance_shell', gunlance_shell, 1, '砲撃', 'Gunlance shelling'),
    ('weapon.gunlance_wyvernfire', 'sfx/weapon/gunlance_wyvernfire', gunlance_wyvernfire, 1, '竜撃砲',
     "Wyvern's Fire"),
    ('weapon.reload', 'sfx/weapon/reload', reload, 1, 'リロード', 'Reload'),
    ('weapon.bowgun_light', 'sfx/weapon/bowgun_light', bowgun_light, 1, 'ライトボウガン発射', 'Light bowgun fires'),
    ('weapon.bowgun_heavy', 'sfx/weapon/bowgun_heavy', bowgun_heavy, 1, 'ヘビィボウガン発射', 'Heavy bowgun fires'),
    ('weapon.bowgun_gatling', 'sfx/weapon/bowgun_gatling', bowgun_gatling, 1, '機関竜弾', 'Wyvernheart fires'),
    ('weapon.bow_draw', 'sfx/weapon/bow_draw', bow_draw, 1, '弓を引き絞る', 'Bow draws'),
    ('weapon.bow_release', 'sfx/weapon/bow_release', bow_release, 1, '矢を放つ', 'Arrow released'),
    ('weapon.bow_piercer', 'sfx/weapon/bow_piercer', bow_piercer, 1, '竜の一矢', 'Dragon Piercer'),
    ('weapon.kinsect', 'sfx/weapon/kinsect', kinsect, 1, '猟虫の羽音', 'Kinsect buzzes'),
    ('weapon.horn_note1', 'sfx/weapon/horn_note1', horn_note('C5'), 1, '狩猟笛の音色', 'Hunting horn note'),
    ('weapon.horn_note2', 'sfx/weapon/horn_note2', horn_note('E5'), 1, '狩猟笛の音色', 'Hunting horn note'),
    ('weapon.horn_note3', 'sfx/weapon/horn_note3', horn_note('G5'), 1, '狩猟笛の音色', 'Hunting horn note'),
    ('weapon.horn_song', 'sfx/weapon/horn_song', horn_song, 1, '旋律効果発動', 'Song performed'),
    ('weapon.vault', 'sfx/weapon/vault', vault, 1, '跳躍', 'Vault'),
    ('player.dodge', 'sfx/player/dodge', dodge, 1, '回避', 'Player dodges'),
    ('player.hurt', 'sfx/player/hurt', hurt, 1, '被弾', 'Player hurt'),
    ('player.sharpen', 'sfx/player/sharpen', sharpen, 1, '砥石で研ぐ', 'Sharpening'),
    ('player.drink', 'sfx/player/drink', drink, 1, '回復薬を飲む', 'Drinking potion'),
    ('player.heal', 'sfx/player/heal', heal, 1, '体力回復', 'Health restored'),
    ('player.eat', 'sfx/player/eat', eat, 1, '食事', 'Eating'),
    ('player.buff', 'sfx/player/buff', buff, 1, '強化効果', 'Buff applied'),
    ('player.faint', 'sfx/player/faint', faint, 1, '力尽きた', 'Player fainted'),
    ('player.cart', 'sfx/player/cart', cart, 1, 'ネコ車で運ばれる', 'Carted away'),
    ('player.carve', 'sfx/player/carve', carve, 1, '剥ぎ取り', 'Carving'),
    ('item.paintball', 'sfx/item/paintball', paintball, 1, 'ペイントボール', 'Paintball splats'),
    ('item.flash', 'sfx/item/flash', flash, 1, '閃光玉', 'Flash pod bursts'),
    ('item.trap_set', 'sfx/item/trap_set', trap_set, 1, '罠を設置', 'Trap set'),
    ('item.trap_shock', 'sfx/item/trap_shock', trap_shock, 1, 'シビレ罠作動', 'Shock trap crackles'),
    ('item.pitfall', 'sfx/item/pitfall', pitfall, 1, '落とし穴', 'Pitfall trap'),
    ('item.tranq', 'sfx/item/tranq', tranq, 1, '捕獲用麻酔玉', 'Tranq bomb'),
    ('item.bomb', 'sfx/item/bomb', bomb, 1, '大タル爆弾', 'Barrel bomb explodes'),
    ('item.farcaster', 'sfx/item/farcaster', farcaster, 1, 'モドリ玉', 'Farcaster'),
    ('monster.roar_sakeran', 'sfx/monster/roar_sakeran', roar_sakeran, 1, 'サケランの咆哮', 'Sakeran roars'),
    ('monster.roar_barino', 'sfx/monster/roar_barino', roar_barino, 1, 'ブスコバリーノ王の咆哮',
     'King Buskobarino roars'),
    ('monster.roar_jangria', 'sfx/monster/roar_jangria', roar_jangria, 1, 'ジャングリアの咆哮', 'Jangria roars'),
    ('monster.roar_busaiber', 'sfx/monster/roar_busaiber', roar_busaiber, 1, 'ブサイバーの咆哮', 'Busaiber roars'),
    ('monster.roar_buskoba', 'sfx/monster/roar_buskoba', roar_buskoba, 1, 'ブスコバの咆哮', 'Buskoba roars'),
    ('monster.step', 'sfx/monster/step', monster_step, 3, '大型モンスターの足音', 'Heavy footsteps'),
    ('monster.slam', 'sfx/monster/slam', slam, 1, '地響き', 'Monster slams'),
    ('monster.swipe', 'sfx/monster/swipe', swipe, 1, '薙ぎ払い', 'Monster swipes'),
    ('monster.hurt', 'sfx/monster/hurt', monster_hurt, 1, 'モンスターが怯む', 'Monster flinches'),
    ('monster.down', 'sfx/monster/down', monster_down, 1, 'モンスターが転倒', 'Monster topples'),
    ('monster.death', 'sfx/monster/death', monster_death, 1, 'モンスターの断末魔', 'Monster dies'),
    ('monster.fire', 'sfx/monster/fire', monster_fire, 1, '火炎ブレス', 'Fire breath'),
    ('monster.beam', 'sfx/monster/beam', monster_beam, 1, 'レーザー', 'Beam fires'),
    ('monster.water', 'sfx/monster/water', monster_water, 1, '水ブレス', 'Water blast'),
    ('monster.electric', 'sfx/monster/electric', monster_electric, 1, '放電', 'Electric discharge'),
    ('monster.holy', 'sfx/monster/holy', monster_holy, 1, '聖なる光の炸裂', 'Holy light bursts'),
    ('monster.wing', 'sfx/monster/wing', wing, 1, '羽ばたき', 'Wings flap'),
    ('monster.tired', 'sfx/monster/tired', tired, 1, 'モンスターが疲労', 'Monster pants'),
    ('monster.sleep', 'sfx/monster/sleep', snore, 1, 'モンスターのいびき', 'Monster snores'),
    ('monster.part_break', 'sfx/monster/part_break', part_break, 1, '部位破壊', 'Part broken'),
    ('monster.glitch', 'sfx/monster/glitch', monster_glitch, 1, 'グリッチ', 'Glitch teleport'),
    ('monster.bottle', 'sfx/monster/bottle', bottle, 1, '酒瓶が割れる', 'Bottle shatters'),
    ('status.poison', 'sfx/status/poison', st_poison, 1, '毒', 'Poisoned'),
    ('status.sleep', 'sfx/status/sleep', st_sleep, 1, '睡眠', 'Falling asleep'),
    ('status.paralysis', 'sfx/status/paralysis', st_paralysis, 1, '麻痺', 'Paralyzed'),
    ('status.blast', 'sfx/status/blast', st_blast, 1, '爆破', 'Blastblight explodes'),
    ('status.buskoba', 'sfx/status/buskoba', st_buskoba, 1, 'ブスコバ状態', 'Buskoba-fied'),
    ('status.stun', 'sfx/status/stun', st_stun, 1, '気絶', 'Stunned'),
    ('element.fire', 'sfx/element/fire', el_fire, 1, '火属性ダメージ', 'Fire damage'),
    ('element.water', 'sfx/element/water', el_water, 1, '水属性ダメージ', 'Water damage'),
    ('element.thunder', 'sfx/element/thunder', el_thunder, 1, '雷属性ダメージ', 'Thunder damage'),
    ('element.ice', 'sfx/element/ice', el_ice, 1, '氷属性ダメージ', 'Ice damage'),
    ('element.dragon', 'sfx/element/dragon', el_dragon, 1, '龍属性ダメージ', 'Dragon damage'),
    ('ui.select', 'ui/select', ui_select, 1, 'カーソル移動', 'Cursor moves'),
    ('ui.confirm', 'ui/confirm', ui_confirm, 1, '決定', 'Confirm'),
    ('ui.back', 'ui/back', ui_back, 1, '戻る', 'Back'),
    ('ui.open', 'ui/open', ui_open, 1, 'メニューを開く', 'Menu opens'),
    ('ui.accept', 'ui/accept', ui_accept, 1, 'クエスト受注', 'Quest accepted'),
    ('ui.error', 'ui/error', ui_error, 1, '操作できない', 'Not allowed'),
    ('ui.reward', 'ui/reward', ui_reward, 1, '報酬獲得', 'Reward obtained'),
    ('quest.start', 'quest/start', q_start, 1, 'クエスト開始', 'Quest starts'),
    ('quest.area', 'quest/area', q_area, 1, 'エリア移動', 'Area changed'),
    ('quest.alert', 'quest/alert', q_alert, 1, 'モンスター発見', 'Monster spotted'),
    ('quest.timer', 'quest/timer', q_timer, 1, '残り時間わずか', 'Time running out'),
]
