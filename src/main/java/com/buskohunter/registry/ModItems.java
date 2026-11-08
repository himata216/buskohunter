package com.buskohunter.registry;

import com.buskohunter.BuskoHunter;
import com.buskohunter.core.ArmorSeries;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.HuntItem;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.WeaponType;
import com.buskohunter.item.HuntItemItem;
import com.buskohunter.item.HunterBookItem;
import com.buskohunter.item.HunterWeaponItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** アイテム登録。 */
public final class ModItems {
	private ModItems() {
	}

	public static final Map<WeaponType, HunterWeaponItem> WEAPONS = new EnumMap<>(WeaponType.class);
	public static final Map<HuntItem, HuntItemItem> HUNT_ITEMS = new EnumMap<>(HuntItem.class);
	public static final Map<MonsterKind, Item> TROPHIES = new EnumMap<>(MonsterKind.class);
	public static final Map<String, Item> PROJECTILE_ITEMS = new LinkedHashMap<>();
	public static final Map<ArmorSeries, Item[]> ARMOR = new EnumMap<>(ArmorSeries.class);
	public static Item HUNTER_BOOK;

	public static final String[] PROJECTILES = {
		"proj_bullet", "proj_arrow", "proj_sake_bottle", "proj_fireball", "proj_water_orb", "proj_missile",
		"proj_holy_orb", "proj_glitch_cube", "proj_kinsect", "proj_shell", "proj_sombrero"
	};

	public static final ResourceKey<CreativeModeTab> TAB_KEY =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, BuskoHunter.id("main"));

	static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
		Identifier id = BuskoHunter.id(name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		T item = factory.apply(props.setId(key));
		return Reg.register(Registries.ITEM, id, item);
	}

	/** アイテムのレジストリイベントで呼ぶ。 */
	public static void registerItems() {
		for (WeaponType t : WeaponType.values()) {
			WEAPONS.put(t, register(t.id, p -> new HunterWeaponItem(t, p), new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
		}
		for (HuntItem h : HuntItem.values()) {
			HUNT_ITEMS.put(h, register(h.id, p -> new HuntItemItem(h, p), new Item.Properties().stacksTo(Math.max(1, Math.min(64, h.maxCarry)))));
		}
		for (MonsterKind k : MonsterKind.values()) {
			TROPHIES.put(k, register("trophy_" + k.id, Item::new, new Item.Properties().rarity(Rarity.EPIC)));
		}
		for (String p : PROJECTILES) {
			PROJECTILE_ITEMS.put(p, register(p, Item::new, new Item.Properties()));
		}
		for (ArmorSeries s : ArmorSeries.values()) {
			ArmorMaterial mat = new ArmorMaterial(
				33,
				Map.of(ArmorType.BOOTS, 0, ArmorType.LEGGINGS, 0, ArmorType.CHESTPLATE, 0, ArmorType.HELMET, 0, ArmorType.BODY, 0),
				10,
				SoundEvents.ARMOR_EQUIP_LEATHER,
				0f,
				0f,
				ItemTags.REPAIRS_LEATHER_ARMOR,
				ResourceKey.create(EquipmentAssets.ROOT_ID, BuskoHunter.id(s.id)));
			Item[] pieces = new Item[4];
			ArmorType[] types = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
			String[] names = {"helmet", "chestplate", "leggings", "boots"};
			for (int i = 0; i < 4; i++) {
				ArmorType type = types[i];
				pieces[i] = register(s.id + "_" + names[i], Item::new, new Item.Properties().humanoidArmor(mat, type).stacksTo(1));
			}
			ARMOR.put(s, pieces);
		}
		HUNTER_BOOK = register("hunter_book", HunterBookItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	/** クリエイティブタブのレジストリイベントで呼ぶ。 */
	public static void registerTab() {
		Reg.register(Registries.CREATIVE_MODE_TAB, TAB_KEY.identifier(), CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.buskohunter.main"))
			.icon(() -> WEAPONS.get(WeaponType.GREAT_SWORD).create(ElementType.FIRE))
			.displayItems((ctx, out) -> {
				out.accept(new ItemStack(HUNTER_BOOK));
				for (WeaponType t : WeaponType.values()) {
					for (ElementType e : ElementType.SERIES) {
						out.accept(WEAPONS.get(t).create(e));
					}
				}
				for (HuntItemItem i : HUNT_ITEMS.values()) out.accept(new ItemStack(i));
				for (Item[] arr : ARMOR.values()) {
					for (Item i : arr) out.accept(new ItemStack(i));
				}
				for (Item i : TROPHIES.values()) out.accept(new ItemStack(i));
				for (Item i : ModBlocks.BLOCK_ITEMS.values()) out.accept(new ItemStack(i));
			})
			.build());
	}

	public static Item projectile(String id) {
		return PROJECTILE_ITEMS.get(id);
	}
}
