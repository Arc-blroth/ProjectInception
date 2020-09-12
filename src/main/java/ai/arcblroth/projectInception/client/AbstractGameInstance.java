package ai.arcblroth.projectInception.client;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.block.AbstractDisplayBlockEntity;
import ai.arcblroth.projectInception.block.GameMultiblock;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;

import static ai.arcblroth.projectInception.client.mc.QueueProtocol.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

public abstract class AbstractGameInstance<T extends AbstractDisplayBlockEntity<T>> {

    private static int instanceCounter = 0;
    private static final Object cleanUpLock = new Object();
    private static final ArrayList<AbstractGameInstance<?>> instancesToCleanUp = new ArrayList<>();
    protected final int instanceNumber;

    protected final GameMultiblock<T> multiblock;

    protected final ChronicleQueue childQueue;
    protected final ExcerptTailer tailer;
    protected OptionalLong tailerStartIndex = OptionalLong.empty();
    private final Thread tailerThread;

    private int lastWidth = 0;
    private int lastHeight = 0;
    private double lastMouseX = 0.5;
    private double lastMouseY = 0.5;
    private boolean showCursor = true;

    private ByteBuffer texture = null;
    private Identifier textureId = null;
    private NativeImageBackedTexture lastTextureImage = null;

    private final Object send2ChildLock = new Object();
    private ArrayList<Message> messages2ChildToSend = new ArrayList<>();

    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ProjectInception.LOGGER.log(Level.INFO, "Destroying game and taterwebz instances on exit...");
            stopAllGameInstances();
            if(ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS != null) {
                ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS.destroy();
                for(int i = 0; ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS.isAlive() && i < 10; i++) {
                    iSleep(300);
                }
                if(ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS.isAlive()) {
                    ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS.destroyForcibly();
                }
            }
            if(ProjectInceptionClient.TATERWEBZ_CHILD_QUEUE != null) {
                ProjectInceptionClient.TATERWEBZ_CHILD_QUEUE.close();
            }
            if(ProjectInception.toParentQueue != null
            && !ProjectInception.toParentQueue.isClosed()) {
                ProjectInception.toParentQueue.close();
            }
            iSleep(1000);
            File queueDir = new File(MinecraftClient.getInstance().runDirectory, "projectInception");
            ProjectInceptionEarlyRiser.yeetChronicleQueues(queueDir, false);
        }));
    }

    public AbstractGameInstance(GameMultiblock<T> multiblock) {
        instanceNumber = instanceCounter;
        instanceCounter++;
        synchronized (cleanUpLock) {
            instancesToCleanUp.add(this);
        }

        this.multiblock = multiblock;

        File childQueueDir = new File(MinecraftClient.getInstance().runDirectory, "projectInception" + File.separator + getNewInstanceQueueDirectory());
        if(childQueueDir.exists() && childQueueDir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(childQueueDir);
            } catch (IOException e) {
                ProjectInception.LOGGER.warn("Couldn't delete existing child queue directory. Things might not work.", e);
            }
        }
        this.childQueue = ProjectInceptionEarlyRiser.buildQueue(childQueueDir);
        this.tailer = this.childQueue.createTailer("projectInceptionGameInstance").direction(TailerDirection.NONE);
        this.tailerThread = new Thread(this::tailerThread);
    }

    protected abstract String getNewInstanceQueueDirectory();

    public void start() {
        this.tailerThread.start();
    }

    public final void stop(boolean async) {
        Runnable stopFunc = () -> {
            ProjectInception.LOGGER.log(Level.DEBUG, "Destroying game instance #" + this.instanceNumber);
            stopInner();
            if(this.childQueue != null && !this.childQueue.isClosed()) {
                this.childQueue.close();
            }
        };
        if(async) {
            new Thread(stopFunc).start();
        } else {
            stopFunc.run();
        }
    }

    protected void stopInner() {}

    private void tailerThread() {
        Thread.currentThread().setName("Game Instance Tailer " + instanceNumber);
        final AtomicBoolean isTextureUploading = new AtomicBoolean(false);
        final Object textureUploadLock = new Object();
        this.tailer.toEnd();
        final ExcerptAppender appender = this.childQueue.acquireAppender();
        try {
            while (isAlive() && !this.childQueue.isClosed()) {
                synchronized (send2ChildLock) {
                    if (messages2ChildToSend.size() > 0) {
                        messages2ChildToSend.forEach((message) -> writeParent2ChildMessage(message, appender));
                        messages2ChildToSend.clear();
                    }
                }
                tailerLoopInner();
                boolean previousShowCursor = showCursor;
                this.texture = getLastTexture();
                if (this.textureId == null) {
                    if (this.texture != null) {
                        isTextureUploading.set(true);
                        RenderSystem.recordRenderCall(() -> {
                            synchronized (textureUploadLock) {
                                NativeImage image = new NativeImage(NativeImage.Format.ABGR, lastWidth, lastHeight, false, memAddress(this.texture));
                                this.lastTextureImage = new NativeImageBackedTexture(image);
                                this.textureId = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("project_inception_game", lastTextureImage);
                                isTextureUploading.set(false);
                                textureUploadLock.notify();
                            }
                        });
                    }
                } else {
                    if (this.lastTextureImage != null) {
                        isTextureUploading.set(true);
                        RenderSystem.recordRenderCall(() -> {
                            synchronized (textureUploadLock) {
                                try {
                                    this.lastTextureImage.bindTexture();
                                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                                    this.lastTextureImage.upload();
                                    if(!previousShowCursor && showCursor) {
                                        clampCursor();
                                        Window w = MinecraftClient.getInstance().getWindow();
                                        glfwSetCursorPos(w.getHandle(), lastMouseX * w.getWidth(), lastMouseY * w.getHeight());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                isTextureUploading.set(false);
                                textureUploadLock.notify();
                            }
                        });
                    }
                }
                if(isTextureUploading.get()) {
                    synchronized (textureUploadLock) {
                        textureUploadLock.wait();
                    }
                } else {
                    int framerateLimit = MinecraftClient.getInstance().getWindow().getFramerateLimit();
                    if (framerateLimit != 0) {
                        iSleep(1000 / framerateLimit);
                    } else {
                        iSleep(1);
                    }
                }
            }
        } catch (InterruptedException ignored) {

        } finally {
            RenderSystem.recordRenderCall(() -> {
                if(this.lastTextureImage != null) {
                    // this segfaults Minecraft and I don't know why
                    // this.lastTextureImage.close();
                    this.lastTextureImage = null;
                }
                this.texture = null;
                this.textureId = null;
            });
        }
    }

    protected void tailerLoopInner() {}

    private ByteBuffer getLastTexture() {
        if(!isAlive()) return null;
        this.tailer.toEnd();
        if (this.tailer.index() == 0) return texture;
        if(!tailerStartIndex.isPresent()) {
            tailerStartIndex = OptionalLong.of(this.tailer.toStart().index());
            this.tailer.toEnd();
        }
        byte tries = 0; // Prevent softlocking in case the child instance is lagging
        while(!QueueProtocol.peekMessageType(this.tailer).equals(QueueProtocol.MessageType.IMAGE)) {
            tries++;
            if(this.tailer.index() - 1 == tailerStartIndex.getAsLong() || tries > 8) return texture;
            this.tailer.moveToIndex(this.tailer.index() - 1);
        }
        try (DocumentContext dc = this.tailer.readingDocument()) {
            if (dc.isPresent()) {
                Bytes<?> bytes = dc.wire().bytes();
                if(bytes.readByte() != QueueProtocol.MessageType.IMAGE.header) throw new IllegalStateException();
                lastWidth = bytes.readInt();
                lastHeight = bytes.readInt();
                showCursor = bytes.readBoolean();
                if (texture != null) {
                    texture.rewind();
                }
                if (texture == null || texture.capacity() < lastWidth * lastHeight * 4) {
                    texture = BufferUtils.createByteBuffer(lastWidth * lastHeight * 4);
                }
                bytes.read(texture);
                texture.rewind();
                for (int i = 0; i < texture.capacity(); i += 4) {
                    // on the sending side alpha is zero, so
                    // we make sure it gets set to 100% here
                    texture.put(i + 3, (byte) 255);
                }
            }
            return texture;
        } catch (IllegalStateException e) {
            // this can occur if the queue
            // is closed while we try to read
            // from it
            return null;
        }
    }

    private static void iSleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Identifier getLastTextureId() {
        return textureId;
    }

    public void click(double hitX, double hitY) {
        MouseSetPosMessage message1 = new MouseSetPosMessage();
        message1.x = hitX;
        message1.y = hitY;
        lastMouseX = hitX;
        lastMouseY = hitY;
        sendParent2ChildMessage(message1);
        MouseButtonMessage message2 = new MouseButtonMessage();
        message2.button = GLFW_MOUSE_BUTTON_LEFT;
        message2.message = GLFW_PRESS;
        sendParent2ChildMessage(message2);
        MouseButtonMessage message3 = new MouseButtonMessage();
        message3.button = GLFW_MOUSE_BUTTON_LEFT;
        message3.message = GLFW_RELEASE;
        sendParent2ChildMessage(message3);
    }

    public void clampCursor() {
        MouseSetPosMessage mpMessage = new MouseSetPosMessage();
        mpMessage.x = Math.min(Math.max(lastMouseX, 0), 1);
        mpMessage.y = Math.min(Math.max(lastMouseY, 0), 1);
        lastMouseX = mpMessage.x;
        lastMouseY = mpMessage.y;
        sendParent2ChildMessage(mpMessage);
    }

    public void sendParent2ChildMessage(Message message) {
        if(message instanceof MouseMoveMessage) {
            MouseMoveMessage mmMessage = (MouseMoveMessage) message;
            if(this.shouldShowCursor()) {
                mmMessage.x = Math.min(Math.max(mmMessage.x, 0), 1);
                mmMessage.y = Math.min(Math.max(mmMessage.y, 0), 1);
            }
            lastMouseX = mmMessage.x;
            lastMouseY = mmMessage.y;
        } else if(message instanceof MouseSetPosMessage) {
            MouseSetPosMessage mpMessage = (MouseSetPosMessage) message;
            if(this.shouldShowCursor()) {
                mpMessage.x = Math.min(Math.max(mpMessage.x, 0), 1);
                mpMessage.y = Math.min(Math.max(mpMessage.y, 0), 1);
            }
            lastMouseX = mpMessage.x;
            lastMouseY = mpMessage.y;
        }
        synchronized (send2ChildLock) {
            this.messages2ChildToSend.add(message);
        }
    }

    public static void stopAllGameInstances() {
        synchronized (cleanUpLock) {
            instancesToCleanUp.forEach(g -> {
                g.stop(false);
            });
            instancesToCleanUp.clear();
        }
    }

    public abstract boolean isAlive();

    public double getLastMouseX() {
        return lastMouseX;
    }

    public double getLastMouseY() {
        return lastMouseY;
    }

    public boolean shouldShowCursor() {
        return showCursor;
    }

}
