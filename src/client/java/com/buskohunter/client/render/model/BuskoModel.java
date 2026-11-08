package com.buskohunter.client.render.model;

import com.buskohunter.client.render.MonsterRenderState;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.monster.MonsterAnim;
import com.buskohunter.monster.MonsterFlags;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import static com.buskohunter.client.anim.K.k;
import static com.buskohunter.client.anim.K.kRaw;

/**
 * ブスコ系モンスター（ブスコバ / ブスコバリーノ王 / ジャングリア / ブサイバー / 訓練人形）の共通モデル。
 * 種類ごとに角・翼・光輪・装甲の表示を切り替え、手続き的にアニメーションする。
 */
public class BuskoModel extends EntityModel<MonsterRenderState> {
	private final PartMap p;
	private final ModelPart root, body, lobeR, lobeL, neck, head, hat, armR, armL, forearmR, forearmL, halo;
	private final ModelPart wingR, wingL, wing2R, wing2L, wingMemR, wingMemL, wing2MemR, wing2MemL;

	public BuskoModel(ModelPart meshRoot) {
		super(meshRoot);
		this.p = new PartMap(meshRoot, BuskoMeshes.BUSKO_PARTS);
		root = p.get("root");
		body = p.get("body");
		lobeR = p.get("lobe_r");
		lobeL = p.get("lobe_l");
		neck = p.get("neck");
		head = p.get("head");
		hat = p.get("hat");
		armR = p.get("arm_r");
		armL = p.get("arm_l");
		forearmR = p.get("forearm_r");
		forearmL = p.get("forearm_l");
		halo = p.get("halo");
		wingR = p.get("wing_r");
		wingL = p.get("wing_l");
		wing2R = p.get("wing2_r");
		wing2L = p.get("wing2_l");
		wingMemR = p.get("wing_mem_r");
		wingMemL = p.get("wing_mem_l");
		wing2MemR = p.get("wing2_mem_r");
		wing2MemL = p.get("wing2_mem_l");
	}

	private void show(String prefix, boolean visible) {
		for (String[] path : BuskoMeshes.BUSKO_PARTS) {
			String n = path[path.length - 1];
			if (n.startsWith(prefix)) p.get(n).visible = visible;
		}
	}

	private void variant(MonsterRenderState s) {
		MonsterKind k = s.kind;
		boolean dummy = s.dummy;
		boolean barino = !dummy && k == MonsterKind.BARINO;
		boolean jangria = !dummy && k == MonsterKind.JANGRIA;
		boolean cyber = !dummy && k == MonsterKind.BUSAIBER;
		show("halo", barino);
		show("horn", jangria || cyber);
		show("brim_spike", jangria);
		show("claw", jangria || cyber);
		show("wing", jangria || cyber);
		show("spike", jangria);
		show("plate", cyber);
		show("cable", cyber);
		show("visor", cyber);
		hat.visible = true;
		// 部位破壊の見た目
		switch (k) {
			case BUSKOBA -> hat.visible = !s.broken(0);
			case BARINO -> {
				hat.visible = !s.broken(0);
				if (s.broken(4)) {
					for (int i = 0; i < 16; i += 2) p.get("halo_" + i).visible = false;
				}
			}
			case JANGRIA -> {
				if (s.broken(0)) {
					show("horn_tip", false);
					show("brim_spike", false);
				}
				if (s.broken(2)) {
					wingMemR.visible = false;
					wing2MemL.visible = false;
				}
				if (s.broken(3)) show("claw", false);
			}
			case BUSAIBER -> {
				if (s.broken(0)) {
					show("horn_tip", false);
					p.get("visor").visible = false;
				}
				if (s.broken(1)) show("plate", false);
				if (s.broken(2)) {
					wingMemL.visible = false;
					wing2MemR.visible = false;
				}
			}
			default -> {
			}
		}
	}

