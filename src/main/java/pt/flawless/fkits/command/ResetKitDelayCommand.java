package pt.flawless.fkits.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.model.Kit;

import java.util.List;

public class ResetKitDelayCommand implements CommandExecutor, TabCompleter {
    private final FKits plugin;

    public ResetKitDelayCommand(FKits plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!CommandUtil.requireAdmin(sender)) return true;
        if (args.length != 2) {
            sender.sendMessage("§cUso: /resetkitdelay <jogador> <kit>");
            return true;
        }
        OfflinePlayer target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore()) {
                sender.sendMessage("§cO jogador " + args[0] + " nunca entrou no servidor.");
                return true;
            }
        }
        Kit kit = CommandUtil.kitOrMessage(plugin, sender, args[1]);
        if (kit == null) return true;
        String name = target.getName() != null ? target.getName() : args[0];
        plugin.service().resetCooldown(target.getUniqueId(), kit.getId()).whenComplete((v, err) ->
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(err != null
                        ? "§cOcorreu um erro ao repor o delay."
                        : "§aDelay do kit " + kit.getId() + " reposto para " + name + ".")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandUtil.ADMIN)) return List.of();
        if (args.length == 1) return CommandUtil.onlinePlayers(args[0]);
        return args.length == 2 ? CommandUtil.kitIds(plugin, args[1]) : List.of();
    }
}
