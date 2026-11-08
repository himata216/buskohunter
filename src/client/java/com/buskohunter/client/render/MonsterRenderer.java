package com.buskohunter.client.render;

import com.buskohunter.BuskoHunter;
import com.buskohunter.client.render.model.BuskoModel;
import com.buskohunter.client.render.model.SakeranModel;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.monster.HuntMonster;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** 大型モンスターの描画。 */
public class MonsterRenderer extends MobRenderer<HuntMonster, MonsterRenderState, EntityModel<MonsterRenderState>> {
	public static final ModelLayerLocation BUSKO_LAYER = new ModelLayerLocation(BuskoHunter.id("busko"), "main");
	public static final ModelLayerLocation SAKERAN_LAYER = new ModelLayerLocation(BuskoHunter.id("sakeran"), "main");

	private final MonsterKind kind;
	private final Identifier texture;

	public MonsterRenderer(EntityRendererProvider.Context context, MonsterKind kind) {
		super(context, createModel(context, kind), kind.width * 0.55f);
		this.kind = kind;
		this.texture = BuskoHunter.id("textures/entity/" + kind.id + ".png");
	}

	private static EntityModel<MonsterRenderState> createModel(EntityRendererProvider.Context context, MonsterKind kind) {
		if (kind == MonsterKind.SAKERAN) {
			return new SakeranModel(context.bakeLayer(SAKERAN_LAYER));
		}
		return new BuskoModel(context.bakeLayer(BUSKO_LAYER));
	}

	@Override
	public MonsterRenderState createRenderState() {
		return new MonsterRenderState();
	}

	@Override
	public void extractRenderState(HuntMonster entity, MonsterRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.kind = kind;
		state.dummy = false;
		state.anim = entity.getAnim();
		state.animTime = (entity.tickCount - entity.getAnimStart()) + partialTick;
		state.animLength = entity.getAnimLength();
		state.flags = entity.getMonsterFlags();
		state.broken = entity.getBrokenParts();
		state.isInvisible = state.isInvisible || (state.has(com.buskohunter.monster.MonsterFlags.GLITCH)
			&& ((entity.tickCount / 2) % 2 == 0));
	}

	@Override
	public Identifier getTextureLocation(MonsterRenderState state) {
		return texture;
	}

	@Override
	protected boolean affectedByCulling(HuntMonster entity) {
		return false;
	}
}
