package com.buskohunter.core;

import java.util.EnumSet;
import java.util.Set;

/** 防具シリーズ。スキルと属性耐性のセットを選ぶ形式。 */
public enum ArmorSeries {
	HUNTER("hunter", "ハンター", 0xA07040,
		new int[]{5, 5, 5, 5, 5},
		EnumSet.of(ArmorSkill.DEFENSE_UP, ArmorSkill.HEALTH_BOOST, ArmorSkill.RECOVERY_UP)),
	SAKERAN("sakeran", "サケラン", 0xF0F0F0,
		new int[]{-5, 5, 0, 10, 5},
		EnumSet.of(ArmorSkill.ATTACK_UP, ArmorSkill.HEALTH_BOOST, ArmorSkill.DRUNK_IMMUNE, ArmorSkill.KO_UP)),
	BARINO("barino", "バリーノ", 0xFFE7A0,
		new int[]{5, 5, 5, 5, 15},
		EnumSet.of(ArmorSkill.EVADE_WINDOW, ArmorSkill.EVADE_DISTANCE, ArmorSkill.RECOVERY_UP, ArmorSkill.DIVINE_BLESSING)),
	JANGRIA("jangria", "ジャングリア", 0xB02020,
		new int[]{20, -10, 0, 5, -5},
		EnumSet.of(ArmorSkill.ATTACK_UP, ArmorSkill.CHALLENGER, ArmorSkill.FIRE_ATTACK, ArmorSkill.PARTBREAKER)),
	BUSAIBER("busaiber", "ブサイバー", 0x40F0F0,
		new int[]{0, 0, 20, -5, 0},
		EnumSet.of(ArmorSkill.CRITICAL_EYE, ArmorSkill.WEAKNESS_EXPLOIT, ArmorSkill.STAMINA_SURGE, ArmorSkill.ELEMENT_UP)),
	BUSKOBA("buskoba", "ブスコバ", 0xF0C080,
		new int[]{-5, 20, 0, 0, 5},
		EnumSet.of(ArmorSkill.EARPLUGS, ArmorSkill.RECOVERY_UP, ArmorSkill.STAMINA_SURGE, ArmorSkill.BUSKOBA_SOUL));

	public final String id;
	public final String jaName;
	public final int color;
	/** 火・水・雷・氷・龍 の耐性。 */
	public final int[] resistances;
	public final Set<ArmorSkill> skills;

	ArmorSeries(String id, String jaName, int color, int[] resistances, Set<ArmorSkill> skills) {
		this.id = id;
		this.jaName = jaName;
		this.color = color;
		this.resistances = resistances;
		this.skills = skills;
	}

	public int resistance(ElementType element) {
		return switch (element) {
			case FIRE -> resistances[0];
			case WATER -> resistances[1];
			case THUNDER -> resistances[2];
			case ICE -> resistances[3];
			case DRAGON -> resistances[4];
			default -> 0;
		};
	}

	public boolean has(ArmorSkill skill) {
		return skills.contains(skill);
	}

	public static ArmorSeries byId(String id) {
		for (ArmorSeries s : values()) {
			if (s.id.equals(id)) return s;
		}
		return HUNTER;
	}

	public static ArmorSeries byOrdinal(int ordinal) {
		ArmorSeries[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : HUNTER;
	}
}
