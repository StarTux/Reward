package com.winthier.reward;

import com.winthier.playercache.PlayerCache;
import com.winthier.reward.sql.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
class RewardCommand implements CommandExecutor {
    final RewardPlugin plugin;
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        try {
            if (false) {
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.configure();
                sender.sendMessage("" + ChatColor.YELLOW + "Configuration reloaded");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("deliver")) {
                if (!(sender instanceof Player)) throw new CommandException("Player expected");
                String idArg = args[1];
                Player player = (Player)sender;
                Integer id = null;
                try { id = Integer.parseInt(idArg); } catch (NumberFormatException nfe) {}
                if (id == null || id <= 0) throw new CommandException("Positive number expected: " + idArg);
                Reward reward = plugin.getDatabase().find(Reward.class, id);
                if (reward == null) throw new CommandException("Reward not found: #" + id);
                plugin.deliver(reward, player);
                sender.sendMessage("" + ChatColor.YELLOW + "Rewards delivered to you");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("deliverall")) {
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) throw new CommandException("Player not found: " + args[1]);
                plugin.deliverAll(target);
                sender.sendMessage("" + ChatColor.YELLOW + "All rewards of " + target.getName() + " delivered.");
            } else if (args.length >= 1 && args[0].equalsIgnoreCase("test")) {
                if (!(sender instanceof Player)) throw new CommandException("Player expected");
                RewardBuilder builder = plugin.createBuilder();
                builder.player((Player)sender);
                YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "test.yml"));
                for (String key : config.getKeys(false)) {
                    sender.sendMessage("" + ChatColor.YELLOW + "Loading section " + key);
                    builder.config(config.getConfigurationSection(key));
                }
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
                UUID uuid = PlayerCache.uuidForName(name);
                if (uuid == null) throw new CommandException("Player not found: " + name);
                name = PlayerCache.nameForUuid(uuid);
                RewardBuilder builder = plugin.createBuilder();
                builder.uuid(uuid);
                builder.name(name);
                builder.comment(comment.toString().trim());
                if (exp != null) builder.exp(exp);
                if (money != null) builder.money(money);
                for (ItemStack item : player.getInventory()) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    builder.item(item.clone());
                }
                builder.store();
                sender.sendMessage("" + ChatColor.AQUA + "Reward for " + name + " stored.");
            } else {
                return false;
            }
        } catch (CommandException ce) {
            sender.sendMessage("" + ChatColor.RED + ce.getMessage());
        }
        return true;
    }
}
