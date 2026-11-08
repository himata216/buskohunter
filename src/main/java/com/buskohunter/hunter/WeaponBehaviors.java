package com.buskohunter.hunter;

import com.buskohunter.net.Net;
import com.buskohunter.combat.DamageCalc;
import com.buskohunter.combat.InputKind;
import com.buskohunter.combat.MoveDef;
import com.buskohunter.combat.ShotKind;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.WeaponType;
import com.buskohunter.entity.Kinsect;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.net.Payloads;
import com.buskohunter.util.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import static com.buskohunter.combat.MoveRegistry.*;

/**
 * 14 武器種それぞれのコンボ派生・ゲージ・特殊処理。
 */
public final class WeaponBehaviors {
	private WeaponBehaviors() {
	}

	// =====================================================================
	// 技の選択
	// =====================================================================
	/** 何も技を出していない状態からの入力。 */
	public static MoveDef start(HunterState s, InputKind in, boolean airborne) {
		InputKind b = in.base();
		return switch (s.weapon) {
			case GREAT_SWORD -> {
				s.gsChain = 0;
				yield switch (b) {
					case A -> GS_OVERHEAD;
					case B -> GS_SWEEP;
					default -> GS_TACKLE;
				};
			}
			case LONG_SWORD -> switch (in) {
				case GUARD_S -> LS_FORESIGHT;
				case GUARD_A -> s.spiritLevel > 0 ? LS_HELMBREAKER : LS_OVERHEAD;
				case A -> LS_OVERHEAD;
				case B, GUARD_B -> LS_STEP;
				default -> LS_SPIRIT1;
			};
			case SWORD_AND_SHIELD -> switch (b) {
				case A -> SNS_CHOP;
				case B -> SNS_ROUND;
				default -> SNS_BASH;
			};
			case DUAL_BLADES -> switch (b) {
				case A -> DB_DOUBLE;
				case B -> s.demonMode ? DB_DANCE : DB_SPIN_A;
				default -> DB_DEMON;
			};
			case HAMMER -> switch (b) {
				case A -> HM_SWING1;
				case B -> HM_CHARGE;
				default -> HM_SPIN;
			};
			case HUNTING_HORN -> in == InputKind.GUARD_A ? HH_RECITAL : switch (b) {
				case A -> HH_LEFT;
				case B -> HH_RIGHT;
				default -> HH_BACK;
			};
			case LANCE -> {
				s.chain = 0;
				yield switch (in) {
					case GUARD_A -> LN_COUNTER;
					case A -> LN_MID;
					case B, GUARD_B -> LN_HIGH;
					default -> LN_CHARGE;
				};
			}
			case GUNLANCE -> {
				s.chain = 0;
				yield switch (in) {
					case GUARD_S -> GL_RELOAD;
					case A, GUARD_A -> GL_POKE;
					case B, GUARD_B -> GL_SHELL;
					default -> GL_WYVERN;
				};
			}
			case SWITCH_AXE -> {
				if (s.saSword) {
					yield switch (in) {
						case GUARD_S -> SA_DISCHARGE;
						case A, GUARD_A -> SA_SW_DOWN;
						case B, GUARD_B -> SA_SW_UP;
						default -> SA_TO_AXE;
					};
				}
				yield switch (in) {
					case GUARD_S -> SA_RELOAD;
					case A, GUARD_A -> SA_AXE_DOWN;
					case B, GUARD_B -> SA_AXE_WILD;
					default -> s.saGauge >= 30 ? SA_TO_SWORD : SA_AXE_UP;
				};
			}
			case CHARGE_BLADE -> {
				if (s.cbAxe) {
					yield switch (b) {
						case A -> CB_AX_RISING;
						case B -> CB_AED1;
						default -> CB_TO_SWORD;
					};
				}
				yield switch (in) {
					case GUARD_A -> CB_TO_AXE;
					case A -> CB_WEAK;
					case B, GUARD_B -> CB_DASH;
					default -> CB_CHARGE;
				};
			}
			case INSECT_GLAIVE -> {
				if (airborne) {
					yield b == InputKind.B ? IG_DIVE : (b == InputKind.A ? IG_AERIAL : IG_KINSECT);
				}
				yield switch (b) {
					case A -> IG_RISING;
					case B -> IG_VAULT;
					default -> IG_KINSECT;
				};
			}
			case LIGHT_BOWGUN -> switch (in) {
				case GUARD_B -> null;
				case A, GUARD_A -> LBG_FIRE;
				case B -> LBG_RAPID;
				default -> LBG_RELOAD;
			};
			case HEAVY_BOWGUN -> switch (in) {
				case GUARD_B -> null;
				case A, GUARD_A -> HBG_FIRE;
				case B -> HBG_GATLING;
				default -> HBG_RELOAD;
			};
			case BOW -> switch (b) {
				case A -> BOW_CHARGE;
				case B -> BOW_POWER_SHOT;
				default -> BOW_PIERCER;
			};
		};
	}

