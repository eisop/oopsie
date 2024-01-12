import java.sql.*;

class Issues {

    Connection conn;

    public Issues() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    void semicolon() throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice WHERE Total > ?;");
    }
}
