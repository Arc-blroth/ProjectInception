package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.client.InceptionInterfaceScreen;
import ai.arcblroth.projectInception.item.InceptionInterfaceItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class GameBlock extends AbstractDisplayBlock<GameBlockEntity> {

    public GameBlock(Settings settings) {
        super(settings, GameBlockEntity.class);
    }

    @Override
    public GameBlockEntity createDisplayBlockEntity(BlockView world) {
        return new GameBlockEntity();
    }

    @Override
    public void click(GameBlockEntity ge, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, double hitX, double hitY) {
        if (world.isClient) {
            clickClient(ge, state, world, pos, player, hand, hitX, hitY);
        }
    }

    @Environment(EnvType.CLIENT)
    public void clickClient(GameBlockEntity ge, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, double hitX, double hitY) {
        if (player.getStackInHand(hand).getItem() instanceof InceptionInterfaceItem) {
            MinecraftClient.getInstance().openScreen(new InceptionInterfaceScreen(ge));
            player.sendMessage(new TranslatableText("message.project_inception.escape", ProjectInceptionClient.EXIT_INNER_LOCK.getBoundKeyLocalizedText()), true);
        } else {
            ge.getGameInstance().click(hitX, hitY);
        }
    }

}
