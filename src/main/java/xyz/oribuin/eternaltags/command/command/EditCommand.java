package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.RoseSubCommand;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import xyz.oribuin.eternaltags.command.command.edit.EditCategoryCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditIconCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditDescriptionCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditNameCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditOrderCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditPermissionCommand;
import xyz.oribuin.eternaltags.command.command.edit.EditTagCommand;
import xyz.oribuin.eternaltags.obj.Tag;

public class EditCommand extends RoseCommand {

    public EditCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent,
                EditCategoryCommand.class,
                EditDescriptionCommand.class,
                EditIconCommand.class,
                EditNameCommand.class,
                EditOrderCommand.class,
                EditPermissionCommand.class,
                EditTagCommand.class);
    }

    @RoseExecutable
    public void execute(CommandContext context, RoseSubCommand command) {
        // Has no functionality, just used to pass the tag to the subcommand
    }

    @Override
    protected String getDefaultName() {
        return "edit";
    }

    @Override
    public String getDescriptionKey() {
        return "command-edit-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.edit";
    }
}
