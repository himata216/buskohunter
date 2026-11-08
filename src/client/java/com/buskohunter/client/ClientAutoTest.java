package com.buskohunter.client;

import com.buskohunter.BuskoHunter;
import com.buskohunter.combat.InputKind;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.WeaponType;
import com.buskohunter.client.screen.CanteenScreen;
import com.buskohunter.client.screen.EquipmentScreen;
import com.buskohunter.client.screen.QuestBoardScreen;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.net.Net;
import com.buskohunter.net.Payloads;
import com.buskohunter.quest.QuestManager;
import com.buskohunter.quest.QuestSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * 開発用の自動テスト（-Dbuskohunter.autotest=true のときだけ動く。普段のプレイには影響しない）。
 * シングルプレイのワールドで、集会所 → 画面 → 受注 → 出発 → 戦闘 → 討伐 → 報酬画面 までを自動で進める。
 */
public final class ClientAutoTest {
	private ClientAutoTest() {
	}

	public static final boolean ENABLED = Boolean.getBoolean("buskohunter.autotest");
	private static int t = -1;
	private static int areaProbe;
	private static boolean found;

	private static void log(String msg) {
		BuskoHunter.LOGGER.info("[AUTOTEST] t={} {}", t, msg);
	}

	/** 統合サーバーのスレッドで、自分の ServerPlayer に対して処理する。 */
	private static void onServer(Consumer<ServerPlayer> action) {
		Minecraft mc = Minecraft.getInstance();
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null || mc.player == null) return;
		var uuid = mc.player.getUUID();
		server.execute(() -> {
			ServerPlayer p = server.getPlayerList().getPlayer(uuid);
			if (p == null) return;
			try {
				action.accept(p);
			} catch (Throwable e) {
				BuskoHunter.LOGGER.error("[AUTOTEST] server step failed", e);
			}
		});
	}

	private static void action(int action, int a, int b) {
		onServer(p -> QuestManager.INSTANCE.handleAction(p, new Payloads.Action(action, a, b)));
	}

	public static void tick(Minecraft mc) {
		if (!ENABLED) return;
		if (t < 0) {
			if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
			t = 0;
			log("world loaded");
		}
		t++;
		try {
			step(mc);
		} catch (Throwable e) {
			BuskoHunter.LOGGER.error("[AUTOTEST] client step failed", e);
		}
	}

	private static void step(Minecraft mc) {
		switch (t) {
			case 60 -> {
				log("go hub");
				action(Payloads.Action.GO_HUB, 0, 0);
			}
			case 200 -> {
				log("open quest board");
				mc.gui.setScreen(new QuestBoardScreen());
			}
			case 300 -> {
				log("open equipment");
				mc.gui.setScreen(new EquipmentScreen());
			}
			case 400 -> {
				log("open canteen");
				mc.gui.setScreen(new CanteenScreen());
			}
			case 500 -> {
				mc.gui.setScreen(null);
				log("accept quest, long sword (fire), eat, depart");
				action(Payloads.Action.ACCEPT_QUEST, 0, 0);
				action(Payloads.Action.SET_WEAPON, WeaponType.LONG_SWORD.ordinal(), ElementType.FIRE.ordinal());
				action(Payloads.Action.EAT_MEAL, 1, 0);
			}
			case 560 -> action(Payloads.Action.DEPART, 0, 0);
			default -> {
			}
		}
		// フィールドに着いたら、モンスターを探して近くへ移動
		if (t >= 900 && !found && t % 40 == 0) {
			onServer(p -> {
				QuestSession s = QuestManager.INSTANCE.sessionOf(p);
				if (s == null || s.layout == null) return;
				ServerLevel level = (ServerLevel) p.level();
				HuntMonster m = s.monster(level, 0);
				if (m == null) {
					Vec3 a = s.layout.areaPos(areaProbe++ % 8);
					p.teleportTo(a.x, a.y + 1, a.z);
					log("probe area " + (areaProbe - 1));
					return;
				}
				Vec3 dir = p.position().subtract(m.position()).multiply(1, 0, 1);
				if (dir.lengthSqr() < 1) dir = new Vec3(1, 0, 0);
				dir = dir.normalize();
				Vec3 at = m.position().add(dir.scale(m.getBbWidth() + 8));
				p.teleportTo(at.x, m.getY() + 0.5, at.z);
				found = true;
				log("found monster " + m.getType().toShortString() + " hp " + m.hp());
			});
		}
		// 攻撃（A 連打・ときどき特殊・回避）
		if (found && t < 2000 && mc.player != null) {
			int phase = t % 24;
			int bits = 0;
			if (phase == 0) bits = InputKind.PRESS_A | InputKind.HELD_A;
			else if (phase == 8 && t % 96 == 8) bits = InputKind.PRESS_S | InputKind.HELD_S;
			else if (phase == 16 && t % 120 == 16) bits = InputKind.PRESS_DODGE | InputKind.DIR_LEFT;
			if (phase == 0 || phase == 8 || phase == 16) Net.sendToServer(new Payloads.Input(bits));
			if (phase == 4 || phase == 12 || phase == 20) Net.sendToServer(new Payloads.Input(0));
		}
		if (t == 2000) {
			log("finish monster");
			onServer(p -> {
				QuestSession s = QuestManager.INSTANCE.sessionOf(p);
				if (s == null) return;
				HuntMonster m = s.monster((ServerLevel) p.level(), 0);
				if (m != null) m.applyRawDamage((ServerLevel) p.level(), m.hp() + 10, p);
			});
		}
		if (t == 2100) {
			onServer(p -> {
				QuestSession s = QuestManager.INSTANCE.sessionOf(p);
				if (s == null) return;
				HuntMonster m = s.monster((ServerLevel) p.level(), 0);
				if (m != null) p.teleportTo(m.getX() + m.getBbWidth() + 2, m.getY() + 0.5, m.getZ());
			});
		}
		if (t == 3200) {
			log("RESULT DONE screen=" + (mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName()));
		}
		if (t == 3300) {
			log("quit");
			mc.stop();
		}
	}
}
