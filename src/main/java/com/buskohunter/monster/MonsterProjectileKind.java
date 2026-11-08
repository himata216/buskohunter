package com.buskohunter.monster;

import com.buskohunter.core.ElementType;

/** モンスターの飛び道具。 */
public enum MonsterProjectileKind {
	SAKE_BOTTLE("proj_sake_bottle", ElementType.NONE, 0.05f, false, 2.6f, 1.6f),
	FIREBALL("proj_fireball", ElementType.FIRE, 0.0f, false, 2.4f, 1.8f),
	WATER_ORB("proj_water_orb", ElementType.WATER, 0.0f, false, 2.0f, 1.8f),
	MISSILE("proj_missile", ElementType.THUNDER, 0.0f, true, 2.2f, 1.4f),
	HOLY_ORB("proj_holy_orb", ElementType.DRAGON, 0.0f, true, 2.4f, 2.0f),
	GLITCH_CUBE("proj_glitch_cube", ElementType.THUNDER, 0.0f, false, 1.6f, 1.6f),
	SOMBRERO("proj_sombrero", ElementType.NONE, 0.0f, false, 2.0f, 2.6f);

	public final String itemId;
	public final ElementType element;
	public final float gravity;
	public final boolean homing;
	public final float blastRadius;
	public final float renderScale;

	MonsterProjectileKind(String itemId, ElementType element, float gravity, boolean homing, float blastRadius, float renderScale) {
		this.itemId = itemId;
		this.element = element;
		this.gravity = gravity;
		this.homing = homing;
		this.blastRadius = blastRadius;
		this.renderScale = renderScale;
	}

	public static MonsterProjectileKind byOrdinal(int ordinal) {
		MonsterProjectileKind[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : FIREBALL;
	}
}
