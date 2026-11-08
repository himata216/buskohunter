package com.buskohunter.combat;

import com.buskohunter.core.ElementType;
import com.buskohunter.core.WeaponType;

import java.util.ArrayList;
import java.util.List;

/**
 * プレイヤーの技 1 つ分のタイムライン定義（tick 単位, 20tick = 1秒）。
 * 攻撃判定・射撃・踏み込み・無敵・ガード・溜め・コンボ受付を持つ。
 */
public final class MoveDef {
	/** 攻撃判定の形。 */
	public enum Shape {
		/** 前方の扇形（range = 半径, angle = 半角）。 */
		ARC,
		/** 自分中心の円（全周）。 */
		CIRCLE,
		/** 前方の直線（range = 長さ, angle = 幅）。 */
		LINE
	}

	public record Hit(int tick, int repeat, int interval, Shape shape, float range, float angle, float height,
					  float mv, float elementMul, float statusMul, float ko, float fixedDamage, ElementType fixedElement,
					  WeaponType.DamageKind kindOverride, float sharpnessCost) {
	}

	public record Shot(int tick, ShotKind kind, int count, float mv) {
	}

	public record Lunge(int from, int to, float forward, float up) {
	}

	public final String id;
	public final WeaponType weapon;
	public final AnimKind anim;
	public final int duration;
	public final int cancelFrom;
	public final List<Hit> hits;
	public final List<Shot> shots;
	public final List<Lunge> lunges;
	public final int iFrom, iTo;
	public final int guardFrom, guardTo;
	public final int counterFrom, counterTo;
	public final boolean hyperArmor;
	public final float staminaCost;
	public final float staminaPerTick;
	public final int holdTick;
	public final InputKind holdButton;
	public final int[] chargeLevels;
	public final float[] chargeMul;
	public final int maxHold;
	public final boolean airborne;
	public final float moveSpeed;
	public final String sound;
	public final int soundTick;
	public final boolean mirror;
	/** 内部番号（ネットワーク同期用、MoveRegistry が割り当てる）。 */
	public int netId = -1;

	private MoveDef(Builder b) {
		this.id = b.id;
		this.weapon = b.weapon;
		this.anim = b.anim;
		this.duration = b.duration;
		this.cancelFrom = b.cancelFrom < 0 ? Math.max(1, (int) (b.duration * 0.6f)) : b.cancelFrom;
		this.hits = List.copyOf(b.hits);
		this.shots = List.copyOf(b.shots);
		this.lunges = List.copyOf(b.lunges);
		this.iFrom = b.iFrom;
		this.iTo = b.iTo;
		this.guardFrom = b.guardFrom;
		this.guardTo = b.guardTo;
		this.counterFrom = b.counterFrom;
		this.counterTo = b.counterTo;
		this.hyperArmor = b.hyperArmor;
		this.staminaCost = b.staminaCost;
		this.staminaPerTick = b.staminaPerTick;
		this.holdTick = b.holdTick;
		this.holdButton = b.holdButton;
		this.chargeLevels = b.chargeLevels;
		this.chargeMul = b.chargeMul;
		this.maxHold = b.maxHold;
		this.airborne = b.airborne;
		this.moveSpeed = b.moveSpeed;
		this.sound = b.sound;
		this.soundTick = b.soundTick;
		this.mirror = b.mirror;
	}

	public boolean chargeable() {
		return holdTick >= 0 && chargeLevels != null && chargeLevels.length > 0;
	}

	/** 溜め時間から溜め段階（0 = 溜めなし）。 */
	public int chargeLevelFor(int heldTicks) {
		if (!chargeable()) return 0;
		int lv = 0;
		for (int i = 0; i < chargeLevels.length; i++) {
			if (heldTicks >= chargeLevels[i]) lv = i + 1;
		}
		return lv;
	}

	public float chargeMultiplier(int level) {
		if (chargeMul == null || level <= 0) return 1.0f;
		return chargeMul[Math.min(level, chargeMul.length) - 1];
	}

	public static Builder builder(String id, WeaponType weapon, AnimKind anim, int duration) {
		return new Builder(id, weapon, anim, duration);
	}

	public static final class Builder {
		private final String id;
		private final WeaponType weapon;
		private final AnimKind anim;
		private final int duration;
		private int cancelFrom = -1;
		private final List<Hit> hits = new ArrayList<>();
		private final List<Shot> shots = new ArrayList<>();
		private final List<Lunge> lunges = new ArrayList<>();
		private int iFrom = -1, iTo = -1;
		private int guardFrom = -1, guardTo = -1;
		private int counterFrom = -1, counterTo = -1;
		private boolean hyperArmor;
		private float staminaCost;
		private float staminaPerTick;
		private int holdTick = -1;
		private InputKind holdButton = InputKind.A;
		private int[] chargeLevels;
		private float[] chargeMul;
		private int maxHold = 120;
		private boolean airborne;
		private float moveSpeed = 0.0f;
		private String sound;
		private int soundTick;
		private boolean mirror;

		private Builder(String id, WeaponType weapon, AnimKind anim, int duration) {
			this.id = id;
			this.weapon = weapon;
			this.anim = anim;
			this.duration = duration;
		}

