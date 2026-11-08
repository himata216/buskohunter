package com.buskohunter.client;

import com.buskohunter.hunter.HunterState;
import com.buskohunter.net.Payloads;

import java.util.ArrayList;
import java.util.List;

/** クライアント側に同期されたハンター／クエスト情報。 */
public final class ClientHunter {
	private ClientHunter() {
	}

	/** 自分のハンター状態の写し。 */
	public static final HunterState STATE = new HunterState();
	public static Payloads.QuestSync QUEST = Payloads.QuestSync.empty();
	public static Payloads.HubSync HUB = Payloads.HubSync.empty();
	public static long lastHunterSync;
	public static long lastQuestSync;

	/** 画面中央のバナー。 */
	public record ActiveBanner(Payloads.Banner banner, long startMs, long durationMs) {
	}

	public static final List<ActiveBanner> BANNERS = new ArrayList<>();

	public static void pushBanner(Payloads.Banner b) {
		long dur = switch (b.kind()) {
			case Payloads.Banner.QUEST_CLEAR, Payloads.Banner.QUEST_FAILED, Payloads.Banner.MONSTER_INTRO -> 5000;
			case Payloads.Banner.QUEST_START, Payloads.Banner.DEPART -> 4000;
			case Payloads.Banner.POSE -> 2500;
			case Payloads.Banner.SMALL -> 2500;
			default -> 3200;
		};
		// 同じ種類の古い小さな通知は置き換える
		BANNERS.removeIf(a -> a.banner().kind() == b.kind() && b.kind() != Payloads.Banner.INFO);
		BANNERS.add(new ActiveBanner(b, System.currentTimeMillis(), dur));
		while (BANNERS.size() > 4) BANNERS.remove(0);
	}

	public static boolean inQuest() {
		return QUEST.state() == Payloads.QuestSync.IN_PROGRESS || QUEST.state() == Payloads.QuestSync.COMPLETE
			|| QUEST.state() == Payloads.QuestSync.FAILED;
	}

	public static void reset() {
		QUEST = Payloads.QuestSync.empty();
		HUB = Payloads.HubSync.empty();
		BANNERS.clear();
	}
}
