#!/usr/bin/env python3
"""ブスコハンターのリソース JSON を一括生成する。

  python3 gen_resources.py

出力先: buskohunter/src/main/resources と src/client/resources
（sounds.json は音声生成ツールが作るのでここでは触らない）
"""
import json
import os

ROOT = "/home/claude/buskohunter"
MAIN = ROOT + "/src/main/resources"
CLIENT = ROOT + "/src/client/resources"
A = MAIN + "/assets/buskohunter"
D = MAIN + "/data"
NS = "buskohunter"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


# ---------------------------------------------------------------------------
# 名前の一覧（Java の enum と一致させる）
# ---------------------------------------------------------------------------
WEAPONS = [  # id, 日本語, English, 三人称の大きさ
    ("great_sword", "大剣", "Great Sword", 1.75),
    ("long_sword", "太刀", "Long Sword", 1.6),
    ("sword_and_shield", "片手剣", "Sword and Shield", 1.0),
    ("dual_blades", "双剣", "Dual Blades", 1.0),
    ("hammer", "ハンマー", "Hammer", 1.6),
    ("hunting_horn", "狩猟笛", "Hunting Horn", 1.6),
    ("lance", "ランス", "Lance", 1.8),
    ("gunlance", "ガンランス", "Gunlance", 1.8),
    ("switch_axe", "スラッシュアックス", "Switch Axe", 1.6),
    ("charge_blade", "チャージアックス", "Charge Blade", 1.5),
    ("insect_glaive", "操虫棍", "Insect Glaive", 1.6),
    ("light_bowgun", "ライトボウガン", "Light Bowgun", 1.2),
    ("heavy_bowgun", "ヘビィボウガン", "Heavy Bowgun", 1.5),
    ("bow", "弓", "Bow", 1.3),
]
HUNT_ITEMS = [
    ("potion", "回復薬", "Potion"),
    ("mega_potion", "回復薬グレート", "Mega Potion"),
    ("max_potion", "いにしえの秘薬", "Max Potion"),
    ("antidote", "解毒薬", "Antidote"),
    ("energy_drink", "強走薬", "Energy Drink"),
    ("demondrug", "鬼人薬", "Demondrug"),
    ("armorskin", "硬化薬", "Armorskin"),
    ("ration", "携帯食料", "Ration"),
    ("well_done_steak", "こんがり肉", "Well-done Steak"),
    ("whetstone", "砥石", "Whetstone"),
    ("paintball", "ペイントボール", "Paintball"),
    ("flash_bomb", "閃光玉", "Flash Bomb"),
    ("shock_trap", "シビレ罠", "Shock Trap"),
    ("pitfall_trap", "落とし穴", "Pitfall Trap"),
    ("tranq_bomb", "捕獲用麻酔玉", "Tranq Bomb"),
    ("barrel_bomb", "大タル爆弾", "Large Barrel Bomb"),
    ("farcaster", "モドリ玉", "Farcaster"),
]
MONSTERS = [  # id, 日本語（二つ名 名前）, English
    ("sakeran", "声劇へ導く、酒池肉林 サケラン", "Sakeran"),
    ("barino", "凡百の突起物を消し ブスコバリーノ王", "King Buskobarino"),
    ("jangria", "様々造形を破壊せし ジャングリア", "Jangria"),
    ("busaiber", "POSE YOUR GAME ブサイバー", "Busaiber"),
    ("buskoba", "潤いはオアシスの笑み ブスコバ", "Buskoba"),
]
SHORT_JA = {"sakeran": "サケラン", "barino": "ブスコバリーノ王", "jangria": "ジャングリア", "busaiber": "ブサイバー", "buskoba": "ブスコバ"}
ARMOR = [
    ("hunter", "ハンター", "Hunter"),
    ("sakeran", "サケラン", "Sakeran"),
    ("barino", "バリーノ", "Barino"),
    ("jangria", "ジャングリア", "Jangria"),
    ("busaiber", "ブサイバー", "Busaiber"),
    ("buskoba", "ブスコバ", "Buskoba"),
]
PIECES = [("helmet", "ヘルム", "Helm"), ("chestplate", "メイル", "Mail"), ("leggings", "フォールド", "Coil"), ("boots", "グリーヴ", "Greaves")]
PROJECTILES = ["proj_bullet", "proj_arrow", "proj_sake_bottle", "proj_fireball", "proj_water_orb", "proj_missile",
               "proj_holy_orb", "proj_glitch_cube", "proj_kinsect", "proj_shell", "proj_sombrero"]
