package xyz.oribuin.eternaltags.command.command.edit;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.command.argument.TagsArgumentHandlers;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class EditIconCommand extends BaseRoseCommand {

    public EditIconCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag, Boolean remove) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        Player player = (Player) context.getSender();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        boolean shouldRemove = remove != null && remove;

        if (item.getType().isAir() || !item.getType().isItem() && !shouldRemove) {
            locale.sendMessage(context.getSender(), "command-edit-invalid-item");
            return;
        }

        if (shouldRemove) {
            item = null;
        }

        tag.setIcon(item); // If the material is null, then the player is holding the item.
        tag.setHandIcon(item != null); // If the material is null, then the player is holding the item.

        manager.saveTag(tag);
        manager.updateActiveTag(tag);

        final StringPlaceholders placeholders = StringPlaceholders.builder()
                .add("tag", manager.getDisplayTag(tag, player))
                .add("option", "icon")
                .add("id", tag.getId())
                .add("name", tag.getName())
                .add("value", (item == null ? "None" : item.getType().name().toLowerCase()))
                .build();

        locale.sendMessage(context.getSender(), "command-edit-edited", placeholders);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("icon")
                .playerOnly(true)
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("tag", TagsArgumentHandlers.TAG)
                .optional("remove", ArgumentHandlers.BOOLEAN)
                .build();
    }
}