	/** 技の途中（派生受付中）の入力。null なら派生しない。 */
	public static MoveDef combo(HunterState s, MoveDef cur, InputKind in, boolean airborne) {
		InputKind b = in.base();
		switch (s.weapon) {
			case GREAT_SWORD -> {
				if (b == InputKind.S) {
					s.gsChain = cur == GS_OVERHEAD || cur == GS_SWEEP ? 1 : (cur == GS_STRONG ? 2 : 0);
					return GS_TACKLE;
				}
				if (cur == GS_TACKLE && b == InputKind.A) {
					return s.gsChain == 2 ? GS_TRUE : (s.gsChain == 1 ? GS_STRONG : GS_OVERHEAD);
				}
				if (b == InputKind.A) {
					if (cur == GS_OVERHEAD || cur == GS_SWEEP) return GS_STRONG;
					if (cur == GS_STRONG) return GS_TRUE;
					if (cur == GS_TRUE) return GS_RISING;
					return GS_OVERHEAD;
				}
				if (b == InputKind.B) return cur == GS_SWEEP ? GS_RISING : GS_SWEEP;
				return null;
			}
			case LONG_SWORD -> {
				if (in == InputKind.GUARD_S) return LS_FORESIGHT;
				if (in == InputKind.GUARD_A && s.spiritLevel > 0) return LS_HELMBREAKER;
				if (b == InputKind.S) {
					if (cur == LS_SPIRIT1) return LS_SPIRIT2;
					if (cur == LS_SPIRIT2) return LS_SPIRIT3;
					if (cur == LS_SPIRIT3 || cur == LS_FORESIGHT) return LS_ROUNDSLASH;
					return LS_SPIRIT1;
				}
				if (b == InputKind.A) {
					if (cur == LS_OVERHEAD || cur == LS_STEP) return LS_THRUST;
					if (cur == LS_THRUST) return LS_RISING;
					return LS_OVERHEAD;
				}
				if (b == InputKind.B) return LS_FADE;
				return null;
			}
			case SWORD_AND_SHIELD -> {
				if (b == InputKind.S) return cur == SNS_BASH ? SNS_UPPER : SNS_BASH;
				if (b == InputKind.B) return SNS_ROUND;
				if (cur == SNS_CHOP) return SNS_SIDE;
				if (cur == SNS_SIDE) return SNS_COMBO;
				return SNS_CHOP;
			}
			case DUAL_BLADES -> {
				if (b == InputKind.S) return DB_DEMON;
				if (b == InputKind.B) {
					if (s.demonMode && cur != DB_DANCE) return DB_DANCE;
					return cur == DB_SPIN_A ? DB_SPIN_B : DB_SPIN_A;
				}
				if (cur == DB_DOUBLE) return DB_RETURN;
				if (cur == DB_RETURN) return DB_WHEEL;
				return DB_DOUBLE;
			}
			case HAMMER -> {
				if (b == InputKind.B) return HM_CHARGE;
				if (b == InputKind.S) return HM_SPIN;
				if (cur == HM_SWING1 || cur == HM_C1) return HM_SWING2;
				if (cur == HM_SWING2 || cur == HM_C2) return HM_UPSWING;
				return HM_SWING1;
			}
			case HUNTING_HORN -> {
				return start(s, in, airborne);
			}
			case LANCE -> {
				if (cur == LN_CHARGE) return b == InputKind.A ? LN_FINISH : null;
				if (in == InputKind.GUARD_A) return LN_COUNTER;
				if (b == InputKind.S) return LN_CHARGE;
				if (s.chain >= 3) return null;
				return b == InputKind.A ? LN_MID : LN_HIGH;
			}
			case GUNLANCE -> {
				if (in == InputKind.GUARD_S) return GL_RELOAD;
				if (b == InputKind.S) return GL_WYVERN;
				if (b == InputKind.B) return cur == GL_SLAM ? GL_BURST : GL_SHELL;
				if (cur == GL_POKE) return s.chain >= 2 ? GL_UPSLASH : GL_POKE;
				if (cur == GL_UPSLASH || cur == GL_SHELL) return GL_SLAM;
				return GL_POKE;
			}
			case SWITCH_AXE -> {
				if (s.saSword) {
					if (in == InputKind.GUARD_S) return SA_DISCHARGE;
					if (b == InputKind.S) return SA_TO_AXE;
					if (b == InputKind.B) return SA_SW_UP;
					if (cur == SA_SW_DOWN || cur == SA_TO_SWORD) return SA_SW_DOUBLE;
					if (cur == SA_SW_DOUBLE) return SA_SW_UP;
					return SA_SW_DOWN;
				}
				if (in == InputKind.GUARD_S) return SA_RELOAD;
				if (b == InputKind.S) return s.saGauge >= 30 ? SA_TO_SWORD : null;
				if (b == InputKind.B) return SA_AXE_WILD;
				return cur == SA_AXE_DOWN ? SA_AXE_UP : SA_AXE_DOWN;
			}
			case CHARGE_BLADE -> {
				if (s.cbAxe) {
					if (b == InputKind.S) return CB_TO_SWORD;
					if (b == InputKind.B) {
						if (cur == CB_AED1) return CB_AED2;
						if (cur == CB_AED2) return CB_SAED;
						return CB_AED1;
					}
					return cur == CB_AX_RISING ? CB_AX_SLAM : CB_AX_RISING;
				}
				if (in == InputKind.GUARD_A) return CB_TO_AXE;
				if (b == InputKind.S) return CB_CHARGE;
				if (b == InputKind.B) return CB_DASH;
				if (cur == CB_WEAK || cur == CB_DASH) return CB_RETURN;
				if (cur == CB_RETURN) return CB_SPIN;
				return CB_WEAK;
			}
			case INSECT_GLAIVE -> {
				if (b == InputKind.S) return IG_KINSECT;
				if (airborne || cur == IG_VAULT || cur == IG_AERIAL) {
					if (b == InputKind.A) return IG_AERIAL;
					if (b == InputKind.B) return IG_DIVE;
				}
				if (b == InputKind.B) return IG_VAULT;
				if (cur == IG_RISING) return IG_KESA;
				if (cur == IG_KESA) return IG_SPIN;
				return IG_RISING;
			}
			default -> {
				return start(s, in, airborne);
			}
		}
	}

