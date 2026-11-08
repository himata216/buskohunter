package com.buskohunter.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 弾・矢・猟虫・モンスターの飛び道具を 3D モデルで、飛んでいく向きに合わせて描く。
 * モデルは +Z が前。帽子は水平に回転、酒瓶などは回転しながら飛ぶ。
 */
public class OrientedProjectileRenderer<T extends Entity & ItemSupplier> extends EntityRenderer<T, OrientedProjectileRenderer.State> {
	/** 飛び方（見た目）。 */
	public enum Spin { NONE, TUMBLE, FLAT_SPIN, WOBBLE }

	private static final Map<Entity, float[]> LAST_DIR = new WeakHashMap<>();
	private final ItemModelResolver itemModelResolver;
	private final float scale;
	private final boolean fullBright;

	public OrientedProjectileRenderer(EntityRendererProvider.Context context, float scale, boolean fullBright) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
		this.scale = scale;
		this.fullBright = fullBright;
	}

	@Override
	protected int getBlockLightLevel(T entity, BlockPos blockPos) {
		return fullBright ? 15 : super.getBlockLightLevel(entity, blockPos);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		ItemStack stack = entity.getItem();
		itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.NONE, entity);
		Vec3 v = entity.getDeltaMovement();
		double h = Math.sqrt(v.x * v.x + v.z * v.z);
		float[] dir = LAST_DIR.computeIfAbsent(entity, e -> new float[]{e.getYRot(), -e.getXRot()});
		if (v.lengthSqr() > 1.0E-4) {
			dir[0] = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
			dir[1] = (float) (Mth.atan2(v.y, h) * Mth.RAD_TO_DEG);
		}
		state.yaw = dir[0];
		state.pitch = dir[1];
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		state.spin = switch (id) {
			case "proj_sombrero" -> Spin.FLAT_SPIN;
			case "proj_sake_bottle", "proj_glitch_cube" -> Spin.TUMBLE;
			case "proj_fireball", "proj_water_orb", "proj_holy_orb" -> Spin.WOBBLE;
			default -> Spin.NONE;
		};
		state.kinsect = id.equals("proj_kinsect");
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0, state.boundingBoxHeight * 0.5f, 0);
		float t = state.ageInTicks;
		switch (state.spin) {
			case FLAT_SPIN -> {
				poseStack.rotateDegrees(Axis.YP, t * 36f);
				poseStack.rotateDegrees(Axis.XP, 8f * Mth.sin(t * 0.4f));
			}
			case TUMBLE -> {
				poseStack.rotateDegrees(Axis.YP, state.yaw);
				poseStack.rotateDegrees(Axis.XP, t * 30f);
				poseStack.rotateDegrees(Axis.ZP, t * 11f);
			}
			case WOBBLE -> {
				poseStack.rotateDegrees(Axis.YP, state.yaw);
				poseStack.rotateDegrees(Axis.XP, -state.pitch);
				poseStack.rotateDegrees(Axis.ZP, t * 18f);
			}
			default -> {
				poseStack.rotateDegrees(Axis.YP, state.yaw);
				poseStack.rotateDegrees(Axis.XP, -state.pitch);
				if (state.kinsect) {
					// 羽ばたき風に少し揺らす
					poseStack.rotateDegrees(Axis.ZP, 10f * Mth.sin(t * 1.3f));
				}
			}
		}
		poseStack.scale(scale, scale, scale);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	public static class State extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public float yaw;
		public float pitch;
		public Spin spin = Spin.NONE;
		public boolean kinsect;
	}
}
