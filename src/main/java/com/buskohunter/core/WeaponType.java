package com.buskohunter.core;

/**
 * 14 種の武器種。数値はモンハン風の「表示攻撃力」ではなく、内部の基礎攻撃力（真攻撃力に近い値）。
 */
public enum WeaponType {
	GREAT_SWORD("great_sword", "大剣", 235, DamageKind.CUT, 0.80f, false, true),
	LONG_SWORD("long_sword", "太刀", 205, DamageKind.CUT, 0.95f, false, false),
	SWORD_AND_SHIELD("sword_and_shield", "片手剣", 180, DamageKind.CUT, 1.00f, false, true),
	DUAL_BLADES("dual_blades", "双剣", 175, DamageKind.CUT, 1.00f, false, false),
	HAMMER("hammer", "ハンマー", 235, DamageKind.BLUNT, 0.88f, false, false),
	HUNTING_HORN("hunting_horn", "狩猟笛", 215, DamageKind.BLUNT, 0.88f, false, false),
	LANCE("lance", "ランス", 205, DamageKind.CUT, 0.70f, false, true),
	GUNLANCE("gunlance", "ガンランス", 205, DamageKind.CUT, 0.70f, false, true),
	SWITCH_AXE("switch_axe", "スラッシュアックス", 215, DamageKind.CUT, 0.85f, false, false),
	CHARGE_BLADE("charge_blade", "チャージアックス", 205, DamageKind.CUT, 0.85f, false, true),
	INSECT_GLAIVE("insect_glaive", "操虫棍", 195, DamageKind.CUT, 0.95f, false, false),
	LIGHT_BOWGUN("light_bowgun", "ライトボウガン", 185, DamageKind.SHOT, 0.95f, true, false),
	HEAVY_BOWGUN("heavy_bowgun", "ヘビィボウガン", 215, DamageKind.SHOT, 0.70f, true, false),
	BOW("bow", "弓", 190, DamageKind.SHOT, 0.95f, true, false);

	public enum DamageKind { CUT, BLUNT, SHOT }

	public final String id;
	public final String jaName;
	public final int baseAttack;
	public final DamageKind damageKind;
	/** 抜刀中の移動速度倍率。 */
	public final float drawnSpeed;
	public final boolean ranged;
	public final boolean canGuard;

	WeaponType(String id, String jaName, int baseAttack, DamageKind damageKind, float drawnSpeed, boolean ranged, boolean canGuard) {
		this.id = id;
		this.jaName = jaName;
		this.baseAttack = baseAttack;
		this.damageKind = damageKind;
		this.drawnSpeed = drawnSpeed;
		this.ranged = ranged;
		this.canGuard = canGuard;
	}

	/** 属性値の倍率（手数の多い武器は 1 発あたりの属性が低め）。 */
	public float elementScale() {
		return switch (this) {
			case DUAL_BLADES -> 0.70f;
			case GREAT_SWORD, HAMMER -> 1.25f;
			case LIGHT_BOWGUN -> 0.80f;
			case HEAVY_BOWGUN -> 0.85f;
			case BOW -> 0.90f;
			default -> 1.0f;
		};
	}

	/** 操作説明（装備画面・ハンターメニューで表示）。 */
	public String[] controls() {
		return switch (this) {
			case GREAT_SWORD -> new String[]{
				"左: 縦斬り（長押しで溜め／溜め斬り→強溜め→真溜め）",
				"右: なぎ払い", "特殊(R): タックル（スーパーアーマー・溜め段階継続）", "右長押し: ガード（刀身で受ける）"};
			case LONG_SWORD -> new String[]{
				"左: 縦斬り→突き→斬り上げ", "右: 踏み込み斬り",
				"特殊(R): 気刃斬りI→II→III→大回転（練気レベル上昇）",
				"右を押したまま R: 見切り斬り（カウンター）", "右を押したまま左: 気刃兜割（練気Lv消費）"};
			case SWORD_AND_SHIELD -> new String[]{
				"左: 斬り下ろし→横斬り→剣盾コンボ", "右: 回転斬り",
				"特殊(R): 盾攻撃→昇竜撃", "右長押し: ガード"};
			case DUAL_BLADES -> new String[]{
				"左: 二連斬り→返し斬り→車輪斬り", "右: 回転斬り",
				"特殊(R): 鬼人化 ON/OFF（スタミナ消費）", "鬼人化中の右: 鬼人乱舞"};
			case HAMMER -> new String[]{
				"左: 縦振り1→2→アッパー", "右長押し: 溜め（Lv1〜3）→離して溜め攻撃",
				"特殊(R): 回転攻撃→ホームラン"};
			case HUNTING_HORN -> new String[]{
				"左: ぶん回し（音符1）", "右: 叩きつけ（音符2）", "特殊(R): 後方攻撃（音符3）",
				"右を押したまま左: 演奏（直前の音符で旋律効果）"};
			case LANCE -> new String[]{
				"左: 中段突き×3", "右: 上段突き×3", "特殊(R): 突進（左でフィニッシュ）",
				"右長押し: ガード", "ガード中に左: カウンター突き"};
			case GUNLANCE -> new String[]{
				"左: 突き→突き→斬り上げ→叩きつけ", "右: 砲撃",
				"叩きつけ後の右: フルバースト", "特殊(R): 竜撃砲", "右長押し: ガード / ガード中に R: リロード"};
			case SWITCH_AXE -> new String[]{
				"斧: 左 縦斬り / 右 なぎ払い", "剣: 左 縦斬り→二連 / 右 斬り上げ",
				"特殊(R): 変形斬り", "剣モードで右を押したまま R: 属性解放突き", "斧モードで右を押したまま R: ビン装填"};
			case CHARGE_BLADE -> new String[]{
				"剣: 左 斬り / 右 突進斬り / 右長押し ガード", "剣: 特殊(R) ビンにチャージ",
				"ガード中に左: 変形斬り（斧へ）", "斧: 左 叩きつけ / 右 属性解放斬りI→II→高出力",
				"斧: 特殊(R) 変形（剣へ）"};
			case INSECT_GLAIVE -> new String[]{
				"左: 斬り上げ→袈裟斬り→回転斬り", "右: 跳躍（空中で左: 空中斬り / 右: 急襲突き）",
				"特殊(R): 猟虫を飛ばす（エキス採取：頭=赤 胴=橙 脚=白）"};
			case LIGHT_BOWGUN -> new String[]{
				"左: 通常弾", "右: 速射", "特殊(R): リロード", "右を押したまま R: 弾種切替"};
			case HEAVY_BOWGUN -> new String[]{
				"左: 射撃", "右長押し: 機関竜弾", "特殊(R): リロード", "右を押したまま R: 弾種切替"};
			case BOW -> new String[]{
				"左長押し: 溜め→離して射撃（Lv1〜3）", "右: 剛射", "特殊(R): 竜の一矢（溜め→貫通）"};
		};
	}

	public static WeaponType byId(String id) {
		for (WeaponType t : values()) {
			if (t.id.equals(id)) return t;
		}
		return GREAT_SWORD;
	}

	public static WeaponType byOrdinal(int ordinal) {
		WeaponType[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : GREAT_SWORD;
	}
}
