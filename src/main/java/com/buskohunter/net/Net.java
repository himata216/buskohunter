package com.buskohunter.net;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** パケット送信の窓口（Forge のチャンネルを包む）。 */
public final class Net {
	private Net() {
	}

	/** サーバー → 指定プレイヤー。 */
	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		ModNetworking.CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
	}

	/** クライアント → サーバー。 */
	public static void sendToServer(CustomPacketPayload payload) {
		ModNetworking.CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
	}

	/** そのエンティティが見えている（追跡している）プレイヤー。本人は含まない。 */
	public static List<ServerPlayer> tracking(Entity entity) {
		List<ServerPlayer> out = new ArrayList<>();
		if (!(entity.level() instanceof ServerLevel level)) return out;
		double range = 128;
		for (ServerPlayer p : level.players()) {
			if (p != entity && p.distanceToSqr(entity) < range * range) out.add(p);
		}
		return out;
	}
}
