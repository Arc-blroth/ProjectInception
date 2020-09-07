package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.client.InceptionInterfaceScreen;
import ai.arcblroth.projectInception.item.InceptionInterfaceItem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class TaterwebzBlock extends AbstractDisplayBlock<TaterwebzBlockEntity> {

    public TaterwebzBlock(Settings settings) {
        super(settings, TaterwebzBlockEntity.class);
    }

    @Override
    public TaterwebzBlockEntity createDisplayBlockEntity(BlockView world) {
        return new TaterwebzBlockEntity();
    }

    @Override
    public void click(TaterwebzBlockEntity te, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, double hitX, double hitY) {
        if (world.isClient) {
            if (player.getStackInHand(hand).getItem() instanceof InceptionInterfaceItem) {
                //MinecraftClient.getInstance().openScreen(new InceptionInterfaceScreen(ge));
                //player.sendMessage(new TranslatableText("message.project_inception.escape", ProjectInceptionClient.EXIT_INNER_LOCK.getBoundKeyLocalizedText()), true);
            } else {
                te.getTaterwebzInstance().click(hitX, hitY);
            }
        }
    }

}
