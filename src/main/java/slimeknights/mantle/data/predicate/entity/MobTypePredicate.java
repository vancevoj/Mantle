package slimeknights.mantle.data.predicate.entity;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.NamedComponentRegistry;

/**
 * Predicate matching a specific mob type.
 * Since {@code MobType} was removed in 1.21, mob types are now represented as entity type tags
 * (e.g. {@link net.minecraft.tags.EntityTypeTags#UNDEAD}). The {@link #MOB_TYPES} registry maps
 * the legacy mob type names to their equivalent entity type tags.
 */
public record MobTypePredicate(TagKey<EntityType<?>> tag) implements LivingEntityPredicate {
  /**
   * Registry of mob types, to allow addons to register types.
   * Each entry maps a name to the entity type tag representing the former mob type.
   * TODO: support registering via IMC
   */
  public static final NamedComponentRegistry<TagKey<EntityType<?>>> MOB_TYPES = new NamedComponentRegistry<>("Unknown mob type");
  /** Loader for a mob type predicate */
  public static RecordLoadable<MobTypePredicate> LOADER = RecordLoadable.create(MOB_TYPES.requiredField("mobs", MobTypePredicate::tag), MobTypePredicate::new);

  @Override
  public boolean matches(LivingEntity input) {
    return input.getType().is(tag);
  }

  @Override
  public RecordLoadable<? extends LivingEntityPredicate> getLoader() {
    return LOADER;
  }
}
