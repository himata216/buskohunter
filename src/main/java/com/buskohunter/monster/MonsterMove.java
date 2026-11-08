package com.buskohunter.monster;

import com.buskohunter.core.ElementType;

import java.util.ArrayList;
import java.util.List;

/**
 * モンスターの攻撃 1 つ分のタイムライン（データ定義）。
 * 実行は {@link MonsterAttackRunner} が行う。
 */
public final class MonsterMove {
	public enum Shape {
		/** 本体ローカル座標の球（forward/up/side, radius）。 */
		SPHERE,
		/** 本体中心から広がるリング（radius = 最大半径）。 */
		SHOCKWAVE,
		/** 頭（または指定位置）から前方への直線ビーム（radius = 長さ, width）。 */
		BEAM,
		/** 前方の扇形ブレス（radius = 射程, width = 半角）。 */
		CONE,
		/** 本体中心の円。 */
		CIRCLE
	}

	public enum Knock {
		/** のけぞり。 */
		SMALL,
		/** 吹っ飛ばし（ダウン）。 */
		LARGE,
		/** 打ち上げ。 */
		LAUNCH,
		/** 風圧（ダメージ小、押し出し）。 */
		WIND
	}

	public enum Fx {
		NONE, DUST, FIRE, WATER, THUNDER, ICE, HOLY, GLITCH, SAKE, SHOCK, NEON, DRAGON, BUBBLE
	}

	/** 特殊処理。 */
	public enum Special {
		NONE,
		/** 咆哮（範囲内のプレイヤーを硬直）。 */
		ROAR,
		/** 頭上から落ちる光柱（プレイヤー位置に予兆→落雷）。 */
		PILLARS,
		/** プレイヤーの足元から間欠泉。 */
		GEYSERS,
		/** マグマ噴出。 */
		ERUPTIONS,
		/** POSE YOUR GAME（動いた者を裁く）。 */
		POSE_JUDGE,
		/** ターゲット背後へテレポート。 */
		TELEPORT_BEHIND,
		/** 周囲の地形を破壊。 */
		TERRAIN_BREAK,
		/** 消去の光（地上にいる者へ広がる波）。 */
		ERASE_WAVE,
		/** 酒の池を撒く。 */
		SAKE_POOLS,
		/** 体力を少し回復（オアシスの笑み）。 */
		HEAL_SMILE
	}

	public record Hit(int from, int to, int interval, Shape shape, float forward, float up, float side,
					  float radius, float width, float damage, Knock knock, ElementType element, Fx fx) {
	}

	public record Projectile(int tick, MonsterProjectileKind kind, int count, float spread, float speed,
							 float damage, boolean aimAtTarget) {
	}

	public record Motion(int from, int to, float forward, float up, boolean towardTarget) {
	}

	public final String id;
	public final MonsterAnim anim;
	public final int duration;
	/** この tick までターゲットの方を向き続ける。 */
	public final int trackUntil;
	public final float minRange, maxRange;
	/** ターゲットが前方(0)/後方(1)/どこでも(2)。 */
	public final int facing;
	public final int weight;
	public final int cooldown;
	public final boolean enragedOnly;
	public final int minRank;
	public final List<Hit> hits;
	public final List<Projectile> projectiles;
	public final List<Motion> motions;
	public final Special special;
	public final int specialTick;
	public final String sound;
	public final int soundTick;
	public final Fx telegraph;
	public final boolean flying;

	private MonsterMove(Builder b) {
		this.id = b.id;
		this.anim = b.anim;
		this.duration = b.duration;
		this.trackUntil = b.trackUntil;
		this.minRange = b.minRange;
		this.maxRange = b.maxRange;
		this.facing = b.facing;
		this.weight = b.weight;
		this.cooldown = b.cooldown;
		this.enragedOnly = b.enragedOnly;
		this.minRank = b.minRank;
		this.hits = List.copyOf(b.hits);
		this.projectiles = List.copyOf(b.projectiles);
		this.motions = List.copyOf(b.motions);
		this.special = b.special;
		this.specialTick = b.specialTick;
		this.sound = b.sound;
		this.soundTick = b.soundTick;
		this.telegraph = b.telegraph;
		this.flying = b.flying;
	}

	public static Builder of(String id, MonsterAnim anim, int duration) {
		return new Builder(id, anim, duration);
	}

	public static final class Builder {
		private final String id;
		private final MonsterAnim anim;
		private final int duration;
		private int trackUntil = 8;
		private float minRange = 0, maxRange = 8;
		private int facing = 0;
		private int weight = 1;
		private int cooldown = 0;
		private boolean enragedOnly;
		private int minRank;
		private final List<Hit> hits = new ArrayList<>();
		private final List<Projectile> projectiles = new ArrayList<>();
		private final List<Motion> motions = new ArrayList<>();
		private Special special = Special.NONE;
		private int specialTick;
		private String sound;
		private int soundTick;
		private Fx telegraph = Fx.NONE;
		private boolean flying;

