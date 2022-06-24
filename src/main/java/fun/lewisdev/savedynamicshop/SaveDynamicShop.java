package fun.lewisdev.savedynamicshop;

import fun.lewisdev.savedynamicshop.command.ShopCommand;
import fun.lewisdev.savedynamicshop.config.Messages;
import fun.lewisdev.savedynamicshop.shop.ShopManager;
import fun.lewisdev.savedynamicshop.task.TimerTask;
import me.mattstudios.mf.base.CommandManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public final class SaveDynamicShop extends JavaPlugin {

    private ShopManager shopManager;
    private BukkitTask timerTask;
    private Economy economy;
    private Logger LOGGER;

    @Override
    public void onEnable() {
        LOGGER = this.getLogger();
        LOGGER.info("SaveDynamicShop has been started");
        if (!setupEconomy()) {
            LOGGER.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        Messages.setConfiguration(getConfig());

        (shopManager = new ShopManager(this)).onEnable();
        loadTimerTask();

        CommandManager commandManager = new CommandManager(this, true);
        commandManager.getMessageHandler().register("cmd.no.permission", Messages.NO_PERMISSION::send);
        commandManager.register(new ShopCommand(this));
    }

    public void onReload() {
        shopManager.onDisable();

        reloadConfig();
        Messages.setConfiguration(getConfig());

        shopManager.onEnable();
        loadTimerTask();
    }

    @Override
    public void onDisable() {
        if(shopManager != null) shopManager.onDisable();
    }

    private void loadTimerTask() {
        if(timerTask != null) timerTask.cancel();

        if(shopManager.getBuyShop().isRotatingShop()) {
            timerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new TimerTask(this), 20L, 20L);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public Economy getEconomy() {
        return economy;
    }
}
