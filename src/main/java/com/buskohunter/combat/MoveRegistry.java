package com.buskohunter.combat;

import com.buskohunter.core.ElementType;
import com.buskohunter.core.WeaponType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.buskohunter.combat.AnimKind.*;
import static com.buskohunter.combat.MoveDef.Shape.*;
import static com.buskohunter.core.WeaponType.*;

/**
 * 全武器の技テーブル。数値はモンハンのモーション値を参考にした独自調整。
 * tick 数は 20tick = 1 秒。
 */
public final class MoveRegistry {
	private static final Map<String, MoveDef> BY_ID = new HashMap<>();
	private static final List<MoveDef> BY_NET = new ArrayList<>();

	private MoveRegistry() {
	}

	private static MoveDef reg(MoveDef.Builder b) {
		MoveDef m = b.build();
		m.netId = BY_NET.size();
		BY_NET.add(m);
		BY_ID.put(m.id, m);
		return m;
	}

	public static MoveDef get(String id) {
		MoveDef m = BY_ID.get(id);
		if (m == null) throw new IllegalArgumentException("Unknown move: " + id);
		return m;
	}

	public static MoveDef byNetId(int netId) {
		return netId >= 0 && netId < BY_NET.size() ? BY_NET.get(netId) : null;
	}

	public static int count() {
		return BY_NET.size();
	}

	// =====================================================================
	// 共通
	// =====================================================================
	public static final MoveDef DODGE = reg(MoveDef.builder("dodge", null, DODGE_ROLL, 14)
		.iframes(1, 7).lunge(0, 9, 0.55f).stamina(25).cancel(11).sound("player.dodge", 0));
	public static final MoveDef DODGE_SIDESTEP = reg(MoveDef.builder("sidestep", null, SIDESTEP, 10)
		.iframes(1, 5).lunge(0, 6, 0.62f).stamina(15).cancel(8).sound("player.dodge", 0));
	public static final MoveDef DODGE_DEMON = reg(MoveDef.builder("demon_step", null, DEMON_STEP, 8)
		.iframes(1, 5).lunge(0, 6, 0.75f).stamina(10).cancel(6).sound("player.dodge", 0));
	public static final MoveDef KNOCKDOWN = reg(MoveDef.builder("knockdown", null, KNOCKED_DOWN, 34).cancel(22));
	public static final MoveDef STAGGER_MOVE = reg(MoveDef.builder("stagger", null, STAGGER, 12).cancel(12));
	public static final MoveDef ROAR_STUN = reg(MoveDef.builder("roar_cover", null, ROAR_COVER, 36).cancel(36));
	public static final MoveDef USE_SHARPEN = reg(MoveDef.builder("use_sharpen", null, SHARPEN, 40).cancel(40).move(0.2f));
	public static final MoveDef USE_FLEX = reg(MoveDef.builder("use_flex", null, FLEX, 22).cancel(22));
	public static final MoveDef USE_CARVE = reg(MoveDef.builder("use_carve", null, CARVE, 20).cancel(20));
	public static final MoveDef USE_THROW = reg(MoveDef.builder("use_throw", null, THROW, 10).cancel(8).move(0.5f));
	public static final MoveDef USE_PLACE = reg(MoveDef.builder("use_place", null, PLACE, 14).cancel(14));
	public static final MoveDef VICTORY = reg(MoveDef.builder("victory", null, TRIUMPH, 60).cancel(60));
	public static final MoveDef GUARD_HIT = reg(MoveDef.builder("guard_hit", null, GUARD, 10).cancel(8));

