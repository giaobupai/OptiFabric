package me.modmuss50.optifabric.compat;

import java.util.*;

public class ModMixinFixer {
	public static final ModMixinFixer INSTANCE = new ModMixinFixer();

	private final Map<String, List<IMixinFixer>> classFixes = new HashMap<>();

	private ModMixinFixer() {
	}

	public void addFixer(String mixinClass, IMixinFixer fixer) {
		classFixes.computeIfAbsent(mixinClass, s -> new ArrayList<>()).add(fixer);
	}

	public List<IMixinFixer> getFixers(String className) {
		return classFixes.getOrDefault(className.replace('.', '/'), Collections.emptyList());
	}
}
