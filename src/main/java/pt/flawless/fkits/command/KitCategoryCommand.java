package pt.flawless.fkits.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.menu.CategoriesAdminMenu;

import java.util.List;

public class KitCategoryCommand implements CommandExecutor, TabCompleter {
    public KitCategoryCommand(FKits plugin) {}

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!CommandUtil.requireAdmin(sender)) return true;
        Player p = CommandUtil.requirePlayer(sender);
        if (p != null) CategoriesAdminMenu.open(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
