package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.mc.GameInstance;
import ai.arcblroth.projectInception.ProjectInception;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void stopGameInstancesOnServerStop(CallbackInfo ci) {
        ProjectInception.LOGGER.log(Level.INFO, "Destroying game instances on server stop");
        try {
            GameInstance.stopAllGameInstances();
        } catch (Exception e) {
            ProjectInception.LOGGER.log(Level.WARN, "Some instances may have not been destroyed: ", e);
        }
    }

}
