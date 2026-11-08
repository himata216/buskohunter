package com.buskohunter.client.anim;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** 描画状態（プレイヤーの描画状態オブジェクト）ごとに、計算したポーズを覚えておく。 */
public final class PoseKeys {
	private PoseKeys() {
	}

	private static final Map<HumanoidRenderState, Pose> POSES = Collections.synchronizedMap(new WeakHashMap<>());

	public static void set(HumanoidRenderState state, Pose pose) {
		if (pose == null) POSES.remove(state);
		else POSES.put(state, pose);
	}

	public static Pose get(HumanoidRenderState state) {
		return POSES.get(state);
	}
}
