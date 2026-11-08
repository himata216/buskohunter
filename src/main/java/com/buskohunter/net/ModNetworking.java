package com.buskohunter.net;

import com.buskohunter.BuskoHunter;
import com.buskohunter.client.ClientPackets;
import com.buskohunter.hunter.HunterManager;
import com.buskohunter.quest.QuestManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

/**
 * パケットの登録と受信処理。
 * クライアント向けの処理は ClientPackets（クライアント専用クラス）に任せる。
 * ラムダの中でだけ呼ぶので、専用サーバーでは ClientPackets は読み込まれない。
 */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static Channel<CustomPacketPayload> CHANNEL;

	public static void init() {
		CHANNEL = ChannelBuilder.named(BuskoHunter.id("main"))
			.networkProtocolVersion(1)
			.payloadChannel()
			.play()
			.serverbound()
				.addMain(Payloads.Input.TYPE, Payloads.Input.CODEC, (p, ctx) -> {
					if (ctx.getSender() != null) HunterManager.onInput(ctx.getSender(), p.bits());
				})
				.addMain(Payloads.Action.TYPE, Payloads.Action.CODEC, (p, ctx) -> {
					if (ctx.getSender() != null) QuestManager.INSTANCE.handleAction(ctx.getSender(), p);
				})
			.clientbound()
				.addMain(Payloads.HunterSync.TYPE, Payloads.HunterSync.CODEC, (p, ctx) -> ClientPackets.hunterSync(p))
				.addMain(Payloads.MoveEvent.TYPE, Payloads.MoveEvent.CODEC, (p, ctx) -> ClientPackets.moveEvent(p))
				.addMain(Payloads.QuestSync.TYPE, Payloads.QuestSync.CODEC, (p, ctx) -> ClientPackets.questSync(p))
				.addMain(Payloads.HubSync.TYPE, Payloads.HubSync.CODEC, (p, ctx) -> ClientPackets.hubSync(p))
				.addMain(Payloads.OpenScreen.TYPE, Payloads.OpenScreen.CODEC, (p, ctx) -> ClientPackets.openScreen(p))
				.addMain(Payloads.Banner.TYPE, Payloads.Banner.CODEC, (p, ctx) -> ClientPackets.banner(p))
				.addMain(Payloads.DamageNumber.TYPE, Payloads.DamageNumber.CODEC, (p, ctx) -> ClientPackets.damageNumber(p))
				.addMain(Payloads.Knockback.TYPE, Payloads.Knockback.CODEC, (p, ctx) -> ClientPackets.knockback(p))
				.addMain(Payloads.Result.TYPE, Payloads.Result.CODEC, (p, ctx) -> ClientPackets.result(p))
			.build();
	}
}
