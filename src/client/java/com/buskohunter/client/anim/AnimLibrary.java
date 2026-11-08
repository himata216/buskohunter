package com.buskohunter.client.anim;

import com.buskohunter.combat.AnimKind;
import com.buskohunter.combat.InputKind;
import com.buskohunter.core.WeaponType;

import static com.buskohunter.client.anim.K.DEG;
import static com.buskohunter.client.anim.K.k;

/**
 * モンハン風プレイヤーモーション集。
 * 角度の向き（HumanoidModel）：腕の xRot は負で前・上へ、右腕の zRot は正で外側、左腕は負で外側。
 * body/root の yRot は正でモンスターから見て右へひねる。rootX は正で前傾。
 *
 * @param t        技の進行度 0〜1
 * @param holdFrac 溜め技で一時停止する位置（0〜1、溜めない技は -1）
 * @param age      見た目用の経過 tick（震え・ループ用）
 * @param dir      回避方向ビット（InputKind.DIR_*）
 */
public final class AnimLibrary {
	private AnimLibrary() {
	}

	public static void pose(AnimKind kind, float t, float holdFrac, boolean holding, float age, int dir, Pose p) {
		float shake = holding ? (float) Math.sin(age * 2.7f) * 1.6f * DEG : 0f;
		switch (kind) {
			case SLASH_DOWN -> {
				float a = k(t, 0, -35, 0.3f, -168, 0.45f, -55, 0.6f, -22, 1, -30);
				p.arms(a, -18 * DEG, 4 * DEG, a + 6 * DEG, 30 * DEG, -4 * DEG);
				p.rootX = k(t, 0, 0, 0.3f, -6, 0.5f, 14, 1, 3);
				p.root = true;
			}
			case SLASH_SIDE_R, HORN_SWING -> sideSwing(t, p, 1);
			case SLASH_SIDE_L -> sideSwing(t, p, -1);
			case WIDE_SWEEP -> {
				sideSwing(t, p, 1);
				p.bodyY *= 1.4f;
				p.rootX = k(t, 0, 0, 0.4f, 8, 1, 2);
				p.root = true;
			}
			case SLASH_UP -> {
				float a = k(t, 0, 25, 0.3f, 35, 0.5f, -150, 1, -60);
				p.arms(a, -15 * DEG, 0, a + 5 * DEG, 28 * DEG, 0);
				p.rootX = k(t, 0, 8, 0.4f, 10, 0.6f, -8, 1, 0);
				p.root = true;
			}
			case THRUST, GUNLANCE_SHELL, COUNTER_THRUST -> {
				float a = k(t, 0, -70, 0.25f, -60, 0.4f, -95, 1, -85);
				p.arms(a, k(t, 0, 10, 0.4f, -8, 1, 0), 0, -75 * DEG, 25 * DEG, -10 * DEG);
				p.body(0, k(t, 0, 0, 0.25f, 25, 0.45f, -15, 1, 0), 0);
				p.rootX = k(t, 0, 0, 0.3f, -4, 0.45f, 10, 1, 2);
				if (kind == AnimKind.GUNLANCE_SHELL) {
					p.rootX += k(t, 0.3f, 0, 0.36f, -14, 0.6f, 0);
				}
				if (kind == AnimKind.COUNTER_THRUST) {
					p.lArmX = k(t, 0, -80, 0.6f, -80, 1, -60);
				}
				p.root = true;
			}
			case THRUST_HIGH -> {
				float a = k(t, 0, -110, 0.25f, -100, 0.4f, -140, 1, -120);
				p.arms(a, k(t, 0, 10, 0.4f, -6, 1, 0), 0, -75 * DEG, 25 * DEG, -10 * DEG);
				p.body(0, k(t, 0, 0, 0.25f, 20, 0.45f, -10, 1, 0), 0);
			}
			case SPIN, SPIRIT_ROUNDSLASH -> {
				p.arms(-85 * DEG, 0, 40 * DEG, -85 * DEG, 0, -40 * DEG);
				p.rootYaw = k(t, 0, 0, 0.15f, 25, 0.6f, -360, 1, -360);
				p.rootX = k(t, 0, 0, 0.3f, 8, 1, 0);
				p.root = true;
			}
			case DOUBLE_SLASH -> {
				float x = k(t, 0, -150, 0.35f, -150, 0.6f, -40, 1, -50);
				float z = k(t, 0, 50, 0.35f, 55, 0.6f, -15, 1, 0);
				p.arms(x, 0, z * 1f, x, 0, -z);
			}
			case DUAL_FLURRY -> {
				float s = (float) Math.sin(t * Math.PI * 6);
				p.arms((-90 + 55 * s) * DEG, -10 * DEG, 20 * DEG, (-90 - 55 * s) * DEG, 10 * DEG, -20 * DEG);
				p.body(0, s * 15 * DEG, 0);
			}
			case DUAL_SPIN -> {
				float s = (float) Math.sin(t * Math.PI * 10);
				p.arms((-80 + 40 * s) * DEG, 0, 60 * DEG, (-80 - 40 * s) * DEG, 0, -60 * DEG);
				p.rootYaw = k(t, 0, 0, 1, -720);
				p.root = true;
			}
			case DUAL_WHEEL -> {
				p.arms(-120 * DEG, 0, 60 * DEG, -120 * DEG, 0, -60 * DEG);
				p.rootYaw = k(t, 0, 0, 0.8f, -360, 1, -360);
				p.rootY = K.kRaw(t, 0, 0, 0.4f, -3, 0.8f, 0);
				p.root = true;
			}
			case CHARGE_OVERHEAD -> {
				float hf = holdFrac > 0 ? holdFrac : 0.3f;
				float a = k(t, 0, -40, hf, -165, hf + 0.18f, -28, 1, -35);
				p.arms(a + shake, -20 * DEG, 8 * DEG, a + 5 * DEG + shake, 25 * DEG, -6 * DEG);
				p.rootX = k(t, 0, 0, hf, -10, hf + 0.18f, 18, 1, 5);
				p.rootY = holding ? 1.0f : 0f;
				p.root = true;
			}
			case CHARGE_LOW -> {
				p.arms(35 * DEG + shake, 0, 15 * DEG, 25 * DEG + shake, 30 * DEG, -10 * DEG);
				p.body(0, 30 * DEG, 0);
				p.rootY = 1.5f;
				p.root = true;
			}
			case SLAM, HORN_SLAM -> {
				float a = k(t, 0, -60, 0.35f, -170, 0.5f, -25, 1, -30);
				p.arms(a, -15 * DEG, 0, a + 5 * DEG, 25 * DEG, 0);
				p.rootX = k(t, 0, 0, 0.35f, -10, 0.5f, 22, 1, 6);
				p.rootY = K.kRaw(t, 0, 0, 0.5f, 0, 0.55f, 2.5f, 1, 0);
				p.root = true;
			}
			case UPPERCUT -> {
				float a = k(t, 0, 20, 0.35f, 45, 0.5f, -170, 1, -100);
				p.arms(a, -10 * DEG, 0, a + 5 * DEG, 25 * DEG, 0);
				p.body(0, k(t, 0, 30, 0.4f, 35, 0.55f, -20, 1, 0), 0);
				p.rootX = k(t, 0, 6, 0.4f, 12, 0.6f, -12, 1, 0);
				p.root = true;
			}
			case HOMERUN -> {
				p.arms(-80 * DEG, 0, 10 * DEG, -80 * DEG, 30 * DEG, -10 * DEG);
				p.body(0, k(t, 0, 0, 0.5f, 80, 0.7f, -110, 1, -60), 0);
				p.rootYaw = k(t, 0, 0, 0.5f, 20, 0.75f, -200, 1, -360);
				p.root = true;
			}
			case TACKLE -> {
				p.arms(-25 * DEG, 0, 15 * DEG, -40 * DEG, 20 * DEG, -15 * DEG);
				p.body(0, 35 * DEG, 0);
				p.rootX = k(t, 0, 0, 0.3f, 25, 1, 5);
				p.root = true;
			}
			case SHIELD_BASH -> {
				float l = k(t, 0, -50, 0.35f, -60, 0.5f, -95, 1, -70);
				p.arms(-40 * DEG, 0, 10 * DEG, l, -15 * DEG, -10 * DEG);
				p.body(0, k(t, 0, 0, 0.35f, 25, 0.5f, -20, 1, 0), 0);
				p.rootX = k(t, 0, 0, 0.5f, 10, 1, 0);
				p.root = true;
			}
			case SHIELD_UPPER -> {
				float l = k(t, 0, -30, 0.3f, 10, 0.5f, -175, 1, -120);
				p.arms(-60 * DEG, 0, 15 * DEG, l, 0, -10 * DEG);
				p.rootY = K.kRaw(t, 0, 0, 0.3f, 2, 0.5f, -3, 0.8f, 0, 1, 0);
				p.rootX = k(t, 0, 0, 0.3f, 15, 0.55f, -10, 1, 0);
				p.root = true;
			}
			case GUARD -> {
				p.arms(-55 * DEG, -10 * DEG, 5 * DEG, -80 * DEG, -25 * DEG, -5 * DEG);
				p.rootY = 1.5f;
				p.rootX = 6 * DEG;
				p.root = true;
			}
			case SHOOT -> aim(p, k(t, 0, 0, 0.2f, -18, 0.5f, 0), 0);
			case SHOOT_HEAVY -> {
				aim(p, k(t, 0, 0, 0.2f, -26, 0.6f, 0), 0);
				p.rootX = k(t, 0, 0, 0.2f, -8, 0.6f, 0);
				p.root = true;
			}
			case GATLING -> aim(p, (float) Math.sin(age * 3.1f) * 4f * DEG, 0);
			case RELOAD, PHIAL_CHARGE -> {
				float pump = k(t, 0, 0, 0.4f, 30, 0.6f, -10, 1, 0);
				p.arms(-45 * DEG, -20 * DEG, 0, -60 * DEG + pump, 45 * DEG, -10 * DEG);
				p.head(18 * DEG, 0);
			}
			case BOW_DRAW -> {
				p.arms(-90 * DEG, -6 * DEG, 0, -90 * DEG, 29 * DEG, 0);
				p.armsFollowHead = true;
			}
			case BOW_SHOOT, BOW_POWER -> {
				float r = k(t, 0, -90, 0.3f, -90, 0.4f, -60, 1, -70);
				p.arms(r, k(t, 0, -6, 0.35f, 20, 1, 10), 0, -90 * DEG, 29 * DEG, 0);
				p.armsFollowHead = true;
				if (kind == AnimKind.BOW_POWER) {
					p.rootX = k(t, 0, 0, 0.3f, -6, 0.5f, 8, 1, 0);
					p.root = true;
				}
			}
			case PIERCER -> {
				float r = k(t, 0, -90, 0.75f, -90, 0.8f, -40, 1, -60);
				p.arms(r + shake, -6 * DEG, 0, -95 * DEG + shake, 29 * DEG, 0);
				p.rootX = k(t, 0, 0, 0.7f, -14, 0.8f, 6, 1, 0);
				p.root = true;
				p.armsFollowHead = true;
			}
			case VAULT -> {
				float a = k(t, 0, -30, 0.25f, 20, 0.5f, -170, 1, -160);
				p.arms(a, 0, 20 * DEG, a, 0, -20 * DEG);
				p.rootY = K.kRaw(t, 0, 0, 0.25f, 3, 0.5f, 0);
				p.legs(k(t, 0, 0, 0.5f, -40, 1, -30), k(t, 0, 0, 0.5f, 20, 1, 10));
				p.root = true;
			}
			case AERIAL_SLASH -> {
				p.arms(-110 * DEG, 0, 50 * DEG, -110 * DEG, 0, -50 * DEG);
				p.rootX = k(t, 0, 0, 1, 360);
				p.legs(-50 * DEG, -50 * DEG);
				p.root = true;
			}
			case DIVE_THRUST -> {
				p.arms(-40 * DEG, 0, 0, -50 * DEG, 25 * DEG, 0);
				p.rootX = k(t, 0, 0, 0.3f, 45, 1, 20);
				p.legs(30 * DEG, 30 * DEG);
				p.root = true;
			}
			case KINSECT_LAUNCH -> p.arms(-45 * DEG, 0, 10 * DEG, k(t, 0, -40, 0.3f, -100, 1, -90), 20 * DEG, 0);
			case MORPH -> {
				float a = k(t, 0, -40, 0.3f, -180, 0.6f, -270, 1, -60);
				p.arms(a, 0, 15 * DEG, -60 * DEG, 30 * DEG, -10 * DEG);
				p.body(0, k(t, 0, 0, 0.4f, 30, 0.7f, -20, 1, 0), 0);
			}
			case DISCHARGE -> {
				float jit = (float) Math.sin(age * 3.3f) * 4f * DEG;
				p.arms(-80 * DEG + jit, -18 * DEG, 0, -80 * DEG - jit, 30 * DEG, 0);
				p.rootX = 12 * DEG;
				p.rootY = 1f;
				p.root = true;
			}
			case SUPER_DISCHARGE -> {
				float a = k(t, 0, -60, 0.4f, -175, 0.55f, -20, 1, -30);
				p.arms(a + shake, -15 * DEG, 0, a + 5 * DEG, 25 * DEG, 0);
				p.rootX = k(t, 0, 0, 0.4f, -12, 0.55f, 24, 1, 6);
				p.root = true;
			}
			case WYVERNFIRE -> {
				float fire = 0.65f;
				float tremble = t < fire ? (float) Math.sin(age * 4.1f) * t * 5f * DEG : 0f;
				p.arms(-65 * DEG + tremble, -10 * DEG, 0, -70 * DEG + tremble, 25 * DEG, 0);
				p.rootX = k(t, 0, 4, fire, 14, fire + 0.06f, -24, 1, 0);
				p.rootY = t < fire ? 2.5f : K.kRaw(t, fire, 2.5f, 1, 0);
				p.root = true;
			}
			case FULL_BURST -> {
				float a = k(t, 0, -60, 0.2f, -170, 0.3f, -25, 1, -35);
				p.arms(a, -15 * DEG, 0, a, 25 * DEG, 0);
				float kick = t > 0.3f ? (float) Math.abs(Math.sin((t - 0.3f) * Math.PI * 8)) * -10 * DEG : 0;
				p.rootX = k(t, 0, 0, 0.3f, 18, 1, 10) + kick;
				p.root = true;
			}
			case HORN_BACK -> {
				float a = k(t, 0, -60, 0.35f, -160, 0.5f, 20, 1, -20);
				p.arms(a, 0, 10 * DEG, a, 25 * DEG, -10 * DEG);
				p.rootYaw = k(t, 0, 0, 0.35f, -40, 0.55f, 160, 1, 180);
				p.root = true;
			}
			case HORN_PLAY -> {
				float up = k(t, 0, -60, 0.2f, -170, 0.8f, -170, 1, -60);
				p.arms(up, 0, 20 * DEG, up, 0, -20 * DEG);
				p.rootYaw = k(t, 0, 0, 0.25f, 0, 0.75f, -360, 1, -360);
				p.head(-25 * DEG, 0);
				p.root = true;
			}
			case FORESIGHT -> {
				float a = k(t, 0, -60, 0.3f, -150, 0.45f, -40, 1, -40);
				p.arms(a, -15 * DEG, 0, a, 25 * DEG, 0);
				p.rootX = k(t, 0, 0, 0.25f, -14, 0.45f, 12, 1, 0);
				p.root = true;
			}
			case SPIRIT_SLASH -> {
				float s = (float) Math.sin(t * Math.PI * 3);
				p.arms(-85 * DEG, s * 50 * DEG, 0, -85 * DEG, 25 * DEG + s * 50 * DEG, 0);
				p.body(0, s * 35 * DEG, 0);
				p.rootX = 8 * DEG;
				p.root = true;
			}
			case HELM_BREAKER -> {
				float a = k(t, 0, -60, 0.3f, -175, 0.8f, -175, 0.9f, -20, 1, -30);
				p.arms(a, -15 * DEG, 0, a, 25 * DEG, 0);
				p.rootX = k(t, 0, 0, 0.3f, -360, 0.42f, -360, 0.85f, -340, 0.9f, 0, 1, 0);
				p.legs(-40 * DEG, 20 * DEG);
				p.root = true;
			}
			case LANCE_CHARGE -> {
				float run = (float) Math.sin(age * 0.9f);
				p.arms(-85 * DEG, -5 * DEG, 0, -70 * DEG, -20 * DEG, 0);
				p.legs(run * 55 * DEG, -run * 55 * DEG);
				p.rootX = 15 * DEG;
				p.root = true;
			}
			case DODGE_ROLL, DEMON_STEP -> roll(kind, t, dir, p);
			case SIDESTEP -> {
				float side = (dir & InputKind.DIR_LEFT) != 0 ? 1 : ((dir & InputKind.DIR_RIGHT) != 0 ? -1 : 0);
				p.rootZ = k(t, 0, 0, 0.3f, 22, 1, 0) * side;
				p.rootX = side == 0 ? k(t, 0, 0, 0.3f, -18, 1, 0) : 0;
				p.rootY = K.kRaw(t, 0, 0, 0.3f, 2, 1, 0);
				aim(p, 0, 0);
				p.root = true;
			}
			case KNOCKED_DOWN -> {
				p.rootX = k(t, 0, 0, 0.15f, -95, 0.65f, -90, 0.85f, -20, 1, 0);
				p.rootY = K.kRaw(t, 0, 0, 0.15f, 9, 0.7f, 9, 1, 0);
				p.arms(k(t, 0, -40, 0.2f, -150, 0.7f, -160, 1, 0), 0, 30 * DEG,
					k(t, 0, -40, 0.2f, -150, 0.7f, -160, 1, 0), 0, -30 * DEG);
				p.legs(k(t, 0, 0, 0.2f, -30, 0.7f, -20, 1, 0), k(t, 0, 0, 0.2f, -10, 0.7f, 0, 1, 0));
				p.root = true;
			}
			case STAGGER -> {
				p.rootX = k(t, 0, 0, 0.3f, -16, 1, 0);
				p.arms(k(t, 0, 0, 0.3f, -60, 1, 0), 0, 40 * DEG, k(t, 0, 0, 0.3f, -50, 1, 0), 0, -40 * DEG);
				p.root = true;
			}
			case ROAR_COVER -> {
				p.arms(-150 * DEG, 20 * DEG, -25 * DEG, -150 * DEG, -20 * DEG, 25 * DEG);
				p.rootY = 2.5f;
				p.rootX = 14 * DEG;
				p.head(25 * DEG, 0);
				p.root = true;
			}
			case SHARPEN -> {
				float scrub = (float) Math.sin(t * Math.PI * 6) * 20 * DEG;
				p.arms(-40 * DEG + scrub, -30 * DEG, 0, -45 * DEG, 30 * DEG, 0);
				p.rootY = 5f;
				p.rootX = 18 * DEG;
				p.head(30 * DEG, 0);
				p.root = true;
			}
			case FLEX -> {
				float pump = (float) Math.sin(t * Math.PI * 2) * 8 * DEG;
				p.arms(-15 * DEG, 0, 105 * DEG + pump, -15 * DEG, 0, -105 * DEG - pump);
				p.head(-12 * DEG, 0);
			}
			case CARVE -> {
				float stab = (float) Math.abs(Math.sin(t * Math.PI * 3)) * 40 * DEG;
				p.arms(-60 * DEG + stab, -10 * DEG, 0, -50 * DEG, 20 * DEG, 0);
				p.rootY = 6f;
				p.rootX = 28 * DEG;
				p.head(30 * DEG, 0);
				p.root = true;
			}
			case TRIUMPH -> {
				float pump = (float) Math.abs(Math.sin(age * 0.25f)) * 15 * DEG;
				p.arms(-175 * DEG + pump, 0, 10 * DEG, -10 * DEG, 0, -20 * DEG);
				p.head(-15 * DEG, 0);
			}
			case THROW -> {
				float a = k(t, 0, 40, 0.4f, 60, 0.6f, -150, 1, -60);
				p.arms(a, 0, 5 * DEG, -40 * DEG, 0, -15 * DEG);
				p.body(0, k(t, 0, 0, 0.4f, 30, 0.6f, -20, 1, 0), 0);
			}
			case PLACE -> {
				p.arms(-50 * DEG, -15 * DEG, 0, -50 * DEG, 15 * DEG, 0);
				p.rootY = K.kRaw(t, 0, 0, 0.4f, 5, 1, 0);
				p.rootX = k(t, 0, 0, 0.4f, 25, 1, 0);
				p.root = true;
			}
			default -> {
			}
		}
	}

