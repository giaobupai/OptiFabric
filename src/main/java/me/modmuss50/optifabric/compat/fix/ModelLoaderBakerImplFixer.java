package me.modmuss50.optifabric.compat.fix;

import me.modmuss50.optifabric.util.RemappingUtils;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.modmuss50.optifabric.util.MixinFixerUtils.addParams;

public class ModelLoaderBakerImplFixer implements IMixinFixer {
	@Override
	public List<String> getTargets() {
		return Collections.singletonList("net/fabricmc/fabric/mixin/client/model/loading/ModelLoaderBakerImplMixin");
	}

	@Override
	public void fix(IMixinInfo mixinInfo, ClassNode mixinNode) {
		String bakeDesc = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_2960;Lnet/minecraft/class_3665;Ljava/util/function/Function;)Lnet/minecraft/class_1087;");
		String optifineBake = 'L' + RemappingUtils.getClassName("class_1088$class_7778") + ";bake" + bakeDesc;
		for (MethodNode method : mixinNode.methods) {
			List<String> injectMethod = new ArrayList<>();
			injectMethod.add(optifineBake);
			AnnotationNode annotation;
			switch (method.name) {
				case "invokeModifyBeforeBake":
					addParams(method, mixinInfo, "Ljava/util/function/Function;");
					//no break is intentional
				case "invokeModifyAfterBake":
					annotation = InjectionInfo.getInjectorAnnotation(mixinInfo, method);
					Annotations.setValue(annotation, "method", injectMethod);
					break;
			}
		}
	}

	@Override
	public boolean shouldUpdateClassInfo() {
		return true;
	}
}
