package ai.arcblroth.projectInception;

import ai.arcblroth.projectInception.client.AbstractGameInstance;
import ai.arcblroth.projectInception.client.GameBlockEntityRenderer;
import ai.arcblroth.projectInception.client.TaterwebzBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.client.options.KeyBinding;
import net.openhft.chronicle.queue.ChronicleQueue;
import org.lwjgl.glfw.GLFW;

public class ProjectInceptionClient implements ClientModInitializer {

    public static KeyBinding EXIT_INNER_LOCK;

    public static Process TATERWEBZ_CHILD_PROCESS;
    public static ChronicleQueue TATERWEBZ_CHILD_QUEUE;

    public static AbstractGameInstance<?> focusedInstance = null;

    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(ProjectInception.GAME_BLOCK_ENTITY_TYPE, GameBlockEntityRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(ProjectInception.TATERWEBZ_BLOCK_ENTITY_TYPE, TaterwebzBlockEntityRenderer::new);

        EXIT_INNER_LOCK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.project_inception.exit_inner_lock", GLFW.GLFW_KEY_F12, "key.categories.project_inception"));
    }

}
