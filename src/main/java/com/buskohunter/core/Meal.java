package com.buskohunter.core;

/** 出発前の食事（1クエストに1回）。 */
public enum Meal {
	NONE("none", "なし", 0, 0, 0f, 0f, ElementType.NONE, 0, 0, MealSkill.NONE, 0x808080),
	BUSKO_NABE("busko_nabe", "ブスコ鍋", 50, 50, 0f, 0f, ElementType.NONE, 0, 0, MealSkill.CARVE_PLUS, 0xE0A060),
	SAKERAN_MORI("sakeran_mori", "サケラン盛り（特盛）", 30, 30, 0.10f, 0f, ElementType.NONE, 0, 5, MealSkill.HEAVY_EATER, 0xF0F0F0),
	TENKAI_PUDDING("tenkai_pudding", "天界プリン", 30, 50, 0f, 0.15f, ElementType.DRAGON, 15, 0, MealSkill.FAST_GETUP, 0xFFE8A0),
	YOUGAN_CURRY("yougan_curry", "溶岩激辛カレー", 20, 50, 0.08f, 0f, ElementType.FIRE, 20, 0, MealSkill.STAMINA_SAVE, 0xD04020),
	OASIS_REIMEN("oasis_reimen", "オアシス冷麺", 50, 30, 0f, 0.05f, ElementType.WATER, 20, 0, MealSkill.HEAL_BOOST, 0x60C0F0),
	NEON_TEISHOKU("neon_teishoku", "ネオン定食", 30, 30, 0.05f, 0f, ElementType.THUNDER, 20, 10, MealSkill.CRIT_DRAW, 0x40F0D0);

	public enum MealSkill {
		NONE("なし"),
		CARVE_PLUS("ネコの解体術（剥ぎ取り+1）"),
		HEAVY_EATER("ネコの大食い（携帯食料の効果UP）"),
		FAST_GETUP("ネコの受け身（吹っ飛びからすぐ起き上がる）"),
		STAMINA_SAVE("ネコの体術（スタミナ消費 -25%）"),
		HEAL_BOOST("ネコの医療術（回復薬の効果UP）"),
		CRIT_DRAW("ネコの会心術（会心率UP）");

		public final String jaName;

		MealSkill(String jaName) {
			this.jaName = jaName;
		}
	}

	public final String id;
	public final String jaName;
	public final int healthBonus;
	public final int staminaBonus;
	public final float attackBonus;
	public final float defenseBonus;
	public final ElementType resistElement;
	public final int resistValue;
	public final int affinityBonus;
	public final MealSkill skill;
	public final int color;

	Meal(String id, String jaName, int healthBonus, int staminaBonus, float attackBonus, float defenseBonus,
		 ElementType resistElement, int resistValue, int affinityBonus, MealSkill skill, int color) {
		this.id = id;
		this.jaName = jaName;
		this.healthBonus = healthBonus;
		this.staminaBonus = staminaBonus;
		this.attackBonus = attackBonus;
		this.defenseBonus = defenseBonus;
		this.resistElement = resistElement;
		this.resistValue = resistValue;
		this.affinityBonus = affinityBonus;
		this.skill = skill;
		this.color = color;
	}

	public String describe() {
		StringBuilder sb = new StringBuilder();
		if (healthBonus > 0) sb.append("体力+").append(healthBonus).append(" ");
		if (staminaBonus > 0) sb.append("スタミナ+").append(staminaBonus).append(" ");
		if (attackBonus > 0) sb.append("攻撃+").append(Math.round(attackBonus * 100)).append("% ");
		if (defenseBonus > 0) sb.append("防御+").append(Math.round(defenseBonus * 100)).append("% ");
		if (resistValue > 0) sb.append(resistElement.jaName).append("耐性+").append(resistValue).append(" ");
		if (affinityBonus > 0) sb.append("会心+").append(affinityBonus).append("% ");
		return sb.toString().trim();
	}

	public static Meal byOrdinal(int ordinal) {
		Meal[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NONE;
	}
}
