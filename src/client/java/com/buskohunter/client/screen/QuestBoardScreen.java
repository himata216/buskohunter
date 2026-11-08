package com.buskohunter.client.screen;

import com.buskohunter.client.ClientHunter;
import com.buskohunter.client.audio.ClientSounds;
import com.buskohunter.client.hud.Gfx;
import com.buskohunter.core.HunterRank;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.QuestDef;
import com.buskohunter.net.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** クエストカウンター：受注・参加・出発。 */
public class QuestBoardScreen extends BhScreen {
	private QuestDef.Rank tab = QuestDef.Rank.LOW;
	private int selected = 0;
	private int scroll = 0;

	public QuestBoardScreen() {
		super("クエストカウンター");
	}

	@Override
	protected void init() {
		super.init();
		pw = Math.min(width - 16, 460);
		ph = Math.min(height - 16, 260);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		Payloads.HubSync hub = ClientHunter.HUB;
		int bx = px + 190;
		int by = py + ph - 26;
		Payloads.HubSync.SessionInfo mine = mySession();
		QuestDef q = QuestDef.byIndex(selected);
		boolean canAccept = HunterRank.canAccept(hub.hrPoints(), q);
		if (mine == null) {
			var b = button("このクエストを受注", bx, by, 130, () -> {
				ClientSounds.ui("ui.accept", 1.0f);
				send(Payloads.Action.ACCEPT_QUEST, selected, 0);
			});
			b.active = canAccept;
			Payloads.HubSync.SessionInfo joinable = joinableFor(selected);
			if (joinable != null) {
				button("参加する", bx + 136, by, 80, () -> send(Payloads.Action.JOIN_SESSION, joinable.id(), 0));
			}
		} else {
			boolean leader = mine.leader().equals(minecraft.player.getName().getString());
			var dep = button(leader ? "出発！" : "リーダーの出発待ち", bx, by, 130, () -> {
				send(Payloads.Action.DEPART, 0, 0);
				onClose();
			});
			dep.active = leader;
			button("受注をやめる", bx + 136, by, 90, () -> send(Payloads.Action.LEAVE_SESSION, 0, 0));
		}
	}

	private Payloads.HubSync.SessionInfo mySession() {
		for (Payloads.HubSync.SessionInfo s : ClientHunter.HUB.sessions()) {
			if (s.id() == ClientHunter.HUB.mySession()) return s;
		}
		return null;
	}

	private Payloads.HubSync.SessionInfo joinableFor(int questIndex) {
		for (Payloads.HubSync.SessionInfo s : ClientHunter.HUB.sessions()) {
			if (s.questIndex() == questIndex && s.state() == Payloads.QuestSync.PREPARING && s.members().size() < 4) return s;
		}
		return null;
	}

