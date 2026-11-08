package com.buskohunter.hunter;

import com.buskohunter.combat.AnimKind;
import com.buskohunter.combat.DamageCalc;
import com.buskohunter.combat.InputKind;
import com.buskohunter.combat.MoveDef;
import com.buskohunter.combat.MoveRegistry;
import com.buskohunter.combat.ShotKind;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.WeaponType;
import com.buskohunter.entity.HunterShot;
import com.buskohunter.entity.TrainingDummy;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.net.Payloads;
import com.buskohunter.util.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * プレイヤーの技の実行（サーバー権威）。入力 → 技の開始・派生・溜め・攻撃判定・射撃。
 */
public final class WeaponController {
	private WeaponController() {
	}

	private static final Random RNG = new Random();
	private static final int BUFFER_TICKS = 8;

	// =====================================================================
	// 入力
	// =====================================================================
	public static void handleInput(ServerPlayer player, HunterState s, int bits) {
		int prev = s.inputBits;
		s.prevInputBits = prev;
		s.inputBits = bits;
		if (s.incapacitated()) return;

		boolean guard = (bits & InputKind.HELD_GUARD) != 0;
		if ((bits & InputKind.PRESS_DODGE) != 0) {
			tryDodge(player, s, bits);
			return;
		}
		InputKind in = null;
		if ((bits & InputKind.PRESS_A) != 0) in = guard ? InputKind.GUARD_A : InputKind.A;
		else if ((bits & InputKind.PRESS_B) != 0) in = guard ? InputKind.GUARD_B : InputKind.B;
		else if ((bits & InputKind.PRESS_S) != 0) in = guard ? InputKind.GUARD_S : InputKind.S;
		if (in != null) {
			if (!tryInput(player, s, in)) {
				s.buffered = in;
				s.bufferTicks = BUFFER_TICKS;
			}
		}
		// 溜めの解放
		if (s.move != null && s.holding && s.move.chargeable()) {
			int bit = s.move.holdButton.heldBit();
			if ((bits & bit) == 0) {
				release(player, s);
			}
		}
	}

	private static boolean canAct(ServerPlayer player, HunterState s) {
		return player.getMainHandItem().getItem() instanceof com.buskohunter.item.HunterWeaponItem
			&& !player.isUsingItem() && s.knockdown <= 0;
	}

	private static boolean tryInput(ServerPlayer player, HunterState s, InputKind in) {
		if (!canAct(player, s)) return false;
		if (in == InputKind.GUARD_B && (s.weapon == WeaponType.LIGHT_BOWGUN || s.weapon == WeaponType.HEAVY_BOWGUN)) {
			if (s.move == null || s.moveTick >= s.move.cancelFrom) {
				WeaponBehaviors.switchAmmo(player, s);
				return true;
			}
			return false;
		}
		MoveDef next;
		if (s.move == null) {
			next = WeaponBehaviors.start(s, in, (s.inputBits & InputKind.AIRBORNE) != 0);
		} else {
			if (s.holding || s.moveTick < s.move.cancelFrom) return false;
			next = WeaponBehaviors.combo(s, s.move, in, (s.inputBits & InputKind.AIRBORNE) != 0);
		}
		if (next == null) return s.move == null;
		if (!WeaponBehaviors.canStart(player, s, next)) return true;
		begin(player, s, next, 0);
		return true;
	}

	private static void tryDodge(ServerPlayer player, HunterState s, int bits) {
		if (s.move != null && !s.holding && s.moveTick < s.move.cancelFrom) {
			return;
		}
		MoveDef dodge = MoveRegistry.dodgeFor(s.weapon, s.demonMode);
		if (s.stamina < Math.min(10, dodge.staminaCost * s.staminaCostMul())) return;
		interrupt(player, s);
		int dir = bits & (InputKind.DIR_FORWARD | InputKind.DIR_BACK | InputKind.DIR_LEFT | InputKind.DIR_RIGHT);
		begin(player, s, dodge, dir);
		s.iframes = (dodge.iTo - dodge.iFrom) + s.iframeBonus() + 1;
		// 消火（火やられは転がると早く治る）
		if (s.fireblight > 0) s.fireblight = Math.max(0, s.fireblight - 80);
	}

