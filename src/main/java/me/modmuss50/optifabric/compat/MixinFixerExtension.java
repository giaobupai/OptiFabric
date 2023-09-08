package me.modmuss50.optifabric.compat;

import me.modmuss50.optifabric.compat.fix.IMixinFixer;
import me.modmuss50.optifabric.compat.fix.ModMixinFixer;
import me.modmuss50.optifabric.mod.OptifabricError;
import me.modmuss50.optifabric.mod.OptifabricSetup;
import me.modmuss50.optifabric.util.MixinInternals;
import me.modmuss50.optifabric.util.MixinUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.*;
import java.util.stream.Collectors;

public class MixinFixerExtension implements IExtension {
	private static final Set<ClassNode> PRE_MIXINS = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Set<ClassNode> POST_MIXINS = Collections.newSetFromMap(new WeakHashMap<>());
	private static final HashMap<String, Boolean> CLASS_INFO_UPDATES = new HashMap<>();

	@Override
	public boolean checkActive(MixinEnvironment environment) {
		return true;
	}

	@Override
	public void preApply(ITargetClassContext context) {
		if (OptifabricError.hasError()) return;
		for (Pair<IMixinInfo, ClassNode> pair : MixinInternals.getMixinsFor(context)) {
			prepareMixin(pair.getLeft(), pair.getRight());
		}
		updateClassInfo(context.getClassInfo(), context.getClassNode());
	}

	@Override
	public void postApply(ITargetClassContext context) {
		for (Pair<IMixinInfo, ClassNode> pair : MixinInternals.getMixinsFor(context)) {
			handleErrorInjectors(pair.getLeft(), pair.getRight(), context);
		}
	}

	@Override
	public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {

	}

	private static void prepareMixin(IMixinInfo mixinInfo, ClassNode mixinNode) {
		if (PRE_MIXINS.contains(mixinNode)) {
			// Don't scan the whole class again.
			return;
		}
		List<IMixinFixer> fixers = ModMixinFixer.INSTANCE.getFixers(mixinInfo.getClassName());
		fixers.forEach(fixer -> fixer.fix(mixinInfo, mixinNode));
		if (fixers.stream().anyMatch(IMixinFixer::shouldUpdateClassInfo)) {
			mixinInfo.getTargetClasses().forEach(target -> CLASS_INFO_UPDATES.put(target, false));
		}
		PRE_MIXINS.add(mixinNode);
	}

	public void updateClassInfo(ClassInfo info, ClassNode node) {
		if (!CLASS_INFO_UPDATES.getOrDefault(info.getName(), true)) {
			MixinUtils.completeClassInfo(info, node.methods);
			CLASS_INFO_UPDATES.put(info.getName(), true);
		}
	}

	//this could use some refactoring
	private static void handleErrorInjectors(IMixinInfo mixinInfo, ClassNode mixinNode, ITargetClassContext context) {
		if (POST_MIXINS.contains(mixinNode)) {
			return;
		}
		ClassNode classNode = context.getClassNode();

		List<String> methods = classNode.methods.stream().map(method -> method.name).collect(Collectors.toList());
		//check for error methods
		for (MethodNode method : classNode.methods) {
			if (method.name.endsWith("$missing") && methods.stream().anyMatch(name -> (name + "$missing").equals(method.name))) {
				for (AbstractInsnNode insn : method.instructions) {
					if (insn instanceof LdcInsnNode) {
						String error = (String) ((LdcInsnNode) insn).cst;
						if (!OptifabricError.hasError()) {
							OptifabricError.setError(new InjectionError(error), getError(mixinInfo, method));
							OptifabricError.modError = true;
						}
						break;
					}
				}
				OptifabricSetup.LOGGER.warn("Removed InjectionException from Error Injector method " + method.name);
				method.instructions.clear();
				method.instructions.add(new InsnNode(Opcodes.RETURN));
			}
		}
		POST_MIXINS.add(mixinNode);
	}

	private static String getError(IMixinInfo mixinInfo, MethodNode method) {
		boolean compat = !ModMixinFixer.INSTANCE.getFixers(mixinInfo.getClassName()).isEmpty();
		return String.format("Injector method %s in %s couldn't apply due to " +
				(compat ? "outdated compatibility patch" : "missing compatibility patch!") +
				" Please report this issue.",
				method.name, mixinInfo);
	}
}
