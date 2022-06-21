package fun.lewisdev.saverotatingshop.command;

import fun.lewisdev.saverotatingshop.shop.ShopManager;
import me.mattstudios.mf.annotations.Command;
import me.mattstudios.mf.annotations.Default;
import me.mattstudios.mf.base.CommandBase;
import org.bukkit.entity.Player;

@Command("sell")
public class SellCommand extends CommandBase {

    private final ShopManager shopManager;

    public SellCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Default
    public void defaultCommand(Player player) {
        shopManager.openSellGui(player);
    }
}