package me.modmuss50.optifabric.compat.fix;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModMixinFixer {
	public static final ModMixinFixer INSTANCE = new ModMixinFixer();

	private final List<IMixinFixer> classFixes = new ArrayList<>();

	private ModMixinFixer() {
		addFixer(new ChunkRendererRegionMixinFixer());
		addFixer(new ModelLoaderBakerImplFixer());
	}

	public void addFixer(IMixinFixer fixer) {
		classFixes.add(fixer);
	}

	public List<IMixinFixer> getFixers(IMixinInfo mixin) {
		return classFixes.stream().filter(fixer -> fixer.getTargets().contains(mixin.getClassRef())).collect(Collectors.toList());
	}
}
