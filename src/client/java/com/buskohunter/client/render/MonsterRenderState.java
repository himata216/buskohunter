package com.buskohunter.client.render;

import com.buskohunter.core.MonsterKind;
import com.buskohunter.monster.MonsterAnim;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** 大型モンスターの描画用状態。 */
public class MonsterRenderState extends LivingEntityRenderState {
	public MonsterKind kind = MonsterKind.BUSKOBA;
	public boolean dummy;
	public MonsterAnim anim = MonsterAnim.IDLE;
	/** アニメ開始からの経過 tick（部分 tick 込み）。 */
	public float animTime;
	public int animLength = 20;
	public int flags;
	public int broken;

	public boolean has(int flag) {
		return (flags & flag) != 0;
	}

	public boolean broken(int partIndex) {
		return partIndex >= 0 && (broken & (1 << partIndex)) != 0;
	}

	/** 0〜1 の進行度。 */
	public float progress() {
		if (animLength <= 0) return 0f;
		float t = animTime / animLength;
		return t < 0 ? 0 : (t > 1 ? 1 : t);
	}
}
