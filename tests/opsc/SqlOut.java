package opsc;

import io.github.eisop.opsc.qual.Sql;
import java.math.BigDecimal;
import java.sql.*;

public class SqlOut {

    Connection conn;

    public SqlOut() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    void getResultCorrectly() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");

        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));

        @Sql(out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        // this should work
        ResultSet rs = ps.executeQuery();

        // this should work
        rs.getInt(1);
        rs.getBigDecimal(2);
        rs.getString(3);
    }

    void resultSetWrongAnnotation() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");

        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));

        @Sql(out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) INTEGER"})
        // :: error: (assignment.type.incompatible)
        ResultSet rs = ps.executeQuery();
    }

    void resultSetWrongColumnType() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");

        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));

        @Sql(out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        ResultSet rs = ps.executeQuery();

        // :: error: (column.type.incompatible)
        rs.getInt(3);
    }

    void getOutOfBounds() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement("SELECT InvoiceId, Total, BillingCountry FROM Invoice");

        @Sql(out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        ResultSet rs = ps.executeQuery();

        // :: error: (column.index.out.of.bounds)
        rs.getBigDecimal(9);
    }

    void noParameters() throws SQLException {
        // This should work
        PreparedStatement ps =
                conn.prepareStatement("SELECT InvoiceId, Total, BillingCountry FROM Invoice");

        @Sql(out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        ResultSet rs = ps.executeQuery();
        rs.getInt(1);
    }
}
