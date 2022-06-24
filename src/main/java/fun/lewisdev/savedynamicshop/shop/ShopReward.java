package fun.lewisdev.savedynamicshop.shop;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopReward {

    private final String identifier;
    private final ItemStack displayItem;
    private final List<String> commands;
    private final long cost;
    private final long sellPrice;
    private final boolean allowBulkBuy;

    public ShopReward(String identifier, ItemStack displayItem, List<String> commands, long cost, long sellPrice, boolean allowBulkBuy) {
        this.identifier = identifier;
        this.displayItem = displayItem;
        this.commands = commands;
        this.cost = cost;
        this.sellPrice = sellPrice;
        this.allowBulkBuy = allowBulkBuy;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public List<String> getCommands() {
        return commands;
    }

    public long getCost() {
        return cost;
    }

    public long getSellPrice() {
        return sellPrice;
    }

    public boolean isAllowBulkBuy() {
        return allowBulkBuy;
    }
}
