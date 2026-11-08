package com.buskohunter.client;

import com.buskohunter.client.anim.PlayerAnimator;
import com.buskohunter.client.audio.BgmManager;
import com.buskohunter.client.hud.DamageNumbers;
import com.buskohunter.client.hud.HunterHud;
import com.buskohunter.client.input.HunterInput;
import com.buskohunter.client.render.DummyRenderer;
import com.buskohunter.client.render.MonsterRenderer;
import com.buskohunter.client.render.model.BuskoMeshes;
import com.buskohunter.client.screen.QuestResultScreen;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.net.Payloads;
import com.buskohunter.quest.Dims;
import com.buskohunter.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;

/** クライアント側の初期化（BuskoHunter からクライアントのときだけ呼ばれる）。 */
public final class BuskoHunterClient {
	private BuskoHunterClient() {
	}

	/** 次に開く報酬画面（ディメンション移動の読み込み画面が終わってから開く）。 */
	private static Payloads.Result pendingResult;
	private static int pendingDelay;
	private static int pendingWait;

	public static void init() {
		EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(e -> {
			e.registerLayerDefinition(MonsterRenderer.BUSKO_LAYER, BuskoMeshes::createBuskoLayer);
			e.registerLayerDefinition(MonsterRenderer.SAKERAN_LAYER, BuskoMeshes::createSakeranLayer);
		});
		EntityRenderersEvent.RegisterRenderers.BUS.addListener(BuskoHunterClient::registerRenderers);
		HunterInput.register();
		HunterHud.register();

		TickEvent.ClientTickEvent.Pre.BUS.addListener(e -> HunterInput.tick(Minecraft.getInstance()));
		TickEvent.ClientTickEvent.Post.BUS.addListener(e -> {
			Minecraft mc = Minecraft.getInstance();
			BgmManager.tick(mc);
			tickPendingResult(mc);
		});
		ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(e -> {
			ClientHunter.reset();
			PlayerAnimator.clear();
			DamageNumbers.clear();
			BgmManager.stopAll();
			pendingResult = null;
		});
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
		for (MonsterKind kind : MonsterKind.values()) {
			e.registerEntityRenderer(ModEntities.MONSTERS.get(kind), ctx -> new MonsterRenderer(ctx, kind));
		}
		e.registerEntityRenderer(ModEntities.TRAINING_DUMMY, DummyRenderer::new);
		e.registerEntityRenderer(ModEntities.MONSTER_PROJECTILE, ctx -> new ThrownItemRenderer<>(ctx, 2.4f, true));
		e.registerEntityRenderer(ModEntities.HUNTER_SHOT, ctx -> new ThrownItemRenderer<>(ctx, 0.8f, true));
		e.registerEntityRenderer(ModEntities.KINSECT, ctx -> new ThrownItemRenderer<>(ctx, 1.2f, true));
		e.registerEntityRenderer(ModEntities.HUNT_TRAP, ctx -> new ThrownItemRenderer<>(ctx, 1.6f, false));
		e.registerEntityRenderer(ModEntities.BARREL_BOMB, ctx -> new ThrownItemRenderer<>(ctx, 2.0f, false));
	}

	static void queueResult(Payloads.Result p) {
		pendingResult = p;
		pendingDelay = 20;
		pendingWait = 0;
	}

	private static void tickPendingResult(Minecraft mc) {
		if (pendingResult == null || mc.player == null || mc.level == null) return;
		pendingWait++;
		// 集会所に着いて読み込み画面が消えるまで待つ（20 秒経ったら場所に関係なく開く）
		boolean arrived = Dims.isHub(mc.level) || pendingWait > 20 * 20;
		if (!arrived || mc.gui.screen() != null) {
			pendingDelay = 20;
			return;
		}
		if (pendingDelay-- > 0) return;
		Payloads.Result r = pendingResult;
		pendingResult = null;
		ClientHunter.QUEST = Payloads.QuestSync.empty();
		mc.gui.setScreen(new QuestResultScreen(r));
	}
}
