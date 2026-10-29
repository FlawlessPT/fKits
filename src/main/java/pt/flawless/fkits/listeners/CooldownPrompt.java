package pt.flawless.fkits.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pt.flawless.fapi.sounds.FSound;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.menu.EditKitMenu;
import pt.flawless.fkits.model.Kit;
import pt.flawless.fkits.service.KitService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Pede o cooldown de um kit por chat e apanha a resposta antes de qualquer outro listener de chat. */
public final class CooldownPrompt implements Listener {
    private static final Map<UUID, Kit> PENDING = new ConcurrentHashMap<>();
    private final FKits plugin;

    private CooldownPrompt(FKits plugin) {
        this.plugin = plugin;
    }

    public static void register(FKits p) {
        Bukkit.getPluginManager().registerEvents(new CooldownPrompt(p), p);
    }

    /** Thread principal. */
    public static void start(Player player, Kit kit) {
        PENDING.put(player.getUniqueId(), kit);
        player.closeInventory();
        player.sendMessage("§eEscreve no chat o cooldown do kit §f" + kit.getId() + "§e (atual: §f" + EditKitMenu.delayText(kit) + "§e).");
        player.sendMessage("§7Exemplos: §f1d§7, §f12h§7, §f1d 12h 30m§7, §f90s§7, §f0 §7(sem delay). Escreve §fcancelar §7para sair.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Kit kit = PENDING.remove(e.getPlayer().getUniqueId());
        if (kit == null) return;
        e.setCancelled(true);
        UUID id = e.getPlayer().getUniqueId();
        String msg = e.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(id);
            if (p == null) return;
            if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
                p.sendMessage("§cCancelado.");
                EditKitMenu.open(p, kit);
                return;
            }
            long seconds = KitService.parseDuration(msg);
            if (seconds < 0) {
                p.sendMessage("§cCooldown inválido. Usa por exemplo §f1d 12h 30m §c(d, h, m, s), ou §f0 §cpara não ter delay.");
                FSound.fail(p);
                PENDING.put(id, kit);
                return;
            }
            kit.setDelaySeconds(seconds);
            plugin.service().save();
            p.sendMessage("§aCooldown do kit §f" + kit.getId() + " §adefinido para §f" + EditKitMenu.delayText(kit) + "§a.");
            FSound.success(p);
            EditKitMenu.open(p, kit);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PENDING.remove(e.getPlayer().getUniqueId());
    }
}
