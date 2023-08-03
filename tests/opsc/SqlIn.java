package opsc;

import io.github.eisop.opsc.qual.Sql;
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
                in = {"Double"}, // todo test with @NonNull
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
    }

    void wrongAnnotation() throws SQLException {
        @Sql(
                in = {"String"},
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
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
        ps.setDouble(1, 244.331);
    }

    void setParamOutOfBounds() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
        // :: error: (parameter.index.outOfBounds)
        ps.setDouble(2, 244.331);
    }

    void setParamWrongType() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE Total > ?");
        // :: error: (parameter.type.incompatible)
        ps.setString(1, "244");
    }
}