	// =====================================================================
	// 大剣
	// =====================================================================
	public static final MoveDef GS_OVERHEAD = reg(MoveDef.builder("gs_overhead", GREAT_SWORD, CHARGE_OVERHEAD, 28)
		.charge(8, InputKind.A, new int[]{14, 28, 42}, new float[]{1.25f, 1.55f, 1.9f}, 70)
		.arc(13, 4.2f, 45, 48).cancel(19).sound("weapon.swing_heavy", 11));
	public static final MoveDef GS_STRONG = reg(MoveDef.builder("gs_strong", GREAT_SWORD, CHARGE_OVERHEAD, 30)
		.charge(10, InputKind.A, new int[]{14, 28, 42}, new float[]{1.3f, 1.65f, 2.1f}, 70)
		.arc(15, 4.4f, 50, 62).hyperArmor().cancel(22).sound("weapon.swing_heavy", 13));
	public static final MoveDef GS_TRUE = reg(MoveDef.builder("gs_true", GREAT_SWORD, SLAM, 42)
		.charge(12, InputKind.A, new int[]{14, 28, 42}, new float[]{1.35f, 1.8f, 2.4f}, 70)
		.arc(15, 4.0f, 40, 30).arc(25, 4.6f, 55, 95).tune(1.2f, 1.0f, 0).hyperArmor().cancel(36)
		.sound("weapon.charge_release", 23));
	public static final MoveDef GS_SWEEP = reg(MoveDef.builder("gs_sweep", GREAT_SWORD, WIDE_SWEEP, 22)
		.arc(9, 4.2f, 85, 36).cancel(15).sound("weapon.swing_heavy", 7));
	public static final MoveDef GS_RISING = reg(MoveDef.builder("gs_rising", GREAT_SWORD, SLASH_UP, 20)
		.arc(8, 4.0f, 45, 40).cancel(14).sound("weapon.swing_heavy", 6));
	public static final MoveDef GS_TACKLE = reg(MoveDef.builder("gs_tackle", GREAT_SWORD, TACKLE, 14)
		.arc(6, 2.6f, 50, 18).blunt(12).lunge(0, 6, 0.36f).hyperArmor().stamina(15).cancel(8)
		.sound("weapon.swing_blunt", 4));

	// =====================================================================
	// 太刀
	// =====================================================================
	public static final MoveDef LS_OVERHEAD = reg(MoveDef.builder("ls_overhead", LONG_SWORD, SLASH_DOWN, 16)
		.arc(6, 3.8f, 40, 26).lunge(0, 4, 0.15f).cancel(10).sound("weapon.swing_light", 4));
	public static final MoveDef LS_THRUST = reg(MoveDef.builder("ls_thrust", LONG_SWORD, THRUST, 14)
		.line(5, 4.0f, 1.1f, 14).cancel(9).sound("weapon.swing_light", 3));
	public static final MoveDef LS_RISING = reg(MoveDef.builder("ls_rising", LONG_SWORD, SLASH_UP, 16)
		.arc(6, 3.8f, 40, 18).cancel(10).sound("weapon.swing_light", 4));
	public static final MoveDef LS_FADE = reg(MoveDef.builder("ls_fade", LONG_SWORD, SLASH_SIDE_L, 18)
		.arc(5, 3.6f, 60, 24).lunge(6, 14, -0.32f).cancel(12).sound("weapon.swing_light", 4));
	public static final MoveDef LS_STEP = reg(MoveDef.builder("ls_step", LONG_SWORD, SLASH_SIDE_R, 16)
		.arc(7, 3.8f, 50, 26).lunge(0, 6, 0.45f).cancel(10).sound("weapon.swing_light", 5));
	public static final MoveDef LS_SPIRIT1 = reg(MoveDef.builder("ls_spirit1", LONG_SWORD, SPIRIT_SLASH, 14)
		.arc(5, 3.8f, 50, 28).cancel(9).sound("weapon.spirit", 3));
	public static final MoveDef LS_SPIRIT2 = reg(MoveDef.builder("ls_spirit2", LONG_SWORD, SPIRIT_SLASH, 18)
		.arc(4, 3.8f, 50, 20).arc(10, 3.8f, 50, 22).mirror().cancel(13).sound("weapon.spirit", 2));
	public static final MoveDef LS_SPIRIT3 = reg(MoveDef.builder("ls_spirit3", LONG_SWORD, SPIRIT_SLASH, 24)
		.multi(4, 3, 5, ARC, 4.0f, 60, 14).arc(18, 4.0f, 60, 30).cancel(19).sound("weapon.spirit", 2));
	public static final MoveDef LS_ROUNDSLASH = reg(MoveDef.builder("ls_roundslash", LONG_SWORD, SPIRIT_ROUNDSLASH, 26)
		.circle(10, 4.4f, 42).lunge(0, 6, 0.2f).cancel(20).sound("weapon.spirit", 8));
	public static final MoveDef LS_FORESIGHT = reg(MoveDef.builder("ls_foresight", LONG_SWORD, FORESIGHT, 22)
		.counter(1, 11).iframes(1, 11).arc(9, 3.8f, 50, 30).lunge(0, 5, -0.42f).lunge(8, 12, 0.5f)
		.stamina(10).cancel(16).sound("weapon.swing_light", 7));
	public static final MoveDef LS_HELMBREAKER = reg(MoveDef.builder("ls_helmbreaker", LONG_SWORD, HELM_BREAKER, 42)
		.iframes(0, 14).lunge(0, 9, 0.42f, 0.5f).multi(18, 6, 3, ARC, 3.2f, 60, 9).circle(37, 4.2f, 60)
		.hyperArmor().cancel(36).sound("weapon.spirit", 16));

