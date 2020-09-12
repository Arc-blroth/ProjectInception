package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.client.AbstractGameInstance;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;

public abstract class AbstractDisplayBlockEntity<T extends AbstractDisplayBlockEntity<T>> extends BlockEntity implements Tickable, BlockEntityClientSerializable {

    private final Class<T> selfType;
    protected GameMultiblock<T> multiblock;
    protected BlockPos controllerBlockPos = null;
    protected float offsetX = 0;
    protected float offsetY = 0;
    protected int sizeX = 0;
    protected int sizeY = 0;
    protected boolean isController = false;
    protected boolean isOn = false;

    public AbstractDisplayBlockEntity(BlockEntityType<?> type, Class<T> selfType) {
        super(type);
        this.selfType = selfType;
    }

    public void turnOn(GameMultiblock<T> multiblock, int left, int top) {
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
        this.isOn = false;
        this.markDirty();
        if(!world.isClient) this.sync();
    }

    @SuppressWarnings("unchecked")
    public void tick() {
        if(isOn && !this.isController) {
            BlockEntity blockEntity = world.getBlockEntity(controllerBlockPos);
            if(!(selfType.isInstance(blockEntity))) {
                isOn = false;
                multiblock = null;
                controllerBlockPos = null;
                offsetX = offsetY = sizeX = sizeY = 0;
                this.markDirty();
                if(!world.isClient) this.sync();
            } else if (!((AbstractDisplayBlockEntity<T>) blockEntity).isOn) {
                isOn = false;
                multiblock = null;
                controllerBlockPos = null;
                offsetX = offsetY = sizeX = sizeY = 0;
                this.markDirty();
                if(!world.isClient) this.sync();
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
        this.markDirty();
        if(!world.isClient) {
            this.sync();
        }
    }

    public boolean isController() {
        return isController;
    }

    public boolean isOn() {
        return isOn;
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

    public abstract AbstractGameInstance<T> getGameInstance();

}
