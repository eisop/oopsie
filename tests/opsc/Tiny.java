import io.github.eisop.opsc.qual.Sql;
import java.sql.*;

class Tiny {
    // :: error: (assignment.type.incompatible)
    @Sql String s = "dummy";

    void testSimplePreparedStatement() throws SQLException {
        Connection conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");

        // this should work
        @Sql(out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");

        @Sql(out = {"@NonNull Integer", "@NonNull Double", "@Nullable Integer"})
        // :: error: (assignment.type.incompatible)
        PreparedStatement ps2 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");

        @Sql(
                out = {
                    "@NonNull Integer",
                    "@NonNull Double",
                    "@Nullable @MaxLength(40) String",
                    "@NonNull Integer"
                })
        // :: error: (assignment.type.incompatible)
        PreparedStatement ps3 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");
    }
}
