package slimeknights.mantle.registration;

import net.minecraft.world.level.block.entity.BlockEntityType;
import slimeknights.mantle.block.entity.MantleHangingSignBlockEntity;
import slimeknights.mantle.block.entity.MantleSignBlockEntity;

import javax.annotation.Nullable;

/**
 * Various objects registered under Mantle.
 * <p>
 * NeoForge 1.21 removed {@code @ObjectHolder} injection, so these are populated during block entity type
 * registration (see the {@code BLOCK_ENTITY_TYPE} branch of {@code Mantle#register}) instead of being injected.
 */
public class MantleRegistrations {
  private MantleRegistrations() {}

  /** Sign block entity type, assigned during registration. May be null before block entity types are registered. */
  @Nullable
  public static BlockEntityType<MantleSignBlockEntity> SIGN = null;

  /** Hanging sign block entity type, assigned during registration. May be null before block entity types are registered. */
  @Nullable
  public static BlockEntityType<MantleHangingSignBlockEntity> HANGING_SIGN = null;
}
