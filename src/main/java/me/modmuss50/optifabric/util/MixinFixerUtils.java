package me.modmuss50.optifabric.util;

import me.modmuss50.optifabric.mod.OptifabricSetup;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.util.Annotations;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

public class MixinFixerUtils {
	public static IntSupplier getIndexCI(MethodNode method, boolean afterSequence, String... sequence) {
		return getIndex(method, true, afterSequence, toTypeList(sequence));
	}

	public static IntSupplier getIndex(MethodNode method, boolean afterSequence, String... sequence) {
		return getIndex(method, false, afterSequence, toTypeList(sequence));
	}

	private static IntSupplier getIndex(MethodNode method, boolean afterCallback, boolean afterSequence, List<Type> sequence) {
		return () -> {
			Type[] desc = Type.getArgumentTypes(method.desc);
			int offset = 0;
			if (afterCallback) {
				for (Type type : desc) {
					offset++;
					if (type.toString().startsWith("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo")) {
						break;
					}
				}
			}
			on: for(int i = offset; i < desc.length - sequence.size() + 1; i++) {
				for(int j = 0; j < sequence.size(); j++) {
					if (!desc[i+j].equals(sequence.get(j))) {
						continue on;
					}
				}
				if (afterSequence) {
					i += sequence.size();
				}
				return i;
			}
			return -1;
		};
	}

	public static void addParams(MethodNode method, IMixinInfo mixinInfo, String... params) {
		insertParams(method, mixinInfo, Type.getArgumentTypes(method.desc).length, toTypeList(params));
	}

	public static void insertParams(MethodNode method, IMixinInfo mixinInfo, IntSupplier index, String... params) {
		insertParams(method, mixinInfo, index.getAsInt(), toTypeList(params));
	}

	public static void insertParams(MethodNode method, IMixinInfo mixinInfo, int index, List<Type> params) {
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

	public static void placeSurrogate(ClassNode mixinNode, IMixinInfo mixinInfo, String name, String desc, int[] params) {
		Optional<MethodNode> optional = mixinNode.methods.stream()
				.filter(meth -> name.equals(meth.name) && Annotations.getVisible(meth, Inject.class) != null)
				.findFirst();
		if (!optional.isPresent()) {
			OptifabricSetup.LOGGER.warn("Could not find injector method {} in {}, skipping", name, mixinInfo);
			return;
		}
		MethodNode method = optional.get();
		MethodNode surrogate = new MethodNode(method.access, method.name, desc, null, method.exceptions.toArray(new String[0]));
		Annotations.setVisible(surrogate, Surrogate.class);
		Type[] args = Type.getArgumentTypes(desc);
		for (int i : params) {
			surrogate.instructions.add(new VarInsnNode(args[i].getOpcode(Opcodes.IALOAD), i));
		}
		boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
		boolean isPrivate = (method.access & Opcodes.ACC_PRIVATE) != 0;
		int invokeOpcode = isStatic ? Opcodes.INVOKESTATIC : isPrivate ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
		surrogate.instructions.add(new MethodInsnNode(invokeOpcode, mixinInfo.getClassRef(), method.name, method.desc));
		surrogate.instructions.add(new InsnNode(Opcodes.RETURN));
	}

	private static List<Type> toTypeList(String[] types) {
		return Arrays.stream(types).map(Type::getType).collect(Collectors.toList());
	}
}