	// =====================================================================
	// 片手剣
	// =====================================================================
	public static final MoveDef SNS_CHOP = reg(MoveDef.builder("sns_chop", SWORD_AND_SHIELD, SLASH_DOWN, 10)
		.arc(4, 3.2f, 45, 18).cancel(6).sound("weapon.swing_light", 2));
	public static final MoveDef SNS_SIDE = reg(MoveDef.builder("sns_side", SWORD_AND_SHIELD, SLASH_SIDE_L, 10)
		.arc(4, 3.2f, 60, 16).cancel(6).sound("weapon.swing_light", 2));
	public static final MoveDef SNS_COMBO = reg(MoveDef.builder("sns_combo", SWORD_AND_SHIELD, SHIELD_BASH, 16)
		.arc(4, 2.8f, 45, 12).blunt(10).arc(9, 3.2f, 50, 22).cancel(12).sound("weapon.swing_light", 7));
	public static final MoveDef SNS_ROUND = reg(MoveDef.builder("sns_round", SWORD_AND_SHIELD, SPIN, 16)
		.circle(7, 3.6f, 26).cancel(11).sound("weapon.swing_light", 5));
	public static final MoveDef SNS_BASH = reg(MoveDef.builder("sns_bash", SWORD_AND_SHIELD, SHIELD_BASH, 12)
		.arc(5, 2.8f, 45, 14).blunt(18).lunge(0, 4, 0.2f).cancel(8).sound("weapon.swing_blunt", 3));
	public static final MoveDef SNS_UPPER = reg(MoveDef.builder("sns_upper", SWORD_AND_SHIELD, SHIELD_UPPER, 26)
		.iframes(0, 9).arc(8, 3.2f, 50, 30).blunt(30).lunge(4, 10, 0.2f, 0.4f).cancel(20)
		.sound("weapon.swing_blunt", 6));

