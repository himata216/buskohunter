package com.buskohunter.client.render;

import com.buskohunter.BuskoHunter;
import com.buskohunter.entity.HubNpc;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** 集会所の NPC（プレイヤーと同じ体型・役ごとの見た目）。 */
public class HubNpcRenderer extends HumanoidMobRenderer<HubNpc, HubNpcRenderer.State, HumanoidModel<HubNpcRenderer.State>> {
	public HubNpcRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HubNpc entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.role = entity.role();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return BuskoHunter.id("textures/entity/npc/" + state.role.id + ".png");
	}

	public static class State extends HumanoidRenderState {
		public HubNpc.Role role = HubNpc.Role.RECEPTIONIST;
	}
}
