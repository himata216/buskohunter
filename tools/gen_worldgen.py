#!/usr/bin/env python3
"""狩猟フィールドのディメンション・バイオーム・地物（データパック）を生成する。

  python3 tools/gen_worldgen.py

- フィールドは平らな地形（flat）。水・湖・洞窟・構造物（村・井戸・ダンジョン）は無し。
- 植生はフィールド専用のバイオームに書いた地物だけ（木・草・花・サボテン・岩）。
- 敵対モブは湧かない（バイオームに monster の湧きを書かない）。
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
D = os.path.join(ROOT, "src/main/resources/data/buskohunter")
NS = "buskohunter"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def weighted(*pairs):
    return {"type": "minecraft:weighted_list", "distribution": [{"data": d, "weight": w} for d, w in pairs]}


def tree_feature(name, configured, count, sapling=None):
    placement = [
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
    ]
    if sapling:
        placement.append({"type": "minecraft:block_predicate_filter",
                          "predicate": {"type": "minecraft:would_survive", "state": sapling}})
    placement.append({"type": "minecraft:biome"})
    write(f"{D}/worldgen/placed_feature/{name}.json", {"feature": configured, "placement": placement})


def rock_feature(name, configured, chance):
    write(f"{D}/worldgen/placed_feature/{name}.json", {"feature": configured, "placement": [
        {"type": "minecraft:rarity_filter", "chance": chance},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"},
    ]})


def creature(entity, weight, count):
    return {"type": entity, "count": count, "weight": weight}


def biome(name, temp, downfall, sky, vegetation, creatures, grass=None, foliage=None, music=None, precipitation=True, extra_attr=None):
    attrs = {
        "minecraft:gameplay/natural_mob_spawns": {
            "argument": {"spawn_costs": {}, "spawns_by_category": {"creature": creatures} if creatures else {}},
            "modifier": "overlay",
        },
        "minecraft:visual/sky_color": sky,
    }
    if music:
        attrs["minecraft:audio/background_music"] = {"default": {"max_delay": 24000, "min_delay": 12000, "sound": music}}
    if extra_attr:
        attrs.update(extra_attr)
    effects = {"water_color": "#3f76e4"}
    if grass:
        effects["grass_color"] = grass
    if foliage:
        effects["foliage_color"] = foliage
    features = [[] for _ in range(11)]
    features[9] = vegetation  # VEGETAL_DECORATION
    write(f"{D}/worldgen/biome/{name}.json", {
        "attributes": attrs,
        "carvers": [],
        "downfall": downfall,
        "effects": effects,
        "features": features,
        "has_precipitation": precipitation,
        "temperature": temp,
    })


def flat_dimension(path, biome_id, layers, features=True):
    write(f"{D}/dimension/{path}.json", {
        "type": "minecraft:overworld",
        "generator": {
            "type": "minecraft:flat",
            "settings": {
                "biome": biome_id,
                "features": features,
                "lakes": False,
                "structure_overrides": [],
                "layers": [{"block": b, "height": h} for b, h in layers],
            },
        },
    })


def main():
    # ---- 地物 ----
    tree_feature("hunt_forest_trees", "minecraft:trees_birch_and_oak_leaf_litter", weighted((1, 5), (2, 4), (3, 1)))
    tree_feature("hunt_sky_trees", "minecraft:cherry_bees_005", weighted((0, 2), (1, 5), (2, 3)), "minecraft:cherry_sapling")
    tree_feature("hunt_volcano_trees", "minecraft:trees_badlands", weighted((0, 3), (1, 1)), "minecraft:oak_sapling")
    tree_feature("hunt_desert_trees", "minecraft:trees_savanna", weighted((0, 6), (1, 1)), "minecraft:acacia_sapling")
    rock_feature("hunt_forest_rock", "minecraft:forest_rock", 4)

    # ---- バイオーム ----
    biome("hunt_forest", 0.7, 0.8, "#79a6ff", [
        f"{NS}:hunt_forest_trees", f"{NS}:hunt_forest_rock",
        "minecraft:forest_flowers", "minecraft:flower_default", "minecraft:patch_bush",
        "minecraft:patch_grass_forest", "minecraft:patch_tall_grass_2", "minecraft:patch_leaf_litter", "minecraft:patch_pumpkin",
    ], [creature("minecraft:sheep", 12, 3), creature("minecraft:cow", 8, 3), creature("minecraft:chicken", 6, 3)],
        music="minecraft:music.overworld.forest")
    biome("hunt_desert", 2.0, 0.0, "#6eb1ff", [
        f"{NS}:hunt_desert_trees",
        "minecraft:patch_dry_grass_desert", "minecraft:patch_dead_bush_2", "minecraft:patch_cactus_desert",
    ], [creature("minecraft:rabbit", 4, 2)], music="minecraft:music.overworld.desert", precipitation=False,
        extra_attr={"minecraft:gameplay/snow_golem_melts": True})
    biome("hunt_volcano", 2.0, 0.0, "#e08a5a", [
        f"{NS}:hunt_volcano_trees",
        "minecraft:patch_dry_grass_badlands", "minecraft:patch_dead_bush_badlands", "minecraft:patch_grass_badlands",
    ], [], grass="#90814d", foliage="#9e814d", music="minecraft:music.overworld.badlands", precipitation=False,
        extra_attr={"minecraft:gameplay/snow_golem_melts": True})
    biome("hunt_sky", 0.5, 0.8, "#9ec2ff", [
        f"{NS}:hunt_sky_trees",
        "minecraft:flower_cherry", "minecraft:patch_grass_plain", "minecraft:patch_tall_grass_2", "minecraft:flower_default",
    ], [creature("minecraft:rabbit", 4, 2), creature("minecraft:sheep", 4, 2)], grass="#b6db61", foliage="#b6db61",
        music="minecraft:music.overworld.cherry_grove")

    # ---- ディメンション（新しい平地のフィールド） ----
    soil = [("minecraft:bedrock", 1), ("minecraft:stone", 6)]
    flat_dimension("forest_field", f"{NS}:hunt_forest", soil + [("minecraft:dirt", 3), ("minecraft:grass_block", 1)])
    flat_dimension("desert_field", f"{NS}:hunt_desert", soil + [("minecraft:sandstone", 3), ("minecraft:sand", 2)])
    flat_dimension("volcano_field", f"{NS}:hunt_volcano", soil + [("minecraft:terracotta", 2), ("minecraft:coarse_dirt", 1)])
    flat_dimension("sky_field", f"{NS}:hunt_sky", soil + [("minecraft:dirt", 3), ("minecraft:grass_block", 1)])
    # 電脳廃都・集会所（地物なし）
    flat_dimension("field_cyber", "minecraft:the_void", [("minecraft:bedrock", 1), ("minecraft:deepslate", 6), ("minecraft:polished_blackstone", 1)], features=False)
    flat_dimension("hub", "minecraft:the_void", [("minecraft:bedrock", 1), ("minecraft:stone", 6), ("minecraft:dirt", 3), ("minecraft:grass_block", 1)], features=False)
    # 以前の起伏のあるフィールド（もう使わない）。古いワールドで読み込めるよう、空の平地として残す
    for old in ("field_forest", "field_desert", "field_volcano", "field_sky"):
        flat_dimension(old, "minecraft:the_void", [("minecraft:bedrock", 1)], features=False)
    print("ok")


if __name__ == "__main__":
    main()
