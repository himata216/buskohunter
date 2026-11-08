package com.buskohunter.monster;

import com.buskohunter.core.ArmorSkill;
import com.buskohunter.core.ElementType;
import com.buskohunter.entity.MonsterProjectile;
import com.buskohunter.hunter.HunterCombat;
import com.buskohunter.hunter.HunterManager;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.net.Payloads;
import com.buskohunter.util.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * モンスターの技のタイムラインを実行する（追尾・移動・攻撃判定・飛び道具・特殊技・予兆演出）。
 */
public final class MonsterAttackRunner {
	private final HuntMonster m;
	private MonsterMove move;
	private int tick;
	private UUID target;
	private Vec3 targetPos = Vec3.ZERO;
	private Vec3 leapVel = Vec3.ZERO;
	private Vec3 beamDir = new Vec3(0, 0, 1);
	private final Map<Integer, Set<UUID>> hitOnce = new HashMap<>();
	private final Map<String, Integer> cooldowns = new HashMap<>();

	/** 遅れて発動する攻撃（光柱・間欠泉・噴火・消去の波）と、残り続ける酒の池。 */
	private record Pending(Vec3 pos, int fireAt, float radius, float damage, ElementType element, MonsterMove.Fx fx,
						   MonsterMove.Knock knock, int kind) {
	}

	private final List<Pending> pending = new ArrayList<>();
	private int pendingClock;
	/** POSE YOUR GAME の判定。 */
	private final Map<UUID, Vec3> posePositions = new HashMap<>();
	private int poseJudgeFrom = -1, poseJudgeTo = -1;

	MonsterAttackRunner(HuntMonster m) {
		this.m = m;
	}

	public boolean active() {
		return move != null;
	}

	public boolean hyperArmor() {
		return move != null && (move.anim == MonsterAnim.ROAR || move.special == MonsterMove.Special.POSE_JUDGE);
	}

	public boolean flying() {
		return move != null && move.flying;
	}

	public boolean onCooldown(MonsterMove mv) {
		Integer until = cooldowns.get(mv.id);
		return until != null && m.tickCount < until;
	}

	public void start(MonsterMove mv, ServerPlayer tgt) {
		this.move = mv;
		this.tick = 0;
		this.target = tgt == null ? null : tgt.getUUID();
		this.targetPos = tgt == null ? m.position() : tgt.position();
		this.hitOnce.clear();
		this.leapVel = Vec3.ZERO;
		if (mv.cooldown > 0) cooldowns.put(mv.id, m.tickCount + mv.cooldown);
		m.playAnim(mv.anim, mv.duration);
		m.getNavigation().stop();
		if (mv.flying) m.setFlying(true);
	}

	public void interrupt() {
		if (move != null) {
			finish();
		}
	}

	private void finish() {
		if (move != null && move.flying) {
			m.setFlying(false);
		}
		if (move != null && move.special == MonsterMove.Special.TELEPORT_BEHIND) m.setGlitch(false);
		if (move != null && move.special == MonsterMove.Special.POSE_JUDGE) m.setPosing(false);
		m.setNoGravity(m.kind == com.buskohunter.core.MonsterKind.BARINO);
		move = null;
		poseJudgeFrom = poseJudgeTo = -1;
	}

	private ServerPlayer target(ServerLevel level) {
		if (target == null) return null;
		var p = level.getPlayerByUUID(target);
		return p instanceof ServerPlayer sp && sp.isAlive() ? sp : null;
	}

