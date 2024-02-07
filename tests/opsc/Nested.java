import java.math.BigDecimal;
import java.sql.*;

class Nested {

    Connection conn;

    public Nested() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void whereInSelect() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        """
                SELECT * FROM Invoice WHERE Total IN (SELECT Total FROM Invoice WHERE Total > ?)
                """);
        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));
    }
}
