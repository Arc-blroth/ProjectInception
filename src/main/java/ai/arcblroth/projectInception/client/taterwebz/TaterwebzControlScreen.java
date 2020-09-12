package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.block.TaterwebzBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.NarratorManager;

public class TaterwebzControlScreen extends Screen {

    private final TaterwebzBlockEntity blockEntity;

    protected TaterwebzControlScreen(TaterwebzBlockEntity blockEntity) {
        super(NarratorManager.EMPTY);
        this.blockEntity = blockEntity;
    }

}
