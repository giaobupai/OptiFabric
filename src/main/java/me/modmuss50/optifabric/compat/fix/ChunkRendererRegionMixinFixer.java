package me.modmuss50.optifabric.compat.fix;

import me.modmuss50.optifabric.compat.MixinNodeTransformer;
import me.modmuss50.optifabric.mod.OptifabricSetup;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

public class ChunkRendererRegionMixinFixer implements IMixinFixer {
	@Override
	public List<String> getTargets() {
		return Arrays.asList(
				"net/fabricmc/fabric/mixin/rendering/data/attachment/client/MixinChunkRendererRegion",
				"net/fabricmc/fabric/mixin/rendering/data/attachment/client/ChunkRendererRegionMixin",
				"net/fabricmc/fabric/mixin/rendering/data/attachment/client/MixinChunkRendererRegionBuilder",
				"net/fabricmc/fabric/mixin/rendering/data/attachment/client/ChunkRendererRegionBuilderMixin",
				"net/fabricmc/fabric/mixin/blockview/client/ChunkRendererRegionBuilderMixin"
				);
	}

	@Override
	public void fix(MixinNodeTransformer transformer) {
		List<String> methods = Arrays.asList("create", "createDataMap");
		String newMethod;
		if (OptifabricSetup.generateCachePresent) {
			newMethod = "generateCache";
		} else if (OptifabricSetup.createRegionPresent) {
			newMethod = "createRegion";
		} else {
			return;
		}

		transformer.transformMethods(methods, method -> {
			IntSupplier chunkRadius = method.getIndex(true, "I");
			method.addParams(chunkRadius, "Z");
			method.transformInjector(injector -> injector.setMethod(newMethod));
		});
	}
}
