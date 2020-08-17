package ai.arcblroth.projectInception.block;


import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.apache.logging.log4j.util.TriConsumer;

public class GameMultiblock {

    public World world;
    public BlockPos controllerPos;
    public Direction left;
    public int sizeX;
    public int sizeY;

    private GameMultiblock() {}

    public static GameMultiblock getMultiblock(World world, BlockPos pos) {
        BlockEntity startGeneric = world.getBlockEntity(pos);
        if(startGeneric instanceof GameBlockEntity) {
            GameBlockEntity start = (GameBlockEntity) startGeneric;
            BlockState startBlockState = world.getBlockState(pos);
            Direction facing = startBlockState.get(GameBlock.FACING);
            Direction left = getLeft(facing);
            Direction right = left.getOpposite();
            int leftX = 0;
            int topY = pos.getY();
            int rightX = 0;
            int bottomY = pos.getY();

            BlockPos.Mutable queryPos = pos.mutableCopy();
            BlockPos queryPos2 = null;

            // Traverse up
            do {
                queryPos.setY(++topY);
            } while (world.getBlockEntity(queryPos) instanceof GameBlockEntity
                    && world.getBlockState(queryPos).get(GameBlock.FACING).equals(facing)
                    && !((GameBlockEntity) world.getBlockEntity(queryPos)).isOn());
            // Traverse down
            do {
                queryPos.setY(--bottomY);
            } while (world.getBlockEntity(queryPos) instanceof GameBlockEntity
                    && world.getBlockState(queryPos).get(GameBlock.FACING).equals(facing)
                    && !((GameBlockEntity) world.getBlockEntity(queryPos)).isOn());
            // Traverse left
            do {
                leftX++;
                queryPos2 = pos.offset(left, leftX);
            } while (world.getBlockEntity(queryPos2) instanceof GameBlockEntity
                    && world.getBlockState(queryPos2).get(GameBlock.FACING).equals(facing)
                    && !((GameBlockEntity) world.getBlockEntity(queryPos2)).isOn());
            // Traverse right
            do {
                rightX++;
                queryPos2 = pos.offset(right, rightX);
            } while (world.getBlockEntity(queryPos2) instanceof GameBlockEntity
                    && world.getBlockState(queryPos2).get(GameBlock.FACING).equals(facing)
                    && !((GameBlockEntity) world.getBlockEntity(queryPos2)).isOn());

            GameMultiblock multiblock = new GameMultiblock();
            multiblock.world = world;
            multiblock.controllerPos = pos.offset(left, leftX - 1);
            multiblock.controllerPos = new BlockPos(multiblock.controllerPos.getX(), topY - 1, multiblock.controllerPos.getZ());
            multiblock.left = left;
            multiblock.sizeX = leftX + rightX - 1;
            multiblock.sizeY = topY - bottomY - 1;
            return multiblock;
        } else {
            return null;
        }
    }

    public void forEachBlockEntity(TriConsumer<Integer, Integer, GameBlockEntity> action) {
        Direction right = this.left.getOpposite();
        for(int y = 0; y < this.sizeY; y++) {
           for(int left = 0; left < this.sizeX; left++) {
               BlockPos pos = this.controllerPos.offset(Direction.DOWN, y).offset(right, left);
               if(this.world.getBlockEntity(pos) instanceof GameBlockEntity) {
                   action.accept(left, y, (GameBlockEntity) this.world.getBlockEntity(pos));
               }
           }
        }
    }

    public static Direction getLeft(Direction facing) {
        Direction left;
        if(facing.equals(Direction.SOUTH)) left = Direction.EAST;
        else if(facing.equals(Direction.WEST)) left = Direction.SOUTH;
        else if(facing.equals(Direction.EAST))  left = Direction.NORTH;
        else left = Direction.WEST;
        return left;
    }

}
