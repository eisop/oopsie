import java.sql.*;

class NonPreparedStatement {

    Connection conn;

    public NonPreparedStatement() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void createStatement() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT InvoiceId, Total FROM Invoice");
        rs.getInt(1);
        // :: error: (column.type.incompatible)
        rs.getInt(2);
    }
}
