package com.buskohunter.monster;

import com.buskohunter.core.ElementType;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.monster.MonsterMove.Fx;
import com.buskohunter.monster.MonsterMove.Knock;
import com.buskohunter.monster.MonsterMove.Special;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.buskohunter.monster.MonsterAnim.*;

/**
 * 5 体のボスの技構成。ダメージは下位基準（クエストランクで倍率がかかり、ハンターの防御で軽減される）。
 */
public final class MonsterMoveSets {
	private static final Map<MonsterKind, List<MonsterMove>> SETS = new EnumMap<>(MonsterKind.class);

	/** 発見時・怒り時の咆哮（全種共通の形）。 */
	public static MonsterMove roar(MonsterKind kind) {
		return MonsterMove.of(kind.id + "_roar", ROAR, 50).anyFacing().track(10)
			.special(Special.ROAR, 14).shockwave(14, 22, 7f, 8, Knock.WIND, ElementType.NONE, Fx.SHOCK)
			.sound("monster.roar_" + kind.id, 12).build();
	}

	static {
		// ------------------------------------------------------------------
		// サケラン：酒と筋肉と声劇。ピースサインの後にビーム。
		// ------------------------------------------------------------------
		SETS.put(MonsterKind.SAKERAN, List.of(
			MonsterMove.of("sk_jab", PUNCH, 22).range(0, 4.5f).weight(4).track(8)
				.sphere(9, 12, 2.3f, 2.4f, -0.6f, 1.5f, 28, Knock.SMALL).sound("monster.swipe", 8).build(),
			MonsterMove.of("sk_double", DOUBLE_PUNCH, 34).range(0, 5f).weight(3).track(16)
				.sphere(9, 12, 2.3f, 2.4f, -0.6f, 1.5f, 24, Knock.SMALL)
				.sphere(20, 23, 2.4f, 2.4f, 0.6f, 1.6f, 30, Knock.LARGE).sound("monster.swipe", 8).build(),
			MonsterMove.of("sk_lariat", LARIAT, 54).range(0, 6.5f).weight(3).track(12).anyFacing()
				.multiCircle(14, 42, 6, 3.8f, 26, Knock.LARGE, ElementType.NONE, Fx.DUST)
				.charge(14, 42, 0.13f).sound("monster.swipe", 14).build(),
			MonsterMove.of("sk_sake", THROW, 32).range(6, 30).weight(3).track(16)
				.projectile(17, MonsterProjectileKind.SAKE_BOTTLE, 1, 0f, 1.1f, 24)
				.sound("monster.swipe", 15).build(),
			MonsterMove.of("sk_sake_rain", THROW, 44).range(4, 26).weight(2).track(14).rank(1)
				.projectile(16, MonsterProjectileKind.SAKE_BOTTLE, 5, 22f, 1.0f, 22)
				.projectile(28, MonsterProjectileKind.SAKE_BOTTLE, 3, 15f, 1.2f, 22)
				.special(Special.SAKE_POOLS, 34).sound("monster.swipe", 14).build(),
			MonsterMove.of("sk_tackle", CHARGE, 48).range(5, 26).weight(2).track(14)
				.sphere(14, 36, 1.6f, 1.6f, 0f, 1.9f, 40, Knock.LARGE).charge(14, 36, 0.58f)
				.sound("monster.step", 14).build(),
			MonsterMove.of("sk_press", LEAP, 52).range(4, 16).weight(2).track(14)
				.leap(14, 26, 0.0f, 0.62f).circle(28, 4.2f, 52, Knock.LARGE, ElementType.NONE, Fx.SHOCK)
				.shockwave(28, 34, 7f, 12, Knock.WIND, ElementType.NONE, Fx.DUST).sound("monster.slam", 27).build(),
			MonsterMove.of("sk_peace_beam", BEAM, 64).range(6, 26).weight(2).track(30).telegraph(Fx.NEON)
				.beam(32, 50, 3, 3.6f, 24f, 1.7f, 14, ElementType.NONE, Fx.NEON)
				.sound("monster.beam", 30).build(),
			MonsterMove.of("sk_voice", ROAR, 44).range(0, 18).weight(2).anyFacing().enraged().cooldown(300)
				.special(Special.ROAR, 16).shockwave(16, 26, 8f, 30, Knock.LARGE, ElementType.NONE, Fx.SHOCK)
				.sound("monster.roar_sakeran", 14).build(),
			MonsterMove.of("sk_pose", TAUNT, 40).range(0, 30).weight(1).anyFacing().cooldown(400)
				.sound("monster.tired", 10).build(),
			MonsterMove.of("sk_hip_back", HIP_ATTACK, 26).range(0, 5).weight(3).behind()
				.sphere(10, 13, -1.6f, 1.6f, 0f, 1.8f, 30, Knock.LARGE).sound("monster.swipe", 9).build(),
			MonsterMove.of("sk_fever", GROUND_POUND, 70).range(0, 14).weight(2).enraged().rank(2).cooldown(500)
				.multiCircle(16, 56, 10, 5f, 30, Knock.LAUNCH, ElementType.NONE, Fx.SAKE)
				.special(Special.SAKE_POOLS, 20).sound("monster.slam", 15).build()
		));

		// ------------------------------------------------------------------
		// ブスコバリーノ王：浮遊する天界の王。光輪、裁きの光柱、ソンブレロ投げ。
		// ------------------------------------------------------------------
		SETS.put(MonsterKind.BARINO, List.of(
			MonsterMove.of("ba_sweep", SWIPE_R, 30).range(0, 7f).weight(4).track(10)
				.sphere(12, 18, 2.6f, 1.2f, 0f, 2.6f, 32, Knock.LARGE, ElementType.NONE, Fx.HOLY)
				.sound("monster.swipe", 11).build(),
			MonsterMove.of("ba_hip", HIP_ATTACK, 28).range(0, 6).weight(4).behind()
				.sphere(11, 14, -2.3f, 1.8f, 0f, 2.4f, 36, Knock.LARGE, ElementType.NONE, Fx.HOLY)
				.sound("monster.swipe", 10).build(),
			MonsterMove.of("ba_halo_beam", BEAM, 60).range(5, 30).weight(3).track(30).telegraph(Fx.HOLY)
				.beam(30, 48, 3, 5.0f, 30f, 2.0f, 13, ElementType.DRAGON, Fx.HOLY)
				.sound("monster.beam", 28).build(),
			MonsterMove.of("ba_sombrero", THROW, 48).range(5, 24).weight(2).track(18)
				.projectile(20, MonsterProjectileKind.SOMBRERO, 1, 0f, 0.9f, 34)
				.sound("monster.swipe", 18).build(),
			MonsterMove.of("ba_pillars", SUMMON, 64).range(0, 40).weight(2).anyFacing().cooldown(200)
				.telegraph(Fx.HOLY).special(Special.PILLARS, 18).sound("monster.holy", 16).build(),
			MonsterMove.of("ba_holy_drop", HIP_DROP, 60).range(3, 18).weight(2).track(30).flying()
				.rise(8, 20, 0.45f).leap(22, 34, 0.55f, 0.0f)
				.circle(40, 4.8f, 60, Knock.LARGE, ElementType.DRAGON, Fx.HOLY)
				.shockwave(40, 48, 8f, 14, Knock.WIND, ElementType.NONE, Fx.HOLY).sound("monster.slam", 39).build(),
			MonsterMove.of("ba_orbs", SUMMON, 40).range(6, 30).weight(2).track(14)
				.projectile(16, MonsterProjectileKind.HOLY_ORB, 3, 35f, 0.45f, 26)
				.sound("monster.holy", 14).build(),
			MonsterMove.of("ba_glide", CHARGE, 44).range(6, 26).weight(2).track(14).flying()
				.sphere(14, 34, 1.8f, 1.6f, 0f, 2.2f, 40, Knock.LARGE, ElementType.NONE, Fx.HOLY)
				.charge(14, 34, 0.62f).sound("monster.wing", 14).build(),
			MonsterMove.of("ba_erase", SUMMON, 76).range(0, 40).weight(3).anyFacing().enraged().rank(1).cooldown(600)
				.telegraph(Fx.HOLY).special(Special.ERASE_WAVE, 40).sound("monster.holy", 36).build(),
			MonsterMove.of("ba_double_halo", BEAM, 70).range(5, 30).weight(2).enraged().rank(2).track(26)
				.beam(28, 60, 3, 5.0f, 30f, 2.4f, 14, ElementType.DRAGON, Fx.HOLY)
				.projectile(40, MonsterProjectileKind.HOLY_ORB, 4, 50f, 0.5f, 24)
				.sound("monster.beam", 26).build()
		));

		// ------------------------------------------------------------------
		// ジャングリア：地獄の悪魔。尻眼レーザー、獄炎ブレス、地形破壊。
		// ------------------------------------------------------------------
		SETS.put(MonsterKind.JANGRIA, List.of(
			MonsterMove.of("jg_claws", DOUBLE_SWIPE, 34).range(0, 6f).weight(4).track(14)
				.sphere(10, 13, 2.4f, 1.0f, -1.4f, 1.8f, 30, Knock.SMALL, ElementType.FIRE, Fx.FIRE)
				.sphere(20, 23, 2.4f, 1.0f, 1.4f, 1.8f, 34, Knock.LARGE, ElementType.FIRE, Fx.FIRE)
				.sound("monster.swipe", 9).build(),
			MonsterMove.of("jg_breath", BREATH, 56).range(3, 14).weight(3).track(18).telegraph(Fx.FIRE)
				.cone(20, 44, 4, 3.4f, 13f, 28f, 13, ElementType.FIRE, Fx.FIRE)
				.sound("monster.fire", 19).build(),
			MonsterMove.of("jg_butt_lasers", BUTT_BEAM, 62).range(6, 26).weight(3).behind().track(26).telegraph(Fx.FIRE)
				.rearBeam(28, 50, 4, 1.8f, -0.8f, 24f, 1.4f, 11, ElementType.FIRE, Fx.FIRE)
				.rearBeam(30, 52, 4, 1.8f, 0.8f, 24f, 1.4f, 11, ElementType.FIRE, Fx.FIRE)
				.sound("monster.beam", 26).build(),
			MonsterMove.of("jg_butt_lasers_front", BUTT_BEAM, 70).range(8, 26).weight(2).track(34).telegraph(Fx.FIRE)
				.rearBeam(36, 58, 4, 1.8f, -0.8f, 24f, 1.4f, 11, ElementType.FIRE, Fx.FIRE)
				.rearBeam(38, 60, 4, 1.8f, 0.8f, 24f, 1.4f, 11, ElementType.FIRE, Fx.FIRE)
				.sound("monster.beam", 34).build(),
			MonsterMove.of("jg_hip_drop", HIP_DROP, 58).range(3, 16).weight(2).track(26)
				.leap(10, 26, 0.0f, 0.85f).circle(32, 5f, 62, Knock.LARGE, ElementType.FIRE, Fx.FIRE)
				.special(Special.ERUPTIONS, 34).sound("monster.slam", 31).build(),
			MonsterMove.of("jg_gust", WING_GUST, 40).range(0, 10).weight(2).anyFacing()
				.shockwave(12, 20, 7f, 14, Knock.WIND, ElementType.NONE, Fx.DUST)
				.projectile(18, MonsterProjectileKind.FIREBALL, 4, 70f, 0.7f, 24)
				.sound("monster.wing", 10).build(),
			MonsterMove.of("jg_dive", DIVE, 66).range(8, 30).weight(2).track(30).flying()
				.rise(6, 22, 0.55f).sphere(32, 48, 1.8f, 1.2f, 0f, 2.6f, 56, Knock.LARGE, ElementType.FIRE, Fx.FIRE)
				.leap(30, 48, 0.85f, -0.35f).sound("monster.wing", 6).build(),
			MonsterMove.of("jg_eruption", SUMMON, 54).range(0, 30).weight(2).anyFacing().cooldown(220)
				.telegraph(Fx.FIRE).special(Special.ERUPTIONS, 16).sound("monster.fire", 14).build(),
			MonsterMove.of("jg_fireballs", THROW, 40).range(6, 30).weight(2).track(16)
				.projectile(16, MonsterProjectileKind.FIREBALL, 3, 18f, 1.0f, 28).sound("monster.fire", 15).build(),
			MonsterMove.of("jg_destroy", SLAM, 60).range(0, 9).weight(3).enraged().track(18).cooldown(240)
				.circle(24, 5.5f, 56, Knock.LAUNCH, ElementType.FIRE, Fx.FIRE)
				.special(Special.TERRAIN_BREAK, 24).sound("monster.slam", 23).build()
		));

		// ------------------------------------------------------------------
		// ブサイバー：サイバー鬼。POSE YOUR GAME、ミサイル、グリッチ転移。
		// ------------------------------------------------------------------
		SETS.put(MonsterKind.BUSAIBER, List.of(
			MonsterMove.of("by_slash", SWIPE_L, 28).range(0, 6f).weight(4).track(10)
				.sphere(11, 15, 2.4f, 1.2f, 0f, 2.2f, 32, Knock.SMALL, ElementType.THUNDER, Fx.THUNDER)
				.sound("monster.swipe", 10).build(),
			MonsterMove.of("by_missiles", SUMMON, 46).range(6, 34).weight(3).track(16)
				.projectile(14, MonsterProjectileKind.MISSILE, 3, 40f, 0.55f, 22)
				.projectile(22, MonsterProjectileKind.MISSILE, 3, 40f, 0.55f, 22)
				.sound("monster.electric", 12).build(),
			MonsterMove.of("by_laser", BEAM, 66).range(5, 26).weight(3).track(30).telegraph(Fx.NEON)
				.beam(32, 56, 3, 4.6f, 26f, 1.6f, 13, ElementType.THUNDER, Fx.NEON)
				.sound("monster.beam", 30).build(),
			MonsterMove.of("by_glitch", TELEPORT, 46).range(4, 30).weight(2).anyFacing().cooldown(160)
				.special(Special.TELEPORT_BEHIND, 16)
				.sphere(30, 34, -2.0f, 1.8f, 0f, 2.6f, 40, Knock.LARGE, ElementType.THUNDER, Fx.GLITCH)
				.sound("monster.glitch", 12).build(),
			MonsterMove.of("by_pose", POSE, 110).range(0, 40).weight(2).anyFacing().cooldown(700)
				.telegraph(Fx.NEON).special(Special.POSE_JUDGE, 20).sound("monster.roar_busaiber", 18).build(),
			MonsterMove.of("by_cubes", THROW, 36).range(5, 28).weight(2).track(14)
				.projectile(14, MonsterProjectileKind.GLITCH_CUBE, 4, 25f, 0.9f, 22)
				.sound("monster.glitch", 13).build(),
			MonsterMove.of("by_charge", CHARGE, 46).range(6, 26).weight(2).track(14)
				.sphere(14, 36, 1.8f, 1.6f, 0f, 2.2f, 44, Knock.LARGE, ElementType.THUNDER, Fx.NEON)
				.charge(14, 36, 0.66f).sound("monster.electric", 14).build(),
			MonsterMove.of("by_hip_shock", HIP_DROP, 52).range(0, 12).weight(2).track(16)
				.leap(10, 22, 0.0f, 0.7f).circle(28, 4.8f, 54, Knock.LARGE, ElementType.THUNDER, Fx.THUNDER)
				.shockwave(28, 38, 9f, 22, Knock.SMALL, ElementType.THUNDER, Fx.THUNDER)
				.sound("monster.electric", 27).build(),
			MonsterMove.of("by_back", HIP_ATTACK, 26).range(0, 5).weight(3).behind()
				.sphere(10, 13, -2.2f, 1.8f, 0f, 2.3f, 34, Knock.LARGE, ElementType.THUNDER, Fx.THUNDER)
				.sound("monster.electric", 9).build(),
			MonsterMove.of("by_overload", SUMMON, 80).range(0, 40).weight(2).enraged().rank(1).cooldown(500)
				.telegraph(Fx.NEON).special(Special.PILLARS, 16)
				.projectile(40, MonsterProjectileKind.MISSILE, 6, 60f, 0.6f, 20)
				.sound("monster.electric", 14).build()
		));

		// ------------------------------------------------------------------
		// ブスコバ：オアシスの原点。水流、バウンド、尻プレス。
		// ------------------------------------------------------------------
		SETS.put(MonsterKind.BUSKOBA, List.of(
			MonsterMove.of("bk_whip", SWIPE_R, 26).range(0, 6.5f).weight(4).track(9)
				.sphere(10, 14, 2.6f, 1.0f, 0f, 2.4f, 28, Knock.SMALL).sound("monster.swipe", 9).build(),
			MonsterMove.of("bk_jet", BUTT_BEAM, 60).range(5, 24).weight(3).behind().track(26).telegraph(Fx.WATER)
				.rearBeam(28, 52, 3, 1.6f, 0f, 22f, 1.8f, 12, ElementType.WATER, Fx.WATER)
				.sound("monster.water", 26).build(),
			MonsterMove.of("bk_jet_turn", BUTT_BEAM, 68).range(7, 24).weight(2).track(34).telegraph(Fx.WATER)
				.rearBeam(36, 58, 3, 1.6f, 0f, 22f, 1.8f, 12, ElementType.WATER, Fx.WATER)
				.sound("monster.water", 34).build(),
			MonsterMove.of("bk_bounce", BOUNCE, 66).range(4, 22).weight(3).track(60)
				.leap(6, 16, 0.38f, 0.55f).circle(18, 3.8f, 32, Knock.LARGE, ElementType.NONE, Fx.DUST)
				.leap(24, 34, 0.38f, 0.55f).circle(36, 3.8f, 32, Knock.LARGE, ElementType.NONE, Fx.DUST)
				.leap(42, 52, 0.38f, 0.6f).circle(54, 4.4f, 40, Knock.LARGE, ElementType.WATER, Fx.WATER)
				.sound("monster.slam", 17).build(),
			MonsterMove.of("bk_press", HIP_DROP, 54).range(3, 15).weight(2).track(22)
				.leap(10, 24, 0.0f, 0.9f).circle(30, 5f, 62, Knock.LARGE, ElementType.WATER, Fx.WATER)
				.shockwave(30, 38, 8f, 12, Knock.WIND, ElementType.NONE, Fx.WATER).sound("monster.slam", 29).build(),
			MonsterMove.of("bk_bubbles", THROW, 40).range(5, 26).weight(2).track(14)
				.projectile(14, MonsterProjectileKind.WATER_ORB, 5, 50f, 0.42f, 20).sound("monster.water", 13).build(),
			MonsterMove.of("bk_geyser", SUMMON, 52).range(0, 32).weight(2).anyFacing().cooldown(200)
				.telegraph(Fx.BUBBLE).special(Special.GEYSERS, 16).sound("monster.water", 14).build(),
			MonsterMove.of("bk_spin", SPIN, 44).range(0, 6f).weight(2).anyFacing()
				.multiCircle(12, 32, 7, 4.2f, 22, Knock.SMALL, ElementType.NONE, Fx.DUST).sound("monster.swipe", 11).build(),
			MonsterMove.of("bk_rush", CHARGE, 46).range(6, 26).weight(2).track(14)
				.sphere(14, 36, 1.8f, 1.4f, 0f, 2.2f, 40, Knock.LARGE).charge(14, 36, 0.6f).sound("monster.step", 14).build(),
			MonsterMove.of("bk_smile", TAUNT, 70).range(0, 40).weight(2).anyFacing().enraged().cooldown(600)
				.special(Special.HEAL_SMILE, 20)
				.shockwave(44, 60, 14f, 36, Knock.LARGE, ElementType.WATER, Fx.WATER)
				.sound("monster.roar_buskoba", 18).build()
		));
	}

	private MonsterMoveSets() {
	}

	public static List<MonsterMove> of(MonsterKind kind) {
		return SETS.get(kind);
	}
}
