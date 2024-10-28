package opsc;

import java.math.BigDecimal;
import java.sql.*;

class Insert {

    void insertIntoInvoice() throws SQLException {
        String sql =
                "INSERT INTO INVOICE (INVOICEID, CUSTOMERID, INVOICEDATE, TOTAL) VALUES(?, ?, ?, ?)";

        Connection conn = DriverManager.getConnection("oluwa://burna.ti/de");
        PreparedStatement stmt = conn.prepareStatement(sql);

        // set parameters
        stmt.setInt(1, 1);
        stmt.setInt(2, 2);
        stmt.setTimestamp(3, Timestamp.valueOf("2023-05-02 12:00:00"));
        stmt.setBigDecimal(4, BigDecimal.valueOf(3));

        // more parameters than are good for you
        // :: error: (parameter.index.out.of.bounds)
        stmt.setInt(5, 5);

        // incorrect type
        // :: error: (parameter.type.incompatible)
        stmt.setString(3, "olohun");
    }

    void insertNoParams() throws SQLException {
        String sql =
                "INSERT INTO INVOICE (INVOICEID, CUSTOMERID, INVOICEDATE, TOTAL) VALUES(1, 2, '2023-05-02', 3)";

        Connection conn = DriverManager.getConnection("oluwa://burna.ti/de");
        PreparedStatement stmt = conn.prepareStatement(sql);
    }
}
