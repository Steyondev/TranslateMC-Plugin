package dev.steyon.translateMCPlugin.placeholder;

import dev.steyon.translateMCPlugin.TranslateMCPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {
    private final TranslateMCPlugin plugin;

    public PlaceholderAPIExpansion(TranslateMCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Steyon Development";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "langs";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // Required to not unregister on reload
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * Placeholder format: %langs_<key>%
     * Example: %langs_test.get% returns the translation for "test.get" in the player's language
     *
     * Special placeholders:
     * - %langs_player_language% returns the player's current language code
     * - %langs_player_language_name% returns the player's current language name
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Special placeholder for player's current language code
        if (params.equalsIgnoreCase("player_language")) {
            return plugin.getTranslationManager().getPlayerLanguage(player);
        }

        // Special placeholder for player's current language name
        if (params.equalsIgnoreCase("player_language_name")) {
            String langCode = plugin.getTranslationManager().getPlayerLanguage(player);
            var language = plugin.getTranslationManager().getLanguage(langCode);
            return language != null ? language.getName() : langCode;
        }

        // Default: treat params as translation key
        return plugin.getTranslationManager().getTranslation(player, params);
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            // For offline players, use default language
            return plugin.getTranslationManager().getTranslation(params,
                plugin.getTranslationManager().getDefaultLanguage());
        }

        return onPlaceholderRequest(player.getPlayer(), params);
    }
}