	// =====================================================================
	// 双剣
	// =====================================================================
	public static final MoveDef DB_DOUBLE = reg(MoveDef.builder("db_double", DUAL_BLADES, DOUBLE_SLASH, 10)
		.multi(3, 2, 3, ARC, 3.2f, 50, 9).cancel(7).sound("weapon.swing_light", 2));
	public static final MoveDef DB_RETURN = reg(MoveDef.builder("db_return", DUAL_BLADES, DUAL_FLURRY, 12)
		.multi(3, 3, 3, ARC, 3.2f, 55, 7).cancel(9).sound("weapon.swing_light", 2));
	public static final MoveDef DB_WHEEL = reg(MoveDef.builder("db_wheel", DUAL_BLADES, DUAL_WHEEL, 16)
		.multi(4, 3, 3, ARC, 3.4f, 60, 10).lunge(0, 8, 0.25f).cancel(12).sound("weapon.swing_light", 3));
	public static final MoveDef DB_SPIN_A = reg(MoveDef.builder("db_spin_a", DUAL_BLADES, SPIN, 14)
		.multi(4, 2, 4, CIRCLE, 3.4f, 180, 11).cancel(10).sound("weapon.swing_light", 3));
	public static final MoveDef DB_SPIN_B = reg(MoveDef.builder("db_spin_b", DUAL_BLADES, SPIN, 14)
		.multi(4, 2, 4, CIRCLE, 3.4f, 180, 11).mirror().cancel(10).sound("weapon.swing_light", 3));
	public static final MoveDef DB_DEMON = reg(MoveDef.builder("db_demon", DUAL_BLADES, DOUBLE_SLASH, 10)
		.cancel(8).sound("weapon.demon_mode", 2));
	public static final MoveDef DB_DANCE = reg(MoveDef.builder("db_dance", DUAL_BLADES, DUAL_SPIN, 42)
		.multi(6, 10, 3, ARC, 3.4f, 70, 8).arc(36, 3.6f, 70, 22).stamina(20).cancel(38)
		.sound("weapon.demon_mode", 4));

	// =====================================================================
	// ハンマー
	// =====================================================================
	public static final MoveDef HM_SWING1 = reg(MoveDef.builder("hm_swing1", HAMMER, SLAM, 14)
		.arc(6, 3.2f, 45, 30).blunt(15).cancel(9).sound("weapon.swing_blunt", 4));
	public static final MoveDef HM_SWING2 = reg(MoveDef.builder("hm_swing2", HAMMER, SLAM, 14)
		.arc(6, 3.2f, 45, 30).blunt(15).mirror().cancel(9).sound("weapon.swing_blunt", 4));
	public static final MoveDef HM_UPSWING = reg(MoveDef.builder("hm_upswing", HAMMER, UPPERCUT, 22)
		.arc(9, 3.2f, 45, 90).blunt(40).cancel(16).sound("weapon.swing_blunt", 7));
	public static final MoveDef HM_CHARGE = reg(MoveDef.builder("hm_charge", HAMMER, CHARGE_LOW, 6)
		.charge(3, InputKind.B, new int[]{14, 30, 48}, new float[]{1f, 1f, 1f}, 400)
		.staminaPerTick(0.22f).move(0.65f).cancel(6).sound("weapon.charge", 2));
	public static final MoveDef HM_C1 = reg(MoveDef.builder("hm_c1", HAMMER, UPPERCUT, 14)
		.arc(5, 3.2f, 45, 20).blunt(15).lunge(0, 4, 0.2f).cancel(9).sound("weapon.swing_blunt", 3));
	public static final MoveDef HM_C2 = reg(MoveDef.builder("hm_c2", HAMMER, SLASH_SIDE_R, 18)
		.arc(7, 3.4f, 60, 45).blunt(25).lunge(0, 5, 0.25f).cancel(12).sound("weapon.swing_blunt", 5));
	public static final MoveDef HM_C3 = reg(MoveDef.builder("hm_c3", HAMMER, HOMERUN, 30)
		.arc(8, 3.2f, 45, 15).blunt(10).arc(20, 3.4f, 50, 110).blunt(60).lunge(0, 6, 0.3f).cancel(24)
		.sound("weapon.charge_release", 18));
	public static final MoveDef HM_SPIN = reg(MoveDef.builder("hm_spin", HAMMER, SPIN, 34)
		.multi(4, 4, 6, CIRCLE, 3.4f, 180, 20).blunt(10).arc(30, 3.4f, 50, 80).blunt(40)
		.lunge(0, 24, 0.2f).stamina(10).cancel(31).sound("weapon.swing_blunt", 4));

