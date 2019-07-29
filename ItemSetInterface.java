package plugin.interaction.inter;

import java.util.Arrays;

import org.global.cache.def.impl.ItemDefinition;
import org.global.game.component.CloseEvent;
import org.global.game.component.Component;
import org.global.game.component.ComponentPlugin;
import org.global.game.component.ContextMenu;
import org.global.game.content.global.ItemBoxSet;
import org.global.game.node.entity.player.Player;
import org.global.game.node.item.Item;
import org.global.net.packet.PacketRepository;
import org.global.net.packet.context.AccessMaskContext;
import org.global.net.packet.out.AccessMask;
import org.global.plugin.Plugin;

/**
 * Item sets interface - handles packaging and disassembly.
 * @author Andrew Gomes
 * @date 10/27/2019
 */
public class ItemSetInterface extends ComponentPlugin {
	
	/**
	 * Main component id.
	 */
	private static final int MAIN = 451;
	
	/**
	 * The overlay to use for the inventory
	 */
	private static final int INVENTORY_OVERLAY = 115;

	@Override
	public Plugin<Object> newInstance(Object arg) throws Throwable {
		register(MAIN);
		return this;
	}
	
	@Override
	public void open(Player player, Component component) {
		if (component.getId() == MAIN) {
			register(INVENTORY_OVERLAY);
			component.setCloseEvent(new CloseEvent() {
				@Override
				public boolean close(Player player, Component c) {
					player.getInterfaceManager().closeSingleTab();
					return true;
				}		
			});
			player.getInterfaceManager().openSingleTab(new Component(INVENTORY_OVERLAY));
			PacketRepository.send(AccessMask.class, new AccessMaskContext(player, 1086, 0, INVENTORY_OVERLAY, 0, 27));
			player.getPacketDispatch().sendRunScript(149, "I", new Object[] { "Unpack<col=ff9040>", "", "", "", "", -1, 0, 7, 4, 93, 7536640 });
		}	
	}

	@Override
	public boolean handle(Player player, Component component, ContextMenu option, int button, int slot, int itemId) {
		ItemBoxSet boxSet = ItemBoxSet.forId(itemId);
		if (boxSet == null) {
			player.sendMessage("This is not a box set.");
			return false;
		}
		if (option == ContextMenu.OPTION_EXAMINE) {
			player.sendMessage("This is a box set used to save storage space.");
			return true;
		}
		switch (component.getId()) {
		case MAIN:
			if (option == ContextMenu.OPTION_1) {
				int[] boxSetItems = boxSet.getComponents();
				if (!player.getInventory().containItems(boxSetItems)) {
					// If we don't have the full set of unnoted components, 
					// attempt to use noted instead.
					boxSetItems = boxSet.getNotedComponents();
				}
				if (!player.getInventory().containItems(boxSetItems)) {
					StringBuilder components = new StringBuilder();
					Arrays.stream(boxSetItems).forEach(x -> components.append(ItemDefinition.forId(x).getName() + ", "));
					player.sendMessage("This box set is made up of the components: " + components.toString().substring(0, components.length() - 2) + ".");
				} else {
					if (player.getInventory().remove(boxSetItems)) {
						player.getInventory().add(new Item(boxSet.getItemId()));
						player.sendMessage("You exchange your component items for the box set.");
					}
				}
				return true;
			}
		case INVENTORY_OVERLAY:
			int componentSpace = boxSet.getComponents().length - 1;
			if (player.getInventory().freeSlots() < componentSpace) {
				player.sendMessage("You can't exchange that box set, you don't have enough inventory space.");
				return true;
			}
			Item[] components = Arrays.stream(boxSet.getComponents()).mapToObj(x -> new Item(x)).toArray(Item[]::new);
			if (player.getInventory().remove(new Item(itemId))) {
				player.getInventory().add(components);
				player.sendMessage("You've exchanged your box set for the component pieces.");
			}
			return true;
		}
		return false;
	}
}