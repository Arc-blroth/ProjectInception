package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import com.google.common.collect.Lists;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SplashTextResourceSupplier.class)
public class MixinSplashTextResourceSupplier {

    @Shadow @Final private List<String> splashTexts;

    @Inject(method = "apply", at = @At("RETURN"), require = 0)
    private void addProjectInceptionReferences(List<String> list, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            list.removeIf(s -> s.toLowerCase().contains(I18n.translate("menu.multiplayer").toLowerCase()));
            for(int i = 1; i <= 8; i++) {
                this.splashTexts.add(I18n.translate("message.project_inception.splash" + i));
            }
        }
    }

}
