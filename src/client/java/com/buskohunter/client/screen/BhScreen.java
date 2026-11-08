package com.buskohunter.client.screen;

import com.buskohunter.net.Net;
import com.buskohunter.client.audio.ClientSounds;
import com.buskohunter.client.hud.Gfx;
import com.buskohunter.net.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ブスコハンターの画面の共通部分（モンハン風の金縁パネル・クリック可能な行・効果音）。
 */
public abstract class BhScreen extends Screen {
	/** クリック可能な領域。 */
	protected record Hot(int x, int y, int w, int h, Runnable action) {
		boolean inside(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	protected final List<Hot> hots = new ArrayList<>();
	protected int px, py, pw, ph;

	protected BhScreen(String title) {
		super(Component.literal(title));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		pw = Math.min(width - 20, 420);
		ph = Math.min(height - 20, 250);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		if (!opened) {
			opened = true;
			ClientSounds.ui("ui.open", 1.0f);
		}
	}

	private boolean opened;

	protected Button button(String label, int x, int y, int w, Runnable action) {
		Button b = Button.builder(Component.literal(label), btn -> {
			ClientSounds.ui("ui.confirm", 1.0f);
			action.run();
		}).bounds(x, y, w, 20).build();
		addRenderableWidget(b);
		return b;
	}

	protected static void send(int action, int a, int b) {
		Net.sendToServer(new Payloads.Action(action, a, b));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fillGradient(0, 0, width, height, 0x90000000, 0xB0101010);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		hots.clear();
		// 枠
		Gfx.panel(g, px, py, pw, ph, 0xE8141A20, Gfx.GOLD_DARK);
		g.fill(px + 2, py + 2, px + pw - 2, py + 18, 0xFF2A2218);
		g.fill(px + 2, py + 18, px + pw - 2, py + 19, Gfx.GOLD);
		Gfx.text(g, "◆ " + title.getString(), px + 8, py + 6, Gfx.GOLD);
		drawContent(g, mouseX, mouseY);
		super.extractRenderState(g, mouseX, mouseY, delta);
	}

	protected abstract void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY);

	/** 選択できる行（選択中は金色）。 */
	protected void row(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected, boolean enabled,
					   int mouseX, int mouseY, Runnable onClick) {
		boolean hover = mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
		int bg = selected ? 0xFF4A3A18 : (hover && enabled ? 0xFF2A3038 : 0xFF1C2026);
		g.fill(x, y, x + w, y + h, bg);
		g.outline(x, y, w, h, selected ? Gfx.GOLD : 0xFF3A4048);
		if (enabled) hots.add(new Hot(x, y, w, h, onClick));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (Hot h : new ArrayList<>(hots)) {
			if (h.inside(event.x(), event.y())) {
				ClientSounds.ui("ui.select", 1.0f);
				h.action.run();
				rebuildWidgets();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClose() {
		ClientSounds.ui("ui.back", 1.0f);
		super.onClose();
	}

	/** 折り返し描画（返り値は次の y）。 */
	protected int wrap(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
		return g.textWithWordWrap(font, Component.literal(text), x, y, width, color);
	}
}
