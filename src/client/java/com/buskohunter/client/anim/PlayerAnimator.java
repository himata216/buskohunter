package com.buskohunter.client.anim;

import com.buskohunter.client.ClientHunter;
import com.buskohunter.combat.MoveDef;
import com.buskohunter.combat.MoveRegistry;
import com.buskohunter.core.WeaponType;
import com.buskohunter.item.HunterWeaponItem;
import com.buskohunter.net.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 各プレイヤーの「いま再生中の技モーション」を管理し、描画時に Pose を計算する。
 * サーバーから届く MoveEvent を元に、各クライアントで同じモーションを再生する。
 */
public final class PlayerAnimator {
	private PlayerAnimator() {
	}

	private static final class Track {
		MoveDef move;
		int startAge;
		boolean holding;
		int dir;
		int charge;
		int endedAge = -1000;
		final Pose pose = new Pose();
	}

	private static final Map<Integer, Track> TRACKS = new HashMap<>();

	public static void clear() {
		TRACKS.clear();
	}

	/** サーバーからの技イベント。 */
	public static void onEvent(Payloads.MoveEvent ev) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Entity e = mc.level.getEntity(ev.entityId());
		if (e == null) return;
		Track tr = TRACKS.computeIfAbsent(ev.entityId(), id -> new Track());
		switch (ev.event()) {
			case Payloads.MoveEvent.START -> {
				MoveDef m = MoveRegistry.byNetId(ev.moveNetId());
				tr.move = m;
				tr.startAge = e.tickCount;
				tr.holding = m != null && m.chargeable();
				tr.dir = ev.dir();
				tr.charge = 0;
			}
			case Payloads.MoveEvent.HOLD -> tr.charge = ev.charge();
			case Payloads.MoveEvent.RELEASE -> {
				if (tr.move != null && tr.holding) {
					int elapsed = e.tickCount - tr.startAge;
					if (elapsed > tr.move.holdTick) {
						tr.startAge = e.tickCount - tr.move.holdTick;
					}
				}
				tr.holding = false;
				tr.charge = ev.charge();
			}
			case Payloads.MoveEvent.STOP -> {
				tr.move = null;
				tr.endedAge = e.tickCount;
			}
			default -> {
			}
		}
	}

	/** 現在の技（HUD やカメラ演出用）。 */
	public static MoveDef currentMove(int entityId) {
		Track tr = TRACKS.get(entityId);
		return tr == null ? null : tr.move;
	}

	/**
	 * 描画用のポーズを計算する（null なら上書きなし）。
	 *
	 * @param ageInTicks render state の ageInTicks（部分 tick 込み）
	 */
	public static Pose compute(LivingEntity entity, float ageInTicks, boolean crouching) {
		Track tr = TRACKS.computeIfAbsent(entity.getId(), id -> new Track());
		Pose p = tr.pose.reset();
		WeaponType weapon = weaponOf(entity.getMainHandItem());
		boolean local = entity == Minecraft.getInstance().player;

		if (tr.move != null) {
			float elapsed = ageInTicks - tr.startAge;
			MoveDef m = tr.move;
			if (tr.holding && m.chargeable() && elapsed > m.holdTick) {
				elapsed = m.holdTick;
			}
			if (elapsed >= m.duration) {
				tr.move = null;
				tr.endedAge = (int) ageInTicks;
			} else {
				float t = Math.max(0f, elapsed / m.duration);
				float holdFrac = m.chargeable() ? (float) m.holdTick / m.duration : -1f;
				AnimLibrary.pose(m.anim, t, holdFrac, tr.holding, ageInTicks, tr.dir, p);
				if (m.mirror) p.mirror();
				// 開始直後と終わり際は少しだけバニラと混ぜる
				float in = Math.min(1f, (elapsed + 1f) / 2.5f);
				float out = Math.min(1f, (m.duration - elapsed) / 3f);
				p.weight = Math.max(0f, Math.min(in, out));
				return p;
			}
		}

		// 技を出していない：抜刀中の構え
		if (weapon != null) {
			boolean alt = false;
			boolean guard = crouching && weapon.canGuard;
			if (local) {
				var hs = ClientHunter.STATE;
				alt = switch (weapon) {
					case DUAL_BLADES -> hs.demonMode;
					case CHARGE_BLADE -> hs.cbAxe;
					case SWITCH_AXE -> hs.saSword;
					default -> false;
				};
				if (weapon == WeaponType.CHARGE_BLADE) guard = crouching && !hs.cbAxe;
			}
			AnimLibrary.stance(weapon, guard, alt, ageInTicks, p);
			float since = ageInTicks - tr.endedAge;
			p.weight = Math.max(0f, Math.min(1f, since / 4f));
			return p;
		}
		return null;
	}

	public static WeaponType weaponOf(ItemStack stack) {
		if (stack != null && stack.getItem() instanceof HunterWeaponItem w) {
			return w.weaponType;
		}
		return null;
	}
}
