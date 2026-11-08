package com.buskohunter.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 集会所の看板（空中に浮かぶ文字）。どの設備が何かを示す。
 * 当たり判定なし・保存しない（HubPopulator が配置し直す）。
 */
public class HubLabel extends Entity {
	private static final EntityDataAccessor<String> DATA_TITLE = SynchedEntityData.defineId(HubLabel.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> DATA_SUB = SynchedEntityData.defineId(HubLabel.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(HubLabel.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(HubLabel.class, EntityDataSerializers.FLOAT);

	public HubLabel(EntityType<?> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public void setup(String title, String sub, int color, float scale) {
		entityData.set(DATA_TITLE, title);
		entityData.set(DATA_SUB, sub);
		entityData.set(DATA_COLOR, color);
		entityData.set(DATA_SCALE, scale);
	}

	public String title() {
		return entityData.get(DATA_TITLE);
	}

	public String sub() {
		return entityData.get(DATA_SUB);
	}

	public int color() {
		return entityData.get(DATA_COLOR);
	}

	public float scale() {
		return entityData.get(DATA_SCALE);
	}

	@Override
	public void tick() {
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_TITLE, "");
		builder.define(DATA_SUB, "");
		builder.define(DATA_COLOR, 0xFFFFFF);
		builder.define(DATA_SCALE, 1f);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 80 * 80;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return false;
	}

	@Override
	protected boolean couldAcceptPassenger() {
		return false;
	}
}
