package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.client.IColoredTooltipItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class MixinItemstack {

    @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "java/util/List.add(Ljava/lang/Object;)Z", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void getTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List> ci, List list, MutableText mutableText) {
        if(getItem() instanceof IColoredTooltipItem) {
            TextColor finalColor = ((IColoredTooltipItem) getItem()).getFinalColor();
            mutableText.styled((style -> style.withColor(finalColor)));
        }
    }

    @Shadow
    public abstract Item getItem();

}
