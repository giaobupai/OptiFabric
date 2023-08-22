package me.modmuss50.optifabric.compat;

import me.modmuss50.optifabric.util.MixinInternals;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class OptifabricMixinPlugin extends EmptyMixinPlugin {
	@Override
	public void onLoad(String mixinPackage) {
		MixinInternals.registerExtension(new MixinFixerExtension());
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
