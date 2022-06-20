package fun.lewisdev.saverotatingshop.shop;

import fun.lewisdev.saverotatingshop.SaveRotatingShopPlugin;
import fun.lewisdev.saverotatingshop.config.ConfigHandler;
import fun.lewisdev.saverotatingshop.shop.menu.SellGui;
import fun.lewisdev.saverotatingshop.shop.menu.ShopGui;
import fun.lewisdev.saverotatingshop.util.ItemStackBuilder;
import fun.lewisdev.saverotatingshop.util.universal.XMaterial;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ShopManager {

    private final SaveRotatingShopPlugin plugin;
    private ConfigHandler dataFile;

    private BuyShop shop;
    private ShopGui shopGui;

    private SellGui sellGui;
    private Map<Material, Double> sellItems;

    private ItemStack notEnoughCoinsItem;
    private ItemStack purchaseSuccessItem;

    public ShopManager(SaveRotatingShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        final FileConfiguration config = plugin.getConfig();

        dataFile = new ConfigHandler(plugin, "shopdata");
        dataFile.saveDefaultConfig();

        shopGui = new ShopGui(this);

        notEnoughCoinsItem = ItemStackBuilder.getItemStack(config.getConfigurationSection("gui.not_enough_coins_item")).build();
        purchaseSuccessItem = ItemStackBuilder.getItemStack(config.getConfigurationSection("gui.purchase_success")).build();

        ConfigHandler sellShopConfigHandler = new ConfigHandler(plugin, "shops/sell_shop.yml");
        sellShopConfigHandler.saveDefaultConfig();
        FileConfiguration sellConfig = sellShopConfigHandler.getConfig();
        sellGui = new SellGui(this, sellConfig);
        sellItems = new HashMap<>();
        for(String key : sellConfig.getConfigurationSection("sell_items").getKeys(false)) {
            ConfigurationSection section = sellConfig.getConfigurationSection("sell_items." + key);
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(section.getString("item.material"));
            xMaterial.ifPresent(material -> sellItems.put(material.parseMaterial(), section.getDouble("sellPrice")));
        }

        ConfigHandler shopConfigHandler = new ConfigHandler(plugin, "shops/rotating_shop.yml");
        shopConfigHandler.saveDefaultConfig();
        FileConfiguration shopConfig = shopConfigHandler.getConfig();
        shop = new BuyShop(shopConfig);

        shop.setShopSize(shopConfig.getInt("shop_items_amount"));
        shop.setRotatingShopTime(shopConfig.getBoolean("rotating_shop.enabled") ? shopConfig.getInt("rotating_shop.hours") : -1);

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

    public void openRotatingShop(Player player) {
        shopGui.open(player, shop);
    }

    public void openSellGui(Player player) {
        sellGui.open(player);
    }

    public SaveRotatingShopPlugin getPlugin() {
        return plugin;
    }

    public ItemStack getNotEnoughCoinsItem() {
        return notEnoughCoinsItem;
    }

    public ItemStack getPurchaseSuccessItem() {
        return purchaseSuccessItem;
    }

    public BuyShop getBuyShop() {
        return shop;
    }

    public Map<Material, Double> getSellItems() {
        return sellItems;
    }
}
