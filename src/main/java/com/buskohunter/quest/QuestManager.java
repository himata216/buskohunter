package com.buskohunter.quest;

import com.buskohunter.net.Net;
import com.buskohunter.BuskoHunter;
import com.buskohunter.block.HunterStationBlock;
import com.buskohunter.core.ArmorSeries;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.FieldType;
import com.buskohunter.core.HuntItem;
import com.buskohunter.core.HunterRank;
import com.buskohunter.core.Meal;
import com.buskohunter.core.QuestDef;
import com.buskohunter.core.WeaponType;
import com.buskohunter.hunter.HunterManager;
import com.buskohunter.hunter.HunterProfile;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.item.HuntItemItem;
import com.buskohunter.item.HunterWeaponItem;
import com.buskohunter.net.Payloads;
import com.buskohunter.registry.ModAttachments;
import com.buskohunter.registry.ModItems;
import com.buskohunter.util.Fx;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** クエストの受注・参加・出発・帰還・設備の利用をまとめて管理する。 */
public final class QuestManager {
	public static final QuestManager INSTANCE = new QuestManager();

	private final Map<Integer, QuestSession> sessions = new HashMap<>();
	private int nextId = 1;
	private final Map<FieldType, Map<Integer, Integer>> slots = new EnumMap<>(FieldType.class);
	private final Map<FieldType, Integer> slotCursor = new EnumMap<>(FieldType.class);
	/** 集会所に入る前にサバイバルだったプレイヤー（出る時に戻す）。 */
	private final Set<UUID> wasSurvival = new HashSet<>();
	private final Set<UUID> suppliesTaken = new HashSet<>();

	private QuestManager() {
	}

	public static QuestManager get(ServerPlayer player) {
		return INSTANCE;
	}

	public static QuestSession find(MinecraftServer server, int id) {
		return id < 0 ? null : INSTANCE.sessions.get(id);
	}

	public QuestSession sessionOf(ServerPlayer p) {
		for (QuestSession s : sessions.values()) {
			if (s.isMember(p) && s.phase != QuestSession.Phase.ENDED) return s;
		}
		return null;
	}

	public static boolean inActiveQuest(ServerPlayer p) {
		QuestSession s = INSTANCE.sessionOf(p);
		return s != null && (s.phase == QuestSession.Phase.IN_PROGRESS || s.phase == QuestSession.Phase.COMPLETE
			|| s.phase == QuestSession.Phase.FAILED) && Dims.isField(p.level());
	}

	public static boolean inHuntingDimension(ServerPlayer p) {
		return Dims.isField(p.level());
	}

	void log(String msg) {
		BuskoHunter.LOGGER.warn(msg);
	}

