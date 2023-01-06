package net.valdemarf.savedynamicshop.shop;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopReward {

    private final String identifier;
    private final ItemStack displayItem;
    private final List<String> commands;
    private double cost;
    private double sellPrice;
    private final boolean allowBulkBuy;
    private final long minPrice;
    private final long maxPrice;
    private double multiplier;

    public ShopReward(String identifier, ItemStack displayItem, List<String> commands, double cost, double sellPrice, long minPrice, long maxPrice, double multiplier, boolean allowBulkBuy) {
        this.identifier = identifier;
        this.displayItem = displayItem;
        this.commands = commands;
        this.cost = cost;
        this.sellPrice = sellPrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.multiplier = multiplier;
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

    public double getCost() {
        return cost;
    }

    public double getSellPrice() {
        return (cost / 2);
    }

    public long getMinPrice() {
        return minPrice;
    }

    public long getMaxPrice() {
        return maxPrice;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setCost(double n) {
        this.cost = n;
    }

    public boolean isAllowBulkBuy() {
        return allowBulkBuy;
    }
}
