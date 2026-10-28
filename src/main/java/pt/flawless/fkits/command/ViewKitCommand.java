package pt.flawless.fkits.command;

import pt.flawless.fkits.FKits;
import pt.flawless.fkits.menu.KitItemsMenu;

public class ViewKitCommand extends KitMenuCommand {
    public ViewKitCommand(FKits plugin) {
        super(plugin, "/verkit <kit>", KitItemsMenu::openView);
    }
}
