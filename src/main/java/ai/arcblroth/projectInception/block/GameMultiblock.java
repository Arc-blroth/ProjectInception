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

            // Traverse up
            do {
                topY++;
            } while (world.getBlockEntity(new BlockPos(pos.getX(), topY, pos.getZ())) instanceof GameBlockEntity
                    && world.getBlockState(new BlockPos(pos.getX(), topY, pos.getZ())).get(GameBlock.FACING).equals(facing));
            // Traverse down
            do {
                bottomY--;
            } while (world.getBlockEntity(new BlockPos(pos.getX(), bottomY, pos.getZ())) instanceof GameBlockEntity
                    && world.getBlockState(new BlockPos(pos.getX(), bottomY, pos.getZ())).get(GameBlock.FACING).equals(facing));
            // Traverse left
            do {
                leftX++;
            } while (world.getBlockEntity(pos.offset(left, leftX)) instanceof GameBlockEntity
                    && world.getBlockState(pos.offset(left, leftX)).get(GameBlock.FACING).equals(facing));
            // Traverse right
            do {
                rightX++;
            } while (world.getBlockEntity(pos.offset(right, rightX)) instanceof GameBlockEntity
                    && world.getBlockState(pos.offset(right, rightX)).get(GameBlock.FACING).equals(facing));

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
