package ai.arcblroth.projectInception.item;

import ai.arcblroth.projectInception.client.IColoredTooltipItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class BlockItemWithMagicness extends BlockItem implements IColoredTooltipItem {

    private final boolean glint;
    private final int color;
    private final boolean isRainbow;

    public BlockItemWithMagicness(Block block, Settings settings, boolean glint, boolean rainbow) {
        super(block, settings);
        this.glint = glint;
        this.color = 0xf76aa2;
        this.isRainbow = rainbow;
    }

    public BlockItemWithMagicness(Block block, Settings settings, boolean glint, int color) {
        super(block, settings);
        this.glint = glint;
        this.color = color;
        this.isRainbow = false;
    }

    public BlockItemWithMagicness(Block block, Settings settings, boolean rainbow) {
        this(block, settings, false, rainbow);
    }

    public BlockItemWithMagicness(Block block, Settings settings, int color) {
        this(block, settings, false, color);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public boolean isRainbow() {
        return isRainbow;
    }

}
