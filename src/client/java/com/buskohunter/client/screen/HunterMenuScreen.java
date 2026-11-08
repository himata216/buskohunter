package com.buskohunter.client.screen;

import com.buskohunter.client.ClientHunter;
import com.buskohunter.client.hud.Gfx;
import com.buskohunter.client.hud.HunterHud;
import com.buskohunter.core.HunterRank;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.QuestDef;
import com.buskohunter.net.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** ハンターメニュー（H）：大きな地図・クエスト情報・操作説明・リタイア／広場への移動。 */
public class HunterMenuScreen extends BhScreen {
	private boolean confirmRetire;

	public HunterMenuScreen() {
		super("ハンターメニュー");
	}

	@Override
	protected void init() {
		super.init();
		pw = Math.min(width - 16, 470);
		ph = Math.min(height - 16, 266);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		if (ClientHunter.inQuest()) {
			button(confirmRetire ? "本当にリタイアする" : "クエストをリタイア", px + pw - 138, py + ph - 26, 130, () -> {
				if (confirmRetire) {
					send(Payloads.Action.RETIRE, 0, 0);
					onClose();
				} else {
					confirmRetire = true;
				}
			});
		} else {
			button("集会所（広場）へ行く", px + pw - 278, py + ph - 26, 130, () -> {
				send(Payloads.Action.GO_HUB, 0, 0);
				onClose();
			});
			button("元の世界へ戻る", px + pw - 138, py + ph - 26, 130, () -> {
				send(Payloads.Action.LEAVE_HUB, 0, 0);
				onClose();
			});
		}
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int mx, int my) {
		Payloads.QuestSync q = ClientHunter.QUEST;
		if (ClientHunter.inQuest() && q.questIndex() >= 0) {
			drawMap(g, q);
			QuestDef def = QuestDef.byIndex(q.questIndex());
			int x = px + 210, y = py + 26;
			Gfx.text(g, def.title(), x, y, Gfx.GOLD);
			y += 12;
			Gfx.text(g, def.goalText(), x, y, Gfx.WHITE);
			y += 11;
			Gfx.text(g, "残り時間 " + Gfx.time(q.timeLeft()) + "  力尽き " + q.faints() + "/" + q.maxFaints(), x, y, Gfx.WHITE);
			y += 16;
			for (Payloads.QuestSync.MonsterInfo m : q.monsters()) {
				MonsterKind k = MonsterKind.byOrdinal(m.kind());
				String st;
				if (m.has(Payloads.QuestSync.MonsterInfo.CAPTURED)) st = "捕獲済み";
				else if (m.has(Payloads.QuestSync.MonsterInfo.DEAD)) st = "討伐済み";
				else if (!m.has(Payloads.QuestSync.MonsterInfo.ACTIVE)) st = "未出現";
				else if (!m.has(Payloads.QuestSync.MonsterInfo.DISCOVERED)) st = "未発見";
				else if (m.has(Payloads.QuestSync.MonsterInfo.CAPTURABLE)) st = "エリア" + (m.area() + 1) + "（捕獲可能！）";
				else st = "エリア" + (m.area() + 1) + (m.has(Payloads.QuestSync.MonsterInfo.LIMPING) ? "（弱っている）" : "");
				Gfx.text(g, k.jaName + " : " + st, x, y, 0xFFFFB060);
				y += 11;
			}
			y += 6;
			Gfx.text(g, "キー操作", x, y, Gfx.GOLD);
			y += 11;
			for (String c : ClientHunter.STATE.weapon.controls()) {
				if (y > py + ph - 40) break;
				y = wrap(g, c, x, y, pw - 220, 0xFFD0D0D0);
			}
		} else {
			Payloads.HubSync hub = ClientHunter.HUB;
			int x = px + 10, y = py + 28;
			int hr = HunterRank.hr(hub.hrPoints());
			Gfx.text(g, "ハンターランク  HR " + hr, x, y, Gfx.GOLD);
			y += 12;
			com.buskohunter.client.hud.Gfx.bar(g, x, y, 160, 5, HunterRank.progress(hub.hrPoints()), 0xFFE8C060, 0xFF302810);
			y += 12;
			Gfx.text(g, "所持金 " + hub.coins() + "z   狩猟数 " + hub.totalHunts(), x, y, Gfx.WHITE);
			y += 16;
			String[] help = {
				"【遊び方】",
				"1. 集会所のクエストカウンターでクエストを受注（仲間は「参加する」）",
				"2. 装備ボックスで武器と属性シリーズ・防具を選ぶ",
				"3. 食堂で食事、アイテムボックスで道具を受け取る",
				"4. リーダーが「出発！」でフィールドへ",
				"5. モンスターを探して戦う。弱ると足を引きずり巣へ逃げる",
				"6. 討伐するか、罠＋捕獲用麻酔玉2発で捕獲！",
				"【基本操作】 左/右クリック:攻撃  R:特殊  Shift かジャンプ2回:回避  右長押し:ガード（盾のある武器）",
				"V:カメラ切替  H:このメニュー  ホットバーの道具は右クリックで使用",
			};
			for (String s : help) {
				y = wrap(g, s, x, y, pw - 20, s.startsWith("【") ? Gfx.GOLD : 0xFFD0D0D0) + 1;
			}
		}
	}

