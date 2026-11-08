package com.buskohunter.core;

/** 防具スキル。 */
public enum ArmorSkill {
	ATTACK_UP("攻撃力UP", "攻撃力 +10%"),
	DEFENSE_UP("防御力UP", "被ダメージ -12%"),
	HEALTH_BOOST("体力増強", "最大体力 +30"),
	RECOVERY_UP("回復量UP", "回復アイテムの効果 +30%"),
	DRUNK_IMMUNE("酔い無効", "サケランの酒で酔わない"),
	KO_UP("KO術", "スタン値 +30%"),
	EVADE_WINDOW("回避性能", "回避の無敵時間が延長"),
	EVADE_DISTANCE("回避距離UP", "回避で遠くまで転がる"),
	DIVINE_BLESSING("神の加護", "25%の確率で被ダメージ半減"),
	CHALLENGER("挑戦者", "怒り状態のモンスターに攻撃力 +20%"),
	FIRE_ATTACK("火属性攻撃強化", "火属性値 +30%"),
	PARTBREAKER("破壊王", "部位破壊しやすくなる"),
	CRITICAL_EYE("見切り", "会心率 +20%"),
	WEAKNESS_EXPLOIT("弱点特効", "弱点部位への会心率 +40%"),
	STAMINA_SURGE("スタミナ急速回復", "スタミナ回復速度 +50%"),
	ELEMENT_UP("属性攻撃強化", "全属性値 +20%"),
	EARPLUGS("耳栓", "咆哮で怯まない"),
	BUSKOBA_SOUL("ブスコバ魂", "ブスコバ属性値 +50%、ブスコバ化が長くなる");

	public final String jaName;
	public final String description;

	ArmorSkill(String jaName, String description) {
		this.jaName = jaName;
		this.description = description;
	}
}
