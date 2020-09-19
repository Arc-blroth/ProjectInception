package ai.arcblroth.projectInception.mixin.fabriccord;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.ShulkerBoxBlockEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

/**
 * Response to a question by arxenix in the Fabricord:
 * https://discordapp.com/channels/507304429255393322/507982478276034570/756698683126317176
 * @author Arc'blroth
 */
@Mixin(ShulkerBoxBlockEntityRenderer.class)
public class MixinShulkerBoxEntityRenderer {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "net/minecraft/client/util/SpriteIdentifier.getVertexConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Ljava/util/function/Function;)Lnet/minecraft/client/render/VertexConsumer;"))
    private VertexConsumer getVertexConsumer(SpriteIdentifier spriteIdentifier, VertexConsumerProvider vertexConsumerProvider, Function<Identifier, RenderLayer> layerFactory) {
        return new SpriteTexturedVertexConsumer(
                ItemRenderer.getDirectGlintVertexConsumer(
                        vertexConsumerProvider,
                        spriteIdentifier.getRenderLayer(layerFactory),
                        false,
                        true
                ),
                spriteIdentifier.getSprite()
        );
    }

}
