package net.valdemarf.savedynamicshop.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * <b>ConfigHandler</b> - Easy Configuration Management
 *
 * @author ItsLewizzz
 * @version 1.0.0
 */
public class ConfigHandler {

    private final JavaPlugin plugin;
    private final String name;
    private final File file;
    private FileConfiguration configuration;

    public ConfigHandler(final JavaPlugin plugin, final File path, final String name) {
        this.plugin = plugin;
        this.name = name.endsWith(".yml") ? name : name + ".yml";
        this.file = new File(path, this.name);
        this.configuration = new YamlConfiguration();
    }

    public ConfigHandler(final JavaPlugin plugin, final String name) {
        this(plugin, plugin.getDataFolder(), name);
    }

    public void saveDefaultConfig() {
        if (!this.file.exists()) {
            int length = this.file.toPath().getNameCount();
            this.plugin.saveResource(this.file.getParentFile().getName().equals(this.plugin.getName()) ? this.name : this.file.toPath().subpath(length - 2, length).toFile().getPath(), false);
        }

        try {
            this.configuration.load(this.file);
        } catch (IOException | InvalidConfigurationException ex) {
            ex.printStackTrace();
            this.plugin.getLogger().severe("============= CONFIGURATION ERROR =============");
            this.plugin.getLogger().severe("There was an error loading " + this.name);
            this.plugin.getLogger().severe("Please check for any obvious configuration mistakes");
            this.plugin.getLogger().severe("such as using tabs for spaces or forgetting to end quotes");
            this.plugin.getLogger().severe("before reporting to the developer. The plugin will now disable..");
            this.plugin.getLogger().severe("============= CONFIGURATION ERROR =============");
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
        }
    }

    public void save() {
        if (this.configuration != null && this.file != null) {
            try {
                this.getConfig().save(this.file);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
    }

    public FileConfiguration getConfig() {
        return this.configuration;
    }

    public File getFile() {
        return this.file;
    }
}
