package com.buskohunter.monster;

import com.buskohunter.net.Net;
import com.buskohunter.combat.DamageCalc;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.QuestDef;
import com.buskohunter.hunter.HunterManager;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.net.Payloads;
import com.buskohunter.quest.QuestManager;
import com.buskohunter.quest.QuestSession;
import com.buskohunter.registry.ModItems;
import com.buskohunter.util.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 狩猟対象の大型モンスター（5 種共通の本体）。
 * 体力はバニラの体力ではなく独自の HP を使い、部位・肉質・状態異常・怒り・疲労・エリア移動・
 * 弱り（足を引きずる→巣で眠る）・捕獲・剥ぎ取りをすべて管理する。
 */
public class HuntMonster extends PathfinderMob {
	// ---- 同期データ ----
	private static final EntityDataAccessor<Integer> DATA_ANIM = SynchedEntityData.defineId(HuntMonster.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ANIM_START = SynchedEntityData.defineId(HuntMonster.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ANIM_LEN = SynchedEntityData.defineId(HuntMonster.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_FLAGS = SynchedEntityData.defineId(HuntMonster.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_BROKEN = SynchedEntityData.defineId(HuntMonster.class, EntityDataSerializers.INT);

	public enum State {
		IDLE, COMBAT, MOVE_AREA, FLINCH, DOWN, STUN, STATUS_SLEEP, PARALYZED, TRAPPED, PITFALL, NEST_SLEEP, DEAD, CAPTURED
	}

	public final MonsterKind kind;
	private final Random rng = new Random();
	final MonsterAttackRunner runner = new MonsterAttackRunner(this);

	// ---- 体力と部位 ----
	private float hp = 1000;
	private float maxHp = 1000;
	private int rank = 0;
	private float attackMul = 1f;
	private final float[] partDamage;
	private int broken;

	// ---- 状態 ----
	private State state = State.IDLE;
	private int stateTimer;
	private int flags;
	private boolean enraged;
	private int enrageTimer;
	private float enrageAccum;
	private int tiredTimer;
	private float flinchAccum;
	private float areaMoveAccum;
	private int combatTicks;
	private int noTargetTicks;
	private int attackCooldown = 40;
	private int paintTimer;
	private int buskobaTimer;
	private int poisonTimer;
	private int blindTimer;
	private boolean limping;
	private boolean announcedWeak;
	private int tranqHits;
	private boolean discovered;

	// ---- 状態異常の蓄積 ----
	private final float[] statusBuild = new float[5];
	private final float[] statusThreshold = {150, 150, 150, 120, 140};
	private float ko;
	private float koThreshold = 150;

	// ---- クエスト・エリア ----
	private int sessionId = -1;
	private int targetIndex;
	private int[] areaX = new int[0], areaY = new int[0], areaZ = new int[0];
	private int currentArea;
	private int nestArea;
	private int moveTarget = -1;
	private int wanderTimer;
	private UUID aggroTarget;
	private final Map<UUID, Float> damageBy = new HashMap<>();
	private final Map<UUID, Integer> carves = new HashMap<>();
	private int deadTicks;

	public HuntMonster(EntityType<? extends HuntMonster> type, Level level, MonsterKind kind) {
		super(type, level);
		this.kind = kind;
		this.partDamage = new float[kind.parts.size()];
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes(MonsterKind kind) {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 1000.0)
			.add(Attributes.MOVEMENT_SPEED, kind.moveSpeed)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.STEP_HEIGHT, 2.5)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.SAFE_FALL_DISTANCE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ANIM, MonsterAnim.IDLE.ordinal());
		builder.define(DATA_ANIM_START, 0);
		builder.define(DATA_ANIM_LEN, 20);
		builder.define(DATA_FLAGS, 0);
		builder.define(DATA_BROKEN, 0);
	}

	@Override
	protected void registerGoals() {
		// AI はすべて tick() 内の独自ステートマシンで動かす
	}

	// =====================================================================
	// 同期データのアクセサ（クライアント描画用）
	// =====================================================================
	public MonsterAnim getAnim() {
		return MonsterAnim.byOrdinal(entityData.get(DATA_ANIM));
	}

	public int getAnimStart() {
		return entityData.get(DATA_ANIM_START);
	}

	public int getAnimLength() {
		return entityData.get(DATA_ANIM_LEN);
	}

	public int getMonsterFlags() {
		return entityData.get(DATA_FLAGS);
	}

	public int getBrokenParts() {
		return entityData.get(DATA_BROKEN);
	}

	public boolean hasFlag(int f) {
		return (getMonsterFlags() & f) != 0;
	}

	/** 剥ぎ取りできる（討伐済み）か。 */
	public boolean isCarvable() {
		return hasFlag(MonsterFlags.DEAD);
	}

	public void playAnim(MonsterAnim anim, int length) {
		entityData.set(DATA_ANIM, anim.ordinal());
		entityData.set(DATA_ANIM_START, this.tickCount);
		entityData.set(DATA_ANIM_LEN, Math.max(1, length));
	}

	private void setFlag(int f, boolean on) {
		flags = on ? (flags | f) : (flags & ~f);
	}

	// =====================================================================
	// クエストからの初期化
	// =====================================================================
	public void setupQuest(int sessionId, int targetIndex, QuestDef.Rank rank, int players,
						   int[] ax, int[] ay, int[] az, int startArea, int nestArea) {
		this.sessionId = sessionId;
		this.targetIndex = targetIndex;
		this.rank = rank.ordinal();
		float multi = switch (Math.max(1, players)) {
			case 1 -> 1.0f;
			case 2 -> 1.6f;
			case 3 -> 2.1f;
			default -> 2.6f;
		};
		this.maxHp = kind.baseHp * rank.hpMul * multi;
		this.hp = maxHp;
		this.attackMul = rank.attackMul;
		this.areaX = ax;
		this.areaY = ay;
		this.areaZ = az;
		this.currentArea = startArea;
		this.nestArea = nestArea;
		for (int i = 0; i < statusThreshold.length; i++) statusThreshold[i] *= (1f + this.rank * 0.25f);
		this.koThreshold *= (1f + this.rank * 0.25f);
		if (kind == MonsterKind.BARINO) setFlag(MonsterFlags.HOVER, true);
	}

	/** 訓練・コマンド召喚用（クエスト外）。 */
	public void setupStandalone(QuestDef.Rank rank) {
		setupQuest(-1, 0, rank, 1, new int[]{(int) getX()}, new int[]{(int) getY()}, new int[]{(int) getZ()}, 0, 0);
	}

	public int sessionId() {
		return sessionId;
	}

	public int targetIndex() {
		return targetIndex;
	}

	public float hp() {
		return hp;
	}

	public float maxHp() {
		return maxHp;
	}

	public float hpRatio() {
		return maxHp <= 0 ? 0 : hp / maxHp;
	}

	public int currentArea() {
		return currentArea;
	}

	public boolean isDiscovered() {
		return discovered || paintTimer > 0;
	}

	public boolean isPainted() {
		return paintTimer > 0;
	}

	public boolean isEnraged() {
		return enraged;
	}

	public boolean isLimping() {
		return limping;
	}

	public boolean isCapturable() {
		return hpRatio() <= 0.25f && state != State.DEAD && state != State.CAPTURED;
	}

	public boolean isDead() {
		return state == State.DEAD;
	}

	public boolean isCaptured() {
		return state == State.CAPTURED;
	}

	public State state() {
		return state;
	}

	public float attackMul() {
		return attackMul * (enraged ? 1.25f : 1f);
	}

	public int rankIndex() {
		return rank;
	}

	Random rng() {
		return rng;
	}

	// =====================================================================
	// バニラのダメージ・押し出し・デスポーンは無効
	// =====================================================================
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide() && player instanceof ServerPlayer sp && state == State.DEAD) {
			carve(sp);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	// =====================================================================
	// メインループ
	// =====================================================================
	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) return;

		if (sessionId >= 0 && tickCount % 40 == 0 && QuestManager.find(level.getServer(), sessionId) == null) {
			// クエストが終わった（サーバー再起動など）モンスターは消す
			discard();
			return;
		}

		if (state == State.DEAD || state == State.CAPTURED) {
			deadTicks++;
			getNavigation().stop();
			setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
			syncFlags();
			return;
		}

		tickTimers(level);
		runner.tickPending(level);

		if (runner.active()) {
			runner.tick(level);
		} else {
			switch (state) {
				case IDLE -> tickIdle(level);
				case COMBAT -> tickCombat(level);
				case MOVE_AREA -> tickMoveArea(level);
				case NEST_SLEEP -> tickNestSleep(level);
				default -> tickTimedState(level);
			}
		}
		if (kind == MonsterKind.BARINO && !runner.active()) hover(level);
		if (tickCount % 5 == 0) clearPath(level);
		if (tickCount % 20 == 0) ambientFx(level);
		syncFlags();
	}

