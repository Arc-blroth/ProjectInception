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
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.World;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.system.MemoryUtil.memAddress;

@Environment(EnvType.CLIENT)
public class GameBlockEntityRenderer extends BlockEntityRenderer<GameBlockEntity> {

    private static final SpriteIdentifier LOADING = new SpriteIdentifier(new Identifier("textures/atlas/blocks.png"), new Identifier(ProjectInception.MODID, "block/inception"));
    private static final SpriteIdentifier GENERIC = new SpriteIdentifier(new Identifier("textures/atlas/blocks.png"), new Identifier(ProjectInception.MODID, "block/generic"));
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
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
        MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer().render(
                world,
                MinecraftClient.getInstance().getBlockRenderManager().getModel(state),
                state, pos, matrixStack, vertexConsumer,
                false, new Random(), state.getRenderingSeed(pos),
                OverlayTexture.DEFAULT_UV);
        if(blockEntity.isController() && !blockEntity.isOn()) {
            this.textureId = null;
            this.texture = null;
            this.lastTextureImage = null;
        }
        if(blockEntity.isController() && blockEntity.isOn() && blockEntity.getGameInstance() != null) {
            renderInner(blockEntity, matrixStack, vertexConsumers, light);
        }
        matrixStack.pop();
    }

    private void renderInner(GameBlockEntity blockEntity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
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

        VertexConsumer vertexConsumer = this.textureId != null
                ? vertexConsumers.getBuffer(RenderLayer.getText(this.textureId))
                : LOADING.getVertexConsumer(vertexConsumers, RenderLayer::getText);
        Direction direction = blockEntity.getCachedState().get(GameBlock.FACING);
        matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-direction.asRotation()));
        if (direction.equals(Direction.SOUTH) || direction.equals(Direction.EAST)) {
            matrixStack.translate(-Math.abs(direction.getOffsetX()), 0, 1);
        } else {
            matrixStack.translate(-Math.abs(direction.getOffsetZ()), 0, 0);
        }
        Matrix4f matrix4f = matrixStack.peek().getModel();

        int width = blockEntity.getSizeX();
        int height = blockEntity.getSizeY();

        if(this.textureId == null) {
            VertexConsumer bgVertexConsumer = GENERIC.getVertexConsumer(vertexConsumers, RenderLayer::getText);
            Sprite loadingSprite = LOADING.getSprite();
            Sprite bgSprite = GENERIC.getSprite();
            float squareLength = Math.min(width, height);
            float offsetX = (width - squareLength) / 2F;
            float offsetY = (height - squareLength) / 2F;
            float maxX1, maxY1;
            if(width < height) {
                maxX1 = 1;
                maxY1 = -height + 1 + offsetY;
            } else {
                maxX1 = -width + 1 + offsetX;
                maxY1 = 1;
            }
            renderQuads(bgVertexConsumer, matrix4f,
                    -width + 1, maxX1,
                    -height + 1, maxY1,
                    -1.01F,
                    bgSprite.getMinU(), bgSprite.getMaxU(),
                    bgSprite.getMaxV(), bgSprite.getMinV(),
                    light);
            renderQuads(vertexConsumer, matrix4f,
                    -width + 1 + offsetX, 1 - offsetX,
                    -height + 1 + offsetY, 1 - offsetY,
                    -1.01F,
                    loadingSprite.getMinU(), loadingSprite.getMaxU(),
                    loadingSprite.getMaxV(), loadingSprite.getMinV(),
                    light);
            float minX2, minY2;
            if(width < height) {
                minX2 = -width + 1;
                minY2 = 1 - offsetY;
            } else {
                minX2 = 1 - offsetX;
                minY2 = -height + 1;
            }
            renderQuads(bgVertexConsumer, matrix4f,
                    minX2, 1,
                    minY2, 1,
                    -1.01F,
                    bgSprite.getMinU(), bgSprite.getMaxU(),
                    bgSprite.getMaxV(), bgSprite.getMinV(),
                    light);
        } else {
            renderQuads(vertexConsumer, matrix4f,
                    -width + 1, 1,
                    -height + 1, 1,
                    -1.01F,
                    0.0F, 1.0F,
                    0.0F, 1.0F,
                    light);
        }
    }

    private void renderQuads(VertexConsumer vertexConsumer, Matrix4f matrix4f, float minX, float maxX, float minY, float maxY, float z, float minU, float maxU, float minV, float maxV, int light) {
        vertexConsumer.vertex(matrix4f, minX, minY, z).color(255, 255, 255, 255).texture(maxU, minV).light(light).next();
        vertexConsumer.vertex(matrix4f, maxX, minY, z).color(255, 255, 255, 255).texture(minU, minV).light(light).next();
        vertexConsumer.vertex(matrix4f, maxX, maxY, z).color(255, 255, 255, 255).texture(minU, maxV).light(light).next();
        vertexConsumer.vertex(matrix4f, minX, maxY, z).color(255, 255, 255, 255).texture(maxU, maxV).light(light).next();

        vertexConsumer.vertex(matrix4f, minX, minY, z).color(255, 255, 255, 255).texture(maxU, minV).light(light).next();
        vertexConsumer.vertex(matrix4f, minX, maxY, z).color(255, 255, 255, 255).texture(maxU, maxV).light(light).next();
        vertexConsumer.vertex(matrix4f, maxX, maxY, z).color(255, 255, 255, 255).texture(minU, maxV).light(light).next();
        vertexConsumer.vertex(matrix4f, maxX, minY, z).color(255, 255, 255, 255).texture(minU, minV).light(light).next();
    }

}
