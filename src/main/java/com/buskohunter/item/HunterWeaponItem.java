package com.buskohunter.item;

import com.buskohunter.core.ElementType;
import com.buskohunter.core.WeaponStats;
import com.buskohunter.core.WeaponType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * ハンター武器（14 種、属性は ItemStack の CustomModelData の文字列で持つ）。
 * 攻撃はクリックではなく HunterInput → サーバーの WeaponController で処理するので、
 * バニラの use は何もしない。
 */
public class HunterWeaponItem extends Item {
	public final WeaponType weaponType;

	public HunterWeaponItem(WeaponType weaponType, Properties properties) {
		super(properties);
		this.weaponType = weaponType;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

	/** 属性シリーズの武器 ItemStack を作る（名前・説明・属性色）。 */
	public ItemStack create(ElementType element) {
		ItemStack stack = new ItemStack(this);
		WeaponStats st = WeaponStats.of(weaponType, element);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(st.name()).withColor(element.color));
		stack.set(DataComponents.CUSTOM_MODEL_DATA,
			new CustomModelData(List.of(), List.of(), List.of(element.id), List.of(element.color)));
		List<Component> lore = new ArrayList<>();
		lore.add(Component.literal("攻撃力 " + st.displayAttack() + "  " + element.jaName + " " + st.displayElement()).withColor(0xE0E0E0));
		if (st.affinity() != 0) {
			lore.add(Component.literal("会心率 " + (st.affinity() > 0 ? "+" : "") + st.affinity() + "%").withColor(0xC0C0C0));
		}
		lore.add(Component.literal(st.specialNote()).withColor(0xA0A0A0));
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}

	/** ItemStack から属性を読む。 */
	public static ElementType elementOf(ItemStack stack) {
		CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		if (cmd != null && !cmd.strings().isEmpty()) {
			return ElementType.byId(cmd.strings().get(0));
		}
		return ElementType.FIRE;
	}
}
