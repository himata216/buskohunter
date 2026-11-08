package com.buskohunter.hunter;

import com.buskohunter.combat.MoveRegistry;
import com.buskohunter.core.HuntItem;
import com.buskohunter.core.Meal;
import com.buskohunter.entity.BarrelBomb;
import com.buskohunter.entity.HuntTrap;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.quest.QuestManager;
import com.buskohunter.util.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 狩猟アイテムの効果。 */
public final class ItemActions {
	private ItemActions() {
	}

	public static void onStartUse(ServerPlayer player, HuntItem kind) {
		if (kind == HuntItem.WHETSTONE) {
			HunterManager.startMove(player, MoveRegistry.USE_SHARPEN);
			Fx.playerSound((ServerLevel) player.level(), player.position(), "player.sharpen", 1f, 1f);
		}
	}

	public static void finishUse(ServerPlayer player, HuntItem kind) {
		HunterState s = HunterManager.state(player);
		ServerLevel level = (ServerLevel) player.level();
		Vec3 pos = player.position();
		switch (kind) {
			case POTION -> heal(player, s, 30);
			case MEGA_POTION -> heal(player, s, 60);
			case MAX_POTION -> {
				player.setHealth(player.getMaxHealth());
				s.redHealth = 0;
				s.maxStamina = 150;
				s.stamina = 150;
			}
			case ANTIDOTE -> {
				s.poison = 0;
				s.fireblight = s.waterblight = s.thunderblight = s.iceblight = s.dragonblight = 0;
				s.drunk = 0;
			}
			case ENERGY_DRINK -> {
				s.energyDrink = 20 * 60;
				s.stamina = s.maxStamina;
			}
			case DEMONDRUG -> s.demondrug = true;
			case ARMORSKIN -> s.armorskin = true;
			case RATION -> s.maxStamina = Math.min(150, s.maxStamina + (s.meal.skill == Meal.MealSkill.HEAVY_EATER ? 40 : 25));
			case WELL_DONE_STEAK -> {
				s.maxStamina = 150;
				s.stamina = 150;
			}
			case WHETSTONE -> {
				s.sharpness = s.stats.sharpness().total();
				Fx.particle(level, ParticleTypes.CRIT, pos.add(0, 1, 0), 12, 0.4, 0.2);
			}
			default -> {
			}
		}
		switch (kind.use) {
			case DRINK -> {
				Fx.playerSound(level, pos, "player.drink", 1f, 1f);
				Fx.playerSound(level, pos, "player.heal", 0.7f, 1f);
				Fx.particle(level, new DustParticleOptions(0x60FF60, 1.2f), pos.add(0, 1.2, 0), 14, 0.4, 0);
				// モンハン恒例のガッツポーズ
				HunterManager.startMove(player, MoveRegistry.USE_FLEX);
			}
			case EAT -> Fx.playerSound(level, pos, "player.eat", 1f, 1f);
			default -> {
			}
		}
	}

	private static void heal(ServerPlayer player, HunterState s, float amount) {
		float a = amount * s.healMul();
		player.heal(a);
		s.redHealth = 0;
	}

	/** 投げる・置くアイテム。true なら消費。 */
	public static boolean useInstant(ServerPlayer player, HuntItem kind) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 pos = player.position();
		switch (kind) {
			case PAINTBALL -> {
				HuntMonster m = lookTarget(player, 30);
				HunterManager.startMove(player, MoveRegistry.USE_THROW);
				Fx.playerSound(level, pos, "item.paintball", 1f, 1f);
				if (m != null) {
					m.paint();
					Fx.particle(level, new DustParticleOptions(0xFF60C0, 2.0f), m.position().add(0, m.kind.height * 0.6, 0), 30, 1.0, 0);
					player.sendOverlayMessage(Component.literal(m.kind.jaName + "にペイントした！"));
				}
				return true;
			}
			case FLASH_BOMB -> {
				HunterManager.startMove(player, MoveRegistry.USE_THROW);
				Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(4));
				Fx.playerSound(level, at, "item.flash", 2f, 1f);
				Fx.particle(level, ParticleTypes.END_ROD, at, 40, 2.0, 0.3);
				for (HuntMonster m : level.getEntitiesOfClass(HuntMonster.class, player.getBoundingBox().inflate(20))) {
					Vec3 toPlayer = player.position().subtract(m.position()).normalize();
					Vec3 mLook = m.local(1, 0, 0).subtract(m.position()).normalize();
					if (toPlayer.x * mLook.x + toPlayer.z * mLook.z > -0.2) {
						m.flash(level);
					}
				}
				return true;
			}
			case SHOCK_TRAP, PITFALL_TRAP -> {
				if (!player.onGround()) return false;
				HunterManager.startMove(player, MoveRegistry.USE_PLACE);
				HuntTrap trap = HuntTrap.create(level, player, kind == HuntItem.PITFALL_TRAP);
				trap.setPos(pos.x, pos.y, pos.z);
				level.addFreshEntity(trap);
				Fx.playerSound(level, pos, "item.trap_set", 1f, 1f);
				return true;
			}
			case TRANQ_BOMB -> {
				HunterManager.startMove(player, MoveRegistry.USE_THROW);
				HuntMonster m = lookTarget(player, 20);
				if (m == null) {
					player.sendOverlayMessage(Component.literal("当たらなかった…"));
					return true;
				}
				m.tranq(level, player);
				if (!m.isTrapped()) player.sendOverlayMessage(Component.literal("罠にかかっていないと効かない"));
				else if (!m.isCapturable()) player.sendOverlayMessage(Component.literal("まだ弱っていない"));
				return true;
			}
			case BARREL_BOMB -> {
				HunterManager.startMove(player, MoveRegistry.USE_PLACE);
				BarrelBomb bomb = BarrelBomb.create(level, player);
				Vec3 at = pos.add(player.getLookAngle().multiply(1.5, 0, 1.5));
				bomb.setPos(at.x, pos.y, at.z);
				level.addFreshEntity(bomb);
				Fx.playerSound(level, pos, "item.trap_set", 1f, 0.7f);
				return true;
			}
			case FARCASTER -> {
				if (!QuestManager.inActiveQuest(player)) {
					player.sendOverlayMessage(Component.literal("クエスト中にしか使えない"));
					return false;
				}
				Fx.playerSound(level, pos, "item.farcaster", 1f, 1f);
				Fx.particle(level, ParticleTypes.CLOUD, pos.add(0, 1, 0), 40, 0.6, 0.05);
				QuestManager.get(player).sendToCamp(player);
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/** 視線の先にいるモンスター。 */
	public static HuntMonster lookTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));
		HuntMonster best = null;
		double bestD = Double.MAX_VALUE;
		for (HuntMonster m : player.level().getEntitiesOfClass(HuntMonster.class, new AABB(eye, end).inflate(4))) {
			AABB box = m.getBoundingBox().inflate(1.0);
			var hit = box.clip(eye, end);
			if (hit.isPresent()) {
				double d = hit.get().distanceToSqr(eye);
				if (d < bestD) {
					bestD = d;
					best = m;
				}
			}
		}
		return best;
	}
}