	// =====================================================================
	public void tick(ServerLevel level) {
		MonsterMove mv = move;
		if (mv == null) return;
		ServerPlayer tgt = target(level);
		if (tgt != null && tick <= mv.trackUntil) targetPos = tgt.position();

		// 追尾（尻ビームは背中を向ける）
		if (tick <= mv.trackUntil) {
			boolean rear = mv.anim == MonsterAnim.BUTT_BEAM || mv.anim == MonsterAnim.HIP_ATTACK;
			Vec3 face = targetPos;
			if (rear) {
				face = m.position().subtract(targetPos.subtract(m.position()));
			}
			m.faceTowards(face, m.isEnraged() ? 14f : 10f);
		}

		// 移動
		for (MonsterMove.Motion mo : mv.motions) {
			if (tick < mo.from() || tick > mo.to()) continue;
			int len = Math.max(1, mo.to() - mo.from());
			if (mo.towardTarget()) {
				if (tick == mo.from()) {
					Vec3 d = targetPos.subtract(m.position());
					double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
					double speed = mo.forward() > 0 ? mo.forward() : Math.min(1.2, horiz / len);
					Vec3 dir = horiz < 0.01 ? Vec3.ZERO : new Vec3(d.x / horiz, 0, d.z / horiz);
					leapVel = new Vec3(dir.x * speed, mo.up(), dir.z * speed);
					m.setDeltaMovement(leapVel);
					if (mv.flying) m.setNoGravity(mo.up() <= 0);
				} else if (mo.forward() > 0 && mv.flying) {
					m.setDeltaMovement(leapVel.x, mo.up(), leapVel.z);
				} else {
					m.setDeltaMovement(leapVel.x, m.getDeltaMovement().y, leapVel.z);
				}
			} else if (mo.forward() != 0) {
				float yaw = m.getYRot() * Mth.DEG_TO_RAD;
				m.setDeltaMovement(-Math.sin(yaw) * mo.forward(), m.getDeltaMovement().y, Math.cos(yaw) * mo.forward());
			} else if (mo.up() != 0) {
				m.setNoGravity(true);
				m.setDeltaMovement(m.getDeltaMovement().x * 0.8, mo.up(), m.getDeltaMovement().z * 0.8);
			}
		}
		if (mv.motions.isEmpty() && m.kind != com.buskohunter.core.MonsterKind.BARINO) {
			m.setDeltaMovement(0, m.getDeltaMovement().y, 0);
		}

		// 予兆（最初の攻撃判定まで）
		int firstHit = firstHitTick(mv);
		if (mv.telegraph != MonsterMove.Fx.NONE && tick < firstHit && tick % 3 == 0) {
			telegraph(level, mv);
		}

		// 効果音
		if (mv.sound != null && tick == mv.soundTick) {
			Fx.sound(level, m.position(), mv.sound, mv.anim == MonsterAnim.ROAR ? 4f : 2.5f, 0.92f + m.rng().nextFloat() * 0.16f);
		}

		// 攻撃判定
		for (int i = 0; i < mv.hits.size(); i++) {
			MonsterMove.Hit h = mv.hits.get(i);
			if (tick < h.from() || tick > h.to()) continue;
			boolean doHit = h.interval() <= 0 || (tick - h.from()) % h.interval() == 0;
			if (h.interval() > 0 && doHit) hitOnce.remove(i);
			doHitbox(level, i, h);
		}

		// 飛び道具
		for (MonsterMove.Projectile pr : mv.projectiles) {
			if (tick == pr.tick()) fireProjectiles(level, pr, tgt);
		}

		// 特殊
		if (mv.special != MonsterMove.Special.NONE && tick == mv.specialTick) {
			special(level, mv, tgt);
		}
		if (poseJudgeFrom >= 0 && tick >= poseJudgeFrom && tick <= poseJudgeTo) {
			poseJudge(level);
		}

		// 足音
		if ((mv.anim == MonsterAnim.CHARGE || mv.anim == MonsterAnim.RUN) && tick % 6 == 0) {
			Fx.sound(level, m.position(), "monster.step", 1.4f, 1f);
			Fx.burst(level, MonsterMove.Fx.DUST, m.position(), 6, 1.0);
		}

		tick++;
		if (tick >= mv.duration) {
			finish();
		}
	}

