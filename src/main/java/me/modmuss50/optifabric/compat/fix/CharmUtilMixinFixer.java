package me.modmuss50.optifabric.compat.fix;

import me.modmuss50.optifabric.compat.MixinNodeTransformer;

import java.util.Collections;
import java.util.List;

public class CharmUtilMixinFixer implements IMixinFixer {
	@Override
	public List<String> getTargets() {
		return Collections.singletonList("svenhjol/charm/mixin/UtilMixin");
	}

	@Override
	public void fix(MixinNodeTransformer transformer) {
		String target = "Lorg/apache/logging/log4j/Logger;debug(Ljava/lang/String;Ljava/lang/Object;)V";
		transformer.transformMethods("hookAttemptDataFixInternal", method -> method.transformInjector(injector ->
				injector.setAt(new MixinNodeTransformer.AtNode("INVOKE", target))));
	}
}
