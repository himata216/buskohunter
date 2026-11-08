package com.buskohunter.core;

/** ハンターランク（HR）。ポイントの累計で上がる。 */
public final class HunterRank {
	private HunterRank() {
	}

	public static final int POINTS_PER_RANK = 400;

	public static int hr(int points) {
		return Math.min(999, 1 + Math.max(0, points) / POINTS_PER_RANK);
	}

	public static float progress(int points) {
		return (Math.max(0, points) % POINTS_PER_RANK) / (float) POINTS_PER_RANK;
	}

	public static boolean canAccept(int points, QuestDef quest) {
		return hr(points) >= quest.rank().requiredHr;
	}
}
