package com.winthier.reward;

import com.winthier.reward.sql.*;
import com.winthier.sql.SQLDatabase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class RewardPlugin extends JavaPlugin implements Listener {
    boolean deliver;
    Economy economy;
    public static final String PERM_RECEIVE = "reward.receive";
    @Getter static RewardPlugin instance = null;
    private SQLDatabase db;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        configure();
        db = new SQLDatabase(this);
        db.registerTables(Reward.class,
                          Item.class,
                          Currency.class,
                          Flag.class,
                          Command.class,
                          Daily.class);
        db.createAllTables();
        getCommand("reward").setExecutor(new RewardCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
    }

    void configure() {
        deliver = getConfig().getBoolean("Deliver");
        if (deliver && !setupEconomy()) {
            getLogger().warning("Failed to setup economy");
        }
    }

    @Override
    public void onDisable() {
    }

    boolean setupEconomy() {
        if (economy != null) return true;
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    public RewardBuilder createBuilder() {
        return new RewardBuilder(this);
    }

    static Date getCurrentDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    boolean checkAndSetDaily(UUID uuid, String name) {
        Date today = getCurrentDay();
        Daily daily = db.find(Daily.class).where()
            .eq("uuid", uuid)
            .eq("name", name)
            .findUnique();
        if (daily == null) {
            db.save(new Daily(uuid, name, today));
            return true;
        }
        if (today.equals(daily.getDay())) return false;
        daily.setDay(today);
        db.save(daily);
        return true;
    }

    /**
     * When a player joins, wait a short time and then give them
     * all their rewards, if any.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!deliver) return;
        final Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isValid()) return;
                if (!player.hasPermission(PERM_RECEIVE)) return;
                deliverAll(player);
            }
        }.runTaskLater(this, 20*6);
    }

    public void deliverAll(Player player) {
        if (!player.isValid() || !player.isOnline()) return;
        List<Reward> rewards = db
            .find(Reward.class)
            .where()
            .isNull("delivered")
            .eq("uuid", player.getUniqueId())
            .findList();
        if (rewards.isEmpty()) return;
        for (Reward reward : rewards) {
            // Mark as delivered
            reward.delivered();
            try {
                db.save(reward);
            } catch (OptimisticLockException ole) {
                ole.printStackTrace();
                continue;
            }
            deliver(reward, player);
        }
    }

    void deliver(Reward reward, Player player) {
        // Comment
        player.sendMessage("");
        player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "Reward");
        if (reward.getComment() != null) {
            player.sendMessage(reward.getComment());
        }
        // Money
        if (reward.getMoney() != null && reward.getMoney() > 0.01 && economy != null) {
            player.sendMessage("Money: " + ChatColor.GREEN + economy.format(reward.getMoney()));
            EconomyResponse result = economy.depositPlayer(player, reward.getMoney());
            if (!result.transactionSuccess()) {
                getLogger().warning("Failed giving money reward to " + player.getName() + ": " + result.errorMessage);
            }
        }
        // Exp
        if (reward.getExp() != null && reward.getExp() > 0) {
            player.sendMessage("Exp: " + ChatColor.GREEN + reward.getExp());
            player.giveExp(reward.getExp());
        }
        // Items
        ItemStack[] items = reward.getItemStacks();
        if (items.length > 0) {
            int amount = 0;
            for (ItemStack item : items) if (item.getType() != Material.AIR) amount += item.getAmount();
            for (ItemStack drop : player.getInventory().addItem(items).values()) {
                if (drop.getType() == Material.AIR) continue;
                player.getWorld().dropItem(player.getLocation(), drop).setPickupDelay(0);
            }
            player.sendMessage("Items: " + ChatColor.GREEN + amount);
        }
        player.sendMessage("");
        // Commands
        if (reward.getCommands() != null && !reward.getCommands().isEmpty()) {
            for (Command command : reward.getCommands()) {
                String cmd = command.getCommand();
                cmd = cmd.replace("%player%", player.getName());
                cmd = cmd.replace("%uuid%", player.getUniqueId().toString());
                getLogger().info("Console command: " + cmd);
                getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
            }
        }
        // Log
        getLogger().info("Delivered reward #" + reward.getId() + " to " + player.getName());
        // Event
        getServer().getPluginManager().callEvent(new RewardEvent(player, reward));
    }
}
