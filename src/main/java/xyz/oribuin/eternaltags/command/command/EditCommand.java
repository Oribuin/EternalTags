package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.*;
import xyz.oribuin.eternaltags.command.command.edit.EditCategoryCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditDescriptionCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditIconCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditNameCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditOrderCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditPermissionCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditTagCommand;

public class EditCommand extends BaseRoseCommand {

    public EditCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("edit")
                .descriptionKey("command-edit-description")
                .permission("eternaltags.edit")
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder().optionalSub(
                new EditCategoryCommand(this.rosePlugin),
                new EditDescriptionCommand(this.rosePlugin),
                new EditIconCommand(this.rosePlugin),
                new EditNameCommand(this.rosePlugin),
                new EditOrderCommand(this.rosePlugin),
                new EditPermissionCommand(this.rosePlugin),
                new EditTagCommand(this.rosePlugin)
        );
    }
}
