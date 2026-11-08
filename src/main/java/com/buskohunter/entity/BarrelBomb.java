package com.buskohunter.entity;

import com.buskohunter.net.Net;
import com.buskohunter.core.QuestDef;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.net.Payloads;
import com.buskohunter.registry.ModEntities;
import com.buskohunter.registry.ModItems;
import com.buskohunter.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 大タル爆弾（設置から 3 秒で爆発）。 */
public class BarrelBomb extends ThrowableItemProjectile {
	private int fuse = 60;

	public BarrelBomb(EntityType<? extends ThrowableItemProjectile> type, Level level) {
		super(type, level);
	}

	public static BarrelBomb create(ServerLevel level, ServerPlayer owner) {
		BarrelBomb b = new BarrelBomb(ModEntities.BARREL_BOMB, level);
		b.setOwner(owner);
		b.setItem(new ItemStack(ModItems.HUNT_ITEMS.get(com.buskohunter.core.HuntItem.BARREL_BOMB)));
		b.setNoGravity(true);
		return b;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.HUNT_ITEMS.get(com.buskohunter.core.HuntItem.BARREL_BOMB);
	}

	@Override
	public void tick() {
		if (!(level() instanceof ServerLevel level)) return;
		setDeltaMovement(0, 0, 0);
		if (fuse % 10 == 0) {
			Fx.particle(level, ParticleTypes.SMOKE, position().add(0, 1.0, 0), 3, 0.1, 0.02);
		}
		if (--fuse > 0) return;
		Vec3 pos = position();
		Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, pos.add(0, 0.5, 0), 1, 0, 0);
		Fx.sound(level, pos, "item.bomb", 3f, 1f);
		ServerPlayer owner = getOwner() instanceof ServerPlayer p ? p : null;
		for (HuntMonster m : level.getEntitiesOfClass(HuntMonster.class, getBoundingBox().inflate(5.0))) {
			float dmg = 90f * QuestDef.Rank.values()[m.rankIndex()].attackMul;
			m.applyRawDamage(level, dmg, owner);
			if (owner != null) {
				Net.send(owner, new Payloads.DamageNumber(m.getX(), m.getY() + 2, m.getZ(), Math.round(dmg),
					Payloads.DamageNumber.FIXED));
			}
		}
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(this) < 16) {
				Vec3 away = p.position().subtract(pos).normalize();
				Net.send(p, new Payloads.Knockback(away.x * 0.9, 0.5, away.z * 0.9, false));
			}
		}
		discard();
	}
}
