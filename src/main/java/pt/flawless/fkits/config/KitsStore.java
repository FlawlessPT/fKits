package pt.flawless.fkits.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fkits.model.Kit;
import pt.flawless.fkits.model.KitCategory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Lê e grava {@code plugins/fKits/kits.yml}. */
public class KitsStore {
    public static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,16}");

    private final JavaPlugin plugin;
    private final File file;

    public KitsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
    }

    public record Data(Map<String, KitCategory> categories, Map<String, Kit> kits) {}

    public Data load() {
        if (!file.exists()) plugin.saveResource("kits.yml", false);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        Map<String, KitCategory> categories = new LinkedHashMap<>();
        Map<String, Kit> kits = new LinkedHashMap<>();

        ConfigurationSection cats = yml.getConfigurationSection("categorias");
        if (cats != null) {
            for (String id : cats.getKeys(false)) {
                ConfigurationSection s = cats.getConfigurationSection(id);
                if (s == null || !ID.matcher(id).matches()) {
                    warn("categoria ignorada (id inválido): " + id);
                    continue;
                }
                KitCategory cat = new KitCategory(id, s.getString("nome", id), readIcon(s.get("icone")));
                cat.setRows(s.getInt("linhas", 4));
                categories.put(id, cat);
            }
        }
        ConfigurationSection ks = yml.getConfigurationSection("kits");
        if (ks != null) {
            for (String id : ks.getKeys(false)) {
                ConfigurationSection s = ks.getConfigurationSection(id);
                if (s == null || !ID.matcher(id).matches()) {
                    warn("kit ignorado (id inválido): " + id);
                    continue;
                }
                String category = s.getString("categoria");
                if (category == null || !categories.containsKey(category)) {
                    warn("kit ignorado (categoria inexistente): " + id);
                    continue;
                }
                Kit kit = new Kit(id, category, Math.max(0, s.getLong("delay", 0)), readIcon(s.get("icone")));
                List<ItemStack> items = new ArrayList<>();
                for (Object o : s.getList("itens", List.of())) {
                    items.add(o instanceof ItemStack item ? item : null);
                }
                kit.setPosition(s.getInt("coluna", 0), s.getInt("linha", 0));
                kit.setItems(items);
                kits.put(id, kit);
            }
        }
        return new Data(categories, kits);
    }

    public void save(Map<String, KitCategory> categories, Map<String, Kit> kits) throws IOException {
        YamlConfiguration yml = new YamlConfiguration();
        yml.createSection("categorias");
        yml.createSection("kits");
        for (KitCategory c : categories.values()) {
            String p = "categorias." + c.getId();
            yml.set(p + ".nome", c.getName());
            yml.set(p + ".icone", writeIcon(c.getIcon()));
            yml.set(p + ".linhas", c.getRows());
        }
        for (Kit k : kits.values()) {
            String p = "kits." + k.getId();
            yml.set(p + ".categoria", k.getCategory());
            yml.set(p + ".delay", k.getDelaySeconds());
            yml.set(p + ".coluna", k.getColumn());
            yml.set(p + ".linha", k.getRow());
            yml.set(p + ".icone", writeIcon(k.getIcon()));
            yml.set(p + ".itens", k.getItems());
        }
        plugin.getDataFolder().mkdirs();
        yml.save(file);
    }

    private ItemStack readIcon(Object raw) {
        if (raw instanceof ItemStack item) return item;
        if (raw instanceof String name) {
            Material m = Material.matchMaterial(name);
            if (m != null && m.isItem()) return new ItemStack(m);
            warn("ícone desconhecido: " + name);
        }
        return new ItemStack(Material.CHEST);
    }

    private Object writeIcon(ItemStack icon) {
        return icon.hasItemMeta() ? icon : icon.getType().name();
    }

    private void warn(String msg) {
        plugin.getLogger().warning(msg);
    }
}