	// =====================================================================
	// 技の開始・中断
	// =====================================================================
	public static void begin(ServerPlayer player, HunterState s, MoveDef move, int dir) {
		s.move = move;
		s.moveTick = 0;
		s.holdTicks = 0;
		s.chargeLevel = 0;
		s.holding = move.chargeable() && (s.inputBits & move.holdButton.heldBit()) != 0;
		s.counterSuccess = false;
		s.buffered = null;
		if (move.staminaCost > 0) s.spendStamina(move.staminaCost);
		WeaponBehaviors.onStart(player, s, move);
		HunterManager.broadcastMove(player, new Payloads.MoveEvent(player.getId(), move.netId, Payloads.MoveEvent.START, 0, dir));
		if (!s.holding && move.chargeable()) {
			// 押してすぐ離した：溜めずにそのまま出す
			HunterManager.broadcastMove(player, new Payloads.MoveEvent(player.getId(), move.netId, Payloads.MoveEvent.RELEASE, 0, 0));
		}
	}

	public static void interrupt(ServerPlayer player, HunterState s) {
		if (s.move == null) return;
		s.move = null;
		s.holding = false;
		HunterManager.broadcastMove(player, new Payloads.MoveEvent(player.getId(), -1, Payloads.MoveEvent.STOP, 0, 0));
	}

	private static void release(ServerPlayer player, HunterState s) {
		if (!s.holding) return;
		s.holding = false;
		MoveDef m = s.move;
		HunterManager.broadcastMove(player, new Payloads.MoveEvent(player.getId(), m.netId, Payloads.MoveEvent.RELEASE, s.chargeLevel, 0));
		MoveDef follow = WeaponBehaviors.onRelease(player, s, m);
		if (follow != null && follow != m) {
			int level = s.chargeLevel;
			begin(player, s, follow, 0);
			s.chargeLevel = level;
		}
	}

	// =====================================================================
	// 毎 tick
	// =====================================================================
	public static void tick(ServerPlayer player, HunterState s) {
		if (s.bufferTicks > 0) s.bufferTicks--;
		if (s.move == null) {
			if (s.buffered != null && s.bufferTicks > 0) {
				InputKind in = s.buffered;
				s.buffered = null;
				tryInput(player, s, in);
			}
			return;
		}
		MoveDef m = s.move;
		ServerLevel level = (ServerLevel) player.level();

		// 溜め
		if (s.holding && m.chargeable() && s.moveTick >= m.holdTick) {
			s.holdTicks++;
			if (m.staminaPerTick > 0) s.spendStamina(m.staminaPerTick);
			int lv = m.chargeLevelFor(s.holdTicks);
			if (lv != s.chargeLevel) {
				s.chargeLevel = lv;
				Fx.playerSound(level, player.position(), "weapon.charge", 0.9f, 0.8f + lv * 0.25f);
				Fx.particle(level, new DustParticleOptions(lv >= 3 ? 0xFF4020 : 0xFFD040, 1.2f),
					player.position().add(0, 1.2, 0), 10 + lv * 6, 0.5, 0);
				HunterManager.broadcastMove(player, new Payloads.MoveEvent(player.getId(), m.netId, Payloads.MoveEvent.HOLD, lv, 0));
			}
			WeaponBehaviors.whileHolding(player, s, m);
			if (s.holdTicks >= m.maxHold || (m.staminaPerTick > 0 && s.stamina <= 0)) {
				release(player, s);
			}
			return;
		}

		int t = s.moveTick;
		// 無敵・カウンター
		if (m.iFrom >= 0 && t >= m.iFrom && t <= m.iTo) s.iframes = Math.max(s.iframes, 1);
		// 効果音
		if (m.sound != null && t == m.soundTick) {
			Fx.playerSound(level, player.position(), m.sound, 1f, 0.9f + RNG.nextFloat() * 0.2f);
		}
		// 攻撃判定
		if (!WeaponBehaviors.customHits(m)) {
			for (MoveDef.Hit h : m.hits) {
				for (int r = 0; r < Math.max(1, h.repeat()); r++) {
					if (t == h.tick() + r * h.interval()) {
						doHit(player, s, m, h);
					}
				}
			}
		}
		// 射撃
		for (MoveDef.Shot shot : m.shots) {
			if (t == shot.tick()) fire(player, s, shot.kind(), shot.count(), shot.mv(), 1f);
		}
		WeaponBehaviors.onTick(player, s, m, t);

		s.moveTick++;
		if (s.move == m && s.moveTick >= m.duration) {
			s.move = null;
			WeaponBehaviors.onEnd(player, s, m);
		}
	}

