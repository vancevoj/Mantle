package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.command.client.BookCommand;

@AllArgsConstructor
public class OpenNamedBookPacket implements IThreadsafePacket {
  public static final Type<OpenNamedBookPacket> TYPE = new Type<>(Mantle.getResource("open_named_book"));
  public static final StreamCodec<RegistryFriendlyByteBuf,OpenNamedBookPacket> STREAM_CODEC = ISimplePacket.codec(OpenNamedBookPacket::new);

  private final ResourceLocation book;

  public OpenNamedBookPacket(FriendlyByteBuf buffer) {
    this.book = buffer.readResourceLocation();
  }

  @Override
  public void encode(FriendlyByteBuf buf) {
    buf.writeResourceLocation(book);
  }

  @Override
  public Type<OpenNamedBookPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    BookData bookData = BookLoader.getBook(book);
    if(bookData != null) {
      bookData.openGui(Component.literal("Book"), "", null, null);
    } else {
      ClientOnly.errorStatus(book);
    }
  }

  static class ClientOnly {
    static void errorStatus(ResourceLocation book) {
      BookCommand.bookNotFound(book);
    }
  }
}
