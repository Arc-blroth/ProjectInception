package ai.arcblroth.projectInception.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface AccessorRenderThread {

    @Accessor("thread")
    Thread projectInceptionGetRenderThread();

}
