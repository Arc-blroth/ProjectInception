package ai.arcblroth.projectInception.taterwebz;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;

public class CEFTexture extends AbstractTexture {
    
    public CEFTexture(int glId) {
        this.glId = glId;
    }
    
    @Override
    public void load(ResourceManager manager) {}

    @Override
    public void clearGlId() {}

}
