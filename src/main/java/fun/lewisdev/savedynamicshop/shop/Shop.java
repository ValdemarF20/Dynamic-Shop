package fun.lewisdev.savedynamicshop.shop;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class Shop {

    private final FileConfiguration config;

    private final Map<String, ShopReward> rewards;
    private List<String> activeRewards;
    private int hours;
    private long refreshTime;
    private int shopSize;

    public Shop(FileConfiguration config) {
        this.config = config;
        this.rewards = new LinkedHashMap<>();
        this.activeRewards = new ArrayList<>();
    }

    void setRotatingShopTime(int hours) {
        this.hours = hours;
        if (hours != -1) {
            this.refreshTime = System.currentTimeMillis() + hours * 1000 * 3600;
        } else {
            this.refreshTime = -1;
        }
    }

    void addReward(String identifier, ShopReward reward) {
        rewards.put(identifier, reward);
    }

    void setShopSize(int size) {
        this.shopSize = size;
    }

    void setActiveRewards(List<String> rewards) {
        if (rewards.size() > shopSize) {
            rewards.subList(shopSize, rewards.size()).clear();
        }

        this.activeRewards = rewards;
    }

    public void resetShopItems() {
        List<ShopReward> rewardList = rewards.values().stream().filter(reward -> !activeRewards.contains(reward.getIdentifier())).collect(Collectors.toList());

        if(isRotatingShop()) Collections.shuffle(rewardList);

        if (rewardList.size() > shopSize) {
            rewardList.subList(shopSize, rewardList.size()).clear();
        } else if (rewardList.size() < shopSize) {
            List<String> activeRewardsClone = new ArrayList<>(activeRewards);
            Random rand = new Random();
            int amount = shopSize - rewardList.size();
            for (int i = 1; i <= amount; i++) {
                if (!activeRewardsClone.isEmpty()) {
                    String randReward = activeRewardsClone.get(rand.nextInt(activeRewardsClone.size()));
                    rewardList.add(rewards.get(randReward));
                    activeRewardsClone.remove(randReward);
                }
            }
        }

        activeRewards.clear();
        activeRewards.addAll(rewardList.stream().map(ShopReward::getIdentifier).collect(Collectors.toList()));
    }

    public long getRefreshTime() {
        return refreshTime;
    }

    public boolean isRotatingShop() {
        return refreshTime != -1;
    }

    public void resetRefreshTime() {
        refreshTime = System.currentTimeMillis() + hours * 1000 * 3600;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public List<String> getActiveRewards() {
        return activeRewards;
    }

    public Map<String, ShopReward> getRewards() {
        return rewards;
    }

    public int getShopSize() {
        return shopSize;
    }

    public void setRefreshTime(long refreshTime) {
        this.refreshTime = refreshTime;
    }
}
