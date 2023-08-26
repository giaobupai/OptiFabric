package me.modmuss50.optifabric.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public interface IMixinFixer {
	void fix(IMixinInfo mixinInfo, ClassNode mixinNode);
}