	private static int firstHitTick(MonsterMove mv) {
		int first = mv.duration;
		for (MonsterMove.Hit h : mv.hits) first = Math.min(first, h.from());
		for (MonsterMove.Projectile p : mv.projectiles) first = Math.min(first, p.tick());
		return first;
	}

	// =====================================================================
	// 攻撃判定
	// =====================================================================
	private void doHitbox(ServerLevel level, int index, MonsterMove.Hit h) {
		float yaw = m.getYRot() * Mth.DEG_TO_RAD;
		Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Set<UUID> already = hitOnce.computeIfAbsent(index, k -> new HashSet<>());
		List<ServerPlayer> players = m.playersWithin(level, 40);
		switch (h.shape()) {
			case SPHERE -> {
				Vec3 c = m.local(h.forward(), h.up(), h.side());
				Fx.burst(level, h.fx(), c, 3, h.radius() * 0.4);
				for (ServerPlayer p : players) {
					if (already.contains(p.getUUID())) continue;
					if (aabbSphere(p.getBoundingBox(), c, h.radius())) {
						already.add(p.getUUID());
						hit(level, p, h, c);
					}
				}
			}
			case CIRCLE -> {
				Vec3 c = m.position();
				if (h.from() == tick) {
					Fx.ring(level, ParticleTypes.CLOUD, c, h.radius(), 24);
					Fx.burst(level, h.fx(), c.add(0, 0.5, 0), 12, h.radius() * 0.5);
				}
				for (ServerPlayer p : players) {
					if (already.contains(p.getUUID())) continue;
					double dx = p.getX() - c.x, dz = p.getZ() - c.z;
					double dy = p.getY() - c.y;
					if (dx * dx + dz * dz <= h.radius() * h.radius() + 1 && dy > -2 && dy < m.kind.height + 1) {
						already.add(p.getUUID());
						hit(level, p, h, c);
					}
				}
			}
			case SHOCKWAVE -> {
				int span = Math.max(1, h.to() - h.from());
				double cur = h.radius() * (tick - h.from() + 1) / (double) span;
				Vec3 c = m.position();
				Fx.ring(level, ParticleTypes.CLOUD, c, cur, (int) (8 + cur * 6));
				for (ServerPlayer p : players) {
					if (already.contains(p.getUUID())) continue;
					double d = Math.sqrt(Math.pow(p.getX() - c.x, 2) + Math.pow(p.getZ() - c.z, 2));
					boolean grounded = p.getY() - c.y < 1.2;
					if (d <= cur + 0.6 && d >= cur - 1.8 && grounded) {
						already.add(p.getUUID());
						hit(level, p, h, c);
					}
				}
			}
			case BEAM -> {
				Vec3 origin = m.local(h.forward(), h.up(), h.side());
				boolean rear = h.radius() < 0;
				double len = Math.abs(h.radius());
				if (tick == h.from()) {
					Vec3 aim = targetPos.add(0, 1.0, 0).subtract(origin);
					Vec3 flat = rear ? fwd.scale(-1) : fwd;
					double horiz = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
					double pitch = Mth.clamp(aim.y / Math.max(1, horiz), -0.5, 0.35);
					beamDir = new Vec3(flat.x, pitch, flat.z).normalize();
				}
				Vec3 end = origin.add(beamDir.scale(len));
				Fx.line(level, Fx.beamParticle(h.fx()), origin, end, 0.6);
				for (ServerPlayer p : players) {
					if (already.contains(p.getUUID())) continue;
					if (segmentHitsBox(origin, end, p.getBoundingBox().inflate(h.width() * 0.5))) {
						already.add(p.getUUID());
						hit(level, p, h, origin);
					}
				}
			}
			case CONE -> {
				Vec3 origin = m.local(h.forward(), h.up(), 0);
				double cos = Math.cos(Math.toRadians(h.width()));
				for (int i = 0; i < 6; i++) {
					double a = (m.rng().nextDouble() - 0.5) * Math.toRadians(h.width()) * 2;
					Vec3 dir = new Vec3(fwd.x * Math.cos(a) - fwd.z * Math.sin(a), -0.08, fwd.x * Math.sin(a) + fwd.z * Math.cos(a));
					Fx.burst(level, h.fx(), origin.add(dir.scale(2 + m.rng().nextDouble() * h.radius())), 2, 0.4);
				}
				for (ServerPlayer p : players) {
					if (already.contains(p.getUUID())) continue;
					Vec3 to = p.position().add(0, 1, 0).subtract(origin);
					double d = to.length();
					if (d > h.radius()) continue;
					Vec3 toFlat = new Vec3(to.x, 0, to.z).normalize();
					if (toFlat.x * fwd.x + toFlat.z * fwd.z >= cos) {
						already.add(p.getUUID());
						hit(level, p, h, origin);
					}
				}
			}
		}
	}

