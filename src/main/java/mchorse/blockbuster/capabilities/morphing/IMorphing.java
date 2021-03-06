package mchorse.blockbuster.capabilities.morphing;

import net.minecraft.util.ResourceLocation;

/**
 * Morphing interface
 *
 * This interface is responsible for morphing. See {@link Morphing} class for
 * default implementation.
 */
public interface IMorphing
{
    /**
     * Get id of model
     */
    public String getModel();

    /**
     * Get id of model's skin
     */
    public ResourceLocation getSkin();

    /**
     * Reset model and skin (i.e. turn off morphing)
     */
    public void reset();

    /**
     * Set current model
     */
    public void setModel(String newModel);

    /**
     * Set current skin
     */
    public void setSkin(ResourceLocation newSkin);
}
