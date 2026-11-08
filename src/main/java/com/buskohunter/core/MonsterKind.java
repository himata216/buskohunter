package com.buskohunter.core;

import java.util.List;

/**
 * 狩猟対象の大型モンスター 5 体の基本データ。
 * 肉質（hitzone）は 部位ごとに 斬/打/弾、属性は全身共通の弱点値（0〜30）× 部位の属性倍率。
 */
public enum MonsterKind {
	SAKERAN("sakeran", "声劇へ導く、酒池肉林", "サケラン", FieldType.FOREST,
		6000, 2.0f, 4.0f, 0.30f,
		new int[]{25, 10, 20, 15, 5}, ElementType.NONE, 0xF4F4F4,
		List.of(
			new Part("head", "頭（鼻）", 0.9f, 3.7f, 0f, 0.95f, 70, 75, 60, 1.0f, true, 500, true),
			new Part("chest", "胸筋", 0.25f, 2.75f, 0f, 1.0f, 35, 40, 30, 0.7f, false, 0, false),
			new Part("arm_r", "右拳", 0.4f, 2.4f, -1.25f, 0.75f, 45, 50, 40, 0.8f, true, 450, false),
			new Part("arm_l", "左拳", 0.4f, 2.4f, 1.25f, 0.75f, 45, 50, 40, 0.8f, false, 0, false),
			new Part("legs", "脚", 0.0f, 0.9f, 0f, 1.05f, 40, 45, 35, 0.8f, false, 0, false),
			new Part("tail", "尻尾", -0.9f, 1.6f, 0f, 0.6f, 55, 45, 50, 0.9f, true, 350, false)
		),
		"酒をこよなく愛する筋骨隆々の白き獣人。ピースサインの直後には要注意。豪腕のラリアットと酒瓶投げを使い分け、怒ると声劇モードに入り声量で周囲を吹き飛ばす。"),
	BARINO("barino", "凡百の突起物を消し", "ブスコバリーノ王", FieldType.SKY,
		7000, 3.6f, 4.2f, 0.26f,
		new int[]{5, 10, 15, 10, 25}, ElementType.DRAGON, 0xFFF2D8,
		List.of(
			new Part("sombrero", "宝玉ソンブレロ", 0.9f, 4.4f, 0f, 1.0f, 55, 60, 65, 1.0f, true, 600, false),
			new Part("head", "頭", 1.0f, 3.6f, 0f, 0.6f, 65, 70, 60, 1.0f, false, 0, true),
			new Part("butt", "聖なる臀部", -1.0f, 1.8f, 0f, 1.8f, 60, 55, 55, 1.1f, false, 0, true),
			new Part("arms", "長き腕", 0.8f, 1.6f, 1.9f, 0.7f, 40, 35, 40, 0.8f, true, 450, false),
			new Part("halo", "光輪", 0.6f, 5.4f, 0f, 1.4f, 30, 30, 70, 1.2f, true, 400, false)
		),
		"天界に君臨する真珠色の王。光輪から放たれる裁きの光と、宝玉のソンブレロ投げで凡百の突起物を消し去る。宙に浮くため、尻は打撃より弾が通りやすい。"),
	JANGRIA("jangria", "様々造形を破壊せし", "ジャングリア", FieldType.VOLCANO,
		8000, 3.8f, 4.4f, 0.27f,
		new int[]{0, 30, 5, 25, 10}, ElementType.FIRE, 0xB02828,
		List.of(
			new Part("horns", "角付きソンブレロ", 0.9f, 4.4f, 0f, 1.0f, 55, 65, 50, 1.0f, true, 700, true),
			new Part("butt_eyes", "尻の眼", -1.1f, 1.9f, 0f, 1.8f, 70, 65, 60, 1.2f, true, 800, true),
			new Part("wings", "翼", 0.0f, 3.2f, 2.0f, 1.0f, 50, 40, 55, 0.9f, true, 500, false),
			new Part("claws", "爪", 1.3f, 0.8f, 1.6f, 0.8f, 30, 35, 30, 0.7f, true, 500, false),
			new Part("body", "胴", 0.2f, 2.4f, 0f, 1.4f, 40, 40, 35, 0.8f, false, 0, false)
		),
		"灼熱の地獄から現れた悪魔のブスコ。あらゆる造形物を破壊し、尻に開いた無数の眼から獄炎を放つ。水と氷に極端に弱い。"),
	BUSAIBER("busaiber", "POSE YOUR GAME", "ブサイバー", FieldType.CYBER,
		8500, 3.8f, 4.4f, 0.30f,
		new int[]{10, 20, 0, 20, 15}, ElementType.THUNDER, 0x30E0F0,
		List.of(
			new Part("helmet", "鬼角ヘルメット", 0.9f, 4.4f, 0f, 1.0f, 50, 65, 45, 1.0f, true, 700, true),
			new Part("core", "臀部コア", -1.1f, 1.9f, 0f, 1.8f, 65, 50, 70, 1.2f, true, 900, true),
			new Part("wings", "ネオンウィング", 0.0f, 3.2f, 2.0f, 1.0f, 45, 35, 60, 0.9f, true, 500, false),
			new Part("arms", "機械腕", 1.2f, 1.0f, 1.6f, 0.8f, 30, 40, 30, 0.7f, false, 0, false),
			new Part("body", "胴", 0.2f, 2.4f, 0f, 1.4f, 40, 45, 40, 0.8f, false, 0, false)
		),
		"電脳空間を支配するサイバー鬼。「POSE YOUR GAME!」の号令の間に動いた者へ裁きのレーザーを放つ。ホログラム分身やミサイルを駆使する。雷は吸収されるので効かない。"),
	BUSKOBA("buskoba", "潤いはオアシスの笑み", "ブスコバ", FieldType.DESERT,
		7500, 3.6f, 4.0f, 0.28f,
		new int[]{15, 0, 30, 15, 10}, ElementType.WATER, 0xF0D8C8,
		List.of(
			new Part("sombrero", "ソンブレロ", 0.9f, 4.0f, 0f, 1.0f, 55, 60, 60, 1.0f, true, 600, false),
			new Part("head", "頭", 1.0f, 3.3f, 0f, 0.55f, 70, 75, 60, 1.0f, false, 0, true),
			new Part("butt", "潤いの臀部", -1.0f, 1.7f, 0f, 1.8f, 65, 60, 55, 1.1f, true, 700, true),
			new Part("arms", "細長い腕", 0.8f, 1.5f, 1.8f, 0.7f, 40, 35, 40, 0.8f, true, 450, false)
		),
		"砂原のオアシスに棲む、全てのブスコの原点。潤いに満ちた尻から水流を放ち、跳ね回って砂原を揺らす。笑顔のまま繰り出す尻プレスは見た目以上に重い。");