	private void hit(ServerLevel level, ServerPlayer p, MonsterMove.Hit h, Vec3 from) {
		float dmg = h.damage() * m.attackMul();
		HunterCombat.monsterHitsPlayer(level, m, p, dmg, h.element(), h.knock(), from);
	}

	private static boolean aabbSphere(AABB box, Vec3 c, double r) {
		double x = Mth.clamp(c.x, box.minX, box.maxX);
		double y = Mth.clamp(c.y, box.minY, box.maxY);
		double z = Mth.clamp(c.z, box.minZ, box.maxZ);
		return c.distanceToSqr(x, y, z) <= r * r;
	}

	private static boolean segmentHitsBox(Vec3 a, Vec3 b, AABB box) {
		if (box.contains(a)) return true;
		return box.clip(a, b).isPresent();
	}

	// =====================================================================
	// 飛び道具・特殊技
	// =====================================================================
	private void fireProjectiles(ServerLevel level, MonsterMove.Projectile pr, ServerPlayer tgt) {
		Vec3 origin = m.local(1.5, m.kind.height * 0.65, 0);
		for (int i = 0; i < pr.count(); i++) {
			Vec3 aim = (tgt != null ? tgt.position().add(0, 0.8, 0) : targetPos).subtract(origin);
			double spread = pr.count() > 1 ? Math.toRadians(pr.spread()) * ((double) i / (pr.count() - 1) - 0.5) * 2 : 0;
			double ax = aim.x * Math.cos(spread) - aim.z * Math.sin(spread);
			double az = aim.x * Math.sin(spread) + aim.z * Math.cos(spread);
			Vec3 dir = new Vec3(ax, aim.y, az);
			if (pr.kind().gravity > 0) {
				// 放物線：距離に応じて上向きに
				double horiz = Math.sqrt(ax * ax + az * az);
				dir = new Vec3(ax, aim.y + horiz * 0.35, az);
			}
			MonsterProjectile proj = MonsterProjectile.create(level, m, pr.kind(), pr.damage() * m.attackMul(), tgt);
			proj.setPos(origin.x, origin.y, origin.z);
			proj.shoot(dir.x, dir.y, dir.z, pr.speed(), 0f);
			level.addFreshEntity(proj);
		}
	}

