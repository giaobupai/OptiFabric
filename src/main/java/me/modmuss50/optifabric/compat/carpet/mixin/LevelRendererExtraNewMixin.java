package me.modmuss50.optifabric.compat.carpet.mixin;

import me.modmuss50.optifabric.compat.InterceptingMixin;
import me.modmuss50.optifabric.compat.Shim;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
@InterceptingMixin("carpet/mixins/LevelRenderer_pausedShakeMixin")
abstract class LevelRendererExtraNewMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;)V", argsOnly = true, ordinal = 0, require = 3, allow = 3,
                    at = @At(value = "INVOKE",
                                target = "Lnet/minecraft/client/particle/ParticleManager;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/Frustum;)V",
                                shift = At.Shift.BEFORE))
    private float doNewChangeTickPhaseBack(float previous) {
        return changeTickPhaseBack(previous);
    }

    @Shim
    private native float changeTickPhaseBack(float previous);
}
