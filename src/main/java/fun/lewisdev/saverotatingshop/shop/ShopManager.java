package fun.lewisdev.saverotatingshop.shop;

import fun.lewisdev.saverotatingshop.SaveDynamicShop;
import fun.lewisdev.saverotatingshop.config.ConfigHandler;
import fun.lewisdev.saverotatingshop.shop.menu.ShopGui;
import fun.lewisdev.saverotatingshop.util.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class ShopManager {

    private final SaveDynamicShop plugin;
    private ConfigHandler dataFile;

    private Shop shop;
    private ShopGui shopGui;

    private Map<Material, Double> sellItems;

    private ItemStack notEnoughCoinsItem;
    private ItemStack purchaseSuccessItem;

    public ShopManager(SaveDynamicShop plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        final FileConfiguration config = plugin.getConfig();

        //TODO: shopdata.yml usage?
        dataFile = new ConfigHandler(plugin, "shopdata");
        dataFile.saveDefaultConfig();

        shopGui = new ShopGui(this);

        // This item is shown shortly after a player cannot afford an item
        notEnoughCoinsItem = ItemStackBuilder.getItemStack(config.getConfigurationSection("gui.not_enough_coins_item")).build();
        // This item is shown shortly after a player has purchased an item
        purchaseSuccessItem = ItemStackBuilder.getItemStack(config.getConfigurationSection("gui.purchase_success")).build();

        // Creates a handler for the config file
        ConfigHandler shopConfigHandler = new ConfigHandler(plugin, "shops/dynamic_shop.yml");
        shopConfigHandler.saveDefaultConfig();
        // Gets the FileConfiguration from the handler
        FileConfiguration shopConfig = shopConfigHandler.getConfig();
        // Initializes the GUI
        shop = new Shop(shopConfig);

        shop.setShopSize(shopConfig.getInt("shop_items_amount"));
        shop.setRotatingShopTime(shopConfig.getBoolean("dynamic_shop.enabled") ? shopConfig.getInt("dynamic_shop.hours") : -1);

        // Loop through all items in the "shop_items" list in "dynamic_shop.yml" file
        for(String key : shopConfig.getConfigurationSection("shop_items").getKeys(false)) {
            ConfigurationSection section = shopConfig.getConfigurationSection("shop_items." + key);

            int cost = section.getInt("cost");
            List<String> commands = section.getStringList("commands");
            boolean bulkBuy = section.getBoolean("bulk_buy", true);

            ItemStackBuilder builder = ItemStackBuilder.getItemStack(section);
            shop.addReward(key, new ShopReward(key, builder.build(), commands, cost, bulkBuy));
        }

        final FileConfiguration dataConfig = dataFile.getConfig();
        if (dataConfig.contains("active_rewards")) {
            List<String> rewards = dataConfig.getStringList("active_rewards");
            if (!rewards.isEmpty()) {
                shop.setActiveRewards(rewards);
            }
        }

        if(dataConfig.contains("refresh_time")) {
            shop.setRefreshTime(dataConfig.getLong("refresh_time"));
        }

        if (shop.getActiveRewards().isEmpty() || shop.getActiveRewards().size() < shop.getShopSize()) {
            shop.resetShopItems();
        }
    }

    public void onDisable() {
        final FileConfiguration dataConfig = dataFile.getConfig();
        dataConfig.set("active_rewards", shop.getActiveRewards());
        dataConfig.set("refresh_time", shop.getRefreshTime());
        dataFile.save();
    }

    public void openDynamicShop(Player player) {
        shopGui.open(player, shop);
    }

    public SaveDynamicShop getPlugin() {
        return plugin;
    }

    public ItemStack getNotEnoughCoinsItem() {
        return notEnoughCoinsItem;
    }

    public ItemStack getPurchaseSuccessItem() {
        return purchaseSuccessItem;
    }

    public Shop getBuyShop() {
        return shop;
    }

    public Map<Material, Double> getSellItems() {
        return sellItems;
    }
}
