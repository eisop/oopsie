import io.github.eisop.oopsie.qual.Sql;
import java.sql.*;

class Issues {

    Connection conn;

    public Issues() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

//    NOT IMPLEMENTED FOR JAVA 8
//    void semicolon() throws SQLException {
//        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice WHERE Total > ?;");
//    }

    void overload() throws SQLException {
        @Sql PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice");

        @Sql
        PreparedStatement ps2 =
                conn.prepareStatement("SELECT * FROM Invoice", new String[] {"oluwa", "tikz"});

        // TODO should work but Calcite is calling the type "DECIMAL" instead of "NUMERIC"
        @Sql(
                in = {"NUMERIC"},
                out = {"INTEGER"})
        PreparedStatement ps3 =
                // :: error: (assignment.type.incompatible)
                conn.prepareStatement("SELECT CustomerId FROM Invoice WHERE Total > ?", 1, 2, 3);

        @Sql(
                in = {"DECIMAL"},
                out = {"INTEGER"})
        PreparedStatement ps4 =
                conn.prepareStatement("SELECT CustomerId FROM Invoice WHERE Total > ?", 1, 2, 3);
    }
}