	// =====================================================================
	// 毎 tick
	// =====================================================================
	public void tick(MinecraftServer server) {
		for (QuestSession s : new ArrayList<>(sessions.values())) {
			s.tick(server);
			if (s.phase == QuestSession.Phase.ENDED) sessions.remove(s.id);
		}
		if (server.getTickCount() % 40 == 0) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				// クエストに属さずフィールドにいるプレイヤーは集会所へ戻す
				if (Dims.isField(p.level()) && !p.isCreative() && !p.isSpectator()) {
					QuestSession s = sessionOf(p);
					if (s == null || s.phase == QuestSession.Phase.PREPARING) returnToHub(p);
				}
				if (Dims.isHub(p.level())) {
					WorldBuilder.ensureHub((ServerLevel) p.level());
				}
			}
		}
	}

	// =====================================================================
	// スロット（フィールドの区画）
	// =====================================================================
	int allocateSlot(FieldType field, int sessionId) {
		Map<Integer, Integer> used = slots.computeIfAbsent(field, f -> new HashMap<>());
		int cursor = slotCursor.getOrDefault(field, 0);
		for (int i = 0; i < 8; i++) {
			int slot = (cursor + i) % 8;
			if (!used.containsKey(slot)) {
				used.put(slot, sessionId);
				slotCursor.put(field, slot + 1);
				return slot;
			}
		}
		return cursor % 8;
	}

	void releaseSlot(FieldType field, int sessionId) {
		Map<Integer, Integer> used = slots.get(field);
		if (used != null) used.values().removeIf(v -> v == sessionId);
	}

	// =====================================================================
	// 移動
	// =====================================================================
	public void teleport(ServerPlayer p, ServerLevel level, Vec3 pos, float yaw) {
		p.teleport(new TeleportTransition(level, pos, Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING));
	}

	public void toggleHub(ServerPlayer p) {
		if (Dims.isHub(p.level())) leaveHub(p);
		else if (!Dims.isField(p.level())) goHub(p);
		else p.sendOverlayMessage(Component.literal("クエスト中は使えません（H メニューからリタイアできます）"));
	}

	public void goHub(ServerPlayer p) {
		if (Dims.isOurs(p.level())) {
			if (Dims.isField(p.level())) return;
			teleport(p, Dims.hub(p.level().getServer()), WorldBuilder.hubSpawn(Dims.hub(p.level().getServer())), 180f);
			return;
		}
		HunterProfile pr = ModAttachments.profile(p);
		ModAttachments.set(p, pr.withReturn(p.level().dimension().identifier().toString(), (int) p.getX(), (int) p.getY(), (int) p.getZ()));
		enterHuntDim(p);
		ServerLevel hub = Dims.hub(p.level().getServer());
		WorldBuilder.ensureHub(hub);
		teleport(p, hub, WorldBuilder.hubSpawn(hub), 180f);
		p.sendSystemMessage(Component.literal("§6ブスコハンター集会所へようこそ！§r クエストカウンターでクエストを受注しよう。H キーでヘルプ。"));
		syncHub(p);
	}

	public void leaveHub(ServerPlayer p) {
		QuestSession s = sessionOf(p);
		if (s != null && s.phase == QuestSession.Phase.PREPARING) leaveSession(p);
		HunterProfile pr = ModAttachments.profile(p);
		MinecraftServer server = p.level().getServer();
		ServerLevel dest = server.overworld();
		Vec3 pos;
		if (!pr.returnDim().isEmpty()) {
			Identifier id = Identifier.tryParse(pr.returnDim());
			ServerLevel l = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
			if (l != null && !Dims.isOurs(l)) dest = l;
			pos = new Vec3(pr.returnX() + 0.5, pr.returnY(), pr.returnZ() + 0.5);
		} else {
			int y = FieldLayout.surface(server.overworld(), 0, 0);
			pos = new Vec3(0.5, y + 1, 0.5);
		}
		if (wasSurvival.remove(p.getUUID())) p.setGameMode(GameType.SURVIVAL);
		teleport(p, dest, pos, p.getYRot());
	}

	/** 集会所・フィールドに入る時：サバイバルならアドベンチャーにする（ブロックを壊せない）。 */
	void enterHuntDim(ServerPlayer p) {
		if (p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) {
			wasSurvival.add(p.getUUID());
			p.setGameMode(GameType.ADVENTURE);
		}
	}

	public void returnToHub(ServerPlayer p) {
		ServerLevel hub = Dims.hub(p.level().getServer());
		WorldBuilder.ensureHub(hub);
		teleport(p, hub, WorldBuilder.hubSpawn(hub), 180f);
		syncHub(p);
	}

	public void sendToCamp(ServerPlayer p) {
		QuestSession s = sessionOf(p);
		if (s == null || s.layout == null) return;
		teleport(p, (ServerLevel) p.level(), s.layout.campSpawn(), p.getYRot());
	}

	// =====================================================================
	// 設備
	// =====================================================================
	public void useStation(ServerPlayer p, HunterStationBlock.Station station) {
		switch (station) {
			case QUEST_BOARD -> open(p, Payloads.OpenScreen.QUEST_BOARD);
			case EQUIPMENT_BOX -> open(p, Payloads.OpenScreen.EQUIPMENT);
			case CANTEEN -> open(p, Payloads.OpenScreen.CANTEEN);
			case ITEM_BOX -> open(p, Payloads.OpenScreen.ITEM_BOX);
			case DEPARTURE_GATE -> depart(p);
			case SUPPLY_BOX -> supplyBox(p);
			case HUNTER_BED -> {
				p.setHealth(p.getMaxHealth());
				HunterState s = HunterManager.state(p);
				s.stamina = s.maxStamina;
				s.clearAilments();
				Fx.playerSound((ServerLevel) p.level(), p.position(), "player.heal", 1f, 1f);
				p.sendOverlayMessage(Component.literal("ひと休みして体力が回復した"));
			}
			case EXIT_GATE -> leaveHub(p);
		}
	}

	private void open(ServerPlayer p, int screen) {
		syncHub(p);
		Net.send(p, new Payloads.OpenScreen(screen));
	}

	private void supplyBox(ServerPlayer p) {
		QuestSession s = sessionOf(p);
		if (s == null || s.phase != QuestSession.Phase.IN_PROGRESS) {
			p.sendOverlayMessage(Component.literal("支給品はクエスト中に受け取れます"));
			return;
		}
		UUID key = new UUID(s.id, p.getUUID().getLeastSignificantBits());
		if (!suppliesTaken.add(key)) {
			p.sendOverlayMessage(Component.literal("支給品はもう受け取った"));
			return;
		}
		give(p, HuntItem.POTION, 2);
		give(p, HuntItem.RATION, 2);
		give(p, HuntItem.PAINTBALL, 2);
		if (s.quest.goal() == QuestDef.Goal.CAPTURE) {
			give(p, HuntItem.SHOCK_TRAP, 1);
			give(p, HuntItem.TRANQ_BOMB, 2);
		}
		Fx.playerSound((ServerLevel) p.level(), p.position(), "ui.reward", 1f, 1f);
		p.sendOverlayMessage(Component.literal("支給品を受け取った"));
	}

	private static void give(ServerPlayer p, HuntItem item, int count) {
		ItemStack st = new ItemStack(ModItems.HUNT_ITEMS.get(item), count);
		if (!p.getInventory().add(st)) {
			p.sendOverlayMessage(Component.literal("持ち物がいっぱいです"));
		}
	}

	// =====================================================================
	// メニュー操作
	// =====================================================================
	public void handleAction(ServerPlayer p, Payloads.Action a) {
		MinecraftServer server = p.level().getServer();
		switch (a.action()) {
			case Payloads.Action.ACCEPT_QUEST -> acceptQuest(p, a.a());
			case Payloads.Action.JOIN_SESSION -> joinSession(p, a.a());
			case Payloads.Action.LEAVE_SESSION -> leaveSession(p);
			case Payloads.Action.DEPART -> depart(p);
			case Payloads.Action.SET_WEAPON -> setWeapon(p, WeaponType.byOrdinal(a.a()), ElementType.byOrdinal(a.b()));
			case Payloads.Action.SET_ARMOR -> setArmor(p, ArmorSeries.byOrdinal(a.a()));
			case Payloads.Action.EAT_MEAL -> eat(p, Meal.byOrdinal(a.a()));
			case Payloads.Action.SET_CARRY -> {
				HuntItem item = HuntItem.byOrdinal(a.a());
				ModAttachments.set(p, ModAttachments.profile(p).withCarry(a.a(), Math.max(0, Math.min(item.maxCarry, a.b()))));
			}
			case Payloads.Action.TAKE_SUPPLIES -> takeSupplies(p);
			case Payloads.Action.RETIRE -> retire(p);
			case Payloads.Action.GO_HUB -> goHub(p);
			case Payloads.Action.LEAVE_HUB -> {
				if (Dims.isHub(p.level())) leaveHub(p);
			}
			case Payloads.Action.REQUEST_HUB_SYNC -> syncHub(p);
			case Payloads.Action.SUPPLY_BOX -> supplyBox(p);
			default -> {
			}
		}
		if (a.action() != Payloads.Action.REQUEST_HUB_SYNC) {
			for (ServerPlayer o : server.getPlayerList().getPlayers()) {
				if (Dims.isHub(o.level())) syncHub(o);
			}
		}
	}

	private void acceptQuest(ServerPlayer p, int questIndex) {
		if (!Dims.isHub(p.level())) {
			p.sendOverlayMessage(Component.literal("クエストは集会所で受注してください"));
			return;
		}
		QuestDef q = QuestDef.byIndex(questIndex);
		HunterProfile pr = ModAttachments.profile(p);
		if (!HunterRank.canAccept(pr.hrPoints(), q) && !p.isCreative()) {
			p.sendOverlayMessage(Component.literal("HR が足りません"));
			return;
		}
		if (sessionOf(p) != null) leaveSession(p);
		QuestSession s = new QuestSession(nextId++, q, p.getUUID());
		sessions.put(s.id, s);
		Fx.playerSound((ServerLevel) p.level(), p.position(), "ui.accept", 1f, 1f);
		for (ServerPlayer o : p.level().getServer().getPlayerList().getPlayers()) {
			if (Dims.isHub(o.level())) {
				Net.send(o, new Payloads.Banner(Payloads.Banner.SMALL,
					p.getName().getString() + " がクエストを受注した", q.title() + "（クエストカウンターから参加できます）", 0xFFE8C060));
			}
		}
	}

	private void joinSession(ServerPlayer p, int sessionId) {
		QuestSession s = sessions.get(sessionId);
		if (s == null || s.phase != QuestSession.Phase.PREPARING) return;
		if (s.members.size() >= 4) {
			p.sendOverlayMessage(Component.literal("満員です（最大4人）"));
			return;
		}
		QuestSession cur = sessionOf(p);
		if (cur != null) leaveSession(p);
		s.members.add(p.getUUID());
		s.broadcastBanner(p.level().getServer(), new Payloads.Banner(Payloads.Banner.SMALL,
			p.getName().getString() + " がパーティに参加した", "", 0x80FF80));
	}

	private void leaveSession(ServerPlayer p) {
		QuestSession s = sessionOf(p);
		if (s == null) return;
		s.members.remove(p.getUUID());
	}

	private void depart(ServerPlayer p) {
		QuestSession s = sessionOf(p);
		if (s == null) {
			p.sendOverlayMessage(Component.literal("先にクエストカウンターでクエストを受注してください"));
			return;
		}
		if (!s.leader.equals(p.getUUID())) {
			p.sendOverlayMessage(Component.literal("出発はリーダーが行います"));
			return;
		}
		if (s.phase != QuestSession.Phase.PREPARING) return;
		// 出発するメンバーは集会所にいる人だけ
		s.members.removeIf(u -> {
			ServerPlayer m = p.level().getServer().getPlayerList().getPlayer(u);
			return m == null || !Dims.isHub(m.level());
		});
		s.depart(p.level().getServer());
	}

	private void retire(ServerPlayer p) {
		QuestSession s = sessionOf(p);
		if (s == null) return;
		s.members.remove(p.getUUID());
		p.sendSystemMessage(Component.literal("クエストをリタイアしました"));
		HunterState st = HunterManager.state(p);
		st.meal = Meal.NONE;
		st.mealEaten = false;
		returnToHub(p);
	}

	private void eat(ServerPlayer p, Meal meal) {
		HunterState s = HunterManager.state(p);
		if (s.mealEaten) {
			p.sendOverlayMessage(Component.literal("もうお腹いっぱいだ（次のクエストまで）"));
			return;
		}
		s.meal = meal;
		s.mealEaten = true;
		s.maxStamina = 100 + meal.staminaBonus;
		Fx.playerSound((ServerLevel) p.level(), p.position(), "player.eat", 1f, 1f);
		Net.send(p, new Payloads.Banner(Payloads.Banner.INFO, meal.jaName + " を食べた！",
			meal.describe() + " / " + meal.skill.jaName, meal.color));
	}

	/** 武器を選ぶ：手持ちのハンター武器を入れ替える。 */
	private void setWeapon(ServerPlayer p, WeaponType type, ElementType element) {
		if (element == ElementType.NONE) element = ElementType.FIRE;
		ModAttachments.set(p, ModAttachments.profile(p).withLoadout(type.ordinal(), element.ordinal()));
		Inventory inv = p.getInventory();
		int slot = -1;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).getItem() instanceof HunterWeaponItem) {
				if (slot < 0) slot = i;
				inv.setItem(i, ItemStack.EMPTY);
			}
		}
		ItemStack weapon = ModItems.WEAPONS.get(type).create(element);
		if (slot < 0) slot = inv.getSelectedSlot();
		if (!inv.getItem(slot).isEmpty()) {
			if (!inv.add(weapon)) p.sendOverlayMessage(Component.literal("持ち物がいっぱいです"));
		} else {
			inv.setItem(slot, weapon);
		}
		HunterManager.refreshLoadout(p);
		Fx.playerSound((ServerLevel) p.level(), p.position(), "weapon.morph", 0.7f, 1f);
		p.sendOverlayMessage(Component.literal(com.buskohunter.core.WeaponStats.of(type, element).name() + " を装備した"));
	}

	private void setArmor(ServerPlayer p, ArmorSeries series) {
		ModAttachments.set(p, ModAttachments.profile(p).withArmor(series.ordinal()));
		var pieces = ModItems.ARMOR.get(series);
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) {
			ItemStack old = p.getItemBySlot(slots[i]);
			boolean ours = false;
			for (var arr : ModItems.ARMOR.values()) {
				for (var it : arr) if (old.is(it)) ours = true;
			}
			if (!old.isEmpty() && !ours) {
				if (!p.getInventory().add(old.copy())) continue;
			}
			p.setItemSlot(slots[i], new ItemStack(pieces[i]));
		}
		HunterManager.refreshLoadout(p);
	}

	/** アイテムボックスから狩猟アイテムを受け取る（前回の残りは回収）。 */
	private void takeSupplies(ServerPlayer p) {
		Inventory inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).getItem() instanceof HuntItemItem) inv.setItem(i, ItemStack.EMPTY);
		}
		HunterProfile pr = ModAttachments.profile(p);
		for (HuntItem item : HuntItem.values()) {
			int n = pr.carryOf(item.ordinal(), defaultCarry(item));
			if (n > 0) give(p, item, n);
		}
		Fx.playerSound((ServerLevel) p.level(), p.position(), "ui.reward", 1f, 1f);
	}

	public static int defaultCarry(HuntItem item) {
		return switch (item) {
			case POTION -> 10;
			case MEGA_POTION -> 5;
			case WHETSTONE -> 20;
			case RATION -> 5;
			case PAINTBALL -> 5;
			case FLASH_BOMB -> 3;
			case SHOCK_TRAP, PITFALL_TRAP -> 1;
			case TRANQ_BOMB -> 2;
			case FARCASTER -> 1;
			default -> 0;
		};
	}

	public void syncHub(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		List<Payloads.HubSync.SessionInfo> infos = new ArrayList<>();
		for (QuestSession s : sessions.values()) {
			if (s.phase == QuestSession.Phase.PREPARING || s.phase == QuestSession.Phase.IN_PROGRESS) infos.add(s.info(server));
		}
		QuestSession mine = sessionOf(p);
		HunterProfile pr = ModAttachments.profile(p);
		HunterState st = HunterManager.state(p);
		int[] carry = new int[HuntItem.values().length];
		for (HuntItem item : HuntItem.values()) carry[item.ordinal()] = pr.carryOf(item.ordinal(), defaultCarry(item));
		Net.send(p, new Payloads.HubSync(infos, mine == null ? -1 : mine.id, pr.hrPoints(), pr.coins(),
			pr.cleared(), pr.weapon(), pr.element(), pr.armor(), st.meal.ordinal(), carry, st.mealEaten, pr.hunts()));
	}

	// =====================================================================
	// プレイヤーのイベント
	// =====================================================================
	/** 体力 0：クエスト中なら「力尽きた」としてキャンプへ。true = 死亡をキャンセル。 */
	public boolean onLethal(ServerPlayer p) {
		if (!Dims.isOurs(p.level())) return false;
		QuestSession s = sessionOf(p);
		if (s != null && Dims.isField(p.level())) {
			s.onFaint(p.level().getServer(), p);
			return true;
		}
		p.setHealth(p.getMaxHealth());
		if (Dims.isField(p.level())) returnToHub(p);
		return true;
	}

	public void onJoin(ServerPlayer p) {
		HunterProfile pr = ModAttachments.profile(p);
		if (!pr.bookGiven()) {
			p.getInventory().add(new ItemStack(ModItems.HUNTER_BOOK));
			ModAttachments.set(p, pr.withBookGiven());
			p.sendSystemMessage(Component.literal("§6[ブスコハンター]§r ハンターノートを右クリックすると集会所へ行けます。"));
		}
		if (Dims.isHub(p.level())) syncHub(p);
	}

	public void onLeave(ServerPlayer p) {
		QuestSession s = sessionOf(p);
		if (s != null && s.phase == QuestSession.Phase.PREPARING) s.members.remove(p.getUUID());
		HunterManager.remove(p);
	}

	public void clear() {
		sessions.clear();
		slots.clear();
		wasSurvival.clear();
		suppliesTaken.clear();
	}
}