	@Override
	public void setupAnim(MonsterRenderState s) {
		super.setupAnim(s);
		variant(s);
		float age = s.ageInTicks;
		float t = s.progress();
		float walk = s.walkAnimationPos;
		float ws = Math.min(1f, s.walkAnimationSpeed * 1.4f);
		boolean enraged = s.has(MonsterFlags.ENRAGED);
		float spd = enraged ? 1.4f : 1f;

		// ---- 待機（呼吸・尻の揺れ・光輪の回転・翼） ----
		body.y += (float) Math.sin(age * 0.09f * spd) * 0.5f;
		lobeR.yRot += (float) Math.sin(age * 0.13f) * 0.04f;
		lobeL.yRot -= (float) Math.sin(age * 0.13f) * 0.04f;
		halo.zRot += age * (enraged ? 0.06f : 0.025f);
		float flap = (float) Math.sin(age * 0.18f * spd) * 0.15f;
		wingR.zRot -= flap;
		wingL.zRot += flap;
		wing2R.zRot -= flap * 0.7f;
		wing2L.zRot += flap * 0.7f;
		neck.xRot += (float) Math.sin(age * 0.05f) * 0.03f;
		if (s.has(MonsterFlags.HOVER) || s.has(MonsterFlags.FLYING)) {
			root.y -= 6f + (float) Math.sin(age * 0.12f) * 1.2f;
			armR.xRot -= 0.3f;
			armL.xRot -= 0.3f;
		}

		// ---- 移動 ----
		if (ws > 0.02f && !s.has(MonsterFlags.ASLEEP)) {
			float c = (float) Math.sin(walk * 0.55f);
			body.zRot += c * 0.07f * ws;
			body.y -= Math.abs(c) * 2.0f * ws;
			armR.xRot += c * 0.55f * ws;
			armL.xRot -= c * 0.55f * ws;
			forearmR.xRot += Math.max(0, c) * 0.3f * ws;
			forearmL.xRot += Math.max(0, -c) * 0.3f * ws;
			lobeR.xRot += c * 0.07f * ws;
			lobeL.xRot -= c * 0.07f * ws;
			if (s.has(MonsterFlags.LIMPING)) {
				root.zRot += 0.08f;
				armL.xRot *= 0.3f;
			}
		}

		// ---- 状態（疲労・睡眠・ダウン等） ----
		if (s.has(MonsterFlags.TIRED)) {
			neck.xRot += 0.35f;
			armR.zRot += 0.15f;
			armL.zRot -= 0.15f;
		}

		switch (s.anim) {
			case ROAR -> {
				float r = kRaw(t, 0, 0, 0.25f, 1, 0.8f, 1, 1, 0);
				root.xRot -= 0.25f * r;
				neck.xRot -= 0.6f * r;
				armR.zRot += 0.7f * r;
				armL.zRot -= 0.7f * r;
				armR.xRot -= 0.4f * r;
				armL.xRot -= 0.4f * r;
				hat.y -= 2f * r;
				float shake = (float) Math.sin(age * 2.4f) * 0.04f * r;
				body.zRot += shake;
				lobeR.zRot += shake * 2;
				lobeL.zRot -= shake * 2;
			}
			case SWIPE_R, SWIPE_L, DOUBLE_SWIPE -> {
				boolean right = s.anim != MonsterAnim.SWIPE_L;
				swipe(t, right ? armR : armL, right ? forearmR : forearmL, right ? 1 : -1);
				if (s.anim == MonsterAnim.DOUBLE_SWIPE) {
					swipe(Math.max(0, t - 0.4f) / 0.6f, armL, forearmL, -1);
				}
				body.yRot += k(t, 0, 0, 0.3f, 15, 0.5f, -20, 1, 0) * (right ? 1 : -1);
			}
			case CHARGE, RUN, FLEE -> {
				root.xRot += 0.25f;
				float c = (float) Math.sin(age * 0.9f);
				armR.xRot += c * 0.9f;
				armL.xRot -= c * 0.9f;
				body.y -= Math.abs(c) * 2f;
			}
			case LEAP, HIP_DROP, BOUNCE, SLAM -> squashJump(s, t);
			case BREATH, BEAM -> {
				float a = kRaw(t, 0, 0, 0.35f, 1, 0.9f, 1, 1, 0);
				neck.xRot += 0.25f * a;
				root.xRot += 0.08f * a;
				armR.zRot += 0.3f * a;
				armL.zRot -= 0.3f * a;
				hat.xRot -= 0.15f * a;
				halo.zRot += age * 0.08f * a;
				float jit = (float) Math.sin(age * 3f) * 0.02f * a;
				body.xRot += jit;
			}
			case BUTT_BEAM, HIP_ATTACK -> {
				float a = s.anim == MonsterAnim.HIP_ATTACK
					? kRaw(t, 0, 0, 0.35f, -0.2f, 0.5f, 1, 1, 0)
					: kRaw(t, 0, 0, 0.4f, 1, 0.9f, 1, 1, 0);
				root.xRot += 0.35f * a;
				lobeR.xRot -= 0.25f * a;
				lobeL.xRot -= 0.25f * a;
				lobeR.zRot += 0.08f * a;
				lobeL.zRot -= 0.08f * a;
				neck.xRot -= 0.3f * a;
				float jit = (float) Math.sin(age * 3.3f) * 0.03f * a;
				lobeR.yRot += jit;
				lobeL.yRot -= jit;
			}
			case THROW -> {
				armR.zRot += k(t, 0, 0, 0.4f, 40, 0.55f, 60, 1, 0);
				armR.xRot += k(t, 0, 0, 0.4f, 60, 0.55f, -110, 1, 0);
				body.yRot += k(t, 0, 0, 0.4f, 20, 0.55f, -25, 1, 0);
			}
			case SPIN -> {
				root.yRot += k(t, 0, 0, 0.15f, 10, 0.85f, -720, 1, -720);
				armR.zRot += 0.7f;
				armL.zRot -= 0.7f;
			}
			case FLY_UP, HOVER, DIVE, WING_GUST -> {
				float f = (float) Math.sin(age * 0.8f) * 0.6f;
				wingR.zRot -= f;
				wingL.zRot += f;
				wing2R.zRot -= f * 0.8f;
				wing2L.zRot += f * 0.8f;
				if (s.anim == MonsterAnim.DIVE) root.xRot += k(t, 0, 0, 0.4f, 35, 1, 10);
			}
			case TAUNT -> {
				// ブスコバの笑み：尻をふりふり、腕を振る
				float w = (float) Math.sin(age * 0.6f);
				lobeR.yRot += w * 0.25f;
				lobeL.yRot += w * 0.25f;
				body.zRot += w * 0.06f;
				armR.zRot += 0.9f + w * 0.3f;
				armR.xRot -= 0.6f;
			}
			case POSE -> {
				float a = kRaw(t, 0, 0, 0.15f, 1, 0.9f, 1, 1, 0);
				armR.zRot += 1.4f * a;
				armR.xRot -= 0.9f * a;
				armL.zRot -= 0.3f * a;
				armL.xRot += 0.5f * a;
				body.yRot += 0.35f * a;
				neck.xRot -= 0.2f * a;
				root.zRot += 0.08f * a;
			}
			case GROUND_POUND -> {
				float c = (float) Math.abs(Math.sin(t * Math.PI * 4));
				armR.xRot -= 1.4f * c;
				armL.xRot -= 1.4f * c;
				body.y += (1 - c) * 2f;
			}
			case TELEPORT -> {
				float j = (float) Math.sin(age * 5f);
				body.xScale = 1f + j * 0.15f * kRaw(t, 0, 1, 0.5f, 1, 1, 0);
				body.zScale = 1f - j * 0.15f * kRaw(t, 0, 1, 0.5f, 1, 1, 0);
			}
			case SUMMON -> {
				float a = kRaw(t, 0, 0, 0.3f, 1, 0.8f, 1, 1, 0);
				armR.zRot += 1.2f * a;
				armL.zRot -= 1.2f * a;
				armR.xRot -= 1.0f * a;
				armL.xRot -= 1.0f * a;
				neck.xRot -= 0.4f * a;
				halo.zRot += age * 0.12f * a;
			}
			case FLINCH -> {
				root.xRot -= k(t, 0, 0, 0.2f, 14, 1, 0);
				neck.xRot -= k(t, 0, 0, 0.2f, 20, 1, 0);
			}
			case DOWN -> {
				float a = kRaw(t, 0, 0, 0.15f, 1, 0.85f, 1, 1, 0);
				root.zRot += 1.45f * a;
				root.y += 6f * a;
				float flail = (float) Math.sin(age * 1.1f) * 0.6f * a;
				armR.xRot += flail;
				armL.xRot -= flail;
			}
			case STUN -> {
				neck.xRot += 0.3f;
				neck.zRot += (float) Math.sin(age * 0.5f) * 0.3f;
				neck.yRot += (float) Math.cos(age * 0.5f) * 0.3f;
			}
			case TRAPPED, PITFALL -> {
				float j = (float) Math.sin(age * 2.8f) * 0.06f;
				body.zRot += j;
				body.xRot += j * 0.5f;
				armR.zRot += 0.5f;
				armL.zRot -= 0.5f;
				if (s.anim == MonsterAnim.PITFALL) root.y += 22f;
			}
			case TIRED -> {
				neck.xRot += 0.5f;
				body.y += (float) Math.sin(age * 0.25f) * 1.2f;
			}
			case EAT -> neck.xRot += 0.6f + (float) Math.abs(Math.sin(age * 0.4f)) * 0.3f;
			default -> {
			}
		}

		if (s.has(MonsterFlags.ASLEEP) || s.has(MonsterFlags.CAPTURED)) {
			root.y += 4f;
			neck.xRot += 0.7f;
			armR.zRot += 0.5f;
			armL.zRot -= 0.5f;
			body.y += (float) Math.sin(age * 0.08f) * 0.8f;
		}
		if (s.has(MonsterFlags.DEAD)) {
			float a = Math.min(1f, s.animTime / 30f);
			root.zRot += 1.5f * a;
			root.y += 6f * a;
			neck.xRot += 0.5f * a;
		}
		if (s.has(MonsterFlags.BUSKOBA)) {
			// ブスコバ化：ソンブレロが膨らみ、尻がぷるぷる
			hat.xScale = hat.yScale = hat.zScale = 1.35f + (float) Math.sin(age * 0.5f) * 0.08f;
			float j = (float) Math.sin(age * 1.7f) * 0.12f;
			lobeR.yRot += j;
			lobeL.yRot -= j;
		}
	}

	/** 腕を水平に広げて前方へ薙ぐ。 */
	private static void swipe(float t, ModelPart arm, ModelPart fore, int side) {
		float lift = kRaw(t, 0, 0, 0.3f, 1, 0.65f, 1, 1, 0);
		arm.zRot += 0.75f * lift * side;
		arm.xRot += k(t, 0, 0, 0.3f, 35, 0.55f, -95, 1, 0);
		fore.zRot -= 0.8f * lift * side;
	}

	/** ジャンプ系の溜め・伸び・着地のスカッシュ＆ストレッチ。 */
	private void squashJump(MonsterRenderState s, float t) {
		float sq = kRaw(t, 0, 1, 0.15f, 0.82f, 0.3f, 1.12f, 0.5f, 1.06f, 0.56f, 0.78f, 0.7f, 1.04f, 1, 1);
		body.yScale = sq;
		body.xScale = 2f - sq;
		body.zScale = 2f - sq;
		armR.zRot += (1.15f - sq) * 1.2f;
		armL.zRot -= (1.15f - sq) * 1.2f;
		lobeR.xRot -= (sq - 1f) * 0.6f;
		lobeL.xRot -= (sq - 1f) * 0.6f;
	}
}
