package fun.lewisdev.savedynamicshop.shop.menu;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import fun.lewisdev.savedynamicshop.config.Messages;
import fun.lewisdev.savedynamicshop.shop.Shop;
import fun.lewisdev.savedynamicshop.shop.ShopManager;
import fun.lewisdev.savedynamicshop.shop.ShopReward;
import fun.lewisdev.savedynamicshop.util.GuiUtils;
import fun.lewisdev.savedynamicshop.util.ItemStackBuilder;
import fun.lewisdev.savedynamicshop.util.SignMenuFactory;
import fun.lewisdev.savedynamicshop.util.TextUtil;
import fun.lewisdev.savedynamicshop.util.universal.XSound;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public class BulkPurchaseGui {

    private final SignMenuFactory signMenuFactory;
    private final ShopManager shopManager;

    public BulkPurchaseGui(ShopManager shopManager) {
        this.shopManager = shopManager;
        this.signMenuFactory = new SignMenuFactory(shopManager.getPlugin());
    }

    public void open(Player player, Shop shop, ShopReward reward) {
        ConfigurationSection config = shopManager.getPlugin().getConfig().getConfigurationSection("bulk_purchase_gui");
        Gui gui = new Gui(config.getInt("rows"), TextUtil.color(config.getString("title")));

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        GuiUtils.setFillerItems(gui, config.getConfigurationSection("filler_items"), shopManager, player, shop);

        gui.setOpenGuiAction(event -> player.playSound(player.getLocation(), XSound.ENTITY_BAT_TAKEOFF.parseSound(), 1L, 0L));

        GuiItem goBackItem = new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("go_back_item")).build());
        goBackItem.setAction(event -> shopManager.openDynamicShop(player));
        gui.setItem(config.getInt("go_back_item.slot"), goBackItem);

        Economy economy = shopManager.getPlugin().getEconomy();

        // Defines what happens when clicked on custom amount item (sign)
        GuiItem customAmountItem = new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("custom_amount_item")).build());
        customAmountItem.setAction(event -> {

            SignMenuFactory.Menu menu = signMenuFactory.newMenu(config.getStringList("sign_menu"))
                    .reopenIfFail(false)
                    .response((signPlayer, strings) -> {
                        long amount;
                        try {
                            amount = Long.parseLong(strings[0]);
                        }catch (NumberFormatException ex) {
                            player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 0L);
                            Messages.BULK_PURCHASE_INVALID_AMOUNT.send(player);
                            return false;
                        }

                        BigDecimal tempCost = BigDecimal.valueOf(0);
                        BigDecimal tempPrice = BigDecimal.valueOf(reward.getCost());

                        for(int i = 1; i <= amount; i++) {
                            tempCost = tempCost.add(tempPrice);

                            tempPrice = tempPrice.multiply(BigDecimal.valueOf(reward.getMultiplier()));
                        }
                        double cost = tempCost.doubleValue();

                        if (economy.getBalance(player) >= cost) {
                            reward.setCost(cost);
                            economy.withdrawPlayer(player, cost);

                            for (String command : reward.getCommands()) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{PLAYER}", player.getName()).replace("{AMOUNT}", String.valueOf(amount)));
                            }

                            player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 6L);
                            Messages.BULK_PURCHASE_PURCHASE.send(player, "{AMOUNT}", TextUtil.numberFormat(amount), "{COST}", TextUtil.format(cost));

                        }else{
                            player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 0L);
                            Messages.BULK_PURCHASE_NO_FUNDS.send(player, "{COINS}", TextUtil.format(cost));
                        }

                        return true;
                    });

            menu.open(player);

        });
        gui.setItem(config.getInt("custom_amount_item.slot"), customAmountItem);

        // Defines how many items are in every slot (and in which slots)
        for (String slot : config.getConfigurationSection("buy_item_slots").getKeys(false)) {

            ItemStack clone = reward.getDisplayItem().clone();
            int amount = config.getInt("buy_item_slots." + slot);

            BigDecimal tempTotalCost = BigDecimal.valueOf(0);
            BigDecimal tempCost = BigDecimal.valueOf(reward.getCost());
            BigDecimal tempTotalSellPrice = BigDecimal.valueOf(0);
            BigDecimal tempSellPrice = tempCost.divide(BigDecimal.valueOf(2), MathContext.DECIMAL128);
            double multiplier = reward.getMultiplier();

            for(int i = 1; i <= amount; i++) {
                tempTotalCost = tempTotalCost.add(tempCost);

                tempCost = tempCost.multiply(BigDecimal.valueOf(multiplier));
                tempSellPrice = tempSellPrice.divide(BigDecimal.valueOf(multiplier), MathContext.DECIMAL128);

                tempTotalSellPrice = tempTotalSellPrice.add(tempSellPrice);
            }

            double cost = tempTotalCost.doubleValue();
            double sellPrice = tempTotalSellPrice.doubleValue();

            List<String> lore = new ArrayList<>();
            // Updates the lore with proper format
            clone.getItemMeta().getLore().forEach(line -> lore.add(line
                    .replace("{COST}", TextUtil.numberFormat(cost))
                    .replace("{SELL_PRICE}", TextUtil.numberFormat(sellPrice))));

            GuiItem guiItem = new GuiItem(new ItemStackBuilder(clone).withLore(lore).withAmount(Math.min(amount, 64)).build());
            shopManager.handleInventoryAction(player, reward, guiItem, shop, gui, config);
            gui.setItem(Integer.parseInt(slot), guiItem);
        }

        gui.open(player);
    }

}