	// =====================================================================
	// 近接の攻撃判定
	// =====================================================================
	public static void doHit(ServerPlayer player, HunterState s, MoveDef m, MoveDef.Hit h) {
		ServerLevel level = (ServerLevel) player.level();
		float yaw = player.getYRot() * Mth.DEG_TO_RAD;
		Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 origin = player.position();
		double hitY = player.getY() + heightFor(m, h);
		double reach = h.range() + 4;
		AABB search = player.getBoundingBox().inflate(reach, 3, reach);
		boolean any = false;

		// スウィングの軌跡
		Vec3 tip = origin.add(fwd.scale(Math.min(h.range(), 3.5))).add(0, heightFor(m, h), 0);
		if (h.fixedDamage() > 0) {
			Fx.particle(level, ParticleTypes.EXPLOSION, tip, 1, 0.2, 0);
			Fx.particle(level, ParticleTypes.FLAME, tip, 12, 0.6, 0.05);
		} else {
			Fx.particle(level, ParticleTypes.SWEEP_ATTACK, tip, 1, 0, 0);
		}

		for (Entity e : level.getEntities(player, search, en -> en instanceof HuntMonster || en instanceof TrainingDummy)) {
			AABB box = e.getBoundingBox();
			Vec3 aim = new Vec3(origin.x + fwd.x * Math.min(h.range() * 0.6, 2.5), hitY, origin.z + fwd.z * Math.min(h.range() * 0.6, 2.5));
			Vec3 hp = closest(box, aim);
			if (!inShape(origin, fwd, hp, h, hitY)) continue;
			any = true;
			if (e instanceof HuntMonster mon) {
				hitMonster(level, player, s, m, h, mon, hp);
			} else if (e instanceof TrainingDummy dummy) {
				hitDummy(level, player, s, m, h, dummy, hp);
			}
		}
		if (any && h.fixedDamage() <= 0 && !s.weapon.ranged) {
			s.sharpness = Math.max(0, s.sharpness - Math.max(0, Math.round(h.sharpnessCost())));
		}
	}

	private static double heightFor(MoveDef m, MoveDef.Hit h) {
		if (h.height() != 1.2f) return h.height();
		return switch (m.anim) {
			case SLASH_UP, UPPERCUT, SHIELD_UPPER, HOMERUN -> 2.6;
			case THRUST_HIGH -> 2.4;
			case AERIAL_SLASH, HELM_BREAKER, VAULT -> 2.2;
			case SLAM, CHARGE_OVERHEAD, SUPER_DISCHARGE -> 1.4;
			default -> 1.3;
		};
	}

	private static boolean inShape(Vec3 origin, Vec3 fwd, Vec3 p, MoveDef.Hit h, double hitY) {
		double dx = p.x - origin.x, dz = p.z - origin.z;
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (Math.abs(p.y - hitY) > 3.2) return false;
		switch (h.shape()) {
			case CIRCLE:
				return dist <= h.range();
			case LINE: {
				double along = dx * fwd.x + dz * fwd.z;
				double lateral = Math.abs(dx * fwd.z - dz * fwd.x);
				return along >= -0.5 && along <= h.range() && lateral <= h.angle() * 0.5 + 0.6;
			}
			case ARC:
			default: {
				if (dist > h.range()) return false;
				if (dist < 1.2) return true;
				double cos = (dx * fwd.x + dz * fwd.z) / dist;
				return cos >= Math.cos(Math.toRadians(h.angle()));
			}
		}
	}

