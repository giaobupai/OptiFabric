package me.modmuss50.optifabric.compat.fix;

import me.modmuss50.optifabric.compat.MixinNodeTransformer;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

public class TextureAtlasMixinFixer implements IMixinFixer {

	@Override
	public List<String> getTargets() {
		return Arrays.asList(
				"me/shedaniel/architectury/mixin/fabric/client/MixinTextureAtlas",
				"dev/architectury/mixin/fabric/client/MixinTextureAtlas",
				"svenhjol/meson/mixin/SpriteAtlasTextureMixin",
				"svenhjol/charm/mixin/SpriteAtlasTextureMixin",
				"svenhjol/charm/mixin/event/StitchTextureMixin",
				"svenhjol/charm/mixin/callback/StitchTextureCallbackMixin");
	}

	@Override
	public void fix(MixinNodeTransformer transformer) {
		transformer.transformMethods(Arrays.asList("preStitch", "hookStitch"), method -> {
			IntSupplier setIndex = method.getIndexCI(false, "Ljava/util/Set;");
			/*
			Normally Optifine's mipmapLevels (lv) should be passed instead of the method argument with the
			same name (which Optifine calls maxMipmapLevelIn), but these injectors only care about the set
			*/
			method.addParams(setIndex, "I");
		});
	}
}