		/** 次の技へ派生できるようになる tick。 */
		public Builder cancel(int tick) {
			this.cancelFrom = tick;
			return this;
		}

		/** 前方扇形の攻撃判定。 */
		public Builder arc(int tick, float range, float halfAngle, float mv) {
			return hit(tick, 1, 0, Shape.ARC, range, halfAngle, mv);
		}

		public Builder circle(int tick, float radius, float mv) {
			return hit(tick, 1, 0, Shape.CIRCLE, radius, 180f, mv);
		}

		public Builder line(int tick, float length, float width, float mv) {
			return hit(tick, 1, 0, Shape.LINE, length, width, mv);
		}

		public Builder multi(int tick, int repeat, int interval, Shape shape, float range, float angle, float mv) {
			return hit(tick, repeat, interval, shape, range, angle, mv);
		}

		public Builder hit(int tick, int repeat, int interval, Shape shape, float range, float angle, float mv) {
			hits.add(new Hit(tick, repeat, interval, shape, range, angle, 1.2f, mv, 1.0f, 1.0f, 0f, 0f, null, null, 1f));
			return this;
		}

		/** 直前の判定の詳細を差し替える。 */
		public Builder tune(float elementMul, float statusMul, float ko) {
			Hit h = hits.remove(hits.size() - 1);
			hits.add(new Hit(h.tick(), h.repeat(), h.interval(), h.shape(), h.range(), h.angle(), h.height(), h.mv(),
				elementMul, statusMul, ko, h.fixedDamage(), h.fixedElement(), h.kindOverride(), h.sharpnessCost()));
			return this;
		}

		/** 直前の判定を打撃扱いに（盾攻撃など）。 */
		public Builder blunt(float ko) {
			Hit h = hits.remove(hits.size() - 1);
			hits.add(new Hit(h.tick(), h.repeat(), h.interval(), h.shape(), h.range(), h.angle(), h.height(), h.mv(),
				h.elementMul(), h.statusMul(), ko, h.fixedDamage(), h.fixedElement(), WeaponType.DamageKind.BLUNT, h.sharpnessCost()));
			return this;
		}

		/** 固定ダメージの爆発（砲撃・ビン爆発）。肉質無視。 */
		public Builder fixed(int tick, int repeat, int interval, Shape shape, float range, float angle, float damage, ElementType element) {
			hits.add(new Hit(tick, repeat, interval, shape, range, angle, 1.2f, 0f, 0f, 0f, 0f, damage, element, null, 0f));
			return this;
		}

		/** 判定の高さ（足元からの中心高さ）。 */
		public Builder height(float h) {
			Hit last = hits.remove(hits.size() - 1);
			hits.add(new Hit(last.tick(), last.repeat(), last.interval(), last.shape(), last.range(), last.angle(), h,
				last.mv(), last.elementMul(), last.statusMul(), last.ko(), last.fixedDamage(), last.fixedElement(),
				last.kindOverride(), last.sharpnessCost()));
			return this;
		}

		public Builder shot(int tick, ShotKind kind, int count, float mv) {
			shots.add(new Shot(tick, kind, count, mv));
			return this;
		}

		public Builder lunge(int from, int to, float forward) {
			lunges.add(new Lunge(from, to, forward, 0f));
			return this;
		}

		public Builder lunge(int from, int to, float forward, float up) {
			lunges.add(new Lunge(from, to, forward, up));
			return this;
		}

		public Builder iframes(int from, int to) {
			this.iFrom = from;
			this.iTo = to;
			return this;
		}

		public Builder guard(int from, int to) {
			this.guardFrom = from;
			this.guardTo = to;
			return this;
		}

		public Builder counter(int from, int to) {
			this.counterFrom = from;
			this.counterTo = to;
			return this;
		}

		public Builder hyperArmor() {
			this.hyperArmor = true;
			return this;
		}

		public Builder stamina(float cost) {
			this.staminaCost = cost;
			return this;
		}

		public Builder staminaPerTick(float perTick) {
			this.staminaPerTick = perTick;
			return this;
		}

		/**
		 * 溜め技。holdTick で一時停止し、ボタンを離すまで溜める。
		 * levels = 段階に到達する溜め tick, mul = 各段階のダメージ倍率。
		 */
		public Builder charge(int holdTick, InputKind button, int[] levels, float[] mul, int maxHold) {
			this.holdTick = holdTick;
			this.holdButton = button;
			this.chargeLevels = levels;
			this.chargeMul = mul;
			this.maxHold = maxHold;
			return this;
		}

		public Builder airborne() {
			this.airborne = true;
			return this;
		}

		/** 技中の移動速度倍率（0 = 移動不可）。 */
		public Builder move(float speed) {
			this.moveSpeed = speed;
			return this;
		}

		public Builder sound(String key, int tick) {
			this.sound = key;
			this.soundTick = tick;
			return this;
		}

		public Builder mirror() {
			this.mirror = true;
			return this;
		}

		public MoveDef build() {
			return new MoveDef(this);
		}
	}
}
