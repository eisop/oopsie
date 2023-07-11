package opsc;

import io.github.eisop.opsc.qual.Sql;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlIn {

    void simpleIn() throws SQLException {
        Connection conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");

        // this should work
        @Sql(
                in = {"Double"},
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
    }
}
