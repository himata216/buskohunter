package com.buskohunter.hunter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * ハンターの永続データ（プレイヤーに添付して保存）。
 * HR ポイント・所持金・選んだ装備・クリア済みクエスト・持ち込みアイテム・元の世界の帰還位置。
 */
public record HunterProfile(int hrPoints, int coins, int weapon, int element, int armor, long cleared, int hunts,
							List<Integer> carry, String returnDim, int returnX, int returnY, int returnZ, boolean bookGiven) {

	public static final HunterProfile DEFAULT = new HunterProfile(0, 1000, 0, 1, 0, 0L, 0, List.of(), "", 0, 0, 0, false);

	public static final Codec<HunterProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("hr_points", 0).forGetter(HunterProfile::hrPoints),
		Codec.INT.optionalFieldOf("coins", 1000).forGetter(HunterProfile::coins),
		Codec.INT.optionalFieldOf("weapon", 0).forGetter(HunterProfile::weapon),
		Codec.INT.optionalFieldOf("element", 1).forGetter(HunterProfile::element),
		Codec.INT.optionalFieldOf("armor", 0).forGetter(HunterProfile::armor),
		Codec.LONG.optionalFieldOf("cleared", 0L).forGetter(HunterProfile::cleared),
		Codec.INT.optionalFieldOf("hunts", 0).forGetter(HunterProfile::hunts),
		Codec.INT.listOf().optionalFieldOf("carry", List.of()).forGetter(HunterProfile::carry),
		Codec.STRING.optionalFieldOf("return_dim", "").forGetter(HunterProfile::returnDim),
		Codec.INT.optionalFieldOf("return_x", 0).forGetter(HunterProfile::returnX),
		Codec.INT.optionalFieldOf("return_y", 0).forGetter(HunterProfile::returnY),
		Codec.INT.optionalFieldOf("return_z", 0).forGetter(HunterProfile::returnZ),
		Codec.BOOL.optionalFieldOf("book_given", false).forGetter(HunterProfile::bookGiven)
	).apply(i, HunterProfile::new));

	public HunterProfile withPoints(int points, int coins) {
		return new HunterProfile(points, coins, weapon, element, armor, cleared, hunts, carry, returnDim, returnX, returnY, returnZ, bookGiven);
	}

	public HunterProfile withLoadout(int weapon, int element) {
		return new HunterProfile(hrPoints, coins, weapon, element, armor, cleared, hunts, carry, returnDim, returnX, returnY, returnZ, bookGiven);
	}

	public HunterProfile withArmor(int armor) {
		return new HunterProfile(hrPoints, coins, weapon, element, armor, cleared, hunts, carry, returnDim, returnX, returnY, returnZ, bookGiven);
	}

	public HunterProfile withCleared(int questIndex) {
		return new HunterProfile(hrPoints, coins, weapon, element, armor, cleared | (1L << questIndex), hunts + 1, carry,
			returnDim, returnX, returnY, returnZ, bookGiven);
	}

	public HunterProfile withCarry(int index, int count) {
		List<Integer> c = new ArrayList<>(carry);
		while (c.size() <= index) c.add(-1);
		c.set(index, count);
		return new HunterProfile(hrPoints, coins, weapon, element, armor, cleared, hunts, c, returnDim, returnX, returnY, returnZ, bookGiven);
	}

	public HunterProfile withReturn(String dim, int x, int y, int z) {
		return new HunterProfile(hrPoints, coins, weapon, element, armor, cleared, hunts, carry, dim, x, y, z, bookGiven);
	}

	public HunterProfile withBookGiven() {
		return new HunterProfile(hrPoints, coins, weapon, element, armor, cleared, hunts, carry, returnDim, returnX, returnY, returnZ, true);
	}

	public int carryOf(int index, int def) {
		return index < carry.size() && carry.get(index) >= 0 ? carry.get(index) : def;
	}
}
