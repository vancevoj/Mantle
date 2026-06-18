package slimeknights.mantle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command to dump the harvest tier ordering.
 * <p>
 * TODO(neoport): NeoForge 1.21 removed {@code TierSortingRegistry} and the {@code forge:item_tier_ordering.json} concept
 * entirely (tool tiers no longer have a global ordering or block tag mapping). This command therefore has no functional
 * equivalent and now only reports that the feature is unavailable. Revisit if Mantle reintroduces a tier ordering system.
 */
public class HarvestTiersCommand {
  /** Message shown since tier ordering no longer exists in 1.21 */
  private static final Component UNSUPPORTED = Component.translatable("command.mantle.harvest_tiers.unsupported");

  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(sender -> sender.hasPermission(MantleCommand.PERMISSION_EDIT_SPAWN))
              .then(Commands.literal("save").executes(HarvestTiersCommand::unsupported))
              .then(Commands.literal("log").executes(HarvestTiersCommand::unsupported))
              .then(Commands.literal("list").executes(HarvestTiersCommand::unsupported));
  }

  /** Reports that tier ordering is no longer supported */
  private static int unsupported(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    context.getSource().sendSuccess(() -> UNSUPPORTED, true);
    return 0;
  }
}
