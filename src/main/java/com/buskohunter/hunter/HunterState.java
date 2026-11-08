package com.buskohunter.hunter;

import com.buskohunter.combat.MoveDef;
import com.buskohunter.combat.ShotKind;
import com.buskohunter.core.ArmorSeries;
import com.buskohunter.core.ArmorSkill;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.Meal;
import com.buskohunter.core.WeaponStats;
import com.buskohunter.core.WeaponType;

/**
 * ハンター 1 人の「戦闘中の状態」。サーバーが保持し、要約をクライアントへ同期する。
 * （Minecraft の型に依存しない純粋なデータ）
 */
public final class HunterState {
	// ---- 装備 ----
	public WeaponType weapon = WeaponType.GREAT_SWORD;
	public ElementType element = ElementType.FIRE;
	public ArmorSeries armor = ArmorSeries.HUNTER;
	public Meal meal = Meal.NONE;
	public WeaponStats stats = WeaponStats.of(weapon, element);

	// ---- 基本ゲージ ----
	public float stamina = 100;
	public float maxStamina = 100;
	public int staminaRegenDelay;
	public int sharpness;
	public float redHealth;

	// ---- 技の実行状態 ----
	public MoveDef move;
	public int moveTick;
	public boolean holding;
	public int holdTicks;
	public int chargeLevel;
	public int inputBits;
	public int prevInputBits;
	/** 先行入力（次の技）。 */
	public com.buskohunter.combat.InputKind buffered;
	public int bufferTicks;
	public int iframes;
	public boolean counterSuccess;
	public float dodgeYaw;

	// ---- 武器固有ゲージ ----
	/** 大剣：タックル後に続く溜め段階（0=縦斬り,1=強溜め,2=真溜め）。 */
	public int gsChain;
	/** 太刀：練気ゲージと練気レベル。 */
	public float spirit;
	public int spiritLevel;
	public int spiritLevelTimer;
	/** ガンランス：弾数とクールタイム。 */
	public int shells = 5;
	public int wyvernCooldown;
	/** スラッシュアックス。 */
	public float saGauge = 100;
	public boolean saSword;
	/** チャージアックス。 */
	public float cbEnergy;
	public int phials;
	public boolean cbAxe;
	/** 双剣：鬼人化。 */
	public boolean demonMode;
	/** 操虫棍：エキス（残り tick）。 */
	public int extractRed, extractWhite, extractOrange;
	public boolean kinsectOut;
	/** ボウガン：装填数と弾種。 */
	public int ammo = 9;
	public int ammoType;
	public float gatlingGauge = 100;
	/** 弓：竜の一矢クールタイム。 */
	public int piercerCooldown;
	/** 狩猟笛：直近の音符（最大 3）。 */
	public final int[] notes = new int[3];
	public int noteCount;

	// ---- バフ（残り tick） ----
	public int hornAttack, hornDefense, hornSpeed, hornElement, hornEarplug, hornSelf;
	public int energyDrink;
	public boolean demondrug, armorskin;

	// ---- やられ状態（残り tick） ----
	public int fireblight, waterblight, thunderblight, iceblight, dragonblight;
	public int poison, sleep, paralysis, stun, drunk;
	public int knockdown;

	// ---- クエスト ----
	/** 武器挙動用の作業領域（フルバーストの弾数・連続突きの回数など）。 */
	public int scratch;
	public int chain;
	public boolean mealEaten;
	public int questRankDefense = 80;

	public void setLoadout(WeaponType weapon, ElementType element, ArmorSeries armor) {
		this.weapon = weapon;
		this.element = element;
		this.armor = armor;
		this.stats = WeaponStats.of(weapon, element);
		resetWeaponGauges();
	}

	public void resetWeaponGauges() {
		sharpness = stats.sharpness().total();
		gsChain = 0;
		spirit = 0;
		spiritLevel = 0;
		shells = 5;
		wyvernCooldown = 0;
		saGauge = 100;
		saSword = false;
		cbEnergy = 0;
		phials = 0;
		cbAxe = false;
		demonMode = false;
		ammoType = 0;
		ammo = ShotKind.BOWGUN_AMMO[0].magazine;
		gatlingGauge = 100;
		piercerCooldown = 0;
		noteCount = 0;
		move = null;
		holding = false;
	}

	/** クエスト開始時のリセット。 */
	public void resetForQuest() {
		resetWeaponGauges();
		maxStamina = 100 + meal.staminaBonus;
		stamina = maxStamina;
		redHealth = 0;
		hornAttack = hornDefense = hornSpeed = hornElement = hornEarplug = hornSelf = 0;
		energyDrink = 0;
		demondrug = armorskin = false;
		clearAilments();
		extractRed = extractWhite = extractOrange = 0;
	}

	public void clearAilments() {
		fireblight = waterblight = thunderblight = iceblight = dragonblight = 0;
		poison = sleep = paralysis = stun = drunk = 0;
		knockdown = 0;
	}

