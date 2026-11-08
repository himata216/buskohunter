"""Dev tool: render log-frequency spectrograms (PNG) of arrays / audio files
for visual QA.  python3 specview.py in.ogg out.png"""
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw

SR = 44100


def load(path):
    raw = subprocess.run(['ffmpeg', '-loglevel', 'error', '-i', path, '-f', 'f32le', '-ac', '1', '-ar', str(SR), '-'],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype=np.float32).astype(float)


def spec_img(x, width=900, height=300, fmin=30, fmax=18000, nfft=2048, title=''):
    x = np.asarray(x, float)
    if x.ndim == 2:
        x = x.mean(axis=1)
    hop = max(64, len(x) // width)
    win = np.hanning(nfft)
    frames = []
    for s in range(0, max(1, len(x) - nfft), hop):
        frames.append(np.abs(np.fft.rfft(x[s:s + nfft] * win)))
    if not frames:
        frames = [np.zeros(nfft // 2 + 1)]
    S = np.array(frames).T
    S = 20 * np.log10(S + 1e-9)
    S -= S.max()
    freqs = np.fft.rfftfreq(nfft, 1 / SR)
    logf = np.geomspace(fmin, fmax, height)
    idx = np.clip(np.searchsorted(freqs, logf), 0, len(freqs) - 1)
    img = S[idx][::-1]
    img = np.clip((img + 90) / 90, 0, 1)
    rgb = np.stack([img ** 0.7, img ** 1.5, (1 - img) * img * 3], -1)
    rgb = (np.clip(rgb, 0, 1) * 255).astype(np.uint8)
    im = Image.fromarray(rgb).resize((width, height))
    # waveform strip
    wav = Image.new('RGB', (width, 80), (10, 10, 10))
    d = ImageDraw.Draw(wav)
    seg = max(1, len(x) // width)
    for i in range(width):
        c = x[i * seg:(i + 1) * seg]
        if len(c):
            a, b = c.min(), c.max()
            d.line([(i, 40 - b * 38), (i, 40 - a * 38)], fill=(120, 220, 120))
    out = Image.new('RGB', (width, height + 100), (0, 0, 0))
    out.paste(im, (0, 0))
    out.paste(wav, (0, height + 20))
    dd = ImageDraw.Draw(out)
    dd.text((4, height + 3), f'{title}  {len(x) / SR:.2f}s  peak {np.max(np.abs(x)):.3f}', fill=(255, 255, 255))
    for f in (100, 1000, 10000):
        yy = height - int(np.log(f / fmin) / np.log(fmax / fmin) * height)
        dd.line([(0, yy), (12, yy)], fill=(255, 255, 255))
        dd.text((14, yy - 6), f'{f}', fill=(255, 255, 255))
    return out


def grid(items, path, cols=2, width=600, height=200):
    ims = [spec_img(x, width, height, title=t) for t, x in items]
    rows = (len(ims) + cols - 1) // cols
    W, H = ims[0].size
    out = Image.new('RGB', (W * cols, H * rows))
    for i, im in enumerate(ims):
        out.paste(im, ((i % cols) * W, (i // cols) * H))
    out.save(path)


if __name__ == '__main__':
    x = load(sys.argv[1])
    spec_img(x, title=sys.argv[1].split('/')[-1]).save(sys.argv[2])
