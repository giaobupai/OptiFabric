package me.modmuss50.optifabric.compat.fix;

import me.modmuss50.optifabric.compat.MixinNodeTransformer;
import me.modmuss50.optifabric.util.RemappingUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;

public class BlockEntityRenderDispatcherMixinFixer implements IMixinFixer {
	@Override
	public List<String> getTargets() {
		return Collections.singletonList("svenhjol/charm/mixin/storage_labels/CallStorageLabelsRenderMixin");
	}

	@Override
	public void fix(MixinNodeTransformer transformer) {
		transformer.transformMethods("hookExtraRender", method -> {
			IntSupplier lightIndex = method.getIndexCI(false, "I");
			method.addParams(lightIndex, 'L' + RemappingUtils.getClassName("class_1937") + ';');
		});
	}
}
