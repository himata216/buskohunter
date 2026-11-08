package com.buskohunter.entity;

import com.buskohunter.combat.DamageCalc;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.WeaponType;
import com.buskohunter.hunter.HunterManager;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.hunter.ItemActions;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.registry.ModEntities;
import com.buskohunter.registry.ModItems;
import com.buskohunter.util.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

/** 操虫棍の猟虫。モンスターまで飛んでエキスを採り、戻ってくる。 */
public class Kinsect extends ThrowableItemProjectile {
	private static final Random RNG = new Random();
	private UUID ownerId;
	private int targetId = -1;
	private Vec3 dest = Vec3.ZERO;
	private boolean returning;
	private int extract;
	private int life;

	public Kinsect(EntityType<? extends ThrowableItemProjectile> type, Level level) {
		super(type, level);
	}

	public static void launch(ServerLevel level, ServerPlayer player) {
		Kinsect k = new Kinsect(ModEntities.KINSECT, level);
		k.ownerId = player.getUUID();
		k.setItem(new ItemStack(ModItems.projectile("proj_kinsect")));
		Vec3 eye = player.getEyePosition();
		k.setPos(eye.x, eye.y, eye.z);
		k.setNoGravity(true);
		HuntMonster target = ItemActions.lookTarget(player, 26);
		if (target != null) {
			k.targetId = target.getId();
			k.dest = target.position().add(0, target.kind.height * 0.5, 0);
		} else {
			k.dest = eye.add(player.getLookAngle().scale(18));
		}
		level.addFreshEntity(k);
		Fx.playerSound(level, eye, "weapon.kinsect", 1f, 1f);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.projectile("proj_kinsect");
	}

	@Override
	public void tick() {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		life++;
		ServerPlayer owner = ownerId == null ? null : (ServerPlayer) level.getPlayerByUUID(ownerId);
		if (owner == null || life > 240) {
			if (owner != null) HunterManager.state(owner).kinsectOut = false;
			discard();
			return;
		}
		if (!returning) {
			if (targetId >= 0 && level.getEntity(targetId) instanceof HuntMonster m) {
				dest = m.position().add(0, m.kind.height * 0.5, 0);
				if (m.getBoundingBox().inflate(0.6).contains(position())) {
					strike(level, owner, m);
				}
			} else if (position().distanceToSqr(dest) < 1.5) {
				returning = true;
			}
		} else {
			dest = owner.getEyePosition();
			if (position().distanceToSqr(dest) < 2.0) {
				deliver(level, owner);
				discard();
				return;
			}
		}
		Vec3 dir = dest.subtract(position());
		double len = dir.length();
		Vec3 vel = len < 1e-3 ? Vec3.ZERO : dir.scale(Math.min(1.0, 0.9 / len) * (len > 0.9 ? 1 : len));
		setDeltaMovement(vel);
		setPos(getX() + vel.x, getY() + vel.y, getZ() + vel.z);
		if (life % 2 == 0) {
			int color = extract == 1 ? 0xFF3030 : (extract == 2 ? 0xFFFFFF : (extract == 3 ? 0xFF9030 : 0xF0C040));
			Fx.particle(level, new DustParticleOptions(color, 1.0f), position(), 2, 0.1, 0);
		}
	}

	private void strike(ServerLevel level, ServerPlayer owner, HuntMonster m) {
		int part = m.partAt(position());
		MonsterKind.Part p = m.kind.parts.get(part);
		String id = p.id();
		if (p.weakSpot() || id.contains("head") || id.contains("helmet") || id.contains("horn") || id.contains("sombrero")) extract = 1;
		else if (id.contains("arm") || id.contains("leg") || id.contains("claw") || id.contains("wing")) extract = 2;
		else extract = 3;
		HunterState s = HunterManager.state(owner);
		DamageCalc.Result r = DamageCalc.compute(s, m.targetInfo(part), 10, 1f, 0.5f, 0f, WeaponType.DamageKind.CUT, 1f, RNG);
		m.receiveHit(level, owner, part, r, s);
		m.sendDamageNumber(owner, position(), r);
		returning = true;
	}

	private void deliver(ServerLevel level, ServerPlayer owner) {
		HunterState s = HunterManager.state(owner);
		s.kinsectOut = false;
		int dur = 20 * 90;
		switch (extract) {
			case 1 -> {
				s.extractRed = dur;
				owner.sendOverlayMessage(Component.literal("赤エキス：攻撃力UP"));
			}
			case 2 -> {
				s.extractWhite = dur;
				owner.sendOverlayMessage(Component.literal("白エキス：移動速度UP"));
			}
			case 3 -> {
				s.extractOrange = dur;
				owner.sendOverlayMessage(Component.literal("橙エキス：防御力UP"));
			}
			default -> {
			}
		}
		if (extract > 0) {
			Fx.playerSound(level, owner.position(), "player.buff", 0.8f, 1.2f);
			if (s.extractRed > 0 && s.extractWhite > 0 && s.extractOrange > 0) {
				owner.sendOverlayMessage(Component.literal("トリプルアップ！"));
			}
		}
	}
}
