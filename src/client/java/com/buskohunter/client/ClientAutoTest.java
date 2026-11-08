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
import net.minecraft.world.level.levelgen.Heightmap;
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
		// フィールドに着いたら、モンスターを探して近くの地面へ移動（モンスターの方を向く）
		if (t >= 900 && t < 2300 && (!found ? t % 40 == 0 : t % 200 == 0)) {
			onServer(p -> {
				QuestSession s = QuestManager.INSTANCE.sessionOf(p);
				if (s == null || s.layout == null) return;
				ServerLevel level = (ServerLevel) p.level();
				HuntMonster m = s.monster(level, 0);
				if (m == null) {
					if (found) return;
					Vec3 a = s.layout.areaPos(areaProbe++ % 8);
					p.teleportTo(a.x, a.y + 1, a.z);
					log("probe area " + (areaProbe - 1));
					return;
				}
				Vec3 dir = p.position().subtract(m.position()).multiply(1, 0, 1);
				if (dir.lengthSqr() < 1) dir = new Vec3(1, 0, 0);
				dir = dir.normalize();
				Vec3 at = m.position().add(dir.scale(m.getBbWidth() * 0.5 + 7));
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z));
				double dx = m.getX() - at.x, dz = m.getZ() - at.z;
				float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
				p.connection.teleport(at.x, y, at.z, yaw, 10f);
				if (!found) log("found monster " + m.getType().toShortString() + " hp " + m.hp());
				found = true;
			});
		}
		// 武器をホットバーで持つ（クエスト中は三人称になる）
		if (t == 880 && mc.player != null) {
			var inv = mc.player.getInventory();
			for (int i = 0; i < 9; i++) {
				if (inv.getItem(i).getItem() instanceof com.buskohunter.item.HunterWeaponItem) {
					inv.setSelectedSlot(i);
					log("hold weapon slot " + i);
					break;
				}
			}
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
				if (m != null) {
					int y = p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (m.getX() + m.getBbWidth()), (int) m.getZ());
					p.connection.teleport(m.getX() + m.getBbWidth(), y, m.getZ(), 90f, 20f);
				}
			});
		}
		if (t == 3200) {
			log("RESULT DONE screen=" + (mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName()));
		}
		if (t >= 3300) tour(mc, t - 3300);
	}

	// =====================================================================
	// 見た目の確認（集会所の NPC・看板、武器の持ち方、フィールドの地形）を撮影する
	// =====================================================================
	private static final int HUB_VIEWS = 4;
	private static final int VIEW_TICKS = 90;
	private static final int WEAPON_TICKS = 40;
	private static final int FIELD_TICKS = 260;
	private static final com.buskohunter.core.FieldType[] TOUR_FIELDS = {
		com.buskohunter.core.FieldType.FOREST, com.buskohunter.core.FieldType.DESERT,
		com.buskohunter.core.FieldType.VOLCANO, com.buskohunter.core.FieldType.SKY};

	private static void shot(Minecraft mc, String name) {
		net.minecraft.client.Screenshot.grab(mc.gameDirectory, "bh_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1,
			msg -> log("screenshot " + name));
	}

	private static void tour(Minecraft mc, int k) {
		if (k == 0) {
			mc.gui.setScreen(null);
			if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			onServer(p -> p.setGameMode(net.minecraft.world.level.GameType.CREATIVE));
			log("tour start");
		}
		// 集会所の 4 方向
		double[][] views = {{0.5, -7.5, 180}, {-9.5, 0.5, 90}, {9.5, 0.5, -90}, {0.5, 9.5, 0}};
		if (k < HUB_VIEWS * VIEW_TICKS) {
			int v = k / VIEW_TICKS, r = k % VIEW_TICKS;
			if (r == 1) {
				double[] vw = views[v];
				onServer(p -> {
					ServerLevel hub = p.level().getServer().getLevel(com.buskohunter.quest.Dims.HUB);
					if (hub == null) return;
					int y = com.buskohunter.quest.WorldBuilder.hubFloorY(hub);
					p.teleportTo(hub, vw[0], y, vw[1], java.util.Set.of(), (float) vw[2], 8f, true);
				});
			}
			if (r == VIEW_TICKS - 5) shot(mc, "hub_" + v);
			return;
		}
		k -= HUB_VIEWS * VIEW_TICKS;
		// 武器の持ち方（右前・左前から）
		WeaponType[] ws = WeaponType.values();
		if (k < ws.length * WEAPON_TICKS) {
			int i = k / WEAPON_TICKS, r = k % WEAPON_TICKS;
			WeaponType w = ws[i];
			if (r == 0) {
				ElementType e = ElementType.SERIES[i % ElementType.SERIES.length];
				onServer(p -> {
					ServerLevel hub = p.level().getServer().getLevel(com.buskohunter.quest.Dims.HUB);
					if (hub == null) return;
					int y = com.buskohunter.quest.WorldBuilder.hubFloorY(hub);
					if (p.level() != hub || p.distanceToSqr(0.5, y, 13.5) > 1) p.teleportTo(hub, 0.5, y, 13.5, java.util.Set.of(), 180f, 0f, true);
					p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, com.buskohunter.registry.ModItems.WEAPONS.get(w).create(e));
				});
			}
			var pl = mc.player;
			if (r == 4) {
				ShowcaseScreen.angle = 90f;
				mc.gui.setScreen(new ShowcaseScreen());
			}
			if (r == 12) shot(mc, "weapon_" + w.id + "_side");
			if (r == 14) ShowcaseScreen.angle = 145f;
			if (r == 20) shot(mc, "weapon_" + w.id + "_front");
			if (r == 22) {
				mc.gui.setScreen(null);
				mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
				if (pl != null) {
					pl.setYRot(180f);
					pl.setYHeadRot(180f);
					pl.setXRot(10f);
				}
			}
			if (r == 36) shot(mc, "weapon_" + w.id + "_fp");
			if (r == 38 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			return;
		}
		k -= ws.length * WEAPON_TICKS;
		// フィールドの地形
		if (k < TOUR_FIELDS.length * FIELD_TICKS) {
			int i = k / FIELD_TICKS, r = k % FIELD_TICKS;
			com.buskohunter.core.FieldType f = TOUR_FIELDS[i];
			if (r == 0) {
				mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
				onServer(p -> {
					ServerLevel lvl = p.level().getServer().getLevel(com.buskohunter.quest.Dims.field(f));
					if (lvl == null) {
						log("no field level " + f);
						return;
					}
					com.buskohunter.quest.FieldLayout l = com.buskohunter.quest.FieldLayout.create(lvl, f, 6);
					com.buskohunter.quest.WorldBuilder.prepareField(lvl, f, l);
					int a = 2;
					double cx = l.areaX[a] + 0.5, cz = l.areaZ[a] + 0.5;
					double px = cx + 16, pz = cz + 12, py = l.areaY[a] + 9;
					float yaw = (float) (Math.toDegrees(Math.atan2(cz - pz, cx - px)) - 90.0);
					p.teleportTo(lvl, px, py, pz, java.util.Set.of(), yaw, 22f, true);
					p.getAbilities().flying = true;
					p.onUpdateAbilities();
					int water = 0;
					for (int dx = -40; dx <= 40; dx += 2) {
						for (int dz = -40; dz <= 40; dz += 2) {
							int hy = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) cx + dx, (int) cz + dz);
							if (!lvl.getBlockState(new net.minecraft.core.BlockPos((int) cx + dx, hy - 1, (int) cz + dz)).getFluidState().isEmpty()) water++;
						}
					}
					log("field " + f + " camp y=" + l.campY + " area y=" + l.areaY[a] + " fluid=" + water);
				});
			}
			if (r == FIELD_TICKS - 10) shot(mc, "field_" + f.name().toLowerCase());
			return;
		}
		k -= TOUR_FIELDS.length * FIELD_TICKS;
		if (k == 20) {
			log("RESULT TOUR DONE");
		}
		if (k == 40) {
			log("quit");
			mc.stop();
		}
	}
}
