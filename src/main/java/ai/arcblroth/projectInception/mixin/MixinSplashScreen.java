package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.postlaunch.PostLaunchEntrypoint;
import ai.arcblroth.projectInception.postlaunch.ProgressBar;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static net.minecraft.client.gui.DrawableHelper.fill;
import static net.minecraft.client.gui.hud.BackgroundHelper.ColorMixer;

@Mixin(SplashScreen.class)
public class MixinSplashScreen {

    @Shadow MinecraftClient client;
    @Shadow private long applyCompleteTime;
    @Unique private static int projectInceptionProgressBarBorder;
    @Unique private static int projectInceptionProgressBarColor;
    @Unique private static List<PostLaunchEntrypoint> projectInceptionEntrypoints;
    @Unique private static float projectInceptionProgress;
    @Unique private static boolean projectInceptionShouldRenderBar = false;
    @Unique private static ProgressBar projectInceptionCurrentBar;
    @Unique private static int projectInceptionCurrentEntrypoint = -1;
    @Unique private static boolean projectInceptionIsDone = false;
    @Unique private static ExecutorService projectInceptionThreadPool;
    @Unique private static Future<?> projectInceptionCurrentEntrypointFuture;

    @Inject(method = "init", at = @At("RETURN"))
    private static void init(CallbackInfo ci) {
        projectInceptionEntrypoints = Collections.unmodifiableList(FabricLoader.getInstance().getEntrypoints("project_inception:postLaunch", PostLaunchEntrypoint.class));
        projectInceptionThreadPool = Executors.newFixedThreadPool(1);

        if(FabricLoader.getInstance().isModLoaded("dark-loading-screen")) {
            try {
                Class<?> dls = Class.forName("io.github.a5b84.darkloadingscreen.Mod");
                Object config = dls.getField("config").get(null);
                Class<?> configClass = config.getClass();
                projectInceptionProgressBarBorder = (int) configClass.getField("borderColor").get(config);
                projectInceptionProgressBarColor = (int) configClass.getField("barColor").get(config);
            } catch (ReflectiveOperationException e) {
                projectInceptionProgressBarBorder = ColorMixer.getArgb(255, 255, 255, 255);
                projectInceptionProgressBarColor = projectInceptionProgressBarBorder;
            }
        } else {
            projectInceptionProgressBarBorder = ColorMixer.getArgb(255, 255, 255, 255);
            projectInceptionProgressBarColor = projectInceptionProgressBarBorder;
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void preventFadeOut(CallbackInfo ci) {
        if(applyCompleteTime != -1 && !projectInceptionIsDone) {
            applyCompleteTime = Util.getMeasuringTimeMs();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "net/minecraft/client/gui/screen/Screen.init(Lnet/minecraft/client/MinecraftClient;II)V"))
    private void yeetTitleScreenInit(Screen screen, MinecraftClient client, int width, int height) {
        // this shouldn't run, but its here just in case another mod messes with screen init
        if (projectInceptionIsDone) {
            screen.init(client, width, height);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "net/minecraft/client/MinecraftClient.setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V"))
    private void onRemoveOverlay(CallbackInfo ci) {
        projectInceptionShouldRenderBar = false;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void runPostLaunchEntrypoints(CallbackInfo ci) {
        if(applyCompleteTime != -1L) {
            if (projectInceptionEntrypoints.size() == 0 || projectInceptionCurrentEntrypoint == projectInceptionEntrypoints.size()) {
                if(!projectInceptionIsDone) {
                    projectInceptionIsDone = true;
                    client.currentScreen.init(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
                }
            } else {
                projectInceptionShouldRenderBar = true;
                if (projectInceptionCurrentEntrypoint == -1 || (projectInceptionCurrentEntrypoint < projectInceptionEntrypoints.size() && projectInceptionCurrentEntrypointFuture.isDone())) {
                    projectInceptionCurrentBar = new ProgressBar();
                    projectInceptionCurrentEntrypoint++;
                    if (projectInceptionCurrentEntrypoint < projectInceptionEntrypoints.size()) {
                        projectInceptionProgress = 0;
                        projectInceptionCurrentEntrypointFuture = projectInceptionThreadPool.submit(() ->
                                projectInceptionEntrypoints.get(projectInceptionCurrentEntrypoint).onPostLaunch(projectInceptionCurrentBar)
                        );
                    }
                }
            }
            if (projectInceptionCurrentBar != null) {
                float actualProgress = !projectInceptionIsDone ? projectInceptionCurrentBar.getProgress() : 1;
                projectInceptionProgress = MathHelper.clamp(projectInceptionProgress * 0.95F + actualProgress * 0.05F, 0.0F, 1.0F);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "net/minecraft/client/gui/screen/SplashScreen.renderProgressBar(Lnet/minecraft/client/util/math/MatrixStack;IIIIF)V", ordinal = 0))
    private void renderPostLaunchBar(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(!projectInceptionShouldRenderBar) return;

        float f = this.applyCompleteTime > -1L ? (float)(Util.getMeasuringTimeMs() - this.applyCompleteTime) / 1000.0F : -1.0F;
        int scaledWidth = this.client.getWindow().getScaledWidth();
        int scaledHeight = this.client.getWindow().getScaledHeight();
        double d = Math.min(this.client.getWindow().getScaledWidth() * 0.75D, this.client.getWindow().getScaledHeight()) * 0.25D;
        int s = (int)(d * 2.0D);
        int t = (int)((double)scaledHeight * 0.8325D);
        renderPostLaunchBarInner(matrices, scaledWidth / 2 - s, t + 15, scaledWidth / 2 + s, t + 25, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
    }

    private void renderPostLaunchBarInner(MatrixStack matrixStack, int xMin, int yMin, int xMax, int yMax, float fade) {
        int m = MathHelper.ceil((float)(xMax - xMin - 2) * projectInceptionProgress);
        int n = Math.round(fade * 255.0F);
        int border = ColorMixer.getArgb(n, ColorMixer.getRed(projectInceptionProgressBarBorder), ColorMixer.getGreen(projectInceptionProgressBarBorder), ColorMixer.getBlue(projectInceptionProgressBarBorder));
        int bar = ColorMixer.getArgb(n, ColorMixer.getRed(projectInceptionProgressBarColor), ColorMixer.getGreen(projectInceptionProgressBarColor), ColorMixer.getBlue(projectInceptionProgressBarColor));

        fill(matrixStack, xMin + 1, yMin, xMax - 1, yMin + 1, border);
        fill(matrixStack, xMin + 1, yMax, xMax - 1, yMax - 1, border);
        fill(matrixStack, xMin, yMin, xMin + 1, yMax, border);
        fill(matrixStack, xMax, yMin, xMax - 1, yMax, border);
        fill(matrixStack, xMin + 2, yMin + 2, xMin + m, yMax - 2, bar);

        DrawableHelper.drawCenteredString(
                matrixStack,
                client.textRenderer,
                projectInceptionCurrentBar.getText(),
                (xMin + xMax) / 2,
                (yMin + yMax) / 2 - 14,
                bar
        );
    }

}
