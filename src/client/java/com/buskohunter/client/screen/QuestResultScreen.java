package com.buskohunter.client.screen;

import com.buskohunter.client.audio.ClientSounds;
import com.buskohunter.client.hud.Gfx;
import com.buskohunter.core.QuestDef;
import com.buskohunter.net.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** クエスト結果（報酬）画面。 */
public class QuestResultScreen extends BhScreen {
	private final Payloads.Result result;
	private final long opened = System.currentTimeMillis();
	private int revealed;

	public QuestResultScreen(Payloads.Result result) {
		super(result.success() ? "クエスト報酬" : "クエスト結果");
		this.result = result;
	}

	@Override
	protected void init() {
		super.init();
		button("集会所へ戻る", px + pw - 128, py + ph - 26, 120, this::onClose);
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int mx, int my) {
		QuestDef def = QuestDef.byIndex(result.questIndex());
		int x = px + 12, y = py + 28;
		Gfx.text(g, def.title(), x, y, Gfx.GOLD);
		y += 14;
		if (result.success()) {
			Gfx.bigText(g, "QUEST CLEAR", px + pw / 2, y, 2.2f, 0xFFFFD040);
			y += 26;
			Gfx.center(g, "クリアタイム " + Gfx.time(result.timeTicks()), px + pw / 2, y, Gfx.WHITE);
		} else {
			Gfx.bigText(g, "QUEST FAILED", px + pw / 2, y, 2.2f, 0xFFE04040);
			y += 26;
		}
		y += 16;
		Gfx.text(g, "報酬金 " + result.coins() + "z    ハンターランクポイント +" + result.points(), x, y, Gfx.WHITE);
		y += 12;
		if (result.hrAfter() > result.hrBefore()) {
			Gfx.text(g, "HR が " + result.hrBefore() + " → " + result.hrAfter() + " に上がった！", x, y, 0xFF80FF80);
			y += 12;
		}
		y += 4;
		int show = (int) Math.min(result.lines().size(), (System.currentTimeMillis() - opened) / 350);
		if (show > revealed) {
			revealed = show;
			ClientSounds.ui("ui.reward", 1.0f + revealed * 0.05f);
		}
		for (int i = 0; i < show; i++) {
			Gfx.text(g, "・" + result.lines().get(i), x + 6, y, 0xFFFFE0A0);
			y += 11;
		}
	}
}
