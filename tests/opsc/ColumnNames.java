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
        int billingPostalCode = rs.getInt("BillingPostalCode"); // actually varchar
    }

    void namedColumnsWithoutCalcite(Connection conn) throws SQLException {
        // force fallback JDBCSchemaInfo, with "SELECT ?" which is not supported by Calcite

        // sadly this doesn't work because the Postgres driver doesn't provide the column names
        // :: warning: (determine.in.type.failed.first.try)
        // :: warning: (determine.out.type.failed.first.try)
        PreparedStatement ps = conn.prepareStatement("SELECT ?; SELECT * FROM Invoice");
        ResultSet rs = ps.executeQuery();

        // [OPSC] FALSE POSITIVE
        // :: error: (column.name.not.found)
        int invoiceId = rs.getInt("InvoiceId"); // this should work

        // [OPSC] FALSE POSITIVE (should be column.type.incompatible)
        // :: error: (column.name.not.found)
        int billingPostalCode = rs.getInt("BillingPostalCode"); // actually varchar
    }

    void namedColumnNoLiteral(Connection conn) throws SQLException {
        String columnName = "InvoiceId";
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice");
        ResultSet rs = ps.executeQuery();

        int invoiceId = rs.getInt(columnName);
        // :: error: (column.type.incompatible)
        Date invoiceDate = rs.getDate(columnName);
    }
}
