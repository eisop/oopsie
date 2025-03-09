package opsc;

import io.github.eisop.opsc.qual.Sql;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StringConstValues {

    Connection conn;

    final String stmt =
            "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?";

    public StringConstValues() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    // Working test from Tiny.java
    void simpleLiteral() throws SQLException {
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");
    }

    void concatenation() throws SQLException {
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry "
                                + "FROM Invoice WHERE InvoiceDate > ?");
    }

    void stringFromLocalVariable() throws SQLException {
        String sql = "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?";
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 = conn.prepareStatement(sql);
    }

    void stringFromConstantField() throws SQLException {
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 = conn.prepareStatement(stmt);
    }

    void stringfromLocalVariableConcatenated() throws SQLException {
        String sql =
                "SELECT InvoiceId, Total, BillingCountry " + "FROM Invoice WHERE InvoiceDate > ?";

        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 = conn.prepareStatement(sql);
    }

    void stringfromLocalVariableConcatenated2() throws SQLException {
        // Only works with -AnonNullStringsConcatenation=true option for Constant Value Checker
        String sql = "SELECT InvoiceId, Total, BillingCountry ";
        sql += "FROM Invoice WHERE InvoiceDate > ?";

        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 = conn.prepareStatement(sql);
    }

    void stringFromTextBlock() throws SQLException {
        String sql =
                """
                SELECT InvoiceId, Total, BillingCountry
                FROM Invoice WHERE InvoiceDate > ?
                """;
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        PreparedStatement ps1 = conn.prepareStatement(sql);
    }

    void negativeTest() throws SQLException {
        String sql =
                """
                SELECT InvoiceId, Total, BillingCountry
                FROM Invoice WHERE InvoiceDate > ?
                """;
        @Sql(
                in = {"Timestamp"},
                out = {
                    "@NonNull INTEGER",
                    "@NonNull DECIMAL",
                    "@Nullable @MaxLength(40) VARCHAR",
                    "@NonNull INTEGER"
                })
        PreparedStatement ps1 =
                // :: error: (assignment.type.incompatible)
                conn.prepareStatement(sql);
    }

    void notExtractable(boolean filter) throws SQLException {
        String sql = "SELECT * FROM INVOICE";
        if (filter) {
            sql += " adding something?";
        }
        PreparedStatement ps = conn.prepareStatement(sql);
    }

    public void mixInsertAndUpdate(boolean newInstance) throws SQLException {
        String stmt = "";
        if (newInstance) {
            stmt = "INSERT INTO genre (genreid, name) VALUES (?, ?)";
        } else {
            stmt = "UPDATE genre SET genreid = ? WHERE name = ?";
        }

        PreparedStatement ps = conn.prepareStatement(stmt);
        ps.setInt(1, 1);
        ps.setString(2, "scary industrial hip hop");
    }
}
