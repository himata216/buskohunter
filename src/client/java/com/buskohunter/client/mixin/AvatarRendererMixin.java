package com.buskohunter.client.mixin;

import com.buskohunter.client.anim.Pose;
import com.buskohunter.client.anim.PlayerAnimator;
import com.buskohunter.client.anim.PoseKeys;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** プレイヤー描画の状態抽出時に、MH 風モーションのポーズを計算して添付する。 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
	private void buskohunter$extractPose(AvatarlikeEntity entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		Pose pose = PlayerAnimator.compute(entity, entity.tickCount + partialTicks, state.isCrouching);
		PoseKeys.set(state, pose);
		com.buskohunter.client.render.LeftArmLayer.extract(entity, state);
	}
}
