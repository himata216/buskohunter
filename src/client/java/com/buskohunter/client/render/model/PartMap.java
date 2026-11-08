package com.buskohunter.client.render.model;

import net.minecraft.client.model.geom.ModelPart;

import java.util.HashMap;
import java.util.Map;

/** 生成されたパーツパスから名前 → ModelPart の表を作る。 */
public final class PartMap {
	private final Map<String, ModelPart> parts = new HashMap<>();

	public PartMap(ModelPart meshRoot, String[][] paths) {
		for (String[] path : paths) {
			ModelPart p = meshRoot;
			for (String name : path) {
				p = p.getChild(name);
			}
			parts.put(path[path.length - 1], p);
		}
	}

	public ModelPart get(String name) {
		ModelPart p = parts.get(name);
		if (p == null) throw new IllegalArgumentException("No model part " + name);
		return p;
	}

	public boolean has(String name) {
		return parts.containsKey(name);
	}
}