	private void syncFlags() {
		setFlag(MonsterFlags.ENRAGED, enraged);
		setFlag(MonsterFlags.TIRED, tiredTimer > 0);
		setFlag(MonsterFlags.LIMPING, limping && state != State.NEST_SLEEP);
		setFlag(MonsterFlags.BUSKOBA, buskobaTimer > 0);
		setFlag(MonsterFlags.ASLEEP, state == State.STATUS_SLEEP || state == State.NEST_SLEEP);
		setFlag(MonsterFlags.DEAD, state == State.DEAD);
		setFlag(MonsterFlags.CAPTURED, state == State.CAPTURED);
		setFlag(MonsterFlags.PAINTED, paintTimer > 0);
		setFlag(MonsterFlags.TRAPPED, state == State.TRAPPED || state == State.PITFALL || state == State.PARALYZED);
		setFlag(MonsterFlags.CAPTURABLE, isCapturable());
		setFlag(MonsterFlags.BLINDED, blindTimer > 0);
		setFlag(MonsterFlags.STUNNED, state == State.STUN);
		if (entityData.get(DATA_FLAGS) != flags) entityData.set(DATA_FLAGS, flags);
		if (entityData.get(DATA_BROKEN) != broken) entityData.set(DATA_BROKEN, broken);
	}

