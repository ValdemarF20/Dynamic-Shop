package fun.lewisdev.savedynamicshop.shop;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import fun.lewisdev.savedynamicshop.SaveDynamicShop;
import fun.lewisdev.savedynamicshop.config.ConfigHandler;
import fun.lewisdev.savedynamicshop.shop.menu.BulkPurchaseGui;
import fun.lewisdev.savedynamicshop.shop.menu.ShopGui;
import fun.lewisdev.savedynamicshop.util.GuiUtils;
import fun.lewisdev.savedynamicshop.util.ItemStackBuilder;
import fun.lewisdev.savedynamicshop.util.TextUtil;
import fun.lewisdev.savedynamicshop.util.universal.XSound;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ShopManager {

    private final SaveDynamicShop plugin;
    private ConfigHandler dataFile;
    private final Economy economy;
    private final BulkPurchaseGui bulkPurchaseGui;
    private final Logger logger;

    private Shop shop;
    private ShopGui shopGui;
    private FileConfiguration shopConfig;

    private Map<Material, Double> sellItems;

    private ItemStack notEnoughCoinsItem;
    private ItemStack purchaseSuccessItem;

    public ShopManager(SaveDynamicShop plugin) {
        this.plugin = plugin;
        this.economy = this.getPlugin().getEconomy();
        this.bulkPurchaseGui = new BulkPurchaseGui(this);
        this.logger = plugin.getLogger();
    }

    public void onEnable() {
        final FileConfiguration config = plugin.getConfig();

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
        shopConfig = shopConfigHandler.getConfig();
        // Initializes the GUI
        shop = new Shop(shopConfig);

        shop.setShopSize(shopConfig.getInt("shop_items_amount"));
        shop.setRotatingShopTime(shopConfig.getBoolean("dynamic_shop.enabled") ? shopConfig.getInt("dynamic_shop.hours") : -1);

        // Loop through all items in the "shop_items" list in "dynamic_shop.yml" file
        for(String key : shopConfig.getConfigurationSection("shop_items").getKeys(false)) {
            ConfigurationSection section = shopConfig.getConfigurationSection("shop_items." + key);

            int cost = section.getInt("cost");
            int sellPrice = section.getInt("sell_price");
            long minPrice = section.getInt("sell_min");
            long maxPrice = section.getInt("sell_max");
            double ratio = section.getDouble("multiplier");
            List<String> commands = section.getStringList("commands");
            boolean bulkBuy = section.getBoolean("bulk_buy", true);

            ItemStackBuilder builder = ItemStackBuilder.getItemStack(section);
            shop.addReward(key, new ShopReward(key, builder.build(), commands, cost, sellPrice, minPrice, maxPrice, ratio, bulkBuy));
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
     * @param identifier
     */
    public void handleInventoryAction(Player player, ShopReward reward, GuiItem guiItem, Shop shop, Gui gui, ConfigurationSection config, String identifier) {
        AtomicBoolean itemSold = new AtomicBoolean(false);
        guiItem.setAction(event -> {
            if (reward.getCost() < 0) return;
            int amount = guiItem.getItemStack().getAmount();
            final ClickType clickType = event.getClick();

            // Q is pressed / Enter bulk buy GUI and return
            if ((clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) && reward.isAllowBulkBuy()) {
                bulkPurchaseGui.open(player, shop, reward);
            } else if(clickType == ClickType.LEFT) { // Left click is used to purchase

                BigDecimal cost = BigDecimal.valueOf(0);
                BigDecimal tempPrice = BigDecimal.valueOf(reward.getCost());

                for(int i = 1; i <= amount; i++) {
                    cost = cost.add(tempPrice);

                    tempPrice = tempPrice.multiply(BigDecimal.valueOf(reward.getMultiplier()));
                    }

                if (economy.getBalance(player) >= cost.doubleValue()) {
                    reward.setCost(tempPrice.doubleValue());

                    // Run all commands attached to the item / reward
                    for (String command : reward.getCommands()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{PLAYER}", player.getName()).replace("{AMOUNT}", String.valueOf(amount)));
                    }

                    GuiUtils.setFillerItems(gui, config.getConfigurationSection("gui.filler_items"), this, player, shop);
                    gui.update();

                    // Play a success sound
                    player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 6L);

                    player.sendMessage(TextUtil.color(plugin.getConfig().get("messages.bulk_purchase_success").toString()
                            .replace("{AMOUNT}", String.valueOf(amount))
                            .replace("{COST}", String.valueOf(TextUtil.numberFormat(cost.doubleValue())))));
                    economy.withdrawPlayer(player, cost.doubleValue());

                    Set<String> buySlots = plugin.getConfig().getConfigurationSection("bulk_purchase_gui.buy_item_slots").getKeys(false);
                    boolean mainShop = false;
                    if(identifier.equals("shop")) { // Player is buying from the main page instead of bulk page
                        buySlots = shopConfig.getConfigurationSection("shop_items").getKeys(false);
                        mainShop = true;
                    }
                    // Update lore's in GUI
                    for (String slotStr : buySlots) {
                        int slot = -1;
                        try {
                            slot = Integer.parseInt(slotStr);
                        } catch(NumberFormatException e) {
                            logger.log(Level.SEVERE, "A slot from config.yml or dynamic_shop.yml cannot be parsed to an integer");
                        }
                        if(slot == -1) {
                            continue;
                        }

                        ItemStack clone = reward.getDisplayItem().clone();
                        ItemMeta meta = clone.getItemMeta();

                        if(!clone.hasItemMeta() || !meta.hasLore()) {
                            return;
                        }

                        // Update the prices to update lore
                        BigDecimal calcTotalCost = BigDecimal.valueOf(0);
                        BigDecimal calcTotalSellPrice = BigDecimal.valueOf(0);
                        BigDecimal calcCost = BigDecimal.valueOf(reward.getCost());
                        BigDecimal calcSellPrice = calcCost.divide(BigDecimal.valueOf(2), MathContext.DECIMAL128);

                        int newAmount = 1;

                        if(!mainShop) {
                            newAmount = config.getInt("buy_item_slots." + slot);
                            double multiplier = reward.getMultiplier();

                            for(int i = 1; i <= newAmount; i++) {
                                calcTotalCost = calcTotalCost.add(calcCost);
                                calcTotalSellPrice = calcTotalSellPrice.add(calcSellPrice);

                                calcCost = calcCost.multiply(BigDecimal.valueOf(multiplier));
                                calcSellPrice = calcSellPrice.divide(BigDecimal.valueOf(multiplier), MathContext.DECIMAL128);
                            }
                        } else {
                            calcTotalCost = calcCost;
                            calcTotalSellPrice = calcSellPrice;
                        }

                        List<String> lore = new ArrayList<>();
                        // Updates the lore with proper format
                        for (String line : meta.getLore()) {
                            lore.add(line
                                    .replace("{COST}", TextUtil.numberFormat(calcTotalCost.doubleValue()))
                                    .replace("{SELL_PRICE}", TextUtil.numberFormat(calcTotalSellPrice.doubleValue())));
                        }
                        clone = new ItemStackBuilder(clone).withLore(lore).withAmount(Math.min(newAmount, 64)).build();

                        if(mainShop) {
                            slot = getSlot(clone.getType(), gui.getInventory());
                        }
                        gui.updateItem(slot, clone);
                    }
                     gui.update();
                } else { // Will run if the player cannot afford the item
                     ItemStack previousItem = event.getCurrentItem();
                     player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 0L);
                     event.getClickedInventory().setItem(event.getSlot(), this.getNotEnoughCoinsItem());
                     Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> event.getClickedInventory().setItem(event.getSlot(), previousItem), 45L);
                }
            } else if(clickType == ClickType.RIGHT) {
                itemSold.set(false);
                // Amount of money to be deposited
                BigDecimal sold = BigDecimal.valueOf(0);
                // Amount of the item located in the player inventory
                int itemsSold = 0;

                BigDecimal tempPrice = BigDecimal.valueOf(reward.getCost()); // Used to update the cost properly
                BigDecimal tempSellPrice = tempPrice.divide(BigDecimal.valueOf(2), MathContext.DECIMAL128);

                if(player.getInventory().isEmpty()) {
                    return;
                }
                for (ItemStack item : player.getInventory()) {
                    if(item == null) {
                        continue;
                    } else if(itemSold.get()) {
                        break;
                    }
                    if(item.getType().equals(reward.getDisplayItem().getType())) {
                        if(amount >= item.getAmount()) { // Item in the shop has a greater or equal to amount
                            for(int i = 1; i <= item.getAmount(); i++) {
                                sold = sold.add(tempSellPrice);

                                tempSellPrice = tempSellPrice.divide(BigDecimal.valueOf(reward.getMultiplier()), MathContext.DECIMAL128);
                                tempPrice = tempPrice.divide(BigDecimal.valueOf(reward.getMultiplier()), MathContext.DECIMAL128);
                            }
                            itemsSold+=item.getAmount();
                            item.setAmount(0);

                        } else { // Item in the inventory has a higher amount than in the shop
                            for(int i = 1; i <= amount; i++) {
                                sold = sold.add(tempSellPrice);

                                tempSellPrice = tempSellPrice.divide(BigDecimal.valueOf(reward.getMultiplier()), MathContext.DECIMAL128);
                                tempPrice = tempPrice.divide(BigDecimal.valueOf(reward.getMultiplier()), MathContext.DECIMAL128);
                            }

                            itemsSold+=amount;
                            item.setAmount(item.getAmount() - amount);
                        }
                        itemSold.set(true);
                    }
                }
                reward.setCost(tempPrice.doubleValue());

                if(itemSold.get()) {
                    // Play a success sound
                    player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 6L);

                    economy.depositPlayer(player, sold.doubleValue());
                    player.sendMessage(TextUtil.color(plugin.getConfig().get("messages.sell_success").toString()
                            .replace("{AMOUNT}", String.valueOf(itemsSold))
                            .replace("{SOLD}", String.valueOf(TextUtil.numberFormat(sold.doubleValue())))));

                    Set<String> buySlots = plugin.getConfig().getConfigurationSection("bulk_purchase_gui.buy_item_slots").getKeys(false);
                    boolean mainShop = false;
                    if(identifier.equals("shop")) { // Player is buying from the main page instead of bulk page
                        buySlots = shopConfig.getConfigurationSection("shop_items").getKeys(false);
                        mainShop = true;
                    }
                    // Update lore's in GUI
                    for (String slotStr : buySlots) {
                        int slot = -1;
                        try {
                            slot = Integer.parseInt(slotStr);
                        } catch(NumberFormatException e) {
                            logger.log(Level.SEVERE, "A slot from config.yml or dynamic_shop.yml cannot be parsed to an integer");
                        }
                        if(slot == -1) {
                            continue;
                        }

                        ItemStack clone = reward.getDisplayItem().clone();
                        ItemMeta meta = clone.getItemMeta();

                        if(!clone.hasItemMeta() || !meta.hasLore()) {
                            return;
                        }

                        // Update the prices to update lore
                        BigDecimal calcTotalCost = BigDecimal.valueOf(0);
                        BigDecimal calcTotalSellPrice = BigDecimal.valueOf(0);
                        BigDecimal calcCost = BigDecimal.valueOf(reward.getCost());
                        BigDecimal calcSellPrice = calcCost.divide(BigDecimal.valueOf(2), MathContext.DECIMAL128);

                        int newAmount = 1;

                        if(!mainShop) {
                            newAmount = config.getInt("buy_item_slots." + slot);
                            double multiplier = reward.getMultiplier();

                            for(int i = 1; i <= newAmount; i++) {
                                calcTotalCost = calcTotalCost.add(calcCost);
                                calcTotalSellPrice = calcTotalSellPrice.add(calcSellPrice);

                                calcCost = calcCost.multiply(BigDecimal.valueOf(multiplier));
                                calcSellPrice = calcSellPrice.divide(BigDecimal.valueOf(multiplier), MathContext.DECIMAL128);
                            }
                        } else {
                            calcTotalCost = calcCost;
                            calcTotalSellPrice = calcSellPrice;
                        }

                        List<String> lore = new ArrayList<>();
                        // Updates the lore with proper format
                        for (String line : meta.getLore()) {
                            lore.add(line
                                    .replace("{COST}", TextUtil.numberFormat(calcTotalCost.doubleValue()))
                                    .replace("{SELL_PRICE}", TextUtil.numberFormat(calcTotalSellPrice.doubleValue())));
                        }

                        clone = new ItemStackBuilder(clone).withLore(lore).withAmount(Math.min(newAmount, 64)).build();

                        if(mainShop) {
                            slot = getSlot(clone.getType(), gui.getInventory());
                        }
                        gui.updateItem(slot, clone);
                    }
                    gui.update();

                } else {
                    player.sendMessage(TextUtil.color(plugin.getConfig().get("messages.sell_no_item").toString()));
                }
            }
        });
    }

    public int getSlot(Material type, Inventory inventory) {
        for(int i = 0; i < inventory.getSize(); i++) {
            ItemStack newItem = inventory.getItem(i);
            if(newItem != null && newItem.getType().equals(type)) {
                return i;
            }
        }
        return -1;
    }
}
