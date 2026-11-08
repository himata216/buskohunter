package com.buskohunter.client.render;

import com.buskohunter.BuskoHunter;
import com.buskohunter.client.render.model.BuskoModel;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.entity.TrainingDummy;
import com.buskohunter.monster.MonsterAnim;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** 訓練用ブスコ人形（ブスコバの模型を小さくしたもの。叩くと揺れる）。 */
public class DummyRenderer extends MobRenderer<TrainingDummy, MonsterRenderState, EntityModel<MonsterRenderState>> {
	private static final Identifier TEXTURE = BuskoHunter.id("textures/entity/training_dummy.png");
	private static final float SCALE = 0.45f;
	/** entityId → {最後に見たヒット値, その時のクライアント tick}。 */
	private static final Map<Integer, int[]> HITS = new HashMap<>();

	public DummyRenderer(EntityRendererProvider.Context context) {
		super(context, new BuskoModel(context.bakeLayer(MonsterRenderer.BUSKO_LAYER)), 0.7f);
	}

	@Override
	public MonsterRenderState createRenderState() {
		return new MonsterRenderState();
	}

	@Override
	public void extractRenderState(TrainingDummy entity, MonsterRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.kind = MonsterKind.BUSKOBA;
		state.dummy = true;
		state.flags = 0;
		state.broken = 0;
		int[] h = HITS.computeIfAbsent(entity.getId(), id -> new int[]{entity.lastHitTick(), -1000});
		if (h[0] != entity.lastHitTick()) {
			h[0] = entity.lastHitTick();
			h[1] = entity.tickCount;
		}
		if (HITS.size() > 256) HITS.clear();
		float since = entity.tickCount - h[1] + partialTick;
		if (since >= 0 && since < 10) {
			state.anim = MonsterAnim.FLINCH;
			state.animTime = since;
			state.animLength = 10;
		} else {
			state.anim = MonsterAnim.IDLE;
			state.animTime = entity.tickCount + partialTick;
			state.animLength = 40;
		}
	}

	@Override
	protected void scale(MonsterRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	@Override
	public Identifier getTextureLocation(MonsterRenderState state) {
		return TEXTURE;
	}
}
