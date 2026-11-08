package com.buskohunter.client.render;

import com.buskohunter.client.anim.PlayerAnimator;
import com.buskohunter.item.HunterWeaponItem;
import com.buskohunter.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 左腕に、武器に付いている物を描く：片手剣・ランス・ガンランス・チャージアックスの盾、双剣の左、操虫棍の猟虫。
 * 盾などは武器と同じ属性色。猟虫を飛ばしている間は腕から消える。
 */
public class LeftArmLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public static ItemModelResolver resolver;
	private static final Map<AvatarRenderState, ItemStackRenderState> ITEMS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<String, ItemStack> STACKS = new HashMap<>();

	public LeftArmLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	/** プレイヤーの描画状態を作る時に呼ぶ（AvatarRendererMixin）。 */
	public static void extract(LivingEntity entity, AvatarRenderState state) {
		ItemStackRenderState out = ITEMS.computeIfAbsent(state, k -> new ItemStackRenderState());
		out.clear();
		if (resolver == null || state.mainArm != HumanoidArm.RIGHT) return;
		if (!entity.getOffhandItem().isEmpty()) return;
		ItemStack main = entity.getMainHandItem();
		if (!(main.getItem() instanceof HunterWeaponItem w)) return;
		int vis = PlayerAnimator.visualOf(entity);
		String id = switch (w.weaponType) {
			case SWORD_AND_SHIELD -> "disp_shield_small";
			case LANCE, GUNLANCE -> "disp_shield_large";
			case CHARGE_BLADE -> (vis & 2) != 0 ? null : "disp_shield_cb";
			case DUAL_BLADES -> "disp_dual_left";
			case INSECT_GLAIVE -> (vis & 16) != 0 ? null : "disp_kinsect";
			default -> null;
		};
		if (id == null) return;
		int color = -1;
		CustomModelData cmd = main.get(DataComponents.CUSTOM_MODEL_DATA);
		if (cmd != null && !cmd.colors().isEmpty()) color = cmd.colors().get(0);
		resolver.updateForLiving(out, stack(id, color), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
	}

	private static ItemStack stack(String id, int color) {
		String key = id + "#" + color;
		ItemStack st = STACKS.get(key);
		if (st == null) {
			Item item = ModItems.projectile(id);
			st = new ItemStack(item);
			st.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(color)));
			STACKS.put(key, st);
		}
		return st;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
		ItemStackRenderState item = ITEMS.get(state);
		if (item == null || item.isEmpty() || state.isInvisible) return;
		poseStack.pushPose();
		getParentModel().translateToHand(state, HumanoidArm.LEFT, poseStack);
		poseStack.rotateDegrees(Axis.XP, -90.0F);
		poseStack.rotateDegrees(Axis.YP, 180.0F);
		poseStack.translate(-1 / 16.0F, 2 / 16.0F, -10 / 16.0F);
		item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
