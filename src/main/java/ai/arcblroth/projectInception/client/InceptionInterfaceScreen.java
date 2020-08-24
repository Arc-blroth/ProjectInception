package ai.arcblroth.projectInception.client;

import ai.arcblroth.projectInception.GameInstance;
import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.block.GameBlockEntity;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import static org.lwjgl.glfw.GLFW.*;

public class InceptionInterfaceScreen extends Screen {

    private final GameBlockEntity blockEntity;
    private final GameInstance gameInstance;
    private long lastEscPressTime = 0;

    public InceptionInterfaceScreen(GameBlockEntity blockEntity) {
        super(NarratorManager.EMPTY);
        this.blockEntity = blockEntity;
        this.gameInstance = blockEntity.getGameInstance();
        ProjectInception.focusedInstance = gameInstance;
    }

    @Override
    public void init() {
        this.client.keyboard.setRepeatEvents(true);
        gameInstance.clampCursor();
        InputUtil.setCursorParameters(
                this.client.getWindow().getHandle(),
                GLFW_CURSOR_DISABLED,
                gameInstance.getLastMouseX() * this.client.getWindow().getWidth(),
                gameInstance.getLastMouseY() * this.client.getWindow().getHeight()
        );
    }

    @Override
    public void tick() {
        // exact variable testing intentional
        if(ProjectInception.focusedInstance != gameInstance || !blockEntity.isOn()
        || !this.client.player.isAlive() || this.client.player.removed) {
            this.client.openScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == GLFW_KEY_ESCAPE) {
            long currentTime = Util.nanoTimeSupplier.getAsLong();
            if(currentTime - lastEscPressTime < 500000000) {
                this.client.player.sendMessage(new TranslatableText("message.project_inception.escape", ProjectInceptionClient.EXIT_INNER_LOCK.getBoundKeyLocalizedText()), true);
            }
            lastEscPressTime = currentTime;
        }
        if(InputUtil.fromKeyCode(keyCode, scanCode).equals(KeyBindingHelper.getBoundKeyOf(ProjectInceptionClient.EXIT_INNER_LOCK))) {
            this.client.openScreen(null);
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        this.client.keyboard.setRepeatEvents(false);
        if(ProjectInception.focusedInstance == gameInstance) {
            ProjectInception.focusedInstance = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
