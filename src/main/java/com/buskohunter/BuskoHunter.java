package com.buskohunter;

import com.buskohunter.combat.MoveRegistry;
import com.buskohunter.command.ModCommands;
import com.buskohunter.net.ModNetworking;
import com.buskohunter.registry.ModBlocks;
import com.buskohunter.registry.ModEntities;
import com.buskohunter.registry.ModItems;
import com.buskohunter.registry.ModSounds;
import com.buskohunter.registry.Reg;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ブスコハンター — ブスコたちを狩るモンハン風ボス討伐 MOD（Forge 版）。
 */
@Mod(BuskoHunter.MOD_ID)
public final class BuskoHunter {
	public static final String MOD_ID = "buskohunter";
	public static final Logger LOGGER = LoggerFactory.getLogger("BuskoHunter");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public BuskoHunter(FMLJavaModLoadingContext context) {
		var modBus = context.getModBusGroup();
		MoveRegistry.init();

		// 登録はレジストリごとのイベントの中で行う
		RegisterEvent.getBus(modBus).addListener(event -> {
			Reg.begin(event);
			try {
				var key = event.getRegistryKey();
				if (key.equals(Registries.SOUND_EVENT)) ModSounds.init();
				else if (key.equals(Registries.BLOCK)) ModBlocks.registerBlocks();
				else if (key.equals(Registries.ITEM)) {
					ModBlocks.registerItems();
					ModItems.registerItems();
				} else if (key.equals(Registries.ENTITY_TYPE)) ModEntities.registerEntities();
				else if (key.equals(Registries.CREATIVE_MODE_TAB)) ModItems.registerTab();
			} finally {
				Reg.end();
			}
		});
		EntityAttributeCreationEvent.BUS.addListener(e -> ModEntities.ATTRIBUTES.forEach((type, attrs) -> e.put(type, attrs.get())));

		ModNetworking.init();
		ModCommands.init();
		ServerEvents.init();

		if (FMLEnvironment.dist == Dist.CLIENT) {
			com.buskohunter.client.BuskoHunterClient.init();
		}
		LOGGER.info("ブスコハンター 起動: 技 {} 個", MoveRegistry.count());
	}
}
