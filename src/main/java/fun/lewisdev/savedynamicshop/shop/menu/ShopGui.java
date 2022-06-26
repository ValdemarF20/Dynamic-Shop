package fun.lewisdev.savedynamicshop.shop.menu;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import fun.lewisdev.savedynamicshop.shop.Shop;
import fun.lewisdev.savedynamicshop.shop.ShopManager;
import fun.lewisdev.savedynamicshop.shop.ShopReward;
import fun.lewisdev.savedynamicshop.util.GuiUtils;
import fun.lewisdev.savedynamicshop.util.ItemStackBuilder;
import fun.lewisdev.savedynamicshop.util.TextUtil;
import fun.lewisdev.savedynamicshop.util.universal.XSound;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class ShopGui {

    private final ShopManager shopManager;

    public ShopGui(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void open(Player player, Shop shop) {
        FileConfiguration config = shop.getConfig();
        Gui gui = new Gui(config.getInt("gui.rows"), TextUtil.color(config.getString("gui.title")));

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        GuiUtils.setFillerItems(gui, config.getConfigurationSection("gui.filler_items"), shopManager, player, shop);

        gui.setOpenGuiAction(event -> player.playSound(player.getLocation(), XSound.ENTITY_BAT_TAKEOFF.parseSound(), 1L, 0L));

        ShopReward errorItem = new ShopReward("error", new ItemStackBuilder(Material.BARRIER).withName("&c&lINVALID SHOP ITEM").withLore(Arrays.asList("&fThis item no longer exists", "&fin the configuration. Please", "&frun &6/mobcoins refresh")).build(), Collections.emptyList(), -999, 0, 0, 0, 0, false);
        List<ShopReward> rewardList = shop.getActiveRewards().stream().map(identifier -> shop.getRewards().getOrDefault(identifier, errorItem)).collect(Collectors.toList());
        for (ShopReward reward : rewardList) {
            List<String> lore = new ArrayList<>();
            ItemStack displayItem =reward.getDisplayItem();
            // Replace placeholders
            if(displayItem.hasItemMeta() && displayItem.getItemMeta().hasLore()) {
                displayItem.getItemMeta().getLore().forEach(line -> lore.add(line
                        .replace("{COST}", TextUtil.numberFormat(reward.getCost()))
                        .replace("{SELL_PRICE}", TextUtil.numberFormat(reward.getSellPrice()))));
            }

            GuiItem guiItem = new GuiItem(new ItemStackBuilder(displayItem.clone()).withLore(lore).build());
            shopManager.handleInventoryAction(player, reward, guiItem, shop, gui, config);
            gui.addItem(guiItem);
        }

        gui.open(player);
    }
}
