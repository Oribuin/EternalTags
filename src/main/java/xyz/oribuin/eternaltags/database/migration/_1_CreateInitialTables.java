package xyz.oribuin.eternaltags.database.migration;

import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.SQLException;

public class _1_CreateInitialTables extends DataMigration {

    public _1_CreateInitialTables() {
        super(1);
    }

    @Override
    public void migrate(DatabaseConnector connector, Connection connection, String tablePrefix) throws SQLException {
        connection.createStatement().execute("CREATE TABLE " + tablePrefix + "tags (" +
                "player VARCHAR(36), " +
                "tagID TEXT, " +
                "PRIMARY KEY(player))");

        connection.createStatement().executeUpdate("CREATE TABLE " + tablePrefix + "favourites (" +
                "player VARCHAR(36), " +
                "tagID TEXT)");

    }
}
