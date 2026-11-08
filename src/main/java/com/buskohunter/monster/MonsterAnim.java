package com.buskohunter.monster;

/** モンスターのモーション（クライアントのモデルがポーズとして再生）。 */
public enum MonsterAnim {
	IDLE, WALK, RUN, ROAR, SWIPE_R, SWIPE_L, DOUBLE_SWIPE, PUNCH, DOUBLE_PUNCH, LARIAT, CHARGE, LEAP, SLAM,
	BREATH, BEAM, THROW, SPIN, HIP_ATTACK, HIP_DROP, BOUNCE, FLY_UP, HOVER, DIVE, TAUNT, POSE, GROUND_POUND,
	WING_GUST, TELEPORT, SUMMON, BUTT_BEAM, FLINCH, TIRED, SLEEP, DOWN, STUN, TRAPPED, PITFALL, DEATH, LIMP,
	EAT, FLEE;

	public static MonsterAnim byOrdinal(int ordinal) {
		MonsterAnim[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : IDLE;
	}
}
