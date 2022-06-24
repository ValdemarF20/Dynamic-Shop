package fun.lewisdev.savedynamicshop.shop;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import fun.lewisdev.savedynamicshop.SaveDynamicShop;
import fun.lewisdev.savedynamicshop.config.ConfigHandler;
import fun.lewisdev.savedynamicshop.shop.menu.BulkPurchaseGui;
import fun.lewisdev.savedynamicshop.shop.menu.ShopGui;
import fun.lewisdev.savedynamicshop.util.GuiUtils;
import fun.lewisdev.savedynamicshop.util.ItemStackBuilder;
import fun.lewisdev.savedynamicshop.util.universal.XSound;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ShopManager {

    private final SaveDynamicShop plugin;
    private ConfigHandler dataFile;
    private final Economy economy;
    private final BulkPurchaseGui bulkPurchaseGui;

    private Shop shop;
    private ShopGui shopGui;

    private Map<Material, Double> sellItems;

    private ItemStack notEnoughCoinsItem;
    private ItemStack purchaseSuccessItem;

    public ShopManager(SaveDynamicShop plugin) {
        this.plugin = plugin;
        this.economy = this.getPlugin().getEconomy();
        this.bulkPurchaseGui = new BulkPurchaseGui(this);
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

    /**
     * This method handles what should happen when a player clicks on a purchasable item in the shop
     * @param player Player who is in the GUI
     * @param reward Reward item that is being purchased
     * @param guiItem The reward item as GuiItem
     * @param shop Shop that is being used (currently only one type shop)
     * @param gui Gui that is open where the item is purchased
     * @param config Config file used to find filler-items
     */
    public void handleInventoryAction(Player player, ShopReward reward, GuiItem guiItem, Shop shop, Gui gui, ConfigurationSection config) {
        guiItem.setAction(event -> {
            if (reward.getCost() < 0) return;
            int amount = guiItem.getItemStack().getAmount();

            // Q is pressed / Enter bulk buy GUI and return
            if ((event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) && reward.isAllowBulkBuy()) {
                bulkPurchaseGui.open(player, shop, reward);
                return;
            }

            // Check if the player can afford the item
            if (economy.getBalance(player) >= reward.getCost()) {
                economy.withdrawPlayer(player, reward.getCost());

                // Run all commands attached to the item / reward
                for (String command : reward.getCommands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{PLAYER}", player.getName()).replace("{AMOUNT}", String.valueOf(amount)));
                }

                //TODO: Why are the filler items updated again?
                GuiUtils.setFillerItems(gui, config.getConfigurationSection("gui.filler_items"), this, player, shop);
                gui.update();

                // Get the previous item to reset the item after confirmation item
                ItemStack previousItem = event.getCurrentItem();
                // Play a success sound
                player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 6L);
                // Sets the item that appears in a short amount of time after purchasing (confirmation item)
                event.getClickedInventory().setItem(event.getSlot(), this.getPurchaseSuccessItem());
                // Sets the previous item (item that has been bought) again, to remove confirmation item
                Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
                    event.getClickedInventory().setItem(event.getSlot(), previousItem);
                }, 20L);

            } else { // Will run if the player cannot afford the item
                ItemStack previousItem = event.getCurrentItem();
                player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 0L);
                event.getClickedInventory().setItem(event.getSlot(), this.getNotEnoughCoinsItem());
                Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> event.getClickedInventory().setItem(event.getSlot(), previousItem), 45L);
            }
        });
    }
}
