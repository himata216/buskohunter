package com.buskohunter.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Forge の RegisterEvent の中でだけ使う登録ヘルパー。
 * BuskoHunter が各レジストリのイベントで current を設定してから各 Mod*.register*() を呼ぶ。
 */
public final class Reg {
	private Reg() {
	}

	static RegisterEvent current;

	public static void begin(RegisterEvent event) {
		current = event;
	}

	public static void end() {
		current = null;
	}

	public static <T, V extends T> V register(ResourceKey<? extends Registry<T>> registry, Identifier id, V value) {
		if (current == null) throw new IllegalStateException("Reg.register は RegisterEvent の中でだけ呼べます: " + id);
		current.register(registry, id, () -> value);
		return value;
	}
}
