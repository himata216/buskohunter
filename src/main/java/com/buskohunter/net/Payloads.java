package com.buskohunter.net;

import com.buskohunter.BuskoHunter;
import com.buskohunter.hunter.HunterState;
import com.buskohunter.core.ArmorSeries;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.Meal;
import com.buskohunter.core.WeaponType;
import com.buskohunter.combat.MoveRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** 全カスタムパケット。 */
public final class Payloads {
	private Payloads() {
	}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
		return new CustomPacketPayload.Type<>(BuskoHunter.id(path));
	}

	// =====================================================================
	// クライアント → サーバー
	// =====================================================================

	/** ボタン入力（押しっぱなし・押した瞬間・回避方向）。 */
	public record Input(int bits) implements CustomPacketPayload {
		public static final Type<Input> TYPE = payloadType("input");
		public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC =
			CustomPacketPayload.codec((p, buf) -> buf.writeVarInt(p.bits), buf -> new Input(buf.readVarInt()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** メニュー操作（クエスト受注・装備変更など）。 */
	public record Action(int action, int a, int b) implements CustomPacketPayload {
		public static final Type<Action> TYPE = payloadType("action");
		public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = CustomPacketPayload.codec(
			(p, buf) -> {
				buf.writeVarInt(p.action);
				buf.writeVarInt(p.a);
				buf.writeVarInt(p.b);
			},
			buf -> new Action(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

		public static final int ACCEPT_QUEST = 1;
		public static final int JOIN_SESSION = 2;
		public static final int LEAVE_SESSION = 3;
		public static final int DEPART = 4;
		public static final int SET_WEAPON = 5;
		public static final int SET_ARMOR = 6;
		public static final int EAT_MEAL = 7;
		public static final int SET_CARRY = 8;
		public static final int TAKE_SUPPLIES = 9;
		public static final int RETIRE = 10;
		public static final int GO_HUB = 11;
		public static final int LEAVE_HUB = 12;
		public static final int REQUEST_HUB_SYNC = 13;
		public static final int SUPPLY_BOX = 14;
		public static final int CARVE = 15;

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// =====================================================================
	// サーバー → クライアント
	// =====================================================================

	/** 自分のハンター状態（スタミナ・切れ味・武器ゲージ・やられ状態）。 */
	public record HunterSync(byte[] data) implements CustomPacketPayload {
		public static final Type<HunterSync> TYPE = payloadType("hunter_sync");
		public static final StreamCodec<RegistryFriendlyByteBuf, HunterSync> CODEC = CustomPacketPayload.codec(
			(p, buf) -> buf.writeByteArray(p.data), buf -> new HunterSync(buf.readByteArray()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		/** HunterState → バイト列。 */
		public static HunterSync of(HunterState s) {
			io.netty.buffer.ByteBuf raw = io.netty.buffer.Unpooled.buffer(160);
			FriendlyByteBuf b = new FriendlyByteBuf(raw);
			b.writeByte(s.weapon.ordinal());
			b.writeByte(s.element.ordinal());
			b.writeByte(s.armor.ordinal());
			b.writeByte(s.meal.ordinal());
			b.writeFloat(s.stamina);
			b.writeFloat(s.maxStamina);
			b.writeVarInt(s.sharpness);
			b.writeFloat(s.redHealth);
			b.writeVarInt(s.move == null ? -1 : s.move.netId);
			b.writeVarInt(s.moveTick);
			b.writeByte(s.chargeLevel);
			b.writeBoolean(s.holding);
			b.writeVarInt(s.holdTicks);
			// 武器固有
			b.writeByte(s.gsChain);
			b.writeFloat(s.spirit);
			b.writeByte(s.spiritLevel);
			b.writeByte(s.shells);
			b.writeVarInt(s.wyvernCooldown);
			b.writeFloat(s.saGauge);
			b.writeBoolean(s.saSword);
			b.writeFloat(s.cbEnergy);
			b.writeByte(s.phials);
			b.writeBoolean(s.cbAxe);
			b.writeBoolean(s.demonMode);
			b.writeVarInt(s.extractRed);
			b.writeVarInt(s.extractWhite);
			b.writeVarInt(s.extractOrange);
			b.writeBoolean(s.kinsectOut);
			b.writeByte(s.ammo);
			b.writeByte(s.ammoType);
			b.writeFloat(s.gatlingGauge);
			b.writeVarInt(s.piercerCooldown);
			b.writeByte(s.noteCount);
			for (int n : s.notes) b.writeByte(n);
			// バフ
			b.writeVarInt(s.hornAttack);
			b.writeVarInt(s.hornDefense);
			b.writeVarInt(s.hornSpeed);
			b.writeVarInt(s.hornElement);
			b.writeVarInt(s.hornEarplug);
			b.writeVarInt(s.energyDrink);
			b.writeBoolean(s.demondrug);
			b.writeBoolean(s.armorskin);
			// やられ状態
			b.writeVarInt(s.fireblight);
			b.writeVarInt(s.waterblight);
			b.writeVarInt(s.thunderblight);
			b.writeVarInt(s.iceblight);
			b.writeVarInt(s.dragonblight);
			b.writeVarInt(s.poison);
			b.writeVarInt(s.sleep);
			b.writeVarInt(s.paralysis);
			b.writeVarInt(s.stun);
			b.writeVarInt(s.drunk);
			b.writeVarInt(s.knockdown);
			b.writeBoolean(s.mealEaten);
			byte[] out = new byte[raw.readableBytes()];
			raw.readBytes(out);
			raw.release();
			return new HunterSync(out);
		}

		/** バイト列 → HunterState（クライアント側の写し）。 */
		public void applyTo(HunterState s) {
			io.netty.buffer.ByteBuf raw = io.netty.buffer.Unpooled.wrappedBuffer(data);
			FriendlyByteBuf b = new FriendlyByteBuf(raw);
			WeaponType w = WeaponType.byOrdinal(b.readByte());
			ElementType e = ElementType.byOrdinal(b.readByte());
			ArmorSeries a = ArmorSeries.byOrdinal(b.readByte());
			if (w != s.weapon || e != s.element || a != s.armor) {
				s.setLoadout(w, e, a);
			}
			s.meal = Meal.byOrdinal(b.readByte());
			s.stamina = b.readFloat();
			s.maxStamina = b.readFloat();
			s.sharpness = b.readVarInt();
			s.redHealth = b.readFloat();
			int mv = b.readVarInt();
			s.move = mv < 0 ? null : MoveRegistry.byNetId(mv);
			s.moveTick = b.readVarInt();
			s.chargeLevel = b.readByte();
			s.holding = b.readBoolean();
			s.holdTicks = b.readVarInt();
			s.gsChain = b.readByte();
			s.spirit = b.readFloat();
			s.spiritLevel = b.readByte();
			s.shells = b.readByte();
			s.wyvernCooldown = b.readVarInt();
			s.saGauge = b.readFloat();
			s.saSword = b.readBoolean();
			s.cbEnergy = b.readFloat();
			s.phials = b.readByte();
			s.cbAxe = b.readBoolean();
			s.demonMode = b.readBoolean();
			s.extractRed = b.readVarInt();
			s.extractWhite = b.readVarInt();
			s.extractOrange = b.readVarInt();
			s.kinsectOut = b.readBoolean();
			s.ammo = b.readByte();
			s.ammoType = b.readByte();
			s.gatlingGauge = b.readFloat();
			s.piercerCooldown = b.readVarInt();
			s.noteCount = b.readByte();
			for (int i = 0; i < s.notes.length; i++) s.notes[i] = b.readByte();
			s.hornAttack = b.readVarInt();
			s.hornDefense = b.readVarInt();
			s.hornSpeed = b.readVarInt();
			s.hornElement = b.readVarInt();
			s.hornEarplug = b.readVarInt();
			s.energyDrink = b.readVarInt();
			s.demondrug = b.readBoolean();
			s.armorskin = b.readBoolean();
			s.fireblight = b.readVarInt();
			s.waterblight = b.readVarInt();
			s.thunderblight = b.readVarInt();
			s.iceblight = b.readVarInt();
			s.dragonblight = b.readVarInt();
			s.poison = b.readVarInt();
			s.sleep = b.readVarInt();
			s.paralysis = b.readVarInt();
			s.stun = b.readVarInt();
			s.drunk = b.readVarInt();
			s.knockdown = b.readVarInt();
			s.mealEaten = b.readBoolean();
			raw.release();
		}
	}

	/** 技の開始・溜め解放など（周囲のプレイヤーにも送ってモーションを再生させる）。 */
	public record MoveEvent(int entityId, int moveNetId, int event, int charge, int dir) implements CustomPacketPayload {
		public static final Type<MoveEvent> TYPE = payloadType("move_event");
		public static final StreamCodec<RegistryFriendlyByteBuf, MoveEvent> CODEC = CustomPacketPayload.codec(
			(p, buf) -> {
				buf.writeVarInt(p.entityId);
				buf.writeVarInt(p.moveNetId);
				buf.writeByte(p.event);
				buf.writeByte(p.charge);
				buf.writeVarInt(p.dir);
			},
			buf -> new MoveEvent(buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readVarInt()));

		public static final int START = 0;
		public static final int HOLD = 1;
		public static final int RELEASE = 2;
		public static final int STOP = 3;

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** クエスト中の情報（パーティ・モンスター位置・エリア・残り時間）。 */
	public record QuestSync(int sessionId, int questIndex, int state, int timeLeft, int faints, int maxFaints,
							int field, int campX, int campZ, int[] areaX, int[] areaZ, List<Member> members,
							List<MonsterInfo> monsters, int battleKind) implements CustomPacketPayload {
		public static final Type<QuestSync> TYPE = payloadType("quest_sync");
		public static final StreamCodec<RegistryFriendlyByteBuf, QuestSync> CODEC = CustomPacketPayload.codec(QuestSync::write, QuestSync::read);

		public record Member(String name, float hp, float maxHp, int weapon, int x, int z, boolean fainted) {
		}

		public record MonsterInfo(int entityId, int kind, int x, int z, int area, int flags, float hpRatio) {
			public static final int DISCOVERED = 1;
			public static final int PAINTED = 2;
			public static final int CAPTURABLE = 4;
			public static final int DEAD = 8;
			public static final int CAPTURED = 16;
			public static final int ENRAGED = 32;
			public static final int LIMPING = 64;
			public static final int ASLEEP = 128;
			public static final int ACTIVE = 256;

			public boolean has(int flag) {
				return (flags & flag) != 0;
			}
		}

		public static final int NONE = 0;
		public static final int PREPARING = 1;
		public static final int IN_PROGRESS = 2;
		public static final int COMPLETE = 3;
		public static final int FAILED = 4;

		public static QuestSync empty() {
			return new QuestSync(-1, -1, NONE, 0, 0, 3, 0, 0, 0, new int[0], new int[0], List.of(), List.of(), -1);
		}

		private static void write(QuestSync p, FriendlyByteBuf buf) {
			buf.writeVarInt(p.sessionId);
			buf.writeVarInt(p.questIndex);
			buf.writeByte(p.state);
			buf.writeVarInt(p.timeLeft);
			buf.writeByte(p.faints);
			buf.writeByte(p.maxFaints);
			buf.writeByte(p.field);
			buf.writeInt(p.campX);
			buf.writeInt(p.campZ);
			buf.writeVarIntArray(p.areaX);
			buf.writeVarIntArray(p.areaZ);
			buf.writeVarInt(p.members.size());
			for (Member m : p.members) {
				buf.writeUtf(m.name);
				buf.writeFloat(m.hp);
				buf.writeFloat(m.maxHp);
				buf.writeByte(m.weapon);
				buf.writeInt(m.x);
				buf.writeInt(m.z);
				buf.writeBoolean(m.fainted);
			}
			buf.writeVarInt(p.monsters.size());
			for (MonsterInfo m : p.monsters) {
				buf.writeVarInt(m.entityId);
				buf.writeByte(m.kind);
				buf.writeInt(m.x);
				buf.writeInt(m.z);
				buf.writeByte(m.area);
				buf.writeVarInt(m.flags);
				buf.writeFloat(m.hpRatio);
			}
			buf.writeVarInt(p.battleKind + 1);
		}

		private static QuestSync read(FriendlyByteBuf buf) {
			int sid = buf.readVarInt();
			int qi = buf.readVarInt();
			int st = buf.readByte();
			int tl = buf.readVarInt();
			int fa = buf.readByte();
			int mf = buf.readByte();
			int fi = buf.readByte();
			int cx = buf.readInt();
			int cz = buf.readInt();
			int[] ax = buf.readVarIntArray();
			int[] az = buf.readVarIntArray();
			int n = buf.readVarInt();
			List<Member> members = new ArrayList<>(n);
			for (int i = 0; i < n; i++) {
				members.add(new Member(buf.readUtf(), buf.readFloat(), buf.readFloat(), buf.readByte(), buf.readInt(),
					buf.readInt(), buf.readBoolean()));
			}
			int m = buf.readVarInt();
			List<MonsterInfo> monsters = new ArrayList<>(m);
			for (int i = 0; i < m; i++) {
				monsters.add(new MonsterInfo(buf.readVarInt(), buf.readByte(), buf.readInt(), buf.readInt(), buf.readByte(),
					buf.readVarInt(), buf.readFloat()));
			}
			int bk = buf.readVarInt() - 1;
			return new QuestSync(sid, qi, st, tl, fa, mf, fi, cx, cz, ax, az, members, monsters, bk);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 広場の情報（募集中のクエストと自分のプロフィール）。 */
	public record HubSync(List<SessionInfo> sessions, int mySession, int hrPoints, int coins, long clearedMask,
						  int weapon, int element, int armor, int meal, int[] carry, boolean mealEaten,
						  int totalHunts) implements CustomPacketPayload {
		public static final Type<HubSync> TYPE = payloadType("hub_sync");
		public static final StreamCodec<RegistryFriendlyByteBuf, HubSync> CODEC = CustomPacketPayload.codec(HubSync::write, HubSync::read);

		public record SessionInfo(int id, int questIndex, String leader, List<String> members, int state) {
		}

		public static HubSync empty() {
			return new HubSync(List.of(), -1, 0, 0, 0L, 0, 1, 0, 0, new int[0], false, 0);
		}

		private static void write(HubSync p, FriendlyByteBuf buf) {
			buf.writeVarInt(p.sessions.size());
			for (SessionInfo s : p.sessions) {
				buf.writeVarInt(s.id);
				buf.writeVarInt(s.questIndex);
				buf.writeUtf(s.leader);
				buf.writeVarInt(s.members.size());
				for (String m : s.members) buf.writeUtf(m);
				buf.writeByte(s.state);
			}
			buf.writeVarInt(p.mySession + 1);
			buf.writeVarInt(p.hrPoints);
			buf.writeVarInt(p.coins);
			buf.writeLong(p.clearedMask);
			buf.writeByte(p.weapon);
			buf.writeByte(p.element);
			buf.writeByte(p.armor);
			buf.writeByte(p.meal);
			buf.writeVarIntArray(p.carry);
			buf.writeBoolean(p.mealEaten);
			buf.writeVarInt(p.totalHunts);
		}

		private static HubSync read(FriendlyByteBuf buf) {
			int n = buf.readVarInt();
			List<SessionInfo> list = new ArrayList<>(n);
			for (int i = 0; i < n; i++) {
				int id = buf.readVarInt();
				int qi = buf.readVarInt();
				String leader = buf.readUtf();
				int mc = buf.readVarInt();
				List<String> members = new ArrayList<>(mc);
				for (int j = 0; j < mc; j++) members.add(buf.readUtf());
				list.add(new SessionInfo(id, qi, leader, members, buf.readByte()));
			}
			return new HubSync(list, buf.readVarInt() - 1, buf.readVarInt(), buf.readVarInt(), buf.readLong(),
				buf.readByte(), buf.readByte(), buf.readByte(), buf.readByte(), buf.readVarIntArray(),
				buf.readBoolean(), buf.readVarInt());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** ステーション（クエストボード等）の画面を開く。 */
	public record OpenScreen(int screen) implements CustomPacketPayload {
		public static final Type<OpenScreen> TYPE = payloadType("open_screen");
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> CODEC =
			CustomPacketPayload.codec((p, buf) -> buf.writeByte(p.screen), buf -> new OpenScreen(buf.readByte()));

		public static final int QUEST_BOARD = 0;
		public static final int EQUIPMENT = 1;
		public static final int CANTEEN = 2;
		public static final int ITEM_BOX = 3;
		public static final int HUNTER_MENU = 4;

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 画面中央の大きな通知（クエスト開始・クリア・部位破壊など）。 */
	public record Banner(int kind, String title, String sub, int color) implements CustomPacketPayload {
		public static final Type<Banner> TYPE = payloadType("banner");
		public static final StreamCodec<RegistryFriendlyByteBuf, Banner> CODEC = CustomPacketPayload.codec(
			(p, buf) -> {
				buf.writeByte(p.kind);
				buf.writeUtf(p.title);
				buf.writeUtf(p.sub);
				buf.writeInt(p.color);
			},
			buf -> new Banner(buf.readByte(), buf.readUtf(), buf.readUtf(), buf.readInt()));

		public static final int INFO = 0;
		public static final int QUEST_START = 1;
		public static final int QUEST_CLEAR = 2;
		public static final int QUEST_FAILED = 3;
		public static final int MONSTER_INTRO = 4;
		public static final int PART_BREAK = 5;
		public static final int AREA_MOVE = 6;
		public static final int FAINT = 7;
		public static final int WARNING = 8;
		public static final int POSE = 9;
		public static final int DEPART = 10;
		public static final int SMALL = 11;

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** ダメージ数値表示。 */
	public record DamageNumber(double x, double y, double z, int amount, int flags) implements CustomPacketPayload {
		public static final Type<DamageNumber> TYPE = payloadType("damage_number");
		public static final StreamCodec<RegistryFriendlyByteBuf, DamageNumber> CODEC = CustomPacketPayload.codec(
			(p, buf) -> {
				buf.writeDouble(p.x);
				buf.writeDouble(p.y);
				buf.writeDouble(p.z);
				buf.writeVarInt(p.amount);
				buf.writeVarInt(p.flags);
			},
			buf -> new DamageNumber(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt()));

		public static final int CRIT = 1;
		public static final int WEAK_SPOT = 2;
		public static final int BOUNCE = 4;
		public static final int ELEMENT = 8;
		public static final int FIXED = 16;
		public static final int STATUS = 32;

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 吹っ飛ばし・打ち上げ（クライアントで速度を与える）。add=true なら加算。 */
	public record Knockback(double x, double y, double z, boolean add) implements CustomPacketPayload {
		public static final Type<Knockback> TYPE = payloadType("knockback");
		public static final StreamCodec<RegistryFriendlyByteBuf, Knockback> CODEC = CustomPacketPayload.codec(
			(p, buf) -> {
				buf.writeDouble(p.x);
				buf.writeDouble(p.y);
				buf.writeDouble(p.z);
				buf.writeBoolean(p.add);
			},
			buf -> new Knockback(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** クエスト結果（報酬画面）。 */
	public record Result(int questIndex, boolean success, int points, int coins, int hrBefore, int hrAfter,
						 int timeTicks, List<String> lines) implements CustomPacketPayload {
		public static final Type<Result> TYPE = payloadType("result");
		public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = CustomPacketPayload.codec(
			(p, buf) -> {
				buf.writeVarInt(p.questIndex);
				buf.writeBoolean(p.success);
				buf.writeVarInt(p.points);
				buf.writeVarInt(p.coins);
				buf.writeVarInt(p.hrBefore);
				buf.writeVarInt(p.hrAfter);
				buf.writeVarInt(p.timeTicks);
				buf.writeVarInt(p.lines.size());
				for (String s : p.lines) buf.writeUtf(s);
			},
			buf -> {
				int qi = buf.readVarInt();
				boolean ok = buf.readBoolean();
				int pts = buf.readVarInt();
				int coins = buf.readVarInt();
				int hb = buf.readVarInt();
				int ha = buf.readVarInt();
				int tt = buf.readVarInt();
				int n = buf.readVarInt();
				List<String> lines = new ArrayList<>(n);
				for (int i = 0; i < n; i++) lines.add(buf.readUtf());
				return new Result(qi, ok, pts, coins, hb, ha, tt, lines);
			});

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
