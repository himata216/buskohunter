package com.buskohunter.hunter;

import com.buskohunter.net.Net;
import com.buskohunter.BuskoHunter;
import com.buskohunter.combat.MoveRegistry;
import com.buskohunter.core.ArmorSkill;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.Meal;
import com.buskohunter.core.WeaponType;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.monster.MonsterMove;
import com.buskohunter.net.Payloads;
import com.buskohunter.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** モンスター → ハンターのダメージ（回避の無敵・ガード・防御・属性やられ・吹っ飛び）。 */
public final class HunterCombat {
	private HunterCombat() {
	}

	public static final ResourceKey<DamageType> MONSTER_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, BuskoHunter.id("monster"));
	private static final Random RNG = new Random();

	public static DamageSource source(ServerLevel level, Entity attacker) {
		var holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(MONSTER_DAMAGE);
		return attacker == null ? new DamageSource(holder) : new DamageSource(holder, attacker);
	}

	public static void monsterHitsPlayer(ServerLevel level, HuntMonster monster, ServerPlayer player, float damage,
										 ElementType element, MonsterMove.Knock knock, Vec3 from) {
		if (player.isCreative() || player.isSpectator() || !player.isAlive()) return;
		HunterState s = HunterManager.state(player);

		// カウンター（見切り斬り・ランスのカウンター突き）
		if (s.move != null && s.move.counterFrom >= 0 && s.moveTick >= s.move.counterFrom && s.moveTick <= s.move.counterTo) {
			WeaponBehaviors.onCounter(player, s);
			return;
		}
		// 回避の無敵
		if (s.iframes > 0) {
			Fx.particle(level, ParticleTypes.CLOUD, player.position().add(0, 1, 0), 4, 0.3, 0.02);
			return;
		}

		Vec3 toSource = from.subtract(player.position());
		Vec3 flat = new Vec3(toSource.x, 0, toSource.z);
		Vec3 away = flat.lengthSqr() < 1e-4 ? player.getLookAngle().scale(-1) : flat.normalize().scale(-1);

		// ガード
		if (s.guarding()) {
			Vec3 look = player.getLookAngle();
			double facing = look.x * -away.x + look.z * -away.z;
			if (facing > 0.1) {
				boolean lance = s.weapon == WeaponType.LANCE || s.weapon == WeaponType.GUNLANCE;
				float chip = damage * s.defenseMultiplier() * (lance ? 0.08f : 0.25f);
				s.spendStamina(lance ? 10 : 18);
				hurt(level, player, monster, chip);
				Fx.playerSound(level, player.position(), "weapon.guard", 1.4f, 0.9f + RNG.nextFloat() * 0.2f);
				Fx.particle(level, ParticleTypes.CRIT, player.position().add(look).add(0, 1.2, 0), 10, 0.3, 0.3);
				double push = knock == MonsterMove.Knock.LARGE ? 0.5 : 0.25;
				if (s.stamina <= 0) push = 0.9;
				Net.send(player, new Payloads.Knockback(away.x * push, 0.05, away.z * push, false));
				if (s.move == null) WeaponController.begin(player, s, MoveRegistry.GUARD_HIT, 0);
				return;
			}
		}

		// 防御・耐性
		float dmg = damage * s.defenseMultiplier();
		int res = s.armor.resistance(element) + (s.meal.resistElement == element ? s.meal.resistValue : 0);
		dmg *= Math.max(0.3f, 1f - res / 100f);
		if (s.has(ArmorSkill.DIVINE_BLESSING) && RNG.nextInt(4) == 0) dmg *= 0.5f;
		if (s.move != null && s.move.hyperArmor) dmg *= 0.85f;
		hurt(level, player, monster, dmg);
		s.redHealth = Math.min(s.redHealth + dmg * 0.45f, 60f);
		Fx.playerSound(level, player.position(), "player.hurt", 1f, 0.9f + RNG.nextFloat() * 0.2f);

		// 属性やられ
		int blight = 20 * 25;
		switch (element) {
			case FIRE -> s.fireblight = blight;
			case WATER -> s.waterblight = blight;
			case THUNDER -> {
				if (s.thunderblight > 0 && RNG.nextInt(3) == 0) s.stun = 60;
				s.thunderblight = blight;
			}
			case ICE -> s.iceblight = blight;
			case DRAGON -> s.dragonblight = blight;
			default -> {
			}
		}

		// のけぞり・吹っ飛び
		boolean armor = s.move != null && s.move.hyperArmor && knock != MonsterMove.Knock.LAUNCH;
		double h, up;
		switch (knock) {
			case SMALL -> {
				h = 0.35;
				up = 0.15;
				if (!armor) HunterManager.forceMove(player, MoveRegistry.STAGGER_MOVE);
			}
			case LARGE -> {
				h = 0.95;
				up = 0.45;
				if (!armor) knockdown(player, s);
			}
			case LAUNCH -> {
				h = 0.35;
				up = 1.05;
				knockdown(player, s);
			}
			default -> {
				h = 0.6;
				up = 0.1;
			}
		}
		if (armor) {
			h *= 0.2;
			up *= 0.2;
		}
		Net.send(player, new Payloads.Knockback(away.x * h, up, away.z * h, false));
	}

	private static void knockdown(ServerPlayer player, HunterState s) {
		int dur = s.meal.skill == Meal.MealSkill.FAST_GETUP ? 14 : 24;
		s.knockdown = dur;
		HunterManager.forceMove(player, MoveRegistry.KNOCKDOWN);
	}

	private static void hurt(ServerLevel level, ServerPlayer player, Entity attacker, float amount) {
		if (amount <= 0) return;
		player.setInvulnerableTime(0);
		player.hurtServer(level, source(level, attacker), amount);
	}

	/** 毒・火やられなどの継続ダメージ。 */
	public static void dotDamage(ServerLevel level, ServerPlayer player, float amount) {
		if (player.isCreative() || player.isSpectator()) return;
		if (player.getHealth() <= amount + 1) return;
		player.setInvulnerableTime(0);
		player.hurtServer(level, source(level, null), amount);
	}
}
