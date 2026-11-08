package com.buskohunter.client.render.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 自動生成ファイル（tools/models/gen_models.py）。手で編集せず、スクリプトを直して再生成してください。
 */
@SuppressWarnings("unused")
public final class BuskoMeshes {
	private BuskoMeshes() {
	}

	/** 各パーツの root からのパス。 */
	public static final String[][] BUSKO_PARTS = {
		{"root"},
		{"root", "body"},
		{"root", "body", "lobe_r"},
		{"root", "body", "lobe_l"},
		{"root", "body", "neck"},
		{"root", "body", "neck", "head"},
		{"root", "body", "neck", "head", "visor"},
		{"root", "body", "neck", "head", "hat"},
		{"root", "body", "neck", "head", "hat", "hat_band"},
		{"root", "body", "neck", "head", "hat", "horn_r"},
		{"root", "body", "neck", "head", "hat", "horn_r", "horn_tip_r"},
		{"root", "body", "neck", "head", "hat", "horn_l"},
		{"root", "body", "neck", "head", "hat", "horn_l", "horn_tip_l"},
		{"root", "body", "neck", "head", "hat", "brim_spike_0"},
		{"root", "body", "neck", "head", "hat", "brim_spike_1"},
		{"root", "body", "neck", "head", "hat", "brim_spike_2"},
		{"root", "body", "neck", "head", "hat", "brim_spike_3"},
		{"root", "body", "halo"},
		{"root", "body", "halo", "halo_0"},
		{"root", "body", "halo", "halo_1"},
		{"root", "body", "halo", "halo_2"},
		{"root", "body", "halo", "halo_3"},
		{"root", "body", "halo", "halo_4"},
		{"root", "body", "halo", "halo_5"},
		{"root", "body", "halo", "halo_6"},
		{"root", "body", "halo", "halo_7"},
		{"root", "body", "halo", "halo_8"},
		{"root", "body", "halo", "halo_9"},
		{"root", "body", "halo", "halo_10"},
		{"root", "body", "halo", "halo_11"},
		{"root", "body", "halo", "halo_12"},
		{"root", "body", "halo", "halo_13"},
		{"root", "body", "halo", "halo_14"},
		{"root", "body", "halo", "halo_15"},
		{"root", "body", "halo", "halo_in_0"},
		{"root", "body", "halo", "halo_in_1"},
		{"root", "body", "halo", "halo_in_2"},
		{"root", "body", "halo", "halo_in_3"},
		{"root", "body", "halo", "halo_in_4"},
		{"root", "body", "halo", "halo_in_5"},
		{"root", "body", "halo", "halo_in_6"},
		{"root", "body", "halo", "halo_in_7"},
		{"root", "body", "halo", "halo_rune_0"},
		{"root", "body", "halo", "halo_rune_1"},
		{"root", "body", "halo", "halo_rune_2"},
		{"root", "body", "halo", "halo_rune_3"},
		{"root", "body", "halo", "halo_rune_4"},
		{"root", "body", "halo", "halo_rune_5"},
		{"root", "body", "arm_r"},
		{"root", "body", "arm_r", "forearm_r"},
		{"root", "body", "arm_r", "forearm_r", "hand_r"},
		{"root", "body", "arm_r", "forearm_r", "hand_r", "claw_r_0"},
		{"root", "body", "arm_r", "forearm_r", "hand_r", "claw_r_1"},
		{"root", "body", "arm_r", "forearm_r", "hand_r", "claw_r_2"},
		{"root", "body", "arm_l"},
		{"root", "body", "arm_l", "forearm_l"},
		{"root", "body", "arm_l", "forearm_l", "hand_l"},
		{"root", "body", "arm_l", "forearm_l", "hand_l", "claw_l_0"},
		{"root", "body", "arm_l", "forearm_l", "hand_l", "claw_l_1"},
		{"root", "body", "arm_l", "forearm_l", "hand_l", "claw_l_2"},
		{"root", "body", "wing_r"},
		{"root", "body", "wing_r", "wing_mem_r"},
		{"root", "body", "wing2_r"},
		{"root", "body", "wing2_r", "wing2_mem_r"},
		{"root", "body", "wing_l"},
		{"root", "body", "wing_l", "wing_mem_l"},
		{"root", "body", "wing2_l"},
		{"root", "body", "wing2_l", "wing2_mem_l"},
		{"root", "body", "spike_0"},
		{"root", "body", "spike_1"},
		{"root", "body", "spike_2"},
		{"root", "body", "spike_3"},
		{"root", "body", "lobe_r", "plate_r"},
		{"root", "body", "lobe_l", "plate_l"},
		{"root", "body", "cable_0"},
		{"root", "body", "cable_1"},
		{"root", "body", "cable_2"},
		{"root", "body", "cable_3"}
	};
	/** 各パーツの root からのパス。 */
	public static final String[][] SAKERAN_PARTS = {
		{"root"},
		{"root", "hips"},
		{"root", "hips", "waist"},
		{"root", "hips", "waist", "chest"},
		{"root", "hips", "waist", "chest", "ruff"},
		{"root", "hips", "waist", "chest", "neck"},
		{"root", "hips", "waist", "chest", "neck", "head"},
		{"root", "hips", "waist", "chest", "neck", "head", "muzzle"},
		{"root", "hips", "waist", "chest", "neck", "head", "muzzle", "nose"},
		{"root", "hips", "waist", "chest", "neck", "head", "muzzle", "jaw"},
		{"root", "hips", "waist", "chest", "neck", "head", "ear_r"},
		{"root", "hips", "waist", "chest", "neck", "head", "cheek_r"},
		{"root", "hips", "waist", "chest", "delt_r"},
		{"root", "hips", "waist", "chest", "delt_r", "arm_r"},
		{"root", "hips", "waist", "chest", "delt_r", "arm_r", "forearm_r"},
		{"root", "hips", "waist", "chest", "delt_r", "arm_r", "forearm_r", "paw_r"},
		{"root", "hips", "waist", "chest", "delt_r", "arm_r", "forearm_r", "paw_r", "finger1_r"},
		{"root", "hips", "waist", "chest", "delt_r", "arm_r", "forearm_r", "paw_r", "finger2_r"},
		{"root", "hips", "waist", "chest", "delt_r", "arm_r", "forearm_r", "paw_r", "finger1_r", "claw1_r"},
		{"root", "hips", "waist", "chest", "delt_r", "arm_r", "forearm_r", "paw_r", "finger2_r", "claw2_r"},
		{"root", "hips", "thigh_r"},
		{"root", "hips", "thigh_r", "shin_r"},
		{"root", "hips", "thigh_r", "shin_r", "foot_r"},
		{"root", "hips", "waist", "chest", "neck", "head", "ear_l"},
		{"root", "hips", "waist", "chest", "neck", "head", "cheek_l"},
		{"root", "hips", "waist", "chest", "delt_l"},
		{"root", "hips", "waist", "chest", "delt_l", "arm_l"},
		{"root", "hips", "waist", "chest", "delt_l", "arm_l", "forearm_l"},
		{"root", "hips", "waist", "chest", "delt_l", "arm_l", "forearm_l", "paw_l"},
		{"root", "hips", "waist", "chest", "delt_l", "arm_l", "forearm_l", "paw_l", "finger1_l"},
		{"root", "hips", "waist", "chest", "delt_l", "arm_l", "forearm_l", "paw_l", "finger2_l"},
		{"root", "hips", "waist", "chest", "delt_l", "arm_l", "forearm_l", "paw_l", "finger1_l", "claw1_l"},
		{"root", "hips", "waist", "chest", "delt_l", "arm_l", "forearm_l", "paw_l", "finger2_l", "claw2_l"},
		{"root", "hips", "thigh_l"},
		{"root", "hips", "thigh_l", "shin_l"},
		{"root", "hips", "thigh_l", "shin_l", "foot_l"},
		{"root", "hips", "tail"}
	};

