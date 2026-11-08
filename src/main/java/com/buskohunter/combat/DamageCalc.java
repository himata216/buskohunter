package com.buskohunter.combat;

import com.buskohunter.core.ArmorSkill;
import com.buskohunter.core.ElementType;
import com.buskohunter.core.MonsterKind;
import com.buskohunter.core.Sharpness;
import com.buskohunter.core.WeaponType;
import com.buskohunter.hunter.HunterState;

import java.util.Random;

/** モンハン風ダメージ計算（肉質・切れ味・会心・属性・状態異常蓄積）。 */
public final class DamageCalc {
	private DamageCalc() {
	}

	public record Result(float raw, float element, float total, boolean crit, boolean bounce, boolean weakSpot,
						 ElementType element_type, float statusBuildup, float ko, float partDamage) {
	}

	/** モンスター側の状態。 */
	public record Target(MonsterKind kind, MonsterKind.Part part, boolean enraged, boolean buskobaState,
						 boolean asleep, float hpMul) {
	}

	/**
	 * 近接・射撃の通常ダメージ。
	 *
	 * @param mv            モーション値
	 * @param elementMulMv  技の属性倍率
	 * @param statusMulMv   技の状態異常倍率
	 * @param ko            技のスタン値
	 * @param kind          ダメージ種別（打撃上書きなど）
	 * @param extraMul      溜め倍率・弾種倍率など
	 */
	public static Result compute(HunterState hunter, Target target, float mv, float elementMulMv, float statusMulMv,
								 float ko, WeaponType.DamageKind kind, float extraMul, Random rng) {
		MonsterKind.Part part = target.part();
		int hz = part.hitzone(kind);
		if (target.buskobaState()) hz = Math.min(100, hz + 30);

		boolean melee = !hunter.weapon.ranged;
		Sharpness.Color color = melee ? hunter.stats.sharpness().colorAt(hunter.sharpness) : Sharpness.Color.WHITE;
		float sharpRaw = melee ? color.raw : 1.0f;
		float sharpElem = melee ? color.element : 1.0f;
		boolean bounce = melee && kind != WeaponType.DamageKind.BLUNT && hz * sharpRaw < 25;

		int affinity = hunter.affinity(part.weakSpot());
		boolean crit = false;
		float critMul = 1.0f;
		if (affinity > 0 && rng.nextInt(100) < affinity) {
			crit = true;
			critMul = 1.25f;
		} else if (affinity < 0 && rng.nextInt(100) < -affinity) {
			critMul = 0.75f;
		}

		float atkMul = hunter.attackMultiplier(target.enraged());
		float raw = hunter.stats.attack() * atkMul * (mv / 100f) * sharpRaw * (hz / 100f) * critMul * extraMul;

		float elem = 0f;
		float status = 0f;
		ElementType e = hunter.element;
		float eVal = hunter.stats.elementValue() * hunter.elementMultiplier();
		if (e.isElemental()) {
			int weak = target.kind().weaknessTo(e);
			elem = eVal * (weak / 100f) * part.elementMul() * elementMulMv * sharpElem * 1.6f;
			if (target.buskobaState()) elem *= 1.2f;
		} else if (e.isStatus()) {
			status = eVal * 0.45f * statusMulMv;
		} else if (e == ElementType.BUSKOBA) {
			elem = eVal * 0.12f * part.elementMul() * elementMulMv * sharpElem * 1.6f;
			status = eVal * 0.40f * statusMulMv;
		}

		if (target.asleep()) {
			raw *= 2.0f;
			elem *= 2.0f;
		}

		float total = Math.max(1f, raw + elem);
		float koOut = ko;
		if (hunter.has(ArmorSkill.KO_UP)) koOut *= 1.3f;
		float partDmg = total * (hunter.has(ArmorSkill.PARTBREAKER) ? 1.3f : 1.0f);
		return new Result(raw, elem, total, crit, bounce, part.weakSpot() && hz >= 45, e, status, koOut, partDmg);
	}

	/** 肉質を無視する固定ダメージ（砲撃・ビン爆発・爆弾）。 */
	public static Result fixed(HunterState hunter, Target target, float damage, ElementType element) {
		float total = damage * (hunter == null ? 1f : (1f + (hunter.attackMultiplier(target.enraged()) - 1f) * 0.5f));
		float elem = 0f;
		if (element != null && element.isElemental()) {
			elem = damage * 0.25f * (target.kind().weaknessTo(element) / 20f);
		}
		if (target.asleep()) total *= 3.0f;
		return new Result(total, elem, total + elem, false, false, false, element == null ? ElementType.NONE : element,
			0f, 0f, (total + elem) * 0.8f);
	}
}
