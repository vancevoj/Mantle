package slimeknights.mantle.recipe.condition;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.mantle.Mantle;

/** Registers Mantle's custom recipe {@link ICondition} codecs */
@SuppressWarnings("unused")
public class MantleConditions {
  private MantleConditions() {}

  /** Registry for condition codecs */
  private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Mantle.modId);

  static {
    CONDITIONS.register("tag_empty", () -> TagEmptyCondition.CODEC);
    CONDITIONS.register("tag_filled", () -> TagFilledCondition.CODEC);
    CONDITIONS.register("tag_combination_filled", () -> TagCombinationCondition.CODEC);
  }

  /** Registers the condition codecs with the given event bus */
  public static void init(IEventBus bus) {
    CONDITIONS.register(bus);
  }
}
