package pt.flawless.fkits.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pt.flawless.fkits.FKits;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/** Pede um valor por chat e apanha a resposta antes de qualquer outro listener de chat. */
public final class ChatPrompt implements Listener {
    private record Pending(BiPredicate<Player, String> handler, Consumer<Player> onCancel) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private final FKits plugin;

    private ChatPrompt(FKits plugin) {
        this.plugin = plugin;
    }

    public static void register(FKits p) {
        Bukkit.getPluginManager().registerEvents(new ChatPrompt(p), p);
    }

    /**
     * Thread principal. Fecha o menu e mostra as {@code lines}. O {@code handler} corre na thread principal
     * com a resposta: devolve true se terminou (e trata de reabrir o menu) ou false, depois de avisar do
     * erro, para continuar à espera. "cancelar" chama o {@code onCancel}.
     */
    public static void start(Player player, String[] lines, BiPredicate<Player, String> handler, Consumer<Player> onCancel) {
        PENDING.put(player.getUniqueId(), new Pending(handler, onCancel));
        player.closeInventory();
        for (String line : lines) player.sendMessage(line);
        player.sendMessage("§7Escreve §fcancelar §7para sair.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        Pending pending = PENDING.remove(id);
        if (pending == null) return;
        e.setCancelled(true);
        String msg = e.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(id);
            if (p == null) return;
            if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
                p.sendMessage("§cCancelado.");
                pending.onCancel().accept(p);
                return;
            }
            if (!pending.handler().test(p, msg)) PENDING.put(id, pending);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PENDING.remove(e.getPlayer().getUniqueId());
    }
}
