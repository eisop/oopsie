import java.sql.*;

import io.github.eisop.opsc.qual.Sql;

class IntConstValues {

    Connection conn;

    public IntConstValues() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void incrementation() throws SQLException {
        PreparedStatement ps1 =
                conn.prepareStatement(
                        "SELECT * FROM Invoice WHERE InvoiceId > ? and Total > ?");

        int ctr = 1;
        ps1.setInt(ctr++, 1);
        // :: error: (parameter.type.incompatible)
        ps1.setString(ctr++, "1"); // Total needs to be a BigDecimal

    }

}