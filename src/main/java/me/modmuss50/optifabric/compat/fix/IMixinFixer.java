package me.modmuss50.optifabric.compat.fix;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;

public interface IMixinFixer {
	List<String> getTargets();
	void fix(IMixinInfo mixinInfo, ClassNode mixinNode);
	default boolean shouldUpdateClassInfo() {
		return false;
	}
}
