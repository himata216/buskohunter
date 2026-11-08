package com.buskohunter.core;

/** アイテムボックスから持ち出せる狩猟アイテム。 */
public enum HuntItem {
	POTION("potion", "回復薬", "体力を30回復", Use.DRINK, 10, 20),
	MEGA_POTION("mega_potion", "回復薬グレート", "体力を60回復", Use.DRINK, 10, 24),
	MAX_POTION("max_potion", "いにしえの秘薬", "体力全快＋最大スタミナ回復", Use.DRINK, 2, 30),
	ANTIDOTE("antidote", "解毒薬", "毒・やられ状態を治す", Use.DRINK, 5, 16),
	ENERGY_DRINK("energy_drink", "強走薬", "60秒間スタミナが減らない", Use.DRINK, 5, 20),
	DEMONDRUG("demondrug", "鬼人薬", "攻撃力+8%（クエスト中）", Use.DRINK, 5, 20),
	ARMORSKIN("armorskin", "硬化薬", "防御力+12%（クエスト中）", Use.DRINK, 5, 20),
	RATION("ration", "携帯食料", "最大スタミナを25回復", Use.EAT, 10, 20),
	WELL_DONE_STEAK("well_done_steak", "こんがり肉", "最大スタミナを全快", Use.EAT, 10, 30),
	WHETSTONE("whetstone", "砥石", "切れ味を最大まで回復", Use.SHARPEN, 20, 40),
	PAINTBALL("paintball", "ペイントボール", "モンスターにマーキング", Use.THROW, 10, 0),
	FLASH_BOMB("flash_bomb", "閃光玉", "周囲のモンスターの目をくらませる", Use.THROW, 5, 0),
	SHOCK_TRAP("shock_trap", "シビレ罠", "踏んだモンスターをしびれさせる", Use.PLACE, 1, 0),
	PITFALL_TRAP("pitfall_trap", "落とし穴", "モンスターを落として拘束", Use.PLACE, 1, 0),
	TRANQ_BOMB("tranq_bomb", "捕獲用麻酔玉", "罠にかかった弱ったモンスターを捕獲", Use.THROW, 8, 0),
	BARREL_BOMB("barrel_bomb", "大タル爆弾", "設置3秒後に大爆発", Use.PLACE, 3, 0),
	FARCASTER("farcaster", "モドリ玉", "ベースキャンプへ戻る", Use.THROW, 1, 0);

	public enum Use { DRINK, EAT, SHARPEN, THROW, PLACE }

	public final String id;
	public final String jaName;
	public final String description;
	public final Use use;
	/** 1クエストで持ち出せる最大数。 */
	public final int maxCarry;
	/** 使用にかかる時間（tick）。0 は即時。 */
	public final int useTicks;

	HuntItem(String id, String jaName, String description, Use use, int maxCarry, int useTicks) {
		this.id = id;
		this.jaName = jaName;
		this.description = description;
		this.use = use;
		this.maxCarry = maxCarry;
		this.useTicks = useTicks;
	}

	public static HuntItem byOrdinal(int ordinal) {
		HuntItem[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : POTION;
	}
}
