package com.buskohunter.quest;

import com.buskohunter.core.FieldType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * 1 回のクエストで使うフィールドの配置（ベースキャンプとエリア 1〜8）。
 * フィールドのディメンションを X 方向に 1200 ブロックずつ区切った「スロット」を割り当て、
 * 同時に複数のパーティが狩りをしても重ならないようにする。
 */
public final class FieldLayout {
	public static final int AREAS = 8;
	public static final int SLOT_SPACING = 1200;

	public final FieldType field;
	public final int slot;
	public final int campX, campY, campZ;
	public final int[] areaX = new int[AREAS];
	public final int[] areaY = new int[AREAS];
	public final int[] areaZ = new int[AREAS];
	public final int nestArea;
	private boolean wetCamp;

	private FieldLayout(FieldType field, int slot, int cx, int cy, int cz, int nest) {
		this.field = field;
		this.slot = slot;
		this.campX = cx;
		this.campY = cy;
		this.campZ = cz;
		this.nestArea = nest;
	}

	public static FieldLayout create(ServerLevel level, FieldType field, int slot) {
		int ox = slot * SLOT_SPACING;
		// 海や川の上にならないよう、陸地の多い場所を Z 方向に探す
		FieldLayout best = null;
		int bestScore = Integer.MIN_VALUE;
		// （チャンク生成はサーバーを止めるので、候補は 3 か所まで・水場 1 つまでなら即採用）
		for (int oz : new int[]{0, 420, -420}) {
			FieldLayout l = build(level, field, slot, ox, oz, false);
			int score = l.landScore(level);
			if (score > bestScore) {
				best = l;
				bestScore = score;
			}
			if (score >= AREAS) break;
		}
		// どこも水場だった：最初の候補をそのまま使う（キャンプの床は WorldBuilder が水の上に張る）
		if (best.wetCamp) best = build(level, field, slot, ox, 0, true);
		return best;
	}

	private static FieldLayout build(ServerLevel level, FieldType field, int slot, int ox, int oz, boolean force) {
		Random rng = new Random(field.ordinal() * 7919L + slot * 104729L);
		int cy = surface(level, ox, oz);
		int nest = 5;
		FieldLayout l = new FieldLayout(field, slot, ox, cy, oz, nest);
		if (!force && !dry(level, ox, cy, oz)) {
			// キャンプが水の上なら、この候補はエリアを調べずに捨てる（チャンク生成を節約）
			l.wetCamp = true;
			for (int i = 0; i < AREAS; i++) {
				l.areaX[i] = ox;
				l.areaY[i] = cy;
				l.areaZ[i] = oz;
			}
			return l;
		}
		for (int i = 0; i < AREAS; i++) {
			double angle = Math.PI * 2 * i / AREAS + (rng.nextDouble() - 0.5) * 0.5;
			double radius = (i % 2 == 0 ? 62 : 108) + rng.nextInt(18);
			if (i == nest) radius = 140;
			int x = ox + (int) Math.round(Math.cos(angle) * radius);
			int z = oz + (int) Math.round(Math.sin(angle) * radius);
			l.areaX[i] = x;
			l.areaZ[i] = z;
			l.areaY[i] = surface(level, x, z);
		}
		return l;
	}

	/** キャンプとエリアのうち、水の上でない地点の数（キャンプは 1 点分）。 */
	private int landScore(ServerLevel level) {
		if (wetCamp) return -AREAS;
		int score = dry(level, campX, campY, campZ) ? 1 : -AREAS;
		for (int i = 0; i < AREAS; i++) {
			if (dry(level, areaX[i], areaY[i], areaZ[i])) score++;
		}
		return score;
	}

	private static boolean dry(ServerLevel level, int x, int y, int z) {
		return level.getBlockState(new BlockPos(x, y - 1, z)).getFluidState().isEmpty()
			&& level.getBlockState(new BlockPos(x, y, z)).getFluidState().isEmpty();
	}

	/** 地表の高さ（立てる位置の Y）。チャンクが無ければ生成する。木の幹・葉・草は地面とみなさない。 */
	public static int surface(ServerLevel level, int x, int z) {
		// 未ロードのチャンクだと高さが取れないので、先に読み込む（無ければ生成される）
		level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (y <= level.getMinY() + 1) return 64;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y - 1, z);
		int guard = 0;
		while (pos.getY() > level.getMinY() + 1 && guard++ < 40) {
			BlockState s = level.getBlockState(pos);
			if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.isAir() || s.canBeReplaced()) {
				pos.move(0, -1, 0);
			} else {
				break;
			}
		}
		return pos.getY() + 1;
	}

	public Vec3 campSpawn() {
		return new Vec3(campX + 0.5, campY + 1, campZ + 3.5);
	}

	public Vec3 areaPos(int i) {
		return new Vec3(areaX[i] + 0.5, areaY[i], areaZ[i] + 0.5);
	}

	/** 狩猟対象ごとの最初のエリア（キャンプから少し離れた所）。 */
	public int startArea(int targetIndex) {
		return switch (targetIndex % 4) {
			case 0 -> 2;
			case 1 -> 6;
			case 2 -> 3;
			default -> 7;
		};
	}
}
