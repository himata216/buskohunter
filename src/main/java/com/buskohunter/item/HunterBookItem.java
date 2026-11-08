package com.buskohunter.item;

import com.buskohunter.quest.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** ハンターノート：右クリックで集会所（広場）へ移動、広場では元の世界へ戻る。 */
public class HunterBookItem extends Item {
	public HunterBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			QuestManager.get(sp).toggleHub(sp);
		}
		return InteractionResult.SUCCESS;
	}
}
