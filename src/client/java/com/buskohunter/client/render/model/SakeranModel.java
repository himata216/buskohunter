package com.buskohunter.client.render.model;

import com.buskohunter.client.render.MonsterRenderState;
import com.buskohunter.monster.MonsterAnim;
import com.buskohunter.monster.MonsterFlags;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import static com.buskohunter.client.anim.K.k;
import static com.buskohunter.client.anim.K.kRaw;

/**
 * サケラン（筋骨隆々の白い獣人）のモデル。ピースサイン・ダブルバイセップス・ラリアットなど。
 */
public class SakeranModel extends EntityModel<MonsterRenderState> {
	private final ModelPart root, hips, waist, chest, neck, head, jaw, nose, tail;
	private final ModelPart armR, armL, foreR, foreL, pawR, pawL, f1R, f2R, f1L, f2L;
	private final ModelPart thighR, thighL, shinR, shinL, earR, earL;
	private final PartMap p;

	public SakeranModel(ModelPart meshRoot) {
		super(meshRoot);
		p = new PartMap(meshRoot, BuskoMeshes.SAKERAN_PARTS);
		root = p.get("root");
		hips = p.get("hips");
		waist = p.get("waist");
		chest = p.get("chest");
		neck = p.get("neck");
		head = p.get("head");
		jaw = p.get("jaw");
		nose = p.get("nose");
		tail = p.get("tail");
		armR = p.get("arm_r");
		armL = p.get("arm_l");
		foreR = p.get("forearm_r");
		foreL = p.get("forearm_l");
		pawR = p.get("paw_r");
		pawL = p.get("paw_l");
		f1R = p.get("finger1_r");
		f2R = p.get("finger2_r");
		f1L = p.get("finger1_l");
		f2L = p.get("finger2_l");
		thighR = p.get("thigh_r");
		thighL = p.get("thigh_l");
		shinR = p.get("shin_r");
		shinL = p.get("shin_l");
		earR = p.get("ear_r");
		earL = p.get("ear_l");
	}

