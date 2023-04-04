package xyz.oribuin.eternaltags.database.migration;

import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.database.DatabaseConnector;
import dev.rosewood.rosegarden.database.SQLiteConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class _3_ModifyTagDataItems extends DataMigration {

    public _3_ModifyTagDataItems() {
        super(3);
    }

    @Override
    public void migrate(DatabaseConnector connector, Connection connection, String tablePrefix) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            if (connector instanceof SQLiteConnector) {

                // thank you chatgpt for this whole section

                // Rename old table to a temporary table
                statement.addBatch("ALTER TABLE " + tablePrefix + "tag_data RENAME TO " + tablePrefix + "tag_data_old");

                // Create new table with modified columns
                statement.addBatch("CREATE TABLE " + tablePrefix + "tag_data ("
                        + "`tagId` VARCHAR(256) NOT NULL,"
                        + "`name` TEXT NOT NULL,"
                        + "`tag` TEXT NOT NULL,"
                        + "`permission` TEXT,"
                        + "`description` TEXT NOT NULL,"
                        + "`order` INTEGER NOT NULL,"
                        + "`icon` VARBINARY(2456),"
                        + "`category` TEXT, "
                        + "PRIMARY KEY (tagId))");

                // Copy data from old table to new table
                statement.addBatch("INSERT INTO " + tablePrefix + "tag_data (tagId, `name`, `tag`, `permission`, `description`, `order`) "
                        + "SELECT `tagId`, `name`, `tag`, `permission`, `description`, `order` FROM " + tablePrefix + "tag_data_old "
                        + "WHERE EXISTS (SELECT 1 FROM " + tablePrefix + "tag_data_old)");

            } else {
                // Modify columns to the desired types and nullability
                // Add category text column
                statement.addBatch("ALTER TABLE " + tablePrefix + "tag_data ADD COLUMN `category` TEXT NULL");
                statement.addBatch("ALTER TABLE " + tablePrefix + "tag_data MODIFY COLUMN `permission` TEXT NULL");
                statement.addBatch("UPDATE " + tablePrefix + "tag_data SET `icon` = NULL");
                statement.addBatch("ALTER TABLE " + tablePrefix + "tag_data MODIFY COLUMN `icon` VARBINARY(2456) NULL");
            }

            // Execute all batched statements
            statement.executeBatch();
        }
    }

}
