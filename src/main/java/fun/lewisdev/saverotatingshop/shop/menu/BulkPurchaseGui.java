package fun.lewisdev.saverotatingshop.shop.menu;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import fun.lewisdev.saverotatingshop.config.Messages;
import fun.lewisdev.saverotatingshop.shop.BuyShop;
import fun.lewisdev.saverotatingshop.shop.ShopManager;
import fun.lewisdev.saverotatingshop.shop.ShopReward;
import fun.lewisdev.saverotatingshop.util.GuiUtils;
import fun.lewisdev.saverotatingshop.util.ItemStackBuilder;
import fun.lewisdev.saverotatingshop.util.SignMenuFactory;
import fun.lewisdev.saverotatingshop.util.TextUtil;
import fun.lewisdev.saverotatingshop.util.universal.XSound;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
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

    public void open(Player player, BuyShop shop, ShopReward shopReward) {
        ConfigurationSection config = shopManager.getPlugin().getConfig().getConfigurationSection("bulk_purchase_gui");
        Gui gui = new Gui(config.getInt("rows"), TextUtil.color(config.getString("title")));

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        GuiUtils.setFillerItems(gui, config.getConfigurationSection("filler_items"), shopManager, player, shop);

        gui.setOpenGuiAction(event -> player.playSound(player.getLocation(), XSound.ENTITY_BAT_TAKEOFF.parseSound(), 1L, 0L));

        GuiItem goBackItem = new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("go_back_item")).build());
        goBackItem.setAction(event -> shopManager.openRotatingShop(player));
        gui.setItem(config.getInt("go_back_item.slot"), goBackItem);

        Economy economy = shopManager.getPlugin().getEconomy();
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

        for (String slot : config.getConfigurationSection("buy_item_slots").getKeys(false)) {

            ItemStack clone = shopReward.getDisplayItem().clone();
            int amount = config.getInt("buy_item_slots." + slot);
            long cost = shopReward.getCost() * amount;

            List<String> lore = new ArrayList<>();
            clone.getItemMeta().getLore().forEach(line -> lore.add(line.replace("{COST}", TextUtil.numberFormat(cost))));
            GuiItem guiItem = new GuiItem(new ItemStackBuilder(clone).withLore(lore).withAmount(Math.min(amount, 64)).build());

            guiItem.setAction(event -> {
                if (economy.getBalance(player) >= cost) {
                    economy.withdrawPlayer(player, cost);

                    for (String command : shopReward.getCommands()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{PLAYER}", player.getName()).replace("{AMOUNT}", String.valueOf(amount)));
                    }

                    GuiUtils.setFillerItems(gui, config.getConfigurationSection("filler_items"), shopManager, player, shop);
                    gui.update();

                    ItemStack previousItem = event.getCurrentItem();
                    player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 6L);
                    event.getClickedInventory().setItem(event.getSlot(), shopManager.getPurchaseSuccessItem());
                    Bukkit.getScheduler().runTaskLater(shopManager.getPlugin(), () -> {
                        event.getClickedInventory().setItem(event.getSlot(), previousItem);
                    }, 20L);

                } else {
                    ItemStack previousItem = event.getCurrentItem();
                    player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 0L);
                    event.getClickedInventory().setItem(event.getSlot(), shopManager.getNotEnoughCoinsItem());
                    Bukkit.getScheduler().runTaskLater(shopManager.getPlugin(), () -> event.getClickedInventory().setItem(event.getSlot(), previousItem), 45L);
                }
            });
            gui.setItem(Integer.parseInt(slot), guiItem);
        }

        gui.open(player);
    }

}
