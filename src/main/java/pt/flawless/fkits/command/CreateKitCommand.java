package pt.flawless.fkits.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.service.KitService;

import java.util.List;
import java.util.Locale;

public class CreateKitCommand implements CommandExecutor, TabCompleter {
    private final FKits plugin;

    public CreateKitCommand(FKits plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!CommandUtil.requireAdmin(sender)) return true;
        if (args.length < 2 || args.length > 5 || args.length == 4) {
            sender.sendMessage("§cUso: /criarkit <id> <categoria> [delay] [coluna linha]");
            return true;
        }
        KitService service = plugin.service();
        String id = args[0].toLowerCase(Locale.ROOT);
        if (!CommandUtil.validId(sender, id)) return true;
        if (service.kit(id) != null) {
            sender.sendMessage("§cJá existe um kit com o id " + id + ".");
            return true;
        }
        String category = args[1].toLowerCase(Locale.ROOT);
        if (service.category(category) == null) {
            sender.sendMessage("§cA categoria " + args[1] + " não existe.");
            return true;
        }
        int rows = service.category(category).getRows();
        if (service.kitsIn(category).size() >= rows * 9) {
            sender.sendMessage("§cA categoria já tem o máximo de " + rows * 9 + " kits.");
            return true;
        }
        long delay = 0;
        if (args.length == 3) {
            delay = KitService.parseDuration(args[2]);
            if (delay < 0) {
                sender.sendMessage("§cO delay é inválido. Usa segundos ou por exemplo 1d, 12h30m (d, h, m, s), sem espaços.");
                return true;
            }
        }
        int column = 0, row = 0;
        if (args.length == 5) {
            column = parseInt(args[3]);
            row = parseInt(args[4]);
            if (column < 1 || column > 9 || row < 1 || row > rows) {
                sender.sendMessage("§cA coluna vai de 1 a 9 e a linha de 1 a " + rows + " (linhas desta categoria).");
                return true;
            }
            if (service.layout(category).containsKey((row - 1) * 9 + column - 1)) {
                sender.sendMessage("§cEssa posição já está ocupada.");
                return true;
            }
        }
        service.createKit(id, category, delay, column, row);
        sender.sendMessage("§aKit " + id + " criado. Usa /editarkit " + id + " para o configurar.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandUtil.ADMIN)) return List.of();
        return args.length == 2 ? CommandUtil.categoryIds(plugin, args[1]) : List.of();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
