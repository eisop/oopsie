package oopsie;

import java.math.BigDecimal;
import java.sql.*;

class Update {

    void updateInvoice() throws SQLException {
        String sql =
                "UPDATE INVOICE SET CUSTOMERID = ?, INVOICEDATE = ?, TOTAL = ? WHERE INVOICEID = ?";

        Connection conn = DriverManager.getConnection("oluwa://burna.ti/de");
        PreparedStatement stmt = conn.prepareStatement(sql);

        // set parameters
        stmt.setInt(1, 2);
        stmt.setTimestamp(2, Timestamp.valueOf("2023-05-02 12:00:00"));
        stmt.setBigDecimal(3, BigDecimal.valueOf(3));
        stmt.setInt(4, 1);

        // more parameters than are good for you
        // :: error: (parameter.index.out.of.bounds)
        stmt.setInt(5, 5);

        // incorrect type
        // :: error: (parameter.type.incompatible)
        stmt.setString(3, "olohun");
    }

    void updateNoParams() throws SQLException {
        String sql =
                "UPDATE INVOICE SET CUSTOMERID = 2, INVOICEDATE = '2023-05-02', TOTAL = 3 WHERE INVOICEID = 1";

        Connection conn = DriverManager.getConnection("oluwa://burna.ti/de");
        PreparedStatement stmt = conn.prepareStatement(sql);
    }
}