	/** 横振り（dirSign = 1 で右から左）。 */
	private static void sideSwing(float t, Pose p, int dirSign) {
		float b = k(t, 0, 0, 0.3f, 50, 0.55f, -65, 1, -25) * dirSign;
		p.arms(-80 * DEG, -10 * DEG, 10 * DEG, -78 * DEG, 32 * DEG, -10 * DEG);
		p.body(0, b, 0);
		p.rootX = k(t, 0, 0, 0.45f, 8, 1, 0);
		p.root = true;
	}

	/** 構え（ボウガン・射撃）。recoil は腕を跳ね上げる量。 */
	private static void aim(Pose p, float recoil, float side) {
		p.arms(-90 * DEG + recoil, -18 * DEG + side, 0, -90 * DEG + recoil, 35 * DEG + side, 0);
		p.armsFollowHead = true;
	}

	/** 回避ローリング（前転／横転／後転）。 */
	private static void roll(AnimKind kind, float t, int dir, Pose p) {
		boolean back = (dir & InputKind.DIR_BACK) != 0;
		boolean left = (dir & InputKind.DIR_LEFT) != 0;
		boolean right = (dir & InputKind.DIR_RIGHT) != 0;
		boolean fwd = (dir & InputKind.DIR_FORWARD) != 0 || (!back && !left && !right);
		float spin = kind == AnimKind.DEMON_STEP ? 0f : k(t, 0, 0, 0.75f, 360, 1, 360);
		if (kind == AnimKind.DEMON_STEP) {
			p.rootX = k(t, 0, 0, 0.2f, 28, 1, 0) * (back ? -1 : 1);
			p.rootZ = k(t, 0, 0, 0.2f, 25, 1, 0) * (left ? 1 : right ? -1 : 0);
			p.arms(45 * DEG, 0, 30 * DEG, 45 * DEG, 0, -30 * DEG);
		} else if (fwd && !left && !right) {
			p.rootX = spin;
		} else if (back) {
			p.rootX = -spin;
		} else {
			p.rootZ = left ? spin : -spin;
		}
		p.rootY = kind == AnimKind.DEMON_STEP ? 2f : K.kRaw(t, 0, 0, 0.15f, 7, 0.75f, 7, 1, 0);
		if (kind != AnimKind.DEMON_STEP) {
			p.arms(-35 * DEG, 0, 10 * DEG, -35 * DEG, 0, -10 * DEG);
			p.legs(-60 * DEG, -60 * DEG);
		}
		p.root = true;
	}

