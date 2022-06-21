package fun.lewisdev.saverotatingshop.command;

import fun.lewisdev.saverotatingshop.SaveDynamicShop;
import fun.lewisdev.saverotatingshop.config.Messages;
import fun.lewisdev.saverotatingshop.shop.BuyShop;
import fun.lewisdev.saverotatingshop.shop.ShopManager;
import fun.lewisdev.saverotatingshop.util.TextUtil;
import me.mattstudios.mf.annotations.*;
import me.mattstudios.mf.base.CommandBase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command("shop")
public class ShopCommand extends CommandBase {

    private final SaveDynamicShop plugin;
    private final ShopManager shopManager;

    public ShopCommand(SaveDynamicShop plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getShopManager();
    }

    @Default
    public void defaultCommand(CommandSender sender) {
        if (sender instanceof Player) {
            shopManager.openRotatingShop((Player) sender);
        } else {
            helpSubCommand(sender);
        }
    }

    @SubCommand("help")
    @Permission("saverotatingshop.admin")
    public void helpSubCommand(CommandSender sender) {
        Messages.HELP_DEFAULT.send(sender, "{VERSION}", plugin.getDescription().getVersion());
    }

    @SubCommand("reload")
    @Permission("saverotatingshop.admin")
    public void reloadSubCommand(CommandSender sender) {
        plugin.onReload();
        Messages.RELOAD.send(sender);
    }

    @SubCommand("refresh")
    @Permission("saverotatingshop.admin")
    public void refreshSubCommand(CommandSender sender) {
        BuyShop shop = shopManager.getBuyShop();
        if (!shop.isRotatingShop()) {
            sender.sendMessage(TextUtil.color("&cThe shop is not a rotating shop."));
        } else {
            shop.resetShopItems();
            shop.resetRefreshTime();
            Messages.COMMAND_REFRESH_SHOP.send(sender);
        }
    }
}
