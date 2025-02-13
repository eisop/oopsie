import io.github.eisop.opsc.qual.Sql;
import java.sql.*;

class SqlUnsupported {

    Connection conn;

    public SqlUnsupported() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void unsupportedStatement() throws SQLException {
        Statement stmt = conn.createStatement();
        // :: error: (determine.in.type.failed.final)
        ResultSet rs = stmt.executeQuery("This is not a valid SQL statement");

        // No further warnings for unsupported SQL statements (@SqlUnsupported)
        rs.getInt(1);
    }

    public void unsupportedPreparedStatement() throws SQLException {
        // :: error: (determine.in.type.failed.final)
        PreparedStatement ps = conn.prepareStatement("This is not a valid SQL statement");

        // No further warnings for unsupported SQL statements (@SqlUnsupported)
        ps.setInt(1, 1);
        ResultSet rs = ps.executeQuery();
        rs.getInt(1);
    }

    public void nonlocalPreparedStatement(PreparedStatement ps) throws SQLException {
        // :: warning: (nonlocal.prepared.statement)
        ps.setInt(1, 1);
        ResultSet rs = ps.executeQuery();
        // :: warning: (nonlocal.result.set)
        rs.getInt(1);
    }

    public void nonlocalResultSet(ResultSet rs) throws SQLException {
        // :: warning: (nonlocal.result.set)
        rs.getInt(1);
    }

    public void annotatedResultSet(@Sql(out = {"INTEGER"}) ResultSet rs) throws SQLException {
        rs.getInt(1);

        // :: error: (column.type.incompatible)
        rs.getBoolean(1);
    }
}
