package com.buskohunter.combat;

/** 攻撃入力の種類（クライアントのボタン状態からサーバーで判定）。 */
public enum InputKind {
	/** 攻撃ボタン（左クリック）。 */
	A,
	/** サブ攻撃（右クリック）。 */
	B,
	/** 特殊アクション（R）。 */
	S,
	/** ガード（しゃがみ）中の左。 */
	GUARD_A,
	/** ガード（しゃがみ）中の右。 */
	GUARD_B,
	/** ガード（しゃがみ）中の特殊。 */
	GUARD_S;

	/** 入力ビット（クライアント→サーバーのパケット）。 */
	public static final int HELD_A = 1;
	public static final int HELD_B = 1 << 1;
	public static final int HELD_S = 1 << 2;
	public static final int HELD_GUARD = 1 << 3;
	public static final int PRESS_A = 1 << 4;
	public static final int PRESS_B = 1 << 5;
	public static final int PRESS_S = 1 << 6;
	public static final int PRESS_DODGE = 1 << 7;
	public static final int DIR_FORWARD = 1 << 8;
	public static final int DIR_BACK = 1 << 9;
	public static final int DIR_LEFT = 1 << 10;
	public static final int DIR_RIGHT = 1 << 11;
	public static final int AIRBORNE = 1 << 12;

	/** このボタンの押しっぱなし判定ビット。 */
	public int heldBit() {
		return switch (this) {
			case A, GUARD_A -> HELD_A;
			case B, GUARD_B -> HELD_B;
			case S, GUARD_S -> HELD_S;
		};
	}

	public InputKind base() {
		return switch (this) {
			case GUARD_A -> A;
			case GUARD_B -> B;
			case GUARD_S -> S;
			default -> this;
		};
	}
}