	// =====================================================================
	// 狩猟笛
	// =====================================================================
	public static final MoveDef HH_LEFT = reg(MoveDef.builder("hh_left", HUNTING_HORN, HORN_SWING, 16)
		.arc(7, 3.4f, 70, 30).blunt(15).cancel(11).sound("weapon.horn_note1", 6));
	public static final MoveDef HH_RIGHT = reg(MoveDef.builder("hh_right", HUNTING_HORN, HORN_SLAM, 18)
		.arc(8, 3.2f, 45, 34).blunt(20).cancel(12).sound("weapon.horn_note2", 7));
	public static final MoveDef HH_BACK = reg(MoveDef.builder("hh_back", HUNTING_HORN, HORN_BACK, 20)
		.arc(9, 3.6f, 60, 36).blunt(25).cancel(14).sound("weapon.horn_note3", 8));
	public static final MoveDef HH_RECITAL = reg(MoveDef.builder("hh_recital", HUNTING_HORN, HORN_PLAY, 40)
		.circle(10, 3.2f, 20).blunt(10).circle(30, 3.8f, 25).blunt(10).hyperArmor().cancel(34)
		.sound("weapon.horn_song", 28));

	// =====================================================================
	// ランス
	// =====================================================================
	public static final MoveDef LN_MID = reg(MoveDef.builder("ln_mid", LANCE, THRUST, 12)
		.line(5, 4.4f, 1.0f, 24).cancel(8).sound("weapon.swing_light", 3));
	public static final MoveDef LN_HIGH = reg(MoveDef.builder("ln_high", LANCE, THRUST_HIGH, 12)
		.line(5, 4.4f, 1.0f, 24).height(2.2f).cancel(8).sound("weapon.swing_light", 3));
	public static final MoveDef LN_CHARGE = reg(MoveDef.builder("ln_charge", LANCE, LANCE_CHARGE, 80)
		.multi(8, 12, 6, ARC, 3.0f, 40, 16).lunge(4, 76, 0.45f).stamina(10).staminaPerTick(0.25f)
		.cancel(6).sound("weapon.swing_heavy", 4));
	public static final MoveDef LN_FINISH = reg(MoveDef.builder("ln_finish", LANCE, THRUST, 18)
		.line(6, 4.6f, 1.2f, 50).lunge(0, 4, 0.2f).cancel(14).sound("weapon.swing_heavy", 5));
	public static final MoveDef LN_COUNTER = reg(MoveDef.builder("ln_counter", LANCE, COUNTER_THRUST, 24)
		.guard(1, 12).counter(1, 12).line(16, 4.6f, 1.2f, 40).cancel(19).sound("weapon.swing_heavy", 15));

	// =====================================================================
	// ガンランス
	// =====================================================================
	public static final MoveDef GL_POKE = reg(MoveDef.builder("gl_poke", GUNLANCE, THRUST, 13)
		.line(5, 4.2f, 1.0f, 20).cancel(8).sound("weapon.swing_light", 3));
	public static final MoveDef GL_UPSLASH = reg(MoveDef.builder("gl_upslash", GUNLANCE, SLASH_UP, 18)
		.arc(7, 3.6f, 45, 30).cancel(12).sound("weapon.swing_heavy", 5));
	public static final MoveDef GL_SLAM = reg(MoveDef.builder("gl_slam", GUNLANCE, SLAM, 22)
		.arc(9, 3.8f, 45, 40).cancel(14).sound("weapon.swing_heavy", 7));
	public static final MoveDef GL_SHELL = reg(MoveDef.builder("gl_shell", GUNLANCE, GUNLANCE_SHELL, 16)
		.charge(3, InputKind.B, new int[]{18}, new float[]{1.6f}, 40)
		.fixed(5, 1, 0, ARC, 3.4f, 40, 30, ElementType.FIRE).cancel(11).sound("weapon.gunlance_shell", 5));
	public static final MoveDef GL_BURST = reg(MoveDef.builder("gl_burst", GUNLANCE, FULL_BURST, 30)
		.cancel(25).sound("weapon.gunlance_shell", 6));
	public static final MoveDef GL_WYVERN = reg(MoveDef.builder("gl_wyvern", GUNLANCE, WYVERNFIRE, 52)
		.fixed(34, 3, 3, LINE, 6.5f, 2.6f, 75, ElementType.FIRE).lunge(34, 40, -0.45f).hyperArmor()
		.cancel(46).sound("weapon.gunlance_wyvernfire", 0));
	public static final MoveDef GL_RELOAD = reg(MoveDef.builder("gl_reload", GUNLANCE, RELOAD, 20)
		.guard(0, 6).cancel(18).sound("weapon.reload", 8));

