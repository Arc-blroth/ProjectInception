package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.mc.GameInstance;
import ai.arcblroth.projectInception.ProjectInception;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

public class GameBlockEntity extends AbstractDisplayBlockEntity<GameBlockEntity> {

    private GameInstance gameInstance = null;

    public GameBlockEntity() {
        super(ProjectInception.GAME_BLOCK_ENTITY_TYPE, GameBlockEntity.class);
    }

    @Override
    public void turnOff() {
        if(this.world != null && this.world.isClient && this.isController && gameInstance != null) {
            gameInstance.stop(true);
        }
        super.turnOff();
    }

    public void tick() {
        super.tick();
        if(isOn && !this.isController) {
            BlockEntity blockEntity = world.getBlockEntity(controllerBlockPos);
            if(!(blockEntity instanceof GameBlockEntity)) {
                gameInstance = null;
            } else if (!((GameBlockEntity) blockEntity).isOn) {
                gameInstance = null;
            } else if(world.isClient) {
                this.gameInstance = ((GameBlockEntity) blockEntity).gameInstance;
            }
        }
    }

    public void setController(boolean controller) {
        super.setController(controller);
        if(this.world != null && this.world.isClient && this.multiblock != null) {
            gameInstance = new GameInstance((GameMultiblock<GameBlockEntity>) this.multiblock);
            gameInstance.start();
        }
        if(!world.isClient) {
            if(world instanceof ServerWorld) {
                ((ServerWorld) world).getPlayers(EntityPredicates.maxDistance(
                        multiblock.controllerPos.getX(), multiblock.controllerPos.getY(), multiblock.controllerPos.getZ(), 48
                )).forEach(player -> {
                    player.sendSystemMessage(new TranslatableText("message.project_inception.loading"), Util.NIL_UUID);
                });
            }
        }
    }

    public GameInstance getGameInstance() {
        return gameInstance;
    }

}
