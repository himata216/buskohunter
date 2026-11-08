package com.buskohunter.quest;

import com.buskohunter.core.ElementType;
import com.buskohunter.core.HuntItem;
import com.buskohunter.core.WeaponType;
import com.buskohunter.entity.HubLabel;
import com.buskohunter.entity.HubNpc;
import com.buskohunter.registry.ModEntities;
import com.buskohunter.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 集会所の NPC と看板（浮かぶ文字）を置く。どちらもディスクには保存しないので、
 * 集会所に人がいる間は 2 秒ごとに「足りない物を置き直す」。
 */
public final class HubPopulator {
	private HubPopulator() {
	}

	private record NpcSpot(HubNpc.Role role, double x, double z, float yaw) {
	}

	private record LabelSpot(String title, String sub, int color, double x, double dy, double z, float scale) {
	}

	private static final NpcSpot[] NPCS = {
		new NpcSpot(HubNpc.Role.RECEPTIONIST, 2.5, -16.5, 0f),
		new NpcSpot(HubNpc.Role.GUILD_MASTER, -2.5, -16.5, 0f),
		new NpcSpot(HubNpc.Role.COOK, 19.5, 0.5, 90f),
		new NpcSpot(HubNpc.Role.SMITH, -19.5, -1.5, -90f),
		new NpcSpot(HubNpc.Role.CLERK, -19.5, 2.5, -90f),
		new NpcSpot(HubNpc.Role.GATEKEEPER, 2.5, 18.5, 180f),
		new NpcSpot(HubNpc.Role.INSTRUCTOR, 7.5, 14.5, 90f),
		new NpcSpot(HubNpc.Role.ROOKIE, -6.5, 7.5, 135f),
		new NpcSpot(HubNpc.Role.BARD, 6.5, -6.5, -45f),
	};

	private static final LabelSpot[] LABELS = {
		new LabelSpot("ブスコハンター 集会所", "", 0xFFD860, 0.5, 7.2, 0.5, 2.2f),
		new LabelSpot("クエストカウンター", "受付嬢に話しかけてクエストを受注", 0xFFE080, 0.5, 3.3, -15.0, 1.5f),
		new LabelSpot("装備ボックス", "武器・属性・防具を選ぶ", 0xFFB060, -16.5, 2.4, -1.5, 1.2f),
		new LabelSpot("アイテムボックス", "回復薬や罠を受け取る", 0x90FF90, -16.5, 2.4, 2.5, 1.2f),
		new LabelSpot("ブスコ食堂", "食事で体力・スタミナ UP", 0xFFA070, 17.5, 3.0, 0.5, 1.5f),
		new LabelSpot("出発ゲート", "クエストに出発する", 0x80C8FF, 0.5, 4.4, 20.5, 1.6f),
		new LabelSpot("帰還ゲート", "元の世界へ戻る", 0xE0E0FF, -11.5, 2.6, 16.5, 1.2f),
		new LabelSpot("訓練場", "人形で技とダメージを確かめる", 0xFF8080, 14.5, 3.0, 9.0, 1.4f),
	};

	public static void tick(MinecraftServer server) {
		ServerLevel hub = server.getLevel(Dims.HUB);
		if (hub == null || hub.players().isEmpty()) return;
		int y = WorldBuilder.hubFloorY(hub);

		Map<HubNpc.Role, HubNpc> npcs = new EnumMap<>(HubNpc.Role.class);
		for (HubNpc n : hub.getEntities(EntityTypeTest.forClass(HubNpc.class), e -> e.isAlive())) {
			if (npcs.containsKey(n.role())) n.discard();
			else npcs.put(n.role(), n);
		}
		for (NpcSpot s : NPCS) {
			BlockPos pos = BlockPos.containing(s.x, y, s.z);
			if (!hub.isPositionEntityTicking(pos)) continue;
			HubNpc n = npcs.get(s.role);
			if (n == null) {
				n = ModEntities.HUB_NPC.create(hub, EntitySpawnReason.TRIGGERED);
				if (n == null) continue;
				n.setRole(s.role);
				n.snapTo(s.x, y, s.z, s.yaw, 0f);
				n.setYHeadRot(s.yaw);
				n.setYBodyRot(s.yaw);
				equip(n, s.role);
				hub.addFreshEntity(n);
			}
			n.homeX = s.x;
			n.homeY = y;
			n.homeZ = s.z;
			n.homeYaw = s.yaw;
		}

		Map<String, HubLabel> labels = new HashMap<>();
		for (HubLabel l : hub.getEntities(EntityTypeTest.forClass(HubLabel.class), e -> e.isAlive())) {
			if (labels.containsKey(l.title())) l.discard();
			else labels.put(l.title(), l);
		}
		for (LabelSpot s : LABELS) {
			BlockPos pos = BlockPos.containing(s.x, y, s.z);
			if (!hub.isPositionEntityTicking(pos) || labels.containsKey(s.title)) continue;
			HubLabel l = ModEntities.HUB_LABEL.create(hub, EntitySpawnReason.TRIGGERED);
			if (l == null) continue;
			l.setup(s.title, s.sub, s.color, s.scale);
			l.snapTo(s.x, y + s.dy, s.z, 0f, 0f);
			hub.addFreshEntity(l);
		}
	}

	private static void equip(HubNpc n, HubNpc.Role role) {
		ItemStack hand = switch (role) {
			case SMITH -> ModItems.WEAPONS.get(WeaponType.HAMMER).create(ElementType.FIRE);
			case GATEKEEPER -> ModItems.WEAPONS.get(WeaponType.LANCE).create(ElementType.THUNDER);
			case INSTRUCTOR -> ModItems.WEAPONS.get(WeaponType.LONG_SWORD).create(ElementType.DRAGON);
			case ROOKIE -> ModItems.WEAPONS.get(WeaponType.SWORD_AND_SHIELD).create(ElementType.WATER);
			case COOK -> new ItemStack(ModItems.HUNT_ITEMS.get(HuntItem.WELL_DONE_STEAK));
			case CLERK -> new ItemStack(ModItems.HUNT_ITEMS.get(HuntItem.POTION));
			case RECEPTIONIST -> new ItemStack(Items.WRITABLE_BOOK);
			case BARD -> new ItemStack(Items.GOAT_HORN);
			default -> ItemStack.EMPTY;
		};
		n.setItemSlot(EquipmentSlot.MAINHAND, hand);
		n.setDropChance(EquipmentSlot.MAINHAND, 0f);
	}
}
