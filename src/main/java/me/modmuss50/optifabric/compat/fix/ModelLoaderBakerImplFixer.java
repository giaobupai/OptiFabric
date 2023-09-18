package me.modmuss50.optifabric.compat.fix;

import me.modmuss50.optifabric.compat.MixinNodeTransformer;
import me.modmuss50.optifabric.util.RemappingUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModelLoaderBakerImplFixer implements IMixinFixer {
	@Override
	public List<String> getTargets() {
		return Collections.singletonList("net/fabricmc/fabric/mixin/client/model/loading/ModelLoaderBakerImplMixin");
	}

	@Override
	public void fix(MixinNodeTransformer transformer) {
		String bakeDesc = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_2960;Lnet/minecraft/class_3665;Ljava/util/function/Function;)Lnet/minecraft/class_1087;");
		String optifineBake = 'L' + RemappingUtils.getClassName("class_1088$class_7778") + ";bake" + bakeDesc;
		transformer.transformMethods("invokeModifyBeforeBake", method -> {
			method.addParams("Ljava/util/function/Function;");
		});
		transformer.transformMethods(Arrays.asList("invokeModifyAfterBake", "invokeModifyBeforeBake"), method -> {
			method.transformInjector(injector -> injector.setMethod(optifineBake));
		});
	}
}