	// =====================================================================
	// スラッシュアックス
	// =====================================================================
	public static final MoveDef SA_AXE_DOWN = reg(MoveDef.builder("sa_axe_down", SWITCH_AXE, SLASH_DOWN, 18)
		.arc(8, 4.0f, 45, 40).cancel(12).sound("weapon.swing_heavy", 6));
	public static final MoveDef SA_AXE_UP = reg(MoveDef.builder("sa_axe_up", SWITCH_AXE, SLASH_UP, 16)
		.arc(6, 4.0f, 45, 30).cancel(10).sound("weapon.swing_heavy", 4));
	public static final MoveDef SA_AXE_WILD = reg(MoveDef.builder("sa_axe_wild", SWITCH_AXE, WIDE_SWEEP, 20)
		.arc(8, 4.2f, 85, 34).lunge(0, 4, 0.2f).cancel(14).sound("weapon.swing_heavy", 6));
	public static final MoveDef SA_TO_SWORD = reg(MoveDef.builder("sa_to_sword", SWITCH_AXE, MORPH, 16)
		.arc(9, 3.8f, 50, 30).cancel(12).sound("weapon.morph", 2));
	public static final MoveDef SA_RELOAD = reg(MoveDef.builder("sa_reload", SWITCH_AXE, RELOAD, 24)
		.cancel(22).sound("weapon.reload", 10));
	public static final MoveDef SA_SW_DOWN = reg(MoveDef.builder("sa_sw_down", SWITCH_AXE, SLASH_DOWN, 14)
		.arc(6, 3.6f, 45, 30).tune(1.3f, 1.2f, 0).cancel(9).sound("weapon.swing_light", 4));
	public static final MoveDef SA_SW_DOUBLE = reg(MoveDef.builder("sa_sw_double", SWITCH_AXE, DOUBLE_SLASH, 16)
		.multi(5, 2, 4, ARC, 3.6f, 50, 22).tune(1.3f, 1.2f, 0).cancel(11).sound("weapon.swing_light", 3));
	public static final MoveDef SA_SW_UP = reg(MoveDef.builder("sa_sw_up", SWITCH_AXE, SLASH_UP, 14)
		.arc(6, 3.6f, 45, 28).tune(1.3f, 1.2f, 0).cancel(9).sound("weapon.swing_light", 4));
	public static final MoveDef SA_DISCHARGE = reg(MoveDef.builder("sa_discharge", SWITCH_AXE, DISCHARGE, 50)
		.multi(8, 10, 3, LINE, 3.2f, 1.4f, 10).tune(1.6f, 1.5f, 0)
		.fixed(42, 1, 0, CIRCLE, 4.2f, 180, 120, null).hyperArmor().cancel(46).sound("weapon.phial_burst", 40));
	public static final MoveDef SA_TO_AXE = reg(MoveDef.builder("sa_to_axe", SWITCH_AXE, MORPH, 16)
		.arc(9, 4.0f, 50, 34).cancel(12).sound("weapon.morph", 2));

