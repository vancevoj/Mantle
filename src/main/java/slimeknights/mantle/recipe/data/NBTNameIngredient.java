package slimeknights.mantle.recipe.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Ingredient for a NBT sensitive item from another mod, should never be used outside datagen.
 * <p>
 * Behavior change for the NeoForge 1.21 port: NeoForge replaced {@code StrictNBTIngredient} with
 * {@code DataComponentIngredient}. Since this is datagen-only it keeps a legacy {@link CompoundTag} and a
 * {@link MapCodec} that encodes {item, nbt}. Downstream datapacks may need the data-component format.
 */
public class NBTNameIngredient implements ICustomIngredient {
  /** Ingredient type. Encode-only since the referenced items may not be loaded during datagen. Registered in MantleIngredients. */
  // TODO(neoport): NBTNameIngredient uses legacy NBT; downstream datapacks may need component format
  public static final IngredientType<NBTNameIngredient> TYPE = new IngredientType<>(
    RecordCodecBuilder.mapCodec(inst -> inst.group(
      ResourceLocation.CODEC.fieldOf("item").forGetter(i -> i.name),
      CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(i -> Optional.ofNullable(i.nbt))
    ).apply(inst, (name, nbt) -> new NBTNameIngredient(name, nbt.orElse(null)))));

  private final ResourceLocation name;
  @Nullable
  private final CompoundTag nbt;

  protected NBTNameIngredient(ResourceLocation name, @Nullable CompoundTag nbt) {
    this.name = name;
    this.nbt = nbt;
  }

  /**
   * Creates an ingredient for the given name and NBT
   * @param name  Item name
   * @param nbt   NBT
   * @return  Ingredient
   */
  public static NBTNameIngredient from(ResourceLocation name, CompoundTag nbt) {
    return new NBTNameIngredient(name, nbt);
  }

  /**
   * Creates an ingredient for an item that must have no NBT
   * @param name  Item name
   * @return  Ingredient
   */
  public static NBTNameIngredient from(ResourceLocation name) {
    return new NBTNameIngredient(name, null);
  }

  @Override
  public boolean test(ItemStack stack) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stream<ItemStack> getItems() {
    // datagen-only; return the item if it happens to resolve so the ingredient is not considered empty
    return Stream.of(BuiltInRegistries.ITEM.get(name))
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
    if (!(obj instanceof NBTNameIngredient other)) {
      return false;
    }
    return name.equals(other.name) && java.util.Objects.equals(nbt, other.nbt);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(name, nbt);
  }
}
