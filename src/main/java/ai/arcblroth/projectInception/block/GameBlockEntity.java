package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.GameInstance;
import ai.arcblroth.projectInception.ProjectInception;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;

public class GameBlockEntity extends BlockEntity implements Tickable, BlockEntityClientSerializable {

    private GameMultiblock multiblock;
    private BlockPos controllerBlockPos = null;
    private float offsetX = 0;
    private float offsetY = 0;
    private int sizeX = 0;
    private int sizeY = 0;
    private boolean isController = false;
    private boolean isOn = false;
    private GameInstance gameInstance = null;

    public GameBlockEntity() {
        super(ProjectInception.GAME_BLOCK_ENTITY_TYPE);
    }

    public void turnOn(GameMultiblock multiblock, int left, int top) {
        this.isOn = true;
        this.multiblock = multiblock;
        this.controllerBlockPos = multiblock.controllerPos;
        this.offsetX = (float) left / multiblock.sizeX;
        this.offsetY = (float) top / multiblock.sizeY;
        this.sizeX = multiblock.sizeX;
        this.sizeY = multiblock.sizeY;
        this.markDirty();
        if(!world.isClient) this.sync();
    }

    public void turnOff() {
        if(this.world != null && this.world.isClient && this.isController && gameInstance != null) {
            gameInstance.stop(true);
        }
        this.isOn = false;
        this.markDirty();
        if(!world.isClient) this.sync();
    }


    public void tick() {
        if(isOn && !this.isController) {
            BlockEntity blockEntity = world.getBlockEntity(controllerBlockPos);
            if(!(blockEntity instanceof GameBlockEntity)) {
                isOn = false;
                multiblock = null;
                controllerBlockPos = null;
                gameInstance = null;
                offsetX = offsetY = sizeX = sizeY = 0;
                this.markDirty();
                if(!world.isClient) this.sync();
            } else if (!((GameBlockEntity) blockEntity).isOn) {
                isOn = false;
                multiblock = null;
                controllerBlockPos = null;
                gameInstance = null;
                offsetX = offsetY = sizeX = sizeY = 0;
                this.markDirty();
                if(!world.isClient) this.sync();
            } else if(world.isClient) {
                this.gameInstance = ((GameBlockEntity) blockEntity).gameInstance;
            }
        }
    }

    @Override
    public void fromClientTag(CompoundTag compoundTag) {
        this.isController = compoundTag.getBoolean("isController");
        this.isOn = compoundTag.getBoolean("isOn");
        this.offsetX = compoundTag.getFloat("offsetX");
        this.offsetY = compoundTag.getFloat("offsetY");
        if(compoundTag.contains("controllerX")) {
            controllerBlockPos = new BlockPos(compoundTag.getInt("controllerX"), compoundTag.getInt("controllerY"), compoundTag.getInt("controllerZ"));
        } else {
            controllerBlockPos = null;
        }
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        tag.putBoolean("isController", isController);
        tag.putBoolean("isOn", isOn);
        tag.putFloat("offsetX", offsetX);
        tag.putFloat("offsetY", offsetY);
        if(controllerBlockPos != null) {
            tag.putInt("controllerX", controllerBlockPos.getX());
            tag.putInt("controllerY", controllerBlockPos.getY());
            tag.putInt("controllerZ", controllerBlockPos.getZ());
        }
        return tag;
    }

    public BlockPos getControllerBlockPos() {
        return controllerBlockPos;
    }

    public void setController(boolean controller) {
        isController = controller;
        if(this.world != null && this.world.isClient && this.multiblock != null) {
            gameInstance = new GameInstance(this.multiblock);
            gameInstance.start();
        }
        this.markDirty();
        if(!world.isClient) this.sync();
    }

    public boolean isController() {
        return isController;
    }

    public boolean isOn() {
        return isOn;
    }

    public GameInstance getGameInstance() {
        return gameInstance;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

}
