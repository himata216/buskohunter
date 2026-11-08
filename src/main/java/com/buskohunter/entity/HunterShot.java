package com.buskohunter.entity;

import com.buskohunter.combat.DamageCalc;
import com.buskohunter.combat.MoveDef;
import com.buskohunter.combat.ShotKind;
import com.buskohunter.core.WeaponType;
import com.buskohunter.hunter.HunterManager;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.hunter.WeaponController;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.registry.ModEntities;
import com.buskohunter.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** ハンターの弾・矢。 */
public class HunterShot extends ThrowableItemProjectile {
	private static final Random RNG = new Random();
	private ShotKind kind = ShotKind.NORMAL;
	private float mv = 10;
	private float extraMul = 1f;
	private int pierceLeft = 1;
	private int life;
	private int lastHitTick = -10;

	public HunterShot(EntityType<? extends ThrowableItemProjectile> type, Level level) {
		super(type, level);
	}

	public static HunterShot create(ServerLevel level, ServerPlayer owner, ShotKind kind, float mv, float extraMul) {
		HunterShot s = new HunterShot(ModEntities.HUNTER_SHOT, level);
		s.setOwner(owner);
		s.kind = kind;
		s.mv = mv;
		s.extraMul = extraMul;
		s.pierceLeft = kind.pierce;
		boolean arrow = kind == ShotKind.ARROW || kind == ShotKind.ARROW_POWER;
		s.setItem(new ItemStack(ModItems.projectile(arrow ? "proj_arrow" : "proj_bullet")));
		s.setNoGravity(!arrow);
		return s;
	}

	/** 弾の先を属性色に染める（3D モデルの tintindex 1）。 */
	public void tint(int color) {
		ItemStack it = getItem().copy();
		it.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
			new net.minecraft.world.item.component.CustomModelData(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(color)));
		setItem(it);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.projectile("proj_bullet");
	}

	@Override
	protected double getDefaultGravity() {
		return 0.01;
	}

	@Override
	public void tick() {
		super.tick();
		if (++life > 50 && !level().isClientSide()) discard();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return (entity instanceof HuntMonster || entity instanceof TrainingDummy) && super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof ServerPlayer player)) {
			discard();
			return;
		}
		if (tickCount - lastHitTick < 2) return;
		lastHitTick = tickCount;
		HunterState s = HunterManager.state(player);
		Vec3 at = hit.getLocation();
		float elementMul = kind == ShotKind.ELEMENT ? 2.6f : 1f;
		float useMv = kind == ShotKind.ELEMENT ? mv * 0.35f : mv;
		MoveDef.Hit h = new MoveDef.Hit(0, 1, 0, MoveDef.Shape.LINE, 1, 1, 1.2f, useMv, elementMul, 1f, 0f, 0f, null,
			WeaponType.DamageKind.SHOT, 0f);
		MoveDef move = s.move != null ? s.move : com.buskohunter.combat.MoveRegistry.LBG_FIRE;
		if (hit.getEntity() instanceof HuntMonster mon) {
			int part = mon.partAt(at);
			DamageCalc.Result r = DamageCalc.compute(s, mon.targetInfo(part), useMv, elementMul, 1f, 0f,
				WeaponType.DamageKind.SHOT, extraMul, RNG);
			mon.receiveHit(level, player, part, r, s);
			mon.sendDamageNumber(player, at, r);
			WeaponController.hitFx(level, player, s, at, r, h);
		} else if (hit.getEntity() instanceof TrainingDummy dummy) {
			DamageCalc.Result r = DamageCalc.compute(s, dummy.targetInfo(), useMv, elementMul, 1f, 0f,
				WeaponType.DamageKind.SHOT, extraMul, RNG);
			dummy.onHit(level, r);
			com.buskohunter.net.Net.send(player,
				new com.buskohunter.net.Payloads.DamageNumber(at.x, at.y, at.z, Math.max(1, Math.round(r.total())),
					r.crit() ? com.buskohunter.net.Payloads.DamageNumber.CRIT : 0));
			WeaponController.hitFx(level, player, s, at, r, h);
		}
		if (--pierceLeft <= 0) discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!level().isClientSide()) discard();
	}
}
