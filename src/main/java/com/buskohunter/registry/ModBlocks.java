package com.buskohunter.registry;

import com.buskohunter.BuskoHunter;
import com.buskohunter.block.HunterStationBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.Map;

/** ブロック登録。 */
public final class ModBlocks {
	private ModBlocks() {
	}

	public static final Map<HunterStationBlock.Station, HunterStationBlock> STATIONS = new EnumMap<>(HunterStationBlock.Station.class);
	public static final Map<HunterStationBlock.Station, Item> BLOCK_ITEMS = new EnumMap<>(HunterStationBlock.Station.class);

	/** ブロックのレジストリイベントで呼ぶ。 */
	public static void registerBlocks() {
		for (HunterStationBlock.Station s : HunterStationBlock.Station.values()) {
			Identifier id = BuskoHunter.id(s.id);
			ResourceKey<Block> bk = ResourceKey.create(Registries.BLOCK, id);
			BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
				.strength(-1.0f, 3600000.0f)
				.sound(SoundType.WOOD)
				.setId(bk);
			if (s == HunterStationBlock.Station.DEPARTURE_GATE || s == HunterStationBlock.Station.EXIT_GATE) {
				props = props.lightLevel(state -> 12);
			}
			HunterStationBlock block = Reg.register(Registries.BLOCK, id, new HunterStationBlock(s, props));
			STATIONS.put(s, block);
		}
	}

	/** アイテムのレジストリイベントで呼ぶ（ブロックの後）。 */
	public static void registerItems() {
		for (HunterStationBlock.Station s : HunterStationBlock.Station.values()) {
			Identifier id = BuskoHunter.id(s.id);
			ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, id);
			Item item = Reg.register(Registries.ITEM, id,
				new BlockItem(STATIONS.get(s), new Item.Properties().useBlockDescriptionPrefix().setId(ik)));
			BLOCK_ITEMS.put(s, item);
		}
	}

	public static Block get(HunterStationBlock.Station s) {
		return STATIONS.get(s);
	}
}