	private void special(ServerLevel level, MonsterMove mv, ServerPlayer tgt) {
		switch (mv.special) {
			case ROAR -> {
				for (ServerPlayer p : m.playersWithin(level, 26)) {
					HunterState s = HunterManager.state(p);
					if (s.has(ArmorSkill.EARPLUGS) || s.hornEarplug > 0) continue;
					HunterManager.forceMove(p, com.buskohunter.combat.MoveRegistry.ROAR_STUN);
				}
			}
			case PILLARS -> {
				ElementType el = m.kind == com.buskohunter.core.MonsterKind.BUSAIBER ? ElementType.THUNDER : ElementType.DRAGON;
				MonsterMove.Fx fx = m.kind == com.buskohunter.core.MonsterKind.BUSAIBER ? MonsterMove.Fx.THUNDER : MonsterMove.Fx.HOLY;
				for (ServerPlayer p : m.playersWithin(level, 44)) {
					for (int i = 0; i < 2; i++) {
						Vec3 pos = p.position().add((m.rng().nextDouble() - 0.5) * 6 * i, 0, (m.rng().nextDouble() - 0.5) * 6 * i);
						addPending(level, pos, 26 + i * 8, 2.3f, 30 * m.attackMul(), el, fx, MonsterMove.Knock.LARGE, 0);
					}
				}
			}
			case GEYSERS -> {
				for (ServerPlayer p : m.playersWithin(level, 36)) {
					addPending(level, p.position(), 24, 2.2f, 26 * m.attackMul(), ElementType.WATER, MonsterMove.Fx.WATER,
						MonsterMove.Knock.LAUNCH, 0);
				}
				for (int i = 0; i < 4; i++) {
					Vec3 pos = m.position().add((m.rng().nextDouble() - 0.5) * 20, 0, (m.rng().nextDouble() - 0.5) * 20);
					addPending(level, pos, 30 + i * 5, 2.2f, 26 * m.attackMul(), ElementType.WATER, MonsterMove.Fx.WATER,
						MonsterMove.Knock.LAUNCH, 0);
				}
			}
			case ERUPTIONS -> {
				for (int i = 0; i < 7; i++) {
					double a = m.rng().nextDouble() * Math.PI * 2;
					double r = 4 + m.rng().nextDouble() * 9;
					Vec3 pos = m.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
					addPending(level, pos, 20 + i * 4, 2.4f, 28 * m.attackMul(), ElementType.FIRE, MonsterMove.Fx.FIRE,
						MonsterMove.Knock.LAUNCH, 0);
				}
				for (ServerPlayer p : m.playersWithin(level, 30)) {
					addPending(level, p.position(), 28, 2.4f, 28 * m.attackMul(), ElementType.FIRE, MonsterMove.Fx.FIRE,
						MonsterMove.Knock.LAUNCH, 0);
				}
			}
			case ERASE_WAVE -> addPending(level, m.position(), 0, 22f, 42 * m.attackMul(), ElementType.DRAGON, MonsterMove.Fx.HOLY,
				MonsterMove.Knock.LARGE, 2);
			case POSE_JUDGE -> {
				m.setPosing(true);
				Payloads.Banner b = new Payloads.Banner(Payloads.Banner.POSE, "POSE YOUR GAME!", "ポーズを決めろ！ 動いたら撃たれる！", 0xFF40C0);
				for (ServerPlayer p : m.playersWithin(level, 50)) {
					com.buskohunter.net.Net.send(p, b);
				}
				posePositions.clear();
				poseJudgeFrom = mv.specialTick + 30;
				poseJudgeTo = poseJudgeFrom + 50;
			}
			case TELEPORT_BEHIND -> {
				if (tgt != null) {
					m.setGlitch(true);
					Fx.burst(level, MonsterMove.Fx.GLITCH, m.position().add(0, 2, 0), 30, 1.6);
					m.teleportBehind(level, tgt);
					Fx.burst(level, MonsterMove.Fx.GLITCH, m.position().add(0, 2, 0), 30, 1.6);
				}
			}
			case TERRAIN_BREAK -> terrainBreak(level, 6);
			case SAKE_POOLS -> {
				for (int i = 0; i < 4; i++) {
					double a = m.rng().nextDouble() * Math.PI * 2;
					double r = 3 + m.rng().nextDouble() * 9;
					Vec3 pos = m.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
					addPending(level, pos, 0, 2.5f, 0, ElementType.NONE, MonsterMove.Fx.SAKE, MonsterMove.Knock.SMALL, 3);
				}
			}
			case HEAL_SMILE -> {
				m.healRatio(0.03f);
				Fx.particle(level, ParticleTypes.HEART, m.position().add(0, m.kind.height, 0), 12, 1.5, 0.1);
			}
			default -> {
			}
		}
	}

