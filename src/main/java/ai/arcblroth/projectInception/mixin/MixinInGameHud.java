package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.client.IColoredTooltipItem;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Shadow private ItemStack currentStack;

    @Redirect(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "net/minecraft/text/MutableText.formatted(Lnet/minecraft/util/Formatting;)Lnet/minecraft/text/MutableText;"), require = 0)
    private MutableText formatHeldItemTooltip(MutableText in, Formatting formatting) {
        if(formatting != Formatting.ITALIC && this.currentStack.getItem() instanceof IColoredTooltipItem) {
            return in.styled((style -> style.withColor(((IColoredTooltipItem) this.currentStack.getItem()).getFinalColor())));
        } else {
            return in.formatted(formatting);
        }
    }

}
