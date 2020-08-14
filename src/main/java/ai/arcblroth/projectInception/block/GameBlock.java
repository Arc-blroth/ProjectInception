package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.client.InceptionInterfaceScreen;
import ai.arcblroth.projectInception.item.InceptionInterfaceItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
            GameBlockEntity ge = (GameBlockEntity) be;
            if (hand.equals(Hand.MAIN_HAND)) {
                if (!ge.isOn()) {
                    if(player.getStackInHand(hand).isEmpty()) {
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
                } else {
                    if(player.getStackInHand(hand).getItem() instanceof InceptionInterfaceItem) {
                        if (world.isClient) {
                            MinecraftClient.getInstance().openScreen(new InceptionInterfaceScreen(ge));
                        }
                        player.sendMessage(new TranslatableText("message.project_inception.escape"), true);
                        return ActionResult.SUCCESS;
                    } else {
                        if (world.isClient) {
                            Direction dir = state.get(FACING);
                            if (hit.getSide().equals(dir.getOpposite())) {
                                Direction left = GameMultiblock.getLeft(dir);
                                Vec3d hitPoint = hit.getPos().subtract(pos.getX(), pos.getY(), pos.getZ());
                                double hitX = (
                                        left.getDirection().equals(Direction.AxisDirection.POSITIVE)
                                                ? (1 - hitPoint.getComponentAlongAxis(left.getAxis()))
                                                : hitPoint.getComponentAlongAxis(left.getAxis())
                                ) / ge.getSizeX();
                                double hitY = (1 - hitPoint.getComponentAlongAxis(Direction.Axis.Y)) / ge.getSizeY();
                                hitX += ge.getOffsetX();
                                hitY += ge.getOffsetY();
                                ge.getGameInstance().click(hitX, hitY);
                                return ActionResult.SUCCESS;
                            }
                        }
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
