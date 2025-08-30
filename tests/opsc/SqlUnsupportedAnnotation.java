import io.github.eisop.opsc.qual.Sql;
import io.github.eisop.opsc.qual.SqlUnsupported;
import java.sql.*;

class SqlUnsupportedAnnotation {

    Connection conn;

    public SqlUnsupportedAnnotation() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void unsupportedStatement() throws SQLException {
        Statement stmt = conn.createStatement();
        // :: warning: (statement.skipped)
        ResultSet rs = stmt.executeQuery("This is not a valid SQL statement");

        // No further warnings for unsupported SQL statements (@SqlUnsupported)
        rs.getInt(1);
    }

    public void unsupportedPreparedStatement() throws SQLException {
        // :: warning: (statement.skipped)
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

    public void stmtGetResultSet() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("SELECT name FROM genre");
        ResultSet rs = stmt.getResultSet();
        rs.getString(1);

        // :: error: (column.type.incompatible)
        rs.getInt(1);
    }

    public void stmtGetResultSetUnsupported() throws SQLException {
        Statement stmt = conn.createStatement();
        // :: warning: (statement.skipped)
        @SqlUnsupported ResultSet rs = stmt.executeQuery("This is not a valid SQL statement");

        // No further warnings for unsupported SQL statements (@SqlUnsupported)
        rs.getInt("column_name");
    }

    public void ignorePrepareCall() throws SQLException {
        CallableStatement stmt = conn.prepareCall("{call get_genre(?)}");
        stmt.setInt(245, 1);
        ResultSet rs = stmt.executeQuery();
        rs.getString(4444);
    }

    public void ignoreGetGeneratedKeys() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT INTO genre (genreid, name) VALUES (1, 'scary industrial hip hop')");

        PreparedStatement ps =
                conn.prepareStatement("INSERT INTO genre (genreid, name) VALUES (?, ?)");
        ps.setInt(1, 1);
        ps.setString(2, "scary industrial hip hop");

        ResultSet rs1 = stmt.getGeneratedKeys();
        ResultSet rs2 = ps.getGeneratedKeys();

        // No warnings for getGeneratedKeys
        rs1.getInt(4535);
        rs2.getInt(1337);
    }
}
