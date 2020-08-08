package ai.arcblroth.projectInception.block;

import ai.arcblroth.projectInception.ProjectInception;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.system.MemoryUtil.memAddress;

@Environment(EnvType.CLIENT)
public class GameBlockEntityRenderer extends BlockEntityRenderer<GameBlockEntity> {

    private static final Identifier LOADING = new Identifier("minecraft", "block/black_concrete");
    private static final float WIDTH_MULTI = 16/4F;
    private static final float HEIGHT_MULTI = 9/4F;
    private ByteBuffer texture;
    private NativeImageBackedTexture lastTextureImage;
    private Identifier textureId;

    public GameBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
        this.textureId = null;
        this.texture = null;
    }

    @Override
    public void render(GameBlockEntity blockEntity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = blockEntity.getWorld();
        BlockPos pos = blockEntity.getPos();
        BlockState state = world.getBlockState(pos);

        matrixStack.push();
        RenderLayer renderLayer = RenderLayers.getBlockLayer(state);
        if(!blockEntity.isOn()) {
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
            MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer().render(
                    world,
                    MinecraftClient.getInstance().getBlockRenderManager().getModel(state),
                    state, pos, matrixStack, vertexConsumer,
                    false, new Random(), state.getRenderingSeed(pos),
                    OverlayTexture.DEFAULT_UV);
        } else {
            this.texture = blockEntity.getGameInstance().getLastTexture(this.texture);
            int lastWidth = blockEntity.getGameInstance().getLastWidth();
            int lastHeight = blockEntity.getGameInstance().getLastHeight();
            if(this.textureId == null) {
                if(this.texture != null) {
                    NativeImage image = new NativeImage(NativeImage.Format.ABGR, lastWidth, lastHeight, true, memAddress(this.texture));
                    this.lastTextureImage = new NativeImageBackedTexture(image);
                    this.textureId = dispatcher.textureManager.registerDynamicTexture("project_inception_game", lastTextureImage);
                }
            } else {
                if(this.lastTextureImage != null) {
                    try {
                        this.lastTextureImage.upload();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            Identifier actualTextureId = this.textureId != null ? this.textureId : LOADING;
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(actualTextureId));
            Direction direction = blockEntity.getCachedState().get(GameBlock.FACING);
            matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-direction.asRotation()));
            if (direction.equals(Direction.SOUTH) || direction.equals(Direction.EAST)) {
                matrixStack.translate(-Math.abs(direction.getOffsetX()), 0, 1);
            } else {
                matrixStack.translate(-Math.abs(direction.getOffsetZ()), 0, 0);
            }
            Matrix4f matrix4f = matrixStack.peek().getModel();
            vertexConsumer.vertex(matrix4f, -WIDTH_MULTI,             0.0F, -0.01F).color(255, 255, 255, 255).texture(1.0F, 0.0F).light(light).next();
            vertexConsumer.vertex(matrix4f,  WIDTH_MULTI,             0.0F, -0.01F).color(255, 255, 255, 255).texture(0.0F, 0.0F).light(light).next();
            vertexConsumer.vertex(matrix4f,  WIDTH_MULTI, 2 * HEIGHT_MULTI, -0.01F).color(255, 255, 255, 255).texture(0.0F, 1.0F).light(light).next();
            vertexConsumer.vertex(matrix4f, -WIDTH_MULTI, 2 * HEIGHT_MULTI, -0.01F).color(255, 255, 255, 255).texture(1.0F, 1.0F).light(light).next();

            vertexConsumer.vertex(matrix4f, -WIDTH_MULTI,             0.0F, -0.01F).color(255, 255, 255, 255).texture(1.0F, 0.0F).light(light).next();
            vertexConsumer.vertex(matrix4f, -WIDTH_MULTI, 2 * HEIGHT_MULTI, -0.01F).color(255, 255, 255, 255).texture(1.0F, 1.0F).light(light).next();
            vertexConsumer.vertex(matrix4f,  WIDTH_MULTI, 2 * HEIGHT_MULTI, -0.01F).color(255, 255, 255, 255).texture(0.0F, 1.0F).light(light).next();
            vertexConsumer.vertex(matrix4f,  WIDTH_MULTI,             0.0F, -0.01F).color(255, 255, 255, 255).texture(0.0F, 0.0F).light(light).next();
        }
        matrixStack.pop();
    }

}
