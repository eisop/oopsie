import java.sql.*;

class Unsupported {

    Connection conn;

    public Unsupported() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void deitsch() throws SQLException {
        PreparedStatement ps =
                // :: error: (determine.in.type.failed.final)
                conn.prepareStatement("ZEIG MA * AUS Invoice WO Total < 244.331");
    }
}
