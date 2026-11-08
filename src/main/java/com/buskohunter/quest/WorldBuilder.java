package com.buskohunter.quest;

import com.buskohunter.block.HunterStationBlock.Station;
import com.buskohunter.core.FieldType;
import com.buskohunter.entity.TrainingDummy;
import com.buskohunter.registry.ModBlocks;
import com.buskohunter.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * 集会所（広場）・ベースキャンプ・フィールドの装飾をプログラムで建てる。
 * 色付きブロック（羊毛など）は 26.2 で API が変わったので、色の無いブロックだけで組んでいる。
 */
public final class WorldBuilder {
	private WorldBuilder() {
	}

	private static final int FLAGS = Block.UPDATE_CLIENTS;

	private static void set(ServerLevel level, int x, int y, int z, BlockState s) {
		level.setBlock(new BlockPos(x, y, z), s, FLAGS);
	}

	private static void set(ServerLevel level, int x, int y, int z, Block b) {
		set(level, x, y, z, b.defaultBlockState());
	}

	private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
		BlockState s = b.defaultBlockState();
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
					set(level, x, y, z, s);
	}

	private static BlockState leaves(Block b) {
		BlockState s = b.defaultBlockState();
		if (s.hasProperty(LeavesBlock.PERSISTENT)) s = s.setValue(LeavesBlock.PERSISTENT, true);
		return s;
	}

	// =====================================================================
	// 集会所
	// =====================================================================
	private static int hubBase(ServerLevel level) {
		return FieldLayout.surface(level, 60, 60);
	}

	/** 集会所の地面の高さ（立つ位置の Y）。 */
	public static int hubFloorY(ServerLevel level) {
		return hubBase(level);
	}

	public static Vec3 hubSpawn(ServerLevel level) {
		return new Vec3(0.5, hubBase(level), 10.5);
	}

	public static void ensureHub(ServerLevel level) {
		int y = hubBase(level);
		if (level.getBlockState(new BlockPos(0, y, -16)).getBlock() == ModBlocks.get(Station.QUEST_BOARD)) return;
		buildHub(level, y);
	}

	private static void buildHub(ServerLevel level, int y) {
		int floor = y - 1;
		// 整地
		fill(level, -34, y, -34, 34, y + 18, 34, Blocks.AIR);
		// 外周の草地と広場の石畳
		for (int x = -34; x <= 34; x++) {
			for (int z = -34; z <= 34; z++) {
				double r = Math.sqrt(x * x + z * z);
				Block b;
				if (r <= 4) b = Blocks.POLISHED_ANDESITE;
				else if (r <= 24) b = ((x + z) & 3) == 0 ? Blocks.CHISELED_STONE_BRICKS : (r % 6 < 1 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
				else if (r <= 25) b = Blocks.SMOOTH_STONE;
				else b = Blocks.GRASS_BLOCK;
				set(level, x, floor, z, b);
			}
		}
		// 中央の噴水
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				double r = Math.sqrt(x * x + z * z);
				if (r <= 4.2 && r > 3.2) set(level, x, y, z, Blocks.STONE_BRICK_WALL);
				else if (r <= 3.2) set(level, x, floor, z, Blocks.WATER);
			}
		}
		fill(level, 0, y, 0, 0, y + 3, 0, Blocks.QUARTZ_PILLAR);
		set(level, 0, y + 4, 0, Blocks.SEA_LANTERN);
		set(level, 0, y + 5, 0, Blocks.GOLD_BLOCK);

		// 北：クエストカウンター
		fill(level, -6, floor, -20, 6, floor, -14, Blocks.SPRUCE_PLANKS);
		fill(level, -5, y, -15, 5, y, -15, Blocks.SPRUCE_PLANKS);
		set(level, 0, y, -16, ModBlocks.get(Station.QUEST_BOARD));
		set(level, 0, y + 1, -16, ModBlocks.get(Station.QUEST_BOARD));
		for (int x : new int[]{-6, 6}) {
			fill(level, x, y, -20, x, y + 5, -20, Blocks.DARK_OAK_LOG);
			fill(level, x, y, -14, x, y + 5, -14, Blocks.DARK_OAK_LOG);
		}
		fill(level, -7, y + 6, -21, 7, y + 6, -13, Blocks.RED_NETHER_BRICKS);
		fill(level, -6, y + 7, -20, 6, y + 7, -14, Blocks.NETHER_BRICKS);
		fill(level, -5, y + 1, -20, 5, y + 4, -20, Blocks.BOOKSHELF);
		set(level, -3, y + 1, -15, Blocks.LANTERN);
		set(level, 3, y + 1, -15, Blocks.LANTERN);
		set(level, -5, y + 1, -15, Blocks.BARREL);
		set(level, 5, y + 1, -15, Blocks.BARREL);

		// 東：ブスコ食堂
		fill(level, 13, floor, -5, 21, floor, 5, Blocks.OAK_PLANKS);
		set(level, 17, y, 0, ModBlocks.get(Station.CANTEEN));
		set(level, 17, y, -2, Blocks.CAMPFIRE);
		set(level, 17, y, 2, Blocks.CAMPFIRE);
		for (int z : new int[]{-4, 4}) {
			set(level, 14, y, z, Blocks.OAK_FENCE);
			set(level, 14, y + 1, z, Blocks.OAK_SLAB);
			set(level, 20, y, z, Blocks.OAK_FENCE);
			set(level, 20, y + 1, z, Blocks.OAK_SLAB);
		}
		for (int x : new int[]{13, 21}) {
			for (int z : new int[]{-5, 5}) {
				fill(level, x, y, z, x, y + 4, z, Blocks.SPRUCE_LOG);
			}
		}
		fill(level, 12, y + 5, -6, 22, y + 5, 6, Blocks.HAY_BLOCK);
		set(level, 15, y + 1, 0, Blocks.LANTERN);

		// 西：装備ボックスとアイテムボックス
		fill(level, -21, floor, -5, -13, floor, 5, Blocks.SPRUCE_PLANKS);
		set(level, -17, y, -2, ModBlocks.get(Station.EQUIPMENT_BOX));
		set(level, -17, y, 2, ModBlocks.get(Station.ITEM_BOX));
		for (int x : new int[]{-13, -21}) {
			for (int z : new int[]{-5, 5}) {
				fill(level, x, y, z, x, y + 4, z, Blocks.DARK_OAK_LOG);
			}
		}
		fill(level, -22, y + 5, -6, -12, y + 5, 6, Blocks.DARK_OAK_PLANKS);
		fill(level, -21, y, -5, -21, y + 2, 5, Blocks.BOOKSHELF);
		set(level, -15, y + 1, 0, Blocks.LANTERN);

		// 南：出発ゲート
		for (int x : new int[]{-4, 4}) {
			fill(level, x, y, 20, x, y + 6, 20, Blocks.DARK_OAK_LOG);
		}
		fill(level, -5, y + 7, 20, 5, y + 7, 20, Blocks.RED_NETHER_BRICKS);
		fill(level, -3, y + 6, 20, 3, y + 6, 20, Blocks.GOLD_BLOCK);
		set(level, 0, y, 20, ModBlocks.get(Station.DEPARTURE_GATE));
		set(level, -2, y, 20, Blocks.LANTERN);
		set(level, 2, y, 20, Blocks.LANTERN);

		// 南西：帰還ゲート
		fill(level, -14, y, 16, -14, y + 3, 16, Blocks.QUARTZ_PILLAR);
		fill(level, -10, y, 16, -10, y + 3, 16, Blocks.QUARTZ_PILLAR);
		fill(level, -14, y + 4, 16, -10, y + 4, 16, Blocks.SMOOTH_QUARTZ);
		set(level, -12, y, 16, ModBlocks.get(Station.EXIT_GATE));

		// 南東：訓練場
		fill(level, 9, floor, 9, 19, floor, 19, Blocks.SAND);
		for (int i = 9; i <= 19; i++) {
			set(level, i, y, 9, Blocks.OAK_FENCE);
			set(level, i, y, 19, Blocks.OAK_FENCE);
			set(level, 9, y, i, Blocks.OAK_FENCE);
			set(level, 19, y, i, Blocks.OAK_FENCE);
		}
		set(level, 9, y, 14, Blocks.AIR);
		spawnDummy(level, 14.5, y, 15.5);
		spawnDummy(level, 17.5, y, 12.5);
		spawnDummy(level, 11.5, y, 12.5);

		// 街灯と木
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			int x = (int) Math.round(Math.cos(a) * 24.5);
			int z = (int) Math.round(Math.sin(a) * 24.5);
			fill(level, x, y, z, x, y + 2, z, Blocks.SPRUCE_FENCE);
			set(level, x, y + 3, z, Blocks.LANTERN);
		}
		Random rng = new Random(42);
		for (int i = 0; i < 18; i++) {
			double a = rng.nextDouble() * Math.PI * 2;
			double r = 28 + rng.nextDouble() * 5;
			tree(level, (int) (Math.cos(a) * r), y, (int) (Math.sin(a) * r), rng.nextBoolean() ? Blocks.CHERRY_LOG : Blocks.OAK_LOG,
				rng.nextBoolean() ? Blocks.CHERRY_LEAVES : Blocks.OAK_LEAVES, rng);
		}
	}

	private static void spawnDummy(ServerLevel level, double x, double y, double z) {
		if (!level.getEntitiesOfClass(TrainingDummy.class, new AABB(x - 1, y - 1, z - 1, x + 1, y + 3, z + 1)).isEmpty()) return;
		TrainingDummy d = ModEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.TRIGGERED);
		if (d == null) return;
		d.setPos(x, y, z);
		level.addFreshEntity(d);
	}

	private static void tree(ServerLevel level, int x, int y, int z, Block log, Block leaves, Random rng) {
		int h = 4 + rng.nextInt(3);
		fill(level, x, y, z, x, y + h, z, log);
		BlockState lv = leaves(leaves);
		for (int dx = -2; dx <= 2; dx++)
			for (int dz = -2; dz <= 2; dz++)
				for (int dy = h - 1; dy <= h + 1; dy++) {
					if (Math.abs(dx) + Math.abs(dz) + Math.max(0, dy - h) * 2 > 3) continue;
					if (dx == 0 && dz == 0 && dy <= h) continue;
					set(level, x + dx, y + dy, z + dz, lv);
				}
		set(level, x, y + h + 1, z, lv);
	}

	// =====================================================================
	// フィールド
	// =====================================================================
	public static void prepareField(ServerLevel level, FieldType field, FieldLayout layout) {
		int cx = layout.campX, cy = layout.campY, cz = layout.campZ;
		if (level.getBlockState(new BlockPos(cx + 2, cy, cz - 2)).getBlock() != ModBlocks.get(Station.SUPPLY_BOX)) {
			if (field == FieldType.CYBER) buildCyberCity(level, layout);
			buildCamp(level, cx, cy, cz, field);
			decorate(level, field, layout);
		}
	}

	private static void buildCamp(ServerLevel level, int cx, int cy, int cz, FieldType field) {
		fill(level, cx - 6, cy, cz - 6, cx + 6, cy + 8, cz + 6, Blocks.AIR);
		Block floor = field == FieldType.CYBER ? Blocks.POLISHED_DEEPSLATE : Blocks.SPRUCE_PLANKS;
		fill(level, cx - 5, cy - 1, cz - 5, cx + 5, cy - 1, cz + 5, floor);
		fill(level, cx - 5, cy - 4, cz - 5, cx + 5, cy - 2, cz + 5, Blocks.DIRT);
		// テント
		for (int x : new int[]{cx - 4, cx + 4}) {
			for (int z : new int[]{cz - 4, cz}) {
				fill(level, x, cy, z, x, cy + 3, z, Blocks.SPRUCE_LOG);
			}
		}
		fill(level, cx - 5, cy + 4, cz - 5, cx + 5, cy + 4, cz + 1, Blocks.HAY_BLOCK);
		set(level, cx - 2, cy, cz - 3, ModBlocks.get(Station.HUNTER_BED));
		set(level, cx + 2, cy, cz - 2, ModBlocks.get(Station.SUPPLY_BOX));
		set(level, cx, cy, cz - 3, ModBlocks.get(Station.ITEM_BOX));
		set(level, cx + 2, cy, cz + 3, Blocks.CAMPFIRE);
		set(level, cx - 3, cy, cz + 3, Blocks.LANTERN);
		set(level, cx + 4, cy, cz + 4, Blocks.BARREL);
	}

	private static void decorate(ServerLevel level, FieldType field, FieldLayout layout) {
		Random rng = new Random(layout.slot * 31L + field.ordinal());
		if (field != FieldType.CYBER) {
			// 戦う場所（各エリアの中心）は木や草を払って開けた広場にする
			for (int i = 0; i < FieldLayout.AREAS; i++) {
				clearArena(level, layout.areaX[i], layout.areaY[i], layout.areaZ[i], i == layout.nestArea ? 12 : 9);
			}
		}
		switch (field) {
			case DESERT -> {
				// 緑地（巣とエリア4、ほかに点在）。水場は作らない
				for (int a : new int[]{layout.nestArea, 3}) {
					grove(level, layout.areaX[a] + 13, layout.areaY[a], layout.areaZ[a] - 9, rng);
				}
				for (int i = 0; i < 6; i++) {
					double ang = rng.nextDouble() * Math.PI * 2, r = 30 + rng.nextDouble() * 110;
					int gx = layout.campX + (int) (Math.cos(ang) * r), gz = layout.campZ + (int) (Math.sin(ang) * r);
					if (!nearArea(layout, gx, gz, 14)) grove(level, gx, FieldLayout.surface(level, gx, gz), gz, rng);
				}
				patches(level, layout, rng, new Block[]{Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.COARSE_DIRT}, 40);
			}
			case VOLCANO -> {
				patches(level, layout, rng, new Block[]{Blocks.BLACKSTONE, Blocks.DYED_TERRACOTTA.red(), Blocks.GRAVEL, Blocks.BASALT, Blocks.PODZOL}, 70);
				for (int a : new int[]{1, 4, layout.nestArea}) {
					lavaPool(level, layout.areaX[a] + 14, layout.areaY[a], layout.areaZ[a] + 10, rng);
				}
				for (int i = 0; i < 24; i++) {
					double ang = rng.nextDouble() * Math.PI * 2, r = 12 + rng.nextDouble() * 150;
					int px = layout.campX + (int) (Math.cos(ang) * r), pz = layout.campZ + (int) (Math.sin(ang) * r);
					if (!nearArea(layout, px, pz, 11)) rock(level, px, FieldLayout.surface(level, px, pz), pz, rng, Blocks.BASALT, Blocks.BLACKSTONE);
				}
			}
			case FOREST -> {
				// 酒蔵（巣）
				int x = layout.areaX[layout.nestArea], y = layout.areaY[layout.nestArea], z = layout.areaZ[layout.nestArea];
				for (int i = 0; i < 10; i++) {
					set(level, x + 9 + (i % 5), y, z - 4 + (i / 5) * 2, Blocks.BARREL);
				}
			}
			case SKY -> {
				// 天界の門（巣）
				int x = layout.areaX[layout.nestArea], y = layout.areaY[layout.nestArea], z = layout.areaZ[layout.nestArea];
				for (int i = 0; i < 8; i++) {
					double ang = Math.PI * 2 * i / 8;
					int px = x + (int) (Math.cos(ang) * 14), pz = z + (int) (Math.sin(ang) * 14);
					int py = FieldLayout.surface(level, px, pz);
					fill(level, px, py, pz, px, py + 6, pz, Blocks.QUARTZ_PILLAR);
					set(level, px, py + 7, pz, Blocks.GLOWSTONE);
				}
			}
			default -> {
			}
		}
	}

	/** エリアの中心を開けた広場にする（地面より上を空気に）。 */
	private static void clearArena(ServerLevel level, int x, int y, int z, int r) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r) continue;
				for (int dy = 0; dy <= 14; dy++) {
					BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
					if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
				}
			}
		}
	}

	/** 砂原の緑地：草地と木、花（水は無し）。 */
	private static void grove(ServerLevel level, int x, int y, int z, Random rng) {
		Block[] plants = {Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET};
		for (int dx = -9; dx <= 9; dx++) {
			for (int dz = -9; dz <= 9; dz++) {
				double r = Math.sqrt(dx * dx + dz * dz);
				if (r > 8.5 - rng.nextDouble() * 1.8) continue;
				set(level, x + dx, y - 1, z + dz, Blocks.GRASS_BLOCK);
				if (level.getBlockState(new BlockPos(x + dx, y, z + dz)).isAir() && rng.nextInt(3) == 0) {
					set(level, x + dx, y, z + dz, plants[rng.nextInt(plants.length)]);
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			double a = rng.nextDouble() * Math.PI * 2;
			double r = 2.5 + rng.nextDouble() * 4.5;
			boolean jungle = rng.nextBoolean();
			tree(level, x + (int) (Math.cos(a) * r), y, z + (int) (Math.sin(a) * r), jungle ? Blocks.JUNGLE_LOG : Blocks.ACACIA_LOG,
				jungle ? Blocks.JUNGLE_LEAVES : Blocks.ACACIA_LEAVES, rng);
		}
	}

	/** 地面の色むら（いくつかの石・土をまだらに置く）。 */
	private static void patches(ServerLevel level, FieldLayout layout, Random rng, Block[] blocks, int count) {
		for (int i = 0; i < count; i++) {
			double ang = rng.nextDouble() * Math.PI * 2, rr = rng.nextDouble() * 170;
			int px = layout.campX + (int) (Math.cos(ang) * rr), pz = layout.campZ + (int) (Math.sin(ang) * rr);
			if (Math.abs(px - layout.campX) < 8 && Math.abs(pz - layout.campZ) < 8) continue;
			int y = FieldLayout.surface(level, px, pz) - 1;
			Block b = blocks[rng.nextInt(blocks.length)];
			int r = 2 + rng.nextInt(5);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz > r * r - rng.nextInt(r + 1)) continue;
					BlockPos p = new BlockPos(px + dx, y, pz + dz);
					BlockState cur = level.getBlockState(p);
					if (cur.isAir() || !cur.getFluidState().isEmpty()) continue;
					if (!level.getBlockState(p.above()).isAir() && !level.getBlockState(p.above()).canBeReplaced()) continue;
					level.setBlock(p, b.defaultBlockState(), FLAGS);
				}
			}
		}
	}

	/** 岩（小さな塊）。 */
	private static void rock(ServerLevel level, int x, int y, int z, Random rng, Block a, Block b) {
		int r = 1 + rng.nextInt(2);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int h = r + 1 - Math.max(Math.abs(dx), Math.abs(dz)) + rng.nextInt(2);
				for (int dy = 0; dy < h; dy++) {
					set(level, x + dx, y + dy, z + dz, rng.nextInt(3) == 0 ? b : a);
				}
			}
		}
	}

	private static void lavaPool(ServerLevel level, int x, int y, int z, Random rng) {
		y = FieldLayout.surface(level, x, z);
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				double r = Math.sqrt(dx * dx + dz * dz);
				if (r <= 2.5) set(level, x + dx, y - 1, z + dz, Blocks.LAVA);
				else if (r <= 4) set(level, x + dx, y - 1, z + dz, Blocks.MAGMA_BLOCK);
			}
		}
		for (int i = 0; i < 3; i++) {
			int px = x + rng.nextInt(13) - 6, pz = z + rng.nextInt(13) - 6;
			int py = FieldLayout.surface(level, px, pz);
			fill(level, px, py, pz, px, py + 2 + rng.nextInt(4), pz, Blocks.BASALT);
		}
	}

	/** 電脳廃都：平らな床の上にネオンの廃ビル群を建てる。 */
	private static void buildCyberCity(ServerLevel level, FieldLayout layout) {
		Random rng = new Random(layout.slot * 977L);
		int ox = layout.campX, oz = layout.campZ;
		int ground = layout.campY;
		Block[] neon = {Blocks.VERDANT_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.OCHRE_FROGLIGHT, Blocks.SEA_LANTERN, Blocks.CRYING_OBSIDIAN};
		Block[] wall = {Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE};
		for (int gx = -160; gx <= 160; gx += 22) {
			for (int gz = -160; gz <= 160; gz += 22) {
				int bx = ox + gx + rng.nextInt(6), bz = oz + gz + rng.nextInt(6);
				if (nearArea(layout, bx, bz, 26)) {
					// 道路のネオン
					set(level, bx, ground - 1, bz, neon[rng.nextInt(neon.length)]);
					continue;
				}
				int w = 5 + rng.nextInt(5), d = 5 + rng.nextInt(5), h = 10 + rng.nextInt(28);
				Block wl = wall[rng.nextInt(wall.length)];
				Block ne = neon[rng.nextInt(neon.length)];
				for (int y = 0; y < h; y++) {
					boolean ruined = y > h - 6 && rng.nextInt(3) == 0;
					for (int x = 0; x < w; x++) {
						for (int z = 0; z < d; z++) {
							boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
							if (!edge || ruined) continue;
							Block b = wl;
							if (y % 6 == 3) b = ne;
							else if ((x + z) % 3 == 1 && y % 6 != 0) b = Blocks.TINTED_GLASS;
							set(level, bx + x, ground + y, bz + z, b);
						}
					}
				}
				set(level, bx + w / 2, ground + h, bz + d / 2, Blocks.END_ROD);
			}
		}
		// 道路の光るライン
		for (int i = -160; i <= 160; i += 2) {
			set(level, ox + i, ground - 1, oz, Blocks.SEA_LANTERN);
			set(level, ox, ground - 1, oz + i, Blocks.SEA_LANTERN);
		}
	}

	private static boolean nearArea(FieldLayout layout, int x, int z, int r) {
		if (Math.pow(x - layout.campX, 2) + Math.pow(z - layout.campZ, 2) < r * r) return true;
		for (int i = 0; i < FieldLayout.AREAS; i++) {
			if (Math.pow(x - layout.areaX[i], 2) + Math.pow(z - layout.areaZ[i], 2) < r * r) return true;
		}
		return false;
	}
}