	/** 技を出せるか（ゲージ・弾・クールタイムの消費もここで行う）。 */
	public static boolean canStart(ServerPlayer player, HunterState s, MoveDef m) {
		if (m == null) return false;
		if (m.staminaCost > 0 && s.stamina < Math.min(m.staminaCost * s.staminaCostMul(), 8)) {
			msg(player, "スタミナが足りない");
			return false;
		}
		if (m == GL_SHELL || m == GL_BURST) {
			if (s.shells <= 0) {
				msg(player, "弾切れ！ 右を押したまま R でリロード");
				click(player);
				return false;
			}
		}
		if (m == GL_WYVERN && s.wyvernCooldown > 0) {
			msg(player, "竜撃砲は冷却中…");
			return false;
		}
		if (m == BOW_PIERCER && s.piercerCooldown > 0) {
			msg(player, "竜の一矢は準備中…");
			return false;
		}
		if (m == SA_DISCHARGE && s.saGauge < 30) {
			msg(player, "ビンゲージが足りない");
			return false;
		}
		if (m == LS_HELMBREAKER && s.spiritLevel <= 0) return false;
		if (m == HBG_GATLING && s.gatlingGauge < 10) {
			msg(player, "機関竜弾ゲージ不足");
			return false;
		}
		if (m == DB_DEMON && !s.demonMode && s.stamina < 25) {
			msg(player, "鬼人化するにはスタミナが必要");
			return false;
		}
		return true;
	}

