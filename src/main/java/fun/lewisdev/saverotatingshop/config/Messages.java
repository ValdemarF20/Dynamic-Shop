package fun.lewisdev.saverotatingshop.config;

import fun.lewisdev.saverotatingshop.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public enum Messages {

    RELOAD("reload"),
    NO_PERMISSION("no_permission"),
    HELP_DEFAULT("help_message"),

    COMMAND_REFRESH_SHOP("shop_refreshed"),

    SELL_SUCCESS("sell_success"),
    SELL_RETURNED("sell_returned"),

    BULK_PURCHASE_NO_FUNDS("bulk_purchase_no_funds"),
    BULK_PURCHASE_INVALID_AMOUNT("bulk_purchase_invalid_input"),
    BULK_PURCHASE_PURCHASE("bulk_purchase_success");

    private static FileConfiguration config;
    private final String path;

    Messages(String path) {
        this.path = path;
    }

    public static void setConfiguration(FileConfiguration c) {
        config = c;
    }

    public void send(CommandSender receiver, Object... replacements) {
        Object value = config.get("messages." + this.path);

        String message;
        if (value == null) {
            message = "SaveRotatingShop: message not found (" + this.path + ")";
        }else {
            message = value instanceof List ? TextUtil.fromList((List<?>) value) : value.toString();
        }

        if (!message.isEmpty()) {
            receiver.sendMessage(TextUtil.color(replace(message, replacements)));
        }
    }

    private String replace(String message, Object... replacements) {
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 >= replacements.length) break;
            message = message.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }

        return message;
    }

    public String getPath() {
        return this.path;
    }

}
