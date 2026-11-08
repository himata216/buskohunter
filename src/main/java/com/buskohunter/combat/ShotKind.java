package com.buskohunter.combat;

/** プレイヤーが撃つ弾・矢の種類。 */
public enum ShotKind {
	NORMAL("通常弾", 1, 0, 3.2f, 0.0f, 9, 1.0f),
	PIERCE("貫通弾", 3, 0, 2.8f, 0.0f, 6, 0.55f),
	SPREAD("散弾", 1, 5, 2.2f, 9.0f, 5, 0.45f),
	ELEMENT("属性弾", 1, 0, 3.0f, 0.0f, 6, 0.6f),
	ARROW("矢", 1, 0, 3.4f, 0.0f, 0, 1.0f),
	ARROW_POWER("剛射", 2, 0, 3.6f, 0.0f, 0, 1.0f),
	GATLING("機関竜弾", 1, 0, 3.0f, 2.5f, 0, 0.4f);

	public final String jaName;
	/** 貫通回数（1 = 単発）。 */
	public final int pierce;
	/** 散弾の弾数（0 = 1発）。 */
	public final int pellets;
	public final float speed;
	public final float spread;
	/** 1 マガジンの装填数（0 は装填不要）。 */
	public final int magazine;
	/** 弾種ごとのダメージ係数。 */
	public final float damageMul;

	ShotKind(String jaName, int pierce, int pellets, float speed, float spread, int magazine, float damageMul) {
		this.jaName = jaName;
		this.pierce = pierce;
		this.pellets = pellets;
		this.speed = speed;
		this.spread = spread;
		this.magazine = magazine;
		this.damageMul = damageMul;
	}

	/** ボウガンで切り替えられる弾種。 */
	public static final ShotKind[] BOWGUN_AMMO = {NORMAL, PIERCE, SPREAD, ELEMENT};

	public static ShotKind byOrdinal(int ordinal) {
		ShotKind[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NORMAL;
	}
}
