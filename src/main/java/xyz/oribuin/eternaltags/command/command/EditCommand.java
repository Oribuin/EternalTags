package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.*;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
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

    @RoseExecutable
    public void execute(CommandContext context, BaseRoseCommand command) {
        // Has no functionality, just used to pass the tag to the subcommand
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("edit")
                .permission("eternaltags.edit")
                .descriptionKey("command-edit-description")
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder().requiredSub(
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
