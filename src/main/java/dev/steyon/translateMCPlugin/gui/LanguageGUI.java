package dev.steyon.translateMCPlugin.gui;

import dev.steyon.translateMCPlugin.TranslateMCPlugin;
import dev.steyon.translateMCPlugin.api.TranslationAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class LanguageGUI implements Listener {
    private static final String GUI_TITLE_PREFIX = ChatColor.translateAlternateColorCodes('&', "");

    /**
     * Opens the language selector GUI for a player
     */
    public static void openLanguageSelector(Player player, TranslateMCPlugin plugin) {
        String title = plugin.getConfig().getString("gui.title", "&6&lSelect Your Language");
        title = ChatColor.translateAlternateColorCodes('&', title);

        int size = plugin.getConfig().getInt("gui.size", 27);
        if (size % 9 != 0 || size > 54) {
            size = 27; // Default to 3 rows if invalid
        }

        Inventory gui = Bukkit.createInventory(null, size, title);

        List<TranslationAPI.Language> languages = plugin.getTranslationManager().getAvailableLanguages();
        String currentLanguage = plugin.getTranslationManager().getPlayerLanguage(player);

        int slot = 0;
        for (TranslationAPI.Language language : languages) {
            if (slot >= size) break;

            ItemStack item = createLanguageItem(language, currentLanguage, plugin);
            gui.setItem(slot, item);
            slot++;
        }

        player.openInventory(gui);
    }

    /**
     * Creates an item for a language - FIXED VERSION
     */
    private static ItemStack createLanguageItem(TranslationAPI.Language language, String currentLanguage, TranslateMCPlugin plugin) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        boolean isCurrent = language.getCode().equalsIgnoreCase(currentLanguage);

        // Get the skull meta ONCE and keep using it for everything
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        if (skullMeta != null) {
            // Set texture first if available
            if (language.getMinecraftHead() != null && !language.getMinecraftHead().isEmpty()) {
                try {
                    setSkullTexture(skullMeta, language.getMinecraftHead());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to set skull texture for language " + language.getCode() + ": " + e.getMessage());
                }
            }

            // Now set display name and lore on the SAME meta object
            String displayName = ChatColor.GOLD + ChatColor.BOLD.toString() + language.getName();
            if (isCurrent) {
                displayName = ChatColor.GREEN + ChatColor.BOLD.toString() + language.getName() + " " + ChatColor.YELLOW + "✓";
            }
            skullMeta.setDisplayName(displayName);

            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Code: " + ChatColor.WHITE + language.getCode());

            if (language.isSource()) {
                lore.add(ChatColor.YELLOW + "Default Language");
            }

            if (isCurrent) {
                lore.add("");
                lore.add(ChatColor.GREEN + "Currently Selected");
            } else {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to select");
            }

            skullMeta.setLore(lore);
            item.setItemMeta(skullMeta);  // Set the meta only ONCE at the end
        }

        return item;
    }

    /**
     * Sets the skull texture using a Base64 encoded texture value
     * This method uses Paper/Spigot's PlayerProfile API for modern versions
     */
    private static void setSkullTexture(SkullMeta skullMeta, String base64Texture) {
        try {
            // Use Paper's PlayerProfile API (works on Paper 1.18+)
            java.util.UUID uuid = java.util.UUID.randomUUID();
            org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile(uuid);
            org.bukkit.profile.PlayerTextures textures = profile.getTextures();

            // Decode the Base64 texture to get the texture URL
            String decoded = new String(java.util.Base64.getDecoder().decode(base64Texture));
            System.out.println("[DEBUG] Decoded texture JSON: " + decoded);

            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(decoded).getAsJsonObject();
            String textureUrl = json.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url")
                    .getAsString();

            // Fix: Ensure HTTPS is used (required for modern Minecraft versions)
            if (textureUrl.startsWith("http://")) {
                textureUrl = textureUrl.replace("http://", "https://");
            }

            System.out.println("[DEBUG] Texture URL (fixed): " + textureUrl);
            textures.setSkin(new java.net.URL(textureUrl));
            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);
            System.out.println("[DEBUG] Successfully set skull texture");
        } catch (Exception e) {
            System.out.println("[DEBUG] Failed to set texture with PlayerProfile: " + e.getMessage());
            e.printStackTrace();
            // Fallback: Try using the NBT method for older versions
            try {
                setSkullTextureReflection(skullMeta, base64Texture);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to set skull texture", ex);
            }
        }
    }

    /**
     * Fallback method using reflection for older Spigot versions
     */
    private static void setSkullTextureReflection(SkullMeta skullMeta, String base64Texture) throws Exception {
        Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

        java.lang.reflect.Constructor<?> profileConstructor = profileClass.getConstructor(java.util.UUID.class, String.class);
        Object profile = profileConstructor.newInstance(java.util.UUID.randomUUID(), null);

        java.lang.reflect.Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
        Object property = propertyConstructor.newInstance("textures", base64Texture);

        java.lang.reflect.Method getProperties = profile.getClass().getMethod("getProperties");
        Object properties = getProperties.invoke(profile);

        java.lang.reflect.Method put = properties.getClass().getMethod("put", Object.class, Object.class);
        put.invoke(properties, "textures", property);

        java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        profileField.set(skullMeta, profile);
    }

    /**
     * Handles GUI clicks
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if it's our GUI
        TranslateMCPlugin plugin = TranslateMCPlugin.getInstance();
        String guiTitle = plugin.getConfig().getString("gui.title", "&6&lSelect Your Language");
        guiTitle = ChatColor.translateAlternateColorCodes('&', guiTitle);

        if (!title.equals(guiTitle)) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || meta.getLore() == null) return;

        // Extract language code from lore
        for (String loreLine : meta.getLore()) {
            if (loreLine.contains("Code: ")) {
                String code = ChatColor.stripColor(loreLine).replace("Code: ", "").trim();

                // Set player language
                plugin.getTranslationManager().setPlayerLanguage(player, code);

                // Send confirmation message
                String languageName = plugin.getTranslationManager().getLanguage(code).getName();
                String changeMsg = plugin.getConfig().getString("messages.language-changed",
                        "&aYour language has been changed to &e{language}&a!");
                changeMsg = changeMsg.replace("{language}", languageName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', changeMsg));

                // Close GUI
                player.closeInventory();
                break;
            }
        }
    }

    /**
     * Extracts the base title without color codes for comparison
     */
    private static String getBaseTitle(String title) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', title));
    }
}