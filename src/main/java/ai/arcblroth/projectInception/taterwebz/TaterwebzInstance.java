package ai.arcblroth.projectInception.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.block.GameMultiblock;
import ai.arcblroth.projectInception.block.TaterwebzBlockEntity;
import ai.arcblroth.taterwebz.TaterwebzChild;
import ai.arcblroth.taterwebz.TaterwebzPandomium;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.cef.browser.ProjectInceptionBrowser;

import java.util.ArrayList;

import static org.lwjgl.system.MemoryUtil.memAddress;

public class TaterwebzInstance {

    private static int instanceCounter = 0;
    private static final Object cleanUpLock = new Object();
    private static final ArrayList<TaterwebzInstance> instancesToCleanUp = new ArrayList<>();
    private final int instanceNumber;

    private final GameMultiblock<TaterwebzBlockEntity> multiblock;

    private ProjectInceptionBrowser browser;

    private double lastMouseX = 0.5;
    private double lastMouseY = 0.5;
    private boolean showCursor = true;

    private static long textureUUIDCounter = 0;
    private Identifier textureId = null;
    private CEFTexture lastTextureImage = null;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ProjectInception.LOGGER.log(Level.INFO, "Destroying taterwebz instances on exit...");
            stopAllTaterwebzInstances();
        }));
    }

    public TaterwebzInstance(GameMultiblock<TaterwebzBlockEntity> multiblock) {
        instanceNumber = instanceCounter;
        instanceCounter++;
        synchronized (cleanUpLock) {
            instancesToCleanUp.add(this);
        }

        this.multiblock = multiblock;
    }

    public void start() {
        if(browser == null || !browser.isClosed()) {
            try {
                browser = TaterwebzPandomium.createBrowser(
                        "https://google.com",
                        multiblock.sizeX * ProjectInceptionEarlyRiser.DISPLAY_SCALE,
                        multiblock.sizeY * ProjectInceptionEarlyRiser.DISPLAY_SCALE
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop(boolean async) {
        Runnable stopFunc = () -> {
            browser.onBeforeClose();
        };
        if(async) {
            new Thread(stopFunc).start();
        } else {
            stopFunc.run();
        }
    }

    public Identifier getLastTextureId() {
        if(textureId == null) {

        }
        return textureId;
    }

    private Identifier getNextTaterwebzTextureId() {
        return new Identifier(ProjectInception.MODID, "taterwebz_" + ++textureUUIDCounter);
    }

    public static void stopAllTaterwebzInstances() {
        synchronized (cleanUpLock) {
            instancesToCleanUp.forEach(g -> {
                g.stop(false);
            });
            instancesToCleanUp.clear();
        }
    }

    public boolean isAlive() {
        return browser != null && !browser.isClosed();
    }

    public double getLastMouseX() {
        return lastMouseX;
    }

    public double getLastMouseY() {
        return lastMouseY;
    }

    public boolean shouldShowCursor() {
        return showCursor;
    }

    public ProjectInceptionBrowser getBrowser() {
        return browser;
    }

}