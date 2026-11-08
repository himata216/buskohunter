package com.buskohunter.client.screen;

import com.buskohunter.client.ClientHunter;
import com.buskohunter.client.hud.Gfx;
import com.buskohunter.core.Meal;
import com.buskohunter.net.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 集会所の食事場：1 クエストに 1 回、食事で強化。 */
public class CanteenScreen extends BhScreen {
	private Meal selected = Meal.BUSKO_NABE;

	public CanteenScreen() {
		super("ブスコ食堂");
	}

	@Override
	protected void init() {
		super.init();
		var b = button(ClientHunter.HUB.mealEaten() ? "もう食べました" : "いただきます！", px + pw - 128, py + ph - 26, 120, () -> {
			send(Payloads.Action.EAT_MEAL, selected.ordinal(), 0);
			onClose();
		});
		b.active = !ClientHunter.HUB.mealEaten();
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int mx, int my) {
		int x = px + 8, y = py + 26;
		Gfx.text(g, "クエストを受注してから食べると効果がクエスト中ずっと続きます。", x, y, Gfx.GREY);
		y += 14;
		Meal[] meals = Meal.values();
		for (Meal m : meals) {
			if (m == Meal.NONE) continue;
			row(g, x, y, 170, 24, selected == m, true, mx, my, () -> selected = m);
			g.fill(x + 3, y + 3, x + 21, y + 21, Gfx.opaque(m.color));
			g.outline(x + 3, y + 3, 18, 18, 0xFF000000);
			Gfx.text(g, m.jaName, x + 26, y + 8, Gfx.WHITE);
			y += 26;
		}
		int dx = px + 186, dy = py + 40, dw = pw - 194;
		Gfx.panel(g, dx, dy, dw, ph - 74, 0xFF1A1F26, 0xFF3A4048);
		Gfx.text(g, selected.jaName, dx + 6, dy + 6, Gfx.opaque(selected.color));
		int yy = wrap(g, selected.describe(), dx + 6, dy + 22, dw - 12, Gfx.WHITE) + 6;
		Gfx.text(g, "食事スキル", dx + 6, yy, Gfx.GOLD);
		yy += 11;
		wrap(g, selected.skill.jaName, dx + 10, yy, dw - 16, 0xFF80FF80);
		Meal eaten = Meal.byOrdinal(ClientHunter.HUB.meal());
		if (ClientHunter.HUB.mealEaten() && eaten != Meal.NONE) {
			Gfx.text(g, "食事済み: " + eaten.jaName, dx + 6, dy + ph - 92, 0xFFFFD060);
		}
	}
}
