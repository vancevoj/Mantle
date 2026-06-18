package slimeknights.mantle.data;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.neoforge.common.loot.LootModifierManager;
import slimeknights.mantle.data.JsonCodec.GsonCodec;

/** This class contains codecs for various vanilla things that we need to use in codecs. */
public class MantleCodecs {
  /** Codec for loot pool entries */
  public static final Codec<LootPoolEntryContainer> LOOT_ENTRY = new GsonCodec<>("loot entry", LootModifierManager.GSON_INSTANCE, LootPoolEntryContainer.class);
  /** Codec for loot pool entries */
  public static final Codec<LootItemFunction[]> LOOT_FUNCTIONS = new GsonCodec<>("loot functions", LootModifierManager.GSON_INSTANCE, LootItemFunction[].class);
  /** Codec for ingredients, handling NeoForge ingredient types */
  public static final Codec<Ingredient> INGREDIENT = Ingredient.CODEC;
}