	/**
	 * 部位定義。位置はモンスターの足元中心から 前方/上方/横 のブロック単位オフセット（横は左右対称に扱う場合あり）。
	 */
	public record Part(String id, String jaName, float forward, float up, float side, float radius,
					   int cut, int blunt, int shot, float elementMul, boolean breakable, int breakHp, boolean weakSpot) {
		public int hitzone(WeaponType.DamageKind kind) {
			return switch (kind) {
				case CUT -> cut;
				case BLUNT -> blunt;
				case SHOT -> shot;
			};
		}
	}

	public final String id;
	public final String title;
	public final String jaName;
	public final FieldType homeField;
	public final int baseHp;
	public final float width;
	public final float height;
	public final float moveSpeed;
	/** 火・水・雷・氷・龍 の弱点値（0〜30）。 */
	public final int[] weakness;
	/** この種族が攻撃で使う主属性（やられ状態付与）。 */
	public final ElementType attackElement;
	public final int color;
	public final List<Part> parts;
	public final String lore;

	MonsterKind(String id, String title, String jaName, FieldType homeField, int baseHp, float width, float height,
				float moveSpeed, int[] weakness, ElementType attackElement, int color, List<Part> parts, String lore) {
		this.id = id;
		this.title = title;
		this.jaName = jaName;
		this.homeField = homeField;
		this.baseHp = baseHp;
		this.width = width;
		this.height = height;
		this.moveSpeed = moveSpeed;
		this.weakness = weakness;
		this.attackElement = attackElement;
		this.color = color;
		this.parts = parts;
		this.lore = lore;
	}

	public String fullName() {
		return title + " " + jaName;
	}

	public int weaknessTo(ElementType e) {
		return switch (e) {
			case FIRE -> weakness[0];
			case WATER -> weakness[1];
			case THUNDER -> weakness[2];
			case ICE -> weakness[3];
			case DRAGON -> weakness[4];
			default -> 0;
		};
	}

	public int partIndex(String id) {
		for (int i = 0; i < parts.size(); i++) {
			if (parts.get(i).id().equals(id)) return i;
		}
		return -1;
	}

	/** 弱点を星で表示（★★★ = 有効）。 */
	public String weaknessStars(ElementType e) {
		int w = weaknessTo(e);
		if (w >= 25) return "★★★";
		if (w >= 15) return "★★";
		if (w >= 10) return "★";
		if (w > 0) return "△";
		return "×";
	}

	public static MonsterKind byOrdinal(int ordinal) {
		MonsterKind[] v = values();
		return ordinal >= 0 && ordinal < v.length ? v[ordinal] : SAKERAN;
	}
}
