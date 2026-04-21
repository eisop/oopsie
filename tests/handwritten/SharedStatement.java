import io.github.eisop.opsc.qual.Sql;
import java.sql.*;

class SharedStatement {

    Connection conn;

    private PreparedStatement preparedStmt;

    public SharedStatement() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void sharedStmt1() throws SQLException {
        preparedStmt = conn.prepareStatement("SELECT genreid FROM genre WHERE name = ?");

        // :: error: (parameter.type.incompatible)
        preparedStmt.setInt(1, 234);

        @Sql ResultSet rs = preparedStmt.executeQuery(); // (assignment.type.incompatible)

        // :: error: (column.type.incompatible)
        rs.getString(1);
    }

    /**
     * Working example exquivalent to {@link #sharedStmt1()} that uses a local variable statement
     * for comparison.
     */
    public void localStmt1() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT genreid FROM genre WHERE name = ?");

        // :: error: (parameter.type.incompatible)
        stmt.setInt(1, 234);

        @Sql ResultSet rs = stmt.executeQuery(); // assignment works

        // :: error: (column.type.incompatible)
        rs.getString(1);
    }

    /**
     * Here, the PreparedStatement is reinitialized with a new SQL string (parameter and result
     * types are swapped).
     */
    public void sharedStmt3() throws SQLException {
        preparedStmt = conn.prepareStatement("SELECT name FROM genre WHERE genreid = ?");

        // :: error: (parameter.type.incompatible)
        preparedStmt.setString(1, "1");
        // :: error: (parameter.type.incompatible)
        preparedStmt.setString(1, "1");
    }
}
