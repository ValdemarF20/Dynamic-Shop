package fun.lewisdev.saverotatingshop.task;

import fun.lewisdev.saverotatingshop.SaveDynamicShop;
import fun.lewisdev.saverotatingshop.shop.BuyShop;
import fun.lewisdev.saverotatingshop.shop.ShopManager;
import fun.lewisdev.saverotatingshop.util.TextUtil;
import org.bukkit.Bukkit;

public class TimerTask implements Runnable {

    private final ShopManager shopManager;

    public TimerTask(SaveDynamicShop plugin) {
        this.shopManager = plugin.getShopManager();
    }

    @Override
    public void run() {
        BuyShop shop = shopManager.getBuyShop();
        if (!shop.isRotatingShop()) return;

        if (shop.getRefreshTime() < System.currentTimeMillis()) {

            shop.resetRefreshTime();
            shop.resetShopItems();

            String broadcast = TextUtil.color(shop.getConfig().getString("messages.shop_refreshed"));
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(broadcast));
        }
    }

}
