import java.math.BigDecimal;
import java.sql.*;

class ColumnNames {

    void namedColumns(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice");
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
    }

    void namedColumnWrongType(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice");
        ResultSet rs = ps.executeQuery();

        // :: error: (column.type.incompatible)
        int billingPostalCode = rs.getInt("BillingPostalCode");
    }

    // todo test with JDBCSchemaInfo (maybe "SELECT ?; SELECT * FROM Invoice"?)
}
