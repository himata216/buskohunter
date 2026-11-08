package com.buskohunter.client.audio;

import com.buskohunter.client.ClientHunter;
import com.buskohunter.client.screen.QuestResultScreen;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.net.Payloads;
import com.buskohunter.quest.Dims;
import com.buskohunter.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

/**
 * BGM の切り替え。集会所・準備中・フィールド・モンスターごとの戦闘曲・クリア／失敗ジングル・報酬画面。
 * 曲の切り替えはクロスフェード。BGM を流している間はバニラの音楽を止める。
 */
public final class BgmManager {
	private BgmManager() {
	}

	/** フェードできる BGM 1 曲。 */
	private static final class Track extends AbstractTickableSoundInstance {
		final String key;
		private final float fadeIn;
		private float fadeOut = 0.02f;
		private float level;
		private boolean leaving;

		Track(String key, SoundEvent ev, boolean loop, float fadeInPerTick) {
			super(ev, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
			this.key = key;
			this.fadeIn = fadeInPerTick;
			this.looping = loop;
			this.delay = 0;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.x = 0;
			this.y = 0;
			this.z = 0;
			this.level = fadeInPerTick >= 1f ? 1f : 0.02f;
			this.volume = level;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		void leave(float perTick) {
			leaving = true;
			fadeOut = perTick;
		}

		void kill() {
			stop();
		}

		@Override
		public void tick() {
			if (leaving) {
				level -= fadeOut;
				if (level <= 0f) {
					level = 0f;
					stop();
				}
			} else if (level < 1f) {
				level = Math.min(1f, level + fadeIn);
			}
			volume = Math.max(0.0001f, level);
		}
	}

	private static Track current;
	private static final List<Track> leaving = new ArrayList<>();
	/** ジングル（出発・クリア・失敗）の再生中は通常の BGM を流さない。 */
	private static Track jingle;
	private static int jingleTicks;
	/** 戦闘曲を少しの間維持する（モンスターが一瞬離れただけで曲が切り替わらないように）。 */
	private static int battleKind = -1;
	private static int battleHold;

	/** クリア・失敗・出発のジングル。lengthTicks の間は他の BGM を止める。 */
	public static void jingle(String key, int lengthTicks) {
		SoundEvent ev = ModSounds.get(key);
		if (ev == null) return;
		Minecraft mc = Minecraft.getInstance();
		fadeCurrent(0.08f);
		if (jingle != null) jingle.kill();
		jingle = new Track(key, ev, false, 1f);
		jingleTicks = lengthTicks;
		mc.getSoundManager().play(jingle);
	}

	public static void onBanner(Payloads.Banner b) {
		switch (b.kind()) {
			case Payloads.Banner.DEPART -> jingle("music.depart", 20 * 8);
			case Payloads.Banner.QUEST_CLEAR -> jingle("music.clear", 20 * 10);
			case Payloads.Banner.QUEST_FAILED -> jingle("music.failed", 20 * 7);
			default -> {
			}
		}
	}

	private static void fadeCurrent(float perTick) {
		if (current != null) {
			current.leave(perTick);
			leaving.add(current);
			current = null;
		}
	}

	/** 今流すべき BGM のキー（null = 流さない＝バニラの音楽に任せる）。 */
	private static String desired(Minecraft mc) {
		if (mc.level == null || mc.player == null) return null;
		if (mc.gui.screen() instanceof QuestResultScreen) return "music.result";
		Payloads.QuestSync q = ClientHunter.QUEST;
		if (Dims.isField(mc.level)) {
			if (q.state() == Payloads.QuestSync.IN_PROGRESS) {
				if (q.battleKind() >= 0) {
					battleKind = q.battleKind();
					battleHold = 20 * 10;
				}
				if (battleKind >= 0 && battleHold > 0) {
					battleHold--;
					return "music.battle_" + MonsterKind.byOrdinal(battleKind).id;
				}
			} else {
				battleHold = 0;
			}
			if (q.state() == Payloads.QuestSync.FAILED) return "";
			return "music.field";
		}
		if (Dims.isHub(mc.level)) {
			return q.state() == Payloads.QuestSync.PREPARING ? "music.prepare" : "music.hub";
		}
		return null;
	}

	public static void tick(Minecraft mc) {
		leaving.removeIf(Track::isStopped);
		if (jingle != null) {
			if (--jingleTicks <= 0 || jingle.isStopped()) {
				jingle = null;
			}
		}

		String want = desired(mc);
		boolean ours = want != null || jingle != null;
		if (ours) mc.getMusicManager().stopPlaying();

		if (jingle != null) return;
		if (want == null || want.isEmpty()) {
			fadeCurrent(want == null ? 0.05f : 0.02f);
			return;
		}
		if (current != null && current.key.equals(want) && !current.isStopped()) return;

		SoundEvent ev = ModSounds.get(want);
		if (ev == null) return;
		// 戦闘曲へはすばやく、それ以外はゆっくり切り替える
		boolean battle = want.startsWith("music.battle_");
		fadeCurrent(battle ? 0.06f : 0.025f);
		current = new Track(want, ev, true, battle ? 0.08f : 0.02f);
		mc.getSoundManager().play(current);
	}

	public static void stopAll() {
		if (current != null) current.kill();
		for (Track t : leaving) t.kill();
		if (jingle != null) jingle.kill();
		current = null;
		jingle = null;
		leaving.clear();
	}
}
