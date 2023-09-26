package me.modmuss50.optifabric.compat.fix;

import com.google.common.collect.Lists;
import me.modmuss50.optifabric.compat.MixinNodeTransformer;
import me.modmuss50.optifabric.mod.OptifabricSetup;
import me.modmuss50.optifabric.util.RemappingUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

public class GameRendererMixinFixer implements IMixinFixer {
	@Override
	public List<String> getTargets() {
		return Arrays.asList(
				"net/fabricmc/fabric/mixin/screen/GameRendererMixin",
				"dev/architectury/mixin/fabric/client/MixinGameRenderer",
				"dev/architectury/mixin/fabric/client/MixinGameRenderer013",
				"me/shedaniel/architectury/mixin/fabric/client/MixinGameRenderer",
				"me/shedaniel/cloth/mixin/client/events/MixinGameRenderer");
	}

	@Override
	public void fix(MixinNodeTransformer transformer) {
		String window = 'L' + RemappingUtils.getClassName("class_1041") + ';';
		String matrixStack = 'L' + RemappingUtils.getClassName("class_4587") + ';';
		String matrix4f = 'L' + RemappingUtils.getClassName("class_1159") + ';';
		String drawContext = 'L' + RemappingUtils.getClassName("class_332") + ';';
		boolean useMatrix4f = OptifabricSetup.isPresent("minecraft", ">=1.17-alpha.21.10.a");
		boolean useJomlMatrix4F = OptifabricSetup.isPresent("minecraft", ">=1.19.3");
		boolean completeModelView = OptifabricSetup.isPresent("fabric-screen-api-v1", "<1.0.47");
		boolean useDrawContext = OptifabricSetup.isPresent("minecraft", ">=1.20");
		List<String> fabricMethods = Arrays.asList("onBeforeRenderScreen", "onAfterRenderScreen");
		List<String> methods = Lists.newArrayList("renderScreenPre", "renderScreenPost", "renderScreen");
		methods.addAll(fabricMethods);

		transformer.transformMethods(fabricMethods, method -> {
			IntSupplier matrixStackIndex = method.getIndexCI(false, matrixStack);
			List<String> params = new ArrayList<>();
			params.add(window);                               //window
			if (useMatrix4f) {
				if (useJomlMatrix4F) {
					params.add("Lorg/joml/Matrix4f;");        //projection
				} else {
					params.add(matrix4f);                     //projection
				}
				if (completeModelView) params.add(matrixStack); //modelView
			}
			method.addParams(matrixStackIndex, params.toArray(new String[0]));
			OptifabricSetup.usingScreenAPI = true;
		});

		transformer.transformMethods(methods, method -> {
			if (OptifabricSetup.farPlanePresent) {
				IntSupplier windowIndex = method.getIndexCI(true, window);
				method.addParams(windowIndex, "F");      //guiFarPlane
			}
			if (useDrawContext) {
				IntSupplier drawContextIndex = method.getIndexCI(false, drawContext);
				method.addParams(drawContextIndex, "F"); //guiOffsetZ
			}
		});
	}
}
