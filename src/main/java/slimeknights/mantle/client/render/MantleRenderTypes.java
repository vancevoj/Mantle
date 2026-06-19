package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import slimeknights.mantle.Mantle;

/**
 * Class for render types defined by Mantle
 */
public class MantleRenderTypes extends RenderType {

  private MantleRenderTypes(String name, VertexFormat format, Mode mode, int bufferSize, boolean useDelegate, boolean needsSorting, Runnable setupTaskIn, Runnable clearTaskIn) {
    super(name, format, mode, bufferSize, useDelegate, needsSorting, setupTaskIn, clearTaskIn);
  }

  /** Extension of {@link RenderType#POSITION_COLOR_TEX_LIGHTMAP_SHADER} with fog information based on {@link RenderType#ENTITY_TRANSLUCENT_CULL} */
  public static final RenderStateShard.ShaderStateShard FLUID_SHADER = new RenderStateShard.ShaderStateShard(MantleShaders::getConfiguredFluidShader);

  /**
   * Render type used for the fluid renderer.
   * <p>1.21: uses the vanilla {@link RenderType#entityTranslucentCull} shader and {@link DefaultVertexFormat#NEW_ENTITY}
   * format (vertices must include overlay + normal, see {@link FluidRenderer#putTexturedQuad}). The previous custom
   * {@link #FLUID_SHADER} / {@code POSITION_COLOR_TEX_LIGHTMAP} render type was invisible under Iris/Oculus, which only
   * render geometry on render types they recognize; the vanilla entity-translucent shader is fully supported.
   */
  public static final RenderType FLUID = entityTranslucentCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);

  /**
   * Render type used for the structure renderer
   */
  public static final VertexFormat BLOCK_WITH_OVERLAY = VertexFormat.builder()
    .add("Position", VertexFormatElement.POSITION)
    .add("Color", VertexFormatElement.COLOR)
    .add("UV0", VertexFormatElement.UV0)
    .add("UV1", VertexFormatElement.UV1)
    .add("UV2", VertexFormatElement.UV2)
    .add("Normal", VertexFormatElement.NORMAL)
    .padding(1)
    .build();

  public static final RenderType TRANSLUCENT_FULLBRIGHT = create(
    Mantle.modId + ":translucent_fullbright",
    BLOCK_WITH_OVERLAY, Mode.QUADS, 256, false, false,
    RenderType.CompositeState.builder()
      .setShaderState(new RenderStateShard.ShaderStateShard(MantleShaders::getBlockFullBrightShader))
      .setLightmapState(new RenderStateShard.LightmapStateShard(false))
      .setOverlayState(OVERLAY)
      .setTextureState(BLOCK_SHEET_MIPPED)
      .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
      .createCompositeState(false));
}
