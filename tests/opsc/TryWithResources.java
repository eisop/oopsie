import java.sql.*;

public class TryWithResources {

    Connection conn;

    public TryWithResources() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void testTryWithResources() throws SQLException {
        try (PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?")) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            // :: error: (parameter.type.incompatible)
            ps.setString(1, "should be a timestamp");
        }
    }
}