	/**
	 * 抜刀中（技を出していない時）の構え。歩行中の脚はバニラのまま。
	 */
	public static void stance(WeaponType type, boolean guarding, boolean altMode, float age, Pose p) {
		float breath = (float) Math.sin(age * 0.08f) * 2 * DEG;
		if (guarding) {
			pose(AnimKind.GUARD, 0, -1, false, age, 0, p);
			return;
		}
		switch (type) {
			case GREAT_SWORD -> p.arms(-50 * DEG + breath, -25 * DEG, 0, -45 * DEG + breath, 35 * DEG, 0);
			case LONG_SWORD -> p.arms(-60 * DEG + breath, -20 * DEG, 0, -55 * DEG + breath, 35 * DEG, 0);
			case SWORD_AND_SHIELD -> p.arms(-30 * DEG + breath, 0, 10 * DEG, -60 * DEG + breath, -15 * DEG, -5 * DEG);
			case DUAL_BLADES -> {
				if (altMode) {
					p.arms(-100 * DEG + breath, 0, 50 * DEG, -100 * DEG + breath, 0, -50 * DEG);
				} else {
					p.arms(-25 * DEG + breath, 0, 25 * DEG, -25 * DEG + breath, 0, -25 * DEG);
				}
			}
			case HAMMER, HUNTING_HORN -> p.arms(-130 * DEG + breath, -10 * DEG, 10 * DEG, -60 * DEG, 40 * DEG, 0);
			case LANCE, GUNLANCE -> p.arms(-75 * DEG + breath, -5 * DEG, 0, -70 * DEG + breath, -25 * DEG, -5 * DEG);
			case SWITCH_AXE -> p.arms(-55 * DEG + breath, -20 * DEG, 0, -50 * DEG + breath, 30 * DEG, 0);
			case CHARGE_BLADE -> {
				if (altMode) {
					p.arms(-55 * DEG + breath, -20 * DEG, 0, -50 * DEG + breath, 30 * DEG, 0);
				} else {
					p.arms(-35 * DEG + breath, 0, 10 * DEG, -65 * DEG + breath, -15 * DEG, -5 * DEG);
				}
			}
			case INSECT_GLAIVE -> p.arms(-45 * DEG + breath, -25 * DEG, 0, -40 * DEG + breath, 40 * DEG, 0);
			case LIGHT_BOWGUN, HEAVY_BOWGUN -> aim(p, 0, 0);
			case BOW -> p.arms(-75 * DEG + breath, -8 * DEG, 0, -45 * DEG + breath, 20 * DEG, 0);
		}
	}
}
