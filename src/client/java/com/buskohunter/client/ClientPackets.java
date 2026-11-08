package com.buskohunter.client;

import com.buskohunter.client.anim.PlayerAnimator;
import com.buskohunter.client.audio.BgmManager;
import com.buskohunter.client.hud.DamageNumbers;
import com.buskohunter.client.input.HunterInput;
import com.buskohunter.client.screen.CanteenScreen;
import com.buskohunter.client.screen.EquipmentScreen;
import com.buskohunter.client.screen.HunterMenuScreen;
import com.buskohunter.client.screen.ItemBoxScreen;
import com.buskohunter.client.screen.QuestBoardScreen;
import com.buskohunter.net.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** サーバーから届いたパケットの処理（クライアントのメインスレッドで呼ばれる）。 */
public final class ClientPackets {
	private ClientPackets() {
	}

	private static LocalPlayer player() {
		return Minecraft.getInstance().player;
	}

	public static void hunterSync(Payloads.HunterSync p) {
		p.applyTo(ClientHunter.STATE);
		ClientHunter.lastHunterSync = System.currentTimeMillis();
	}

	public static void moveEvent(Payloads.MoveEvent p) {
		PlayerAnimator.onEvent(p);
		LocalPlayer pl = player();
		if (pl != null && p.entityId() == pl.getId()) HunterInput.onOwnMove(p);
	}

	public static void questSync(Payloads.QuestSync p) {
		ClientHunter.QUEST = p;
		ClientHunter.lastQuestSync = System.currentTimeMillis();
	}

	public static void hubSync(Payloads.HubSync p) {
		ClientHunter.HUB = p;
		// パーティを抜けたら準備中の表示を消す
		if (p.mySession() < 0 && ClientHunter.QUEST.state() == Payloads.QuestSync.PREPARING) {
			ClientHunter.QUEST = Payloads.QuestSync.empty();
		}
	}

	public static void openScreen(Payloads.OpenScreen p) {
		Screen s = switch (p.screen()) {
			case Payloads.OpenScreen.QUEST_BOARD -> new QuestBoardScreen();
			case Payloads.OpenScreen.EQUIPMENT -> new EquipmentScreen();
			case Payloads.OpenScreen.CANTEEN -> new CanteenScreen();
			case Payloads.OpenScreen.ITEM_BOX -> new ItemBoxScreen();
			case Payloads.OpenScreen.HUNTER_MENU -> new HunterMenuScreen();
			default -> null;
		};
		if (s != null) Minecraft.getInstance().gui.setScreen(s);
	}

	public static void banner(Payloads.Banner p) {
		ClientHunter.pushBanner(p);
		BgmManager.onBanner(p);
	}

	public static void damageNumber(Payloads.DamageNumber p) {
		DamageNumbers.add(p);
	}

	public static void knockback(Payloads.Knockback p) {
		LocalPlayer pl = player();
		if (pl == null) return;
		Vec3 v = new Vec3(p.x(), p.y(), p.z());
		pl.setDeltaMovement(p.add() ? pl.getDeltaMovement().add(v) : v);
	}

	public static void result(Payloads.Result p) {
		BuskoHunterClient.queueResult(p);
	}
}
