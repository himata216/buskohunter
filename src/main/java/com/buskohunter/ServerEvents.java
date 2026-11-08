package com.buskohunter;

import com.buskohunter.hunter.HunterManager;
import com.buskohunter.item.HunterWeaponItem;
import com.buskohunter.quest.Dims;
import com.buskohunter.quest.QuestManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.ArrayList;
import java.util.List;

/** サーバー側のイベント登録（Forge のイベントバス）。 */
public final class ServerEvents {
	private ServerEvents() {
	}

	public static void init() {
		TickEvent.ServerTickEvent.Post.BUS.addListener(e -> {
			MinecraftServer server = e.server();
			HunterManager.tick(server);
			QuestManager.INSTANCE.tick(server);
			if (server.getTickCount() % 100 == 0) purgeVanillaMonsters(server);
		});

		// 体力 0 → クエスト中は「力尽きた」扱い（キャンプへ戻る）。true を返すと死亡がキャンセルされる
		LivingDeathEvent.BUS.addListener(e -> e.getEntity() instanceof ServerPlayer p && QuestManager.INSTANCE.onLethal(p));

		// 集会所ではダメージ無し、フィールドでは落下ダメージ無し（true でキャンセル）
		LivingAttackEvent.BUS.addListener(e -> {
			if (!(e.getEntity() instanceof ServerPlayer p)) return false;
			if (Dims.isHub(p.level())) return true;
			return Dims.isField(p.level()) && e.getSource().is(DamageTypeTags.IS_FALL);
		});

		// ハンター武器を持っている間はバニラの攻撃・ブロック破壊をしない（武器の攻撃は独自処理）
		AttackEntityEvent.BUS.addListener(e -> e.getEntity().getMainHandItem().getItem() instanceof HunterWeaponItem);
		PlayerInteractEvent.LeftClickBlock.BUS.addListener(e -> {
			if (e.getEntity().isCreative()) return false;
			if (e.getEntity().getMainHandItem().getItem() instanceof HunterWeaponItem) return true;
			return Dims.isOurs(e.getLevel());
		});

		PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
			if (e.getEntity() instanceof ServerPlayer p) {
				MinecraftServer server = p.level().getServer();
				if (server != null) server.execute(() -> QuestManager.INSTANCE.onJoin(p));
			}
		});
		PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
			if (e.getEntity() instanceof ServerPlayer p) QuestManager.INSTANCE.onLeave(p);
		});

		ServerStoppingEvent.BUS.addListener(e -> {
			QuestManager.INSTANCE.clear();
			HunterManager.clearAll();
		});
	}

	/** 集会所とフィールドからバニラの敵モブ（ゾンビ等）を取り除く。 */
	private static void purgeVanillaMonsters(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			if (!Dims.isOurs(level)) continue;
			List<Entity> remove = new ArrayList<>();
			for (Entity e : level.getAllEntities()) {
				if (e instanceof Monster) remove.add(e);
			}
			for (Entity e : remove) e.discard();
		}
	}
}