	// =====================================================================
	// 技のライフサイクル
	// =====================================================================
	public static void onStart(ServerPlayer player, HunterState s, MoveDef m) {
		ServerLevel level = (ServerLevel) player.level();
		if (m == LN_MID || m == LN_HIGH || m == GL_POKE) s.chain++;
		if (m == GL_SHELL) s.shells--;
		if (m == GL_BURST) {
			s.scratch = s.shells;
			s.shells = 0;
		}
		if (m == GL_WYVERN) s.wyvernCooldown = 20 * 60;
		if (m == BOW_PIERCER) s.piercerCooldown = 20 * 45;
		if (m == HH_LEFT) s.pushNote(1);
		if (m == HH_RIGHT) s.pushNote(2);
		if (m == HH_BACK) s.pushNote(3);
		if (m == SA_TO_SWORD) s.saSword = true;
		if (m == SA_TO_AXE) s.saSword = false;
		if (m == CB_TO_AXE) s.cbAxe = true;
		if (m == CB_TO_SWORD) s.cbAxe = false;
		if (m == LS_HELMBREAKER) {
			s.spiritLevel = Math.max(0, s.spiritLevel - 1);
		}
		if (m == LS_SPIRIT1 || m == LS_SPIRIT2 || m == LS_SPIRIT3 || m == LS_ROUNDSLASH) {
			float cost = m == LS_ROUNDSLASH ? 20 : 12;
			s.scratch = s.spirit >= cost ? 1 : 0;
			s.spirit = Math.max(0, s.spirit - cost);
		}
		if (m == DB_DEMON) {
			s.demonMode = !s.demonMode;
			Fx.particle(level, new DustParticleOptions(0xE02020, 1.5f), player.position().add(0, 1, 0), 30, 0.5, 0);
		}
		if (m == CB_CHARGE) {
			int add = s.cbEnergy >= 80 ? 5 : (s.cbEnergy >= 50 ? 3 : (s.cbEnergy >= 25 ? 1 : 0));
			s.phials = Math.min(5, Math.max(s.phials, add));
			s.cbEnergy = 0;
			if (add > 0) Fx.playerSound(level, player.position(), "weapon.charge", 1f, 1.2f);
		}
		if (m == IG_VAULT) s.chain = 0;
	}

	public static void onEnd(ServerPlayer player, HunterState s, MoveDef m) {
		if (m == GL_RELOAD) s.shells = 5;
		if (m == SA_RELOAD) s.saGauge = Math.min(100, s.saGauge + 50);
		if (m == SA_DISCHARGE) {
			s.saGauge = 0;
			s.saSword = false;
		}
		if (m == LBG_RELOAD || m == HBG_RELOAD) s.ammo = s.currentAmmo().magazine;
		if (m == CB_SAED) s.phials = 0;
	}

