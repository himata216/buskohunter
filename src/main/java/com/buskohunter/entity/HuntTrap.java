package com.buskohunter.entity;

import com.buskohunter.monster.HuntMonster;
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

/** シビレ罠・落とし穴。地面に置かれ、モンスターが踏むと拘束する。 */
public class HuntTrap extends ThrowableItemProjectile {
	private boolean pitfall;
	private int life;
	private int triggered = -1;

	public HuntTrap(EntityType<? extends ThrowableItemProjectile> type, Level level) {
		super(type, level);
	}

	public static HuntTrap create(ServerLevel level, ServerPlayer owner, boolean pitfall) {
		HuntTrap t = new HuntTrap(ModEntities.HUNT_TRAP, level);
		t.setOwner(owner);
		t.pitfall = pitfall;
		t.setItem(new ItemStack(ModItems.HUNT_ITEMS.get(pitfall ? com.buskohunter.core.HuntItem.PITFALL_TRAP
			: com.buskohunter.core.HuntItem.SHOCK_TRAP)));
		t.setNoGravity(true);
		return t;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.HUNT_ITEMS.get(com.buskohunter.core.HuntItem.SHOCK_TRAP);
	}

	@Override
	public void tick() {
		if (!(level() instanceof ServerLevel level)) return;
		life++;
		setDeltaMovement(0, 0, 0);
		if (triggered >= 0) {
			if (life % 4 == 0) {
				Fx.particle(level, pitfall ? ParticleTypes.POOF : ParticleTypes.ELECTRIC_SPARK, position().add(0, 0.5, 0), 8, 1.2, 0.1);
			}
			if (life - triggered > (pitfall ? 200 : 160)) discard();
			return;
		}
		if (life % 10 == 0) {
			Fx.particle(level, pitfall ? ParticleTypes.CLOUD : ParticleTypes.ELECTRIC_SPARK, position().add(0, 0.2, 0), 2, 0.3, 0);
		}
		if (life > 20 * 120) {
			discard();
			return;
		}
		for (HuntMonster m : level.getEntitiesOfClass(HuntMonster.class, getBoundingBox().inflate(1.0))) {
			if (m.trap(level, pitfall)) {
				triggered = life;
				return;
			}
		}
	}
}
