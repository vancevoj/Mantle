package slimeknights.mantle.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferPacket;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
import slimeknights.mantle.network.packet.OpenLecternBookPacket;
import slimeknights.mantle.network.packet.OpenNamedBookPacket;
import slimeknights.mantle.network.packet.SwingArmPacket;
import slimeknights.mantle.network.packet.UpdateHeldPagePacket;
import slimeknights.mantle.network.packet.UpdateInventoryPagePacket;
import slimeknights.mantle.network.packet.UpdateLecternPagePacket;

public class MantleNetwork {
  /**
   * Network instance
   * 1: 1.11.101 and before
   * 2: 1.11.102 - New predicate types, enum loadable nullable field optimization
   */
  public static final NetworkWrapper INSTANCE = new NetworkWrapper(Mantle.getResource("network"), "2");

  /**
   * Registers the payload handler listener on the mod event bus. Call from the mod constructor.
   * @param modBus  Mod event bus
   */
  public static void init(IEventBus modBus) {
    modBus.addListener(RegisterPayloadHandlersEvent.class, MantleNetwork::registerPackets);
  }

  /**
   * Registers packets into this network. Invoked from {@link RegisterPayloadHandlersEvent} on the mod bus.
   * @param event  Payload registration event
   */
  public static void registerPackets(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = INSTANCE.getRegistrar(event);
    INSTANCE.registerToClient(registrar, OpenLecternBookPacket.TYPE, OpenLecternBookPacket::new);
    INSTANCE.registerToServer(registrar, UpdateHeldPagePacket.TYPE, UpdateHeldPagePacket::new);
    INSTANCE.registerToServer(registrar, UpdateInventoryPagePacket.TYPE, UpdateInventoryPagePacket::new);
    INSTANCE.registerToServer(registrar, UpdateLecternPagePacket.TYPE, UpdateLecternPagePacket::new);
    INSTANCE.registerToServer(registrar, DropLecternBookPacket.TYPE, DropLecternBookPacket::new);
    INSTANCE.registerToClient(registrar, SwingArmPacket.TYPE, SwingArmPacket::new);
    INSTANCE.registerToClient(registrar, OpenNamedBookPacket.TYPE, OpenNamedBookPacket::new);
    INSTANCE.registerToClient(registrar, FluidContainerTransferPacket.TYPE, FluidContainerTransferPacket::new);
  }
}