	/** 溜め中の処理（機関竜弾の連射など）。 */
	public static void whileHolding(ServerPlayer player, HunterState s, MoveDef m) {
		if (m == HBG_GATLING) {
			if (s.holdTicks % 3 == 0) {
				WeaponController.fire(player, s, ShotKind.GATLING, 1, 6, 1f);
				s.gatlingGauge -= 2.5f;
				if (s.gatlingGauge <= 0) {
					s.gatlingGauge = 0;
					s.holding = false;
				}
			}
		}
		if (m == LN_CHARGE && s.stamina <= 0) s.holding = false;
	}

	/** 溜めを離した。別の技へ続ける場合はその技を返す。 */
	public static MoveDef onRelease(ServerPlayer player, HunterState s, MoveDef m) {
		if (m == HM_CHARGE) {
			return switch (s.chargeLevel) {
				case 3 -> HM_C3;
				case 2 -> HM_C2;
				default -> HM_C1;
			};
		}
		if (m == BOW_CHARGE) {
			int lv = Math.max(1, s.chargeLevel);
			float mv = switch (lv) {
				case 1 -> 10;
				case 2 -> 12;
				default -> 14;
			};
			WeaponController.fire(player, s, ShotKind.ARROW, lv, mv, m.chargeMultiplier(lv));
			Fx.playerSound((ServerLevel) player.level(), player.position(), "weapon.bow_release", 1f, 1f + lv * 0.08f);
			return null;
		}
		return null;
	}

