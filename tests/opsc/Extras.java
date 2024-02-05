import io.github.eisop.opsc.qual.Sql;
import java.sql.*;

class Predicates {

    Connection conn;

    public Predicates() throws SQLException {
        conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    void bangEqual() throws SQLException {
        PreparedStatement ps1 = conn.prepareStatement(
                        "SELECT DISTINCT demographic_no FROM log WHERE id >= ? and action != 'read'");
        ps1.setTimestamp(1, new Timestamp(0));
    }

}