package com.buskohunter.entity;

import com.buskohunter.hunter.HunterCombat;
import com.buskohunter.hunter.HunterManager;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.core.ArmorSkill;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.monster.MonsterMove;
import com.buskohunter.monster.MonsterProjectileKind;
import com.buskohunter.registry.ModEntities;
import com.buskohunter.registry.ModItems;
import com.buskohunter.util.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** モンスターの飛び道具（酒瓶・火球・水球・ミサイル・聖光球・グリッチキューブ・ソンブレロ）。 */
public class MonsterProjectile extends ThrowableItemProjectile {
	private MonsterProjectileKind kind = MonsterProjectileKind.FIREBALL;
	private float damage = 20;
	private UUID homingTarget;
	private int life;
	private boolean returning;

	public MonsterProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
		super(type, level);
	}

	public static MonsterProjectile create(ServerLevel level, HuntMonster owner, MonsterProjectileKind kind, float damage, ServerPlayer target) {
		MonsterProjectile p = new MonsterProjectile(ModEntities.MONSTER_PROJECTILE, level);
		p.setOwner(owner);
		p.kind = kind;
		p.damage = damage;
		p.homingTarget = target == null ? null : target.getUUID();
		p.setItem(new ItemStack(ModItems.projectile(kind.itemId)));
		p.setNoGravity(kind.gravity <= 0);
		return p;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.projectile("proj_fireball");
	}

	@Override
	protected double getDefaultGravity() {
		return kind == null ? 0.03 : Math.max(0.0, kind.gravity);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) return;
		life++;
		if (life > 160) {
			discard();
			return;
		}
		MonsterMove.Fx fx = switch (kind) {
			case FIREBALL -> MonsterMove.Fx.FIRE;
			case WATER_ORB -> MonsterMove.Fx.WATER;
			case MISSILE -> MonsterMove.Fx.NEON;
			case HOLY_ORB -> MonsterMove.Fx.HOLY;
			case GLITCH_CUBE -> MonsterMove.Fx.GLITCH;
			case SAKE_BOTTLE -> MonsterMove.Fx.SAKE;
			case SOMBRERO -> MonsterMove.Fx.HOLY;
		};
		if (life % 2 == 0) Fx.burst(level, fx, position(), 1, 0.1);
		// 追尾
		if (kind.homing && homingTarget != null && life > 8) {
			var t = level.getPlayerByUUID(homingTarget);
			if (t != null) {
				Vec3 want = t.position().add(0, 1, 0).subtract(position()).normalize().scale(getDeltaMovement().length());
				setDeltaMovement(getDeltaMovement().scale(0.9).add(want.scale(0.1)));
			}
		}
		// ソンブレロはブーメランのように戻る
		if (kind == MonsterProjectileKind.SOMBRERO) {
			if (life == 22) returning = true;
			if (returning && getOwner() != null) {
				Vec3 to = getOwner().position().add(0, 3, 0).subtract(position());
				if (to.length() < 2.5) {
					discard();
					return;
				}
				setDeltaMovement(to.normalize().scale(0.9));
			}
			hitPlayersAround(level, 1.6, false);
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return entity instanceof ServerPlayer && super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!(level() instanceof ServerLevel level)) return;
		if (kind == MonsterProjectileKind.SOMBRERO) {
			if (hit.getType() == HitResult.Type.BLOCK) returning = true;
			return;
		}
		explode(level);
		discard();
	}

	private void explode(ServerLevel level) {
		switch (kind) {
			case SAKE_BOTTLE -> Fx.sound(level, position(), "monster.bottle", 1.4f, 1f);
			case FIREBALL -> Fx.sound(level, position(), "monster.fire", 1.2f, 1.2f);
			case WATER_ORB -> Fx.sound(level, position(), "monster.water", 1.2f, 1.2f);
			case MISSILE, GLITCH_CUBE -> Fx.sound(level, position(), "monster.electric", 1.2f, 1.2f);
			case HOLY_ORB -> Fx.sound(level, position(), "monster.holy", 1.2f, 1.2f);
			default -> {
			}
		}
		Fx.particle(level, net.minecraft.core.particles.ParticleTypes.EXPLOSION, position(), 1, 0, 0);
		hitPlayersAround(level, kind.blastRadius, true);
	}

	private void hitPlayersAround(ServerLevel level, double radius, boolean once) {
		HuntMonster owner = getOwner() instanceof HuntMonster m ? m : null;
		if (owner == null) return;
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(this) > radius * radius) continue;
			if (!once && life % 10 != 0) continue;
			HunterCombat.monsterHitsPlayer(level, owner, p, damage, kind.element,
				kind == MonsterProjectileKind.SOMBRERO ? MonsterMove.Knock.LARGE : MonsterMove.Knock.SMALL, position());
			HunterState s = HunterManager.state(p);
			switch (kind) {
				case SAKE_BOTTLE -> {
					if (!s.has(ArmorSkill.DRUNK_IMMUNE)) s.drunk = Math.max(s.drunk, 140);
				}
				case GLITCH_CUBE -> s.paralysis = Math.max(s.paralysis, 30);
				case WATER_ORB -> s.waterblight = Math.max(s.waterblight, 400);
				default -> {
				}
			}
		}
	}
}
