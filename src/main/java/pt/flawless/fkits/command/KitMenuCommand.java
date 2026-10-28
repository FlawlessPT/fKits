package pt.flawless.fkits.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.model.Kit;

import java.util.List;
import java.util.function.BiConsumer;

/** Base dos comandos de admin que atuam num kit a partir de um jogador: {@code /<cmd> <kit>}. */
abstract class KitMenuCommand implements CommandExecutor, TabCompleter {
    private final FKits plugin;
    private final String usage;
    private final BiConsumer<Player, Kit> action;

    KitMenuCommand(FKits plugin, String usage, BiConsumer<Player, Kit> action) {
        this.plugin = plugin;
        this.usage = usage;
        this.action = action;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!CommandUtil.requireAdmin(sender)) return true;
        Player p = CommandUtil.requirePlayer(sender);
        if (p == null) return true;
        if (args.length != 1) {
            p.sendMessage("§cUso: " + usage);
            return true;
        }
        Kit kit = CommandUtil.kitOrMessage(plugin, p, args[0]);
        if (kit != null) action.accept(p, kit);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandUtil.ADMIN)) return List.of();
        return args.length == 1 ? CommandUtil.kitIds(plugin, args[0]) : List.of();
    }
}
