package xyz.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import xyz.oribuin.eternaltags.command.impl.edit.EditCategoryCommand;
import xyz.oribuin.eternaltags.command.impl.edit.EditDescriptionCommand;
import xyz.oribuin.eternaltags.command.impl.edit.EditIconCommand;
import xyz.oribuin.eternaltags.command.impl.edit.EditNameCommand;
import xyz.oribuin.eternaltags.command.impl.edit.EditOrderCommand;
import xyz.oribuin.eternaltags.command.impl.edit.EditPermissionCommand;
import xyz.oribuin.eternaltags.command.impl.edit.EditTagCommand;

public class EditCommand extends BaseRoseCommand {

    public EditCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        // Has no functionality, just used to pass the tag to the subcommand
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("edit")
                .descriptionKey("command-edit-description")
                .permission("eternaltags.edit")
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
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
