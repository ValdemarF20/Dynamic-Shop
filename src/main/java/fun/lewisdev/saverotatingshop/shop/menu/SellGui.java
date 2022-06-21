package fun.lewisdev.saverotatingshop.shop.menu;

import dev.triumphteam.gui.guis.Gui;
import fun.lewisdev.saverotatingshop.config.Messages;
import fun.lewisdev.saverotatingshop.shop.ShopManager;
import fun.lewisdev.saverotatingshop.util.GuiUtils;
import fun.lewisdev.saverotatingshop.util.TextUtil;
import fun.lewisdev.saverotatingshop.util.universal.XSound;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class SellGui {

    private final ShopManager shopManager;
    private final FileConfiguration config;

    public SellGui(ShopManager shopManager, FileConfiguration config) {
        this.shopManager = shopManager;
        this.config = config;
    }

    public void open(Player player) {
        Gui gui = new Gui(config.getInt("gui.rows"), TextUtil.color(config.getString("gui.title")));

        GuiUtils.setFillerItems(gui, config.getConfigurationSection("gui.filler_items"), shopManager, player, null);

        gui.setOpenGuiAction(event -> player.playSound(player.getLocation(), XSound.ENTITY_BAT_TAKEOFF.parseSound(), 1L, 0L));

        gui.setCloseGuiAction(event -> {
            List<ItemStack> toReturn = new ArrayList<>();
            double total = 0;
            int amount = 0;
            for(ItemStack item : gui.getInventory().getContents()) {
                if(item == null || item.getType() == Material.AIR) continue;

                final Material material = item.getType();
                if(shopManager.getSellItems().containsKey(material)) {
                    total += shopManager.getSellItems().get(material) * item.getAmount();
                    amount += item.getAmount();
                }else{
                    toReturn.add(item);
                }
            }

            if(total > 0) {
                shopManager.getPlugin().getEconomy().depositPlayer(player, total);
                Messages.SELL_SUCCESS.send(player, "{AMOUNT}", amount, "{SOLD}", total);
                player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1L, 0L);
            }

            if(!toReturn.isEmpty()) {
                PlayerInventory inventory = player.getInventory();
                toReturn.forEach(inventory::addItem);
                Messages.SELL_RETURNED.send(player);
            }
        });

        gui.open(player);
    }

}
