import io.github.eisop.opsc.qual.Sql;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;

class MethodAnnotations {

    Connection conn;

    public MethodAnnotations() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public @Sql(
            in = {"Timestamp"},
            out = {"@NonNull Integer", "@NonNull BigDecimal", "@Nullable @MaxLength(40) String"})
    PreparedStatement getPreparedStatement() throws SQLException {
        return conn.prepareStatement(
                "SELECT InvoiceId, Total, BillingCountry FROM Invoice WHERE InvoiceDate > ?");
    }

    public @Sql(
            out = {"@NonNull Integer", "@NonNull BigDecimal", "@Nullable @MaxLength(40) String"})
    ResultSet getResultSet() throws SQLException {
        return getPreparedStatement().executeQuery();
    }

    public void test() throws SQLException {
        ResultSet rs = getResultSet();
        // :: error: (column.type.incompatible)
        rs.getInt(3);
    }

    public int getAge(@Sql(out = {"DATE dob"}) ResultSet rs) throws SQLException {
        Date dob = rs.getDate("dob");
        return Period.between(dob.toLocalDate(), LocalDate.now()).getYears();
    }

    public void testGetAge() throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT Date '1990-01-01' AS dob, 'anotherColumn'");
        ResultSet rs = ps.executeQuery();
        int age = getAge(rs);
    }
}
