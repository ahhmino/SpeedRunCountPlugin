package com.ahhmino.speedRunCountPlugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SpeedRunCountPlugin extends JavaPlugin {

    private List<Material> trackedItems = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadTrackedItems();

        // Repeating scoreboard updater
        new BukkitRunnable() {
            @Override
            public void run() {
                int interval = getConfig().getInt("update-interval", 40); // ticks, default 2s
                int totalPlayers = Bukkit.getOnlinePlayers().size();

                // Calculate global totals for each tracked item once
                var globalCounts = calculateGlobalCounts();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player, globalCounts);
                }
            }
        }.runTaskTimer(this, 0L, getConfig().getInt("update-interval", 40));

        getLogger().info("SpeedRunCountPlugin enabled!");
    }

    private void loadTrackedItems() {
        trackedItems.clear();
        List<String> list = getConfig().getStringList("tracked-items");

        for (String name : list) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                trackedItems.add(mat);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material in config: " + name);
            }
        }

        if (trackedItems.isEmpty()) {
            getLogger().warning("No valid tracked items found in config!");
        } else {
            getLogger().info("Tracking items: " + trackedItems);
        }
    }

    private void updateScoreboard(Player player, List<ItemCount> globalCounts) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard board = manager.getNewScoreboard();

        Objective obj = board.registerNewObjective("speedrun", "dummy",
                Component.text("§6§lSpeedrun Counts"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = trackedItems.size() * 2 + 2;

        // Per-player counts
        for (Material mat : trackedItems) {
            int myCount = countItems(player, mat);
            String itemName = mat.name().replace("_", " ").toLowerCase();
            setLine(board, obj, "p_" + mat.name(), String.format("§f%s: §a%d", capitalize(itemName), myCount), line--);
        }

        // Spacer
        setLine(board, obj, "spacer", " ", line--);

        // Global totals
        for (ItemCount count : globalCounts) {
            String itemName = count.material.name().replace("_", " ").toLowerCase();
            setLine(board, obj, "g_" + count.material.name(),
                    String.format("§eAll %s: §b%d", capitalize(itemName), count.total), line--);
        }

        player.setScoreboard(board);
    }

    private List<ItemCount> calculateGlobalCounts() {
        List<ItemCount> counts = new ArrayList<>();
        for (Material mat : trackedItems) {
            int total = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                total += countItems(p, mat);
            }
            counts.add(new ItemCount(mat, total));
        }
        return counts;
    }

    private int countItems(Player player, Material mat) {
        return player.getInventory().all(mat).values().stream()
                .filter(Objects::nonNull)
                .mapToInt(item -> item.getAmount())
                .sum();
    }

    private void setLine(Scoreboard board, Objective obj, String key, String text, int score) {
        Team team = board.getTeam(key);
        if (team == null) team = board.registerNewTeam(key);
        team.prefix(Component.text(text));
        team.addEntry(key);
        obj.getScore(key).setScore(score);
    }

    private String capitalize(String text) {
        if (text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override
    public void onDisable() {
        getLogger().info("SpeedRunCountPlugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("speedruncount")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadTrackedItems();
                sender.sendMessage(Component.text("§aSpeedRunCount config reloaded!"));
                return true;
            }
        }
        return false;
    }

    private static class ItemCount {
        final Material material;
        final int total;

        ItemCount(Material material, int total) {
            this.material = material;
            this.total = total;
        }
    }
}