	private void tickTimers(ServerLevel level) {
		if (enraged && --enrageTimer <= 0) {
			enraged = false;
			tiredTimer = 800;
			Fx.sound(level, position(), "monster.tired", 1.2f, 1f);
		}
		if (tiredTimer > 0) tiredTimer--;
		if (paintTimer > 0) paintTimer--;
		if (buskobaTimer > 0) buskobaTimer--;
		if (blindTimer > 0) blindTimer--;
		if (poisonTimer > 0) {
			poisonTimer--;
			if (poisonTimer % 20 == 0) {
				applyRawDamage(level, maxHp * 0.0025f, null);
				Fx.particle(level, ParticleTypes.WITCH, getX(), getY() + kind.height * 0.6, getZ(), 6, kind.width * 0.4, 0.5, kind.width * 0.4, 0);
			}
		}
		if (state == State.COMBAT || runner.active()) combatTicks++;
		if (attackCooldown > 0) attackCooldown--;
		if (tiredTimer > 0 && tickCount % 40 == 0) {
			Fx.particle(level, ParticleTypes.FALLING_WATER, getX(), getY() + kind.height * 0.7, getZ(), 4, 0.4, 0.1, 0.4, 0);
		}
		if (enraged && tickCount % 6 == 0) {
			Fx.particle(level, ParticleTypes.SMOKE, getX(), getY() + kind.height * 0.9, getZ(), 3, kind.width * 0.3, 0.2, kind.width * 0.3, 0.02);
		}
	}

	private void ambientFx(ServerLevel level) {
		switch (kind) {
			case JANGRIA -> Fx.particle(level, ParticleTypes.FLAME, getX(), getY() + 2, getZ(), 4, 1.5, 1.2, 1.5, 0.01);
			case BUSAIBER -> Fx.burst(level, MonsterMove.Fx.NEON, position().add(0, 2.5, 0), 3, 1.4);
			case BARINO -> Fx.particle(level, ParticleTypes.END_ROD, getX(), getY() + 4.5, getZ(), 2, 1.2, 0.6, 1.2, 0.01);
			case BUSKOBA -> {
				if (rng.nextInt(3) == 0) Fx.particle(level, ParticleTypes.DRIPPING_WATER, getX(), getY() + 2, getZ(), 3, 1.5, 0.6, 1.5, 0);
			}
			default -> {
			}
		}
		if (state == State.STATUS_SLEEP || state == State.NEST_SLEEP) {
			Fx.sound(level, position(), "monster.sleep", 0.8f, 1f);
		}
	}

