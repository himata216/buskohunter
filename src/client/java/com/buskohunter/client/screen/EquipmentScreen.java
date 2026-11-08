package com.buskohunter.client.screen;

import com.buskohunter.client.ClientHunter;
import com.buskohunter.client.hud.Gfx;
import com.buskohunter.core.ArmorSeries;
import com.buskohunter.core.ArmorSkill;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.Sharpness;
import com.buskohunter.core.WeaponStats;
import com.buskohunter.core.WeaponType;
import com.buskohunter.net.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 装備ボックス：武器種 × 属性シリーズ、防具シリーズを選ぶ。 */
public class EquipmentScreen extends BhScreen {
	private WeaponType weapon;
	private ElementType element;
	private ArmorSeries armor;
	private int tab;

	public EquipmentScreen() {
		super("装備ボックス");
		Payloads.HubSync hub = ClientHunter.HUB;
		weapon = WeaponType.byOrdinal(hub.weapon());
		element = ElementType.byOrdinal(hub.element());
		if (element == ElementType.NONE) element = ElementType.FIRE;
		armor = ArmorSeries.byOrdinal(hub.armor());
	}

	@Override
	protected void init() {
		super.init();
		pw = Math.min(width - 16, 470);
		ph = Math.min(height - 16, 262);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		button("武器", px + 8, py + 24, 60, () -> tab = 0);
		button("防具", px + 72, py + 24, 60, () -> tab = 1);
		button("この装備にする", px + pw - 128, py + ph - 26, 120, () -> {
			send(Payloads.Action.SET_WEAPON, weapon.ordinal(), element.ordinal());
			send(Payloads.Action.SET_ARMOR, armor.ordinal(), 0);
			onClose();
		});
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int mx, int my) {
		if (tab == 0) drawWeapons(g, mx, my);
		else drawArmor(g, mx, my);
	}

	private void drawWeapons(GuiGraphicsExtractor g, int mx, int my) {
		// 武器種（14）
		int gx = px + 8, gy = py + 48;
		Gfx.text(g, "武器種", gx, gy, Gfx.GOLD);
		gy += 11;
		WeaponType[] types = WeaponType.values();
		for (int i = 0; i < types.length; i++) {
			WeaponType t = types[i];
			int x = gx + (i % 2) * 92;
			int y = gy + (i / 2) * 20;
			row(g, x, y, 90, 18, weapon == t, true, mx, my, () -> weapon = t);
			Gfx.icon(g, "gui/weapon_" + t.id, x + 2, y + 1, 16);
			Gfx.text(g, t.jaName, x + 21, y + 5, Gfx.WHITE);
		}
		// 属性シリーズ（10）
		int ex = px + 196, ey = py + 48;
		Gfx.text(g, "属性シリーズ", ex, ey, Gfx.GOLD);
		ey += 11;
		for (int i = 0; i < ElementType.SERIES.length; i++) {
			ElementType e = ElementType.SERIES[i];
			int x = ex + (i % 2) * 64;
			int y = ey + (i / 2) * 20;
			row(g, x, y, 62, 18, element == e, true, mx, my, () -> element = e);
			Gfx.icon(g, "gui/element_" + e.id, x + 2, y + 1, 16);
			Gfx.text(g, e.jaName, x + 21, y + 5, Gfx.opaque(e.color));
		}
		// 性能
		WeaponStats st = WeaponStats.of(weapon, element);
		int sx = px + 330, sy = py + 48;
		int sw = pw - (sx - px) - 8;
		Gfx.panel(g, sx, sy, sw, ph - 82, 0xFF1A1F26, 0xFF3A4048);
		int y = sy + 4;
		Gfx.text(g, st.name(), sx + 4, y, Gfx.opaque(element.color));
		y += 12;
		Gfx.text(g, "攻撃力 " + st.displayAttack(), sx + 4, y, Gfx.WHITE);
		y += 10;
		Gfx.text(g, element.jaName + " " + st.displayElement(), sx + 4, y, Gfx.opaque(element.color));
		y += 10;
		Gfx.text(g, "会心率 " + (st.affinity() > 0 ? "+" : "") + st.affinity() + "%", sx + 4, y,
			st.affinity() < 0 ? Gfx.RED : Gfx.WHITE);
		y += 12;
		if (!weapon.ranged) {
			Sharpness sh = st.sharpness();
			int total = Math.max(1, sh.total());
			int bw = sw - 10, cx = sx + 4;
			for (Sharpness.Color c : Sharpness.Color.values()) {
				int len = sh.length(c);
				if (len <= 0) continue;
				int segW = Math.max(1, Math.round(bw * (float) len / total));
				g.fill(cx, y, cx + segW, y + 5, Gfx.opaque(c.rgb));
				cx += segW;
			}
			y += 9;
		}
		y = wrap(g, st.specialNote(), sx + 4, y, sw - 8, 0xFFC0C0C0) + 3;
		Gfx.text(g, "操作:", sx + 4, y, Gfx.GOLD);
		y += 10;
		for (String c : weapon.controls()) {
			if (y > py + ph - 40) break;
			y = wrap(g, c, sx + 4, y, sw - 8, 0xFFD0D0D0);
		}
	}

	private void drawArmor(GuiGraphicsExtractor g, int mx, int my) {
		int gx = px + 8, gy = py + 48;
		Gfx.text(g, "防具シリーズ", gx, gy, Gfx.GOLD);
		gy += 11;
		for (int i = 0; i < ArmorSeries.values().length; i++) {
			ArmorSeries a = ArmorSeries.values()[i];
			int y = gy + i * 24;
			row(g, gx, y, 150, 22, armor == a, true, mx, my, () -> armor = a);
			g.fill(gx + 3, y + 3, gx + 19, y + 19, Gfx.opaque(a.color));
			Gfx.icon(g, "item/" + a.id + "_chestplate", gx + 3, y + 3, 16);
			Gfx.text(g, a.jaName + "シリーズ", gx + 24, y + 7, Gfx.WHITE);
		}
		int sx = px + 170, sy = py + 48;
		int sw = pw - 178;
		Gfx.panel(g, sx, sy, sw, ph - 82, 0xFF1A1F26, 0xFF3A4048);
		int y = sy + 4;
		Gfx.text(g, armor.jaName + "シリーズ", sx + 4, y, Gfx.opaque(armor.color));
		y += 14;
		String[] el = {"火", "水", "雷", "氷", "龍"};
		StringBuilder res = new StringBuilder("耐性: ");
		for (int i = 0; i < 5; i++) {
			int v = armor.resistances[i];
			res.append(el[i]).append(v >= 0 ? "+" : "").append(v).append(" ");
		}
		Gfx.text(g, res.toString(), sx + 4, y, Gfx.WHITE);
		y += 14;
		Gfx.text(g, "発動スキル", sx + 4, y, Gfx.GOLD);
		y += 11;
		for (ArmorSkill s : armor.skills) {
			Gfx.text(g, "・" + s.jaName, sx + 8, y, 0xFF80FF80);
			y += 10;
			Gfx.text(g, "  " + s.description, sx + 8, y, 0xFFC0C0C0);
			y += 12;
		}
		y += 4;
		wrap(g, "防御力はクエストのランクに合わせて自動で調整されます（装備は集めるのではなく選ぶ方式）。", sx + 4, y, sw - 8, Gfx.GREY);
	}
}
