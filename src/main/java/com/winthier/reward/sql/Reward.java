package com.winthier.reward.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

@Entity
@Table(name = "rewards")
@Getter
@Setter
public class Reward {
    @Id Integer id;
    @Version Integer version;
    Date created;
    Date delivered;
    @NotNull UUID uuid; // player uuid
    @Length(max = 16) String name; // player name, optional
    String comment;
    Integer exp;
    Double money;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reward") List<Item> items;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reward") List<Currency> currencies;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reward") List<Flag> flags;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reward") List<Command> commands;

    public void setItemStacks(ItemStack... itemStacks) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("items", Arrays.<ItemStack>asList(itemStacks));
        String string = config.saveToString();
        int len = (string.length() - 1) / 255 + 1;
        ArrayList<Item> items = new ArrayList<Item>(len);
        for (int i = 0; i < len; ++i) {
            int begin = i * 255;
            int end = Math.min(begin + 255, string.length());
            String line = string.substring(begin, end);
            items.add(new Item(this, i, line));
        }
        setItems(items);
    }

    public void setItemStacks(List<ItemStack> itemStacks) {
        setItemStacks(itemStacks.toArray(new ItemStack[0]));
    }

    public void setCurrencyMap(Map<String, Integer> currencies) {
        List<Currency> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : currencies.entrySet()) {
            list.add(new Currency(this, entry.getKey(), entry.getValue()));
        }
        setCurrencies(list);
    }

    public void setFlagMap(Map<String, Integer> flags) { 
        List<Flag> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : flags.entrySet()) {
            list.add(new Flag(this, entry.getKey(), entry.getValue()));
        }
        setFlags(list);
    }

    public void setCommandList(List<String> commands) {
        List<Command> list = new ArrayList<>();
        for (String command : commands) list.add(new Command(this, command));
        setCommands(list);
    }

    public ItemStack[] getItemStacks() {
        List<Item> items = getItems();
        if (items.isEmpty()) return new ItemStack[0];
        String[] tokens = new String[items.size()];
        for (Item item : items) tokens[item.getLineNumber()] = item.getLine();
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) sb.append(token);
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(sb.toString());
        } catch (InvalidConfigurationException ice) {
            ice.printStackTrace();
            return new ItemStack[0];
        }
        List<ItemStack> result = new ArrayList<>();
        for (Object o : config.getList("items")) {
            if (o instanceof ItemStack) {
                result.add((ItemStack)o);
            } else {
                System.err.println("[Reward] Reward Item constains type " + o.getClass().getName());
            }
        }
        return result.toArray(new ItemStack[0]);
    }

    public void delivered() {
        setDelivered(new Date());
    }

    public void createdNow() {
        setCreated(new Date());
    }
}