STATIONS = [  # id, 日本語, English, モデルの形
    ("quest_board", "クエストカウンター", "Quest Counter", ("column", "quest_board_front", "quest_board_side")),
    ("equipment_box", "装備ボックス", "Equipment Box", ("column", "equipment_box_side", "equipment_box_top")),
    ("canteen", "ブスコ食堂", "Busko Canteen", ("column", "canteen_side", "canteen_top")),
    ("item_box", "アイテムボックス", "Item Box", ("column", "item_box_side", "item_box_top")),
    ("departure_gate", "出発ゲート", "Departure Gate", ("all", "departure_gate")),
    ("supply_box", "支給品ボックス", "Supply Box", ("column", "supply_box_side", "supply_box_top")),
    ("hunter_bed", "ベッド", "Hunter Bed", ("column", "hunter_bed_side", "hunter_bed_top")),
    ("exit_gate", "帰還ゲート", "Exit Gate", ("all", "exit_gate")),
]

# ---------------------------------------------------------------------------
# アイテムのモデル
# ---------------------------------------------------------------------------
def weapon_display(scale, wid):
    bowgun = wid in ("light_bowgun", "heavy_bowgun")
    s = [scale] * 3
    if bowgun:
        tp = {"rotation": [0, 90, 0], "translation": [0, 2.5, 2.0], "scale": s}
        tpl = {"rotation": [0, -90, 0], "translation": [0, 2.5, 2.0], "scale": s}
        fp = {"rotation": [0, 90, 0], "translation": [1.0, 2.5, 0.5], "scale": [0.9, 0.9, 0.9]}
        fpl = {"rotation": [0, -90, 0], "translation": [1.0, 2.5, 0.5], "scale": [0.9, 0.9, 0.9]}
    else:
        lift = 4.0 + (scale - 1.0) * 5.0
        tp = {"rotation": [0, -90, 55], "translation": [0, round(lift, 2), 0.5], "scale": s}
        tpl = {"rotation": [0, 90, -55], "translation": [0, round(lift, 2), 0.5], "scale": s}
        fp = {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.85, 0.85, 0.85]}
        fpl = {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.85, 0.85, 0.85]}
    return {
        "thirdperson_righthand": tp,
        "thirdperson_lefthand": tpl,
        "firstperson_righthand": fp,
        "firstperson_lefthand": fpl,
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.7, 0.7, 0.7]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1.2, 1.2, 1.2]},
    }


def gen_items():
    for wid, ja, en, scale in WEAPONS:
        write(f"{A}/models/item/{wid}.json", {
            "parent": "minecraft:item/handheld",
            "textures": {"layer0": f"{NS}:item/{wid}", "layer1": f"{NS}:item/{wid}_overlay"},
            "display": weapon_display(scale, wid),
        })
        write(f"{A}/items/{wid}.json", {
            "model": {
                "type": "minecraft:model",
                "model": f"{NS}:item/{wid}",
                "tints": [
                    {"type": "minecraft:constant", "value": -1},
                    {"type": "minecraft:custom_model_data", "index": 0, "default": -1},
                ],
            },
        })
    flat = [i[0] for i in HUNT_ITEMS] + ["trophy_" + m[0] for m in MONSTERS] + PROJECTILES + ["hunter_book"]
    flat += [f"{a[0]}_{p[0]}" for a in ARMOR for p in PIECES]
    for iid in flat:
        write(f"{A}/models/item/{iid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{iid}"}})
        write(f"{A}/items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{iid}"}})
    return flat


def gen_blocks():
    for sid, ja, en, shape in STATIONS:
        if shape[0] == "all":
            model = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{shape[1]}"}}
        else:
            model = {"parent": "minecraft:block/cube_column",
                     "textures": {"side": f"{NS}:block/{shape[1]}", "end": f"{NS}:block/{shape[2]}"}}
        write(f"{A}/models/block/{sid}.json", model)
        write(f"{A}/blockstates/{sid}.json", {"variants": {"": {"model": f"{NS}:block/{sid}"}}})
        write(f"{A}/items/{sid}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{sid}"}})


