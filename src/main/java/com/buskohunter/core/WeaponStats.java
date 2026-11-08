package com.buskohunter.core;

/**
 * 武器種 × 属性シリーズ から決まる武器性能。装備は「集める」のではなく、このテーブルから選ぶ。
 */
public record WeaponStats(WeaponType type, ElementType element, int attack, int elementValue, int affinity,
						  Sharpness sharpness, String name, String specialNote) {

	public static WeaponStats of(WeaponType type, ElementType element) {
		int atk = type.baseAttack;
		int elem;
		int aff = 0;
		Sharpness sharp = Sharpness.STANDARD;
		String note;
		switch (element) {
			case FIRE -> { elem = 32; note = "炎で焼き尽くす。サケラン・ブスコバリーノ王に有効"; }
			case WATER -> { elem = 32; atk += 5; sharp = Sharpness.BLUE_LONG; note = "長い青ゲージ。ジャングリアに特効"; }
			case THUNDER -> { elem = 34; atk -= 5; note = "雷光。ブスコバに有効"; }
			case ICE -> { elem = 32; sharp = Sharpness.LONG_WHITE; note = "長い白ゲージ。ジャングリアに有効"; }
			case DRAGON -> { elem = 38; aff = -15; atk += 10; note = "龍の力。会心率マイナスだが高攻撃"; }
			case POISON -> { elem = 26; aff = 5; note = "毒で継続ダメージ"; }
			case PARALYSIS -> { elem = 22; note = "麻痺で動きを止める"; }
			case SLEEP -> { elem = 22; aff = 10; note = "眠らせて大ダメージの起爆を狙え"; }
			case BLAST -> { elem = 26; atk += 5; note = "蓄積で爆発する固定ダメージ"; }
			case BUSKOBA -> { elem = 20; atk += 15; aff = 15; sharp = Sharpness.SHORT_PURPLE; note = "全モンスターを「ブスコバ化」させる謎の力"; }
			default -> { elem = 0; note = ""; }
		}
		if (type.ranged) {
			sharp = Sharpness.NONE;
		}
		elem = Math.round(elem * type.elementScale());
		String name = element == ElementType.BUSKOBA
			? "ブスコバの" + type.jaName
			: element.seriesName() + "ノ" + type.jaName;
		return new WeaponStats(type, element, atk, elem, aff, sharp, name, note);
	}

	/** 画面表示用の「表示攻撃力」（モンハンの武器倍率っぽく見せる）。 */
	public int displayAttack() {
		float bloat = switch (type) {
			case GREAT_SWORD -> 4.8f;
			case LONG_SWORD -> 3.3f;
			case SWORD_AND_SHIELD, DUAL_BLADES -> 1.4f;
			case HAMMER -> 5.2f;
			case HUNTING_HORN -> 4.2f;
			case LANCE, GUNLANCE -> 2.3f;
			case SWITCH_AXE -> 3.5f;
			case CHARGE_BLADE -> 3.6f;
			case INSECT_GLAIVE -> 3.1f;
			case LIGHT_BOWGUN -> 1.3f;
			case HEAVY_BOWGUN -> 1.5f;
			case BOW -> 1.2f;
		};
		return Math.round(attack * bloat);
	}

	public int displayElement() {
		return elementValue * 10;
	}
}
