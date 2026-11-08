package com.buskohunter.command;

import com.buskohunter.net.Net;
import com.buskohunter.core.HunterRank;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.QuestDef;
import com.buskohunter.hunter.HunterProfile;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.net.Payloads;
import com.buskohunter.quest.QuestManager;
import com.buskohunter.registry.ModAttachments;
import com.buskohunter.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * /bh コマンド。
 * <pre>
 * /bh hub                  集会所へ
 * /bh exit                 元の世界へ戻る
 * /bh menu                 ハンターメニューを開く
 * /bh quest list           クエスト一覧
 * /bh quest accept &lt;番号&gt;  クエスト受注
 * /bh quest depart         出発
 * /bh quest retire         リタイア
 * /bh info                 自分のハンター情報
 * --- OP 用 ---
 * /bh hr &lt;ポイント&gt;        HR ポイントを設定
 * /bh coins &lt;枚数&gt;        所持金を設定
 * /bh unlockall            全クエストをクリア済みに
 * /bh spawn &lt;0-4&gt; [0-2]   モンスターを目の前に召喚（クエスト外・練習用）
 * </pre>
 */
public final class ModCommands {
	private ModCommands() {
	}

	public static void init() {
		RegisterCommandsEvent.BUS.addListener(e -> register(e.getDispatcher()));
	}

	private static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("bh")
			.then(Commands.literal("hub").executes(c -> run(c, p -> QuestManager.INSTANCE.goHub(p))))
			.then(Commands.literal("exit").executes(c -> run(c, p -> QuestManager.INSTANCE.leaveHub(p))))
			.then(Commands.literal("menu").executes(c -> run(c, p -> {
				QuestManager.INSTANCE.syncHub(p);
				Net.send(p, new Payloads.OpenScreen(Payloads.OpenScreen.HUNTER_MENU));
			})))
			.then(Commands.literal("info").executes(c -> run(c, ModCommands::info)))
			.then(Commands.literal("quest")
				.then(Commands.literal("list").executes(c -> run(c, ModCommands::listQuests)))
				.then(Commands.literal("accept")
					.then(Commands.argument("index", IntegerArgumentType.integer(0, QuestDef.all().size() - 1))
						.executes(c -> run(c, p -> action(p, Payloads.Action.ACCEPT_QUEST, IntegerArgumentType.getInteger(c, "index"))))))
				.then(Commands.literal("depart").executes(c -> run(c, p -> action(p, Payloads.Action.DEPART, 0))))
				.then(Commands.literal("retire").executes(c -> run(c, p -> action(p, Payloads.Action.RETIRE, 0)))))
			.then(Commands.literal("hr").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("points", IntegerArgumentType.integer(0, 1_000_000))
					.executes(c -> run(c, p -> {
						HunterProfile pr = ModAttachments.profile(p);
						ModAttachments.set(p, pr.withPoints(IntegerArgumentType.getInteger(c, "points"), pr.coins()));
						QuestManager.INSTANCE.syncHub(p);
						p.sendSystemMessage(Component.literal("§6HR " + HunterRank.hr(ModAttachments.profile(p).hrPoints()) + " に設定しました"));
					}))))
			.then(Commands.literal("coins").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("amount", IntegerArgumentType.integer(0, 100_000_000))
					.executes(c -> run(c, p -> {
						HunterProfile pr = ModAttachments.profile(p);
						ModAttachments.set(p, pr.withPoints(pr.hrPoints(), IntegerArgumentType.getInteger(c, "amount")));
						QuestManager.INSTANCE.syncHub(p);
					}))))
			.then(Commands.literal("unlockall").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(c -> run(c, p -> {
					HunterProfile pr = ModAttachments.profile(p);
					for (QuestDef q : QuestDef.all()) pr = pr.withCleared(q.index());
					pr = pr.withPoints(Math.max(pr.hrPoints(), HunterRank.POINTS_PER_RANK * 8), pr.coins());
					ModAttachments.set(p, pr);
					QuestManager.INSTANCE.syncHub(p);
					p.sendSystemMessage(Component.literal("§6全クエストを解放しました"));
				})))
			.then(Commands.literal("spawn").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("kind", IntegerArgumentType.integer(0, MonsterKind.values().length - 1))
					.executes(c -> run(c, p -> spawn(p, IntegerArgumentType.getInteger(c, "kind"), 0)))
					.then(Commands.argument("rank", IntegerArgumentType.integer(0, 2))
						.executes(c -> run(c, p -> spawn(p, IntegerArgumentType.getInteger(c, "kind"), IntegerArgumentType.getInteger(c, "rank")))))))
		);
	}

	private interface PlayerAction {
		void run(ServerPlayer p) throws CommandSyntaxException;
	}

	private static int run(CommandContext<CommandSourceStack> c, PlayerAction action) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		action.run(p);
		return 1;
	}

	private static void action(ServerPlayer p, int action, int a) {
		QuestManager.INSTANCE.handleAction(p, new Payloads.Action(action, a, 0));
	}

	private static void info(ServerPlayer p) {
		HunterProfile pr = ModAttachments.profile(p);
		p.sendSystemMessage(Component.literal("§6=== ハンター情報 ==="));
		p.sendSystemMessage(Component.literal("HR " + HunterRank.hr(pr.hrPoints()) + "（" + pr.hrPoints() + " pt）  所持金 " + pr.coins() + "z  狩猟数 " + pr.hunts()));
	}

	private static void listQuests(ServerPlayer p) {
		HunterProfile pr = ModAttachments.profile(p);
		p.sendSystemMessage(Component.literal("§6=== クエスト一覧 ==="));
		for (QuestDef q : QuestDef.all()) {
			boolean ok = HunterRank.canAccept(pr.hrPoints(), q);
			boolean cleared = (pr.cleared() & (1L << q.index())) != 0;
			p.sendSystemMessage(Component.literal((ok ? "§f" : "§8") + "[" + q.index() + "] " + q.rank().jaName + " ★" + q.stars() + " "
				+ q.title() + (cleared ? " §a(クリア済)" : "")));
		}
	}

	private static void spawn(ServerPlayer p, int kindIndex, int rankIndex) {
		MonsterKind kind = MonsterKind.byOrdinal(kindIndex);
		ServerLevel level = (ServerLevel) p.level();
		HuntMonster m = ModEntities.MONSTERS.get(kind).create(level, EntitySpawnReason.COMMAND);
		if (m == null) return;
		Vec3 look = p.getLookAngle();
		Vec3 pos = p.position().add(look.x * 12, 0, look.z * 12);
		m.setPos(pos.x, pos.y, pos.z);
		m.setYRot(p.getYRot() + 180f);
		m.setupStandalone(QuestDef.Rank.values()[rankIndex]);
		level.addFreshEntity(m);
		p.sendSystemMessage(Component.literal("§c" + kind.fullName() + " §fを召喚しました"));
	}
}
