import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IntelliJ {

    Connection conn;

    public IntelliJ() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    /** No IntelliJ warnings */
    public void mixInsertAndUpdate(boolean newInstance) throws SQLException {
        String stmt = "";
        if (newInstance) {
            stmt = "INSERT INTO genre (genreid, name) VALUES (?, ?)";
        } else {
            stmt = "UPDATE genre SET genreid = ? WHERE name = ?";
        }

        PreparedStatement ps = conn.prepareStatement(stmt);
        ps.setInt(1, 1);
        ps.setString(2, "scary industrial hip hop");

        // :: error: (parameter.type.incompatible)
        ps.setInt(2, 1);
        // :: error: (parameter.index.out.of.bounds)
        ps.setString(3, "gnx");
    }

    /** No IntelliJ warnings */
    public void reassignment() throws SQLException {
        String stmt = "";
        stmt = "INSERT INTO genre (genreid, name) VALUES (?, ?)";

        PreparedStatement ps = conn.prepareStatement(stmt);
        ps.setInt(1, 1);
        ps.setString(2, "scary industrial hip hop");

        // :: error: (parameter.type.incompatible)
        ps.setInt(2, 1);
        // :: error: (parameter.index.out.of.bounds)
        ps.setString(3, "gnx");
    }

    public void normal() throws SQLException {
        String stmt = "INSERT INTO genre (genreid, name) VALUES (?, ?)";
        PreparedStatement ps = conn.prepareStatement(stmt);

        ps.setInt(1, 1);
        ps.setString(2, "scary industrial hip hop");

        // :: error: (parameter.type.incompatible)
        ps.setInt(2, 1);
        // :: error: (parameter.index.out.of.bounds)
        // IntelliJ warning: Cannot resolve query parameter '3'
        ps.setString(3, "gnx");
    }
}
