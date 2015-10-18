package com.winthier.reward;

import com.winthier.reward.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
class RewardEvent extends Event {
    private static HandlerList handlers = new HandlerList();
    final Player player;
    final String comment;
    final int exp;
    final double money;
    final Map<String, Integer> currencies = new HashMap<>();
    final Map<String, Integer> flags = new HashMap<>();
    final List<String> commands = new ArrayList<>();

    RewardEvent(Player player, Reward reward) {
        this.player = player;
        this.comment = reward.getComment();
        this.exp = reward.getExp() == null ? 0 : reward.getExp();
        this.money = reward.getMoney() == null ? 0 : reward.getMoney();
        for (Currency currency : reward.getCurrencies()) this.currencies.put(currency.getName(), currency.getAmount());
        for (Flag flag : reward.getFlags()) this.flags.put(flag.getName(), flag.getValue());
        for (Command command : reward.getCommands()) this.commands.add(command.getCommand());
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
