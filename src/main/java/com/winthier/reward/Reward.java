package com.winthier.reward;

import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

@Entity
@Table(name = "rewards")
public class Reward {
    @Id
    private Integer id;

    @Version
    private Integer version;

    private Timestamp created;
    private Timestamp delivered;

    private UUID uuid; // player uuid

    @Length(max = 16)
    private String name; // player name, if uuid == null

    private String comment;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reward")
    private List<Item> items;

    private Integer exp;
    private Double money;

    public Reward() {}

    public Integer getId() { return this.id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getVersion() { return this.version; }
    public void setVersion(Integer version) { this.version = version; }

    public Timestamp getCreated() { return this.created; }
    public void setCreated(Timestamp created) { this.created = created; }

    public Timestamp getDelivered() { return this.delivered; }
    public void setDelivered(Timestamp delivered) { this.delivered = delivered; }

    public UUID getUuid() { return this.uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }

    public String getComment() { return this.comment; }
    public void setComment(String comment) { this.comment = comment; }

    public List<Item> getItems() { return this.items; }
    public void setItems(List<Item> items) { this.items = items; }

    public Integer getExp() { return this.exp; }
    public void setExp(Integer exp) { this.exp = exp; }

    public Double getMoney() { return this.money; }
    public void setMoney(Double money) { this.money = money; }

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
        setDelivered(new Timestamp(System.currentTimeMillis()));
    }

    public void createdNow() {
        setCreated(new Timestamp(System.currentTimeMillis()));
    }
}
