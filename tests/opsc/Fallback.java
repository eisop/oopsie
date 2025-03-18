import java.math.BigDecimal;
import java.sql.*;

public class Fallback {

    Connection conn;

    public Fallback() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void keywords() throws SQLException {
        PreparedStatement ps =
                // :: warning: (determine.in.type.failed.first.try)
                // :: warning: (determine.out.type.failed.first.try)
                conn.prepareStatement("SELECT * FROM Invoice WHERE 1 != 2");
        ResultSet rs = ps.executeQuery();
        int invoiceId = rs.getInt("InvoiceId");
        int customerId = rs.getInt("CustomerId");
        Timestamp invoiceDate = rs.getTimestamp("InvoiceDate");
        String billingAddress = rs.getString("BillingAddress");
        String billingCity = rs.getString("BillingCity");
        String billingState = rs.getString("BillingState");
        String billingCountry = rs.getString("BillingCountry");
        String billingPostalCode = rs.getString("BillingPostalCode");
        BigDecimal total = rs.getBigDecimal("Total");

        // :: error: (column.name.not.found)
        int invoiceId2 = rs.getInt("InvoiceId2");

        // :: error: (column.type.incompatible)
        int billingPostalCode2 = rs.getInt("BillingPostalCode");
    }
}
