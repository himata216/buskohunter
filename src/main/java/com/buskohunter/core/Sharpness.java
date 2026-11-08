package com.buskohunter.core;

/** 切れ味。ゲージは色ごとのポイントで構成され、ヒットごとに減っていく。 */
public final class Sharpness {
	public enum Color {
		RED(0.50f, 0.25f, 0xD83030),
		ORANGE(0.75f, 0.50f, 0xF08A20),
		YELLOW(1.00f, 0.75f, 0xF0E030),
		GREEN(1.05f, 1.00f, 0x50D040),
		BLUE(1.20f, 1.0625f, 0x3080F0),
		WHITE(1.32f, 1.15f, 0xF0F0F0),
		PURPLE(1.39f, 1.20f, 0xC060F0);

		public final float raw;
		public final float element;
		public final int rgb;

		Color(float raw, float element, int rgb) {
			this.raw = raw;
			this.element = element;
			this.rgb = rgb;
		}
	}

	/** 色ごとの長さ（RED→PURPLE）。 */
	private final int[] lengths;
	private final int total;

	public Sharpness(int... lengths) {
		this.lengths = new int[Color.values().length];
		int t = 0;
		for (int i = 0; i < this.lengths.length && i < lengths.length; i++) {
			this.lengths[i] = lengths[i];
			t += lengths[i];
		}
		this.total = t;
	}

	public int total() {
		return total;
	}

	public int length(Color c) {
		return lengths[c.ordinal()];
	}

	/** 残りポイントから現在の色を求める。 */
	public Color colorAt(int remaining) {
		int acc = 0;
		Color last = Color.RED;
		for (Color c : Color.values()) {
			if (lengths[c.ordinal()] <= 0) continue;
			acc += lengths[c.ordinal()];
			last = c;
			if (remaining <= acc) return c;
		}
		return last;
	}

	public int[] lengths() {
		return lengths.clone();
	}

	/** ガンナー用（切れ味なし）。 */
	public static final Sharpness NONE = new Sharpness(0, 0, 0, 0, 0, 0, 0);
	public static final Sharpness STANDARD = new Sharpness(40, 40, 50, 60, 70, 40, 0);
	public static final Sharpness LONG_WHITE = new Sharpness(30, 40, 40, 50, 60, 80, 0);
	public static final Sharpness SHORT_PURPLE = new Sharpness(40, 40, 40, 60, 60, 30, 20);
	public static final Sharpness BLUE_LONG = new Sharpness(40, 40, 50, 70, 120, 0, 0);
}
