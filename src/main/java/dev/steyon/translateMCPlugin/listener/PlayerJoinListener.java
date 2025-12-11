package dev.steyon.translateMCPlugin.listener;

import dev.steyon.translateMCPlugin.TranslateMCPlugin;
import dev.steyon.translateMCPlugin.gui.LanguageGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {
    private final TranslateMCPlugin plugin;

    public PlayerJoinListener(TranslateMCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if GUI on first join is enabled
        boolean showGUIOnFirstJoin = plugin.getConfig().getBoolean("features.gui-on-first-join", true);

        if (!showGUIOnFirstJoin) {
            return;
        }

        // Check if player has already selected a language
        if (!plugin.getTranslationManager().hasPlayerSelectedLanguage(player.getUniqueId())) {
            // Delay by 1 second to ensure player is fully loaded
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Send welcome message
                    String welcomeMsg = plugin.getConfig().getString("messages.first-join-welcome",
                        "&7Please select your preferred language!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', welcomeMsg));

                    // Open language selector GUI
                    LanguageGUI.openLanguageSelector(player, plugin);
                }
            }.runTaskLater(plugin, 20L); // 1 second delay
        }
    }
}
