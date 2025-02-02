package me.modmuss50.optifabric.patcher.fixes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.modmuss50.optifabric.mod.OptifabricSetup;
import net.fabricmc.loader.api.FabricLoader;

import me.modmuss50.optifabric.compat.fabricrenderingfluids.FluidRendererFix;
import me.modmuss50.optifabric.compat.uglyscoreboardfix.InGameHudFix;
import me.modmuss50.optifabric.util.RemappingUtils;

public class OptifineFixer {

	public static final OptifineFixer INSTANCE = new OptifineFixer();

	private final Map<String, List<ClassFixer>> classFixes = new HashMap<>();
	private final Set<String> skippedClass = new HashSet<>();

	private OptifineFixer() {
		//net/minecraft/client/render/chunk/ChunkBuilder$ChunkData
		registerFix("class_846$class_849", new ChunkDataFix());

		//net/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask
		registerFix("class_846$class_851$class_4578", new ChunkRendererFix());

		//net/minecraft/client/render/block/BlockModelRenderer$AmbientOcclusionCalculator
		registerFix("class_778$class_780", new AmbientOcclusionCalculatorFix());

		//net/minecraft/client/Keyboard
		registerFix("class_309", new KeyboardFix());

		//net/minecraft/client/texture/SpriteAtlasTexture
		registerFix("class_1059", new SpriteAtlasTextureFix());

		//net/minecraft/client/particle/ParticleManager
		registerFix("class_702", new ParticleManagerFix());

		//net/minecraft/client/render/model/json/ModelOverrideList
		registerFix("class_806", new ModelOverrideListFix());

		if (OptifabricSetup.isPresent("minecraft", ">=1.18.2")) {
			//net/minecraft/client/option/GameOptions
			registerFix("class_315", new GameOptionsFix());
		}

		if (FabricLoader.getInstance().isModLoaded("fabric-rendering-fluids-v1")) {
			//net/minecraft/client/render/block/FluidRenderer
			registerFix("class_775", new FluidRendererFix());
		}

		if (FabricLoader.getInstance().isModLoaded("uglyscoreboardfix")) {
			//net/minecraft/client/gui/hud/InGameHud
			registerFix("class_329", new InGameHudFix());
		}

		//net/minecraft/block/entity/BlockEntity
		skipClass("class_2586");
	}

	private void registerFix(String className, ClassFixer classFixer) {
		classFixes.computeIfAbsent(RemappingUtils.getClassName(className), s -> new ArrayList<>()).add(classFixer);
	}

	@SuppressWarnings("SameParameterValue") //Might be useful in future
	private void skipClass(String className) {
		skippedClass.add(RemappingUtils.getClassName(className));
	}

	public boolean shouldSkip(String className) {
		return skippedClass.contains(className);
	}

	public List<ClassFixer> getFixers(String className) {
		return classFixes.getOrDefault(className, Collections.emptyList());
	}
}
