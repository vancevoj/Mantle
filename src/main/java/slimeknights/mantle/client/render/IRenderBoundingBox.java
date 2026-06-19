package slimeknights.mantle.client.render;

import net.minecraft.world.phys.AABB;

/**
 * Interface for block entities that wish to provide a custom render bounding box to their {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}.
 * <p>
 * In 1.21, {@code getRenderBoundingBox} was moved off of {@link net.minecraft.world.level.block.entity.BlockEntity} and onto
 * {@link net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension}. A renderer can implement
 * {@code getRenderBoundingBox} and delegate to this interface to restore the pre-1.21 behavior of a block entity declaring
 * its own enlarged cull box. {@link InventoryBlockEntityRenderer} does exactly this.
 */
public interface IRenderBoundingBox {
  /** Gets the bounding box used to determine when this block entity's renderer should be culled. */
  AABB getRenderBoundingBox();
}
