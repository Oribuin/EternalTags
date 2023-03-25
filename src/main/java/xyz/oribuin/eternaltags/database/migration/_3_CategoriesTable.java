package xyz.oribuin.eternaltags.database.migration;

import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.database.DatabaseConnector;
import dev.rosewood.rosegarden.database.SQLiteConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class _3_CategoriesTable extends DataMigration {

    public _3_CategoriesTable() {
        super(3);
    }

    @Override
    public void migrate(DatabaseConnector connector, Connection connection, String tablePrefix) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "categoryData (" +
                    "categoryID VARCHAR(256), " +
                    "`displayName` TEXT, " +
                    "tags TEXT, " +
                    "`order` INTEGER DEFAULT -1, " +
                    "`icon` VARBINARY(2456) NULL, " +
                    "PRIMARY KEY(categoryID))");

            if (connector instanceof SQLiteConnector) {
                // drop old icon column and add new one

                statement.addBatch("ALTER TABLE " + tablePrefix + "tag_data RENAME TO " + tablePrefix + "tag_data_old");
                statement.addBatch("CREATE TABLE IF NOT EXISTS " + tablePrefix + "tag_data (" +
                        "tagId VARCHAR(256), " +
                        "`name` TEXT, " +
                        "tag TEXT, " +
                        "permission TEXT, " +
                        "description TEXT, " +
                        "`order` INTEGER DEFAULT 0, " +
                        "`icon` VARBINARY(2456) NULL, " +
                        "PRIMARY KEY(tagId))");

                statement.addBatch("INSERT INTO " + tablePrefix + "tag_data (tagId, `name`, tag, permission, description, `order`) SELECT tagId, `name`, tag, permission, description, `order` FROM " + tablePrefix + "tag_data_old");
                statement.addBatch("DROP TABLE " + tablePrefix + "tag_data_old");
            } else {
                statement.addBatch("ALTER TABLE " + tablePrefix + "tag_data MODIFY COLUMN `icon` VARBINARY(2456) NULL");
            }

            statement.executeBatch();
        }
    }
}
