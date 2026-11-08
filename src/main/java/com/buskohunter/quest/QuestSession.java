package com.buskohunter.quest;

import com.buskohunter.net.Net;
import com.buskohunter.combat.MoveRegistry;
import com.buskohunter.core.HunterRank;
import com.buskohunter.core.Meal;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.QuestDef;
import com.buskohunter.hunter.HunterManager;
import com.buskohunter.hunter.HunterProfile;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.monster.HuntMonster;
import com.buskohunter.net.Payloads;
import com.buskohunter.registry.ModAttachments;
import com.buskohunter.registry.ModEntities;
import com.buskohunter.util.Fx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 受注中〜進行中のクエスト 1 つ（最大 4 人のパーティ）。 */
public class QuestSession {
	public enum Phase { PREPARING, DEPARTING, IN_PROGRESS, COMPLETE, FAILED, ENDED }

	public final int id;
	public final QuestDef quest;
	public UUID leader;
	public final List<UUID> members = new ArrayList<>();
	public Phase phase = Phase.PREPARING;
	public int timeLeft;
	public int faints;
	public final int maxFaints = 3;
	private int phaseTimer;
	private int elapsed;
	public FieldLayout layout;
	/** 狩猟対象ごとのエンティティ（null = まだ出現していない）。 */
	private final UUID[] targets;
	private final int[] targetState;
	private int nextSpawnDelay;
	private final List<String> rewardLines = new ArrayList<>();
	private final Map<UUID, Integer> faintBy = new LinkedHashMap<>();

	public QuestSession(int id, QuestDef quest, UUID leader) {
		this.id = id;
		this.quest = quest;
		this.leader = leader;
		this.members.add(leader);
		this.timeLeft = quest.timeMinutes() * 60 * 20;
		this.targets = new UUID[quest.targets().size()];
		this.targetState = new int[quest.targets().size()];
	}

	// =====================================================================
	// メンバー
	// =====================================================================
	public List<ServerPlayer> online(MinecraftServer server) {
		List<ServerPlayer> out = new ArrayList<>();
		for (UUID u : members) {
			ServerPlayer p = server.getPlayerList().getPlayer(u);
			if (p != null) out.add(p);
		}
		return out;
	}

	public boolean isMember(ServerPlayer p) {
		return members.contains(p.getUUID());
	}

	public void broadcastBanner(MinecraftServer server, Payloads.Banner banner) {
		for (ServerPlayer p : online(server)) Net.send(p, banner);
	}

	public void playToMembers(MinecraftServer server, String sound) {
		for (ServerPlayer p : online(server)) {
			Fx.playerSound((ServerLevel) p.level(), p.position(), sound, 1f, 1f);
		}
	}

	// =====================================================================
	// 出発
	// =====================================================================
	public void depart(MinecraftServer server) {
		if (phase != Phase.PREPARING) return;
		phase = Phase.DEPARTING;
		phaseTimer = 60;
		for (ServerPlayer p : online(server)) {
			Net.send(p, new Payloads.Banner(Payloads.Banner.DEPART, "出発！", quest.title() + " — " + quest.field().jaName, 0xFFD060));
			// 出発のジングルはクライアントの BgmManager が DEPART バナーで鳴らす
		}
	}

	private void startHunt(MinecraftServer server) {
		ServerLevel field = server.getLevel(Dims.field(quest.field()));
		if (field == null) {
			QuestManager.INSTANCE.log("フィールドのディメンションが見つかりません: " + quest.field().dimensionPath);
			finish(server, false);
			return;
		}
		layout = FieldLayout.create(field, quest.field(), QuestManager.INSTANCE.allocateSlot(quest.field(), id));
		WorldBuilder.prepareField(field, quest.field(), layout);
		phase = Phase.IN_PROGRESS;
		elapsed = 0;
		for (ServerPlayer p : online(server)) {
			HunterState s = HunterManager.state(p);
			s.questRankDefense = quest.rank().hunterDefense;
			s.resetForQuest();
			QuestManager.INSTANCE.enterHuntDim(p);
			QuestManager.INSTANCE.teleport(p, field, layout.campSpawn(), 0f);
			p.setHealth(p.getMaxHealth());
			Net.send(p, new Payloads.Banner(Payloads.Banner.QUEST_START, "クエスト開始", quest.goalText(), 0xFFE8C060));
			Fx.playerSound(field, layout.campSpawn(), "quest.start", 1.5f, 1f);
		}
		nextSpawnDelay = 20;
	}