	/** POSE YOUR GAME：判定中に動いたプレイヤーを撃つ。 */
	private void poseJudge(ServerLevel level) {
		List<ServerPlayer> players = m.playersWithin(level, 50);
		if (tick == poseJudgeFrom) {
			for (ServerPlayer p : players) posePositions.put(p.getUUID(), p.position());
			Fx.sound(level, m.position(), "monster.glitch", 3f, 1.4f);
			return;
		}
		for (ServerPlayer p : players) {
			Vec3 at = posePositions.get(p.getUUID());
			if (at == null) continue;
			boolean moved = p.position().distanceToSqr(at) > 0.3 * 0.3 || HunterManager.state(p).inMove();
			if (moved) {
				posePositions.remove(p.getUUID());
				Vec3 eye = m.local(0.8, m.kind.height - 0.4, 0);
				Fx.line(level, new DustParticleOptions(0xFF40C0, 2.4f), eye, p.position().add(0, 1, 0), 0.5);
				Fx.sound(level, p.position(), "monster.beam", 2f, 1.3f);
				HunterCombat.monsterHitsPlayer(level, m, p, 34 * m.attackMul(), ElementType.THUNDER, MonsterMove.Knock.LARGE, eye);
				HunterManager.state(p).paralysis = Math.max(HunterManager.state(p).paralysis, 40);
			}
		}
		if (tick == poseJudgeTo) {
			for (ServerPlayer p : players) {
				if (posePositions.containsKey(p.getUUID())) {
					HunterManager.state(p).hornAttack = Math.max(HunterManager.state(p).hornAttack, 20 * 30);
					com.buskohunter.net.Net.send(p,
						new Payloads.Banner(Payloads.Banner.INFO, "PERFECT POSE!! 攻撃力UP", "", 0x40F0FF));
				}
			}
			posePositions.clear();
			m.setPosing(false);
		}
	}