def gen_equipment():
    for aid, ja, en in ARMOR:
        write(f"{A}/equipment/{aid}.json", {"layers": {
            "humanoid": [{"texture": f"{NS}:{aid}"}],
            "humanoid_leggings": [{"texture": f"{NS}:{aid}"}],
        }})


# ---------------------------------------------------------------------------
# データ（ディメンション・ダメージタイプ・タグ）
# ---------------------------------------------------------------------------
def noise_dim(biome, settings="minecraft:overworld"):
    return {"type": "minecraft:overworld", "generator": {
        "type": "minecraft:noise", "settings": settings,
        "biome_source": {"type": "minecraft:fixed", "biome": biome}}}


def flat_dim(biome, layers):
    return {"type": "minecraft:overworld", "generator": {
        "type": "minecraft:flat", "settings": {
            "biome": biome, "features": False, "lakes": False,
            "layers": [{"block": b, "height": h} for b, h in layers]}}}


def gen_data():
    dim = f"{D}/{NS}/dimension"
    write(f"{dim}/hub.json", flat_dim("minecraft:plains", [
        ("minecraft:bedrock", 1), ("minecraft:stone", 6), ("minecraft:dirt", 3), ("minecraft:grass_block", 1)]))
    write(f"{dim}/field_forest.json", noise_dim("minecraft:forest"))
    write(f"{dim}/field_desert.json", noise_dim("minecraft:desert"))
    write(f"{dim}/field_volcano.json", noise_dim("minecraft:badlands"))
    write(f"{dim}/field_sky.json", noise_dim("minecraft:cherry_grove"))
    write(f"{dim}/field_cyber.json", flat_dim("minecraft:the_void", [
        ("minecraft:bedrock", 1), ("minecraft:deepslate", 6), ("minecraft:polished_blackstone", 1)]))

    write(f"{D}/{NS}/damage_type/monster.json", {"message_id": f"{NS}.monster", "scaling": "never", "exhaustion": 0.0})
    tags = f"{D}/minecraft/tags/damage_type"
    for t in ("bypasses_armor", "no_knockback"):
        write(f"{tags}/{t}.json", {"replace": False, "values": [f"{NS}:monster"]})


# ---------------------------------------------------------------------------
# 言語ファイル
# ---------------------------------------------------------------------------
def gen_lang():
    ja, en = {}, {}
    ja[f"itemGroup.{NS}.main"] = "ブスコハンター"
    en[f"itemGroup.{NS}.main"] = "Busko Hunter"
    for wid, j, e, _ in WEAPONS:
        ja[f"item.{NS}.{wid}"] = j
        en[f"item.{NS}.{wid}"] = e
    for iid, j, e in HUNT_ITEMS:
        ja[f"item.{NS}.{iid}"] = j
        en[f"item.{NS}.{iid}"] = e
    for mid, j, e in MONSTERS:
        ja[f"item.{NS}.trophy_{mid}"] = SHORT_JA[mid] + "の討伐証"
        en[f"item.{NS}.trophy_{mid}"] = e + " Hunt Medal"
        ja[f"entity.{NS}.{mid}"] = j
        en[f"entity.{NS}.{mid}"] = e
    for aid, j, e in ARMOR:
        for pid, pj, pe in PIECES:
            ja[f"item.{NS}.{aid}_{pid}"] = f"{j}{pj}"
            en[f"item.{NS}.{aid}_{pid}"] = f"{e} {pe}"
    for p in PROJECTILES:
        ja[f"item.{NS}.{p}"] = "（弾）" + p[5:]
        en[f"item.{NS}.{p}"] = "Projectile: " + p[5:]
    ja[f"item.{NS}.hunter_book"] = "ハンターノート"
    en[f"item.{NS}.hunter_book"] = "Hunter's Notes"
    for sid, j, e, _ in STATIONS:
        ja[f"block.{NS}.{sid}"] = j
        en[f"block.{NS}.{sid}"] = e
    ents = {
        "training_dummy": ("訓練用ブスコ人形", "Training Dummy"),
        "monster_projectile": ("モンスターの飛び道具", "Monster Projectile"),
        "hunter_shot": ("弾・矢", "Hunter Shot"),
        "kinsect": ("猟虫", "Kinsect"),
        "hunt_trap": ("罠", "Trap"),
        "barrel_bomb": ("大タル爆弾", "Large Barrel Bomb"),
    }
    for k, (j, e) in ents.items():
        ja[f"entity.{NS}.{k}"] = j
        en[f"entity.{NS}.{k}"] = e
    keys = {
        "special": ("特殊アクション（R）", "Special Action"),
        "dodge": ("回避（左Alt）", "Dodge"),
        "menu": ("ハンターメニュー", "Hunter Menu"),
        "camera": ("視点切り替え（狩猟用）", "Hunting Camera"),
    }
    for k, (j, e) in keys.items():
        ja[f"key.{NS}.{k}"] = j
        en[f"key.{NS}.{k}"] = e
    # KeyMapping.Category.register(id) の翻訳キーは key.category.<namespace>.<path>
    ja[f"key.category.{NS}.main"] = "ブスコハンター"
    en[f"key.category.{NS}.main"] = "Busko Hunter"
    ja[f"death.attack.{NS}.monster"] = "%1$s は力尽きました"
    en[f"death.attack.{NS}.monster"] = "%1$s was carted"
    ja[f"death.attack.{NS}.monster.player"] = "%1$s は %2$s に倒されました"
    en[f"death.attack.{NS}.monster.player"] = "%1$s was carted by %2$s"
    ja.update(json.load(open("/home/claude/spec/subtitles_ja.json", encoding="utf-8")))
    en.update(json.load(open("/home/claude/spec/subtitles_en.json", encoding="utf-8")))
    write(f"{A}/lang/ja_jp.json", ja)
    write(f"{A}/lang/en_us.json", en)