	private List<QuestDef> listed() {
		List<QuestDef> out = new ArrayList<>();
		for (QuestDef q : QuestDef.all()) {
			if (q.rank() == tab) out.add(q);
		}
		return out;
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int mx, int my) {
		Payloads.HubSync hub = ClientHunter.HUB;
		int hr = HunterRank.hr(hub.hrPoints());
		Gfx.text(g, "HR " + hr + "   所持金 " + hub.coins() + "z   狩猟数 " + hub.totalHunts(), px + pw - 200, py + 6, Gfx.WHITE);

		// タブ
		int tx = px + 8;
		for (QuestDef.Rank r : QuestDef.Rank.values()) {
			boolean locked = hr < r.requiredHr;
			QuestDef.Rank rr = r;
			row(g, tx, py + 24, 54, 16, tab == r, !locked, mx, my, () -> {
				tab = rr;
				scroll = 0;
				List<QuestDef> l = listed();
				if (!l.isEmpty()) selected = l.get(0).index();
			});
			Gfx.center(g, (locked ? "× " : "") + r.jaName, tx + 27, py + 28, locked ? Gfx.GREY : Gfx.WHITE);
			tx += 58;
		}

		// クエスト一覧
		List<QuestDef> list = listed();
		int ly = py + 44;
		int rows = Math.max(1, (ph - 76) / 22);
		for (int i = scroll; i < list.size() && i < scroll + rows; i++) {
			QuestDef q = list.get(i);
			boolean cleared = (hub.clearedMask() & (1L << q.index())) != 0;
			boolean open = joinableFor(q.index()) != null;
			row(g, px + 8, ly, 172, 20, selected == q.index(), true, mx, my, () -> selected = q.index());
			Gfx.text(g, "★".repeat(Math.min(q.stars(), 8)), px + 12, ly + 2, 0xFFF0C040);
			Gfx.text(g, q.title(), px + 12, ly + 11, cleared ? 0xFFB0E0B0 : Gfx.WHITE);
			if (open) Gfx.text(g, "募集中", px + 140, ly + 2, 0xFF60E0FF);
			ly += 22;
		}
		if (list.size() > rows) {
			Gfx.text(g, "▲▼ ホイールでスクロール", px + 12, py + ph - 14, Gfx.GREY);
		}

		// 詳細
		QuestDef q = QuestDef.byIndex(selected);
		int dx = px + 190, dy = py + 24, dw = pw - 198;
		Gfx.panel(g, dx, dy, dw, ph - 56, 0xFF1A1F26, 0xFF3A4048);
		MonsterKind main = q.targets().get(0);
		// モンスターの肖像
		Gfx.icon(g, "gui/portrait_" + main.id, dx + dw - 70, dy + 6, 64);
		g.outline(dx + dw - 71, dy + 5, 66, 66, Gfx.GOLD_DARK);
		int y = dy + 6;
		Gfx.text(g, q.rank().jaName + " ★" + q.stars() + "  " + q.title(), dx + 6, y, Gfx.GOLD);
		y += 12;
		Gfx.text(g, "目的: " + q.goalText(), dx + 6, y, Gfx.WHITE);
		y += 11;
		Gfx.text(g, "場所: " + q.field().jaName + "  制限時間 " + q.timeMinutes() + "分", dx + 6, y, 0xFFC0D0E0);
		y += 11;
		Gfx.text(g, "報酬: " + q.rewardCoins() + "z / " + q.rewardPoints() + "HRP", dx + 6, y, 0xFFE0D0A0);
		y += 11;
		Gfx.text(g, "依頼主: " + q.client(), dx + 6, y, Gfx.GREY);
		y += 14;
		y = wrap(g, q.description(), dx + 6, y, dw - 84, 0xFFE0E0E0) + 4;
		for (MonsterKind k : q.targets()) {
			if (y > dy + ph - 80) break;
			Gfx.text(g, k.fullName(), dx + 6, y, 0xFFFFB060);
			y += 10;
			String weak = "弱点: 火" + k.weaknessStars(com.buskohunter.core.ElementType.FIRE)
				+ " 水" + k.weaknessStars(com.buskohunter.core.ElementType.WATER)
				+ " 雷" + k.weaknessStars(com.buskohunter.core.ElementType.THUNDER)
				+ " 氷" + k.weaknessStars(com.buskohunter.core.ElementType.ICE)
				+ " 龍" + k.weaknessStars(com.buskohunter.core.ElementType.DRAGON);
			Gfx.text(g, weak, dx + 10, y, 0xFFC0C0C0);
			y += 11;
		}
		if (!HunterRank.canAccept(hub.hrPoints(), q)) {
			Gfx.text(g, "HR" + q.rank().requiredHr + " 以上で受注できます", dx + 6, dy + ph - 72, Gfx.RED);
		}

		// 自分のパーティ
		Payloads.HubSync.SessionInfo mine = mySession();
		if (mine != null) {
			int sy = py + ph - 48;
			Gfx.text(g, "受注中: " + QuestDef.byIndex(mine.questIndex()).title() + "  メンバー: " + String.join("、", mine.members()),
				dx, sy, 0xFF80FF80);
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double sx, double sy) {
		int rows = Math.max(1, (ph - 76) / 22);
		int max = Math.max(0, listed().size() - rows);
		scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(sy)));
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		// 募集状況が変わったらボタンを作り直す
		if (lastHubHash != ClientHunter.HUB.hashCode()) {
			lastHubHash = ClientHunter.HUB.hashCode();
			rebuildWidgets();
		}
	}

	private int lastHubHash;
}
