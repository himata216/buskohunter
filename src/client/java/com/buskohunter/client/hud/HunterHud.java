package com.buskohunter.client.hud;

import com.buskohunter.BuskoHunter;
import com.buskohunter.client.ClientHunter;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.QuestDef;
import com.buskohunter.core.Sharpness;
import com.buskohunter.core.WeaponType;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.item.HunterWeaponItem;
import com.buskohunter.net.Payloads;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/** モンハン風 HUD（体力・スタミナ・切れ味・武器ゲージ・ミニマップ・時計・パーティ・通知）。 */
public final class HunterHud {
	private HunterHud() {
	}

	public static void register() {
		AddGuiOverlayLayersEvent.BUS.addListener(e -> {
			ForgeLayeredDraw root = e.getLayeredDraw();
			// チャットの下に狩猟 HUD を描く
			root.addBelow(ForgeLayeredDraw.POST_SLEEP_STACK, BuskoHunter.id("hunter_hud"), ForgeLayeredDraw.CHAT_OVERLAY, HunterHud::extract);
			// クエスト中はバニラの体力・満腹度・防具・経験値バーを隠す
			root.addConditionTo(ForgeLayeredDraw.HOTBAR_AND_DECOS, ForgeLayeredDraw.HEALTH_BAR, () -> !hideVanilla());
			root.addConditionTo(ForgeLayeredDraw.HOTBAR_AND_DECOS, ForgeLayeredDraw.EXPERIENCE_LEVEL, () -> !hideVanilla());
			root.addConditionTo(ForgeLayeredDraw.HOTBAR_AND_DECOS, ForgeLayeredDraw.CONTEXTUAL_INFO, () -> !hideVanilla());
		});
	}

	private static boolean hideVanilla() {
		return ClientHunter.inQuest();
	}

