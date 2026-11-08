package com.buskohunter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 自動テスト用：自分のキャラを 4 方向から並べて描く（武器の持ち方の確認）。 */
public class ShowcaseScreen extends Screen {
	private static final float[] ANGLES = {90f, 135f, 180f, 270f};

	public ShowcaseScreen() {
		super(Component.literal("showcase"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		g.fill(0, 0, width, height, 0xFF6E8CA8);
		g.fill(0, height * 3 / 4, width, height, 0xFF5A6E50);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		int cols = ANGLES.length;
		int w = width / cols;
		int size = Math.min(w, height) * 30 / 100;
		for (int i = 0; i < cols; i++) {
			draw(g, mc.player, ANGLES[i], i * w, 4, (i + 1) * w, height - 4, size);
		}
	}

	private static void draw(GuiGraphicsExtractor g, LivingEntity entity, float bodyRot, int x0, int y0, int x1, int y1, int size) {
		EntityRenderDispatcher d = Minecraft.getInstance().getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity, ?> renderer = d.getRenderer(entity);
		EntityRenderState state = renderer.createRenderState(entity, 1.0F);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		state.nameTag = null;
		if (state instanceof LivingEntityRenderState ls) {
			ls.bodyRot = bodyRot;
			ls.yRot = 0f;
			ls.xRot = 0f;
			ls.boundingBoxWidth = ls.boundingBoxWidth / ls.scale;
			ls.boundingBoxHeight = ls.boundingBoxHeight / ls.scale;
			ls.scale = 1.0F;
		}
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf pitch = new Quaternionf().rotateX(-12f * (float) (Math.PI / 180.0));
		rotation.mul(pitch);
		Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
		g.entity(state, size, translation, rotation, pitch, x0, y0, x1, y1);
	}
}
