package me.modmuss50.optifabric.compat;

import me.modmuss50.optifabric.mod.OptifabricError;
import me.modmuss50.optifabric.mod.OptifabricSetup;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class OptifabricMixinErrorHandler implements IMixinErrorHandler {
	@Override
	public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
		return handleError(mixin, action, th, false);
	}

	@Override
	public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
		return handleError(mixin, action, th, true);
	}

	private static ErrorAction handleError(IMixinInfo mixin, ErrorAction action, Throwable th, boolean apply) {
		boolean compat = !ModMixinFixer.INSTANCE.getFixers(mixin.getClassName()).isEmpty();
		Level level = action == ErrorAction.ERROR ? Level.ERROR : Level.WARN;
		IMixinConfig config = mixin.getConfig();
		String msg = String.format(getMessage(apply, compat), mixin, config.getName(), FabricUtil.getModId(config));
		OptifabricSetup.LOGGER.log(level, msg);
		//TODO: make this support more than one error and separate OptiFine errors from mod errors
		if (!OptifabricError.hasError()) {
			OptifabricError.setError(th, msg);
			OptifabricError.modError = true;
		}
		if (level == Level.ERROR) {
			OptifabricSetup.LOGGER.info("The following message should have been an error, but will be logged as a " +
					"warn instead in order to allow the game to show the crash screen.");
		}
		//let the game show the crash screen instead of outright crashing
		//TODO: there are some cases where doing this will do more bad than good
		return ErrorAction.WARN;
	}

	private static String getMessage(boolean apply, boolean compat) {
		String msg;
		if (compat) {
			if (apply) {
				msg = "Failed to apply compatibility patch for %s! Try downgrading the affected mod.";
			} else {
				msg = "Prepare error in patched %s! At least one of the patches has a serious flaw!";
			}
		} else {
			if (apply) {
				msg = "Mixin %s could not be applied and no compatibility patch was found!";
			} else {
				msg = "Prepare error in %s! No compatibility patch was found! This might be an issue with the original mod.";
			}
		}
		msg = String.format(msg, "'%s' in '%s' from mod '%s'") + " Please report this issue.";
		return msg;
	}
}
