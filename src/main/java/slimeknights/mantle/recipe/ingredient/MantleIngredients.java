package slimeknights.mantle.recipe.ingredient;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.data.ItemNameIngredient;
import slimeknights.mantle.recipe.data.NBTNameIngredient;

/** Registers Mantle's custom {@link IngredientType}s */
@SuppressWarnings("unused")
public class MantleIngredients {
  private MantleIngredients() {}

  /** Registry for ingredient types */
  private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Mantle.modId);

  static {
    INGREDIENT_TYPES.register("potion", () -> PotionIngredient.TYPE);
    INGREDIENT_TYPES.register("potion_display", () -> PotionDisplayIngredient.TYPE);
    INGREDIENT_TYPES.register("fluid_container", () -> FluidContainerIngredient.TYPE);
    // datagen-only ingredients, registered so their codec can serialize during data generation
    INGREDIENT_TYPES.register("item_name", () -> ItemNameIngredient.TYPE);
    INGREDIENT_TYPES.register("nbt_name", () -> NBTNameIngredient.TYPE);
  }

  /** Registers the ingredient types with the given event bus */
  public static void init(IEventBus bus) {
    INGREDIENT_TYPES.register(bus);
  }
}
