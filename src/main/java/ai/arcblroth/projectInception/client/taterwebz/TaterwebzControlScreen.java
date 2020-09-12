package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.block.TaterwebzBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class TaterwebzControlScreen extends Screen {

    private final TaterwebzBlockEntity blockEntity;
    private ButtonWidget backButton, forwardButton, reloadButton, goButton;
    private TextFieldWidget urlField;

    public TaterwebzControlScreen(TaterwebzBlockEntity blockEntity) {
        super(NarratorManager.EMPTY);
        this.blockEntity = blockEntity;
    }

    @Override
    public void init() {
        client.keyboard.setRepeatEvents(true);
        backButton    = addButton(new ButtonWidget(this.width / 6 - 10, this.height / 2 - 10, 20, 20, new LiteralText("←"), (buttonWidget) -> {
            blockEntity.getGameInstance().goBack();
            this.onClose();
        }));
        forwardButton = addButton(new ButtonWidget(this.width / 6 + 11, this.height / 2 - 10, 20, 20, new LiteralText("→"), (buttonWidget) -> {
            blockEntity.getGameInstance().goForward();
            this.onClose();
        }));
        reloadButton  = addButton(new ButtonWidget(this.width / 6 + 32, this.height / 2 - 10, 20, 20, new LiteralText("⟳"), (buttonWidget) -> {
            blockEntity.getGameInstance().reload();
            this.onClose();
        }));
        goButton      = addButton(new ButtonWidget(this.width * 5 / 6 + 10, this.height / 2 - 10, 20, 20, new LiteralText("Go"), (buttonWidget) -> {
            gotoUrl(urlField.getText());
        }));
        urlField = new TextFieldWidget(this.textRenderer, this.width / 6 + 55, this.height / 2 - 10, this.width * 2 / 3 - 48, 20, NarratorManager.EMPTY);
        urlField.setMaxLength(16000);
        urlField.setEditable(true);
        urlField.setText(blockEntity.getGameInstance().getCurrentURL());
        this.children.add(urlField);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.urlField.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode != '\r' && keyCode != '\n') {
            return false;
        } else {
            if(!this.urlField.getText().isEmpty()) {
                this.gotoUrl(this.urlField.getText());
                return true;
            } else {
                return false;
            }
        }
    }

    private void gotoUrl(String url) {
        if(!url.isEmpty()) {
            blockEntity.getGameInstance().gotoUrl(url);
            this.onClose();
        }
    }

}
