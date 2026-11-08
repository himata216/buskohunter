package com.buskohunter.client.anim;

/** 小さなキーフレーム補間ヘルパー（t は 0〜1、値は度数で書いてラジアンに変換）。 */
public final class K {
	private K() {
	}

	public static final float DEG = (float) (Math.PI / 180.0);

	/**
	 * k(t, t0, v0, t1, v1, ...) — 時刻と値（度）の組を滑らかに補間してラジアンで返す。
	 */
	public static float k(float t, float... tv) {
		return kRaw(t, tv) * DEG;
	}

	/** 補間（単位変換なし）。 */
	public static float kRaw(float t, float... tv) {
		if (tv.length < 2) return 0f;
		if (t <= tv[0]) return tv[1];
		for (int i = 0; i + 3 < tv.length; i += 2) {
			float t0 = tv[i], v0 = tv[i + 1], t1 = tv[i + 2], v1 = tv[i + 3];
			if (t <= t1) {
				float u = (t1 - t0) <= 1e-6f ? 1f : (t - t0) / (t1 - t0);
				u = u * u * (3f - 2f * u);
				return v0 + (v1 - v0) * u;
			}
		}
		return tv[tv.length - 1];
	}

	public static float clamp01(float v) {
		return v < 0 ? 0 : (v > 1 ? 1 : v);
	}

	public static float sin(float v) {
		return (float) Math.sin(v);
	}

	public static float cos(float v) {
		return (float) Math.cos(v);
	}
}
