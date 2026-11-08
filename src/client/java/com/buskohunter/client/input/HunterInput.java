package com.buskohunter.client.input;

import com.buskohunter.net.Net;
import com.buskohunter.BuskoHunter;
import com.buskohunter.block.HunterStationBlock;
import com.buskohunter.client.ClientHunter;
import com.buskohunter.client.anim.PlayerAnimator;
import com.buskohunter.client.screen.HunterMenuScreen;
import com.buskohunter.combat.InputKind;
import com.buskohunter.combat.MoveDef;
import com.buskohunter.combat.MoveRegistry;
import com.buskohunter.item.HunterWeaponItem;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.net.Payloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.world.entity.player.Input;
import com.buskohunter.core.WeaponType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * キー入力 → サーバーへの入力パケット。
 * 抜刀中（ハンター武器を持っている時）は左右クリックをバニラの攻撃・使用から奪い、技として送る。
 * 自分の技の踏み込み（移動）と回避はクライアント側で即座に動かす。
 */
public final class HunterInput {
	private HunterInput() {
	}

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(BuskoHunter.id("main"));
	public static KeyMapping special;
	public static KeyMapping dodge;
	public static KeyMapping menu;
	public static KeyMapping camera;

	private static int lastSent = -1;
	private static boolean prevA, prevB, prevS;
	private static int keepAlive;

	/** 自分の技の踏み込み用。 */
	private static MoveDef localMove;
	private static int localMoveTick;
	private static float localYaw;
	private static int localDir;
	private static boolean autoCamera;
	private static CameraType savedCamera;

	private static final int JUMP_WINDOW = 5;
	private static final int GUARD_HOLD = 5;
	private static boolean prevShift, prevJump;
	private static int jumpWindow, forceJump;
	private static int rTicks;
	private static boolean rUsed;
	/** 武器を持っている間はバニラのしゃがみ・ジャンプを止める（Shift＝回避、ジャンプ＝ダブルクリックで回避）。 */
	private static boolean suppressVanillaMove;
	private static boolean guardHeldNow;

	/** 自分がいまガード入力中か（右クリック長押し）。 */
	public static boolean guardHeld() {
		return guardHeldNow;
	}

	public static void register() {
		special = new KeyMapping("key.buskohunter.special", InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);
		// 回避は Shift（しゃがみキー）とジャンプのダブルクリック。こちらは追加の回避キー（初期は未設定）
		dodge = new KeyMapping("key.buskohunter.dodge", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY);
		menu = new KeyMapping("key.buskohunter.menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, CATEGORY);
		camera = new KeyMapping("key.buskohunter.camera", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, CATEGORY);
		MovementInputUpdateEvent.BUS.addListener(e -> {
			if (!suppressVanillaMove) return;
			Input k = e.getInput().keyPresses;
			e.getInput().keyPresses = new Input(k.forward(), k.backward(), k.left(), k.right(), forceJump > 0, false, k.sprint());
		});
		RegisterKeyMappingsEvent.BUS.addListener(e -> {
			e.register(special);
			e.register(dodge);
			e.register(menu);
			e.register(camera);
		});
	}

	/** 自分の技が始まった（サーバーからの MoveEvent）。 */
	public static void onOwnMove(Payloads.MoveEvent ev) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		if (ev.event() == Payloads.MoveEvent.START) {
			localMove = MoveRegistry.byNetId(ev.moveNetId());
			localMoveTick = 0;
			localYaw = mc.player.getYRot();
			localDir = ev.dir();
		} else if (ev.event() == Payloads.MoveEvent.STOP) {
			localMove = null;
		}
	}

