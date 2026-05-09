import io.github.eisop.oopsie.qual.Sql;
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
     * This example uses the @SqlUnsupported annotation mechanic. This annotation was introduced to
     * suppress setter/getter accesses to unsupported (= unparsable or not extractable) SQL
     * statements as for these, a warning is issued on statement declaration. We do, however, want
     * to warn about/log the usage of getters and setters on "nonlocal" statements and ResultSets,
     * which are not assigned with @SqlUnsupported (see the corresponding test cases in {@code
     * SqlUnsupportedAnnotation.java}).
     */
    public void sharedStmt2(String preparedSQL, String... param) throws SQLException {
        // Statement string not extractable, so @SqlUnsupported should be assigned
        // This suppresses warnings about getter/setter accesses for this statement
        // :: warning: (statement.string.retrieval.failed)
        preparedStmt = conn.prepareStatement(preparedSQL); // has @SqlUnsupported anno
        PreparedStatement localPreparedStmt =
                // :: warning: (statement.string.retrieval.failed)
                conn.prepareStatement(preparedSQL); // has @SqlUnsupported anno
        for (int i = 0; i < param.length; i++) {
            preparedStmt.setString((i + 1), param[i]);
            localPreparedStmt.setString((i + 1), param[i]);
        }
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
