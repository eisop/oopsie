import java.sql.*;

class SharedStatement {

    Connection conn;

    private PreparedStatement preparedStmt;

    public SharedStatement() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    /**
     *
     *
     * <ul>
     *   <li>Diagnostic as expected for setter
     *   <li>Unexpected diagnostic found for the getter call: (nonlocal.result.set)
     *   <li>Expected diagnostic not found for the getter call: (column.type.incompatible)
     * </ul>
     *
     * This means that the ResultSet does not seem to be assigned with @Sql.
     */
    public void sharedStmt1() throws SQLException {
        preparedStmt = conn.prepareStatement("SELECT genreid FROM genre WHERE name = ?");

        // :: error: (parameter.type.incompatible)
        preparedStmt.setInt(1, 234); // True positive (@Sql is present)

        ResultSet rs = preparedStmt.executeQuery();

        // :: error: (column.type.incompatible)
        rs.getString(1); // (nonlocal.result.set) instead of (column.type.incompatible)
    }

    /**
     * This example uses the @SqlUnsupported annotation mechanic. This annotation was introduced to
     * suppress setter/getter accesses to unsupported (= unparsable or not extractable) SQL
     * statements as for these, a warning is issued on statement declaration. We do, however, want
     * to warn about/log the usage of getters and setters on "nonlocal" statements and ResultSets,
     * which are not assigned with @SqlUnsupported (see the corresponding test cases in {@code
     * SqlUnsupportedAnnotation.java}).
     *
     * <p>In this method, both PreparedStatements should be assigned with @SqlUnsupported, as the
     * value of the parameter {@code preparedSQL} is unknown. However {@code preparedStmt} one is
     * not assigned with @SqlUnsupported, so a warning is issued on the setter call.
     */
    public void sharedStmt2(String preparedSQL, String... param) throws SQLException {
        // Statement string not extractable, so @SqlUnsupported should be assigned
        // This suppresses warnings about getter/setter accesses for this statement
        preparedStmt = conn.prepareStatement(preparedSQL); // should have @SqlUnsupported anno
        PreparedStatement localPreparedStmt =
                conn.prepareStatement(preparedSQL); // has @SqlUnsupported anno
        for (int i = 0; i < param.length; i++) {
            preparedStmt.setString((i + 1), param[i]); // FP: warning: (nonlocal.prepared.statement)
            localPreparedStmt.setString((i + 1), param[i]); // no FP
        }
    }

    /**
     * Here, the PreparedStatement is reinitialized with a new SQL string (parameter and result
     * types are swapped). The first setter seems to be lead to the correct error, however, the
     * second setter leads to (nonlocal.prepared.statement), so the @Sql annotation seems to be lost
     * after the first setter.
     */
    public void sharedStmt3() throws SQLException {
        preparedStmt = conn.prepareStatement("SELECT name FROM genre WHERE genreid = ?");

        // :: error: (parameter.type.incompatible)
        preparedStmt.setString(1, "1"); // TP
        // :: error: (parameter.type.incompatible)
        preparedStmt.setString(1, "1"); // actually: (nonlocal.prepared.statement)
    }
}