	/** 毎 tick の技固有処理。 */
	public static void onTick(ServerPlayer player, HunterState s, MoveDef m, int t) {
		ServerLevel level = (ServerLevel) player.level();
		if (m == GL_BURST && t >= 6 && (t - 6) % 3 == 0) {
			int shot = (t - 6) / 3;
			if (shot < s.scratch) {
				blast(player, s, m, 3.4f, 30f, ElementType.FIRE, MoveDef.Shape.ARC, 50f);
			}
		}
		if (m == CB_AED1 && t == 9) phialBurst(player, s, m, 1, 26f);
		if (m == CB_AED2 && t == 15) phialBurst(player, s, m, 1, 26f);
		if (m == CB_SAED && t == 24) {
			int n = s.phials;
			for (int i = 0; i < n; i++) {
				blast(player, s, m, 7.5f, 34f, ElementType.NONE, MoveDef.Shape.LINE, 2.4f);
			}
			if (n > 0) {
				Fx.playerSound(level, player.position(), "weapon.phial_burst", 2f, 0.8f);
				Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, player.position().add(player.getLookAngle().scale(4)), 1, 0, 0);
			}
		}
		if (m == IG_KINSECT && t == 3) {
			Kinsect.launch(level, player);
			s.kinsectOut = true;
		}
		if (m == HH_RECITAL && t == 30) playSong(player, s);
		if (m == BOW_PIERCER && t >= 36 && t <= 46 && (t - 36) % 2 == 0) {
			WeaponController.rayAttack(player, s, 40f, 1.0f, 12f, 1.3f);
		}
		if (m == LN_CHARGE) {
			s.spendStamina(0.2f);
			if (s.stamina <= 0) {
				WeaponController.interrupt(player, s);
			}
		}
		if (m == GL_WYVERN && t < 34 && t % 4 == 0) {
			Fx.particle(level, ParticleTypes.FLAME, player.position().add(player.getLookAngle().scale(2)).add(0, 1.2, 0), 8, 0.3, 0.05);
		}
	}

	public static boolean customHits(MoveDef m) {
		return m == BOW_PIERCER;
	}

	private static void blast(ServerPlayer player, HunterState s, MoveDef m, float range, float dmg, ElementType el,
							  MoveDef.Shape shape, float angle) {
		MoveDef.Hit h = new MoveDef.Hit(0, 1, 0, shape, range, angle, 1.2f, 0f, 0f, 0f, 0f, dmg,
			el == ElementType.NONE ? s.element : el, null, 0f);
		WeaponController.doHit(player, s, m, h);
		Fx.playerSound((ServerLevel) player.level(), player.position(), "weapon.gunlance_shell", 1.2f, 0.9f);
	}

	private static void phialBurst(ServerPlayer player, HunterState s, MoveDef m, int use, float dmg) {
		if (s.phials <= 0) return;
		s.phials = Math.max(0, s.phials - use);
		blast(player, s, m, 4.2f, dmg, ElementType.NONE, MoveDef.Shape.ARC, 50f);
		Fx.playerSound((ServerLevel) player.level(), player.position(), "weapon.phial_burst", 1.4f, 1.1f);
	}

	/** 狩猟笛の旋律効果。 */
	private static void playSong(ServerPlayer player, HunterState s) {
		ServerLevel level = (ServerLevel) player.level();
		int n = s.noteCount;
		int a = n >= 1 ? s.notes[n - 1] : 0;
		int b = n >= 2 ? s.notes[n - 2] : 0;
		int c = n >= 3 ? s.notes[n - 3] : 0;
		String name;
		int dur = 20 * 120;
		java.util.function.Consumer<HunterState> effect;
		if (c == 1 && b == 2 && a == 3) {
			name = "攻撃力UP【大】";
			effect = h -> h.hornAttack = dur;
		} else if (c == 2 && b == 2 && a == 3) {
			name = "防御力UP【大】";
			effect = h -> h.hornDefense = dur;
		} else if (c == 2 && b == 1 && a == 2) {
			name = "属性攻撃強化";
			effect = h -> h.hornElement = dur;
		} else if (c == 3 && b == 1 && a == 3) {
			name = "聴覚保護【大】";
			effect = h -> h.hornEarplug = dur;
		} else if (b == 1 && a == 1) {
			name = "自分強化";
			effect = null;
			s.hornSelf = dur;
		} else if (b == 3 && a == 3) {
			name = "体力回復【中】";
			effect = h -> {
			};
			for (ServerPlayer p : level.players()) {
				if (p.distanceToSqr(player) < 20 * 20) p.heal(30 * HunterManager.state(p).healMul());
			}
		} else if (n > 0) {
			name = "体力回復【小】";
			effect = h -> {
			};
			for (ServerPlayer p : level.players()) {
				if (p.distanceToSqr(player) < 20 * 20) p.heal(12);
			}
		} else {
			name = null;
			effect = null;
		}
		s.noteCount = 0;
		if (name == null) return;
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(player) < 20 * 20) {
				if (effect != null) effect.accept(HunterManager.state(p));
				Net.send(p, new Payloads.Banner(Payloads.Banner.SMALL, "♪ " + name, player.getName().getString() + " の演奏", 0x80E0FF));
				Fx.playerSound(level, p.position(), "player.buff", 0.8f, 1f);
			}
		}
		Fx.particle(level, ParticleTypes.NOTE, player.position().add(0, 2, 0), 20, 1.5, 1.0);
	}

	// =====================================================================
	// 命中時・常時
	// =====================================================================
	public static void onHit(ServerPlayer player, HunterState s, MoveDef m, DamageCalc.Result r, HuntMonster mon, int part) {
		switch (s.weapon) {
			case LONG_SWORD -> {
				boolean spiritMove = m == LS_SPIRIT1 || m == LS_SPIRIT2 || m == LS_SPIRIT3 || m == LS_ROUNDSLASH || m == LS_HELMBREAKER;
				if (!spiritMove) s.spirit = Math.min(100, s.spirit + 7);
				if (m == LS_ROUNDSLASH && s.scratch == 1) {
					s.spiritLevel = Math.min(3, s.spiritLevel + 1);
					s.spiritLevelTimer = 20 * 60;
					s.scratch = 0;
					Fx.playerSound((ServerLevel) player.level(), player.position(), "weapon.spirit", 1.2f, 1.2f + s.spiritLevel * 0.1f);
				}
			}
			case CHARGE_BLADE -> {
				if (!s.cbAxe) s.cbEnergy = Math.min(100, s.cbEnergy + 6);
			}
			case SWITCH_AXE -> {
				if (s.saSword) s.saGauge = Math.max(0, s.saGauge - 3);
				else s.saGauge = Math.min(100, s.saGauge + 4);
			}
			case INSECT_GLAIVE -> {
				if (m == IG_AERIAL) {
					Net.send(player, new Payloads.Knockback(0, 0.55, 0, false));
				}
			}
			default -> {
			}
		}
	}

	public static void passiveTick(ServerPlayer player, HunterState s) {
		if (s.wyvernCooldown > 0) s.wyvernCooldown--;
		if (s.piercerCooldown > 0) s.piercerCooldown--;
		if (s.move != HBG_GATLING && s.gatlingGauge < 100) s.gatlingGauge = Math.min(100, s.gatlingGauge + 0.2f);
		if (s.weapon == WeaponType.LONG_SWORD) {
			if (s.spiritLevel > 0 && --s.spiritLevelTimer <= 0) {
				s.spiritLevel--;
				s.spiritLevelTimer = 20 * 60;
			}
			if (s.move == null && s.spirit > 0) s.spirit = Math.max(0, s.spirit - 0.03f);
		}
		if (s.weapon == WeaponType.SWITCH_AXE) {
			if (s.saSword) {
				s.saGauge -= 0.04f;
				if (s.saGauge <= 0) {
					s.saGauge = 0;
					s.saSword = false;
					msg(player, "ビンゲージが尽きて斧モードに戻った");
				}
			} else {
				s.saGauge = Math.min(100, s.saGauge + 0.06f);
			}
		}
		if (s.weapon != WeaponType.DUAL_BLADES) s.demonMode = false;
	}

	public static float damageMul(HunterState s, MoveDef m) {
		float mul = 1f;
		if ((m == LS_SPIRIT1 || m == LS_SPIRIT2 || m == LS_SPIRIT3 || m == LS_ROUNDSLASH) && s.scratch == 0) mul *= 0.7f;
		if (m == LN_COUNTER && s.counterSuccess) mul *= 1.6f;
		if (m == LS_FORESIGHT && s.counterSuccess) mul *= 1.5f;
		if (s.weapon == WeaponType.CHARGE_BLADE && !s.cbAxe && s.phials >= 5) mul *= 1.05f;
		return mul;
	}

	public static float fixedMul(HunterState s, MoveDef m) {
		return 1f + (s.stats.attack() - 200) / 400f;
	}

	private static void msg(ServerPlayer player, String text) {
		player.sendOverlayMessage(Component.literal(text));
	}

	private static void click(ServerPlayer player) {
		Fx.playerSound((ServerLevel) player.level(), player.position(), "ui.error", 0.6f, 1f);
	}

	/** ボウガンの弾種切替（右クリックを押したまま R）。 */
	public static void switchAmmo(ServerPlayer player, HunterState s) {
		s.ammoType = (s.ammoType + 1) % ShotKind.BOWGUN_AMMO.length;
		s.ammo = 0;
		msg(player, "弾種: " + s.currentAmmo().jaName + "（リロードしてください）");
		Fx.playerSound((ServerLevel) player.level(), player.position(), "weapon.reload", 0.6f, 1.4f);
	}

	/** カウンター成功（見切り斬り・ランスのカウンター）。 */
	public static void onCounter(ServerPlayer player, HunterState s) {
		s.counterSuccess = true;
		if (s.weapon == WeaponType.LONG_SWORD) {
			s.spirit = 100;
		}
		Fx.playerSound((ServerLevel) player.level(), player.position(), "weapon.spirit", 1.4f, 1.5f);
		Fx.particle((ServerLevel) player.level(), ParticleTypes.ENCHANTED_HIT, player.position().add(0, 1, 0), 20, 0.6, 0.3);
	}
}
