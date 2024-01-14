package me.modmuss50.optifabric.compat.fix;

import me.modmuss50.optifabric.compat.MixinNodeTransformer;

import java.util.List;

public interface IMixinFixer {
	List<String> getTargets();
	void fix(MixinNodeTransformer transformer);
}
