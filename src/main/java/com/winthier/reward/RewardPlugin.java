package com.winthier.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
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
    private boolean deliver;
    private Economy economy;
    public static final String PERM_RECEIVE = "reward.receive";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        configure();
        setupDatabase();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void configure() {
        deliver = getConfig().getBoolean("Deliver");
        if (deliver && !setupEconomy()) {
            getLogger().warning("Failed to setup economy");
        }
    }

    private void setupDatabase() {
        try {
            for (Class<?> clazz : getDatabaseClasses()) {
                getDatabase().find(clazz).findRowCount();
            }
        } catch (PersistenceException ex) {
            System.out.println("Installing database for " + getDescription().getName() + " due to first time usage");
            installDDL();
        }
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Reward.class);
        list.add(Item.class);
        return list;
    }

    @Override
    public void onDisable() {
    }

    private boolean setupEconomy() {
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        try {
            if (false) {
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                configure();
                sender.sendMessage("" + ChatColor.YELLOW + "Configuration reloaded");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("deliver")) {
                if (!(sender instanceof Player)) throw new CommandException("Player expected");
                String idArg = args[1];
                Player player = (Player)sender;
                Integer id = null;
                try { id = Integer.parseInt(idArg); } catch (NumberFormatException nfe) {}
                if (id == null || id <= 0) throw new CommandException("Positive number expected: " + idArg);
                Reward reward = getDatabase().find(Reward.class, id);
                if (reward == null) throw new CommandException("Reward not found: #" + id);
                deliver(reward, player);
                sender.sendMessage("" + ChatColor.YELLOW + "Rewards delivered to you");
            } else if (args.length >= 1 && args[0].equalsIgnoreCase("test")) {
                if (!(sender instanceof Player)) throw new CommandException("Player expected");
                // comment
                String comment = null;
                if (args.length >= 2) {
                    StringBuilder sb = new StringBuilder(args[1]);
                    for (int i = 2; i < args.length; ++i) sb.append(" ").append(args[i]);
                    comment = sb.toString();
                }
                // items
                List<ItemStack> items = new ArrayList<>();
                Player player = (Player)sender;
                for (ItemStack item : player.getInventory()) {
                    if (item != null && item.getType() != Material.AIR) items.add(item);
                }
                Builder builder = createBuilder();
                builder.uuid(player.getUniqueId());
                if (comment != null) builder.comment(comment);
                if (!items.isEmpty()) builder.items(items);
                builder.exp(player.getTotalExperience());
                builder.money(12.34);
                Reward reward = builder.store();
                sender.sendMessage("" + ChatColor.YELLOW + "Reward stored as #" + reward.getId());
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
                if (!(sender instanceof Player)) throw new CommandException("Player expected");
                Player player = (Player)sender;
                String name = null;
                Double money = null;
                Integer exp = null;
                StringBuilder comment = new StringBuilder();
                for (int i = 1; i < args.length; ++i) {
                    String arg = args[i];
                    if (arg.startsWith("p:")) {
                        name = arg.substring(2);
                    } else if (arg.startsWith("m:")) {
                        String moneyArg = arg.substring(2);
                        try {
                            money = Double.parseDouble(moneyArg);
                        } catch (NumberFormatException nfe) {
                            throw new CommandException("Money amount expected: " + moneyArg);
                        }
                    } else if (arg.startsWith("x:")) {
                        String expArg = arg.substring(2);
                        try {
                            exp = Integer.parseInt(expArg);
                        } catch (NumberFormatException nfe) {
                            throw new CommandException("XP amount expected: " + expArg);
                        }
                    } else {
                        comment.append(" ").append(arg);
                    }
                }
                if (name == null) throw new CommandException("Player name required");
                if (money != null && money < 0.0) throw new CommandException("Money can't be negative");
                if (exp != null && exp < 0) throw new CommandException("Exp can't be negative");
                OfflinePlayer recipient = getServer().getOfflinePlayer(name);
                if (recipient == null || !recipient.hasPlayedBefore()) throw new CommandException("Player not found: " + name);
                Builder builder = createBuilder();
                builder.uuid(recipient.getUniqueId());
                builder.name(recipient.getName());
                builder.comment(comment.toString().trim());
                if (exp != null) builder.exp(exp);
                if (money != null) builder.money(money);
                for (ItemStack item : player.getInventory()) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    builder.item(item.clone());
                }
                builder.store();
                sender.sendMessage("" + ChatColor.AQUA + "Reward for " + recipient.getName() + " stored.");
            } else {
                return false;
            }
        } catch (CommandException ce) {
            sender.sendMessage("" + ChatColor.RED + ce.getMessage());
        }
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
                if (!player.hasPermission(PERM_RECEIVE)) return;
                deliverAll(player);
            }
        }.runTaskLater(this, 20L);
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

    private void deliver(Reward reward, Player player) {
        // Comment
        String comment = reward.getComment();
        if (comment != null) {
            player.sendMessage("" + ChatColor.AQUA + "You have earned a reward: " + ChatColor.RESET + comment);
        } else {
            player.sendMessage("" + ChatColor.AQUA + "You have earned a reward.");
        }
        // Exp
        if (reward.getExp() != null) player.giveExp(reward.getExp());
        // Money
        if (reward.getMoney() != null && economy != null) {
            EconomyResponse result = economy.depositPlayer(player.getName(), reward.getMoney()); // TODO remove getName() for new Vault version.
            if (!result.transactionSuccess()) {
                getLogger().warning("Failed giving money reward to " + player.getName() + ": " + result.errorMessage);
            }
        }
        // Items
        for (ItemStack drop : player.getInventory().addItem(reward.getItemStacks()).values()) {
            if (drop.getType() == Material.AIR) continue;
            player.getWorld().dropItem(player.getLocation(), drop);
        }
        // Log
        getLogger().info("Delivered reward #" + reward.getId() + " to " + player.getName());
    }
}
