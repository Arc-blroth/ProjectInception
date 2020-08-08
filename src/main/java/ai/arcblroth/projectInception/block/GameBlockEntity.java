package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.GameInstance;
import ai.arcblroth.projectInception.ProjectInception;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.ActionResult;

public class GameBlockEntity extends BlockEntity {

    private boolean isOn; // this WILL NOT persist in saving
    private GameInstance gameInstance;

    public GameBlockEntity() {
        super(ProjectInception.GAME_BLOCK_ENTITY_TYPE);
        this.isOn = false;
        this.gameInstance = new GameInstance();
    }

    public ActionResult turnOn() {
        if(this.world != null && this.world.isClient && !isOn) {
            gameInstance.start();
            this.isOn = true;
            return ActionResult.SUCCESS;
        } else return ActionResult.PASS;
    }

    public boolean isOn() {
        return isOn;
    }

    public GameInstance getGameInstance() {
        return gameInstance;
    }

}
