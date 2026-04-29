package me.tyyni.yoChat.yoChatPlugin;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageParseManager {

    private MiniMessage mm;

    public void setupMM() {
        boolean strictMode = ConfigManager.getInstance().getConfig().getBoolean("minimessage-customization.strict-mode", false);
        this.mm = MiniMessage.builder()
                .tags(TagResolver.empty())
                .strict(strictMode)
                .build();
        ConfigManager.getInstance().debug("MiniMessage configured with strictMode=%s", strictMode);
    }

    public TagResolver getPlayerResolver(Player player) {
        TagResolver.Builder resolverBuilder = TagResolver.builder();
        ConfigurationSection section = ConfigManager.getInstance().getConfig().getConfigurationSection("minimessage-customization.allowed-tags");
        List<String> enabledTags = new ArrayList<>();

        if (section != null) {
            for (String tagName : section.getKeys(false)) {
                TagResolver actualResolver = tagMap.get(tagName.toLowerCase());
                if (actualResolver == null) continue;

                Object val = section.get(tagName);

                if (val instanceof Boolean && (Boolean) val) {
                    resolverBuilder.resolver(actualResolver);
                    enabledTags.add(tagName.toLowerCase());
                } else if (val instanceof String && player.hasPermission((String) val)) {
                    resolverBuilder.resolver(actualResolver);
                    enabledTags.add(tagName.toLowerCase() + ":" + val);
                }
            }
        }
        ConfigManager.getInstance().debug("Built MiniMessage resolver for %s with %d enabled tags -> %s",
                player.getName(), enabledTags.size(), enabledTags);
        return resolverBuilder.build();
    }

    public Component parse(Player player, String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        String normalized = adoptLegacy(input);
        ConfigManager.getInstance().debug("Parsing player message for %s: raw='%s' normalized='%s'", player.getName(), input, normalized);
        try {
            return this.mm.deserialize(normalized, getPlayerResolver(player));
        } catch (ParsingException ex) {
            YoChatAPI.getPlugin().getLogger().warning("Failed to parse chat message from " + player.getName() + ": " + ex.getMessage());
            return Component.text(input);
        }
    }

    public Component parseAdmin(String input, TagResolver... additionalResolvers) {
        if (input == null || input.isEmpty()) return Component.empty();

        TagResolver finalResolver = TagResolver.builder()
                .resolver(TagResolver.standard())
                .resolvers(additionalResolvers)
                .build();

        String normalized = adoptLegacy(input);
        ConfigManager.getInstance().debug("Parsing admin message: raw='%s' normalized='%s' extraResolvers=%d", input, normalized, additionalResolvers.length);
        try {
            return this.mm.deserialize(normalized, finalResolver);
        } catch (ParsingException ex) {
            YoChatAPI.getPlugin().getLogger().warning("Failed to parse admin MiniMessage input: " + ex.getMessage());
            return Component.text(input);
        }
    }

    private String adoptLegacy(String input) {
        if (input == null) return "";

        String hexProcessed = input.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");

        return hexProcessed
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }

    private final Map<String, TagResolver> tagMap = new ConcurrentHashMap<>() {{
        put("color", StandardTags.color());
        put("decoration", StandardTags.decorations());
        put("reset", StandardTags.reset());
        put("font", StandardTags.font());
        put("shadow", StandardTags.shadowColor());
        put("newline", StandardTags.newline());
        put("gradient", StandardTags.gradient());
        put("rainbow", StandardTags.rainbow());
        put("transition", StandardTags.transition());
        put("pride", StandardTags.pride());
        put("hover", StandardTags.hoverEvent());
        put("click", StandardTags.clickEvent());
        put("insertion", StandardTags.insertion());
        put("keybind", StandardTags.keybind());
        put("translatable", StandardTags.translatable());
        put("translatablefallback", StandardTags.translatableFallback());
        put("selector", StandardTags.selector());
        put("score", StandardTags.score());
        put("nbt", StandardTags.nbt());
        put("sprite", StandardTags.sprite());
        put("head", StandardTags.sequentialHead());
        put("sequentialhead", StandardTags.sequentialHead());
    }};
}
