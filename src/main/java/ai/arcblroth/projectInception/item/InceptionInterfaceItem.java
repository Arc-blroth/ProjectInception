package ai.arcblroth.projectInception.item;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.block.GameBlockEntity;
import ai.arcblroth.projectInception.client.IColoredTooltipItem;
import ai.arcblroth.projectInception.client.InceptionInterfaceScreen;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;

public class InceptionInterfaceItem extends Item implements IColoredTooltipItem {

    public InceptionInterfaceItem(Settings settings) {
        super(settings);
    }

    @Override
    public int getColor() {
        return ProjectInception.INCEPTION_BLOCK_ITEM.getColor();
    }

    @Override
    public boolean isRainbow() {
        return true;
    }

}
