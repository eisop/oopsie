import java.math.BigDecimal;
import java.sql.*;

class SelectAll {

    Connection conn;

    public SelectAll() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    void selectAllCorrect() throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice WHERE Total > ?");

        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));

        ResultSet rs = ps.executeQuery();

        // this should work
        rs.getInt(1);
        rs.getBigDecimal(9);
    }

    void selectAllOutOfBounds() throws SQLException {
        // 9 columns
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice WHERE Total > ?");

        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));

        ResultSet rs = ps.executeQuery();

        // this should work
        rs.getInt(1); // invoiceid (integer)
        rs.getBigDecimal(9); // total (numeric)
        // :: error: (column.index.out.of.bounds)
        rs.getInt(10);
    }

    void selectAllWrongType() throws SQLException {
        // 9 columns
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice WHERE Total > ?");

        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));

        ResultSet rs = ps.executeQuery();

        // :: error: (column.type.incompatible)
        rs.getInt(3); // invoicedate (timestamp)
    }
}
