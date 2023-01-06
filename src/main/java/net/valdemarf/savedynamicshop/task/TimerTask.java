package net.valdemarf.savedynamicshop.task;

import net.valdemarf.savedynamicshop.SaveDynamicShop;
import net.valdemarf.savedynamicshop.shop.Shop;
import net.valdemarf.savedynamicshop.shop.ShopManager;
import net.valdemarf.savedynamicshop.util.TextUtil;
import org.bukkit.Bukkit;

public class TimerTask implements Runnable {

    private final ShopManager shopManager;

    public TimerTask(SaveDynamicShop plugin) {
        this.shopManager = plugin.getShopManager();
    }

    @Override
    public void run() {
        Shop shop = shopManager.getBuyShop();
        if (!shop.isRotatingShop()) return;

        if (shop.getRefreshTime() < System.currentTimeMillis()) {

            shop.resetRefreshTime();
            shop.resetShopItems();

            String broadcast = TextUtil.color(shop.getConfig().getString("messages.shop_refreshed"));
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(broadcast));
        }
    }

}
