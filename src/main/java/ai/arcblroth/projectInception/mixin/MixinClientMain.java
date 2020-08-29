package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.util.CyberDragonsUtil;
import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.mc.QueueProtocol;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.Random;

@Mixin(Main.class)
public class MixinClientMain {

    @Inject(method = "main", at = @At("HEAD"), cancellable = true)
    private static void notmain(String[] args, CallbackInfo ci) {
        // enabling slf4j makes ChronicleQueue happy but makes netty sad
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);

        // For reasons that make no sense, this class will unexist
        // later if it is not referenced here.
        SharedConstants.getGameVersion();

        ProjectInceptionEarlyRiser.ARGUMENTS = Arrays.copyOf(args, args.length);
        if(ProjectInceptionEarlyRiser.IS_INNER && ProjectInceptionEarlyRiser.USE_FAUX_INNER) {
            try {
                OptionParser optionParser = new OptionParser();
                optionParser.allowsUnrecognizedOptions();
                OptionSpec<File> gameDir = optionParser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."));
                OptionSet optionSet = optionParser.parse(args);
                ProjectInceptionEarlyRiser.initChronicleQueues(new File(optionSet.valueOf(gameDir), "projectInception" + File.separator + ProjectInceptionEarlyRiser.INSTANCE_PREFIX));
                Random random = new Random();
                final int color = CyberDragonsUtil.hsbToRgb(new double[] {random.nextDouble(), 0.8, 1});
                while(true) {
                    ProjectInception.toParentQueue.acquireAppender().writeBytes(b -> {
                        b.writeByte(QueueProtocol.MessageType.IMAGE.header);
                        b.writeInt(16);
                        b.writeInt(16);
                        b.writeBoolean(true);
                        for (int i = 0; i < 16 * 16; i++) {
                            b.writeInt(color);
                        }
                    });
                    Thread.sleep(1000 / 60);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "main", at = @At(value = "NEW", target = "Lnet/minecraft/client/WindowSettings;"))
    private static WindowSettings buildWindowSettingsWithMultiblockSize(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean fullscreen) {
        if(ProjectInceptionEarlyRiser.IS_INNER) {
            if (System.getProperty(ProjectInceptionEarlyRiser.ARG_DISPLAY_WIDTH) != null) {
                width = Integer.parseInt(System.getProperty(ProjectInceptionEarlyRiser.ARG_DISPLAY_WIDTH));
                fullscreenWidth = OptionalInt.of(width);
            }
            if (System.getProperty(ProjectInceptionEarlyRiser.ARG_DISPLAY_HEIGHT) != null) {
                height = Integer.parseInt(System.getProperty(ProjectInceptionEarlyRiser.ARG_DISPLAY_HEIGHT));
                fullscreenHeight = OptionalInt.of(height);
            }
            fullscreen = false;
        }
        return new WindowSettings(width, height, fullscreenWidth, fullscreenHeight, fullscreen);
    }

}