# ---------------------------------------------------------------------------
# MOD メタデータ・ミックスイン
# ---------------------------------------------------------------------------
def gen_meta():
    write(f"{MAIN}/fabric.mod.json", {
        "schemaVersion": 1,
        "id": NS,
        "version": "${version}",
        "name": "Busko Hunter (ブスコハンター)",
        "description": "ブスコたちを狩るモンハン風ボス討伐 MOD。集会所でクエストを受注し、準備して、広大なフィールドで大型モンスターを狩る。14 種の武器、回避・武器ごとのモーション、部位破壊、怒り・疲労・エリア移動・捕獲、マルチプレイ対応。",
        "authors": ["ブスコハンター制作チーム"],
        "license": "All-Rights-Reserved",
        "icon": f"assets/{NS}/icon.png",
        "environment": "*",
        "entrypoints": {
            "main": ["com.buskohunter.BuskoHunter"],
            "client": ["com.buskohunter.client.BuskoHunterClient"],
        },
        "mixins": [{"config": "buskohunter.client.mixins.json", "environment": "client"}],
        "depends": {
            "fabricloader": ">=0.20.1",
            "minecraft": "~26.3",
            "java": ">=25",
            "fabric-api": "*",
        },
    })
    write(f"{CLIENT}/buskohunter.client.mixins.json", {
        "required": True,
        "package": "com.buskohunter.client.mixin",
        "compatibilityLevel": "JAVA_25",
        "client": ["AvatarRendererMixin", "HumanoidModelMixin"],
        "injectors": {"defaultRequire": 1},
    })


if __name__ == "__main__":
    flat = gen_items()
    gen_blocks()
    gen_equipment()
    gen_data()
    gen_lang()
    gen_meta()
    # テクスチャの存在確認
    missing = []
    tex = A + "/textures"
    for wid, *_ in WEAPONS:
        for suf in ("", "_overlay"):
            if not os.path.exists(f"{tex}/item/{wid}{suf}.png"):
                missing.append(f"item/{wid}{suf}")
    for iid in flat:
        if not os.path.exists(f"{tex}/item/{iid}.png"):
            missing.append(f"item/{iid}")
    for sid, j, e, shape in STATIONS:
        for t in shape[1:]:
            if not os.path.exists(f"{tex}/block/{t}.png"):
                missing.append(f"block/{t}")
    for aid, *_ in ARMOR:
        for layer in ("humanoid", "humanoid_leggings"):
            if not os.path.exists(f"{tex}/entity/equipment/{layer}/{aid}.png"):
                missing.append(f"entity/equipment/{layer}/{aid}")
    print("missing textures:", missing or "none")
