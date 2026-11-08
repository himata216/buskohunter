package com.buskohunter;

import com.buskohunter.core.FieldType;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.QuestDef;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.quest.Dims;
import com.buskohunter.quest.FieldLayout;
import com.buskohunter.quest.WorldBuilder;
import com.buskohunter.registry.ModEntities;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 開発用の自己診断（-Dbuskohunter.selftest=true のときだけ動く。普段のプレイには影響しない）。
 * 集会所・全フィールドの生成と、全モンスターの出現・行動・討伐をサーバーだけで確かめ、結果をログに出して終了する。
 */
public final class SelfTest {
	private SelfTest() {
	}

	private static final boolean ENABLED = Boolean.getBoolean("buskohunter.selftest");
	private static final List<HuntMonster> MONSTERS = new ArrayList<>();
	private static int ticks = -1;
	private static int failures;

	public static void init() {
		if (!ENABLED) return;
		ServerStartedEvent.BUS.addListener(e -> start(e.getServer()));
		TickEvent.ServerTickEvent.Post.BUS.addListener(e -> tick(e.server()));
	}

	private static void log(String msg) {
		BuskoHunter.LOGGER.info("[SELFTEST] {}", msg);
	}

	private static void fail(String what, Throwable t) {
		failures++;
		BuskoHunter.LOGGER.error("[SELFTEST] FAIL {}", what, t);
	}

	private static void start(MinecraftServer server) {
		log("start");
		try {
			ServerLevel hub = Dims.hub(server);
			long t0 = System.currentTimeMillis();
			WorldBuilder.ensureHub(hub);
			log("hub built in " + (System.currentTimeMillis() - t0) + " ms, spawn " + WorldBuilder.hubSpawn(hub));
		} catch (Throwable t) {
			fail("hub", t);
		}
		for (FieldType f : FieldType.values()) {
			try {
				ServerLevel level = server.getLevel(Dims.field(f));
				if (level == null) {
					fail("field level missing: " + f, new IllegalStateException());
					continue;
				}
				long t0 = System.currentTimeMillis();
				FieldLayout layout = FieldLayout.create(level, f, 0);
				WorldBuilder.prepareField(level, f, layout);
				log("field " + f + " ready in " + (System.currentTimeMillis() - t0) + " ms, camp " + layout.campSpawn());
				// その場所にモンスターを 1 体
				MonsterKind kind = MonsterKind.values()[f.ordinal() % MonsterKind.values().length];
				Vec3 at = layout.areaPos(0);
				level.setChunkForced(((int) Math.floor(at.x)) >> 4, ((int) Math.floor(at.z)) >> 4, true);
				HuntMonster m = ModEntities.MONSTERS.get(kind).create(level, EntitySpawnReason.COMMAND);
				if (m == null) {
					fail("create monster " + kind, new IllegalStateException());
					continue;
				}
				m.setPos(at.x, at.y + 1, at.z);
				m.setupStandalone(QuestDef.Rank.HIGH);
				level.addFreshEntity(m);
				MONSTERS.add(m);
				log("spawned " + kind + " hp " + m.hp());
			} catch (Throwable t) {
				fail("field " + f, t);
			}
		}
		ticks = 0;
	}

	private static void tick(MinecraftServer server) {
		if (ticks < 0) return;
		ticks++;
		try {
			if (ticks == 200) {
				for (HuntMonster m : MONSTERS) {
					log(m.getType().toShortString() + " state " + m.state() + " pos " + m.blockPosition() + " alive " + m.isAlive());
				}
			}
			if (ticks == 220) {
				// 討伐の流れ（死亡・剥ぎ取り可能）を確かめる
				for (HuntMonster m : MONSTERS) {
					m.applyRawDamage((ServerLevel) m.level(), m.hp() + 10, null);
				}
			}
			if (ticks == 320) {
				for (HuntMonster m : MONSTERS) {
					log(m.getType().toShortString() + " after kill: state " + m.state() + " carvable " + m.isCarvable());
				}
				log(failures == 0 ? "RESULT OK" : "RESULT FAILURES " + failures);
				ticks = -1;
				server.halt(false);
			}
		} catch (Throwable t) {
			fail("tick " + ticks, t);
			log("RESULT FAILURES " + failures);
			ticks = -1;
			server.halt(false);
		}
	}
}
