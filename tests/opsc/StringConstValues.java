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
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");
    }

    void concatenation() throws SQLException {
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT InvoiceId, Total, BillingCountry "
                                + "FROM Invoice WHERE InvoiceDate > ?");
    }

    void stringFromLocalVariable() throws SQLException {
        String sql = "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?";
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
        PreparedStatement ps1 = conn.prepareStatement(sql);
    }

    void stringFromConstantField() throws SQLException {
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
        PreparedStatement ps1 = conn.prepareStatement(stmt);
    }

    void stringfromLocalVariableConcatenated() throws SQLException {
        String sql =
                "SELECT InvoiceId, Total, BillingCountry " + "FROM Invoice WHERE InvoiceDate > ?";

        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
        PreparedStatement ps1 = conn.prepareStatement(sql);
    }

    //    void stringfromLocalVariableConcatenated2() throws SQLException {
    //        String sql = "SELECT InvoiceId, Total, BillingCountry ";
    //        sql += "FROM Invoice WHERE InvoiceDate > ?";
    //
    //        @Sql(
    //                in = {"Timestamp"},
    //                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40)
    // String"})
    //        PreparedStatement ps1 = conn.prepareStatement(sql);
    //    }

    void stringFromTextBlock() throws SQLException {
        String sql =
                """
                SELECT InvoiceId, Total, BillingCountry
                FROM Invoice WHERE InvoiceDate > ?
                """;
        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull Integer", "@NonNull Double", "@Nullable @MaxLength(40) String"})
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
                    "@NonNull Integer",
                    "@NonNull Double",
                    "@Nullable @MaxLength(40) String",
                    "@NonNull Integer"
                })
        PreparedStatement ps1 =
                // :: error: (assignment.type.incompatible)
                conn.prepareStatement(sql);
    }
}
