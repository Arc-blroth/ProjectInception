package ai.arcblroth.projectInception.block;

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

public abstract class AbstractDisplayBlock<T extends AbstractDisplayBlockEntity> extends BlockWithEntity {

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private final Class<T> blockEntityType;

    public AbstractDisplayBlock(Settings settings, Class<T> blockEntityType) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
        this.blockEntityType = blockEntityType;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(FACING);
    }

    @Override
    public final BlockEntity createBlockEntity(BlockView world) {
        return createDisplayBlockEntity(world);
    }

    public abstract T createDisplayBlockEntity(BlockView world);

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        if(blockEntityType.isInstance(be)) {
            T ge = (T) be;
            Direction dir = state.get(FACING);
            if(hit.getSide().equals(dir.getOpposite())) {
                if (hand.equals(Hand.MAIN_HAND)) {
                    if (!ge.isOn()) {
                        if (player.getStackInHand(hand).isEmpty()) {
                            GameMultiblock<T> multiblock = GameMultiblock.getMultiblock(blockEntityType, world, pos);
                            if (multiblock != null) {
                                multiblock.forEachBlockEntity((left, y, blockEntity) -> {
                                    blockEntity.turnOn(multiblock, left, y);
                                });
                                BlockEntity controller = world.getBlockEntity(multiblock.controllerPos);
                                ((T) controller).setController(true);
                                return ActionResult.SUCCESS;
                            }
                        } else if (world.isClient && !(player.getStackInHand(hand).getItem() instanceof BlockItem)) {
                            MinecraftClient.getInstance().player.sendMessage(new TranslatableText("message.project_inception.empty_hand_warning"), true);
                        }
                    } else {
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
                        click(ge, state, world, pos, player, hand, hitX, hitY);
                        return ActionResult.SUCCESS;
                    }
                }
            }
        }
        return ActionResult.PASS;
    }

    public abstract void click(T blockEntity, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, double hitX, double hitY);

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        destroyMultiblock(world, pos);
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        destroyMultiblock(world, pos);
    }

    @SuppressWarnings("unchecked")
    private void destroyMultiblock(World world, BlockPos pos) {
        if(blockEntityType.isInstance(world.getBlockEntity(pos))) {
            T blockEntity = (T) world.getBlockEntity(pos);
            if(blockEntity.getControllerBlockPos() != null && blockEntityType.isInstance(world.getBlockEntity(blockEntity.getControllerBlockPos()))) {
               T controllerBlockEntity = (T) world.getBlockEntity(blockEntity.getControllerBlockPos());
               controllerBlockEntity.turnOff();
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
