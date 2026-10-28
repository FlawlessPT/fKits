package pt.flawless.fkits.command;

import pt.flawless.fkits.FKits;
import pt.flawless.fkits.menu.EditKitMenu;

public class EditKitCommand extends KitMenuCommand {
    public EditKitCommand(FKits plugin) {
        super(plugin, "/editarkit <kit>", EditKitMenu::open);
    }
}
