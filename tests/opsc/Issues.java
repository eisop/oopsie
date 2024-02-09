import io.github.eisop.opsc.qual.Sql;
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

    void overload() throws SQLException {
        @Sql PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice");

        @Sql
        PreparedStatement ps2 =
                conn.prepareStatement("SELECT * FROM Invoice", new String[] {"oluwa", "tikz"});

        @Sql(
                in = {"@NonNull BigDecimal"},
                out = {"@NonNull Integer"})
        PreparedStatement ps3 =
                conn.prepareStatement("SELECT CustomerId FROM Invoice WHERE Total > ?", 1, 2, 3);
    }
}
