package com.buskohunter.core;

/** 狩猟フィールド。それぞれ専用ディメンション（data/buskohunter/dimension/*.json）。 */
public enum FieldType {
	FOREST("forest_field", "酒池の森", "木々が生い茂る広大な森。サケランの縄張り。", false),
	DESERT("desert_field", "オアシス砂原", "照りつける太陽と、緑が点在する砂原。", false),
	VOLCANO("volcano_field", "獄炎の荒野", "赤い岩肌と溶岩が噴き出す灼熱の荒野。", false),
	SKY("sky_field", "天上桜郷", "桜が舞う天上の郷。王が降臨するという。", false),
	CYBER("field_cyber", "電脳廃都", "ネオンが明滅する、終わりの空の下の廃都市。", true);

	public final String dimensionPath;
	public final String jaName;
	public final String description;
	/** 平坦な地形にプログラムで街を建てるフィールド。 */
	public final boolean builtArena;

	FieldType(String dimensionPath, String jaName, String description, boolean builtArena) {
		this.dimensionPath = dimensionPath;
		this.jaName = jaName;
		this.description = description;
		this.builtArena = builtArena;
	}

	public static FieldType byOrdinal(int ordinal) {
		FieldType[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : FOREST;
	}
}
