package ai.arcblroth.projectInception.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ListIterator;

@Mixin(DebugHud.class)
public class JokeMixinDebugHud {

    @Inject(method = "getLeftText", at = @At("RETURN"))
    public void what(CallbackInfoReturnable<List<String>> info) {
        ListIterator<String> itr = info.getReturnValue().listIterator();
        while (itr.hasNext()) {
            String s = itr.next();
            if(s.startsWith("Minecraft ")) {
                itr.remove();
                itr.add("Minecraft " +  SharedConstants.getGameVersion().getName() + " (Gross/Fabric/Hacks)");
            }
            if(s.startsWith("Facing:")) {
                itr.remove();
                itr.add("Facing: 101 pOrT tO ForGe comments");
            }
            if(s.startsWith("Biome:")) {
                itr.remove();
                itr.add("Bikeshed:" + s.substring("Biome:".length()));
            }
            if(s.startsWith("Local Difficulty:")) {
                itr.remove();
                itr.add("Fluid API Progress:" + s.substring("Local Difficulty:".length()));
            }
            if(s.startsWith("Sounds:")) {
                itr.remove();
                itr.add("Sounds: like Arc'blroth whilst debugging");
            }
        }
    }

    @Redirect(method = "getLeftText", at = @At(value = "INVOKE", target = "net/minecraft/client/MinecraftClient.getServer()Lnet/minecraft/server/integrated/IntegratedServer;"))
    private IntegratedServer yeetIntegratedServer(MinecraftClient client) {
        return null;
    }

    @Redirect(method = "getLeftText", at = @At(value = "INVOKE", target = "net/minecraft/client/network/ClientPlayerEntity.getServerBrand()Ljava/lang/String;"))
    private String getServerBrand(ClientPlayerEntity cpe) {
        return "smol";
    }

    @Redirect(method = "getLeftText", at = @At(value = "INVOKE", target = "net/minecraft/util/registry/RegistryKey.getValue()Lnet/minecraft/util/Identifier;"))
    private Identifier getDimensionId(RegistryKey<?> in) {
        return new Identifier("fabricord", "fluid-volume-api-debating");
    }

    @Inject(method = "getRightText", at = @At("RETURN"))
    public void how(CallbackInfoReturnable<List<String>> info) {
        ListIterator<String> itr = info.getReturnValue().listIterator();
        long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        while (itr.hasNext()) {
            String s = itr.next();
            if(s.startsWith("Java:")) {
                itr.remove();
                itr.add("Javascript: V8 8.7.83 " + (MinecraftClient.getInstance().is64Bit() ? 64 : 32) + "bit");
            }
            if(s.startsWith("Mem:")) {
                itr.remove();
                itr.add("Mem: 0% " + (memory / 1024L / 1024L) + "/∞MB");
            }
            if(s.startsWith("Allocated:")) {
                itr.remove();
                itr.add("Allocated: 0% ∞MB");
            }
        }
    }

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "com/mojang/blaze3d/platform/GlDebugInfo.getVendor()Ljava/lang/String;", ordinal = 0))
    private String getFakeDisplayVendor() {
        return "Bikeshedded";
    }

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "com/mojang/blaze3d/platform/GlDebugInfo.getCpuInfo()Ljava/lang/String;", ordinal = 0))
    private String getFakeCpuInfo() {
        return "ATmega328P (TM) CPU @ 16MHz";
    }

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "com/mojang/blaze3d/platform/GlDebugInfo.getRenderer()Ljava/lang/String;", ordinal = 0))
    private String getFakeRenderer() {
        return "Mojang Repair Systems Image Processor 9000";
    }

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "com/mojang/blaze3d/platform/GlDebugInfo.getVersion()Ljava/lang/String;", ordinal = 0))
    private String getFakeRendererVersion() {
        return "1.7.3 build 27";
    }


}
