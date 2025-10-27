package com.ahhmino.speedRunCountPlugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

public final class SpeedRunCountPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("SpeedRunCountPlugin enabled!");

        // Update every 2 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                int totalPearls = totalAcrossOnline(Material.ENDER_PEARL);
                int totalRods   = totalAcrossOnline(Material.BLAZE_ROD);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player, totalPearls, totalRods);
                }
            }
        }.runTaskTimer(this, 0L, 40L);
    }

    private void updateScoreboard(Player player, int totalPearls, int totalRods) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective(
                "itemcount", "dummy", Component.text("§6§lInventory Stats")
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int myPearls = countItems(player, Material.ENDER_PEARL);
        int myRods   = countItems(player, Material.BLAZE_ROD);

        // Prevent duplicate line conflicts
        objective.getScore(("§fEnder Pearls: §a" + myPearls) + "§r§0").setScore(5);
        objective.getScore(("§fBlaze Rods:  §a" + myRods)   + "§r§1").setScore(4);
        objective.getScore(" ").setScore(3);
        objective.getScore(("§eAll Pearls:  §b" + totalPearls) + "§r§2").setScore(2);
        objective.getScore(("§eAll Rods:    §b" + totalRods)   + "§r§3").setScore(1);

        player.setScoreboard(board);
    }

    private int countItems(Player player, Material mat) {
        return player.getInventory().all(mat).values().stream()
                .mapToInt(item -> item.getAmount())
                .sum();
    }

    private int totalAcrossOnline(Material mat) {
        int total = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            total += countItems(p, mat);
        }
        return total;
    }

    @Override
    public void onDisable() {
        getLogger().info("SpeedRunCountPlugin disabled.");
    }
}