	// =====================================================================
	// チャージアックス
	// =====================================================================
	public static final MoveDef CB_WEAK = reg(MoveDef.builder("cb_weak", CHARGE_BLADE, SLASH_DOWN, 12)
		.arc(5, 3.4f, 45, 16).cancel(8).sound("weapon.swing_light", 3));
	public static final MoveDef CB_RETURN = reg(MoveDef.builder("cb_return", CHARGE_BLADE, SLASH_SIDE_L, 14)
		.arc(6, 3.4f, 60, 18).cancel(9).sound("weapon.swing_light", 4));
	public static final MoveDef CB_SPIN = reg(MoveDef.builder("cb_spin", CHARGE_BLADE, SPIN, 18)
		.circle(8, 3.6f, 26).cancel(13).sound("weapon.swing_light", 6));
	public static final MoveDef CB_DASH = reg(MoveDef.builder("cb_dash", CHARGE_BLADE, THRUST, 16)
		.arc(7, 3.4f, 45, 22).lunge(0, 6, 0.45f).cancel(11).sound("weapon.swing_light", 5));
	public static final MoveDef CB_CHARGE = reg(MoveDef.builder("cb_charge", CHARGE_BLADE, PHIAL_CHARGE, 20)
		.guard(0, 12).cancel(18).sound("weapon.reload", 10));
	public static final MoveDef CB_TO_AXE = reg(MoveDef.builder("cb_to_axe", CHARGE_BLADE, MORPH, 20)
		.arc(10, 3.8f, 45, 30).cancel(15).sound("weapon.morph", 3));
	public static final MoveDef CB_AX_RISING = reg(MoveDef.builder("cb_ax_rising", CHARGE_BLADE, SLASH_UP, 20)
		.arc(8, 3.8f, 45, 30).cancel(14).sound("weapon.swing_heavy", 6));
	public static final MoveDef CB_AX_SLAM = reg(MoveDef.builder("cb_ax_slam", CHARGE_BLADE, SLAM, 22)
		.arc(9, 3.8f, 45, 40).cancel(15).sound("weapon.swing_heavy", 7));
	public static final MoveDef CB_AED1 = reg(MoveDef.builder("cb_aed1", CHARGE_BLADE, DISCHARGE, 20)
		.arc(8, 3.8f, 50, 22).cancel(15).sound("weapon.swing_heavy", 6));
	public static final MoveDef CB_AED2 = reg(MoveDef.builder("cb_aed2", CHARGE_BLADE, SLASH_SIDE_L, 24)
		.arc(8, 3.8f, 50, 25).arc(14, 3.8f, 50, 25).cancel(18).sound("weapon.swing_heavy", 6));
	public static final MoveDef CB_SAED = reg(MoveDef.builder("cb_saed", CHARGE_BLADE, SUPER_DISCHARGE, 40)
		.arc(22, 4.0f, 45, 50).hyperArmor().cancel(35).sound("weapon.phial_burst", 22));
	public static final MoveDef CB_TO_SWORD = reg(MoveDef.builder("cb_to_sword", CHARGE_BLADE, MORPH, 14)
		.cancel(10).sound("weapon.morph", 2));

	// =====================================================================
	// 操虫棍
	// =====================================================================
	public static final MoveDef IG_RISING = reg(MoveDef.builder("ig_rising", INSECT_GLAIVE, SLASH_UP, 14)
		.arc(6, 3.8f, 45, 22).cancel(9).sound("weapon.swing_light", 4));
	public static final MoveDef IG_KESA = reg(MoveDef.builder("ig_kesa", INSECT_GLAIVE, SLASH_SIDE_R, 14)
		.arc(6, 3.8f, 55, 20).cancel(9).sound("weapon.swing_light", 4));
	public static final MoveDef IG_SPIN = reg(MoveDef.builder("ig_spin", INSECT_GLAIVE, SPIN, 18)
		.circle(8, 3.8f, 26).cancel(13).sound("weapon.swing_light", 6));
	public static final MoveDef IG_VAULT = reg(MoveDef.builder("ig_vault", INSECT_GLAIVE, VAULT, 10)
		.lunge(0, 3, 0.25f, 0.95f).iframes(0, 4).stamina(15).cancel(5).sound("weapon.vault", 0));
	public static final MoveDef IG_AERIAL = reg(MoveDef.builder("ig_aerial", INSECT_GLAIVE, AERIAL_SLASH, 12)
		.arc(4, 3.6f, 70, 20).height(0.4f).airborne().cancel(8).sound("weapon.swing_light", 2));
	public static final MoveDef IG_DIVE = reg(MoveDef.builder("ig_dive", INSECT_GLAIVE, DIVE_THRUST, 16)
		.lunge(0, 10, 0.5f, -0.6f).arc(8, 3.2f, 45, 28).airborne().cancel(13).sound("weapon.swing_heavy", 3));
	public static final MoveDef IG_KINSECT = reg(MoveDef.builder("ig_kinsect", INSECT_GLAIVE, KINSECT_LAUNCH, 10)
		.move(0.6f).cancel(7).sound("weapon.kinsect", 2));