	@Override
	public void setupAnim(MonsterRenderState s) {
		super.setupAnim(s);
		float age = s.ageInTicks;
		float t = s.progress();
		float walk = s.walkAnimationPos;
		float ws = Math.min(1f, s.walkAnimationSpeed * 1.5f);
		boolean enraged = s.has(MonsterFlags.ENRAGED);

		// 部位破壊
		nose.xScale = nose.yScale = nose.zScale = s.broken(0) ? 0.6f : 1f;
		p.get("claw1_r").visible = !s.broken(2);
		p.get("claw2_r").visible = !s.broken(2);
		tail.visible = !s.broken(5);

		// 頭の向き（ターゲットを見る）
		neck.yRot += s.yRot * 0.6f * ((float) Math.PI / 180f);
		head.xRot += s.xRot * 0.5f * ((float) Math.PI / 180f);

		// 呼吸と尻尾
		float breath = (float) Math.sin(age * (enraged ? 0.16f : 0.09f));
		chest.xScale = 1f + breath * 0.02f;
		chest.zScale = 1f + breath * 0.03f;
		tail.yRot += (float) Math.sin(age * 0.3f) * 0.4f;
		earR.zRot += (float) Math.sin(age * 0.07f) * 0.05f;
		earL.zRot -= (float) Math.sin(age * 0.07f) * 0.05f;
		armR.zRot += 0.08f + breath * 0.03f;
		armL.zRot -= 0.08f + breath * 0.03f;

		// 歩行
		if (ws > 0.02f) {
			float c = (float) Math.sin(walk * 0.62f);
			thighR.xRot += c * 0.7f * ws;
			thighL.xRot -= c * 0.7f * ws;
			shinR.xRot += Math.max(0, -c) * 0.7f * ws;
			shinL.xRot += Math.max(0, c) * 0.7f * ws;
			armR.xRot -= c * 0.55f * ws;
			armL.xRot += c * 0.55f * ws;
			chest.yRot += c * 0.12f * ws;
			hips.y -= Math.abs(c) * 1.2f * ws;
			if (s.has(MonsterFlags.LIMPING)) {
				thighL.xRot *= 0.3f;
				root.zRot += 0.07f;
			}
		}
		if (s.has(MonsterFlags.TIRED)) {
			neck.xRot += 0.35f;
			jaw.xRot += 0.35f;
			chest.xRot += 0.15f;
		}

		switch (s.anim) {
			case ROAR -> doubleBiceps(kRaw(t, 0, 0, 0.2f, 1, 0.85f, 1, 1, 0), true);
			case TAUNT -> {
				float a = kRaw(t, 0, 0, 0.2f, 1, 0.85f, 1, 1, 0);
				// モストマスキュラー
				armR.xRot -= 0.5f * a;
				armL.xRot -= 0.5f * a;
				armR.zRot -= 0.35f * a;
				armL.zRot += 0.35f * a;
				foreR.zRot += 0.9f * a;
				foreL.zRot -= 0.9f * a;
				chest.xRot += 0.15f * a;
				chest.xScale += 0.06f * a;
				neck.xRot += 0.2f * a;
				float shake = (float) Math.sin(age * 2.5f) * 0.03f * a;
				chest.zRot += shake;
			}
			case PUNCH -> punch(t, armR, foreR, 1);
			case DOUBLE_PUNCH -> {
				punch(Math.min(1f, t * 1.8f), armR, foreR, 1);
				punch(Math.max(0f, t * 1.8f - 0.8f), armL, foreL, -1);
			}
			case LARIAT -> {
				float a = kRaw(t, 0, 0, 0.2f, 1, 0.85f, 1, 1, 0);
				armR.zRot += 1.4f * a;
				armL.zRot -= 1.4f * a;
				root.yRot += k(t, 0, 0, 0.25f, 0, 0.85f, -1080, 1, -1080);
				chest.xRot -= 0.1f * a;
			}
			case THROW -> {
				armR.xRot += k(t, 0, 0, 0.45f, -175, 0.58f, -50, 1, 0);
				foreR.xRot += k(t, 0, 0, 0.45f, -40, 0.58f, 0, 1, 0);
				chest.yRot += k(t, 0, 0, 0.45f, 30, 0.6f, -25, 1, 0);
				thighL.xRot += k(t, 0, 0, 0.45f, -20, 1, 0);
			}
			case CHARGE -> {
				root.xRot += 0.35f;
				float c = (float) Math.sin(age * 1.1f);
				thighR.xRot += c * 1.0f;
				thighL.xRot -= c * 1.0f;
				shinR.xRot += Math.max(0, -c) * 1.0f;
				shinL.xRot += Math.max(0, c) * 1.0f;
				armR.xRot += 0.5f;
				armL.xRot += 0.5f;
				armR.zRot += 0.3f;
				armL.zRot -= 0.3f;
			}
			case LEAP, SLAM -> {
				float crouch = kRaw(t, 0, 0, 0.2f, 1, 0.3f, 0, 0.5f, 0, 0.56f, 1, 0.8f, 0);
				float air = kRaw(t, 0.25f, 0, 0.32f, 1, 0.5f, 1, 0.54f, 0);
				hips.y += 5f * crouch;
				thighR.xRot -= 0.7f * crouch;
				thighL.xRot -= 0.7f * crouch;
				shinR.xRot += 1.2f * crouch;
				shinL.xRot += 1.2f * crouch;
				armR.xRot -= 2.6f * air;
				armL.xRot -= 2.6f * air;
				thighR.xRot -= 0.9f * air;
				thighL.xRot -= 0.9f * air;
				root.xRot += 0.4f * kRaw(t, 0.5f, 0, 0.56f, 1, 0.9f, 0.6f, 1, 0);
			}
			case BEAM -> {
				// ピースサイン → 声劇ビーム
				float peace = kRaw(t, 0, 0, 0.15f, 1, 0.45f, 1, 0.52f, 0);
				armR.xRot -= 2.3f * peace;
				armR.zRot -= 0.45f * peace;
				foreR.xRot -= 1.3f * peace;
				f1R.zRot += 0.3f * peace;
				f2R.zRot -= 0.3f * peace;
				f1R.xRot += 3.0f * peace;
				f2R.xRot += 3.0f * peace;
				neck.zRot += 0.25f * peace;
				float beam = kRaw(t, 0.45f, 0, 0.5f, 1, 0.9f, 1, 1, 0);
				jaw.xRot += 0.55f * beam;
				neck.xRot -= 0.1f * beam;
				armR.zRot += 0.9f * beam;
				armL.zRot -= 0.9f * beam;
				root.xRot -= 0.12f * beam;
			}
			case HIP_ATTACK -> {
				float a = kRaw(t, 0, 0, 0.35f, -0.2f, 0.5f, 1, 1, 0);
				root.xRot += 0.4f * a;
				neck.xRot -= 0.4f * a;
			}
			case GROUND_POUND -> {
				float c = (float) Math.abs(Math.sin(t * Math.PI * 4));
				armR.xRot -= 2.8f * c;
				armL.xRot -= 2.8f * c;
				root.xRot += 0.25f * (1 - c);
				jaw.xRot += 0.4f;
			}
			case FLINCH -> {
				root.xRot -= k(t, 0, 0, 0.2f, 12, 1, 0);
				neck.xRot -= k(t, 0, 0, 0.2f, 25, 1, 0);
				armR.zRot += 0.4f;
				armL.zRot -= 0.4f;
			}
			case DOWN -> {
				float a = kRaw(t, 0, 0, 0.15f, 1, 0.85f, 1, 1, 0);
				root.xRot -= 1.5f * a;
				root.y += 4f * a;
				float flail = (float) Math.sin(age * 1.2f) * 0.6f * a;
				thighR.xRot -= 0.6f * a + flail;
				thighL.xRot -= 0.6f * a - flail;
			}
			case STUN -> {
				neck.zRot += (float) Math.sin(age * 0.5f) * 0.35f;
				neck.xRot += 0.25f;
				jaw.xRot += 0.3f;
			}
			case TRAPPED, PITFALL -> {
				float j = (float) Math.sin(age * 2.9f) * 0.08f;
				chest.zRot += j;
				armR.zRot += 0.6f + j;
				armL.zRot -= 0.6f - j;
				if (s.anim == MonsterAnim.PITFALL) root.y += 26f;
			}
			case TIRED -> {
				neck.xRot += 0.5f;
				chest.xRot += 0.2f;
				jaw.xRot += 0.4f;
			}
			case EAT -> {
				armR.xRot -= 1.6f;
				foreR.xRot -= 1.2f;
				jaw.xRot += 0.3f * (float) Math.abs(Math.sin(age * 0.5f));
			}
			case RUN, FLEE -> root.xRot += 0.2f;
			default -> {
			}
		}

		if (s.has(MonsterFlags.ASLEEP) || s.has(MonsterFlags.CAPTURED)) {
			root.zRot += 1.45f;
			root.y += 5f;
			neck.xRot += 0.3f;
			chest.xScale += (float) Math.sin(age * 0.07f) * 0.03f;
		}
		if (s.has(MonsterFlags.DEAD)) {
			float a = Math.min(1f, s.animTime / 30f);
			root.xRot -= 1.5f * a;
			root.y += 4f * a;
			jaw.xRot += 0.5f * a;
		}
		if (s.has(MonsterFlags.BUSKOBA)) {
			// ブスコバ化：鼻が大きく膨らむ
			nose.xScale = nose.yScale = nose.zScale = 1.5f + (float) Math.sin(age * 0.6f) * 0.12f;
		}
	}

	private void doubleBiceps(float a, boolean roar) {
		armR.zRot += 1.45f * a;
		armL.zRot -= 1.45f * a;
		foreR.zRot += 1.5f * a;
		foreL.zRot -= 1.5f * a;
		chest.xRot -= 0.1f * a;
		neck.xRot -= 0.45f * a;
		if (roar) jaw.xRot += 0.6f * a;
	}

	private void punch(float t, ModelPart arm, ModelPart fore, int side) {
		arm.xRot += k(t, 0, 0, 0.35f, 35, 0.48f, -95, 1, 0);
		fore.xRot += k(t, 0, 0, 0.35f, -70, 0.48f, 0, 1, 0);
		chest.yRot += k(t, 0, 0, 0.35f, 25, 0.48f, -30, 1, 0) * side;
		root.xRot += k(t, 0, 0, 0.48f, 8, 1, 0);
	}
}