	// =====================================================================
	// 毎 tick
	// =====================================================================
	public void tick(MinecraftServer server) {
		members.removeIf(u -> server.getPlayerList().getPlayer(u) == null && phase == Phase.PREPARING);
		if (members.isEmpty()) {
			cleanup(server);
			phase = Phase.ENDED;
			return;
		}
		if (!members.contains(leader)) leader = members.get(0);

		switch (phase) {
			case DEPARTING -> {
				if (--phaseTimer <= 0) startHunt(server);
			}
			case IN_PROGRESS -> tickHunt(server);
			case COMPLETE, FAILED -> {
				if (phase == Phase.COMPLETE && phaseTimer % 20 == 0 && phaseTimer > 0) {
					for (ServerPlayer p : online(server)) {
						p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("集会所へ戻るまで " + (phaseTimer / 20) + " 秒（剥ぎ取りしよう）"));
					}
				}
				if (--phaseTimer <= 0) finish(server, phase == Phase.COMPLETE);
			}
			default -> {
			}
		}
		if (server.getTickCount() % 10 == 0) sync(server);
	}

	private void tickHunt(MinecraftServer server) {
		elapsed++;
		if (--timeLeft <= 0) {
			fail(server, "時間切れ");
			return;
		}
		if (timeLeft == 20 * 60 * 5 || timeLeft == 20 * 60) {
			broadcastBanner(server, new Payloads.Banner(Payloads.Banner.WARNING, "残り時間 " + (timeLeft / 1200) + " 分", "", 0xFF6040));
			playToMembers(server, "quest.timer");
		}
		ServerLevel field = server.getLevel(Dims.field(quest.field()));
		if (field == null) return;
		// 次のターゲットを出現させる
		int active = activeTargetIndex();
		if (active >= 0 && targets[active] == null) {
			if (--nextSpawnDelay <= 0) spawnTarget(server, field, active);
		}
		// 発見判定
		for (int i = 0; i < targets.length; i++) {
			HuntMonster m = monster(field, i);
			if (m == null) continue;
			for (ServerPlayer p : online(server)) {
				if (p.level() == field && p.distanceToSqr(m) < 40 * 40) m.paint();
			}
		}
		// クエスト外に出たメンバー（死亡・ログアウト）を戻す処理は QuestManager 側
	}

	private int activeTargetIndex() {
		for (int i = 0; i < targetState.length; i++) {
			if (targetState[i] == 0) return i;
		}
		return -1;
	}

	private void spawnTarget(MinecraftServer server, ServerLevel field, int index) {
		MonsterKind kind = quest.targets().get(index);
		HuntMonster m = ModEntities.MONSTERS.get(kind).create(field, EntitySpawnReason.TRIGGERED);
		if (m == null) return;
		int startArea = layout.startArea(index);
		Vec3 pos = layout.areaPos(startArea);
		m.setPos(pos.x, pos.y + 1, pos.z);
		m.setupQuest(id, index, quest.rank(), Math.max(1, online(server).size()),
			layout.areaX, layout.areaY, layout.areaZ, startArea, layout.nestArea);
		field.addFreshEntity(m);
		targets[index] = m.getUUID();
		if (index > 0) {
			broadcastBanner(server, new Payloads.Banner(Payloads.Banner.WARNING, "新たな狩猟対象が現れた！",
				kind.fullName() + "（エリア" + (startArea + 1) + "）", 0xFF8040));
			playToMembers(server, "quest.alert");
		}
	}

	public HuntMonster monster(ServerLevel field, int index) {
		if (index < 0 || index >= targets.length || targets[index] == null) return null;
		Entity e = field.getEntity(targets[index]);
		return e instanceof HuntMonster m ? m : null;
	}

	/** 討伐または捕獲された。 */
	public void onMonsterDown(MinecraftServer server, HuntMonster m, boolean captured, ServerPlayer by) {
		int idx = m.targetIndex();
		if (idx < 0 || idx >= targetState.length || targetState[idx] != 0) return;
		boolean okGoal = switch (quest.goal()) {
			case SLAY -> !captured;
			case CAPTURE -> captured;
			case HUNT -> true;
		};
		targetState[idx] = captured ? 2 : 1;
		rewardLines.add(m.kind.jaName + (captured ? " を捕獲" : " を討伐"));
		if (captured) rewardLines.add(m.kind.jaName + "の討伐証（捕獲報酬）");
		if (!okGoal) {
			fail(server, quest.goal() == QuestDef.Goal.CAPTURE ? "捕獲対象を討伐してしまった" : "討伐対象を捕獲してしまった");
			return;
		}
		if (activeTargetIndex() < 0) {
			phase = Phase.COMPLETE;
			phaseTimer = 20 * 25;
			for (ServerPlayer p : online(server)) {
				Net.send(p, new Payloads.Banner(Payloads.Banner.QUEST_CLEAR, "クエストクリア！",
					"クリアタイム " + formatTime(elapsed), 0xFFFFD040));
				HunterManager.startMove(p, MoveRegistry.VICTORY);
			}
		} else {
			nextSpawnDelay = 20 * 12;
			broadcastBanner(server, new Payloads.Banner(Payloads.Banner.INFO, "目標 " + (idx + 1) + "/" + targets.length + " 達成！",
				"次の狩猟対象が近づいている…", 0xFFE060));
		}
	}

	public void recordPartBreak(MonsterKind kind, MonsterKind.Part part) {
		rewardLines.add("部位破壊：" + kind.jaName + "の" + part.jaName() + "（+300z）");
	}

	public void recordCarve(ServerPlayer player, MonsterKind kind) {
		// 剥ぎ取り品はその場でインベントリに入る
	}

	/** 力尽きた。 */
	public void onFaint(MinecraftServer server, ServerPlayer p) {
		if (phase != Phase.IN_PROGRESS) {
			p.setHealth(p.getMaxHealth());
			return;
		}
		faints++;
		faintBy.merge(p.getUUID(), 1, Integer::sum);
		broadcastBanner(server, new Payloads.Banner(Payloads.Banner.FAINT, p.getName().getString() + "が力尽きました",
			"力尽き " + faints + "/" + maxFaints, 0xFF6060));
		Fx.playerSound((ServerLevel) p.level(), p.position(), "player.faint", 1f, 1f);
		HunterState s = HunterManager.state(p);
		s.clearAilments();
		s.redHealth = 0;
		s.stamina = s.maxStamina;
		p.setHealth(p.getMaxHealth());
		if (faints >= maxFaints) {
			fail(server, "力尽きた回数が上限に達した");
			return;
		}
		ServerLevel field = server.getLevel(Dims.field(quest.field()));
		if (field != null && layout != null) {
			QuestManager.INSTANCE.teleport(p, field, layout.campSpawn(), 0f);
			Fx.playerSound(field, layout.campSpawn(), "player.cart", 1f, 1f);
		}
	}

	public void fail(MinecraftServer server, String reason) {
		if (phase == Phase.FAILED || phase == Phase.ENDED) return;
		phase = Phase.FAILED;
		phaseTimer = 20 * 8;
		broadcastBanner(server, new Payloads.Banner(Payloads.Banner.QUEST_FAILED, "クエスト失敗…", reason, 0xFFE04040));
	}

	/** 結果を配り、集会所へ戻す。 */
	public void finish(MinecraftServer server, boolean success) {
		for (ServerPlayer p : online(server)) {
			HunterProfile pr = ModAttachments.profile(p);
			int hrBefore = HunterRank.hr(pr.hrPoints());
			int points = success ? quest.rewardPoints() : 0;
			int coins = success ? quest.rewardCoins() : 0;
			List<String> lines = new ArrayList<>(rewardLines);
			if (success) {
				int breaks = (int) rewardLines.stream().filter(l -> l.startsWith("部位破壊")).count();
				coins += breaks * 300;
				if (faintBy.getOrDefault(p.getUUID(), 0) == 0) {
					coins += 500;
					lines.add("一度も力尽きずにクリア！（+500z）");
				}
			}
			HunterProfile np = pr.withPoints(pr.hrPoints() + points, pr.coins() + coins);
			if (success) np = np.withCleared(quest.index());
			ModAttachments.set(p, np);
			int hrAfter = HunterRank.hr(np.hrPoints());
			Net.send(p, new Payloads.Result(quest.index(), success, points, coins, hrBefore, hrAfter, elapsed, lines));
			HunterState s = HunterManager.state(p);
			s.meal = Meal.NONE;
			s.mealEaten = false;
			s.questRankDefense = 80;
			s.clearAilments();
			QuestManager.INSTANCE.returnToHub(p);
		}
		cleanup(server);
		phase = Phase.ENDED;
	}

	private void cleanup(MinecraftServer server) {
		ServerLevel field = server.getLevel(Dims.field(quest.field()));
		if (field != null) {
			for (int i = 0; i < targets.length; i++) {
				HuntMonster m = monster(field, i);
				if (m != null) m.discard();
			}
		}
		QuestManager.INSTANCE.releaseSlot(quest.field(), id);
	}

	/** クライアントへクエスト情報を送る。 */
	public void sync(MinecraftServer server) {
		List<Payloads.QuestSync.Member> ms = new ArrayList<>();
		for (ServerPlayer p : online(server)) {
			ms.add(new Payloads.QuestSync.Member(p.getName().getString(), p.getHealth(), p.getMaxHealth(),
				HunterManager.state(p).weapon.ordinal(), (int) p.getX(), (int) p.getZ(), false));
		}
		List<Payloads.QuestSync.MonsterInfo> mons = new ArrayList<>();
		ServerLevel field = server.getLevel(Dims.field(quest.field()));
		int battle = -1;
		for (int i = 0; i < targets.length; i++) {
			MonsterKind kind = quest.targets().get(i);
			HuntMonster m = field == null ? null : monster(field, i);
			int flags = 0;
			int x = 0, z = 0, area = 0;
			float hp = 1f;
			if (targetState[i] == 1) flags |= Payloads.QuestSync.MonsterInfo.DEAD | Payloads.QuestSync.MonsterInfo.DISCOVERED;
			if (targetState[i] == 2) flags |= Payloads.QuestSync.MonsterInfo.CAPTURED | Payloads.QuestSync.MonsterInfo.DISCOVERED;
			if (m != null) {
				flags |= Payloads.QuestSync.MonsterInfo.ACTIVE;
				if (m.isDiscovered()) flags |= Payloads.QuestSync.MonsterInfo.DISCOVERED;
				if (m.isPainted()) flags |= Payloads.QuestSync.MonsterInfo.PAINTED;
				if (m.isCapturable()) flags |= Payloads.QuestSync.MonsterInfo.CAPTURABLE;
				if (m.isEnraged()) flags |= Payloads.QuestSync.MonsterInfo.ENRAGED;
				if (m.isLimping()) flags |= Payloads.QuestSync.MonsterInfo.LIMPING;
				if (m.state() == HuntMonster.State.NEST_SLEEP || m.state() == HuntMonster.State.STATUS_SLEEP) {
					flags |= Payloads.QuestSync.MonsterInfo.ASLEEP;
				}
				x = (int) m.getX();
				z = (int) m.getZ();
				area = m.currentArea();
				hp = m.hpRatio();
				if (!m.isDead() && !m.isCaptured() && m.state() != HuntMonster.State.IDLE
					&& m.state() != HuntMonster.State.NEST_SLEEP && m.isDiscovered()) {
					for (ServerPlayer p : online(server)) {
						if (p.level() == field && p.distanceToSqr(m) < 48 * 48) {
							battle = kind.ordinal();
							break;
						}
					}
				}
			}
			mons.add(new Payloads.QuestSync.MonsterInfo(m == null ? -1 : m.getId(), kind.ordinal(), x, z, area, flags, hp));
		}
		int state = switch (phase) {
			case PREPARING, DEPARTING -> Payloads.QuestSync.PREPARING;
			case IN_PROGRESS -> Payloads.QuestSync.IN_PROGRESS;
			case COMPLETE -> Payloads.QuestSync.COMPLETE;
			case FAILED -> Payloads.QuestSync.FAILED;
			case ENDED -> Payloads.QuestSync.NONE;
		};
		int[] ax = layout == null ? new int[0] : layout.areaX;
		int[] az = layout == null ? new int[0] : layout.areaZ;
		int cx = layout == null ? 0 : layout.campX;
		int cz = layout == null ? 0 : layout.campZ;
		Payloads.QuestSync sync = new Payloads.QuestSync(id, quest.index(), state, timeLeft, faints, maxFaints,
			quest.field().ordinal(), cx, cz, ax, az, ms, mons, battle);
		for (ServerPlayer p : online(server)) Net.send(p, sync);
	}

	public static String formatTime(int ticks) {
		int sec = ticks / 20;
		return String.format("%02d'%02d\"", sec / 60, sec % 60);
	}

	public Payloads.HubSync.SessionInfo info(MinecraftServer server) {
		List<String> names = new ArrayList<>();
		for (ServerPlayer p : online(server)) names.add(p.getName().getString());
		ServerPlayer l = server.getPlayerList().getPlayer(leader);
		int st = phase == Phase.PREPARING ? Payloads.QuestSync.PREPARING : Payloads.QuestSync.IN_PROGRESS;
		return new Payloads.HubSync.SessionInfo(id, quest.index(), l == null ? "?" : l.getName().getString(), names, st);
	}
}
