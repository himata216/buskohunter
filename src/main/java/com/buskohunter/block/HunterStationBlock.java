package com.buskohunter.block;

import com.buskohunter.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 集会所・ベースキャンプの設備ブロック（右クリックで画面を開く）。 */
public class HunterStationBlock extends Block {
	public enum Station {
		QUEST_BOARD("quest_board", "クエストカウンター"),
		EQUIPMENT_BOX("equipment_box", "装備ボックス"),
		CANTEEN("canteen", "ブスコ食堂"),
		ITEM_BOX("item_box", "アイテムボックス"),
		DEPARTURE_GATE("departure_gate", "出発ゲート"),
		SUPPLY_BOX("supply_box", "支給品ボックス"),
		HUNTER_BED("hunter_bed", "ベッド"),
		EXIT_GATE("exit_gate", "帰還ゲート");

		public final String id;
		public final String jaName;

		Station(String id, String jaName) {
			this.id = id;
			this.jaName = jaName;
		}
	}

	public final Station station;

	public HunterStationBlock(Station station, Properties properties) {
		super(properties);
		this.station = station;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			QuestManager.get(sp).useStation(sp, station);
		}
		return InteractionResult.SUCCESS;
	}
}
