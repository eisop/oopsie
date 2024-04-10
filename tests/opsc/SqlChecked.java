import io.github.eisop.opsc.qual.*;

import java.math.BigDecimal;
import java.sql.*;

class SqlChecked {

    Connection conn;

    public SqlChecked() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    void selectAllCorrect() throws SQLException {
        @SqlCheckedNegative
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice WHERE Total > ?");

        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));

        @SqlCheckedNegative
        ResultSet rs = ps.executeQuery();

        // this should work
        rs.getInt(1);
        rs.getBigDecimal(9);
    }

    void selectAllOutOfBounds() throws SQLException {
        // 9 columns
        @SqlCheckedNegative
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM Invoice WHERE Total > ?");

        ps.setBigDecimal(1, BigDecimal.valueOf(244.331));

        ResultSet rs = ps.executeQuery();

        // this should work
        rs.getInt(1); // invoiceid (integer)
        rs.getBigDecimal(9); // total (numeric)
        // :: error: (column.index.out.of.bounds)
        rs.getInt(10);

        @SqlCheckedPositive
        ResultSet rsCopy = rs;
    }

}
