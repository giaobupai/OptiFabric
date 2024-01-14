package me.modmuss50.optifabric.compat;

import com.google.common.collect.Lists;
import me.modmuss50.optifabric.compat.fix.IMixinFixer;
import me.modmuss50.optifabric.patcher.fixes.ClassFixer;
import me.modmuss50.optifabric.util.MixinInternals;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.util.Annotations;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An abstraction over raw ASM that aids in patching the mixins of
 * other mods to work with OptiFine. Rather than allowing mixin fixers
 * direct access to the mixin's {@link ClassNode}, a {@link Consumer} of
 * this type is passed to {@link IMixinFixer#fix} in order to provide
 * a standardized way of patching Injectors and to avoid repetitive or
 * "bad" code.
 */
public class MixinNodeTransformer {
	final IMixinInfo mixinInfo;
	private final ClassNode mixinNode;
	private final ClassInfo mixinClassInfo;
	private final Set<ClassInfo.Method> methods;
	private boolean updateClassInfo = false;

	public MixinNodeTransformer(IMixinInfo mixinInfo, ClassNode mixinNode) {
		this.mixinInfo = mixinInfo;
		this.mixinNode = mixinNode;
		mixinClassInfo = MixinInternals.getClassInfoFor(mixinInfo);
		methods = MixinInternals.getClassInfoMethods(mixinClassInfo);
	}

	/**
	 * Transforms all methods with the given name.
	 * @param name name of the methods to be transformed
	 * @param transformer transformation to be performed
	 */
	public void transformMethods(String name, Consumer<MethodTransformer> transformer) {
		transformMethods(method -> name.equals(method.name), transformer);
	}

	/**
	 * Transforms all methods with the given names.
	 * @param names names of the methods to be transformed
	 * @param transformer transformation to be performed
	 */
	public void transformMethods(List<String> names, Consumer<MethodTransformer> transformer) {
		transformMethods(method -> names.contains(method.name), transformer);
	}

	/**
	 * Transforms all methods matching the filter.
	 * @param filter name of the methods to be transformed
	 * @param transformer predicate which returns true for
	 *                    methods to be transformed
	 */
	public void transformMethods(Predicate<MethodNode> filter, Consumer<MethodTransformer> transformer) {
		mixinNode.methods.stream().filter(filter).forEach(method -> {
			MethodTransformer methodTransformer = new MethodTransformer(method);
			transformer.accept(methodTransformer);
			if (methodTransformer.requiresUpdate) {
				methods.removeIf(meth -> method.name.equals(meth.getOriginalName()) && method.desc.equals(meth.getOriginalDesc()));
				methods.add(mixinClassInfo.new Method(method, true));
			}
		});
	}

	/**
	 * An abstraction over raw ASM that aids in patching
	 * methods from another mod's mixin.
	 */
	public class MethodTransformer {
		final MethodNode method;
		boolean requiresUpdate = false;

		public MethodTransformer(MethodNode method) {
			this.method = method;
		}

		/**
		 * Transforms an Injector annotation. May only be used
		 * when the method being transformed is an Injector.
		 * @param transformer transformation to be performed
		 */
		public void transformInjector(Consumer<InjectorTransformer> transformer) {
			transformer.accept(new InjectorTransformer(InjectionInfo.getInjectorAnnotation(mixinInfo, method)));
		}

		/**
		 * An abstraction over raw ASM that aids in patching
		 * the injector annotation of methods from another mod's mixin.
		 */
		public class InjectorTransformer {
			private final AnnotationNode injector;

			public InjectorTransformer(AnnotationNode injector) {
				this.injector = injector;
			}

			/**
			 * Sets the {@code method} key of the injector. The new target selector
			 * <b>should be</b> added by OptiFine, as this is the only
			 * legitimate use case of this method. As such, the {@link ClassInfo}
			 * of this mixin is marked for updating.
			 * @param method the names of the new method(s). The
			 *               descriptor should also be appended to the name
			 *               in order to avoid ambiguity, especially in a
			 *               development environment.
			 */
			public void setMethod(String... method) {
				Annotations.setValue(injector, "method", Arrays.asList(method));
				Annotations.setValue(injector, "remap", false);
				updateClassInfo = true;
			}

			/**
			 * Sets the {@code at} key of the injector. If the
			 * injector takes an array of {@link At}s the array
			 * will be replaced with one that contains only the
			 * given {@code at}. Use only if a {@link ClassFixer}
			 * can't (or shouldn't) be used.
			 * @param atNode {@link AtNode} that constructs an {@link At}
			 *             annotation from the given {@code value} and {@code target}
			 */
			public void setAt(AtNode atNode) {
				Object at = Annotations.getValue(injector, "at");
				if (at instanceof AnnotationNode) {
					Annotations.setValue(injector, "at", atNode.at);
				} else if (at instanceof Collection) {
					Annotations.setValue(injector, "at", Collections.singletonList(atNode.at));
				}
			}

			/**
			 * Sets the {@code ordinal} key of the injector's {@code at}. Preferably
			 * used in conjunction with {@link #setSlice} in order to minimize the
			 * number of fixes needed for different versions. Note that this
			 * <b>does not</b> work with {@link Inject#at} since it's an array,
			 * instead of a single {@link At}.
			 * @param ordinal the new ordinal
			 */
			public void setAtOrdinal(int ordinal) {
				AnnotationNode at = Annotations.getValue(injector, "at");
				Annotations.setValue(at, "ordinal", ordinal);
			}

			/**
			 * Sets the {@code slice} key of the injector. Only takes
			 * the {@code from} argument.
			 * @param from {@link AtNode} that constructs an {@link At}
			 *             annotation from the given {@code value} and {@code target}
			 */
			public void setSlice(AtNode from) {
				AnnotationNode slice = new AnnotationNode(Slice.class.getName());
				Annotations.setValue(slice, "from", from.at);
				Annotations.setValue(injector, "slice", slice);
			}

			/**
			 * Same as {@link #setSlice(AtNode)}, but also takes
			 * the {@code to} argument.
			 * @param from the {@code from} of the {@link Slice}
			 * @param to the {@code to} of the {@link Slice}
			 * @see #setSlice
			 */
			public void setSlice(AtNode from, AtNode to) {
				AnnotationNode slice = new AnnotationNode(Slice.class.getName());
				Annotations.setValue(slice, "from", from.at);
				Annotations.setValue(slice, "to", to.at);
				Annotations.setValue(injector, "slice", slice);
			}
		}

		/**
		 * Lazily gets the index of the first sequence matching the specified
		 * one in the method parameters. If the is not found, the supplied
		 * value will be {@code -1}, causing an {@link IndexOutOfBoundsException}
		 * when used as the index argument of {@link #addParams} or
		 * {@link #removeParam}, which in turn will crash the game.
		 * @param afterSequence whether to return the index of the parameter
		 *                      right after the sequence. The absence of such
		 *                      a parameter does not cause issues when adding
		 *                      parameters, as they will be added at the tail.
		 *                      Setting this to {@code false} will return the
		 *                      index of the first element in the sequence.
		 * @param sequence the sequence of parameters to look for. Must be the
		 *                 internal names of the types.
		 * @return an {@link IntSupplier} that retrieves the index when needed.
		 * If no matching sequence is found, the supplied value will be {@code -1}.
		 */
		public IntSupplier getIndex(boolean afterSequence, String... sequence) {
			return getIndex(false, afterSequence, toTypeList(sequence));
		}

		/**
		 * Lazily gets the index of the first sequence matching the specified
		 * one in the method parameters, but only looks for the sequence
		 * after the first (and preferably only) parameter of type {@link CallbackInfo} or
		 * {@link CallbackInfoReturnable}.
		 * @param afterSequence whether to get the index after the
		 *                      sequence or where the sequence itself is found
		 * @param sequence the sequence of parameters to look for
		 * @return an {@link IntSupplier} that retrieves the index
		 * @see #getIndex
		 */
		public IntSupplier getIndexCI(boolean afterSequence, String... sequence) {
			return getIndex(true, afterSequence, toTypeList(sequence));
		}

		/**
		 * Lazily gets the index of the first sequence matching the specified
		 * one in the method parameters.
		 * @param afterCallback whether to use the behaviour of
		 *                      {@link #getIndexCI} or {@link #getIndex}
		 * @param afterSequence whether to get the index after the
		 *                      sequence or where the sequence itself is found
		 * @param sequence the sequence of parameters to look for in
		 *                 the form of a {@link Type} list
		 * @return an {@link IntSupplier} that retrieves the index
		 * @see #getIndex
		 */
		private IntSupplier getIndex(boolean afterCallback, boolean afterSequence, List<Type> sequence) {
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

		/**
		 * Adds the specified parameters to the method descriptor and
		 * shifts the locals accordingly, taking into account "wide" types
		 * and non-static methods. The index should be obtained via
		 * {@link #getIndex} or {@link #getIndexCI}. Used to correct the locals of an
		 * {@link Inject} or when the target method needs to be changed to one
		 * added by OptiFine (which takes extra arguments) since the body
		 * of the original is replaced with a call to the OptiFine method.
		 * @param index index at which the params are to be inserted.
		 *              Refers strictly to the index of the parameter in the array
		 *              obtained via {@link Type#getArgumentTypes(String)}.
		 * @param params internal names of the params to be added
		 */
		public void addParams(IntSupplier index, String... params) {
			addParams(index.getAsInt(), toTypeList(params));
		}

		/**
		 * Inserts the specified parameters at the given index in the
		 * method descriptor. The index is an {@code int} for convenience
		 * in some situations.
		 * @param index index at which the params are to be inserted
		 * @param params internal names of the params to be added
		 */
		public void addParams(int index, String... params) {
			addParams(index, toTypeList(params));
		}

		/**
		 * Appends the specified parameters to the method descriptor.
		 * @param params internal names of the params to be appended
		 * @see #addParams
		 */
		public void addParams(String... params) {
			addParams(Type.getArgumentTypes(method.desc).length, toTypeList(params));
		}

		/**
		 * Inserts the specified parameters at the given index in the
		 * method descriptor.
		 * @param index index at which the params are to be inserted
		 * @param params list of the parameter {@link Type}s to be added
		 * @see #addParams       
		 */
		private void addParams(int index, List<Type> params) {
			List<Type> newDesc = Arrays.stream(Type.getArgumentTypes(method.desc)).collect(Collectors.toList());
			newDesc.addAll(index, params);
			int shiftBy = 0;
			for (Type param : params) {
				shiftBy += param.getSize();
			}

			if (method.parameters != null) {
				for (int i = 0; i < params.size(); i++) {
					method.parameters.add(index + i, new ParameterNode("syn_" + i, Opcodes.ACC_SYNTHETIC));
				}
			}

			shiftLocals(index, shiftBy, newDesc);
		}

		/**
		 * Removes the parameter at the given index and shifts the local
		 * variables accordingly. Use with caution since no check is done
		 * to confirm that the parameter is unused. The index should be
		 * obtained via {@link #getIndex} or {@link #getIndexCI}. Same use cases as
		 * {@link #addParams}.
		 * @param index index of the parameter to be removed
		 */
		public void removeParam(int index) {
			List<Type> newDesc = Lists.newArrayList(Type.getArgumentTypes(method.desc));
			int shiftBy = -newDesc.remove(index).getSize();

			int lvIndex = shiftLocals(index, shiftBy, newDesc);
			if (method.localVariables != null) method.localVariables.removeIf(local -> local.index == lvIndex);
			if (method.parameters != null) method.parameters.remove(index);
		}

		/**
		 * Handles the shifting of local variables indices and instructions,
		 * and adjust the {@code maxLocals}. Additionally, it marks the method
		 * for updating.
		 * @param index the index of the parameter in the array obtained via
		 *              {@link Type#getArgumentTypes} from which to start shifting.
		 *              All local variables starting with the one corresponding to
		 *              the parameter will be shifted accordingly.
		 * @param shiftBy how much to shift the locals by
		 * @param params list of the parameter {@link Type}s in the order in
		 *               which they appear in the descriptor
		 * @return the calculated index of the local corresponding to the
		 * parameter at the given {@code index}. This is used by {@link #removeParam}
		 * in order to remove the {@link LocalVariableNode} with that index
		 */
		private int shiftLocals(int index, int shiftBy, List<Type> params) {
			int lvIndex = 0;
			if (!Modifier.isStatic(method.access)) lvIndex++;

			for (int i = 0; i < index; i++) {
				lvIndex += params.get(i).getSize();
			}

			for (LocalVariableNode local : method.localVariables) {
				if (local.index >= lvIndex) {
					local.index += shiftBy;
				}
			}

			for (AbstractInsnNode insn : method.instructions) {
				if (insn instanceof VarInsnNode && ((VarInsnNode) insn).var >= lvIndex) {
					((VarInsnNode) insn).var += shiftBy;
				} else if (insn instanceof IincInsnNode && ((IincInsnNode) insn).var >= lvIndex) {
					((IincInsnNode) insn).var += shiftBy;
				}
			}

			method.maxLocals += shiftBy;
			method.desc = Type.getMethodDescriptor(Type.getReturnType(method.desc), params.toArray(new Type[0]));
			requiresUpdate = true;

			return lvIndex;
		}
	}

	/**
	 * Class used to construct an {@link At} annotation node
	 * from the given {@code value} and {@code target}. The
	 * {@code target} is required since the use only use
	 * case of this is when constructing a {@link Slice}.
	 */
	public static class AtNode {
		final AnnotationNode at;
		public AtNode(String value, String target) {
			at = new AnnotationNode(At.class.getName());
			Annotations.setValue(at, "value", value);
			Annotations.setValue(at, "target", target);
		}
	}

	public boolean shouldUpdateClassInfo() {
		return updateClassInfo;
	}

	static List<Type> toTypeList(String[] types) {
		return Arrays.stream(types).map(Type::getType).collect(Collectors.toList());
	}
}