	private static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.gui.hud.isHidden()) return;
		boolean quest = ClientHunter.inQuest();
		boolean weapon = player.getMainHandItem().getItem() instanceof HunterWeaponItem;
		int w = g.guiWidth();
		int h = g.guiHeight();

		int vitalsRight = 0;
		if (quest || weapon) {
			vitalsRight = drawVitals(g, player, quest);
		}
		if (quest) {
			drawClock(g, w, vitalsRight);
			drawMinimap(g, player, w);
			drawParty(g, player);
		}
		DamageNumbers.extract(g, delta);
		drawBanners(g, w, h);
	}

	// ------------------------------------------------------------------
	// 左上：体力・スタミナ・切れ味・武器
	// ------------------------------------------------------------------
	/** @return パネルの右端の X */
	private static int drawVitals(GuiGraphicsExtractor g, LocalPlayer player, boolean quest) {
		HunterState s = ClientHunter.STATE;
		int x = 8, y = 8;
		float hp = player.getHealth();
		float maxHp = Math.max(1f, player.getMaxHealth());
		int barW = (int) Math.min(170, 80 + maxHp * 0.6f);
		Gfx.panel(g, x - 4, y - 4, barW + 66, 60);

		// 武器アイコン
		WeaponType wt = s.weapon;
		Gfx.panel(g, x, y, 26, 26, 0xFF1A1E24, Gfx.opaque(s.element.color));
		Gfx.icon(g, "gui/weapon_" + wt.id, x + 5, y + 5, 16);
		Gfx.icon(g, "gui/element_" + s.element.id, x + 16, y + 16, 9, 16);

		int bx = x + 32;
		// 体力（緑）＋赤ゲージ
		g.fill(bx - 1, y - 1, bx + barW + 1, y + 8, 0xFF000000);
		g.fill(bx, y, bx + barW, y + 7, 0xFF2A1010);
		int red = Math.round(barW * Math.min(1f, (hp + s.redHealth) / maxHp));
		g.fill(bx, y, bx + red, y + 7, 0xFFB02020);
		int hpW = Math.round(barW * (hp / maxHp));
		int hpColor = hp / maxHp < 0.3f ? 0xFFE0C030 : 0xFF40C850;
		if (s.poison > 0) hpColor = 0xFFA050D0;
		g.fill(bx, y, bx + hpW, y + 7, hpColor);
		g.fill(bx, y, bx + hpW, y + 1, Gfx.lighten(hpColor, 0.4f));
		// スタミナ（黄）
		int sy = y + 10;
		float maxSt = Math.max(1f, s.maxStamina);
		int stW = Math.round(barW * (maxSt / 150f));
		Gfx.bar(g, bx, sy, stW, 4, s.stamina / maxSt, s.energyDrink > 0 ? 0xFFFFA020 : 0xFFE8D040, 0xFF2A2610);

		// 切れ味
		int shy = y + 18;
		if (!wt.ranged) {
			drawSharpness(g, bx, shy, s);
		} else {
			Gfx.text(g, "弾: " + s.currentAmmo().jaName + "  " + s.ammo + "/" + s.currentAmmo().magazine, bx, shy - 1, Gfx.WHITE);
		}
		// 武器名
		Gfx.text(g, s.stats.name(), bx, y + 28, Gfx.GOLD);
		// 武器固有ゲージ
		WeaponGauges.draw(g, bx, y + 39, s);
		// やられ状態・バフ
		drawStatus(g, x, y + 58, s);
		return x - 4 + barW + 66;
	}

	private static void drawSharpness(GuiGraphicsExtractor g, int x, int y, HunterState s) {
		Sharpness sh = s.stats.sharpness();
		int total = Math.max(1, sh.total());
		int width = 96;
		g.fill(x - 1, y - 1, x + width + 1, y + 6, 0xFF000000);
		int cx = x;
		int acc = 0;
		for (Sharpness.Color c : Sharpness.Color.values()) {
			int len = sh.length(c);
			if (len <= 0) continue;
			int segW = Math.max(1, Math.round(width * (float) len / total));
			int segStart = acc;
			acc += len;
			int remainingInSeg = Math.max(0, Math.min(len, s.sharpness - segStart));
			int litW = Math.round(segW * (float) remainingInSeg / len);
			g.fill(cx, y, cx + segW, y + 5, Gfx.darken(Gfx.opaque(c.rgb), 0.75f));
			g.fill(cx, y, cx + litW, y + 5, Gfx.opaque(c.rgb));
			cx += segW;
		}
		Sharpness.Color cur = sh.colorAt(s.sharpness);
		Gfx.text(g, "切れ味", x + width + 6, y - 2, Gfx.opaque(cur.rgb));
	}

	private static void drawStatus(GuiGraphicsExtractor g, int x, int y, HunterState s) {
		int ix = x;
		ix = statusIcon(g, ix, y, s.fireblight, "status_fireblight");
		ix = statusIcon(g, ix, y, s.waterblight, "status_waterblight");
		ix = statusIcon(g, ix, y, s.thunderblight, "status_thunderblight");
		ix = statusIcon(g, ix, y, s.iceblight, "status_iceblight");
		ix = statusIcon(g, ix, y, s.dragonblight, "status_dragonblight");
		ix = statusIcon(g, ix, y, s.poison, "status_poison");
		ix = statusIcon(g, ix, y, s.sleep, "status_sleep");
		ix = statusIcon(g, ix, y, s.paralysis, "status_paralysis");
		ix = statusIcon(g, ix, y, s.stun, "status_stun");
		ix = statusIcon(g, ix, y, s.drunk, "status_drunk");
		ix = statusIcon(g, ix, y, s.hornAttack + (s.demondrug ? 1 : 0), "status_attack_up");
		ix = statusIcon(g, ix, y, s.hornDefense + (s.armorskin ? 1 : 0), "status_defense_up");
		ix = statusIcon(g, ix, y, s.hornSpeed + s.extractWhite, "status_speed_up");
		statusIcon(g, ix, y, s.energyDrink, "status_heal");
	}

	private static int statusIcon(GuiGraphicsExtractor g, int x, int y, int ticks, String icon) {
		if (ticks <= 0) return x;
		Gfx.icon(g, "gui/" + icon, x, y, 12);
		return x + 14;
	}

	// ------------------------------------------------------------------
	// 上中央：時計とクエスト名
	// ------------------------------------------------------------------
	/** 残り時間と目標。上の段に入らない（画面が狭い）ときはミニマップの下に出す。 */
	private static void drawClock(GuiGraphicsExtractor g, int w, int vitalsRight) {
		Payloads.QuestSync q = ClientHunter.QUEST;
		if (q.questIndex() < 0) return;
		QuestDef def = QuestDef.byIndex(q.questIndex());
		String goal = def.goalText();
		int pw = Math.max(150, Math.min(220, Gfx.width(goal) + 16));
		int minimapLeft = w - MINIMAP_SIZE - 11;
		int top;
		int cx;
		if (minimapLeft - vitalsRight >= pw + 8) {
			cx = (vitalsRight + minimapLeft) / 2;
			top = 4;
		} else {
			cx = w - 8 - pw / 2;
			top = 8 + MINIMAP_SIZE + 18;
		}
		Gfx.panel(g, cx - pw / 2, top, pw, 30);
		int col = q.timeLeft() < 20 * 60 * 5 ? (System.currentTimeMillis() / 400 % 2 == 0 ? Gfx.RED : Gfx.WHITE) : Gfx.WHITE;
		Gfx.center(g, "残り " + Gfx.time(q.timeLeft()) + "   力尽き " + q.faints() + "/" + q.maxFaints(), cx, top + 4, col);
		Gfx.center(g, goal, cx, top + 16, Gfx.GOLD);
	}

	private static final int MINIMAP_SIZE = 110;

	// ------------------------------------------------------------------
	// 右上：ミニマップ
	// ------------------------------------------------------------------
	private static void drawMinimap(GuiGraphicsExtractor g, LocalPlayer player, int screenW) {
		Payloads.QuestSync q = ClientHunter.QUEST;
		int size = MINIMAP_SIZE;
		int mx = screenW - size - 8;
		int my = 8;
		Gfx.panel(g, mx - 3, my - 3, size + 6, size + 18);
		g.fill(mx, my, mx + size, my + size, 0xE0203020);
		// 表示範囲：キャンプを中心にエリアを全部収める
		int cx = q.campX(), cz = q.campZ();
		int range = 60;
		for (int i = 0; i < q.areaX().length; i++) {
			range = Math.max(range, Math.max(Math.abs(q.areaX()[i] - cx), Math.abs(q.areaZ()[i] - cz)) + 25);
		}
		float scale = (size / 2f) / range;
		int ox = mx + size / 2, oz = my + size / 2;
		g.enableScissor(mx, my, mx + size, my + size);
		// エリア
		for (int i = 0; i < q.areaX().length; i++) {
			int ax = ox + Math.round((q.areaX()[i] - cx) * scale);
			int az = oz + Math.round((q.areaZ()[i] - cz) * scale);
			int r = Math.max(5, Math.round(22 * scale));
			g.fill(ax - r, az - r, ax + r, az + r, 0x3060A060);
			g.outline(ax - r, az - r, r * 2, r * 2, 0x8090D090);
			Gfx.center(g, String.valueOf(i + 1), ax, az - 4, 0xFFE0FFE0);
		}
		// キャンプ
		Gfx.icon(g, "gui/map_camp", ox - 6, oz - 6, 12);
		// 仲間
		for (Payloads.QuestSync.Member m : q.members()) {
			if (m.name().equals(player.getName().getString())) continue;
			int px = ox + Math.round((m.x() - cx) * scale);
			int pz = oz + Math.round((m.z() - cz) * scale);
			g.fill(px - 2, pz - 2, px + 2, pz + 2, 0xFF40A0FF);
		}
		// モンスター
		for (Payloads.QuestSync.MonsterInfo mi : q.monsters()) {
			if (!mi.has(Payloads.QuestSync.MonsterInfo.DISCOVERED) || mi.has(Payloads.QuestSync.MonsterInfo.CAPTURED)) continue;
			int px = ox + Math.round((mi.x() - cx) * scale);
			int pz = oz + Math.round((mi.z() - cz) * scale);
			boolean blink = mi.has(Payloads.QuestSync.MonsterInfo.CAPTURABLE) && System.currentTimeMillis() / 300 % 2 == 0;
			if (mi.has(Payloads.QuestSync.MonsterInfo.DEAD)) {
				g.fill(px - 3, pz - 3, px + 3, pz + 3, 0xFF808080);
			} else {
				Gfx.icon(g, "gui/map_monster", px - 6, pz - 6, 12);
				if (blink) g.outline(px - 7, pz - 7, 14, 14, 0xFFFFE040);
			}
		}
		// 自分（向き付き）
		int px = ox + Math.round(((int) player.getX() - cx) * scale);
		int pz = oz + Math.round(((int) player.getZ() - cz) * scale);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(px, pz);
		pose.rotate((float) Math.toRadians(player.getYRot() + 180f));
		Gfx.icon(g, "gui/map_player", -4, -4, 8);
		pose.popMatrix();
		g.disableScissor();

		// 現在のエリア番号
		int area = nearestArea(q, (int) player.getX(), (int) player.getZ());
		String label = area < 0 ? "ベースキャンプ" : "エリア " + (area + 1);
		Gfx.center(g, FieldName.of(q.field()) + " / " + label, mx + size / 2, my + size + 3, Gfx.WHITE);
	}

	/** 現在地に一番近いエリア（キャンプの近くなら -1）。 */
	public static int nearestArea(Payloads.QuestSync q, int x, int z) {
		int best = -1;
		double md = Double.MAX_VALUE;
		for (int i = 0; i < q.areaX().length; i++) {
			double d = Math.pow(x - q.areaX()[i], 2) + Math.pow(z - q.areaZ()[i], 2);
			if (d < md) {
				md = d;
				best = i;
			}
		}
		double campD = Math.pow(x - q.campX(), 2) + Math.pow(z - q.campZ(), 2);
		if (campD < md && campD < 30 * 30) return -1;
		return best;
	}

	// ------------------------------------------------------------------
	// 左：パーティ
	// ------------------------------------------------------------------
	private static void drawParty(GuiGraphicsExtractor g, LocalPlayer player) {
		var members = ClientHunter.QUEST.members();
		if (members.size() <= 1) return;
		int x = 8, y = 92;
		for (Payloads.QuestSync.Member m : members) {
			if (m.name().equals(player.getName().getString())) continue;
			Gfx.panel(g, x - 2, y - 2, 104, 20);
			Gfx.icon(g, "gui/weapon_" + WeaponType.byOrdinal(m.weapon()).id, x, y, 16);
			Gfx.text(g, m.name(), x + 19, y, m.fainted() ? Gfx.GREY : Gfx.WHITE);
			Gfx.bar(g, x + 19, y + 11, 78, 3, m.hp() / Math.max(1, m.maxHp()), 0xFF40C850, 0xFF2A1010);
			y += 22;
		}
	}

	// ------------------------------------------------------------------
	// 中央：バナー
	// ------------------------------------------------------------------
	private static void drawBanners(GuiGraphicsExtractor g, int w, int h) {
		long now = System.currentTimeMillis();
		ClientHunter.BANNERS.removeIf(a -> now - a.startMs() > a.durationMs());
		int y = h / 2 - 70;
		for (ClientHunter.ActiveBanner ab : ClientHunter.BANNERS) {
			Payloads.Banner b = ab.banner();
			float t = (now - ab.startMs()) / (float) ab.durationMs();
			float alpha = t < 0.08f ? t / 0.08f : (t > 0.85f ? (1f - t) / 0.15f : 1f);
			int color = Gfx.alpha(0xFF000000 | b.color(), alpha);
			switch (b.kind()) {
				case Payloads.Banner.QUEST_CLEAR, Payloads.Banner.QUEST_FAILED, Payloads.Banner.QUEST_START,
					 Payloads.Banner.DEPART -> {
					int bw = (int) (w * 0.7f);
					g.fillGradient(w / 2 - bw / 2, y - 6, w / 2 + bw / 2, y + 44, Gfx.alpha(0xA0000000, alpha), Gfx.alpha(0x60000000, alpha));
					g.fill(w / 2 - bw / 2, y - 6, w / 2 + bw / 2, y - 5, Gfx.alpha(0xFFE8C060, alpha));
					g.fill(w / 2 - bw / 2, y + 43, w / 2 + bw / 2, y + 44, Gfx.alpha(0xFFE8C060, alpha));
					float sc = 3.0f + (t < 0.1f ? (0.1f - t) * 10f : 0f);
					Gfx.bigText(g, b.title(), w / 2, y, sc, color);
					if (!b.sub().isEmpty()) Gfx.center(g, b.sub(), w / 2, y + 32, Gfx.alpha(0xFFFFFFFF, alpha));
					y += 58;
				}
				case Payloads.Banner.MONSTER_INTRO -> {
					// 狩猟対象の登場演出：二つ名を小さく、名前を大きく
					int bw = (int) (w * 0.8f);
					g.fillGradient(0, y - 4, w, y + 52, Gfx.alpha(0x00000000, alpha), Gfx.alpha(0xB0000000, alpha));
					Gfx.center(g, b.sub(), w / 2, y + 2, Gfx.alpha(0xFFE8C060, alpha));
					Gfx.bigText(g, b.title(), w / 2, y + 16, 3.2f, color);
					g.fill(w / 2 - bw / 4, y + 48, w / 2 + bw / 4, y + 49, Gfx.alpha(0xFFE8C060, alpha));
					y += 60;
				}
				case Payloads.Banner.POSE -> {
					boolean flash = (now / 120) % 2 == 0;
					Gfx.bigText(g, b.title(), w / 2, y + 10, 4.0f, flash ? Gfx.alpha(0xFFFF40C0, alpha) : Gfx.alpha(0xFF40F0FF, alpha));
					if (!b.sub().isEmpty()) Gfx.center(g, b.sub(), w / 2, y + 50, Gfx.alpha(0xFFFFFFFF, alpha));
					y += 64;
				}
				default -> {
					int tw = Gfx.font().width(b.title()) + 24;
					Gfx.panel(g, w / 2 - tw / 2, y, tw, b.sub().isEmpty() ? 16 : 28, Gfx.alpha(0xC0101418, alpha), Gfx.alpha(0xFF8A6A28, alpha));
					Gfx.center(g, b.title(), w / 2, y + 4, color);
					if (!b.sub().isEmpty()) Gfx.center(g, b.sub(), w / 2, y + 16, Gfx.alpha(0xFFC0C0C0, alpha));
					y += b.sub().isEmpty() ? 20 : 32;
				}
			}
		}
	}

	/** フィールド名。 */
	static final class FieldName {
		static String of(int ordinal) {
			return com.buskohunter.core.FieldType.byOrdinal(ordinal).jaName;
		}
	}

	/** モンスター名（ミニマップ等で使用）。 */
	static String monsterName(int kind) {
		return MonsterKind.byOrdinal(kind).jaName;
	}

	static int elementColor(ElementType e) {
		return Gfx.opaque(e.color);
	}
}
