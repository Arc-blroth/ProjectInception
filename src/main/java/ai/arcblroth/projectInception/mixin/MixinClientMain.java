package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Main.class)
public class MixinClientMain {

    @Inject(method = "main", at = @At("HEAD"))
    private static void notmain(String[] args, CallbackInfo ci) {
        ProjectInceptionEarlyRiser.ARGUMENTS = Arrays.copyOf(args, args.length);
    }

}
