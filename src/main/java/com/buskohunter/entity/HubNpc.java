package com.buskohunter.entity;

import com.buskohunter.block.HunterStationBlock;
import com.buskohunter.quest.QuestManager;
import com.buskohunter.util.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 集会所の NPC（受付嬢・料理長・鍛冶屋など）。右クリックで話しかけると、役目の画面を開く。
 * 倒せない・動かない・ディスクには保存しない（集会所に人がいる間、HubPopulator が配置し直す）。
 */
public class HubNpc extends PathfinderMob {
	public enum Role {
		RECEPTIONIST("receptionist", "受付嬢 ミナ", 0xFFE080, HunterStationBlock.Station.QUEST_BOARD, new String[]{
			"ようこそ、ブスコハンターズギルドへ！ クエストのご案内ですね♪",
			"今日はどのモンスターを狩りに行きますか？",
			"仲間と同じクエストに参加すれば、みんなで出発できますよ！"}),
		GUILD_MASTER("guild_master", "ギルドマスター", 0xE0C0FF, HunterStationBlock.Station.QUEST_BOARD, new String[]{
			"おう、ハンター。腕は鈍っておらんか？",
			"モンスターは弱ると足を引きずって巣へ逃げる。そこを追い詰めるんじゃ。",
			"捕獲なら、罠と捕獲用麻酔玉 2 発を忘れるでないぞ。"}),
		COOK("cook", "料理長 ブスコ", 0xFFB070, HunterStationBlock.Station.CANTEEN, new String[]{
			"腹が減っては狩りはできぬ！ 食っていけ！",
			"食事で体力とスタミナが増えるぞ。出発前に一杯どうだ！"}),
		SMITH("smith", "鍛冶屋 ゴウ", 0xFF9060, HunterStationBlock.Station.EQUIPMENT_BOX, new String[]{
			"武器と防具の用意は任せな。",
			"属性は相手の弱点を狙え。火に弱い奴には火の武器だ！",
			"14 種類の武器、どれも使いこなせば一流よ。"}),
		CLERK("clerk", "アイテム係 ポポ", 0x90FF90, HunterStationBlock.Station.ITEM_BOX, new String[]{
			"回復薬に罠に砥石… 必要な道具はここで受け取ってね！",
			"道具はホットバーで持ち替えて、右クリックで使うんだよ。"}),
		GATEKEEPER("gatekeeper", "門番 ガルド", 0x80C0FF, HunterStationBlock.Station.DEPARTURE_GATE, new String[]{
			"準備はいいか？ クエストを受けたら、ここから出発だ。",
			"リーダーが出発すると、パーティ全員がフィールドへ向かうぞ。"}),
		INSTRUCTOR("instructor", "訓練教官", 0xFF7070, null, new String[]{
			"訓練場の人形で技を試してみろ！ ダメージの数字が見えるぞ。",
			"回避は Shift か、ジャンプを素早く 2 回だ。",
			"盾のある武器は、右クリック長押しでガードできる。",
			"右クリックを押したまま左や R で、武器ごとの派生技が出せるぞ。",
			"H キーのハンターメニューで、武器ごとの操作を確認できる。"}),
		ROOKIE("rookie", "見習いハンター", 0xC0E0C0, null, new String[]{
			"いつかブスコバリーノ王を狩ってみたいなぁ…",
			"サケランの酒瓶には気をつけて！ 酔うとふらふらになるんだ。",
			"ジャングリアの森は広いから、ペイントボールを当てておくといいよ。"}),
		BARD("bard", "吟遊詩人", 0xFFC0E0, null, new String[]{
			"♪ 桜舞う天上の郷に〜 王は降り立つ〜 ♪",
			"電脳廃都には、ネオンに光る魔物が出るらしい…",
			"ブスコバの歌を知っているかい？ …いや、知らない方がいい。"});

		public final String id;
		public final String jaName;
		public final int color;
		public final HunterStationBlock.Station station;
		public final String[] lines;

		Role(String id, String jaName, int color, HunterStationBlock.Station station, String[] lines) {
			this.id = id;
			this.jaName = jaName;
			this.color = color;
			this.station = station;
			this.lines = lines;
		}

		public static Role byOrdinal(int i) {
			Role[] v = values();
			return i >= 0 && i < v.length ? v[i] : RECEPTIONIST;
		}
	}

	private static final EntityDataAccessor<Integer> DATA_ROLE = SynchedEntityData.defineId(HubNpc.class, EntityDataSerializers.INT);
	private final Map<UUID, Integer> talkIndex = new HashMap<>();
	private final Map<UUID, Long> lastTalk = new HashMap<>();
	/** 立ち位置（HubPopulator が決める）。 */
	public double homeX, homeY, homeZ;
	public float homeYaw;

	public HubNpc(EntityType<? extends HubNpc> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setInvulnerable(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f, 0.9f));
		goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ROLE, 0);
	}

	public Role role() {
		return Role.byOrdinal(entityData.get(DATA_ROLE));
	}

	public void setRole(Role role) {
		entityData.set(DATA_ROLE, role.ordinal());
		setCustomName(Component.literal(role.jaName).withColor(role.color));
		setCustomNameVisible(true);
	}

	@Override
	public void tick() {
		super.tick();
		// 押されたりしても持ち場へ戻る
		if (!level().isClientSide() && (homeX != 0 || homeZ != 0) && distanceToSqr(homeX, homeY, homeZ) > 0.25) {
			setPos(homeX, homeY, homeZ);
			setDeltaMovement(0, 0, 0);
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (!level().isClientSide() && player instanceof ServerPlayer sp) {
			Role r = role();
			long now = level().getGameTime();
			Long last = lastTalk.get(sp.getUUID());
			// 続けて右クリックした時はセリフを出さずに画面だけ開く
			if (last == null || now - last > 60 || r.station == null) {
				int i = talkIndex.getOrDefault(sp.getUUID(), 0);
				sp.sendSystemMessage(Component.literal("[" + r.jaName + "] ").withColor(r.color)
					.append(Component.literal(r.lines[i % r.lines.length]).withStyle(ChatFormatting.WHITE)));
				talkIndex.put(sp.getUUID(), i + 1);
			}
			lastTalk.put(sp.getUUID(), now);
			Fx.playerSound((ServerLevel) level(), position(), "ui.open", 0.6f, 1.2f);
			lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, sp.getEyePosition());
			if (r.station != null) QuestManager.get(sp).useStation(sp, r.station);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putInt("bh_role", role().ordinal());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		setRole(Role.byOrdinal(in.getIntOr("bh_role", 0)));
	}
}
