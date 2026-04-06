package oopsie;

import java.sql.*;

class Delete {

    void deleteFromInvoice() throws SQLException {
        String sql = "DELETE FROM INVOICE WHERE INVOICEID = ?";

        Connection conn = DriverManager.getConnection("oluwa://burna.ti/de");
        PreparedStatement stmt = conn.prepareStatement(sql);

        // set parameters
        stmt.setInt(1, 1);

        // more parameters than are good for you
        // :: error: (parameter.index.out.of.bounds)
        stmt.setInt(2, 2);

        // incorrect type
        // :: error: (parameter.type.incompatible)
        stmt.setString(1, "olohun");
    }

    void deleteNoParams() throws SQLException {
        String sql = "DELETE FROM INVOICE WHERE INVOICEID = 1";

        Connection conn = DriverManager.getConnection("oluwa://burna.ti/de");
        PreparedStatement stmt = conn.prepareStatement(sql);
    }
}
