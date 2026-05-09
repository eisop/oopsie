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

    public void whereEqualsSelect() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        """
                SELECT * FROM Invoice WHERE Total = (SELECT Total FROM Invoice WHERE Total > ?)
                """);
        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));
    }

    public void andEqualsSelect() throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement(
                        """
                SELECT * FROM Invoice WHERE CustomerId = ? AND Total = (SELECT Total FROM Invoice WHERE Total > ?)
                """);
        ps.setInt(1, 1);
        ps.setBigDecimal(2, BigDecimal.valueOf(244.331));
    }
}
