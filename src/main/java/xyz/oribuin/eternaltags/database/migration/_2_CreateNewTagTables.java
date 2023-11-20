package xyz.oribuin.eternaltags.database.migration;

import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class _2_CreateNewTagTables extends DataMigration {

    public _2_CreateNewTagTables() {
        super(2);
    }

    @Override
    public void migrate(DatabaseConnector connector, Connection connection, String tablePrefix) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "tag_data (" +
                                    "tagId VARCHAR(256), " +
                                    "`name` TEXT, " +
                                    "tag TEXT, " +
                                    "permission TEXT, " +
                                    "description TEXT, " +
                                    "`order` INTEGER DEFAULT 0, " +
                                    "`icon` TEXT NULL, " +
                                    "PRIMARY KEY(tagId))");
        }
    }

}
