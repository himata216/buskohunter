package com.buskohunter.client.mixin;

import com.buskohunter.client.anim.Pose;
import com.buskohunter.client.anim.PoseKeys;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 人型モデル（プレイヤー本体と、同じ状態を使う防具モデル）に MH 風のポーズを上書きする。
 * setupAnim は描画直前に呼ばれるので、ここで設定した値がそのまま描画される。
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
	@Shadow
	@Final
	public ModelPart head;
	@Shadow
	@Final
	public ModelPart body;
	@Shadow
	@Final
	public ModelPart rightArm;
	@Shadow
	@Final
	public ModelPart leftArm;
	@Shadow
	@Final
	public ModelPart rightLeg;
	@Shadow
	@Final
	public ModelPart leftLeg;

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
	private void buskohunter$applyPose(HumanoidRenderState state, CallbackInfo ci) {
		Pose p = PoseKeys.get(state);
		if (p == null || p.weight <= 0.001f) return;
		float w = Math.min(1f, p.weight);

		if (p.body) {
			body.xRot = lerp(w, body.xRot, p.bodyX);
			body.yRot = lerp(w, body.yRot, p.bodyY);
			body.zRot = lerp(w, body.zRot, p.bodyZ);
			float by = body.yRot;
			rightArm.z = (float) Math.sin(by) * 5.0f;
			rightArm.x = -(float) Math.cos(by) * 5.0f;
			leftArm.z = -(float) Math.sin(by) * 5.0f;
			leftArm.x = (float) Math.cos(by) * 5.0f;
		}
		if (p.arms) {
			float rx = p.rArmX, ry = p.rArmY, lx = p.lArmX, ly = p.lArmY;
			if (p.armsFollowHead) {
				rx += head.xRot;
				lx += head.xRot;
				ry += head.yRot;
				ly += head.yRot;
			}
			if (p.body) {
				ry += p.bodyY;
				ly += p.bodyY;
			}
			rightArm.xRot = lerp(w, rightArm.xRot, rx);
			rightArm.yRot = lerp(w, rightArm.yRot, ry);
			rightArm.zRot = lerp(w, rightArm.zRot, p.rArmZ);
			leftArm.xRot = lerp(w, leftArm.xRot, lx);
			leftArm.yRot = lerp(w, leftArm.yRot, ly);
			leftArm.zRot = lerp(w, leftArm.zRot, p.lArmZ);
		}
		if (p.legs) {
			rightLeg.xRot = lerp(w, rightLeg.xRot, p.rLegX);
			leftLeg.xRot = lerp(w, leftLeg.xRot, p.lLegX);
		}
		if (p.head) {
			head.xRot += p.headX * w;
			head.yRot += p.headY * w;
		}
		if (p.root) {
			buskohunter$applyRoot(p, w);
		}
	}

	/** 体の中心（y=12）を軸に全身を回転させる（回避ローリング・前傾など）。 */
	@Unique
	private void buskohunter$applyRoot(Pose p, float w) {
		ModelPart root = ((net.minecraft.client.model.Model<?>) (Object) this).root();
		float ax = p.rootX * w, ay = p.rootYaw * w, az = p.rootZ * w;
		root.xRot = ax;
		root.yRot = ay;
		root.zRot = az;
		// R = Rz * Ry * Rx を c = (0, 12, 0) に適用
		double cy = 12.0;
		double x1 = 0, y1 = cy * Math.cos(ax), z1 = cy * Math.sin(ax);
		double x2 = x1 * Math.cos(ay) + z1 * Math.sin(ay);
		double z2 = -x1 * Math.sin(ay) + z1 * Math.cos(ay);
		double y2 = y1;
		double x3 = x2 * Math.cos(az) - y2 * Math.sin(az);
		double y3 = x2 * Math.sin(az) + y2 * Math.cos(az);
		root.x = (float) (0 - x3);
		root.y = (float) (cy - y3) + p.rootY * w;
		root.z = (float) (0 - z2);
	}

	@Unique
	private static float lerp(float t, float a, float b) {
		return a + (b - a) * t;
	}
}
