package dev.steyon.translateMCPlugin;

import dev.steyon.translateMCPlugin.command.TranslateCommand;
import dev.steyon.translateMCPlugin.gui.LanguageGUI;
import dev.steyon.translateMCPlugin.listener.PlayerJoinListener;
import dev.steyon.translateMCPlugin.manager.TranslationManager;
import dev.steyon.translateMCPlugin.placeholder.PlaceholderAPIExpansion;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TranslateMCPlugin extends JavaPlugin {
    private static TranslateMCPlugin instance;
    private TranslationManager translationManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize Translation Manager
        getLogger().info("Initializing Translation Manager...");
        translationManager = new TranslationManager(this);

        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("Registering PlaceholderAPI expansion...");
            new PlaceholderAPIExpansion(this).register();
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        // Register command
        PluginCommand command = getCommand("translate");
        if (command != null) {
            TranslateCommand translateCommand = new TranslateCommand(this);
            command.setExecutor(translateCommand);
            command.setTabCompleter(translateCommand);
            getLogger().info("Registered command: /translate");
        } else {
            getLogger().warning("Could not register command: /translate");
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new LanguageGUI(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getLogger().info("TranslateMC Plugin has been enabled!");
        getLogger().info("Available languages: " + String.join(", ", translationManager.getAvailableLanguageCodes()));
    }

    @Override
    public void onDisable() {
        getLogger().info("TranslateMC Plugin has been disabled!");
    }

    /**
     * Gets the plugin instance
     */
    public static TranslateMCPlugin getInstance() {
        return instance;
    }

    /**
     * Gets the translation manager
     */
    public TranslationManager getTranslationManager() {
        return translationManager;
    }
}
