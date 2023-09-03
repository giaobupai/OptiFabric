package me.modmuss50.optifabric.compat.fix;

import com.google.common.collect.Lists;
import me.modmuss50.optifabric.util.ASMUtils;
import me.modmuss50.optifabric.util.MixinInternals;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

public class ModMixinFixer {
	public static final ModMixinFixer INSTANCE = new ModMixinFixer();

	private final List<IMixinFixer> classFixes = new ArrayList<>();

	private ModMixinFixer() {
		addFixer(new ModelLoaderBakerImplFixer());
	}

	public void addFixer(IMixinFixer fixer) {
		classFixes.add(fixer);
	}

	public List<IMixinFixer> getFixers(String className) {
		return classFixes.stream().filter(fixer -> fixer.getTargets().contains(className.replace('.', '/'))).collect(Collectors.toList());
	}

	static IntSupplier getIndexCI(MethodNode method, boolean afterSequence, String... sequence) {
		return getIndex(method, true, afterSequence, String.join("", sequence));
	}

	static IntSupplier getIndex(MethodNode method, boolean afterSequence, String... sequence) {
		return getIndex(method, false, afterSequence, String.join("", sequence));
	}

	private static IntSupplier getIndex(MethodNode method, boolean afterCallback, boolean afterSequence, String sequence) {
		return () -> {
			String desc = method.desc;
			int offset = 0;
			if (afterCallback) {
				List<Type> params = Lists.newArrayList(Type.getArgumentTypes(desc));
				for (Type type : params) {
					offset++;
					if (type.toString().startsWith("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo")) {
						break;
					}
				}
				desc = params.subList(offset, params.size()).stream().map(Type::toString).collect(Collectors.joining(""));
			}
			int index = desc.indexOf(sequence);
			if (index == -1) {
				return -1;
			}
			desc = afterSequence ? desc.substring(index) : desc.substring(0, index);
			if (!desc.contains("(")) desc = "(" + desc;
			if (!desc.contains(")")) desc = desc + ")V";
			return Type.getArgumentTypes(desc).length + offset;
		};
	}

	static void addParams(MethodNode method, IMixinInfo mixinInfo, String... params) {
		insertParams(method, mixinInfo, Type.getArgumentTypes(method.desc).length, toTypeList(params));
	}

	static void insertParams(MethodNode method, IMixinInfo mixinInfo, IntSupplier index, String... params) {
		insertParams(method, mixinInfo, index.getAsInt(), toTypeList(params));
	}

	static void insertParams(MethodNode method, IMixinInfo mixinInfo, int index, List<Type> params) {
		List<Type> newDesc = Arrays.stream(Type.getArgumentTypes(method.desc)).collect(Collectors.toList());
		newDesc.addAll(index, params);
		int shiftBy = 0;
		int lvIndex = 0;
		for (Type param : params) {
			shiftBy += param.getSize();
		}
		method.maxLocals += shiftBy;

		for (int i = 0; i < params.size(); i++) {
			method.parameters.add(index + i, new ParameterNode("syn_" + i, Opcodes.ACC_SYNTHETIC));
		}

		if (!Modifier.isStatic(method.access)) lvIndex++;

		for (int i = 0; i < index; i++) {
			lvIndex += newDesc.get(i).getSize();
		}

		//shift locals (not mandatory)
		for (LocalVariableNode local : method.localVariables) {
			if (local.index >= lvIndex) {
				local.index += shiftBy;
			}
		}
		//shift instructions
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof VarInsnNode && ((VarInsnNode) insn).var >= lvIndex) {
				((VarInsnNode) insn).var += shiftBy;
			} else if (insn instanceof IincInsnNode && ((IincInsnNode) insn).var >= lvIndex) {
				((IincInsnNode) insn).var += shiftBy;
			}
		}

		ClassInfo info = MixinInternals.getClassInfoFor(mixinInfo);
		Set<ClassInfo.Method> methods = MixinInternals.getClassInfoMethods(info);
		methods.removeIf(meth -> method.name.equals(meth.getOriginalName()) && method.desc.equals(meth.getOriginalDesc()));
		method.desc = Type.getMethodDescriptor(Type.getReturnType(method.desc), newDesc.toArray(new Type[0]));
		methods.add(info.new Method(method, true));
	}

	private static List<Type> toTypeList(String[] types) {
		return Arrays.stream(types).map(Type::getType).collect(Collectors.toList());
	}
}
