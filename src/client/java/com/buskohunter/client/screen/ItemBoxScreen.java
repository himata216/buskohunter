package com.buskohunter.client.screen;

import com.buskohunter.client.ClientHunter;
import com.buskohunter.client.hud.Gfx;
import com.buskohunter.core.HuntItem;
import com.buskohunter.net.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** アイテムボックス：持っていく狩猟アイテムの数を決めて受け取る。 */
public class ItemBoxScreen extends BhScreen {
	private final int[] carry = new int[HuntItem.values().length];

	public ItemBoxScreen() {
		super("アイテムボックス");
		int[] c = ClientHunter.HUB.carry();
		for (int i = 0; i < carry.length; i++) {
			carry[i] = i < c.length ? c[i] : defaultCarry(HuntItem.values()[i]);
		}
	}

	private static int defaultCarry(HuntItem item) {
		return switch (item) {
			case POTION -> 10;
			case MEGA_POTION -> 5;
			case WHETSTONE -> 20;
			case RATION -> 5;
			case PAINTBALL -> 5;
			case FLASH_BOMB -> 3;
			case SHOCK_TRAP, PITFALL_TRAP -> 1;
			case TRANQ_BOMB -> 2;
			case FARCASTER -> 1;
			default -> 0;
		};
	}

	@Override
	protected void init() {
		super.init();
		pw = Math.min(width - 16, 440);
		ph = Math.min(height - 16, 264);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		button("おすすめセット", px + 8, py + ph - 26, 100, () -> {
			for (int i = 0; i < carry.length; i++) carry[i] = defaultCarry(HuntItem.values()[i]);
		});
		button("この内容で受け取る", px + pw - 138, py + ph - 26, 130, () -> {
			for (int i = 0; i < carry.length; i++) send(Payloads.Action.SET_CARRY, i, carry[i]);
			send(Payloads.Action.TAKE_SUPPLIES, 0, 0);
			onClose();
		});
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int mx, int my) {
		HuntItem[] items = HuntItem.values();
		int cols = 2;
		int colW = (pw - 24) / cols;
		for (int i = 0; i < items.length; i++) {
			HuntItem it = items[i];
			int x = px + 8 + (i % cols) * (colW + 8);
			int y = py + 24 + (i / cols) * 22;
			g.fill(x, y, x + colW, y + 20, 0xFF1C2026);
			g.outline(x, y, colW, 20, 0xFF3A4048);
			Gfx.icon(g, "item/" + it.id, x + 2, y + 2, 16);
			Gfx.text(g, it.jaName, x + 21, y + 2, Gfx.WHITE);
			Gfx.text(g, it.description, x + 21, y + 11, 0xFF909090);
			int idx = i;
			int bx = x + colW - 50;
			row(g, bx, y + 3, 12, 14, false, true, mx, my, () -> carry[idx] = Math.max(0, carry[idx] - 1));
			Gfx.center(g, "-", bx + 6, y + 6, Gfx.WHITE);
			Gfx.center(g, carry[i] + "/" + it.maxCarry, bx + 24, y + 6, carry[i] > 0 ? Gfx.GOLD : Gfx.GREY);
			row(g, bx + 36, y + 3, 12, 14, false, true, mx, my, () -> carry[idx] = Math.min(items[idx].maxCarry, carry[idx] + 1));
			Gfx.center(g, "+", bx + 42, y + 6, Gfx.WHITE);
		}
	}
}
