package com.buskohunter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** クエスト定義。 */
public record QuestDef(int index, String id, String title, Rank rank, int stars, Goal goal, FieldType field,
					   List<MonsterKind> targets, int timeMinutes, int rewardPoints, int rewardCoins,
					   String client, String description) {

	public enum Rank {
		LOW("下位", 1.0f, 1.0f, 0, 80),
		HIGH("上位", 2.1f, 1.8f, 3, 220),
		G("G級", 3.4f, 2.6f, 7, 380);

		public final String jaName;
		public final float hpMul;
		public final float attackMul;
		/** 受注に必要な HR。 */
		public final int requiredHr;
		/** このランク帯でのハンターの防御力（防具の数値は自動でランク相当になる）。 */
		public final int hunterDefense;

		Rank(String jaName, float hpMul, float attackMul, int requiredHr, int hunterDefense) {
			this.jaName = jaName;
			this.hpMul = hpMul;
			this.attackMul = attackMul;
			this.requiredHr = requiredHr;
			this.hunterDefense = hunterDefense;
		}
	}

	public enum Goal {
		HUNT("狩猟（討伐または捕獲）"),
		SLAY("討伐"),
		CAPTURE("捕獲");

		public final String jaName;

		Goal(String jaName) {
			this.jaName = jaName;
		}
	}

	public String goalText() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < targets.size(); i++) {
			if (i > 0) sb.append("、");
			sb.append(targets.get(i).jaName);
		}
		if (targets.size() > 1) {
			return sb + " " + targets.size() + "頭の" + goal.jaName;
		}
		return sb + " 1頭の" + goal.jaName;
	}

	public boolean isMulti() {
		return targets.size() > 1;
	}

	// ------------------------------------------------------------------
	private static final List<QuestDef> ALL;

	static {
		List<QuestDef> q = new ArrayList<>();
		int i = 0;
		// ---- 下位 ----
		q.add(new QuestDef(i++, "low_sakeran", "酒池肉林の宴", Rank.LOW, 1, Goal.HUNT, FieldType.FOREST,
			List.of(MonsterKind.SAKERAN), 50, 300, 1200, "酒場のマスター",
			"森の酒蔵が筋骨隆々の白い獣に占拠された！ ピースサインの後に何かが来る…気をつけてくれ！"));
		q.add(new QuestDef(i++, "low_buskoba", "オアシスの微笑み", Rank.LOW, 2, Goal.HUNT, FieldType.DESERT,
			List.of(MonsterKind.BUSKOBA), 50, 380, 1500, "砂原の行商人",
			"オアシスの水場にソンブレロの巨体が居座っている。あの笑顔…なぜか目が離せない。"));
		q.add(new QuestDef(i++, "low_jangria", "地獄の釜の底", Rank.LOW, 3, Goal.HUNT, FieldType.VOLCANO,
			List.of(MonsterKind.JANGRIA), 50, 450, 1800, "鍛冶屋の親方",
			"荒野の岩山が次々と崩されている。尻に眼のある悪魔を見たという話だ。水と氷の武器を持っていけ。"));
		q.add(new QuestDef(i++, "low_busaiber", "電脳に踊れ", Rank.LOW, 3, Goal.HUNT, FieldType.CYBER,
			List.of(MonsterKind.BUSAIBER), 50, 450, 1800, "謎のハッカー",
			"廃都のネオンがすべてアイツに乗っ取られた。「POSE YOUR GAME」が聞こえたら……動くな。"));
		q.add(new QuestDef(i++, "low_barino", "【緊急】天上の王", Rank.LOW, 3, Goal.HUNT, FieldType.SKY,
			List.of(MonsterKind.BARINO), 50, 600, 2500, "ギルドマスター",
			"天上桜郷に光輪の王が降臨した。凡百の突起物を消し去るその力、ここで止めねばならぬ。"));
		// ---- 上位 ----
		q.add(new QuestDef(i++, "high_sakeran", "声劇の夜は終わらない", Rank.HIGH, 4, Goal.HUNT, FieldType.FOREST,
			List.of(MonsterKind.SAKERAN), 50, 900, 3500, "声劇団の座長",
			"上位個体のサケランが連日連夜の大宴会を開いている。声量だけで木々がなぎ倒されるらしい。"));
		q.add(new QuestDef(i++, "high_buskoba", "潤いすぎた砂原", Rank.HIGH, 5, Goal.HUNT, FieldType.DESERT,
			List.of(MonsterKind.BUSKOBA), 50, 950, 3800, "オアシスの長老",
			"砂原が湿地になりかけている。強大化したブスコバの潤いを止めてくれ。"));
		q.add(new QuestDef(i++, "high_jangria", "造形破壊神", Rank.HIGH, 5, Goal.SLAY, FieldType.VOLCANO,
			List.of(MonsterKind.JANGRIA), 50, 1000, 4000, "建築ギルド",
			"我々の建てたものが全て壊される！ 上位ジャングリアを討伐せよ！"));
		q.add(new QuestDef(i++, "high_busaiber", "POSE YOUR GAME", Rank.HIGH, 6, Goal.HUNT, FieldType.CYBER,
			List.of(MonsterKind.BUSAIBER), 50, 1050, 4200, "謎のハッカー",
			"アップデートされたブサイバーはポーズ判定がシビアらしい。ポーズを決めろ！"));
		q.add(new QuestDef(i++, "high_barino", "王は二度降臨する", Rank.HIGH, 6, Goal.HUNT, FieldType.SKY,
			List.of(MonsterKind.BARINO), 50, 1100, 4500, "ギルドマスター",
			"ブスコバリーノ王が再び降臨した。前回よりも光輪の輝きが強い…"));
		q.add(new QuestDef(i++, "high_double", "【大連続狩猟】天国と地獄", Rank.HIGH, 6, Goal.HUNT, FieldType.SKY,
			List.of(MonsterKind.BARINO, MonsterKind.JANGRIA), 50, 1600, 6500, "ギルドマスター",
			"天上の王を追って、地獄の悪魔までもが桜郷に現れた。2頭とも狩猟せよ！"));
		q.add(new QuestDef(i++, "high_capture_buskoba", "ブスコバ生け捕り作戦", Rank.HIGH, 5, Goal.CAPTURE, FieldType.DESERT,
			List.of(MonsterKind.BUSKOBA), 50, 1200, 5000, "王立学術院",
			"研究のためブスコバを生け捕りにしてほしい。弱らせて罠にかけ、麻酔玉を2発！"));
		// ---- G級 ----
		q.add(new QuestDef(i++, "g_sakeran", "極・酒池肉林", Rank.G, 7, Goal.HUNT, FieldType.FOREST,
			List.of(MonsterKind.SAKERAN), 50, 1800, 8000, "伝説の酒豪",
			"極限まで鍛え上げられた肉体。もはや酒瓶すら凶器だ。"));
		q.add(new QuestDef(i++, "g_buskoba", "極・オアシスの笑み", Rank.G, 7, Goal.HUNT, FieldType.DESERT,
			List.of(MonsterKind.BUSKOBA), 50, 1800, 8000, "オアシスの長老",
			"全てのブスコの原点、その真の力を見よ。"));
		q.add(new QuestDef(i++, "g_double", "【大連続狩猟】電脳と砂原", Rank.G, 8, Goal.HUNT, FieldType.CYBER,
			List.of(MonsterKind.BUSAIBER, MonsterKind.BUSKOBA), 50, 2600, 12000, "謎のハッカー",
			"ブサイバーがブスコバのデータを取り込み、廃都に呼び出した。2頭まとめて片付けろ。"));
		q.add(new QuestDef(i++, "g_barino", "極・凡百の突起物", Rank.G, 8, Goal.HUNT, FieldType.SKY,
			List.of(MonsterKind.BARINO), 50, 2400, 11000, "ギルドマスター",
			"真の王が目覚めた。全ての突起物が消える前に。"));
		q.add(new QuestDef(i++, "g_final", "【最終決戦】ブスコ五獣集結", Rank.G, 8, Goal.HUNT, FieldType.CYBER,
			List.of(MonsterKind.SAKERAN, MonsterKind.BUSKOBA, MonsterKind.JANGRIA, MonsterKind.BUSAIBER, MonsterKind.BARINO),
			50, 6000, 30000, "？？？",
			"全てのブスコが電脳廃都に集結した。5頭連続の狩猟だ。真のハンターよ、その名を刻め！"));
		ALL = Collections.unmodifiableList(q);
	}

	public static List<QuestDef> all() {
		return ALL;
	}

	public static QuestDef byIndex(int index) {
		return index >= 0 && index < ALL.size() ? ALL.get(index) : ALL.get(0);
	}
}
