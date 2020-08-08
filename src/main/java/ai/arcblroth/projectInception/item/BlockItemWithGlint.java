package ai.arcblroth.projectInception.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class BlockItemWithGlint extends BlockItem {

    public BlockItemWithGlint(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

}
