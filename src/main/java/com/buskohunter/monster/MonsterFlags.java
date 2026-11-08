package com.buskohunter.monster;

/** モンスターの見た目・状態フラグ（クライアントへ同期）。 */
public final class MonsterFlags {
	private MonsterFlags() {
	}

	public static final int ENRAGED = 1;
	public static final int TIRED = 1 << 1;
	public static final int LIMPING = 1 << 2;
	public static final int FLYING = 1 << 3;
	public static final int BUSKOBA = 1 << 4;
	public static final int ASLEEP = 1 << 5;
	public static final int DEAD = 1 << 6;
	public static final int CAPTURED = 1 << 7;
	public static final int POSING = 1 << 8;
	public static final int GLITCH = 1 << 9;
	public static final int PAINTED = 1 << 10;
	public static final int TRAPPED = 1 << 11;
	public static final int HOVER = 1 << 12;
	public static final int CAPTURABLE = 1 << 13;
	public static final int BLINDED = 1 << 14;
	public static final int STUNNED = 1 << 15;
}
