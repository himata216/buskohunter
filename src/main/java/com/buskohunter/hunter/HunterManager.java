package com.buskohunter.hunter;

import com.buskohunter.net.Net;
import com.buskohunter.BuskoHunter;
import com.buskohunter.combat.MoveDef;
import com.buskohunter.core.ArmorSeries;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.WeaponType;
import com.buskohunter.item.HunterWeaponItem;
import com.buskohunter.net.Payloads;
import com.buskohunter.quest.QuestManager;
import com.buskohunter.registry.ModAttachments;
import com.buskohunter.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 全ハンターの戦闘状態（サーバー側）。毎 tick スタミナ・やられ状態・武器ゲージを更新し、
 * 技の進行を WeaponController に任せ、要約をクライアントへ送る。
 */
public final class HunterManager {
	private HunterManager() {
	}

	private static final Map<UUID, HunterState> STATES = new HashMap<>();
	private static final Identifier SPEED_MOD = BuskoHunter.id("move_lock");
	private static final Identifier HEALTH_MOD = BuskoHunter.id("hunter_health");

	public static HunterState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), id -> {
			HunterState s = new HunterState();
			HunterProfile pr = ModAttachments.profile(player);
			s.setLoadout(WeaponType.byOrdinal(pr.weapon()), ElementType.byOrdinal(pr.element()), ArmorSeries.byOrdinal(pr.armor()));
			return s;
		});
	}

	public static void remove(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	/** クライアントからの入力。 */
	public static void clearAll() {
		STATES.clear();
	}

	public static void onInput(ServerPlayer player, int bits) {
		HunterState s = state(player);
		WeaponController.handleInput(player, s, bits);
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			tickPlayer(p);
		}
	}

	private static void tickPlayer(ServerPlayer player) {
		HunterState s = state(player);
		boolean quest = QuestManager.inActiveQuest(player);
		boolean hunting = QuestManager.inHuntingDimension(player);

		// 手に持っている武器に合わせて性能を更新
		ItemStack held = player.getMainHandItem();
		boolean drawn = held.getItem() instanceof HunterWeaponItem;
		if (drawn) {
			HunterWeaponItem w = (HunterWeaponItem) held.getItem();
			ElementType e = HunterWeaponItem.elementOf(held);
			if (w.weaponType != s.weapon || e != s.element) {
				int sharp = s.sharpness;
				s.setLoadout(w.weaponType, e, s.armor);
				s.sharpness = Math.min(sharp > 0 ? sharp : s.stats.sharpness().total(), s.stats.sharpness().total());
			}
		} else if (s.move != null && s.move.weapon != null) {
			WeaponController.interrupt(player, s);
		}

		// スタミナ
		boolean drain = false;
		if (hunting && player.isSprinting()) {
			s.spendStamina(0.32f);
			drain = true;
		}
		if (s.move != null && s.holding && s.move.staminaPerTick > 0) drain = true;
		if (s.weapon == WeaponType.DUAL_BLADES && s.demonMode) {
			s.spendStamina(0.22f);
			drain = true;
			if (s.stamina <= 0) s.demonMode = false;
		}
		if (s.iceblight > 0 && hunting) s.stamina = Math.max(0, s.stamina - 0.05f);
		if (!drain) {
			if (s.staminaRegenDelay > 0) s.staminaRegenDelay--;
			else {
				float regen = 0.85f * s.staminaRegenMul() * (s.waterblight > 0 ? 0.3f : 1f);
				s.stamina = Math.min(s.maxStamina, s.stamina + regen);
			}
		}
		// 最大スタミナは時間で少しずつ減る（携帯食料で戻す）
		if (quest && player.tickCount % 600 == 0 && s.maxStamina > 50 + s.meal.staminaBonus * 0.5f) {
			s.maxStamina = Math.max(50, s.maxStamina - 2);
		}

		// やられ状態・バフ
		tickAilments(player, s, hunting);
		if (s.iframes > 0) s.iframes--;
		if (s.knockdown > 0) s.knockdown--;

		// 赤ゲージ回復
		if (s.redHealth > 0 && player.tickCount % 10 == 0) {
			float heal = Math.min(s.redHealth, 1.2f);
			player.heal(heal);
			s.redHealth -= heal;
		}

		// バニラの自然回復を止め、ダッシュはできる満腹度に固定
		if (hunting) {
			player.getFoodData().setFoodLevel(17);
			player.getFoodData().setSaturation(0f);
		}

		// 武器ゲージ
		WeaponBehaviors.passiveTick(player, s);
		WeaponController.tick(player, s);

		// 移動速度（抜刀中・技中・行動不能）
		applySpeed(player, s, drawn, hunting);
		// 体力（クエスト中は 100 + 食事 + スキル）
		applyHealth(player, s, quest);

		if (player.tickCount % 2 == 0) {
			Net.send(player, Payloads.HunterSync.of(s));
		}
	}

	private static void tickAilments(ServerPlayer player, HunterState s, boolean hunting) {
		ServerLevel level = (ServerLevel) player.level();
		if (s.poison > 0) {
			s.poison--;
			if (s.poison % 20 == 0) {
				HunterCombat.dotDamage(level, player, 1.5f);
				Fx.particle(level, ParticleTypes.WITCH, player.position().add(0, 1, 0), 4, 0.3, 0);
			}
		}
		if (s.fireblight > 0) {
			s.fireblight--;
			if (s.fireblight % 20 == 0) {
				HunterCombat.dotDamage(level, player, 2f);
				Fx.particle(level, ParticleTypes.FLAME, player.position().add(0, 1, 0), 6, 0.3, 0.02);
			}
			// しゃがみ（転がり）で早く消える
			if (s.move != null && s.move.anim == com.buskohunter.combat.AnimKind.DODGE_ROLL) s.fireblight = Math.max(0, s.fireblight - 60);
		}
		if (s.waterblight > 0) s.waterblight--;
		if (s.thunderblight > 0) s.thunderblight--;
		if (s.iceblight > 0) s.iceblight--;
		if (s.dragonblight > 0) s.dragonblight--;
		if (s.sleep > 0) s.sleep--;
		if (s.paralysis > 0) s.paralysis--;
		if (s.stun > 0) s.stun--;
		if (s.drunk > 0) {
			s.drunk--;
			if (s.drunk % 40 == 0) {
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false));
			}
		}
		if (s.hornAttack > 0) s.hornAttack--;
		if (s.hornDefense > 0) s.hornDefense--;
		if (s.hornSpeed > 0) s.hornSpeed--;
		if (s.hornElement > 0) s.hornElement--;
		if (s.hornEarplug > 0) s.hornEarplug--;
		if (s.hornSelf > 0) s.hornSelf--;
		if (s.energyDrink > 0) s.energyDrink--;
		if (s.extractRed > 0) s.extractRed--;
		if (s.extractWhite > 0) s.extractWhite--;
		if (s.extractOrange > 0) s.extractOrange--;
		if (s.incapacitated() && player.tickCount % 10 == 0) {
			Fx.particle(level, s.sleep > 0 ? ParticleTypes.CLOUD : ParticleTypes.ELECTRIC_SPARK, player.position().add(0, 2.1, 0), 3, 0.3, 0);
		}
	}

	private static void applySpeed(ServerPlayer player, HunterState s, boolean drawn, boolean hunting) {
		AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr == null) return;
		double mul = 1.0;
		if (s.incapacitated() || s.knockdown > 0) {
			mul = 0.0;
		} else if (s.move != null) {
			mul = s.move.moveSpeed;
		} else if (drawn && hunting) {
			mul = s.weapon.drawnSpeed;
			if (s.weapon == WeaponType.DUAL_BLADES && s.demonMode) mul = 1.05;
		}
		if (hunting && mul > 0) mul *= s.speedMultiplier();
		double amount = mul - 1.0;
		attr.removeModifier(SPEED_MOD);
		if (Math.abs(amount) > 0.001) {
			attr.addTransientModifier(new AttributeModifier(SPEED_MOD, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static void applyHealth(ServerPlayer player, HunterState s, boolean quest) {
		AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
		if (attr == null) return;
		double want = 0;
		if (quest) {
			want = 80 + s.meal.healthBonus + (s.has(com.buskohunter.core.ArmorSkill.HEALTH_BOOST) ? 30 : 0);
		}
		AttributeModifier cur = attr.getModifier(HEALTH_MOD);
		double have = cur == null ? 0 : cur.amount();
		if (Math.abs(have - want) > 0.01) {
			attr.removeModifier(HEALTH_MOD);
			if (want > 0) attr.addPermanentModifier(new AttributeModifier(HEALTH_MOD, want, AttributeModifier.Operation.ADD_VALUE));
			if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
		}
	}

	// =====================================================================
	// 技の開始（道具・剥ぎ取り・被弾など外部から）
	// =====================================================================
	public static void startMove(ServerPlayer player, MoveDef move) {
		HunterState s = state(player);
		if (s.move != null) return;
		WeaponController.begin(player, s, move, 0);
	}

	public static void forceMove(ServerPlayer player, MoveDef move) {
		HunterState s = state(player);
		WeaponController.interrupt(player, s);
		WeaponController.begin(player, s, move, 0);
	}

	/** 技イベントを自分と周囲のプレイヤーに送る。 */
	public static void broadcastMove(ServerPlayer player, Payloads.MoveEvent ev) {
		Net.send(player, ev);
		for (ServerPlayer other : Net.tracking(player)) {
			if (other != player) Net.send(other, ev);
		}
	}

	/** クエスト開始・装備変更時に。 */
	public static void refreshLoadout(ServerPlayer player) {
		HunterState s = state(player);
		HunterProfile pr = ModAttachments.profile(player);
		s.setLoadout(WeaponType.byOrdinal(pr.weapon()), ElementType.byOrdinal(pr.element()), ArmorSeries.byOrdinal(pr.armor()));
	}
}