	// =====================================================================
	// 状態ごとの処理
	// =====================================================================
	private void tickIdle(ServerLevel level) {
		if (playAnimIfChanged(MonsterAnim.IDLE)) return;
		ServerPlayer seen = nearestPlayer(level, 22, true);
		if (seen != null) {
			notice(level, seen);
			return;
		}
		if (--wanderTimer <= 0) {
			wanderTimer = 120 + rng.nextInt(120);
			BlockPos c = areaCenter(currentArea);
			double tx = c.getX() + (rng.nextDouble() - 0.5) * 20;
			double tz = c.getZ() + (rng.nextDouble() - 0.5) * 20;
			int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(tx), Mth.floor(tz));
			getMoveControl().setWantedPosition(tx, ty, tz, 0.55);
			if (rng.nextInt(4) == 0) {
				playAnim(MonsterAnim.EAT, 60);
			}
		}
	}

	/** プレイヤーを発見：咆哮して戦闘へ。 */
	private void notice(ServerLevel level, ServerPlayer player) {
		aggroTarget = player.getUUID();
		boolean first = !discovered;
		discovered = true;
		state = State.COMBAT;
		noTargetTicks = 0;
		runner.start(MonsterMoveSets.roar(kind), player);
		if (first) {
			QuestSession s = QuestManager.find(level.getServer(), sessionId);
			if (s != null) {
				s.broadcastBanner(level.getServer(), new Payloads.Banner(Payloads.Banner.MONSTER_INTRO, kind.jaName, kind.title, kind.color));
				s.playToMembers(level.getServer(), "quest.alert");
			}
		}
	}

	private void tickCombat(ServerLevel level) {
		ServerPlayer target = currentTarget(level);
		if (target == null) {
			if (++noTargetTicks > 200) {
				state = State.IDLE;
				aggroTarget = null;
			}
			return;
		}
		noTargetTicks = 0;

		// 弱った → 巣へ
		if (hpRatio() <= 0.2f && sessionId >= 0 && currentArea != nestArea && rng.nextInt(60) == 0) {
			startAreaMove(level, nestArea, true);
			return;
		}
		// エリア移動
		if (sessionId >= 0 && areaX.length > 1 && (areaMoveAccum > maxHp * 0.16f || combatTicks > 20 * 150)) {
			int next;
			do {
				next = rng.nextInt(areaX.length);
			} while (next == currentArea && areaX.length > 1);
			startAreaMove(level, next, false);
			return;
		}

		double dist = Math.sqrt(distanceToSqr(target));
		if (attackCooldown <= 0) {
			MonsterMove move = chooseMove(target, dist);
			if (move != null) {
				runner.start(move, target);
				int base = switch (rank) {
					case 0 -> 26;
					case 1 -> 18;
					default -> 12;
				};
				if (enraged) base = (int) (base * 0.55f);
				if (tiredTimer > 0) base = (int) (base * 1.8f);
				attackCooldown = base + rng.nextInt(base + 1);
				return;
			}
		}
		// 間合いを取る
		float speed = limping ? 0.55f : (enraged ? 1.15f : 0.85f);
		if (buskobaTimer > 0) speed *= 0.7f;
		if (tiredTimer > 0) speed *= 0.75f;
		if (dist > 5.5) {
			getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speed);
			playAnimIfChanged(dist > 14 ? MonsterAnim.RUN : MonsterAnim.WALK);
		} else {
			getNavigation().stop();
			faceTowards(target.position(), 8f);
			playAnimIfChanged(limping ? MonsterAnim.LIMP : MonsterAnim.IDLE);
		}
	}

	private MonsterMove chooseMove(ServerPlayer target, double dist) {
		List<MonsterMove> moves = MonsterMoveSets.of(kind);
		Vec3 toT = target.position().subtract(position());
		float yaw = getYRot();
		Vec3 fwd = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
		double dot = (toT.x * fwd.x + toT.z * fwd.z) / Math.max(0.01, Math.sqrt(toT.x * toT.x + toT.z * toT.z));
		boolean behind = dot < -0.3;
		List<MonsterMove> ok = new ArrayList<>();
		int total = 0;
		for (MonsterMove m : moves) {
			if (m.enragedOnly && !enraged) continue;
			if (m.minRank > rank) continue;
			if (dist < m.minRange || dist > m.maxRange) continue;
			if (runner.onCooldown(m)) continue;
			if (m.facing == 0 && behind) continue;
			if (m.facing == 1 && !behind) continue;
			if (blindTimer > 0 && rng.nextInt(2) == 0) continue;
			int w = m.weight * (enraged && m.enragedOnly ? 2 : 1);
			ok.add(m);
			total += w;
		}
		if (ok.isEmpty()) return null;
		int r = rng.nextInt(total);
		for (MonsterMove m : ok) {
			int w = m.weight * (enraged && m.enragedOnly ? 2 : 1);
			if (r < w) return m;
			r -= w;
		}
		return ok.get(0);
	}

	private void startAreaMove(ServerLevel level, int area, boolean toNest) {
		moveTarget = area;
		state = State.MOVE_AREA;
		stateTimer = 0;
		areaMoveAccum = 0;
		combatTicks = 0;
		if (toNest) {
			limping = true;
		}
		QuestSession s = QuestManager.find(level.getServer(), sessionId);
		if (s != null && isDiscovered()) {
			String msg = toNest ? kind.jaName + "が足を引きずって逃げていく…（エリア" + (area + 1) + "へ）"
				: kind.jaName + "がエリア" + (area + 1) + "へ移動した";
			s.broadcastBanner(level.getServer(), new Payloads.Banner(Payloads.Banner.AREA_MOVE, msg, "", 0xFFE0A0));
			s.playToMembers(level.getServer(), "quest.area");
		}
	}

	private void tickMoveArea(ServerLevel level) {
		BlockPos c = areaCenter(moveTarget);
		double dx = c.getX() - getX(), dz = c.getZ() - getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		stateTimer++;
		boolean canFly = kind != MonsterKind.SAKERAN && kind != MonsterKind.BUSKOBA;
		if (d > 70 || (stateTimer > 200 && d > 12)) {
			// 遠い・詰まった：飛び去る（またはジャンプで移動）演出の後にワープ
			if (stateTimer < 230 || stateTimer > 260) stateTimer = 230;
			playAnimIfChanged(canFly ? MonsterAnim.FLY_UP : MonsterAnim.LEAP);
			setDeltaMovement(getDeltaMovement().x * 0.5, 0.6, getDeltaMovement().z * 0.5);
			setNoGravity(true);
			if (stateTimer >= 255) {
				int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
				setNoGravity(kind == MonsterKind.BARINO);
				teleportTo(c.getX() + 0.5, ty + 1, c.getZ() + 0.5);
				arriveArea(level);
			}
			return;
		}
		if (d < 6) {
			arriveArea(level);
			return;
		}
		float speed = limping ? 0.6f : 1.0f;
		getMoveControl().setWantedPosition(c.getX(), c.getY(), c.getZ(), speed);
		playAnimIfChanged(limping ? MonsterAnim.LIMP : MonsterAnim.RUN);
	}

	private void arriveArea(ServerLevel level) {
		currentArea = moveTarget;
		moveTarget = -1;
		setNoGravity(kind == MonsterKind.BARINO);
		if (limping && currentArea == nestArea) {
			state = State.NEST_SLEEP;
			stateTimer = 20 * 120;
			playAnim(MonsterAnim.SLEEP, 40);
			return;
		}
		state = State.IDLE;
		wanderTimer = 0;
	}

	private void tickNestSleep(ServerLevel level) {
		hp = Math.min(maxHp, hp + maxHp * 0.0004f);
		if (--stateTimer <= 0) {
			wake(level);
		}
	}

	private void wake(ServerLevel level) {
		state = State.COMBAT;
		limping = hpRatio() <= 0.25f;
		runner.start(MonsterMoveSets.roar(kind), currentTarget(level));
	}

	private void tickTimedState(ServerLevel level) {
		getNavigation().stop();
		if (--stateTimer <= 0) {
			if (state == State.STATUS_SLEEP) {
				state = State.COMBAT;
			} else {
				state = State.COMBAT;
			}
			playAnim(MonsterAnim.IDLE, 20);
		}
	}

	private void enterTimedState(State s, int ticks, MonsterAnim anim) {
		runner.interrupt();
		state = s;
		stateTimer = ticks;
		playAnim(anim, ticks);
		getNavigation().stop();
		setDeltaMovement(0, getDeltaMovement().y, 0);
	}

	/** 浮遊（ブスコバリーノ王）。 */
	private void hover(ServerLevel level) {
		if (state == State.DEAD || state == State.CAPTURED || state == State.PITFALL || state == State.DOWN) {
			setNoGravity(false);
			return;
		}
		setNoGravity(true);
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(getX()), Mth.floor(getZ()));
		double want = ground + 1.6 + Math.sin(tickCount * 0.05) * 0.3;
		double vy = Mth.clamp((want - getY()) * 0.15, -0.3, 0.3);
		setDeltaMovement(getDeltaMovement().x, vy, getDeltaMovement().z);
	}

	/** 進行方向の葉・丸太・草を壊して詰まりにくくする（フィールドのみ）。 */
	private void clearPath(ServerLevel level) {
		if (sessionId < 0) return;
		Vec3 v = getDeltaMovement();
		if (v.horizontalDistanceSqr() < 0.002 && !runner.active()) return;
		AABB box = getBoundingBox().inflate(0.6, 0, 0.6).expandTowards(v.x * 4, 0, v.z * 4);
		int count = 0;
		for (BlockPos p : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY + 0.5), Mth.floor(box.minZ),
			Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
			BlockState st = level.getBlockState(p);
			if (st.isAir()) continue;
			if (st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || st.canBeReplaced()) {
				level.destroyBlock(p, false);
				if (++count > 24) break;
			}
		}
	}

	// =====================================================================
	// 被ダメージ（ハンターの攻撃）
	// =====================================================================

	/** 攻撃位置に一番近い部位。 */
	public int partAt(Vec3 point) {
		int best = 0;
		double bestD = Double.MAX_VALUE;
		for (int i = 0; i < kind.parts.size(); i++) {
			Vec3 p = partPos(i);
			double d = p.distanceToSqr(point) - Math.pow(kind.parts.get(i).radius(), 2);
			if (d < bestD) {
				bestD = d;
				best = i;
			}
		}
		return best;
	}

	/** 部位のワールド座標（左右の部位はプレイヤーに近い方）。 */
	public Vec3 partPos(int index) {
		MonsterKind.Part part = kind.parts.get(index);
		return local(part.forward(), part.up(), part.side());
	}

	/** 本体ローカル座標（前・上・横）→ ワールド座標。 */
	public Vec3 local(double forward, double up, double side) {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		double fx = -Math.sin(yaw), fz = Math.cos(yaw);
		double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
		double y = getY() + up + (hasFlag(MonsterFlags.HOVER) ? 0 : 0);
		return new Vec3(getX() + fx * forward + rx * side, y, getZ() + fz * forward + rz * side);
	}

	public DamageCalc.Target targetInfo(int part) {
		return new DamageCalc.Target(kind, kind.parts.get(part), enraged, buskobaTimer > 0,
			state == State.STATUS_SLEEP || state == State.NEST_SLEEP, 1f);
	}

	/**
	 * ハンターの攻撃が当たった。
	 *
	 * @return 実際に与えたダメージ
	 */
	public float receiveHit(ServerLevel level, ServerPlayer attacker, int partIndex, DamageCalc.Result r, HunterState hunter) {
		if (state == State.DEAD || state == State.CAPTURED) return 0;
		float dmg = r.total();
		applyRawDamage(level, dmg, attacker);
		if (attacker != null) {
			damageBy.merge(attacker.getUUID(), dmg, Float::sum);
			aggroTarget = rng.nextInt(4) == 0 ? attacker.getUUID() : aggroTarget == null ? attacker.getUUID() : aggroTarget;
			if (!discovered) {
				notice(level, attacker);
			}
		}
		// 部位ダメージ
		MonsterKind.Part part = kind.parts.get(partIndex);
		partDamage[partIndex] += r.partDamage();
		float breakNeed = part.breakHp() * (float) Math.sqrt(QuestDef.Rank.values()[rank].hpMul);
		if (part.breakable() && (broken & (1 << partIndex)) == 0 && partDamage[partIndex] >= breakNeed) {
			breakPart(level, partIndex);
		}
		// 状態異常
		if (r.statusBuildup() > 0 && hunter != null) {
			int idx = switch (r.element_type()) {
				case POISON -> 0;
				case SLEEP -> 1;
				case PARALYSIS -> 2;
				case BLAST -> 3;
				case BUSKOBA -> 4;
				default -> -1;
			};
			if (idx >= 0) addStatus(level, idx, r.statusBuildup(), attacker);
		}
		// スタン（頭への打撃）
		if (r.ko() > 0 && part.weakSpot()) {
			ko += r.ko();
			if (ko >= koThreshold && state != State.STUN) {
				ko = 0;
				koThreshold *= 1.5f;
				enterTimedState(State.STUN, 200, MonsterAnim.STUN);
				Fx.sound(level, position(), "status.stun", 1.4f, 1f);
				banner(level, "気絶！", 0xFFFF60);
			}
		}
		// 眠っていたら起きる
		if (state == State.STATUS_SLEEP) {
			state = State.COMBAT;
			playAnim(MonsterAnim.FLINCH, 20);
		} else if (state == State.NEST_SLEEP) {
			wake(level);
		}
		// 怯み・怒り
		flinchAccum += dmg;
		enrageAccum += dmg;
		areaMoveAccum += dmg;
		if (flinchAccum > maxHp * 0.075f && !runner.hyperArmor()) {
			flinchAccum = 0;
			if (state == State.COMBAT || state == State.IDLE) {
				enterTimedState(State.FLINCH, 24, MonsterAnim.FLINCH);
				state = State.FLINCH;
				Fx.sound(level, position(), "monster.hurt", 1.6f, 0.9f + rng.nextFloat() * 0.2f);
			}
		}
		if (!enraged && tiredTimer <= 0 && enrageAccum > maxHp * 0.13f && state == State.COMBAT) {
			enrageAccum = 0;
			enraged = true;
			enrageTimer = 20 * 60;
			runner.interrupt();
			runner.start(MonsterMoveSets.roar(kind), attacker);
			banner(level, kind.jaName + "が怒り状態になった！", 0xFF6040);
		}
		if (hpRatio() < 0.3f && !announcedWeak && sessionId >= 0) {
			announcedWeak = true;
			limping = true;
		}
		return dmg;
	}

	/** 固定ダメージ・状態異常ダメージなど、体力を直接減らす。 */
	public void applyRawDamage(ServerLevel level, float dmg, ServerPlayer attacker) {
		if (state == State.DEAD || state == State.CAPTURED) return;
		hp -= dmg;
		if (hp <= 0) {
			hp = 0;
			die(level, attacker);
		}
	}

	private void breakPart(ServerLevel level, int partIndex) {
		broken |= 1 << partIndex;
		MonsterKind.Part part = kind.parts.get(partIndex);
		Fx.sound(level, position(), "monster.part_break", 2.0f, 1f);
		Fx.particle(level, ParticleTypes.EXPLOSION, partPos(partIndex), 3, 0.5, 0);
		QuestSession s = QuestManager.find(level.getServer(), sessionId);
		if (s != null) {
			s.recordPartBreak(kind, part);
			s.broadcastBanner(level.getServer(), new Payloads.Banner(Payloads.Banner.PART_BREAK, "部位破壊！ " + part.jaName(), kind.jaName, 0xFFD040));
		}
		enterTimedState(State.DOWN, 70, MonsterAnim.DOWN);
	}

	private void addStatus(ServerLevel level, int idx, float amount, ServerPlayer attacker) {
		statusBuild[idx] += amount;
		if (statusBuild[idx] < statusThreshold[idx]) return;
		statusBuild[idx] = 0;
		statusThreshold[idx] *= 1.5f;
		switch (idx) {
			case 0 -> {
				poisonTimer = 400;
				Fx.sound(level, position(), "status.poison", 1.5f, 1f);
				banner(level, kind.jaName + "は毒状態になった", 0xC070F0);
			}
			case 1 -> {
				enterTimedState(State.STATUS_SLEEP, 20 * 30, MonsterAnim.SLEEP);
				Fx.sound(level, position(), "status.sleep", 1.5f, 1f);
				banner(level, kind.jaName + "が眠った！", 0x8090FF);
			}
			case 2 -> {
				enterTimedState(State.PARALYZED, 20 * 8, MonsterAnim.TRAPPED);
				Fx.sound(level, position(), "status.paralysis", 1.5f, 1f);
				banner(level, kind.jaName + "が麻痺した！", 0xF0D020);
			}
			case 3 -> {
				Fx.sound(level, position(), "status.blast", 2.5f, 1f);
				Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, position().add(0, kind.height * 0.5, 0), 1, 0, 0);
				applyRawDamage(level, 120f * QuestDef.Rank.values()[rank].attackMul * 1.3f, attacker);
				banner(level, "爆破！", 0xFF9A30);
			}
			case 4 -> {
				int dur = 20 * 30;
				if (attacker != null && HunterManager.state(attacker).has(com.buskohunter.core.ArmorSkill.BUSKOBA_SOUL)) dur = 20 * 45;
				buskobaTimer = dur;
				Fx.sound(level, position(), "status.buskoba", 2.0f, 1f);
				banner(level, kind.jaName + "がブスコバ化した！ 全身が柔らかくなった！", 0xFF70C0);
			}
			default -> {
			}
		}
	}

	private void banner(ServerLevel level, String text, int color) {
		QuestSession s = QuestManager.find(level.getServer(), sessionId);
		if (s != null) s.broadcastBanner(level.getServer(), new Payloads.Banner(Payloads.Banner.INFO, text, "", color));
	}

	// ---- 道具の効果 ----
	public void paint() {
		paintTimer = 20 * 60 * 5;
		discovered = true;
	}

	public void flash(ServerLevel level) {
		blindTimer = 20 * 10;
		if (hasFlag(MonsterFlags.HOVER) || runner.flying()) {
			enterTimedState(State.DOWN, 60, MonsterAnim.DOWN);
		} else {
			enterTimedState(State.FLINCH, 40, MonsterAnim.STUN);
		}
	}

	/** 罠にかかった。 */
	public boolean trap(ServerLevel level, boolean pitfall) {
		if (state == State.DEAD || state == State.CAPTURED || state == State.TRAPPED || state == State.PITFALL) return false;
		if (pitfall && kind == MonsterKind.BARINO) return false;
		if (pitfall) {
			enterTimedState(State.PITFALL, 20 * 10, MonsterAnim.PITFALL);
			Fx.sound(level, position(), "item.pitfall", 1.6f, 1f);
		} else {
			enterTimedState(State.TRAPPED, 20 * 8, MonsterAnim.TRAPPED);
			Fx.sound(level, position(), "item.trap_shock", 1.6f, 1f);
		}
		tranqHits = 0;
		return true;
	}

	public boolean isTrapped() {
		return state == State.TRAPPED || state == State.PITFALL;
	}

	/** 捕獲用麻酔玉が当たった。2 発で捕獲。 */
	public void tranq(ServerLevel level, ServerPlayer by) {
		Fx.particle(level, ParticleTypes.CLOUD, position().add(0, kind.height * 0.6, 0), 20, 1.2, 0.02);
		Fx.sound(level, position(), "item.tranq", 1.4f, 1f);
		if (!isTrapped() || !isCapturable()) return;
		if (++tranqHits >= 2) {
			runner.interrupt();
			state = State.CAPTURED;
			playAnim(MonsterAnim.SLEEP, 40);
			QuestSession s = QuestManager.find(level.getServer(), sessionId);
			if (s != null) s.onMonsterDown(level.getServer(), this, true, by);
		}
	}

	// =====================================================================
	// 討伐・剥ぎ取り
	// =====================================================================
	private void die(ServerLevel level, ServerPlayer killer) {
		runner.interrupt();
		state = State.DEAD;
		deadTicks = 0;
		playAnim(MonsterAnim.DEATH, 40);
		setNoGravity(false);
		Fx.sound(level, position(), "monster.death", 3.0f, 1f);
		QuestSession s = QuestManager.find(level.getServer(), sessionId);
		if (s != null) s.onMonsterDown(level.getServer(), this, false, killer);
	}

	private void carve(ServerPlayer player) {
		int max = 3 + (HunterManager.state(player).meal.skill == com.buskohunter.core.Meal.MealSkill.CARVE_PLUS ? 1 : 0);
		int done = carves.getOrDefault(player.getUUID(), 0);
		if (done >= max) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("もう剥ぎ取れない"));
			return;
		}
		carves.put(player.getUUID(), done + 1);
		HunterManager.startMove(player, com.buskohunter.combat.MoveRegistry.USE_CARVE);
		ServerLevel level = (ServerLevel) level();
		Fx.playerSound(level, position(), "player.carve", 1f, 1f);
		ItemStack drop = new ItemStack(ModItems.TROPHIES.get(kind));
		if (!player.getInventory().add(drop)) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("ポーチがいっぱいです"));
		} else {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("剥ぎ取り " + (done + 1) + "/" + max + " : " + kind.jaName + "の討伐証"));
		}
		QuestSession s = QuestManager.find(level.getServer(), sessionId);
		if (s != null) s.recordCarve(player, kind);
	}

	// =====================================================================
	// 補助
	// =====================================================================
	private boolean playAnimIfChanged(MonsterAnim anim) {
		if (getAnim() != anim) {
			playAnim(anim, anim == MonsterAnim.EAT ? 60 : 20);
		}
		return false;
	}

	public BlockPos areaCenter(int area) {
		if (areaX.length == 0) return blockPosition();
		int a = Mth.clamp(area, 0, areaX.length - 1);
		return new BlockPos(areaX[a], areaY[a], areaZ[a]);
	}

	public void faceTowards(Vec3 target, float maxTurn) {
		double dx = target.x - getX(), dz = target.z - getZ();
		float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
		float cur = getYRot();
		float diff = Mth.wrapDegrees(want - cur);
		float turn = Mth.clamp(diff, -maxTurn, maxTurn);
		float yaw = cur + turn;
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	public ServerPlayer currentTarget(ServerLevel level) {
		if (aggroTarget != null) {
			Player p = level.getPlayerByUUID(aggroTarget);
			if (p instanceof ServerPlayer sp && sp.isAlive() && !sp.isSpectator() && sp.distanceToSqr(this) < 64 * 64) {
				return sp;
			}
		}
		ServerPlayer near = nearestPlayer(level, 48, false);
		aggroTarget = near == null ? null : near.getUUID();
		return near;
	}

	public ServerPlayer nearestPlayer(ServerLevel level, double range, boolean requireSight) {
		ServerPlayer best = null;
		double bestD = range * range;
		for (ServerPlayer p : level.players()) {
			if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
			double d = p.distanceToSqr(this);
			if (d < bestD && (!requireSight || hasLineOfSight(p) || d < 100)) {
				best = p;
				bestD = d;
			}
		}
		return best;
	}

	public List<ServerPlayer> playersWithin(ServerLevel level, double range) {
		List<ServerPlayer> out = new ArrayList<>();
		for (ServerPlayer p : level.players()) {
			if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(this) < range * range) out.add(p);
		}
		return out;
	}

	public void healRatio(float ratio) {
		hp = Math.min(maxHp, hp + maxHp * ratio);
	}

	public void teleportBehind(ServerLevel level, ServerPlayer target) {
		Vec3 look = target.getLookAngle();
		double x = target.getX() - look.x * 5;
		double z = target.getZ() - look.z * 5;
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
		teleportTo(x, y, z);
		faceTowards(target.position(), 180f);
	}

	public void setGlitch(boolean on) {
		setFlag(MonsterFlags.GLITCH, on);
	}

	public void setPosing(boolean on) {
		setFlag(MonsterFlags.POSING, on);
	}

	public void setFlying(boolean on) {
		setFlag(MonsterFlags.FLYING, on);
	}

	public int carvesDone() {
		int sum = 0;
		for (int v : carves.values()) sum += v;
		return sum;
	}

	public int deadTicks() {
		return deadTicks;
	}

	// =====================================================================
	// セーブ
	// =====================================================================
	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putInt("bh_session", sessionId);
		out.putFloat("bh_hp", hp);
		out.putFloat("bh_max_hp", maxHp);
		out.putInt("bh_rank", rank);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		sessionId = in.getIntOr("bh_session", -1);
		maxHp = in.getFloatOr("bh_max_hp", 1000f);
		hp = in.getFloatOr("bh_hp", maxHp);
		rank = in.getIntOr("bh_rank", 0);
		attackMul = QuestDef.Rank.values()[Mth.clamp(rank, 0, 2)].attackMul;
	}

	/** 討伐した時のダメージ貢献（報酬計算用）。 */
	public Map<UUID, Float> damageContribution() {
		return damageBy;
	}

	public ElementType attackElement() {
		return kind.attackElement;
	}

	public void sendDamageNumber(ServerPlayer to, Vec3 at, DamageCalc.Result r) {
		int flags = 0;
		if (r.crit()) flags |= Payloads.DamageNumber.CRIT;
		if (r.weakSpot()) flags |= Payloads.DamageNumber.WEAK_SPOT;
		if (r.bounce()) flags |= Payloads.DamageNumber.BOUNCE;
		if (r.element() > 0) flags |= Payloads.DamageNumber.ELEMENT;
		Net.send(to, new Payloads.DamageNumber(at.x, at.y, at.z, Math.max(1, Math.round(r.total())), flags));
	}
}
