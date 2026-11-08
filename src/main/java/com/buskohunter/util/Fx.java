package com.buskohunter.util;

import com.buskohunter.monster.MonsterMove;
import com.buskohunter.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** サーバー側の演出（効果音・パーティクル）ヘルパー。 */
public final class Fx {
	private Fx() {
	}

	public static void sound(ServerLevel level, Vec3 pos, String key, float volume, float pitch) {
		SoundEvent ev = ModSounds.get(key);
		if (ev == null) return;
		level.playSound(null, pos.x, pos.y, pos.z, ev, SoundSource.HOSTILE, volume, pitch);
	}

	public static void playerSound(ServerLevel level, Vec3 pos, String key, float volume, float pitch) {
		SoundEvent ev = ModSounds.get(key);
		if (ev == null) return;
		level.playSound(null, pos.x, pos.y, pos.z, ev, SoundSource.PLAYERS, volume, pitch);
	}

	public static void particle(ServerLevel level, ParticleOptions p, Vec3 pos, int count, double spread, double speed) {
		level.sendParticles(p, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	public static void particle(ServerLevel level, ParticleOptions p, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		level.sendParticles(p, x, y, z, count, dx, dy, dz, speed);
	}

	/** 演出種別ごとのパーティクル。 */
	public static void burst(ServerLevel level, MonsterMove.Fx fx, Vec3 pos, int count, double spread) {
		switch (fx) {
			case FIRE -> {
				particle(level, ParticleTypes.FLAME, pos, count, spread, 0.05);
				particle(level, ParticleTypes.LARGE_SMOKE, pos, Math.max(1, count / 3), spread, 0.02);
			}
			case WATER -> {
				particle(level, ParticleTypes.SPLASH, pos, count * 2, spread, 0.2);
				particle(level, ParticleTypes.BUBBLE, pos, count, spread, 0.05);
			}
			case BUBBLE -> particle(level, ParticleTypes.BUBBLE_POP, pos, count, spread, 0.02);
			case THUNDER -> particle(level, ParticleTypes.ELECTRIC_SPARK, pos, count * 2, spread, 0.3);
			case ICE -> particle(level, ParticleTypes.SNOWFLAKE, pos, count, spread, 0.05);
			case HOLY -> {
				particle(level, ParticleTypes.END_ROD, pos, count, spread, 0.05);
				particle(level, new DustParticleOptions(0xFFE890, 2.0f), pos, count, spread, 0.0);
			}
			case GLITCH -> {
				particle(level, new DustParticleOptions(0xFF30C0, 1.6f), pos, count, spread, 0.0);
				particle(level, new DustParticleOptions(0x30F0FF, 1.6f), pos, count, spread, 0.0);
			}
			case NEON -> particle(level, new DustParticleOptions(0x40F0FF, 1.8f), pos, count, spread, 0.0);
			case SAKE -> particle(level, new DustParticleOptions(0xF8F0D8, 1.5f), pos, count, spread, 0.0);
			case SHOCK -> particle(level, ParticleTypes.CLOUD, pos, count, spread, 0.15);
			case DRAGON -> particle(level, new DustParticleOptions(0xB01030, 2.0f), pos, count, spread, 0.0);
			case DUST -> particle(level, ParticleTypes.POOF, pos, count, spread, 0.05);
			default -> {
			}
		}
	}

	/** 地面のリング（予兆・衝撃波の可視化）。 */
	public static void ring(ServerLevel level, ParticleOptions p, Vec3 center, double radius, int points) {
		for (int i = 0; i < points; i++) {
			double a = Math.PI * 2 * i / points;
			level.sendParticles(p, center.x + Math.cos(a) * radius, center.y + 0.15, center.z + Math.sin(a) * radius,
				1, 0, 0, 0, 0);
		}
	}

	/** 直線（ビーム）。 */
	public static void line(ServerLevel level, ParticleOptions p, Vec3 from, Vec3 to, double step) {
		Vec3 d = to.subtract(from);
		double len = d.length();
		if (len < 1e-3) return;
		Vec3 u = d.scale(1.0 / len);
		for (double t = 0; t <= len; t += step) {
			Vec3 q = from.add(u.scale(t));
			level.sendParticles(p, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
		}
	}

	public static ParticleOptions telegraphParticle(MonsterMove.Fx fx) {
		return switch (fx) {
			case FIRE -> ParticleTypes.FLAME;
			case WATER, BUBBLE -> ParticleTypes.BUBBLE_POP;
			case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
			case HOLY -> ParticleTypes.END_ROD;
			case NEON, GLITCH -> new DustParticleOptions(0x40F0FF, 1.4f);
			case SAKE -> new DustParticleOptions(0xF8F0D8, 1.4f);
			default -> ParticleTypes.CRIT;
		};
	}

	public static ParticleOptions beamParticle(MonsterMove.Fx fx) {
		return switch (fx) {
			case FIRE -> ParticleTypes.FLAME;
			case WATER -> ParticleTypes.SPLASH;
			case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
			case HOLY -> new DustParticleOptions(0xFFF0A0, 2.2f);
			case NEON -> new DustParticleOptions(0xFF50E0, 2.0f);
			case GLITCH -> new DustParticleOptions(0x30F0FF, 2.0f);
			default -> new DustParticleOptions(0xFFFFFF, 2.0f);
		};
	}
}
