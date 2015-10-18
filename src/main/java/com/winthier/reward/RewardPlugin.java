package com.winthier.reward;

import com.winthier.reward.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
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

public class RewardPlugin extends JavaPlugin implements Listener {
    boolean deliver;
    Economy economy;
    public static final String PERM_RECEIVE = "reward.receive";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        configure();
        setupDatabase();
        getCommand("reward").setExecutor(new RewardCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
    }

    void configure() {
        deliver = getConfig().getBoolean("Deliver");
        if (deliver && !setupEconomy()) {
            getLogger().warning("Failed to setup economy");
        }
    }

    void setupDatabase() {
        try {
            for (Class<?> clazz : getDatabaseClasses()) {
                getDatabase().find(clazz).findRowCount();
            }
        } catch (PersistenceException tmp) {
            System.out.println("Installing database for " + getDescription().getName() + " due to first time usage");
            try {
                installDDL();
            } catch (PersistenceException pe) {
                pe.printStackTrace();
            }
        }
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        return Arrays.asList(
            Reward.class,
            Item.class,
            Currency.class,
            Flag.class,
            Command.class
            );
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

    public Builder createBuilder() {
        return new Builder(this);
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
        List<Reward> rewards = getDatabase()
            .find(Reward.class)
            .where()
            .isNull("delivered")
            .or(getDatabase().getExpressionFactory().eq("uuid", player.getUniqueId()),
                getDatabase().getExpressionFactory().eq("name", player.getName()))
            .findList();
        if (rewards.isEmpty()) return;
        for (Reward reward : rewards) {
            // Mark as delivered
            reward.delivered();
            try {
                getDatabase().save(reward);
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
        // Exp
        if (reward.getExp() != null && reward.getExp() > 0) {
            player.sendMessage("Exp: " + ChatColor.GREEN + reward.getExp());
            player.giveExp(reward.getExp());
        }
        // Money
        if (reward.getMoney() != null && reward.getMoney() > 0.01 && economy != null) {
            player.sendMessage("Money: " + ChatColor.GREEN + economy.format(reward.getMoney()));
            EconomyResponse result = economy.depositPlayer(player, reward.getMoney());
            if (!result.transactionSuccess()) {
                getLogger().warning("Failed giving money reward to " + player.getName() + ": " + result.errorMessage);
            }
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
