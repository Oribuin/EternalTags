package dev.oribuin.eternaltags.database.migration;

import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class _1_CreateInitialTables extends DataMigration {

    public _1_CreateInitialTables() {
        super(1);
    }

    @Override
    public void migrate(DatabaseConnector connector, Connection connection, String tablePrefix) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "tags (" +
                                    "player VARCHAR(36), " +
                                    "tagID TEXT, " +
                                    "PRIMARY KEY(player))");
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "favourites (" +
                                    "player VARCHAR(36), " +
                                    "tagID TEXT)");
        }
    }

}
