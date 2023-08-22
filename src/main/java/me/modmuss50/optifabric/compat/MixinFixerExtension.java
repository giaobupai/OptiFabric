package me.modmuss50.optifabric.compat;

import me.modmuss50.optifabric.util.MixinInternals;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class MixinFixerExtension implements IExtension {
	private static final Set<ClassNode> PREPARED_MIXINS = Collections.newSetFromMap(new WeakHashMap<>());

	@Override
	public boolean checkActive(MixinEnvironment environment) {
		return true;
	}

	@Override
	public void preApply(ITargetClassContext context) {
		for (Pair<IMixinInfo, ClassNode> pair : MixinInternals.getMixinsFor(context)) {
			prepareMixin(pair.getLeft(), pair.getRight());
		}
	}

	@Override
	public void postApply(ITargetClassContext context) {

	}

	@Override
	public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {

	}

	private static void prepareMixin(IMixinInfo mixinInfo, ClassNode mixinNode) {
		if (PREPARED_MIXINS.contains(mixinNode)) {
			// Don't scan the whole class again.
			return;
		}
		ModMixinFixer.INSTANCE.getFixers(mixinInfo.getClassName()).forEach(transformer -> transformer.fix(mixinInfo, mixinNode));
		PREPARED_MIXINS.add(mixinNode);
	}
}
