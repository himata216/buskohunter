package com.buskohunter.registry;

import com.buskohunter.BuskoHunter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 効果音・BGM の SoundEvent。キーは assets/buskohunter/sounds.json と一致させる。
 */
public final class ModSounds {
	private ModSounds() {
	}

	public static final String[] KEYS = {
		"music.hub",
		"music.prepare",
		"music.depart",
		"music.field",
		"music.battle_sakeran",
		"music.battle_barino",
		"music.battle_jangria",
		"music.battle_busaiber",
		"music.battle_buskoba",
		"music.clear",
		"music.failed",
		"music.result",
		"weapon.swing_light",
		"weapon.swing_heavy",
		"weapon.swing_blunt",
		"weapon.hit_slash",
		"weapon.hit_blunt",
		"weapon.hit_pierce",
		"weapon.hit_crit",
		"weapon.hit_weak",
		"weapon.charge",
		"weapon.charge_release",
		"weapon.guard",
		"weapon.morph",
		"weapon.phial_burst",
		"weapon.spirit",
		"weapon.demon_mode",
		"weapon.gunlance_shell",
		"weapon.gunlance_wyvernfire",
		"weapon.reload",
		"weapon.bowgun_light",
		"weapon.bowgun_heavy",
		"weapon.bowgun_gatling",
		"weapon.bow_draw",
		"weapon.bow_release",
		"weapon.bow_piercer",
		"weapon.kinsect",
		"weapon.horn_note1",
		"weapon.horn_note2",
		"weapon.horn_note3",
		"weapon.horn_song",
		"weapon.vault",
		"player.dodge",
		"player.hurt",
		"player.sharpen",
		"player.drink",
		"player.heal",
		"player.eat",
		"player.buff",
		"player.faint",
		"player.cart",
		"player.carve",
		"item.paintball",
		"item.flash",
		"item.trap_set",
		"item.trap_shock",
		"item.pitfall",
		"item.tranq",
		"item.bomb",
		"item.farcaster",
		"monster.roar_sakeran",
		"monster.roar_barino",
		"monster.roar_jangria",
		"monster.roar_busaiber",
		"monster.roar_buskoba",
		"monster.step",
		"monster.slam",
		"monster.swipe",
		"monster.hurt",
		"monster.down",
		"monster.death",
		"monster.fire",
		"monster.beam",
		"monster.water",
		"monster.electric",
		"monster.holy",
		"monster.wing",
		"monster.tired",
		"monster.sleep",
		"monster.part_break",
		"monster.glitch",
		"monster.bottle",
		"status.poison",
		"status.sleep",
		"status.paralysis",
		"status.blast",
		"status.buskoba",
		"status.stun",
		"element.fire",
		"element.water",
		"element.thunder",
		"element.ice",
		"element.dragon",
		"ui.select",
		"ui.confirm",
		"ui.back",
		"ui.open",
		"ui.accept",
		"ui.error",
		"ui.reward",
		"quest.start",
		"quest.area",
		"quest.alert",
		"quest.timer"
	};

	private static final Map<String, SoundEvent> EVENTS = new HashMap<>();

	public static void init() {
		for (String key : KEYS) {
			Identifier id = BuskoHunter.id(key);
			SoundEvent ev = SoundEvent.createVariableRangeEvent(id);
			Reg.register(Registries.SOUND_EVENT, id, ev);
			EVENTS.put(key, ev);
		}
	}

	/** キー（例 "weapon.hit_slash"）から SoundEvent。未登録なら null。 */
	public static SoundEvent get(String key) {
		return key == null ? null : EVENTS.get(key);
	}
}