	// =====================================================================
	// ライトボウガン
	// =====================================================================
	public static final MoveDef LBG_FIRE = reg(MoveDef.builder("lbg_fire", LIGHT_BOWGUN, SHOOT, 8)
		.shot(2, ShotKind.NORMAL, 1, 12).move(0.6f).cancel(6).sound("weapon.bowgun_light", 2));
	public static final MoveDef LBG_RAPID = reg(MoveDef.builder("lbg_rapid", LIGHT_BOWGUN, SHOOT, 14)
		.shot(2, ShotKind.NORMAL, 1, 8).shot(6, ShotKind.NORMAL, 1, 8).shot(10, ShotKind.NORMAL, 1, 8)
		.move(0.4f).cancel(12).sound("weapon.bowgun_light", 2));
	public static final MoveDef LBG_RELOAD = reg(MoveDef.builder("lbg_reload", LIGHT_BOWGUN, RELOAD, 16)
		.move(0.5f).cancel(15).sound("weapon.reload", 4));

	// =====================================================================
	// ヘビィボウガン
	// =====================================================================
	public static final MoveDef HBG_FIRE = reg(MoveDef.builder("hbg_fire", HEAVY_BOWGUN, SHOOT_HEAVY, 14)
		.shot(3, ShotKind.NORMAL, 1, 18).move(0.25f).cancel(10).sound("weapon.bowgun_heavy", 3));
	public static final MoveDef HBG_GATLING = reg(MoveDef.builder("hbg_gatling", HEAVY_BOWGUN, GATLING, 6)
		.charge(2, InputKind.B, new int[]{1}, new float[]{1f}, 160).move(0.15f).cancel(6)
		.sound("weapon.charge", 0));
	public static final MoveDef HBG_RELOAD = reg(MoveDef.builder("hbg_reload", HEAVY_BOWGUN, RELOAD, 26)
		.move(0.3f).cancel(25).sound("weapon.reload", 8));

	// =====================================================================
	// 弓
	// =====================================================================
	public static final MoveDef BOW_CHARGE = reg(MoveDef.builder("bow_charge", BOW, BOW_DRAW, 8)
		.charge(1, InputKind.A, new int[]{10, 22, 36}, new float[]{1.0f, 1.2f, 1.45f}, 200)
		.staminaPerTick(0.28f).move(0.55f).cancel(6).sound("weapon.bow_draw", 0));
	public static final MoveDef BOW_POWER_SHOT = reg(MoveDef.builder("bow_power", BOW, BOW_POWER, 18)
		.shot(6, ShotKind.ARROW_POWER, 3, 14).move(0.2f).cancel(13).sound("weapon.bow_release", 6));
	public static final MoveDef BOW_PIERCER = reg(MoveDef.builder("bow_piercer", BOW, PIERCER, 46)
		.multi(36, 6, 2, LINE, 40f, 1.3f, 12).height(1.5f).lunge(36, 40, -0.4f).hyperArmor()
		.stamina(30).cancel(42).sound("weapon.bow_piercer", 20));

	/** 武器種ごとの回避技。 */
	public static MoveDef dodgeFor(WeaponType type, boolean demonMode) {
		if (type == DUAL_BLADES && demonMode) return DODGE_DEMON;
		if (type == LIGHT_BOWGUN) return DODGE_SIDESTEP;
		return DODGE;
	}

	/** クラスロード時に全技を登録させるためのダミー。 */
	public static void init() {
	}
}