	private static Vec3 closest(AABB box, Vec3 p) {
		return new Vec3(Mth.clamp(p.x, box.minX, box.maxX), Mth.clamp(p.y, box.minY, box.maxY), Mth.clamp(p.z, box.minZ, box.maxZ));
	}

	public static void hitMonster(ServerLevel level, ServerPlayer player, HunterState s, MoveDef m, MoveDef.Hit h,
								  HuntMonster mon, Vec3 at) {
		int part = mon.partAt(at);
		DamageCalc.Result r;
		if (h.fixedDamage() > 0) {
			ElementType el = h.fixedElement() == null ? s.element : h.fixedElement();
			r = DamageCalc.fixed(s, mon.targetInfo(part), h.fixedDamage() * WeaponBehaviors.fixedMul(s, m), el);
		} else {
			WeaponType.DamageKind kind = h.kindOverride() != null ? h.kindOverride() : s.weapon.damageKind;
			float extra = m.chargeMultiplier(s.chargeLevel) * WeaponBehaviors.damageMul(s, m);
			r = DamageCalc.compute(s, mon.targetInfo(part), h.mv(), h.elementMul(), h.statusMul(), h.ko(), kind, extra, RNG);
		}
		mon.receiveHit(level, player, part, r, s);
		mon.sendDamageNumber(player, at, r);
		hitFx(level, player, s, at, r, h);
		WeaponBehaviors.onHit(player, s, m, r, mon, part);
	}

	private static void hitDummy(ServerLevel level, ServerPlayer player, HunterState s, MoveDef m, MoveDef.Hit h,
								 TrainingDummy dummy, Vec3 at) {
		DamageCalc.Result r;
		if (h.fixedDamage() > 0) {
			r = DamageCalc.fixed(s, dummy.targetInfo(), h.fixedDamage() * WeaponBehaviors.fixedMul(s, m), h.fixedElement());
		} else {
			WeaponType.DamageKind kind = h.kindOverride() != null ? h.kindOverride() : s.weapon.damageKind;
			float extra = m.chargeMultiplier(s.chargeLevel) * WeaponBehaviors.damageMul(s, m);
			r = DamageCalc.compute(s, dummy.targetInfo(), h.mv(), h.elementMul(), h.statusMul(), h.ko(), kind, extra, RNG);
		}
		dummy.onHit(level, r);
		com.buskohunter.net.Net.send(player,
			new Payloads.DamageNumber(at.x, at.y, at.z, Math.max(1, Math.round(r.total())),
				(r.crit() ? Payloads.DamageNumber.CRIT : 0) | (r.bounce() ? Payloads.DamageNumber.BOUNCE : 0)));
		hitFx(level, player, s, at, r, h);
		WeaponBehaviors.onHit(player, s, m, r, null, 0);
	}

	public static void hitFx(ServerLevel level, ServerPlayer player, HunterState s, Vec3 at, DamageCalc.Result r, MoveDef.Hit h) {
		String snd;
		if (r.bounce()) snd = "weapon.hit_weak";
		else if (h != null && h.fixedDamage() > 0) snd = "weapon.gunlance_shell";
		else if (s.weapon.damageKind == WeaponType.DamageKind.BLUNT || (h != null && h.kindOverride() == WeaponType.DamageKind.BLUNT)) snd = "weapon.hit_blunt";
		else if (s.weapon.damageKind == WeaponType.DamageKind.SHOT || (h != null && h.shape() == MoveDef.Shape.LINE)) snd = "weapon.hit_pierce";
		else snd = "weapon.hit_slash";
		Fx.playerSound(level, at, snd, 1f, 0.9f + RNG.nextFloat() * 0.2f);
		if (r.crit()) Fx.playerSound(level, at, "weapon.hit_crit", 0.8f, 1f);
		Fx.particle(level, r.crit() ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT, at, 8, 0.25, 0.3);
		if (r.element() > 0) {
			Fx.particle(level, new DustParticleOptions(s.element.color, 1.4f), at, 8, 0.4, 0);
			String es = switch (s.element) {
				case FIRE -> "element.fire";
				case WATER -> "element.water";
				case THUNDER -> "element.thunder";
				case ICE -> "element.ice";
				case DRAGON -> "element.dragon";
				default -> null;
			};
			if (es != null && RNG.nextInt(2) == 0) Fx.playerSound(level, at, es, 0.6f, 1f);
		}
	}