	/** クライアント tick の最初（バニラのキー処理より前）。 */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) return;

		boolean screenOpen = mc.gui.screen() != null;
		boolean holdingWeapon = player.getMainHandItem().getItem() instanceof HunterWeaponItem;

		while (menu.consumeClick()) {
			if (!screenOpen) mc.gui.setScreen(new HunterMenuScreen());
		}
		while (camera.consumeClick()) {
			CameraType c = mc.options.getCameraType();
			mc.options.setCameraType(c == CameraType.FIRST_PERSON ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
			autoCamera = false;
		}
		updateAutoCamera(mc, holdingWeapon);

		// スタミナ切れならダッシュ不可
		if (ClientHunter.inQuest() && ClientHunter.STATE.stamina <= 0.5f && player.isSprinting()) {
			player.setSprinting(false);
		}

		if (forceJump > 0) forceJump--;
		int bits = 0;
		suppressVanillaMove = holdingWeapon && !screenOpen && !player.isSpectator() && !player.getAbilities().flying
			&& !player.isInWater() && !player.isInLava() && !player.isPassenger() && !player.onClimbable();
		if (holdingWeapon && !screenOpen && !player.isSpectator()) {
			boolean interact = targetingInteractable(mc);
			int pressA = 0, pressB = 0;
			if (!interact) {
				while (mc.options.keyAttack.consumeClick()) pressA++;
				while (mc.options.keyUse.consumeClick()) pressB++;
			}
			boolean a = !interact && mc.options.keyAttack.isDown();
			boolean b = !interact && mc.options.keyUse.isDown();
			boolean s = special.isDown();
			int pressS = 0;
			while (special.consumeClick()) pressS++;
			int pressDodge = 0;
			while (dodge.consumeClick()) pressDodge++;

			// 回避：Shift を押した瞬間、またはジャンプのダブルクリック
			boolean shift = mc.options.keyShift.isDown();
			boolean jump = mc.options.keyJump.isDown();
			if (suppressVanillaMove) {
				if (shift && !prevShift) pressDodge++;
				if (jump && !prevJump) {
					if (jumpWindow > 0) {
						pressDodge++;
						jumpWindow = 0;
					} else {
						jumpWindow = JUMP_WINDOW;
					}
				} else if (jumpWindow > 0 && --jumpWindow == 0) {
					// 2 回目が来なかった → 普通のジャンプ
					forceJump = 2;
				}
			} else {
				jumpWindow = 0;
			}
			prevShift = shift;
			prevJump = jump;

			WeaponType wt = ClientHunter.STATE.weapon;
			boolean guardWeapon = wt.canGuard;
			boolean modifierWeapon = wt == WeaponType.LONG_SWORD || wt == WeaponType.HUNTING_HORN
				|| wt == WeaponType.SWITCH_AXE || wt == WeaponType.LIGHT_BOWGUN || wt == WeaponType.HEAVY_BOWGUN;
			boolean guardBit = false;
			if (guardWeapon) {
				// 右クリック：短く押す＝攻撃（離した時）、長押し＝ガード。ガード中の左・R は派生技
				if (b && !prevB) {
					rTicks = 0;
					rUsed = false;
				}
				if (b) rTicks++;
				if (b && (pressA > 0 || pressS > 0 || (a && !prevA) || (s && !prevS))) rUsed = true;
				guardBit = b && (rTicks >= GUARD_HOLD || rUsed);
				if (!b && prevB) {
					if (!rUsed && rTicks < GUARD_HOLD) bits |= InputKind.PRESS_B;
					rTicks = 0;
				} else if (!b && !prevB && pressB > 0) {
					bits |= InputKind.PRESS_B;
				}
			} else {
				if (b) bits |= InputKind.HELD_B;
				if (pressB > 0 || (b && !prevB)) bits |= InputKind.PRESS_B;
				// 右クリックを押したまま左・R で派生技
				guardBit = modifierWeapon && b && prevB;
			}

			if (a) bits |= InputKind.HELD_A;
			if (s) bits |= InputKind.HELD_S;
			if (guardBit) bits |= InputKind.HELD_GUARD;
			guardHeldNow = guardBit && guardWeapon;
			if (pressA > 0 || (a && !prevA)) bits |= InputKind.PRESS_A;
			if (pressS > 0 || (s && !prevS)) bits |= InputKind.PRESS_S;
			if (pressDodge > 0) bits |= InputKind.PRESS_DODGE;
			if (mc.options.keyUp.isDown()) bits |= InputKind.DIR_FORWARD;
			if (mc.options.keyDown.isDown()) bits |= InputKind.DIR_BACK;
			if (mc.options.keyLeft.isDown()) bits |= InputKind.DIR_LEFT;
			if (mc.options.keyRight.isDown()) bits |= InputKind.DIR_RIGHT;
			if (!player.onGround()) bits |= InputKind.AIRBORNE;
			prevA = a;
			prevB = b;
			prevS = s;
		} else {
			prevA = prevB = prevS = false;
			prevShift = mc.options.keyShift.isDown();
			prevJump = mc.options.keyJump.isDown();
			jumpWindow = 0;
			rTicks = 0;
			guardHeldNow = false;
			while (special.consumeClick()) {
				// 武器を持っていない時の特殊キーは何もしない
			}
			while (dodge.consumeClick()) {
				// 同上
			}
		}

		int held = bits & (InputKind.HELD_A | InputKind.HELD_B | InputKind.HELD_S | InputKind.HELD_GUARD | InputKind.AIRBORNE);
		int lastHeld = lastSent < 0 ? -1 : lastSent & (InputKind.HELD_A | InputKind.HELD_B | InputKind.HELD_S | InputKind.HELD_GUARD | InputKind.AIRBORNE);
		boolean pressed = (bits & (InputKind.PRESS_A | InputKind.PRESS_B | InputKind.PRESS_S | InputKind.PRESS_DODGE)) != 0;
		keepAlive++;
		if (pressed || held != lastHeld || (held != 0 && keepAlive >= 10)) {
			Net.sendToServer(new Payloads.Input(bits));
			lastSent = bits;
			keepAlive = 0;
		}

		applyLocalMotion(player);
	}

	/** 自分の技の踏み込み・回避の移動をクライアントで動かす（サーバーは位置を受け取るだけ）。 */
	private static void applyLocalMotion(LocalPlayer player) {
		MoveDef m = localMove;
		if (m == null) return;
		int tick = localMoveTick++;
		if (tick >= m.duration) {
			localMove = null;
			return;
		}
		boolean holding = m.chargeable() && PlayerAnimator.currentMove(player.getId()) == m
			&& ClientHunter.STATE.holding;
		if (holding && tick >= m.holdTick) {
			localMoveTick = m.holdTick;
			return;
		}
		for (MoveDef.Lunge l : m.lunges) {
			if (tick >= l.from() && tick < l.to()) {
				float yaw = (float) Math.toRadians(localYaw);
				double fx = -Math.sin(yaw), fz = Math.cos(yaw);
				double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
				double dx, dz;
				boolean isDodge = m == MoveRegistry.DODGE || m == MoveRegistry.DODGE_SIDESTEP || m == MoveRegistry.DODGE_DEMON;
				if (isDodge) {
					double ix = 0, iz = 0;
					if ((localDir & InputKind.DIR_FORWARD) != 0) { ix += fx; iz += fz; }
					if ((localDir & InputKind.DIR_BACK) != 0) { ix -= fx; iz -= fz; }
					if ((localDir & InputKind.DIR_RIGHT) != 0) { ix += rx; iz += rz; }
					if ((localDir & InputKind.DIR_LEFT) != 0) { ix -= rx; iz -= rz; }
					double len = Math.sqrt(ix * ix + iz * iz);
					if (len < 1e-3) { ix = fx; iz = fz; len = 1; }
					float dist = ClientHunter.STATE.has(com.buskohunter.core.ArmorSkill.EVADE_DISTANCE) ? 1.25f : 1f;
					dx = ix / len * l.forward() * 0.62 * dist;
					dz = iz / len * l.forward() * 0.62 * dist;
				} else {
					dx = fx * l.forward() * 0.6;
					dz = fz * l.forward() * 0.6;
				}
				Vec3 v = player.getDeltaMovement();
				double vy = l.up() != 0 ? l.up() * 0.9 : v.y;
				player.setDeltaMovement(dx, vy, dz);
				break;
			}
		}
	}

	/** クエスト中・抜刀中は三人称カメラへ自動で切り替える。 */
	private static void updateAutoCamera(Minecraft mc, boolean holdingWeapon) {
		boolean want = holdingWeapon && ClientHunter.inQuest();
		if (want && !autoCamera && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
			savedCamera = mc.options.getCameraType();
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			autoCamera = true;
		} else if (!want && autoCamera) {
			if (savedCamera != null) mc.options.setCameraType(savedCamera);
			autoCamera = false;
		}
	}

	/** 照準がステーションブロックや剥ぎ取り可能なモンスターに向いているか（右クリックはバニラへ渡す）。 */
	private static boolean targetingInteractable(Minecraft mc) {
		HitResult hit = mc.hitResult;
		if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
			return mc.level.getBlockState(bh.getBlockPos()).getBlock() instanceof HunterStationBlock;
		}
		if (hit instanceof EntityHitResult eh) {
			return (eh.getEntity() instanceof HuntMonster m && m.isCarvable()) || eh.getEntity() instanceof com.buskohunter.entity.HubNpc;
		}
		return false;
	}
}
