package slimeknights.mantle.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Simple ingredient checking for an item with a specific potion */
public class PotionIngredient extends ItemIngredient {
  /** Loadable for parsing and serializing this ingredient */
  public static final RecordLoadable<PotionIngredient> LOADABLE = RecordLoadable.create(
    ItemsField.INSTANCE, TAG_FIELD,
    Loadables.POTION.defaultField("potion", Potions.WATER.value(), false, i -> i.potion.value()),
    (items, tag, potion) -> new PotionIngredient(items, tag, BuiltInRegistries.POTION.wrapAsHolder(potion)));
  /** Ingredient type instance, registered by {@link MantleIngredients} */
  public static final IngredientType<PotionIngredient> TYPE = new LoadableIngredientSerializer<>(LOADABLE).type();

  private final Holder<Potion> potion;
  protected PotionIngredient(List<Item> items, @Nullable TagKey<Item> itemTag, Holder<Potion> potion) {
    super(items, itemTag);
    this.potion = potion;
  }

  /** Creates a potion ingredient matching a list of items */
  public static PotionIngredient of(Holder<Potion> potion, List<ItemLike> items) {
    return new PotionIngredient(toItem(items), null, potion);
  }

  /** Creates a potion ingredient matching a list of items */
  public static PotionIngredient of(Holder<Potion> potion, ItemLike... items) {
    return of(potion, Arrays.asList(items));
  }

  /** Creates a potion ingredient matching a tag */
  public static PotionIngredient of(Holder<Potion> potion, TagKey<Item> tag) {
    return new PotionIngredient(List.of(), tag, potion);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || !super.test(stack)) {
      return false;
    }
    // stack must match, any item must match, and potion must match
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    Optional<Holder<Potion>> stackPotion = contents == null ? Optional.empty() : contents.potion();
    return stackPotion.isPresent() && stackPotion.get().equals(potion);
  }

  @Override
  public Stream<ItemStack> getItems() {
    // set the potion on each matched item
    return super.getItems().map(stack -> {
      stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
      return stack;
    });
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }
}