	public static LayerDefinition createBuskoLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition p_ = mesh.getRoot();
		PartDefinition p_root = p_.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition p_body = p_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-22.0F, -16.0F, -17.0F, 44.0F, 32.0F, 34.0F).texOffs(186, 66).addBox(-18.0F, -13.0F, -20.0F, 36.0F, 26.0F, 3.0F).texOffs(156, 0).addBox(-25.0F, -12.0F, -13.0F, 50.0F, 24.0F, 26.0F).texOffs(264, 66).addBox(-17.0F, -19.0F, -13.0F, 34.0F, 3.0F, 26.0F).texOffs(0, 99).addBox(-17.0F, 16.0F, -13.0F, 34.0F, 2.0F, 26.0F).texOffs(120, 99).addBox(-14.0F, -23.0F, -15.0F, 28.0F, 7.0F, 20.0F), PartPose.offset(0.0F, -18.0F, 0.0F));
		PartDefinition p_lobe_r = p_body.addOrReplaceChild("lobe_r", CubeListBuilder.create().texOffs(308, 0).addBox(-11.0F, -15.0F, -2.0F, 22.0F, 30.0F, 12.0F).texOffs(216, 99).addBox(-9.0F, -12.0F, 10.0F, 18.0F, 24.0F, 3.0F).texOffs(444, 0).addBox(-12.0F, -12.0F, 0.0F, 24.0F, 24.0F, 9.0F), PartPose.offset(-11.0F, -1.0F, 15.0F));
		PartDefinition p_lobe_l = p_body.addOrReplaceChild("lobe_l", CubeListBuilder.create().texOffs(376, 0).addBox(-11.0F, -15.0F, -2.0F, 22.0F, 30.0F, 12.0F).texOffs(258, 99).addBox(-9.0F, -12.0F, 10.0F, 18.0F, 24.0F, 3.0F).texOffs(0, 66).addBox(-12.0F, -12.0F, 0.0F, 24.0F, 24.0F, 9.0F), PartPose.offset(11.0F, -1.0F, 15.0F));
		PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(208, 127).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 6.0F, 8.0F), PartPose.offset(0.0F, -22.0F, -7.0F));
		PartDefinition p_head = p_neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(436, 99).addBox(-6.0F, -11.0F, -6.0F, 12.0F, 11.0F, 12.0F).texOffs(400, 127).addBox(-5.0F, -9.0F, -7.0F, 10.0F, 7.0F, 1.0F), PartPose.offset(0.0F, -5.0F, 0.0F));
		PartDefinition p_visor = p_head.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(0, 149).addBox(-6.5F, -2.0F, -0.6F, 13.0F, 4.0F, 1.0F), PartPose.offset(0.0F, -7.0F, -7.0F));
		PartDefinition p_hat = p_head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(66, 66).addBox(-15.0F, -1.0F, -15.0F, 30.0F, 2.0F, 30.0F).texOffs(380, 99).addBox(-7.0F, -11.0F, -7.0F, 14.0F, 10.0F, 14.0F).texOffs(272, 127).addBox(-5.0F, -14.0F, -5.0F, 10.0F, 3.0F, 10.0F), PartPose.offset(0.0F, -10.0F, 0.0F));
		PartDefinition p_hat_band = p_hat.addOrReplaceChild("hat_band", CubeListBuilder.create().texOffs(84, 127).addBox(-7.5F, -2.0F, -7.5F, 15.0F, 2.0F, 15.0F), PartPose.offset(0.0F, -3.0F, 0.0F));
		PartDefinition p_horn_r = p_hat.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(240, 127).addBox(-2.0F, -10.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offsetAndRotation(-6.0F, -8.0F, 0.0F, 0.0F, 0.0F, -0.55F));
		PartDefinition p_horn_tip_r = p_horn_r.addOrReplaceChild("horn_tip_r", CubeListBuilder.create().texOffs(312, 127).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, -0.2F, 0.0F, -0.55F));
		PartDefinition p_horn_l = p_hat.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(256, 127).addBox(-2.0F, -10.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offsetAndRotation(6.0F, -8.0F, 0.0F, 0.0F, 0.0F, 0.55F));
		PartDefinition p_horn_tip_l = p_horn_l.addOrReplaceChild("horn_tip_l", CubeListBuilder.create().texOffs(324, 127).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, -0.2F, 0.0F, 0.55F));
		PartDefinition p_brim_spike_0 = p_hat.addOrReplaceChild("brim_spike_0", CubeListBuilder.create().texOffs(422, 127).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(9.1924F, -1.0F, 9.1924F, -0.4243F, 0.0F, 0.4243F));
		PartDefinition p_brim_spike_1 = p_hat.addOrReplaceChild("brim_spike_1", CubeListBuilder.create().texOffs(430, 127).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(-9.1924F, -1.0F, 9.1924F, -0.4243F, 0.0F, -0.4243F));
		PartDefinition p_brim_spike_2 = p_hat.addOrReplaceChild("brim_spike_2", CubeListBuilder.create().texOffs(438, 127).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(-9.1924F, -1.0F, -9.1924F, 0.4243F, 0.0F, -0.4243F));
		PartDefinition p_brim_spike_3 = p_hat.addOrReplaceChild("brim_spike_3", CubeListBuilder.create().texOffs(446, 127).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(9.1924F, -1.0F, -9.1924F, 0.4243F, 0.0F, 0.4243F));
		PartDefinition p_halo = p_body.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offset(0.0F, -38.0F, 22.0F));
		PartDefinition p_halo_0 = p_halo.addOrReplaceChild("halo_0", CubeListBuilder.create().texOffs(28, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(30.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));
		PartDefinition p_halo_1 = p_halo.addOrReplaceChild("halo_1", CubeListBuilder.create().texOffs(56, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(27.7164F, 11.4805F, 0.0F, 0.0F, 0.0F, 1.9635F));
		PartDefinition p_halo_2 = p_halo.addOrReplaceChild("halo_2", CubeListBuilder.create().texOffs(84, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(21.2132F, 21.2132F, 0.0F, 0.0F, 0.0F, 2.3562F));
		PartDefinition p_halo_3 = p_halo.addOrReplaceChild("halo_3", CubeListBuilder.create().texOffs(112, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(11.4805F, 27.7164F, 0.0F, 0.0F, 0.0F, 2.7489F));
		PartDefinition p_halo_4 = p_halo.addOrReplaceChild("halo_4", CubeListBuilder.create().texOffs(140, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 30.0F, 0.0F, 0.0F, 0.0F, 3.1416F));
		PartDefinition p_halo_5 = p_halo.addOrReplaceChild("halo_5", CubeListBuilder.create().texOffs(168, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-11.4805F, 27.7164F, 0.0F, 0.0F, 0.0F, 3.5343F));
		PartDefinition p_halo_6 = p_halo.addOrReplaceChild("halo_6", CubeListBuilder.create().texOffs(196, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-21.2132F, 21.2132F, 0.0F, 0.0F, 0.0F, 3.927F));
		PartDefinition p_halo_7 = p_halo.addOrReplaceChild("halo_7", CubeListBuilder.create().texOffs(224, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-27.7164F, 11.4805F, 0.0F, 0.0F, 0.0F, 4.3197F));
		PartDefinition p_halo_8 = p_halo.addOrReplaceChild("halo_8", CubeListBuilder.create().texOffs(252, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-30.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.7124F));
		PartDefinition p_halo_9 = p_halo.addOrReplaceChild("halo_9", CubeListBuilder.create().texOffs(280, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-27.7164F, -11.4805F, 0.0F, 0.0F, 0.0F, 5.1051F));
		PartDefinition p_halo_10 = p_halo.addOrReplaceChild("halo_10", CubeListBuilder.create().texOffs(308, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-21.2132F, -21.2132F, 0.0F, 0.0F, 0.0F, 5.4978F));
		PartDefinition p_halo_11 = p_halo.addOrReplaceChild("halo_11", CubeListBuilder.create().texOffs(336, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-11.4805F, -27.7164F, 0.0F, 0.0F, 0.0F, 5.8905F));
		PartDefinition p_halo_12 = p_halo.addOrReplaceChild("halo_12", CubeListBuilder.create().texOffs(364, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -30.0F, 0.0F, 0.0F, 0.0F, 6.2832F));
		PartDefinition p_halo_13 = p_halo.addOrReplaceChild("halo_13", CubeListBuilder.create().texOffs(392, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(11.4805F, -27.7164F, 0.0F, 0.0F, 0.0F, 6.6759F));
		PartDefinition p_halo_14 = p_halo.addOrReplaceChild("halo_14", CubeListBuilder.create().texOffs(420, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(21.2132F, -21.2132F, 0.0F, 0.0F, 0.0F, 7.0686F));
		PartDefinition p_halo_15 = p_halo.addOrReplaceChild("halo_15", CubeListBuilder.create().texOffs(448, 149).addBox(-6.3905F, -1.5F, -1.0F, 12.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(27.7164F, -11.4805F, 0.0F, 0.0F, 0.0F, 7.4613F));
		PartDefinition p_halo_in_0 = p_halo.addOrReplaceChild("halo_in_0", CubeListBuilder.create().texOffs(90, 154).addBox(-7.5F, -1.0F, -0.5F, 15.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(17.6412F, 3.576F, 0.5F, 0.0F, 0.0F, 1.7708F));
		PartDefinition p_halo_in_1 = p_halo.addOrReplaceChild("halo_in_1", CubeListBuilder.create().texOffs(122, 154).addBox(-7.5F, -1.0F, -0.5F, 15.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(9.9456F, 15.0029F, 0.5F, 0.0F, 0.0F, 2.5562F));
		PartDefinition p_halo_in_2 = p_halo.addOrReplaceChild("halo_in_2", CubeListBuilder.create().texOffs(154, 154).addBox(-7.5F, -1.0F, -0.5F, 15.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-3.576F, 17.6412F, 0.5F, 0.0F, 0.0F, 3.3416F));
		PartDefinition p_halo_in_3 = p_halo.addOrReplaceChild("halo_in_3", CubeListBuilder.create().texOffs(186, 154).addBox(-7.5F, -1.0F, -0.5F, 15.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-15.0029F, 9.9456F, 0.5F, 0.0F, 0.0F, 4.127F));
		PartDefinition p_halo_in_4 = p_halo.addOrReplaceChild("halo_in_4", CubeListBuilder.create().texOffs(218, 154).addBox(-7.5F, -1.0F, -0.5F, 15.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-17.6412F, -3.576F, 0.5F, 0.0F, 0.0F, 4.9124F));
		PartDefinition p_halo_in_5 = p_halo.addOrReplaceChild("halo_in_5", CubeListBuilder.create().texOffs(250, 154).addBox(-7.5F, -1.0F, -0.5F, 15.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-9.9456F, -15.0029F, 0.5F, 0.0F, 0.0F, 5.6978F));
		PartDefinition p_halo_in_6 = p_halo.addOrReplaceChild("halo_in_6", CubeListBuilder.create().texOffs(282, 154).addBox(-7.5F, -1.0F, -0.5F, 15.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(3.576F, -17.6412F, 0.5F, 0.0F, 0.0F, 6.4832F));
		PartDefinition p_halo_in_7 = p_halo.addOrReplaceChild("halo_in_7", CubeListBuilder.create().texOffs(314, 154).addBox(-7.5F, -1.0F, -0.5F, 15.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(15.0029F, -9.9456F, 0.5F, 0.0F, 0.0F, 7.2686F));
		PartDefinition p_halo_rune_0 = p_halo.addOrReplaceChild("halo_rune_0", CubeListBuilder.create().texOffs(476, 149).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F), PartPose.offset(24.0F, 0.0F, 0.2F));
		PartDefinition p_halo_rune_1 = p_halo.addOrReplaceChild("halo_rune_1", CubeListBuilder.create().texOffs(486, 149).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(12.0F, 20.7846F, 0.2F, 0.0F, 0.0F, 1.0472F));
		PartDefinition p_halo_rune_2 = p_halo.addOrReplaceChild("halo_rune_2", CubeListBuilder.create().texOffs(496, 149).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-12.0F, 20.7846F, 0.2F, 0.0F, 0.0F, 2.0944F));
		PartDefinition p_halo_rune_3 = p_halo.addOrReplaceChild("halo_rune_3", CubeListBuilder.create().texOffs(0, 154).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-24.0F, 0.0F, 0.2F, 0.0F, 0.0F, 3.1416F));
		PartDefinition p_halo_rune_4 = p_halo.addOrReplaceChild("halo_rune_4", CubeListBuilder.create().texOffs(10, 154).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-12.0F, -20.7846F, 0.2F, 0.0F, 0.0F, 4.1888F));
		PartDefinition p_halo_rune_5 = p_halo.addOrReplaceChild("halo_rune_5", CubeListBuilder.create().texOffs(20, 154).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(12.0F, -20.7846F, 0.2F, 0.0F, 0.0F, 5.236F));
		PartDefinition p_arm_r = p_body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(384, 66).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 26.0F, 3.0F), PartPose.offsetAndRotation(-20.0F, -20.0F, -6.0F, -0.25F, 0.0F, 0.75F));
		PartDefinition p_forearm_r = p_arm_r.addOrReplaceChild("forearm_r", CubeListBuilder.create().texOffs(484, 99).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 20.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 25.0F, 0.0F, 0.1F, 0.0F, -0.9F));
		PartDefinition p_hand_r = p_forearm_r.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(454, 127).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 2.0F, 6.0F), PartPose.offset(0.0F, 19.0F, 0.0F));
		PartDefinition p_claw_r_0 = p_hand_r.addOrReplaceChild("claw_r_0", CubeListBuilder.create().texOffs(30, 154).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-1.6F, 1.0F, -3.0F, -0.6F, 0.0F, 0.0F));
		PartDefinition p_claw_r_1 = p_hand_r.addOrReplaceChild("claw_r_1", CubeListBuilder.create().texOffs(40, 154).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -3.0F, -0.6F, 0.0F, 0.0F));
		PartDefinition p_claw_r_2 = p_hand_r.addOrReplaceChild("claw_r_2", CubeListBuilder.create().texOffs(50, 154).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(1.6F, 1.0F, -3.0F, -0.6F, 0.0F, 0.0F));
		PartDefinition p_arm_l = p_body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(396, 66).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 26.0F, 3.0F), PartPose.offsetAndRotation(20.0F, -20.0F, -6.0F, -0.25F, 0.0F, -0.75F));
		PartDefinition p_forearm_l = p_arm_l.addOrReplaceChild("forearm_l", CubeListBuilder.create().texOffs(496, 99).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 20.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 25.0F, 0.0F, 0.1F, 0.0F, 0.9F));
		PartDefinition p_hand_l = p_forearm_l.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(476, 127).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 2.0F, 6.0F), PartPose.offset(0.0F, 19.0F, 0.0F));
		PartDefinition p_claw_l_0 = p_hand_l.addOrReplaceChild("claw_l_0", CubeListBuilder.create().texOffs(60, 154).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-1.6F, 1.0F, -3.0F, -0.6F, 0.0F, 0.0F));
		PartDefinition p_claw_l_1 = p_hand_l.addOrReplaceChild("claw_l_1", CubeListBuilder.create().texOffs(70, 154).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -3.0F, -0.6F, 0.0F, 0.0F));
		PartDefinition p_claw_l_2 = p_hand_l.addOrReplaceChild("claw_l_2", CubeListBuilder.create().texOffs(80, 154).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(1.6F, 1.0F, -3.0F, -0.6F, 0.0F, 0.0F));
		PartDefinition p_wing_r = p_body.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 127).addBox(-1.0F, -20.0F, -1.0F, 2.0F, 20.0F, 2.0F), PartPose.offsetAndRotation(-12.0F, -20.0F, 10.0F, -0.3F, -0.3F, -0.9F));
		PartDefinition p_wing_mem_r = p_wing_r.addOrReplaceChild("wing_mem_r", CubeListBuilder.create().texOffs(16, 127).addBox(-16.0F, -10.0F, -0.5F, 16.0F, 20.0F, 1.0F), PartPose.offset(0.0F, -10.0F, 0.5F));
		PartDefinition p_wing2_r = p_body.addOrReplaceChild("wing2_r", CubeListBuilder.create().texOffs(144, 127).addBox(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.offsetAndRotation(-18.0F, 4.0F, 10.0F, -0.4F, -0.4F, -1.4F));
		PartDefinition p_wing2_mem_r = p_wing2_r.addOrReplaceChild("wing2_mem_r", CubeListBuilder.create().texOffs(160, 127).addBox(-11.0F, -7.0F, -0.5F, 11.0F, 14.0F, 1.0F), PartPose.offset(0.0F, -7.0F, 0.5F));
		PartDefinition p_wing_l = p_body.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(8, 127).addBox(-1.0F, -20.0F, -1.0F, 2.0F, 20.0F, 2.0F), PartPose.offsetAndRotation(12.0F, -20.0F, 10.0F, -0.3F, 0.3F, 0.9F));
		PartDefinition p_wing_mem_l = p_wing_l.addOrReplaceChild("wing_mem_l", CubeListBuilder.create().texOffs(50, 127).addBox(0.0F, -10.0F, -0.5F, 16.0F, 20.0F, 1.0F), PartPose.offset(0.0F, -10.0F, 0.5F));
		PartDefinition p_wing2_l = p_body.addOrReplaceChild("wing2_l", CubeListBuilder.create().texOffs(152, 127).addBox(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.offsetAndRotation(18.0F, 4.0F, 10.0F, -0.4F, 0.4F, 1.4F));
		PartDefinition p_wing2_mem_l = p_wing2_l.addOrReplaceChild("wing2_mem_l", CubeListBuilder.create().texOffs(184, 127).addBox(0.0F, -7.0F, -0.5F, 11.0F, 14.0F, 1.0F), PartPose.offset(0.0F, -7.0F, 0.5F));
		PartDefinition p_spike_0 = p_body.addOrReplaceChild("spike_0", CubeListBuilder.create().texOffs(352, 127).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(-9.0F, -24.0F, 2.0F, -0.4F, 0.0F, 0.0F));
		PartDefinition p_spike_1 = p_body.addOrReplaceChild("spike_1", CubeListBuilder.create().texOffs(364, 127).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(-3.0F, -24.0F, 6.0F, -0.4F, 0.0F, 0.0F));
		PartDefinition p_spike_2 = p_body.addOrReplaceChild("spike_2", CubeListBuilder.create().texOffs(376, 127).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(3.0F, -24.0F, 2.0F, -0.4F, 0.0F, 0.0F));
		PartDefinition p_spike_3 = p_body.addOrReplaceChild("spike_3", CubeListBuilder.create().texOffs(388, 127).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(9.0F, -24.0F, 6.0F, -0.4F, 0.0F, 0.0F));
		PartDefinition p_plate_r = p_lobe_r.addOrReplaceChild("plate_r", CubeListBuilder.create().texOffs(300, 99).addBox(-9.0F, -12.0F, 0.0F, 18.0F, 24.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 13.0F));
		PartDefinition p_plate_l = p_lobe_l.addOrReplaceChild("plate_l", CubeListBuilder.create().texOffs(340, 99).addBox(-9.0F, -12.0F, 0.0F, 18.0F, 24.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 13.0F));
		PartDefinition p_cable_0 = p_body.addOrReplaceChild("cable_0", CubeListBuilder.create().texOffs(336, 127).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(-9.0F, 14.0F, 18.0F, 0.25F, 0.0F, 0.0F));
		PartDefinition p_cable_1 = p_body.addOrReplaceChild("cable_1", CubeListBuilder.create().texOffs(340, 127).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 14.0F, 18.0F, 0.25F, 0.0F, 0.0F));
		PartDefinition p_cable_2 = p_body.addOrReplaceChild("cable_2", CubeListBuilder.create().texOffs(344, 127).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(3.0F, 14.0F, 18.0F, 0.25F, 0.0F, 0.0F));
		PartDefinition p_cable_3 = p_body.addOrReplaceChild("cable_3", CubeListBuilder.create().texOffs(348, 127).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(9.0F, 14.0F, 18.0F, 0.25F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 512, 256);
	}

	public static LayerDefinition createSakeranLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition p_ = mesh.getRoot();
		PartDefinition p_root = p_.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition p_hips = p_root.addOrReplaceChild("hips", CubeListBuilder.create().texOffs(140, 52).addBox(-9.0F, -2.0F, -6.0F, 18.0F, 6.0F, 12.0F), PartPose.offset(0.0F, -26.0F, 0.0F));
		PartDefinition p_waist = p_hips.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(40, 30).addBox(-8.0F, -10.0F, -5.5F, 16.0F, 10.0F, 11.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
		PartDefinition p_chest = p_waist.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -16.0F, -7.0F, 24.0F, 16.0F, 14.0F).texOffs(216, 90).addBox(-11.0F, -14.0F, -9.0F, 10.0F, 8.0F, 2.0F).texOffs(0, 103).addBox(1.0F, -14.0F, -9.0F, 10.0F, 8.0F, 2.0F), PartPose.offset(0.0F, -9.0F, 0.0F));
		PartDefinition p_ruff = p_chest.addOrReplaceChild("ruff", CubeListBuilder.create().texOffs(0, 73).addBox(-10.0F, -3.0F, -6.0F, 20.0F, 5.0F, 12.0F).texOffs(64, 90).addBox(-12.0F, -2.0F, -2.0F, 24.0F, 4.0F, 8.0F), PartPose.offset(0.0F, -15.0F, 0.0F));
		PartDefinition p_neck = p_chest.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(136, 73).addBox(-4.5F, -5.0F, -4.5F, 9.0F, 6.0F, 9.0F), PartPose.offset(0.0F, -16.0F, -1.0F));
		PartDefinition p_head = p_neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(148, 0).addBox(-6.0F, -11.0F, -6.0F, 12.0F, 11.0F, 12.0F), PartPose.offset(0.0F, -4.0F, 0.0F));
		PartDefinition p_muzzle = p_head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(94, 30).addBox(-4.5F, -3.0F, -14.0F, 9.0F, 7.0F, 14.0F), PartPose.offsetAndRotation(0.0F, -5.0F, -5.0F, 0.42F, 0.0F, 0.0F));
		PartDefinition p_nose = p_muzzle.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(128, 90).addBox(-5.0F, -3.5F, -4.0F, 10.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -14.0F));
		PartDefinition p_jaw = p_muzzle.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 90).addBox(-4.0F, -1.0F, -11.0F, 8.0F, 2.0F, 11.0F), PartPose.offset(0.0F, 4.0F, -1.0F));
		PartDefinition p_ear_r = p_head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(196, 90).addBox(-1.5F, -9.0F, -1.0F, 3.0F, 9.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, -10.0F, 1.0F, 0.15F, 0.0F, 0.25F));
		PartDefinition p_cheek_r = p_head.addOrReplaceChild("cheek_r", CubeListBuilder.create().texOffs(156, 90).addBox(-2.0F, -3.0F, -3.0F, 4.0F, 6.0F, 6.0F), PartPose.offset(-6.5F, -3.0F, 1.0F));
		PartDefinition p_delt_r = p_chest.addOrReplaceChild("delt_r", CubeListBuilder.create().texOffs(140, 30).addBox(-5.5F, -5.0F, -5.5F, 11.0F, 10.0F, 11.0F), PartPose.offset(-13.0F, -12.0F, 0.0F));
		PartDefinition p_arm_r = p_delt_r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(76, 0).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 15.0F, 9.0F), PartPose.offsetAndRotation(-1.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.12F));
		PartDefinition p_forearm_r = p_arm_r.addOrReplaceChild("forearm_r", CubeListBuilder.create().texOffs(184, 30).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 13.0F, 8.0F), PartPose.offset(0.0F, 14.0F, 0.0F));
		PartDefinition p_paw_r = p_forearm_r.addOrReplaceChild("paw_r", CubeListBuilder.create().texOffs(64, 73).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 7.0F, 9.0F), PartPose.offset(0.0F, 13.0F, 0.0F));
		PartDefinition p_finger1_r = p_paw_r.addOrReplaceChild("finger1_r", CubeListBuilder.create().texOffs(24, 103).addBox(-1.2F, 0.0F, -1.2F, 2.0F, 6.0F, 2.0F), PartPose.offset(1.8F, 6.0F, -3.0F));
		PartDefinition p_finger2_r = p_paw_r.addOrReplaceChild("finger2_r", CubeListBuilder.create().texOffs(32, 103).addBox(-1.2F, 0.0F, -1.2F, 2.0F, 6.0F, 2.0F), PartPose.offset(-1.2F, 6.0F, -3.0F));
		PartDefinition p_claw1_r = p_finger1_r.addOrReplaceChild("claw1_r", CubeListBuilder.create().texOffs(56, 103).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition p_claw2_r = p_finger2_r.addOrReplaceChild("claw2_r", CubeListBuilder.create().texOffs(64, 103).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition p_thigh_r = p_hips.addOrReplaceChild("thigh_r", CubeListBuilder.create().texOffs(196, 0).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 12.0F, 10.0F), PartPose.offset(-5.0F, 3.0F, 0.0F));
		PartDefinition p_shin_r = p_thigh_r.addOrReplaceChild("shin_r", CubeListBuilder.create().texOffs(76, 52).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 11.0F, 8.0F), PartPose.offset(0.0F, 11.0F, 0.0F));
		PartDefinition p_foot_r = p_shin_r.addOrReplaceChild("foot_r", CubeListBuilder.create().texOffs(172, 73).addBox(-4.5F, 0.0F, -7.0F, 9.0F, 4.0F, 11.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
		PartDefinition p_ear_l = p_head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(206, 90).addBox(-1.5F, -9.0F, -1.0F, 3.0F, 9.0F, 2.0F), PartPose.offsetAndRotation(4.0F, -10.0F, 1.0F, 0.15F, 0.0F, -0.25F));
		PartDefinition p_cheek_l = p_head.addOrReplaceChild("cheek_l", CubeListBuilder.create().texOffs(176, 90).addBox(-2.0F, -3.0F, -3.0F, 4.0F, 6.0F, 6.0F), PartPose.offset(6.5F, -3.0F, 1.0F));
		PartDefinition p_delt_l = p_chest.addOrReplaceChild("delt_l", CubeListBuilder.create().texOffs(0, 52).addBox(-5.5F, -5.0F, -5.5F, 11.0F, 10.0F, 11.0F), PartPose.offset(13.0F, -12.0F, 0.0F));
		PartDefinition p_arm_l = p_delt_l.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(112, 0).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 15.0F, 9.0F), PartPose.offsetAndRotation(1.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.12F));
		PartDefinition p_forearm_l = p_arm_l.addOrReplaceChild("forearm_l", CubeListBuilder.create().texOffs(44, 52).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 13.0F, 8.0F), PartPose.offset(0.0F, 14.0F, 0.0F));
		PartDefinition p_paw_l = p_forearm_l.addOrReplaceChild("paw_l", CubeListBuilder.create().texOffs(100, 73).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 7.0F, 9.0F), PartPose.offset(0.0F, 13.0F, 0.0F));
		PartDefinition p_finger1_l = p_paw_l.addOrReplaceChild("finger1_l", CubeListBuilder.create().texOffs(40, 103).addBox(-1.2F, 0.0F, -1.2F, 2.0F, 6.0F, 2.0F), PartPose.offset(-1.8F, 6.0F, -3.0F));
		PartDefinition p_finger2_l = p_paw_l.addOrReplaceChild("finger2_l", CubeListBuilder.create().texOffs(48, 103).addBox(-1.2F, 0.0F, -1.2F, 2.0F, 6.0F, 2.0F), PartPose.offset(1.2F, 6.0F, -3.0F));
		PartDefinition p_claw1_l = p_finger1_l.addOrReplaceChild("claw1_l", CubeListBuilder.create().texOffs(72, 103).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition p_claw2_l = p_finger2_l.addOrReplaceChild("claw2_l", CubeListBuilder.create().texOffs(80, 103).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition p_thigh_l = p_hips.addOrReplaceChild("thigh_l", CubeListBuilder.create().texOffs(0, 30).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 12.0F, 10.0F), PartPose.offset(5.0F, 3.0F, 0.0F));
		PartDefinition p_shin_l = p_thigh_l.addOrReplaceChild("shin_l", CubeListBuilder.create().texOffs(108, 52).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 11.0F, 8.0F), PartPose.offset(0.0F, 11.0F, 0.0F));
		PartDefinition p_foot_l = p_shin_l.addOrReplaceChild("foot_l", CubeListBuilder.create().texOffs(212, 73).addBox(-4.5F, 0.0F, -7.0F, 9.0F, 4.0F, 11.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
		PartDefinition p_tail = p_hips.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(38, 90).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 0.7F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 256, 256);
	}
}
