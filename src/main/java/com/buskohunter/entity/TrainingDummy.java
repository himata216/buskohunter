package com.buskohunter.entity;

import com.buskohunter.combat.DamageCalc;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.util.Fx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** 集会所の訓練用「ブスコ人形」。技の練習とダメージ確認用。 */
public class TrainingDummy extends PathfinderMob {
	public static final MonsterKind.Part PART = new MonsterKind.Part("dummy", "人形", 0, 1, 0, 1, 60, 60, 60, 1f, false, 0, true);
	/** 最後に叩かれた tick（クライアントで揺れの演出に使う）。 */
	private static final EntityDataAccessor<Integer> DATA_HIT_TICK = SynchedEntityData.defineId(TrainingDummy.class, EntityDataSerializers.INT);
	private int wobble;

	public TrainingDummy(EntityType<? extends TrainingDummy> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 100.0)
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void registerGoals() {
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_HIT_TICK, -100);
	}

	public int lastHitTick() {
		return entityData.get(DATA_HIT_TICK);
	}

	public DamageCalc.Target targetInfo() {
		return new DamageCalc.Target(MonsterKind.BUSKOBA, PART, false, false, false, 1f);
	}

	public void onHit(ServerLevel level, DamageCalc.Result r) {
		wobble = 10;
		entityData.set(DATA_HIT_TICK, tickCount);
		Fx.sound(level, position(), "monster.hurt", 0.4f, 1.6f);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (wobble > 0) wobble--;
	}
}
