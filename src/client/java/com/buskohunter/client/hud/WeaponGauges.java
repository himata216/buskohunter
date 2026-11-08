package com.buskohunter.client.hud;

import com.buskohunter.hunter.HunterState;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 武器種ごとの固有ゲージ（練気・ビン・弾・鬼人化・エキス・音符など）。 */
public final class WeaponGauges {
	private WeaponGauges() {
	}

	public static void draw(GuiGraphicsExtractor g, int x, int y, HunterState s) {
		switch (s.weapon) {
			case GREAT_SWORD -> {
				String[] names = {"縦斬り", "強溜め", "真溜め"};
				Gfx.text(g, "溜め段階: " + names[Math.min(2, s.gsChain)] + (s.holding ? "  溜めLv" + s.chargeLevel : ""), x, y, Gfx.WHITE);
			}
			case LONG_SWORD -> {
				int col = switch (s.spiritLevel) {
					case 1 -> 0xFFF0F0F0;
					case 2 -> 0xFFF0D040;
					case 3 -> 0xFFE04040;
					default -> 0xFF606060;
				};
				Gfx.bar(g, x, y + 1, 80, 5, s.spirit / 100f, 0xFFE05030, 0xFF301010);
				g.fill(x + 84, y, x + 92, y + 8, col);
				g.outline(x + 83, y - 1, 10, 10, 0xFF000000);
				Gfx.text(g, "練気", x + 96, y - 1, col);
			}
			case GUNLANCE -> {
				for (int i = 0; i < 5; i++) {
					int c = i < s.shells ? 0xFFE0A040 : 0xFF404040;
					g.fill(x + i * 9, y, x + i * 9 + 6, y + 8, c);
				}
				String wf = s.wyvernCooldown > 0 ? "竜撃砲 " + (s.wyvernCooldown / 20) + "s" : "竜撃砲 OK";
				Gfx.text(g, wf, x + 50, y, s.wyvernCooldown > 0 ? Gfx.GREY : 0xFFFF8040);
			}
			case SWITCH_AXE -> {
				Gfx.bar(g, x, y + 1, 90, 5, s.saGauge / 100f, s.saSword ? 0xFFE04080 : 0xFF40A0E0, 0xFF101830);
				Gfx.text(g, s.saSword ? "剣モード" : "斧モード", x + 96, y - 1, s.saSword ? 0xFFFF80C0 : 0xFF80C0FF);
			}
			case CHARGE_BLADE -> {
				int ec = s.cbEnergy >= 80 ? 0xFFE04040 : (s.cbEnergy >= 50 ? 0xFFF0D040 : 0xFF808080);
				Gfx.bar(g, x, y + 1, 50, 5, s.cbEnergy / 100f, ec, 0xFF202020);
				for (int i = 0; i < 5; i++) {
					int c = i < s.phials ? 0xFFF0C030 : 0xFF404040;
					g.fill(x + 56 + i * 8, y, x + 62 + i * 8, y + 8, c);
				}
				Gfx.text(g, s.cbAxe ? "斧" : "剣", x + 100, y - 1, Gfx.WHITE);
			}
			case DUAL_BLADES -> {
				Gfx.text(g, s.demonMode ? "鬼人化中！" : "鬼人化: R", x, y - 1, s.demonMode ? 0xFFFF4040 : Gfx.GREY);
			}
			case INSECT_GLAIVE -> {
				g.fill(x, y, x + 10, y + 8, s.extractRed > 0 ? 0xFFE03030 : 0xFF402020);
				g.fill(x + 13, y, x + 23, y + 8, s.extractWhite > 0 ? 0xFFF0F0F0 : 0xFF404040);
				g.fill(x + 26, y, x + 36, y + 8, s.extractOrange > 0 ? 0xFFF09030 : 0xFF403020);
				Gfx.text(g, s.kinsectOut ? "猟虫：飛行中" : "猟虫：待機", x + 42, y - 1, Gfx.WHITE);
			}
			case LIGHT_BOWGUN, HEAVY_BOWGUN -> {
				for (int i = 0; i < s.currentAmmo().magazine; i++) {
					g.fill(x + i * 5, y, x + i * 5 + 3, y + 8, i < s.ammo ? 0xFFE0C060 : 0xFF404040);
				}
				if (s.weapon == com.buskohunter.core.WeaponType.HEAVY_BOWGUN) {
					Gfx.bar(g, x + 60, y + 2, 50, 4, s.gatlingGauge / 100f, 0xFF40E0E0, 0xFF102020);
				}
			}
			case BOW -> {
				Gfx.text(g, s.holding ? "溜めLv " + s.chargeLevel : "竜の一矢 " + (s.piercerCooldown > 0 ? (s.piercerCooldown / 20) + "s" : "OK"),
					x, y - 1, s.holding ? 0xFFFFD040 : Gfx.WHITE);
			}
			case HUNTING_HORN -> {
				int[] colors = {0xFF000000, 0xFFC060F0, 0xFFE04040, 0xFF40A0F0};
				for (int i = 0; i < 3; i++) {
					int c = i < s.noteCount ? colors[Math.max(0, Math.min(3, s.notes[i]))] : 0xFF303030;
					g.fill(x + i * 12, y, x + i * 12 + 9, y + 8, c);
				}
				Gfx.text(g, "♪ 演奏: しゃがみ+左", x + 40, y - 1, Gfx.GREY);
			}
			case HAMMER -> Gfx.text(g, s.holding ? "溜めLv " + s.chargeLevel : "右長押しで溜め", x, y - 1,
				s.holding ? 0xFFFFD040 : Gfx.GREY);
			case LANCE -> Gfx.text(g, "しゃがみでガード / R で突進", x, y - 1, Gfx.GREY);
			case SWORD_AND_SHIELD -> Gfx.text(g, "しゃがみでガード / R で盾攻撃", x, y - 1, Gfx.GREY);
		}
	}
}
