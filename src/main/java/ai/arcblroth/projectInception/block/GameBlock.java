package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.client.InceptionInterfaceScreen;
import ai.arcblroth.projectInception.item.InceptionInterfaceItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

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
            if (player.getStackInHand(hand).getItem() instanceof InceptionInterfaceItem) {
                MinecraftClient.getInstance().openScreen(new InceptionInterfaceScreen(ge));
                player.sendMessage(new TranslatableText("message.project_inception.escape", ProjectInceptionClient.EXIT_INNER_LOCK.getBoundKeyLocalizedText()), true);
            } else {
                ge.getGameInstance().click(hitX, hitY);
            }
        }
    }

}
