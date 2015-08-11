package com.winthier.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class Builder {
    private final RewardPlugin plugin;
    private final List<ItemStack> items = new ArrayList<>();
    private final Reward reward = new Reward();

    public Builder(RewardPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Only call once. Additional calls will override previous
     * calls. You have to call either this or name().
     */
    public Builder uuid(UUID uuid) {
        reward.setUuid(uuid);
        return this;
    }

    /**
     * Only call once. Additional calls will override previous
     * calls. You have to call either this or uuid().
     */
    public Builder name(String name) {
        reward.setName(name);
        return this;
    }

    /**
     * Only call once. Additional calls will override previous
     * calls.
     */
    public Builder comment(String comment) {
        reward.setComment(comment);
        return this;
    }

    /**
     * Call as often as needed. Additional calls will add more
     * items.
     */
    public Builder item(ItemStack... items) {
        for (ItemStack item : items) this.items.add(item);
        return this;
    }

    /**
     * Call as often as needed. Additional calls will add more
     * items.
     */
    public Builder items(List<ItemStack> items) {
        for (ItemStack item : items) this.items.add(item);
        return this;
    }

    /**
     * Only call once. Additional calls will override previous
     * calls.
     */
    public Builder exp(int exp) {
        reward.setExp(exp);
        return this;
    }

    /**
     * Only call once. Additional calls will override previous
     * calls.
     */
    public Builder money(double money) {
        reward.setMoney(money);
        return this;
    }

    public Reward store() {
        if (reward.getUuid() == null && reward.getName() == null) {
            throw new RuntimeException("UUID and name can't both be undefined.");
        }
        if (!items.isEmpty()) reward.setItemStacks(items);
        reward.createdNow();
        plugin.getDatabase().save(reward);
        return reward;
    }
}
