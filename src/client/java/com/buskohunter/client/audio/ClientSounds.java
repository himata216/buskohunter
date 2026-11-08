package com.buskohunter.client.audio;

import com.buskohunter.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/** UI 効果音（画面のボタンなど、位置を持たない音）。 */
public final class ClientSounds {
	private ClientSounds() {
	}

	public static void ui(String key, float pitch) {
		ui(key, pitch, 1.0f);
	}

	public static void ui(String key, float pitch, float volume) {
		SoundEvent ev = ModSounds.get(key);
		if (ev == null) return;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, volume));
	}
}
