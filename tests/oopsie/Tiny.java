import io.github.eisop.oopsie.qual.Sql;
import java.sql.*;

class Tiny {
    // :: error: (assignment.type.incompatible)
    @Sql String s = "dummy";

    void testSimplePreparedStatement() throws SQLException {
        Connection conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");

        // this should work
        @Sql(
                in = {"TIMESTAMP"},
                out = {"INTEGER", "DECIMAL", "VARCHAR"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");

        @Sql(out = {"INTEGER", "DECIMAL", "INTEGER"})
        PreparedStatement ps2 =
                // :: error: (assignment.type.incompatible)
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");

        @Sql(out = {"INTEGER", "DECIMAL", "VARCHAR", "INTEGER"})
        PreparedStatement ps3 =
                // :: error: (assignment.type.incompatible)
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");
    }
}
