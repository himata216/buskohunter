package com.buskohunter.combat;

/**
 * プレイヤーのモーション種類。サーバーは技ごとにこれを指定し、クライアントの
 * PlayerAnimator が腕・胴・脚のポーズとして再生する。
 */
public enum AnimKind {
	NONE,
	// --- 汎用斬撃 ---
	SLASH_DOWN, SLASH_SIDE_R, SLASH_SIDE_L, SLASH_UP, THRUST, THRUST_HIGH, SPIN,
	DOUBLE_SLASH, DUAL_FLURRY, DUAL_SPIN, DUAL_WHEEL,
	// --- 重量武器 ---
	CHARGE_OVERHEAD, CHARGE_LOW, SLAM, UPPERCUT, HOMERUN, TACKLE, WIDE_SWEEP,
	// --- 盾・ガード ---
	SHIELD_BASH, SHIELD_UPPER, GUARD, COUNTER_THRUST,
	// --- ガンナー ---
	SHOOT, SHOOT_HEAVY, GATLING, RELOAD, BOW_DRAW, BOW_SHOOT, BOW_POWER, PIERCER,
	// --- 操虫棍 ---
	VAULT, AERIAL_SLASH, DIVE_THRUST, KINSECT_LAUNCH,
	// --- 変形武器 ---
	MORPH, DISCHARGE, SUPER_DISCHARGE, PHIAL_CHARGE,
	// --- ガンランス ---
	GUNLANCE_SHELL, WYVERNFIRE, FULL_BURST,
	// --- 狩猟笛 ---
	HORN_SWING, HORN_SLAM, HORN_BACK, HORN_PLAY,
	// --- 太刀 ---
	FORESIGHT, SPIRIT_SLASH, SPIRIT_ROUNDSLASH, HELM_BREAKER,
	// --- ランス ---
	LANCE_CHARGE,
	// --- 移動・状態 ---
	DODGE_ROLL, SIDESTEP, DEMON_STEP, KNOCKED_DOWN, STAGGER, ROAR_COVER,
	SHARPEN, FLEX, CARVE, TRIUMPH, THROW, PLACE;

	public static AnimKind byOrdinal(int ordinal) {
		AnimKind[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NONE;
	}
}
