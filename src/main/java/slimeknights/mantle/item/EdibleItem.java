package slimeknights.mantle.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.PossibleEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.util.TranslationHelper;

import java.util.List;

public class EdibleItem extends Item {
  public EdibleItem(FoodProperties foodIn) {
    this(new Properties().food(foodIn));
  }

  public EdibleItem(Item.Properties properties) {
    super(properties);
    if (!this.components().has(DataComponents.FOOD)) {
      throw new IllegalArgumentException("Must set food to make an EdibleItem");
    }
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
    TranslationHelper.addOptionalTooltip(stack, tooltip);
    // TODO: use ContainerFoodItem helper for more potion like effects?
    FoodProperties food = stack.get(DataComponents.FOOD);
    if (food != null) {
      for (PossibleEffect possible : food.effects()) {
        tooltip.add(Component.literal(I18n.get(possible.effect().getDescriptionId()).trim()).withStyle(ChatFormatting.GRAY));
      }
    }
  }
}
