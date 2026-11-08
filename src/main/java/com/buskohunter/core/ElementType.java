package com.buskohunter.core;

/**
 * 武器・攻撃の属性 / 状態異常。
 * 火〜龍は「属性ダメージ」、毒〜爆破は「状態異常の蓄積」、ブスコバは独自の特殊属性。
 */
public enum ElementType {
	NONE("none", "無属性", 0xC8C8C8, Kind.NONE),
	FIRE("fire", "火", 0xFF6A2A, Kind.ELEMENT),
	WATER("water", "水", 0x3AA8FF, Kind.ELEMENT),
	THUNDER("thunder", "雷", 0xFFE03A, Kind.ELEMENT),
	ICE("ice", "氷", 0xA8F0FF, Kind.ELEMENT),
	DRAGON("dragon", "龍", 0xB03040, Kind.ELEMENT),
	POISON("poison", "毒", 0xB060E0, Kind.STATUS),
	PARALYSIS("paralysis", "麻痺", 0xF0D020, Kind.STATUS),
	SLEEP("sleep", "睡眠", 0x8090FF, Kind.STATUS),
	BLAST("blast", "爆破", 0xFF9A30, Kind.STATUS),
	BUSKOBA("buskoba", "ブスコバ", 0xFF70C0, Kind.SPECIAL);

	public enum Kind { NONE, ELEMENT, STATUS, SPECIAL }

	/** 武器として選べる 10 シリーズ（無属性を除く）。 */
	public static final ElementType[] SERIES = {
		FIRE, WATER, THUNDER, ICE, DRAGON, POISON, PARALYSIS, SLEEP, BLAST, BUSKOBA
	};

	public final String id;
	public final String jaName;
	public final int color;
	public final Kind kind;

	ElementType(String id, String jaName, int color, Kind kind) {
		this.id = id;
		this.jaName = jaName;
		this.color = color;
		this.kind = kind;
	}

	public boolean isElemental() {
		return kind == Kind.ELEMENT;
	}

	public boolean isStatus() {
		return kind == Kind.STATUS;
	}

	/** シリーズ名（武器名の頭に付く）。 */
	public String seriesName() {
		return switch (this) {
			case FIRE -> "焔咆";
			case WATER -> "潤水";
			case THUNDER -> "轟雷";
			case ICE -> "凍牙";
			case DRAGON -> "滅龍";
			case POISON -> "毒棘";
			case PARALYSIS -> "痺電";
			case SLEEP -> "夢幻";
			case BLAST -> "爆鎚";
			case BUSKOBA -> "ブスコバ";
			case NONE -> "鉄";
		};
	}

	public static ElementType byId(String id) {
		for (ElementType e : values()) {
			if (e.id.equals(id)) return e;
		}
		return NONE;
	}

	public static ElementType byOrdinal(int ordinal) {
		ElementType[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NONE;
	}
}
