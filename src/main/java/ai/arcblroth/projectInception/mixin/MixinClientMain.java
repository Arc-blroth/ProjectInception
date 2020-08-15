package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.QueueProtocol;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Arrays;

@Mixin(Main.class)
public class MixinClientMain {

    @Inject(method = "main", at = @At("HEAD"), cancellable = true)
    private static void notmain(String[] args, CallbackInfo ci) {
        // enabling slf4j makes ChronicleQueue happy but makes netty sad
        InternalLoggerFactory.setDefaultFactory(Log4JLoggerFactory.INSTANCE);

        // For reasons that make no sense, this class will unexist
        // later if it is not referenced here.
        // SharedConstants.getGameVersion();

        ProjectInceptionEarlyRiser.ARGUMENTS = Arrays.copyOf(args, args.length);
        if(ProjectInceptionEarlyRiser.IS_INNER && ProjectInceptionEarlyRiser.USE_FAUX_INNER) {
            try {
                OptionParser optionParser = new OptionParser();
                optionParser.allowsUnrecognizedOptions();
                OptionSpec<File> gameDir = optionParser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."));
                OptionSet optionSet = optionParser.parse(args);
                ProjectInceptionEarlyRiser.initChronicleQueues(new File(optionSet.valueOf(gameDir), "projectInception"));
                while(true) {
                    ProjectInception.outputQueue.acquireAppender().writeBytes(b -> {
                        b.writeByte(QueueProtocol.MessageType.IMAGE.header);
                        b.writeInt(16);
                        b.writeInt(16);
                        final byte red = (byte) 46,
                                   blue = (byte) 171,
                                   green = (byte) 255,
                                   alpha = (byte) 255;
                        for (int i = 0; i < 16 * 16; i++) {
                            b.writeByte(red);
                            b.writeByte(blue);
                            b.writeByte(green);
                            b.writeByte(alpha);
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

}
