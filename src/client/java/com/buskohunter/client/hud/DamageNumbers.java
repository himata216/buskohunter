package com.buskohunter.client.hud;

import com.buskohunter.net.Payloads;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** ダメージ数値（ワールド座標 → 画面座標へ自前で投影して描く）。 */
public final class DamageNumbers {
	private DamageNumbers() {
	}

	private record Entry(double x, double y, double z, int amount, int flags, long start, float jx, float jz) {
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static final Random RNG = new Random();
	private static final long LIFE = 1100;

	public static void add(Payloads.DamageNumber d) {
		ENTRIES.add(new Entry(d.x(), d.y(), d.z(), d.amount(), d.flags(), System.currentTimeMillis(),
			(RNG.nextFloat() - 0.5f) * 0.8f, (RNG.nextFloat() - 0.5f) * 0.8f));
		while (ENTRIES.size() > 40) ENTRIES.remove(0);
	}

	public static void clear() {
		ENTRIES.clear();
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (ENTRIES.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		Camera cam = mc.gameRenderer.mainCamera();
		Vec3 cp = cam.position();
		double yaw = Math.toRadians(cam.yRot());
		double pitch = Math.toRadians(cam.xRot());
		double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
		double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
		double ux = -Math.sin(yaw) * Math.sin(pitch), uy = Math.cos(pitch), uz = Math.cos(yaw) * Math.sin(pitch);
		int w = g.guiWidth(), h = g.guiHeight();
		double fov = Math.toRadians(Math.max(30f, cam.getFov()));
		double focal = (h / 2.0) / Math.tan(fov / 2.0);
		long now = System.currentTimeMillis();
		Iterator<Entry> it = ENTRIES.iterator();
		while (it.hasNext()) {
			Entry e = it.next();
			float t = (now - e.start) / (float) LIFE;
			if (t >= 1f) {
				it.remove();
				continue;
			}
			double wx = e.x + e.jx, wy = e.y + t * 1.2, wz = e.z + e.jz;
			double dx = wx - cp.x, dy = wy - cp.y, dz = wz - cp.z;
			double zc = dx * fx + dy * fy + dz * fz;
			if (zc < 0.3) continue;
			double xc = dx * rx + dz * rz;
			double yc = dx * ux + dy * uy + dz * uz;
			int sx = (int) (w / 2.0 + xc / zc * focal);
			int sy = (int) (h / 2.0 - yc / zc * focal);
			boolean crit = (e.flags & Payloads.DamageNumber.CRIT) != 0;
			boolean weak = (e.flags & Payloads.DamageNumber.WEAK_SPOT) != 0;
			boolean bounce = (e.flags & Payloads.DamageNumber.BOUNCE) != 0;
			boolean status = (e.flags & Payloads.DamageNumber.STATUS) != 0;
			int color = weak ? 0xFFFFA020 : 0xFFFFFFFF;
			if (crit) color = 0xFFFF5050;
			if (bounce) color = 0xFF909090;
			if (status) color = 0xFFC080FF;
			float alpha = t > 0.7f ? (1f - t) / 0.3f : 1f;
			float scale = (crit || weak ? 1.6f : 1.2f) * (t < 0.12f ? 1.4f - t * 3.3f : 1f);
			String text = status ? "状態異常!" : String.valueOf(e.amount);
			Gfx.bigText(g, text, sx, sy, scale, Gfx.alpha(color, alpha));
		}
	}
}
