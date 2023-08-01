package xyz.oribuin.eternaltags.database.migration;

import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class _4_DeleteOldData extends DataMigration {

    public _4_DeleteOldData() {
        super(4);
    }

    @Override
    public void migrate(DatabaseConnector connector, Connection connection, String tablePrefix) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.addBatch("DROP TABLE IF EXISTS " + tablePrefix + "tag_data_old");
            statement.executeBatch();
        }
    }

}
