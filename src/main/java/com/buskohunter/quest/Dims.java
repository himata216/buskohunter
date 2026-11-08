package com.buskohunter.quest;

import com.buskohunter.BuskoHunter;
import com.buskohunter.core.FieldType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** 集会所・フィールドのディメンションキー（data/buskohunter/dimension/*.json）。 */
public final class Dims {
	private Dims() {
	}

	public static final ResourceKey<Level> HUB = ResourceKey.create(Registries.DIMENSION, BuskoHunter.id("hub"));

	public static ResourceKey<Level> field(FieldType f) {
		return ResourceKey.create(Registries.DIMENSION, BuskoHunter.id(f.dimensionPath));
	}

	public static ServerLevel hub(MinecraftServer server) {
		ServerLevel l = server.getLevel(HUB);
		return l != null ? l : server.overworld();
	}

	public static boolean isHub(Level level) {
		return level.dimension().equals(HUB);
	}

	public static boolean isField(Level level) {
		for (FieldType f : FieldType.values()) {
			if (level.dimension().equals(field(f))) return true;
		}
		return false;
	}

	public static boolean isOurs(Level level) {
		return isHub(level) || isField(level);
	}
}