	public boolean has(ArmorSkill skill) {
		return armor.has(skill);
	}

	/** 行動不能（睡眠・麻痺・気絶）か。 */
	public boolean incapacitated() {
		return sleep > 0 || paralysis > 0 || stun > 0;
	}

	public boolean inMove() {
		return move != null;
	}

	/** 現在ガード中か（ガード可能武器でしゃがみ、または技のガード frame 中）。 */
	public boolean guarding() {
		if (move != null) {
			return move.guardFrom >= 0 && moveTick >= move.guardFrom && moveTick <= move.guardTo;
		}
		boolean guardInput = (inputBits & com.buskohunter.combat.InputKind.HELD_GUARD) != 0;
		if (!guardInput) return false;
		if (weapon == WeaponType.CHARGE_BLADE) return !cbAxe;
		return weapon.canGuard;
	}

	public float attackMultiplier(boolean monsterEnraged) {
		float m = 1.0f + meal.attackBonus;
		if (has(ArmorSkill.ATTACK_UP)) m += 0.10f;
		if (has(ArmorSkill.CHALLENGER) && monsterEnraged) m += 0.20f;
		if (demondrug) m += 0.08f;
		if (hornAttack > 0) m += 0.15f;
		if (extractRed > 0) m += 0.10f;
		if (extractRed > 0 && extractWhite > 0 && extractOrange > 0) m += 0.05f;
		if (weapon == WeaponType.LONG_SWORD) {
			m += switch (spiritLevel) {
				case 1 -> 0.05f;
				case 2 -> 0.10f;
				case 3 -> 0.20f;
				default -> 0f;
			};
		}
		if (weapon == WeaponType.DUAL_BLADES && demonMode) m += 0.15f;
		return m;
	}

	public float elementMultiplier() {
		float m = 1.0f;
		if (has(ArmorSkill.ELEMENT_UP)) m += 0.20f;
		if (has(ArmorSkill.FIRE_ATTACK) && element == ElementType.FIRE) m += 0.30f;
		if (has(ArmorSkill.BUSKOBA_SOUL) && element == ElementType.BUSKOBA) m += 0.50f;
		if (hornElement > 0) m += 0.20f;
		if (weapon == WeaponType.SWITCH_AXE && saSword) m += 0.25f;
		return m;
	}

	public int affinity(boolean weakSpot) {
		int a = stats.affinity() + meal.affinityBonus;
		if (has(ArmorSkill.CRITICAL_EYE)) a += 20;
		if (has(ArmorSkill.WEAKNESS_EXPLOIT) && weakSpot) a += 40;
		if (meal.skill == Meal.MealSkill.CRIT_DRAW) a += 10;
		return Math.max(-100, Math.min(100, a));
	}

	/** 被ダメージ倍率。 */
	public float defenseMultiplier() {
		float def = questRankDefense;
		float m = 80f / (80f + def);
		if (has(ArmorSkill.DEFENSE_UP)) m *= 0.88f;
		if (meal.defenseBonus > 0) m *= (1f - meal.defenseBonus);
		if (armorskin) m *= 0.88f;
		if (hornDefense > 0) m *= 0.85f;
		if (extractOrange > 0) m *= 0.90f;
		return m;
	}

	public int iframeBonus() {
		return has(ArmorSkill.EVADE_WINDOW) ? 3 : 0;
	}

	public float staminaCostMul() {
		float m = 1f;
		if (meal.skill == Meal.MealSkill.STAMINA_SAVE) m *= 0.75f;
		if (energyDrink > 0) m = 0f;
		return m;
	}

	public float staminaRegenMul() {
		float m = 1f;
		if (has(ArmorSkill.STAMINA_SURGE)) m += 0.5f;
		if (extractWhite > 0) m += 0.25f;
		return m;
	}

	public float healMul() {
		float m = 1f;
		if (has(ArmorSkill.RECOVERY_UP)) m += 0.3f;
		if (meal.skill == Meal.MealSkill.HEAL_BOOST) m += 0.2f;
		return m;
	}

	public float speedMultiplier() {
		float m = 1f;
		if (hornSpeed > 0 || hornSelf > 0) m += 0.12f;
		if (extractWhite > 0) m += 0.10f;
		return m;
	}

	public ShotKind currentAmmo() {
		return ShotKind.BOWGUN_AMMO[Math.floorMod(ammoType, ShotKind.BOWGUN_AMMO.length)];
	}

	public void pushNote(int note) {
		if (noteCount < 3) {
			notes[noteCount++] = note;
		} else {
			notes[0] = notes[1];
			notes[1] = notes[2];
			notes[2] = note;
		}
	}

	public void spendStamina(float amount) {
		stamina = Math.max(0, stamina - amount * staminaCostMul());
		staminaRegenDelay = 16;
	}
}
