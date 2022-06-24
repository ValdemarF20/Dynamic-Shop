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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BulkPurchaseGui {

    private final SignMenuFactory signMenuFactory;
    private final ShopManager shopManager;

    public BulkPurchaseGui(ShopManager shopManager) {
        this.shopManager = shopManager;
        this.signMenuFactory = new SignMenuFactory(shopManager.getPlugin());
    }

    public void open(Player player, Shop shop, ShopReward shopReward) {
        ConfigurationSection config = shopManager.getPlugin().getConfig().getConfigurationSection("bulk_purchase_gui");
        Gui gui = new Gui(config.getInt("rows"), TextUtil.color(config.getString("title")));

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        GuiUtils.setFillerItems(gui, config.getConfigurationSection("filler_items"), shopManager, player, shop);

        gui.setOpenGuiAction(event -> player.playSound(player.getLocation(), XSound.ENTITY_BAT_TAKEOFF.parseSound(), 1L, 0L));

        GuiItem goBackItem = new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("go_back_item")).build());
        goBackItem.setAction(event -> shopManager.openDynamicShop(player));
        gui.setItem(config.getInt("go_back_item.slot"), goBackItem);

        Economy economy = shopManager.getPlugin().getEconomy();

        // Defines what happens when clicked on custom amount item
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

                        long cost = shopReward.getCost() * amount;
                        if (economy.getBalance(player) >= cost) {
                            economy.withdrawPlayer(player, cost);

                            Bukkit.getScheduler().runTask(shopManager.getPlugin(), () -> {
                                for (String command : shopReward.getCommands()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{PLAYER}", player.getName()).replace("{AMOUNT}", String.valueOf(amount)));
                                }
                            });

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

            ItemStack clone = shopReward.getDisplayItem().clone();
            int amount = config.getInt("buy_item_slots." + slot);
            long cost = shopReward.getCost() * amount;

            List<String> lore = new ArrayList<>();
            // Updates the lore with proper format
            clone.getItemMeta().getLore().forEach(line -> lore.add(line.replace("{COST}", TextUtil.numberFormat(cost))));

            GuiItem guiItem = new GuiItem(new ItemStackBuilder(clone).withLore(lore).withAmount(Math.min(amount, 64)).build());
            shopManager.handleInventoryAction(player, shopReward, guiItem, shop, gui, config);
            /*
            // Defines what happens when an purchasable item is clicked on
            guiItem.setAction(event -> {
                if (economy.getBalance(player) >= cost) { // Checks if the player has enough money
                    economy.withdrawPlayer(player, cost);

                    for (String command : shopReward.getCommands()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{PLAYER}", player.getName()).replace("{AMOUNT}", String.valueOf(amount)));
                    }

                    //TODO: Why is this run again? After running above
                    GuiUtils.setFillerItems(gui, config.getConfigurationSection("filler_items"), shopManager, player, shop);
                    gui.update();

                    ItemStack previousItem = event.getCurrentItem();
                    player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 6L);
                    event.getClickedInventory().setItem(event.getSlot(), shopManager.getPurchaseSuccessItem());
                    Bukkit.getScheduler().runTaskLater(shopManager.getPlugin(), () -> {
                        event.getClickedInventory().setItem(event.getSlot(), previousItem);
                    }, 20L);

                } else { // Runs when player cannot afford the item
                    ItemStack previousItem = event.getCurrentItem();
                    player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 0L);
                    event.getClickedInventory().setItem(event.getSlot(), shopManager.getNotEnoughCoinsItem());
                    Bukkit.getScheduler().runTaskLater(shopManager.getPlugin(), () -> event.getClickedInventory().setItem(event.getSlot(), previousItem), 45L);
                }
            });
             */
            gui.setItem(Integer.parseInt(slot), guiItem);
        }

        gui.open(player);
    }

}
