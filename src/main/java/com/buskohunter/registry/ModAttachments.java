package com.buskohunter.registry;

import com.buskohunter.BuskoHunter;
import com.buskohunter.hunter.HunterProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * プレイヤーに保存するハンターデータ。
 * Forge の永続データ（"PlayerPersisted" の下はリスポーン時にもコピーされる）に保存する。
 */
public final class ModAttachments {
	private ModAttachments() {
	}

	private static final String PERSISTED = "PlayerPersisted";
	private static final String KEY = BuskoHunter.MOD_ID + "_profile";
	/** 毎回 NBT から読み直さないためのキャッシュ（プレイヤーのオブジェクトごと）。 */
	private static final Map<Player, HunterProfile> CACHE = new WeakHashMap<>();

	public static HunterProfile profile(Player player) {
		HunterProfile cached = CACHE.get(player);
		if (cached != null) return cached;
		HunterProfile p = HunterProfile.DEFAULT;
		CompoundTag persisted = player.getPersistentData().getCompoundOrEmpty(PERSISTED);
		Tag tag = persisted.get(KEY);
		if (tag != null) {
			p = HunterProfile.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(HunterProfile.DEFAULT);
		}
		CACHE.put(player, p);
		return p;
	}

	public static void set(Player player, HunterProfile profile) {
		CACHE.put(player, profile);
		CompoundTag data = player.getPersistentData();
		CompoundTag persisted = data.getCompoundOrEmpty(PERSISTED);
		HunterProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).result().ifPresent(t -> persisted.put(KEY, t));
		data.put(PERSISTED, persisted);
	}
}
