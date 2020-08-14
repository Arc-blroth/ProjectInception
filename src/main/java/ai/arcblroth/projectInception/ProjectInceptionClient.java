package ai.arcblroth.projectInception;

import ai.arcblroth.projectInception.block.GameBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;

import java.lang.ref.WeakReference;

public class ProjectInceptionClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(ProjectInception.GAME_BLOCK_ENTITY_TYPE, GameBlockEntityRenderer::new);
    }

}
