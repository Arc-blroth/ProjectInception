package ai.arcblroth.projectInception.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class GameBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public GameBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(FACING);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new GameBlockEntity();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof GameBlockEntity) {
            if (hand.equals(Hand.MAIN_HAND) && player.getStackInHand(hand).isEmpty()) {
                if (!((GameBlockEntity) world.getBlockEntity(pos)).isOn()) {
                    GameMultiblock multiblock = GameMultiblock.getMultiblock(world, pos);
                    if (multiblock != null) {
                        multiblock.forEachBlockEntity((left, y, blockEntity) -> {
                            blockEntity.turnOn(multiblock.controllerPos, (float) left / multiblock.sizeX, (float) y / multiblock.sizeY, multiblock.sizeX, multiblock.sizeY);
                        });
                        BlockEntity controller = world.getBlockEntity(multiblock.controllerPos);
                        if (controller instanceof GameBlockEntity) {
                            ((GameBlockEntity) controller).setController(true);
                        }
                        return ActionResult.SUCCESS;
                    }
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        destroyMultiblock(world, pos);
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        destroyMultiblock(world, pos);
    }

    private void destroyMultiblock(World world, BlockPos pos) {
        if(world.getBlockEntity(pos) instanceof GameBlockEntity) {
            GameBlockEntity blockEntity = (GameBlockEntity) world.getBlockEntity(pos);
            if(blockEntity.getControllerBlockPos() != null && world.getBlockEntity(blockEntity.getControllerBlockPos()) instanceof GameBlockEntity) {
                if(blockEntity.getControllerBlockPos() != null) {
                    GameBlockEntity controllerBlockEntity = (GameBlockEntity) world.getBlockEntity(blockEntity.getControllerBlockPos());
                    controllerBlockEntity.turnOff();
                }
            }
        }
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return (BlockState)state.with(FACING, rotation.rotate((Direction)state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.get(FACING)));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerFacing());
    }

}
