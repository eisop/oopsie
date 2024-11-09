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
        String sql = "SELECT InvoiceId, Total, BillingCountry ";
        sql += "FROM Invoice WHERE InvoiceDate > ?"; // ConstValueChecker treats `sql` as
        // potentially null in concatenation

        @Sql(
                in = {"Timestamp"},
                out = {"@NonNull INTEGER", "@NonNull DECIMAL", "@Nullable @MaxLength(40) VARCHAR"})
        // False positive as ConstValueChecker is unable to track the value of `sql` in
        // concatenation
        // :: warning: (statement.multiple.string.values.continuing)
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

    void ternary(Integer programId, int roleId, Timestamp startDate, Timestamp endDate)
            throws SQLException {
        // From oscar
        String sql = "select count(distinct demographic_no) from casemgmt_note where reporter_caisi_role=? and observation_date>=? and observation_date<?"+(programId==null?"":" and program_no=?");
        // :: warning: (statement.multiple.string.values.continuing)
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, String.valueOf(roleId));
        ps.setTimestamp(2, new Timestamp(startDate.getTime()));
        ps.setTimestamp(3, new Timestamp(endDate.getTime()));
        if (programId!=null) ps.setString(4, String.valueOf(programId));
    }

}
