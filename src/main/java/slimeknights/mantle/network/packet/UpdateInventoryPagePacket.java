package slimeknights.mantle.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookHelper;

/** Packet to update the page in a book in the players inventory */
public record UpdateInventoryPagePacket(int slot, String page) implements IThreadsafePacket {
  public static final Type<UpdateInventoryPagePacket> TYPE = new Type<>(Mantle.getResource("update_inventory_page"));
  public static final StreamCodec<RegistryFriendlyByteBuf,UpdateInventoryPagePacket> STREAM_CODEC = ISimplePacket.codec(UpdateInventoryPagePacket::new);

  public UpdateInventoryPagePacket(FriendlyByteBuf buffer) {
    this(buffer.readVarInt(), buffer.readUtf(100));
  }

  @Override
  public void encode(FriendlyByteBuf buf) {
    buf.writeVarInt(slot);
    buf.writeUtf(page);
  }

  @Override
  public Type<UpdateInventoryPagePacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    Player player = context.player();
    if (player != null && this.page != null && slot >= 0) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (!stack.isEmpty()) {
        BookHelper.writeSavedPageToBook(stack, this.page);
      }
    }
  }
}
