package xyz.oribuin.eternaltags.command.disabled;

import com.google.common.collect.Lists;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.Optional;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.TagsUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ListCommand extends RoseCommand {

    public ListCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, @Optional Integer page) {
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final Player player = (Player) context.getSender();

        final List<Tag> tags = manager.getPlayersTags(player);
        Map<Integer, List<Tag>> pages = new HashMap<>();

        AtomicInteger integer = new AtomicInteger(0);
        Lists.partition(tags, 10).forEach(smallList -> pages.put(integer.incrementAndGet(), smallList));

        int activePage = page != null ? page : 1;
        List<Tag> pageTags = pages.get(activePage);

        final StringPlaceholders pagePlaceholders = StringPlaceholders.builder("page", activePage)
                .addPlaceholder("total", Math.min(activePage, pages.size()))
                .build();

        // TextComponent removes hex colours and i dont wanna deal with that yet
        final String format = locale.getLocaleMessage("command-list-format");
        locale.sendMessage(player, "command-list-header", pagePlaceholders);

        pageTags.forEach(tag -> {
            final TextComponent comp = new TextComponent(HexUtils.colorify(this.getTagPlaceholders(tag).apply(format)));
            final ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tags set " + tag.getId());
            comp.setClickEvent(clickEvent);

            player.spigot().sendMessage(comp);
        });

        ComponentBuilder builder = new ComponentBuilder();
        TextComponent leftArrow = new TextComponent(HexUtils.colorify(locale.getLocaleMessage("command-list-left-arrow", pagePlaceholders)));
        leftArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tags list " + Math.max(1, activePage - 1)));

        TextComponent separator = new TextComponent(HexUtils.colorify(locale.getLocaleMessage("command-list-separator", pagePlaceholders)));

        TextComponent rightArrow = new TextComponent(HexUtils.colorify(locale.getLocaleMessage("command-list-right-arrow", pagePlaceholders)));
        rightArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tags list " + Math.min(pages.size(), activePage + 1)));

        player.spigot().sendMessage(
                builder.append(leftArrow)
                        .append(separator)
                        .append(rightArrow)
                        .create()
        );

    }

    private StringPlaceholders getTagPlaceholders(Tag tag) {
        final StringPlaceholders.Builder builder = StringPlaceholders.builder();
        builder.addPlaceholder("tag", HexUtils.colorify(tag.getTag()));
        builder.addPlaceholder("id", tag.getId());
        builder.addPlaceholder("name", tag.getName());
        builder.addPlaceholder("description", TagsUtil.formatList(tag.getDescription()));
        return builder.build();
    }

    @Override
    protected String getDefaultName() {
        return "list";
    }

    @Override
    public String getDescriptionKey() {
        return "command-list-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.list";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
