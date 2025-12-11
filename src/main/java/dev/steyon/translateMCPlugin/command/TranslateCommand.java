package dev.steyon.translateMCPlugin.command;

import dev.steyon.translateMCPlugin.TranslateMCPlugin;
import dev.steyon.translateMCPlugin.gui.LanguageGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TranslateCommand implements CommandExecutor, TabCompleter {
    private final TranslateMCPlugin plugin;

    public TranslateCommand(TranslateMCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Handle reload subcommand
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("translatemc.admin.reload")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            plugin.reloadConfig();
            plugin.getTranslationManager().loadTranslations();
            player.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
            return true;
        }

        // If no arguments, show GUI
        if (args.length == 0) {
            LanguageGUI.openLanguageSelector(player, plugin);
            return true;
        }

        // Handle language selection by code
        String languageCode = args[0].toLowerCase();
        if (plugin.getTranslationManager().isLanguageAvailable(languageCode)) {
            plugin.getTranslationManager().setPlayerLanguage(player, languageCode);

            String languageName = plugin.getTranslationManager().getLanguage(languageCode).getName();
            String changeMsg = plugin.getConfig().getString("messages.language-changed",
                "&aYour language has been changed to &e{language}&a!");
            changeMsg = changeMsg.replace("{language}", languageName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', changeMsg));
        } else {
            player.sendMessage(ChatColor.RED + "Language not found: " + languageCode);
            player.sendMessage(ChatColor.YELLOW + "Available languages: " +
                String.join(", ", plugin.getTranslationManager().getAvailableLanguageCodes()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Add language codes
            completions.addAll(plugin.getTranslationManager().getAvailableLanguageCodes());

            // Add reload for admins
            if (sender.hasPermission("translatemc.admin.reload")) {
                completions.add("reload");
            }

            // Filter by current input
            String input = args[0].toLowerCase();
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        }

        return completions;
    }
}
