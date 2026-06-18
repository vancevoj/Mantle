package slimeknights.mantle.recipe.ingredient;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

/** Ingredient that shows all potion variants on the displayed item list */
public class PotionDisplayIngredient extends ItemIngredient {
  /** Loadable for parsing and serializing this ingredient */
  public static final RecordLoadable<PotionDisplayIngredient> LOADABLE = RecordLoadable.create(ItemsField.INSTANCE, TAG_FIELD, PotionDisplayIngredient::new);
  /** Ingredient type instance, registered by {@link MantleIngredients} */
  public static final IngredientType<PotionDisplayIngredient> TYPE = new LoadableIngredientSerializer<>(LOADABLE).type();

  protected PotionDisplayIngredient(List<Item> items, @Nullable TagKey<Item> tag) {
    super(items, tag);
  }

  /** Creates a ingredient matching a list of items */
  public static PotionDisplayIngredient of(List<ItemLike> items) {
    return new PotionDisplayIngredient(toItem(items), null);
  }

  /** Creates a ingredient matching a list of items */
  public static PotionDisplayIngredient of(ItemLike... items) {
    return of(List.of(items));
  }

  /** Creates a ingredient matching a tag */
  public static PotionDisplayIngredient of(TagKey<Item> tag) {
    return new PotionDisplayIngredient(List.of(), tag);
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public Stream<ItemStack> getItems() {
    // show every registered potion on each matched item (1.21 has no empty potion to skip)
    List<ItemStack> parentStacks = super.getItems().toList();
    return BuiltInRegistries.POTION.holders()
      .flatMap(potion -> parentStacks.stream().map(item -> {
        ItemStack copy = item.copy();
        copy.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return copy;
      }));
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }
}