	private void drawMap(GuiGraphicsExtractor g, Payloads.QuestSync q) {
		int size = Math.min(190, ph - 40);
		int mx = px + 10, my = py + 26;
		g.fill(mx, my, mx + size, my + size, 0xFF203020);
		g.outline(mx - 1, my - 1, size + 2, size + 2, Gfx.GOLD_DARK);
		int cx = q.campX(), cz = q.campZ();
		int range = 60;
		for (int i = 0; i < q.areaX().length; i++) {
			range = Math.max(range, Math.max(Math.abs(q.areaX()[i] - cx), Math.abs(q.areaZ()[i] - cz)) + 25);
		}
		float scale = (size / 2f) / range;
		int ox = mx + size / 2, oz = my + size / 2;
		for (int i = 0; i < q.areaX().length; i++) {
			int ax = ox + Math.round((q.areaX()[i] - cx) * scale);
			int az = oz + Math.round((q.areaZ()[i] - cz) * scale);
			int r = Math.max(8, Math.round(22 * scale));
			g.fill(ax - r, az - r, ax + r, az + r, 0x4060A060);
			g.outline(ax - r, az - r, r * 2, r * 2, 0xA090D090);
			Gfx.center(g, String.valueOf(i + 1), ax, az - 4, 0xFFE0FFE0);
		}
		Gfx.icon(g, "gui/map_camp", ox - 6, oz - 6, 12);
		for (Payloads.QuestSync.MonsterInfo m : q.monsters()) {
			if (!m.has(Payloads.QuestSync.MonsterInfo.DISCOVERED) || m.has(Payloads.QuestSync.MonsterInfo.CAPTURED)) continue;
			int x = ox + Math.round((m.x() - cx) * scale);
			int z = oz + Math.round((m.z() - cz) * scale);
			Gfx.icon(g, "gui/map_monster", x - 6, z - 6, 12);
		}
		var player = minecraft.player;
		if (player != null) {
			int x = ox + Math.round(((int) player.getX() - cx) * scale);
			int z = oz + Math.round(((int) player.getZ() - cz) * scale);
			g.fill(x - 2, z - 2, x + 2, z + 2, 0xFFFFFFFF);
			int area = HunterHud.nearestArea(q, (int) player.getX(), (int) player.getZ());
			Gfx.text(g, area < 0 ? "現在地: ベースキャンプ" : "現在地: エリア" + (area + 1), mx, my + size + 4, Gfx.WHITE);
		}
	}
}