	private void terrainBreak(ServerLevel level, int radius) {
		if (m.sessionId() < 0) return;
		BlockPos c = m.blockPosition();
		int count = 0;
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-radius, 0, -radius), c.offset(radius, radius, radius))) {
			if (p.distSqr(c) > radius * radius) continue;
			BlockState st = level.getBlockState(p);
			if (st.isAir() || st.is(Blocks.BEDROCK)) continue;
			if (st.getBlock() instanceof com.buskohunter.block.HunterStationBlock) continue;
			if (st.getDestroySpeed(level, p) < 0) continue;
			level.destroyBlock(p, false);
			if (++count > 220) break;
		}
		Fx.particle(level, ParticleTypes.EXPLOSION, m.position().add(0, 1, 0), 6, radius * 0.5, 0);
		Fx.sound(level, m.position(), "monster.slam", 4f, 0.7f);
	}

	private void addPending(ServerLevel level, Vec3 pos, int delay, float radius, float dmg, ElementType el, MonsterMove.Fx fx,
							MonsterMove.Knock knock, int kind) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(pos.x), Mth.floor(pos.z));
		Vec3 ground = new Vec3(pos.x, Math.min(pos.y, y), pos.z);
		if (Math.abs(pos.y - y) < 6) ground = new Vec3(pos.x, y, pos.z);
		pending.add(new Pending(ground, pendingClock + delay, radius, dmg, el, fx, knock, kind));
	}

	/** 遅延攻撃・酒の池の処理（技が終わっても続く）。 */
	public void tickPending(ServerLevel level) {
		pendingClock++;
		if (pending.isEmpty()) return;
		Iterator<Pending> it = pending.iterator();
		List<Pending> respawn = new ArrayList<>();
		while (it.hasNext()) {
			Pending p = it.next();
			int left = p.fireAt - pendingClock;
			switch (p.kind) {
				case 0 -> {
					// 予兆→炸裂
					if (left > 0) {
						if (left % 3 == 0) Fx.ring(level, Fx.telegraphParticle(p.fx), p.pos, p.radius, 14);
						continue;
					}
					Fx.burst(level, p.fx, p.pos.add(0, 0.5, 0), 20, 0.8);
					Fx.burst(level, p.fx, p.pos.add(0, 2.5, 0), 20, 0.6);
					Fx.burst(level, p.fx, p.pos.add(0, 4.5, 0), 12, 0.4);
					Fx.sound(level, p.pos, switch (p.fx) {
						case FIRE -> "monster.fire";
						case WATER -> "monster.water";
						case THUNDER -> "monster.electric";
						default -> "monster.holy";
					}, 2f, 1f);
					for (ServerPlayer pl : m.playersWithin(level, 60)) {
						double dx = pl.getX() - p.pos.x, dz = pl.getZ() - p.pos.z;
						if (dx * dx + dz * dz <= p.radius * p.radius && Math.abs(pl.getY() - p.pos.y) < 4) {
							HunterCombat.monsterHitsPlayer(level, m, pl, p.damage, p.element, p.knock, p.pos);
						}
					}
					it.remove();
				}
				case 2 -> {
					// 消去の波：予兆 20tick → 半径 0→最大 で広がる
					int age = pendingClock - p.fireAt;
					if (age < 0) continue;
					if (age < 20) {
						if (age % 2 == 0) Fx.ring(level, Fx.telegraphParticle(MonsterMove.Fx.HOLY), p.pos, p.radius, 60);
						continue;
					}
					double cur = p.radius * (age - 20) / 16.0;
					Fx.ring(level, new DustParticleOptions(0xFFF0B0, 2.5f), p.pos, cur, (int) (12 + cur * 5));
					for (ServerPlayer pl : m.playersWithin(level, 60)) {
						double d = Math.sqrt(Math.pow(pl.getX() - p.pos.x, 2) + Math.pow(pl.getZ() - p.pos.z, 2));
						if (Math.abs(d - cur) < 1.2 && pl.getY() - p.pos.y < 1.0 && pl.onGround()) {
							HunterCombat.monsterHitsPlayer(level, m, pl, p.damage, p.element, p.knock, p.pos);
						}
					}
					if (age > 36) it.remove();
				}
				case 3 -> {
					// 酒の池（12 秒）：中にいると酔う
					int age = pendingClock - p.fireAt;
					if (age % 8 == 0) Fx.ring(level, new DustParticleOptions(0xF8F0D8, 1.5f), p.pos, p.radius, 16);
					if (age % 10 == 0) {
						for (ServerPlayer pl : m.playersWithin(level, 40)) {
							if (pl.position().distanceToSqr(p.pos) <= p.radius * p.radius + 1) {
								HunterState s = HunterManager.state(pl);
								if (!s.has(ArmorSkill.DRUNK_IMMUNE)) s.drunk = Math.max(s.drunk, 100);
							}
						}
					}
					if (age > 240) it.remove();
				}
				default -> it.remove();
			}
		}
		pending.addAll(respawn);
	}

	private void telegraph(ServerLevel level, MonsterMove mv) {
		for (MonsterMove.Hit h : mv.hits) {
			switch (h.shape()) {
				case BEAM -> {
					Vec3 origin = m.local(h.forward(), h.up(), h.side());
					float yaw = m.getYRot() * Mth.DEG_TO_RAD;
					Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
					if (h.radius() < 0) fwd = fwd.scale(-1);
					Fx.line(level, Fx.telegraphParticle(mv.telegraph), origin, origin.add(fwd.scale(Math.abs(h.radius()))), 1.5);
					Fx.burst(level, mv.telegraph, origin, 4, 0.3);
				}
				case CONE -> Fx.burst(level, mv.telegraph, m.local(h.forward(), h.up(), 0), 6, 0.5);
				default -> {
				}
			}
		}
		if (mv.special != MonsterMove.Special.NONE) {
			Fx.burst(level, mv.telegraph, m.position().add(0, m.kind.height * 0.7, 0), 8, m.kind.width * 0.4);
		}
	}
}
