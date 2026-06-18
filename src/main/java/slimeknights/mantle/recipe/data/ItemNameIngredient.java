package slimeknights.mantle.recipe.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ingredient for a non-NBT sensitive item from another mod, should never be used outside datagen.
 * <p>
 * Behavior change for the NeoForge 1.21 port: instead of producing raw JSON via {@code toJson()}, this is now a
 * {@link ICustomIngredient} and serializes via {@link #TYPE}'s {@link MapCodec}. It only needs to encode (the items
 * may not exist at datagen time), so decoding is unsupported.
 */
public class ItemNameIngredient implements ICustomIngredient {
  /** Ingredient type. Encode-only since the referenced items may not be loaded during datagen. Registered in MantleIngredients. */
  public static final IngredientType<ItemNameIngredient> TYPE = new IngredientType<>(
    RecordCodecBuilder.mapCodec(inst -> inst.group(
      ResourceLocation.CODEC.listOf().fieldOf("items").forGetter(i -> i.names)
    ).apply(inst, ItemNameIngredient::new)));

  private final List<ResourceLocation> names;
  protected ItemNameIngredient(List<ResourceLocation> names) {
    this.names = names;
  }

  /** Creates a new ingredient from a list of names */
  public static ItemNameIngredient from(List<ResourceLocation> names) {
    return new ItemNameIngredient(names);
  }

  /** Creates a new ingredient from a list of names */
  public static ItemNameIngredient from(ResourceLocation... names) {
    return from(Arrays.asList(names));
  }

  @Override
  public boolean test(ItemStack stack) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stream<ItemStack> getItems() {
    // datagen-only; return the items that happen to resolve so the ingredient is not considered empty
    return names.stream()
                .map(BuiltInRegistries.ITEM::get)
                .filter(item -> item != Items.AIR)
                .map(ItemStack::new);
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ItemNameIngredient other && names.equals(other.names);
  }

  @Override
  public int hashCode() {
    return names.hashCode();
  }
}
