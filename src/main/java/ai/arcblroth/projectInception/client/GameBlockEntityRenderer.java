package ai.arcblroth.projectInception.client;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.block.GameBlock;
import ai.arcblroth.projectInception.block.GameBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;

@Environment(EnvType.CLIENT)
public class GameBlockEntityRenderer extends BlockEntityRenderer<GameBlockEntity> {

    private static final SpriteIdentifier LOADING = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier(ProjectInception.MODID, "block/inception"));
    private static final SpriteIdentifier GENERIC = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier(ProjectInception.MODID, "block/generic"));
    private Sprite pointerSprite;

    public GameBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(GameBlockEntity blockEntity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if(pointerSprite == null) {
            this.pointerSprite = MinecraftClient.getInstance().getItemRenderer().getModels().getSprite(ProjectInception.INCEPTION_INTERFACE_ITEM);
        }
        if(blockEntity.isController() && blockEntity.isOn() && blockEntity.getGameInstance() != null) {
            matrixStack.push();
            renderInner(blockEntity, matrixStack, vertexConsumers, light);
            matrixStack.pop();
        }
    }

    private void renderInner(GameBlockEntity blockEntity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
        Identifier textureId = blockEntity.getGameInstance().getLastTextureId();
        VertexConsumer vertexConsumer = textureId != null
                ? vertexConsumers.getBuffer(RenderLayer.getText(textureId))
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

        if(textureId == null) {
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
            if(ProjectInceptionClient.focusedInstance == blockEntity.getGameInstance() && blockEntity.getGameInstance().shouldShowCursor()) {
                VertexConsumer ptVertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(pointerSprite.getAtlas().getId()));
                float left = 1F - (float) blockEntity.getGameInstance().getLastMouseX();
                float top = 1F - (float) blockEntity.getGameInstance().getLastMouseY();
                renderQuads(ptVertexConsumer, matrix4f,
                        -width + 1 + left * width - 0.1875F, -width + 1 + left * width,
                        -height + 1 + top * height - 0.25F, -height + 1 + top * height,
                        -1.015F,
                        pointerSprite.getMinU() + (pointerSprite.getMaxU() - pointerSprite.getMinU()) / 4, pointerSprite.getMaxU(),
                        pointerSprite.getMaxV(), pointerSprite.getMinV(),
                        light);
            }
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

    @Override
    public boolean rendersOutsideBoundingBox(GameBlockEntity blockEntity) {
        return blockEntity.isController();
    }

}
