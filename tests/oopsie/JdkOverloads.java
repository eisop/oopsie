import java.sql.*;

public class JdkOverloads {

    Connection conn;

    public JdkOverloads() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void execute() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("SELECT total FROM Invoice");
        ResultSet rs = stmt.getResultSet();
        rs.getBigDecimal("total");
        // :: error: (column.index.out.of.bounds)
        rs.getString(2);
    }
}
