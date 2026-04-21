package opsc;

import java.sql.*;

public class IndexIncrement {

    Connection conn;

    public IndexIncrement() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void comic() throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM employee WHERE fax = ?");
        int ctr = 1;
        ps.setString(ctr++, "555-1234");
        // :: error: (parameter.index.out.of.bounds)
        ps.setString(ctr++, "555-5678");
    }
}
