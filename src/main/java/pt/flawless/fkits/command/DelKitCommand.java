package pt.flawless.fkits.command;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pt.flawless.fapi.inventory.ConfirmMenu;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.model.Kit;

import java.util.List;

public class DelKitCommand extends KitMenuCommand {
    public DelKitCommand(FKits plugin) {
        super(plugin, "/delkit <kit>", (p, kit) -> confirm(plugin, p, kit));
    }

    private static void confirm(FKits plugin, Player p, Kit kit) {
        ConfirmMenu.open(p, "Apagar kit " + kit.getId(), new ItemStack(kit.getIcon().getType()),
                new ConfirmMenu.Button(Material.LIME_CONCRETE, "§aApagar",
                        List.of("§7Apaga o kit " + kit.getId(), "", "§7Clica para confirmar"), player -> {
                    if (plugin.service().kit(kit.getId()) == null) return;
                    plugin.service().deleteKit(kit.getId());
                    player.sendMessage("§aKit " + kit.getId() + " apagado.");
                }),
                new ConfirmMenu.Button(Material.RED_CONCRETE, "§cCancelar",
                        List.of("§7Mantém o kit", "", "§7Clica para cancelar"), null));
    }
}
