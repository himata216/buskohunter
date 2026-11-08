package com.buskohunter.registry;

import com.buskohunter.BuskoHunter;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.entity.BarrelBomb;
import com.buskohunter.entity.HuntTrap;
import com.buskohunter.entity.HunterShot;
import com.buskohunter.entity.Kinsect;
import com.buskohunter.entity.MonsterProjectile;
import com.buskohunter.entity.TrainingDummy;
import com.buskohunter.monster.HuntMonster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import java.util.LinkedHashMap;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.EnumMap;
import java.util.Map;

/** エンティティ登録。 */
public final class ModEntities {
	private ModEntities() {
	}

	public static final Map<MonsterKind, EntityType<HuntMonster>> MONSTERS = new EnumMap<>(MonsterKind.class);
	public static EntityType<TrainingDummy> TRAINING_DUMMY;
	public static EntityType<MonsterProjectile> MONSTER_PROJECTILE;
	public static EntityType<HunterShot> HUNTER_SHOT;
	public static EntityType<Kinsect> KINSECT;
	public static EntityType<HuntTrap> HUNT_TRAP;
	public static EntityType<BarrelBomb> BARREL_BOMB;
	public static EntityType<com.buskohunter.entity.HubNpc> HUB_NPC;
	public static EntityType<com.buskohunter.entity.HubLabel> HUB_LABEL;
	/** EntityAttributeCreationEvent で登録する属性。 */
	public static final Map<EntityType<? extends LivingEntity>, Supplier<AttributeSupplier>> ATTRIBUTES = new LinkedHashMap<>();

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, BuskoHunter.id(name));
		return Reg.register(Registries.ENTITY_TYPE, key.identifier(), builder.build(key));
	}

	/** エンティティ種類のレジストリイベントで呼ぶ。 */
	public static void registerEntities() {
		for (MonsterKind kind : MonsterKind.values()) {
			EntityType<HuntMonster> type = register(kind.id,
				EntityType.Builder.<HuntMonster>of((t, level) -> new HuntMonster(t, level, kind), MobCategory.MONSTER)
					.sized(kind.width, kind.height)
					.clientTrackingRange(16)
					.fireImmune());
			MONSTERS.put(kind, type);
			ATTRIBUTES.put(type, () -> HuntMonster.createAttributes(kind).build());
		}
		TRAINING_DUMMY = register("training_dummy",
			EntityType.Builder.<TrainingDummy>of(TrainingDummy::new, MobCategory.MISC).sized(1.4f, 1.9f).clientTrackingRange(8));
		ATTRIBUTES.put(TRAINING_DUMMY, () -> TrainingDummy.createAttributes().build());
		MONSTER_PROJECTILE = register("monster_projectile",
			EntityType.Builder.<MonsterProjectile>of(MonsterProjectile::new, MobCategory.MISC).sized(0.9f, 0.9f).clientTrackingRange(8).updateInterval(2));
		HUNTER_SHOT = register("hunter_shot",
			EntityType.Builder.<HunterShot>of(HunterShot::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(1));
		KINSECT = register("kinsect",
			EntityType.Builder.<Kinsect>of(Kinsect::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(1));
		HUNT_TRAP = register("hunt_trap",
			EntityType.Builder.<HuntTrap>of(HuntTrap::new, MobCategory.MISC).sized(1.0f, 0.3f).clientTrackingRange(8));
		BARREL_BOMB = register("barrel_bomb",
			EntityType.Builder.<BarrelBomb>of(BarrelBomb::new, MobCategory.MISC).sized(0.8f, 1.0f).clientTrackingRange(8));
		HUB_NPC = register("hub_npc",
			EntityType.Builder.<com.buskohunter.entity.HubNpc>of(com.buskohunter.entity.HubNpc::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).noSave());
		ATTRIBUTES.put(HUB_NPC, () -> com.buskohunter.entity.HubNpc.createAttributes().build());
		HUB_LABEL = register("hub_label",
			EntityType.Builder.<com.buskohunter.entity.HubLabel>of(com.buskohunter.entity.HubLabel::new, MobCategory.MISC).sized(1.0f, 0.5f)
				.clientTrackingRange(10).updateInterval(100).noSave());
	}
}
