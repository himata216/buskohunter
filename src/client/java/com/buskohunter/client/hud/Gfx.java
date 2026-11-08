package com.buskohunter.client.hud;

import com.buskohunter.BuskoHunter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** HUD / 画面の描画ヘルパー（モンハン風の枠やゲージ）。 */
public final class Gfx {
	private Gfx() {
	}

	public static final int PANEL = 0xC0101418;
	public static final int PANEL_LIGHT = 0xD0202830;
	public static final int GOLD = 0xFFE8C060;
	public static final int GOLD_DARK = 0xFF8A6A28;
	public static final int WHITE = 0xFFFFFFFF;
	public static final int GREY = 0xFFA0A0A0;
	public static final int RED = 0xFFE04040;
	public static final int GREEN = 0xFF50D040;
	public static final int YELLOW = 0xFFF0D040;

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	public static Identifier tex(String path) {
		return BuskoHunter.id("textures/" + path + ".png");
	}

	/** 金縁のパネル。 */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, PANEL);
		g.outline(x, y, w, h, GOLD_DARK);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
	}

	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int bg, int border) {
		g.fill(x, y, x + w, y + h, bg);
		g.outline(x, y, w, h, border);
	}

	/** ゲージ（背景＋中身＋枠）。 */
	public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float ratio, int color, int back) {
		ratio = Math.max(0f, Math.min(1f, ratio));
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
		g.fill(x, y, x + w, y + h, back);
		int fw = Math.round(w * ratio);
		if (fw > 0) {
			g.fill(x, y, x + fw, y + h, color);
			g.fill(x, y, x + fw, y + 1, lighten(color, 0.35f));
			g.fill(x, y + h - 1, x + fw, y + h, darken(color, 0.35f));
		}
	}

	public static void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
		g.text(font(), s, x, y, color, true);
	}

	public static void center(GuiGraphicsExtractor g, String s, int cx, int y, int color) {
		g.centeredText(font(), s, cx, y, color);
	}

	/** 拡大文字。 */
	public static void bigText(GuiGraphicsExtractor g, String s, int cx, int y, float scale, int color) {
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, y);
		pose.scale(scale, scale);
		int w = font().width(s);
		g.text(font(), s, -w / 2, 0, color, true);
		pose.popMatrix();
	}

	public static void icon(GuiGraphicsExtractor g, String path, int x, int y, int size) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex(path), x, y, 0f, 0f, size, size, size, size);
	}

	public static void icon(GuiGraphicsExtractor g, String path, int x, int y, int drawSize, int texSize) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex(path), x, y, 0f, 0f, drawSize, drawSize, texSize, texSize, texSize, texSize);
	}

	public static int alpha(int argb, float a) {
		int al = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * a)));
		return (al << 24) | (argb & 0xFFFFFF);
	}

	public static int opaque(int rgb) {
		return 0xFF000000 | (rgb & 0xFFFFFF);
	}

	public static int lighten(int argb, float f) {
		int a = argb >>> 24, r = (argb >> 16) & 255, gg = (argb >> 8) & 255, b = argb & 255;
		r = Math.min(255, (int) (r + (255 - r) * f));
		gg = Math.min(255, (int) (gg + (255 - gg) * f));
		b = Math.min(255, (int) (b + (255 - b) * f));
		return (a << 24) | (r << 16) | (gg << 8) | b;
	}

	public static int darken(int argb, float f) {
		int a = argb >>> 24, r = (argb >> 16) & 255, gg = (argb >> 8) & 255, b = argb & 255;
		r = (int) (r * (1 - f));
		gg = (int) (gg * (1 - f));
		b = (int) (b * (1 - f));
		return (a << 24) | (r << 16) | (gg << 8) | b;
	}

	public static String time(int ticks) {
		int sec = Math.max(0, ticks / 20);
		return String.format("%02d:%02d", sec / 60, sec % 60);
	}
}
