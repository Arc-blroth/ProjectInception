package ai.arcblroth.projectInception.mixin;

import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sprite.class)
public class DebugMixinSprite {

    @Shadow @Final Sprite.Info info;

    @Inject(method = "close", at = @At("HEAD"))
    private void beforeClose(CallbackInfo ci) {
        System.out.println("Closing sprite " + info.getId().toString());
    }

}