		private Builder(String id, MonsterAnim anim, int duration) {
			this.id = id;
			this.anim = anim;
			this.duration = duration;
		}

		public Builder track(int untilTick) {
			this.trackUntil = untilTick;
			return this;
		}

		public Builder range(float min, float max) {
			this.minRange = min;
			this.maxRange = max;
			return this;
		}

		/** ターゲットが後ろにいる時に使う技。 */
		public Builder behind() {
			this.facing = 1;
			return this;
		}

		public Builder anyFacing() {
			this.facing = 2;
			return this;
		}

		public Builder weight(int w) {
			this.weight = w;
			return this;
		}

		public Builder cooldown(int ticks) {
			this.cooldown = ticks;
			return this;
		}

		public Builder enraged() {
			this.enragedOnly = true;
			return this;
		}

		/** 0=下位から, 1=上位から, 2=G級のみ。 */
		public Builder rank(int r) {
			this.minRank = r;
			return this;
		}

		public Builder sphere(int from, int to, float fwd, float up, float side, float r, float dmg, Knock k, ElementType e, Fx fx) {
			hits.add(new Hit(from, to, 0, Shape.SPHERE, fwd, up, side, r, 0, dmg, k, e, fx));
			return this;
		}

		public Builder sphere(int from, int to, float fwd, float up, float side, float r, float dmg, Knock k) {
			return sphere(from, to, fwd, up, side, r, dmg, k, ElementType.NONE, Fx.DUST);
		}

		public Builder circle(int tick, float r, float dmg, Knock k, ElementType e, Fx fx) {
			hits.add(new Hit(tick, tick, 0, Shape.CIRCLE, 0, 0, 0, r, 0, dmg, k, e, fx));
			return this;
		}

		public Builder shockwave(int from, int to, float r, float dmg, Knock k, ElementType e, Fx fx) {
			hits.add(new Hit(from, to, 0, Shape.SHOCKWAVE, 0, 0, 0, r, 1.2f, dmg, k, e, fx));
			return this;
		}

		public Builder beam(int from, int to, int interval, float up, float length, float width, float dmg, ElementType e, Fx fx) {
			hits.add(new Hit(from, to, interval, Shape.BEAM, 0.8f, up, 0, length, width, dmg, Knock.SMALL, e, fx));
			return this;
		}

		/** 後方へ撃つビーム（尻から）。 */
		public Builder rearBeam(int from, int to, int interval, float up, float side, float length, float width, float dmg, ElementType e, Fx fx) {
			hits.add(new Hit(from, to, interval, Shape.BEAM, -1.5f, up, side, -length, width, dmg, Knock.SMALL, e, fx));
			return this;
		}

		public Builder cone(int from, int to, int interval, float up, float range, float halfAngle, float dmg, ElementType e, Fx fx) {
			hits.add(new Hit(from, to, interval, Shape.CONE, 0.8f, up, 0, range, halfAngle, dmg, Knock.SMALL, e, fx));
			return this;
		}

		public Builder multiSphere(int from, int to, int interval, float fwd, float up, float side, float r, float dmg, Knock k, ElementType e, Fx fx) {
			hits.add(new Hit(from, to, interval, Shape.SPHERE, fwd, up, side, r, 0, dmg, k, e, fx));
			return this;
		}

		public Builder multiCircle(int from, int to, int interval, float r, float dmg, Knock k, ElementType e, Fx fx) {
			hits.add(new Hit(from, to, interval, Shape.CIRCLE, 0, 0, 0, r, 0, dmg, k, e, fx));
			return this;
		}

		public Builder projectile(int tick, MonsterProjectileKind kind, int count, float spread, float speed, float dmg) {
			projectiles.add(new Projectile(tick, kind, count, spread, speed, dmg, true));
			return this;
		}

		public Builder charge(int from, int to, float speed) {
			motions.add(new Motion(from, to, speed, 0f, false));
			return this;
		}

		/** ターゲットへ向かって跳ぶ（up は上昇速度）。 */
		public Builder leap(int from, int to, float speed, float up) {
			motions.add(new Motion(from, to, speed, up, true));
			return this;
		}

		public Builder rise(int from, int to, float up) {
			motions.add(new Motion(from, to, 0f, up, false));
			return this;
		}

		public Builder special(Special s, int tick) {
			this.special = s;
			this.specialTick = tick;
			return this;
		}

		public Builder sound(String key, int tick) {
			this.sound = key;
			this.soundTick = tick;
			return this;
		}

		public Builder telegraph(Fx fx) {
			this.telegraph = fx;
			return this;
		}

		public Builder flying() {
			this.flying = true;
			return this;
		}

		public MonsterMove build() {
			return new MonsterMove(this);
		}
	}
}
