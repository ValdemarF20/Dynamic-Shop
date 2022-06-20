package fun.lewisdev.saverotatingshop.shop.menu;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import fun.lewisdev.saverotatingshop.shop.BuyShop;
import fun.lewisdev.saverotatingshop.shop.ShopManager;
import fun.lewisdev.saverotatingshop.shop.ShopReward;
import fun.lewisdev.saverotatingshop.util.GuiUtils;
import fun.lewisdev.saverotatingshop.util.ItemStackBuilder;
import fun.lewisdev.saverotatingshop.util.TextUtil;
import fun.lewisdev.saverotatingshop.util.universal.XSound;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class ShopGui {

    private final ShopManager shopManager;
    private final BulkPurchaseGui bulkPurchaseGui;

    public ShopGui(ShopManager shopManager) {
        this.shopManager = shopManager;
        this.bulkPurchaseGui = new BulkPurchaseGui(shopManager);
    }

    public void open(Player player, BuyShop shop) {
        FileConfiguration config = shop.getConfig();
        Gui gui = new Gui(config.getInt("gui.rows"), TextUtil.color(config.getString("gui.title")));

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        GuiUtils.setFillerItems(gui, config.getConfigurationSection("gui.filler_items"), shopManager, player, shop);

        gui.setOpenGuiAction(event -> player.playSound(player.getLocation(), XSound.ENTITY_BAT_TAKEOFF.parseSound(), 1L, 0L));

        ShopReward errorItem = new ShopReward("error", new ItemStackBuilder(Material.BARRIER).withName("&c&lINVALID SHOP ITEM").withLore(Arrays.asList("&fThis item no longer exists", "&fin the configuration. Please", "&frun &6/mobcoins refresh")).build(), Collections.emptyList(), -999, false);
        List<ShopReward> rewardList = shop.getActiveRewards().stream().map(identifier -> shop.getRewards().getOrDefault(identifier, errorItem)).collect(Collectors.toList());
        for (ShopReward reward : rewardList) {
            List<String> lore = new ArrayList<>();
            reward.getDisplayItem().getItemMeta().getLore().forEach(line -> lore.add(line.replace("{COST}", TextUtil.numberFormat(reward.getCost()))));
            GuiItem guiItem = new GuiItem(new ItemStackBuilder(reward.getDisplayItem().clone()).withLore(lore).build());
            guiItem.setAction(event -> {
                if (reward.getCost() < 0) return;

                if (event.getClick().isLeftClick() && reward.isAllowBulkBuy()) {
                    bulkPurchaseGui.open(player, shop, reward);
                    return;
                }

                Economy economy = shopManager.getPlugin().getEconomy();
                if (economy.getBalance(player) >= reward.getCost()) {
                    economy.withdrawPlayer(player, reward.getCost());

                    for (String command : reward.getCommands()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{PLAYER}", player.getName()).replace("{AMOUNT}", String.valueOf(1)));
                    }

                    GuiUtils.setFillerItems(gui, config.getConfigurationSection("gui.filler_items"), shopManager, player, shop);
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
            gui.addItem(guiItem);
        }

        gui.open(player);
    }

}
