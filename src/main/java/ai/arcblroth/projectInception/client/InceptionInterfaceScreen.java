package ai.arcblroth.projectInception.client;

import ai.arcblroth.projectInception.GameInstance;
import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.block.GameBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.NarratorManager;

import static org.lwjgl.glfw.GLFW.*;

public class InceptionInterfaceScreen extends Screen {

    private final GameBlockEntity blockEntity;
    private final GameInstance gameInstance;

    public InceptionInterfaceScreen(GameBlockEntity blockEntity) {
        super(NarratorManager.EMPTY);
        this.blockEntity = blockEntity;
        this.gameInstance = blockEntity.getGameInstance();
        ProjectInception.focusedInstance = gameInstance;
    }

    @Override
    public void init() {
        this.client.keyboard.setRepeatEvents(true);
        double x = gameInstance.getLastMouseX() * client.getWindow().getWidth();
        double y = gameInstance.getLastMouseY() * client.getWindow().getHeight();
        InputUtil.setCursorParameters(this.client.getWindow().getHandle(), GLFW_CURSOR_DISABLED, x, y);
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

}
