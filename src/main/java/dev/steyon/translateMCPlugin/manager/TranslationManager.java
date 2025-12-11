package dev.steyon.translateMCPlugin.manager;

import dev.steyon.translateMCPlugin.TranslateMCPlugin;
import dev.steyon.translateMCPlugin.api.TranslationAPI;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationManager {
    private final TranslateMCPlugin plugin;
    private final TranslationAPI api;
    private final String defaultLanguage;
    private final long cacheDuration;

    // Cache for translations
    private Map<String, Map<String, TranslationAPI.Translation>> translations;
    private List<TranslationAPI.Language> availableLanguages;
    private long lastCacheUpdate;

    // Player language preferences (UUID -> language code)
    private final Map<UUID, String> playerLanguages;

    public TranslationManager(TranslateMCPlugin plugin) {
        this.plugin = plugin;
        this.defaultLanguage = plugin.getConfig().getString("language.default", "en");
        this.cacheDuration = 300 * 1000L; // 5 minutes cache

        String apiUrl = plugin.getConfig().getString("api.url");
        String apiToken = plugin.getConfig().getString("api.token");
        this.api = new TranslationAPI(apiUrl, apiToken, plugin.getLogger());

        this.translations = new ConcurrentHashMap<>();
        this.availableLanguages = new ArrayList<>();
        this.playerLanguages = new ConcurrentHashMap<>();
        this.lastCacheUpdate = 0;

        // Initial load
        loadTranslations();

        // Start auto-refresh task
        startAutoRefreshTask();
    }

    /**
     * Loads translations from the API
     */
    public void loadTranslations() {
        plugin.getLogger().info("Loading translations from API...");

        TranslationAPI.APIResponse response = api.fetchTranslations();
        this.translations = response.getTranslations();
        this.availableLanguages = response.getLanguages();
        this.lastCacheUpdate = System.currentTimeMillis();

        plugin.getLogger().info("Loaded " + translations.size() + " translation keys");
        plugin.getLogger().info("Available languages: " + getAvailableLanguageCodes());
    }

    /**
     * Gets a translation for a specific key and language
     * Falls back to English if translation not found
     */
    public String getTranslation(String key, String languageCode) {
        // Check if cache needs refresh
        if (System.currentTimeMillis() - lastCacheUpdate > cacheDuration) {
            loadTranslations();
        }

        Map<String, TranslationAPI.Translation> keyTranslations = translations.get(key);
        if (keyTranslations == null) {
            return key; // Return key itself if not found
        }

        // Try to get translation in requested language
        TranslationAPI.Translation translation = keyTranslations.get(languageCode);
        if (translation != null && !translation.getValue().isEmpty()) {
            return translation.getValue();
        }

        // Fallback to default language (English)
        translation = keyTranslations.get(defaultLanguage);
        if (translation != null && !translation.getValue().isEmpty()) {
            return translation.getValue();
        }

        // If still not found, return the key itself
        return key;
    }

    /**
     * Gets a translation for a player based on their language preference
     */
    public String getTranslation(Player player, String key) {
        String language = getPlayerLanguage(player);
        return getTranslation(key, language);
    }

    /**
     * Gets a translation for a player UUID based on their language preference
     */
    public String getTranslation(UUID playerUUID, String key) {
        String language = playerLanguages.getOrDefault(playerUUID, defaultLanguage);
        return getTranslation(key, language);
    }

    /**
     * Sets a player's language preference
     */
    public void setPlayerLanguage(Player player, String languageCode) {
        setPlayerLanguage(player.getUniqueId(), languageCode);
    }

    /**
     * Sets a player's language preference by UUID
     */
    public void setPlayerLanguage(UUID playerUUID, String languageCode) {
        if (isLanguageAvailable(languageCode)) {
            playerLanguages.put(playerUUID, languageCode);
            // TODO: Save to database or file for persistence
        }
    }

    /**
     * Gets a player's language preference
     */
    public String getPlayerLanguage(Player player) {
        return getPlayerLanguage(player.getUniqueId());
    }

    /**
     * Gets a player's language preference by UUID
     */
    public String getPlayerLanguage(UUID playerUUID) {
        return playerLanguages.getOrDefault(playerUUID, defaultLanguage);
    }

    /**
     * Gets all available languages from the API
     */
    public List<TranslationAPI.Language> getAvailableLanguages() {
        return new ArrayList<>(availableLanguages);
    }

    /**
     * Gets all available language codes
     */
    public List<String> getAvailableLanguageCodes() {
        List<String> codes = new ArrayList<>();
        for (TranslationAPI.Language language : availableLanguages) {
            codes.add(language.getCode());
        }
        return codes;
    }

    /**
     * Checks if a language code is available
     */
    public boolean isLanguageAvailable(String languageCode) {
        for (TranslationAPI.Language language : availableLanguages) {
            if (language.getCode().equalsIgnoreCase(languageCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a Language object by code
     */
    public TranslationAPI.Language getLanguage(String languageCode) {
        for (TranslationAPI.Language language : availableLanguages) {
            if (language.getCode().equalsIgnoreCase(languageCode)) {
                return language;
            }
        }
        return null;
    }

    /**
     * Gets the default language code
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Clears the player language cache
     */
    public void clearPlayerLanguages() {
        playerLanguages.clear();
    }

    /**
     * Starts an automatic refresh task for translations
     */
    private void startAutoRefreshTask() {
        long refreshInterval = cacheDuration / 50; // Convert milliseconds to ticks (50ms per tick)

        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastCacheUpdate > cacheDuration) {
                    plugin.getLogger().info("Auto-refreshing translations cache...");
                    loadTranslations();
                }
            }
        }.runTaskTimerAsynchronously(plugin, refreshInterval, refreshInterval);
    }

    /**
     * Checks if a player has selected a language before
     */
    public boolean hasPlayerSelectedLanguage(UUID playerUUID) {
        return playerLanguages.containsKey(playerUUID);
    }

    /**
     * Gets all translation keys
     */
    public Set<String> getAllKeys() {
        return translations.keySet();
    }
}
