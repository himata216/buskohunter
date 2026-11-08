package com.buskohunter.client.render;

import com.buskohunter.entity.HubLabel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** 集会所の看板：カメラの方を向く大きな文字（色付き・影付き・半透明の背景）。 */
public class HubLabelRenderer extends EntityRenderer<HubLabel, HubLabelRenderer.State> {
	private static final int FULL_BRIGHT = 0xF000F0;

	public HubLabelRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HubLabel entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.title = entity.title();
		state.sub = entity.sub();
		state.color = entity.color();
		state.scale = entity.scale();
	}

	@Override
	protected boolean affectedByCulling(HubLabel entity) {
		return false;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.title.isEmpty()) return;
		Font font = Minecraft.getInstance().font;
		poseStack.pushPose();
		poseStack.rotate(camera.orientation);
		float s = 0.025f * state.scale;
		poseStack.scale(s, -s, s);
		FormattedCharSequence title = Component.literal(state.title).getVisualOrderText();
		int tw = font.width(title);
		boolean hasSub = !state.sub.isEmpty();
		FormattedCharSequence sub = hasSub ? Component.literal(state.sub).getVisualOrderText() : null;
		int sw = hasSub ? font.width(sub) : 0;
		float subScale = 0.7f;
		float bw = Math.max(tw, sw * subScale) / 2f + 3;
		float top = -10f, bottom = hasSub ? 1.5f + 9 * subScale : 0f;
		collector.submitTextBackground(poseStack, -bw, top - 1, bw, bottom + 1, 0x90000000, Font.DisplayMode.POLYGON_OFFSET, FULL_BRIGHT);
		collector.submitText(poseStack, -tw / 2f, top + 1, title, true, Font.DisplayMode.POLYGON_OFFSET, FULL_BRIGHT, 0xFF000000 | state.color, 0, 0);
		if (hasSub) {
			poseStack.pushPose();
			poseStack.translate(0, 1.5f, 0);
			poseStack.scale(subScale, subScale, subScale);
			collector.submitText(poseStack, -sw / 2f, 0, sub, true, Font.DisplayMode.POLYGON_OFFSET, FULL_BRIGHT, 0xFFE8E8E8, 0, 0);
			poseStack.popPose();
		}
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	public static class State extends EntityRenderState {
		public String title = "";
		public String sub = "";
		public int color = 0xFFFFFF;
		public float scale = 1f;
	}
}