	// =====================================================================
	// 射撃
	// =====================================================================
	public static void fire(ServerPlayer player, HunterState s, ShotKind kind, int count, float mv, float extraMul) {
		ServerLevel level = (ServerLevel) player.level();
		ShotKind actual = kind;
		if (s.weapon == WeaponType.LIGHT_BOWGUN || s.weapon == WeaponType.HEAVY_BOWGUN) {
			if (kind != ShotKind.GATLING) {
				actual = s.currentAmmo();
				if (s.ammo <= 0) {
					Fx.playerSound(level, player.position(), "ui.error", 0.8f, 1f);
					player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("弾切れ！ R でリロード"));
					return;
				}
				s.ammo--;
			}
		}
		int pellets = Math.max(1, actual.pellets);
		Vec3 look = player.getLookAngle();
		for (int i = 0; i < count; i++) {
			for (int p = 0; p < pellets; p++) {
				HunterShot shot = HunterShot.create(level, player, actual, mv, extraMul * actual.damageMul);
				Vec3 eye = player.getEyePosition();
				shot.setPos(eye.x, eye.y - 0.1, eye.z);
				float spread = actual.spread + (count > 1 ? 4f * (i - (count - 1) / 2f) : 0);
				float yawOff = pellets > 1 ? (p - (pellets - 1) / 2f) * actual.spread : (count > 1 ? 4f * (i - (count - 1) / 2f) : 0);
				Vec3 dir = rotateYaw(look, yawOff * Mth.DEG_TO_RAD);
				shot.shoot(dir.x, dir.y, dir.z, actual.speed, pellets > 1 ? 1.5f : (spread > 0 ? 1.0f : 0.2f));
				level.addFreshEntity(shot);
			}
		}
		String snd = switch (s.weapon) {
			case HEAVY_BOWGUN -> kind == ShotKind.GATLING ? "weapon.bowgun_gatling" : "weapon.bowgun_heavy";
			case BOW -> "weapon.bow_release";
			default -> "weapon.bowgun_light";
		};
		Fx.playerSound(level, player.position(), snd, 1f, 0.95f + RNG.nextFloat() * 0.1f);
	}

	private static Vec3 rotateYaw(Vec3 v, double a) {
		double c = Math.cos(a), sn = Math.sin(a);
		return new Vec3(v.x * c - v.z * sn, v.y, v.x * sn + v.z * c);
	}

	/** 竜の一矢などの貫通レイ攻撃。 */
	public static void rayAttack(ServerPlayer player, HunterState s, float length, float width, float mv, float elementMul) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(length));
		Fx.line(level, new DustParticleOptions(s.element.color, 2.0f), eye, end, 0.5);
		Fx.line(level, ParticleTypes.END_ROD, eye, end, 1.5);
		AABB search = new AABB(eye, end).inflate(width + 4);
		for (Entity e : level.getEntities(player, search, en -> en instanceof HuntMonster || en instanceof TrainingDummy)) {
			AABB box = e.getBoundingBox().inflate(width);
			var clip = box.clip(eye, end);
			if (clip.isEmpty() && !box.contains(eye)) continue;
			Vec3 at = clip.orElse(e.position());
			MoveDef.Hit fake = new MoveDef.Hit(0, 1, 0, MoveDef.Shape.LINE, length, width, 1.2f, mv, elementMul, 1f, 0f, 0f, null, null, 0f);
			if (e instanceof HuntMonster mon) {
				hitMonster(level, player, s, s.move != null ? s.move : MoveRegistry.BOW_PIERCER, fake, mon, at);
			} else if (e instanceof TrainingDummy d) {
				hitDummy(level, player, s, MoveRegistry.BOW_PIERCER, fake, d, at);
			}
		}
	}

	/** 現在の技が AnimKind のものか。 */
	public static boolean isMove(HunterState s, AnimKind anim) {
		return s.move != null && s.move.anim == anim;
	}
}
