package opsc;

import io.github.eisop.opsc.qual.Sql;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlIn {

    Connection conn;

    public SqlIn() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    void inAnnotation() throws SQLException {
        // this should work
        @Sql(
                in = {"@NonNull DECIMAL"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
    }

    void wrongAnnotation() throws SQLException {
        @Sql(
                in = {"VARCHAR"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 =
                // :: error: (assignment.type.incompatible)
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
    }

    void setParameterCorrectly() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
        // this should work
        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));
    }

    void setParamOutOfBounds() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
        // :: error: (parameter.index.out.of.bounds)
        ps.setDouble(2, 244.331);
    }

    void noParametersButSet() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement("SELECT InvoiceId, Total, BillingCountry FROM Invoice");

        // :: error: (parameter.index.out.of.bounds)
        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));
    }

    void setParamWrongType() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
        // :: error: (parameter.type.incompatible)
        ps.setString(1, "244");
    }

    void constantValue() throws SQLException {
        // :: warning: (determine.in.type.failed.first.try)
        // :: warning: (determine.out.type.failed.first.try)
        PreparedStatement ps = conn.prepareStatement("SELECT ?");

        ps.setString(1, "A constant");
    }
}
