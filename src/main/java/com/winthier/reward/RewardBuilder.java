package com.winthier.reward;

import com.winthier.reward.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RewardBuilder {
    final RewardPlugin plugin;
    UUID uuid;
    String name;
    String comment;
    double money;
    int exp;
    final List<ItemStack> items = new ArrayList<>();
    final Map<String, Integer> currencies = new HashMap<>();
    final Map<String, Integer> flags = new HashMap<>();
    final List<String> commands = new ArrayList<>();

    public RewardBuilder(RewardPlugin plugin) {
        this.plugin = plugin;
    }

    public static RewardBuilder create() {
        return RewardPlugin.getInstance().createBuilder();
    }

    public RewardBuilder player(Player player) {
        uuid = player.getUniqueId();
        name = player.getName();
        return this;
    }

    /**
     * Only call once. Additional calls will override previous
     * calls. You have to call either this or name().
     */
    public RewardBuilder uuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    /**
     * Only call once. Additional calls will override previous
     * calls. You have to call either this or uuid().
     */
    public RewardBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Only call once. Additional calls will override previous
     * calls.
     */
    public RewardBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Call as often as needed. Additional calls will add more
     * items.
     */
    public RewardBuilder item(ItemStack... items) {
        for (ItemStack item : items) this.items.add(item);
        return this;
    }

    /**
     * Call as often as needed. Additional calls will add more
     * items.
     */
    public RewardBuilder items(List<ItemStack> items) {
        for (ItemStack item : items) this.items.add(item);
        return this;
    }

    /**
     * Add exp. Call as often as needed.
     */
    public RewardBuilder exp(int exp) {
        this.exp += exp;
        return this;
    }

    /**
     * Add money. Call as often as needed.
     */
    public RewardBuilder money(double money) {
        this.money += money;
        return this;
    }

    /**
     * Add a currency. Call as often as needed. Multiple calls
     * with the same name will be added up.
     */
    public RewardBuilder currency(String name, int amount) {
        Integer value = currencies.get(name);
        if (value == null) value = 0;
        value += amount;
        currencies.put(name, value);
        return this;
    }

    /**
     * Set a flag. Subsequent calls with the same name will
     * override the previous value.
     */
    public RewardBuilder flag(String name, int amount) {
        flags.put(name, amount);
        return this;
    }

    /**
     * Add a command. Replaced will be:
     * %player% -> name of the player
     * %uuid% -> uuid of the player
     */
    public RewardBuilder command(String command) {
        commands.add(command);
        return this;
    }

    /**
     * Add rewards via configuration section.
     */
    public RewardBuilder config(ConfigurationSection config) {
        if (config == null) return this;
        plugin.getLogger().info("Loading reward config " + config.getName() + " for player " + name + "...");
        if (config.isSet("Daily") && uuid != null && !plugin.checkAndSetDaily(uuid, config.getString("Daily"))) {
            plugin.getLogger().info("Skipping reward config " + config.getName() + " because the daily " + config.getString("Daily") + " has already been taken by " + name);
            return this;
        }
        comment = config.getString("Comment", comment);
        exp += config.getInt("Exp", 0);
        money += config.getDouble("Money", 0.0);
        for (Object o : config.getList("items", Collections.emptyList())) {
            if (o instanceof ItemStack) {
                items.add((ItemStack)o);
            } else if (o instanceof Map) {
                try {
                    @SuppressWarnings("unchecked") Map<String, Object> tmp = (Map<String, Object>)o;
                    ItemStack item = ItemStack.deserialize(tmp);
                    items.add(item);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        ConfigurationSection section;
        if (null != (section = config.getConfigurationSection("currencies"))) {
            for (String key : section.getKeys(false)) {
                currency(key, section.getInt(key, 0));
            }
        }
        if (null != (section = config.getConfigurationSection("flags"))) {
            for (String key : section.getKeys(false)) {
                flag(key, section.getInt(key, 0));
            }
        }
        for (String command : config.getStringList("Commands")) {
            this.command(command);
        }
        return this;
    }

    public Reward store() {
        if (uuid == null) throw new RuntimeException("UUID cannot be undefined.");
        if (isEmpty()) return null;
        Reward reward = new Reward();
        if (uuid != null) reward.setUuid(uuid);
        if (name != null) reward.setName(name);
        if (comment != null) reward.setComment(comment);
        if (money > 0.01) reward.setMoney(money);
        if (exp > 0) reward.setExp(exp);
        if (!items.isEmpty()) reward.setItemStacks(items);
        if (!currencies.isEmpty()) reward.setCurrencyMap(currencies);
        if (!flags.isEmpty()) reward.setFlagMap(flags);
        if (!commands.isEmpty()) reward.setCommandList(commands);
        reward.createdNow();
        plugin.getDb().save(reward);
        return reward;
    }

    public boolean isEmpty() {
        return
            money < 0.01 &&
            exp == 0 &&
            items.isEmpty() &&
            currencies.isEmpty() &&
            flags.isEmpty() &&
            commands.isEmpty();
    }
}
