package com.buskohunter.item;

import com.buskohunter.core.HuntItem;
import com.buskohunter.hunter.ItemActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/** 狩猟アイテム（回復薬・砥石・罠・閃光玉など）。 */
public class HuntItemItem extends Item {
	public final HuntItem kind;

	public HuntItemItem(HuntItem kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (kind.useTicks > 0) {
			player.startUsingItem(hand);
			if (!level.isClientSide() && player instanceof ServerPlayer sp) {
				ItemActions.onStartUse(sp, kind);
			}
			return InteractionResult.CONSUME;
		}
		// 投げる・置くアイテムは即時
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			if (ItemActions.useInstant(sp, kind)) {
				if (!sp.isCreative()) player.getItemInHand(hand).shrink(1);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return kind.useTicks;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return switch (kind.use) {
			case DRINK -> ItemUseAnimation.DRINK;
			case EAT -> ItemUseAnimation.EAT;
			default -> ItemUseAnimation.BLOCK;
		};
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide() && entity instanceof ServerPlayer sp) {
			ItemActions.finishUse(sp, kind);
			if (!sp.isCreative()) stack.shrink(1);
		}
		return stack;
	}
}
