package com.buskohunter.client.anim;

/**
 * プレイヤーモデルに上書きするポーズ（ラジアン）。
 * set フラグが立っている部位だけを上書き（加算）する。
 */
public final class Pose {
	public float headX, headY, headZ;
	public float bodyX, bodyY, bodyZ;
	public float rArmX, rArmY, rArmZ;
	public float lArmX, lArmY, lArmZ;
	public float rLegX, rLegY, rLegZ;
	public float lLegX, lLegY, lLegZ;
	/** 全身の回転（回避ローリングなど）。体の中心を軸に回る。 */
	public float rootX, rootZ, rootYaw;
	/** 全身の上下オフセット（ピクセル、下が正）。 */
	public float rootY;
	public boolean arms, legs, body, head, root;
	/** 腕の向きを視線（頭のピッチ・ヨー）に追従させる（射撃の構え）。 */
	public boolean armsFollowHead;
	/** 0〜1：バニラのポーズとの混ぜ具合。 */
	public float weight = 1f;

	public Pose reset() {
		headX = headY = headZ = 0;
		bodyX = bodyY = bodyZ = 0;
		rArmX = rArmY = rArmZ = 0;
		lArmX = lArmY = lArmZ = 0;
		rLegX = rLegY = rLegZ = 0;
		lLegX = lLegY = lLegZ = 0;
		rootX = rootZ = rootY = rootYaw = 0;
		arms = legs = body = head = root = false;
		armsFollowHead = false;
		weight = 1f;
		return this;
	}

	/** 左右反転（ミラー技用）。 */
	public void mirror() {
		float ax = rArmX, ay = rArmY, az = rArmZ;
		rArmX = lArmX;
		rArmY = -lArmY;
		rArmZ = -lArmZ;
		lArmX = ax;
		lArmY = -ay;
		lArmZ = -az;
		float lx = rLegX, ly = rLegY, lz = rLegZ;
		rLegX = lLegX;
		rLegY = -lLegY;
		rLegZ = -lLegZ;
		lLegX = lx;
		lLegY = -ly;
		lLegZ = -lz;
		bodyY = -bodyY;
		bodyZ = -bodyZ;
		headY = -headY;
		headZ = -headZ;
		rootZ = -rootZ;
		rootYaw = -rootYaw;
	}

	public void arms(float rx, float ry, float rz, float lx, float ly, float lz) {
		rArmX = rx;
		rArmY = ry;
		rArmZ = rz;
		lArmX = lx;
		lArmY = ly;
		lArmZ = lz;
		arms = true;
	}

	public void body(float x, float y, float z) {
		bodyX = x;
		bodyY = y;
		bodyZ = z;
		body = true;
	}

	public void legs(float rx, float lx) {
		rLegX = rx;
		lLegX = lx;
		legs = true;
	}

	public void head(float x, float y) {
		headX = x;
		headY = y;
		head = true;
	}
}
