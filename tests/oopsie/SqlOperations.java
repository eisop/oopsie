import io.github.eisop.oopsie.qual.Sql;
import io.github.eisop.oopsie.qual.SqlUnsupported;
import java.sql.*;

public class SqlOperations {

    Connection conn;

    public SqlOperations() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    public void supportSelectInserUpdateDelete() throws SQLException {
        @Sql PreparedStatement select, insert, update, delete;
        select = conn.prepareStatement("SELECT total FROM Invoice WHERE total > 300");
        insert =
                conn.prepareStatement(
                        "INSERT INTO Invoice (InvoiceId, CustomerId, InvoiceDate, Total) VALUES ('ayedee', 'c1', '2025-08-22', 123.45)");
        update =
                conn.prepareStatement("UPDATE Invoice SET Total = 3924 WHERE InvoiceId = 'ayedee'");
        delete = conn.prepareStatement("DELETE FROM Invoice WHERE InvoiceId = 'ayedee'");
    }

    public void supportWithClauseSelect() throws SQLException {
        @Sql(in = {"TIMESTAMP", "DECIMAL"})
        PreparedStatement ps;
        ps =
                conn.prepareStatement(
                        "WITH RecentInvoices AS (SELECT * FROM Invoice WHERE InvoiceDate > ?) "
                                + "SELECT * FROM RecentInvoices WHERE Total > ?");
    }

    public void dontSupportDDL() throws SQLException {
        @SqlUnsupported PreparedStatement ps;

        // :: warning: (statement.skipped)
        ps = conn.prepareStatement("CREATE TABLE Test (ID INT)");
        // :: warning: (statement.skipped)
        ps = conn.prepareStatement("ALTER TABLE Invoice ADD COLUMN TestCol INT");
        // :: warning: (statement.skipped)
        ps = conn.prepareStatement("DROP TABLE Test");
    }

    void dontSupportwithRecursive() throws SQLException {
        // We don't support recursive queries
        String sql =
                """
                WITH RECURSIVE playlist_track (playlistid, trackid, level) AS (
                    SELECT playlistid, trackid, 1 AS level
                    FROM playlisttrack
                    WHERE playlistid = ?
                    UNION ALL
                    SELECT pt.playlistid, pt.trackid, pt.level + 1
                    FROM playlist_track pt
                    INNER JOIN playlist_track ppt ON ppt.trackid = pt.trackid
                    WHERE pt.playlistid = 1
                )
                SELECT * FROM playlist_track
                WHERE playlistid = ?;
                """;

        // :: warning: (statement.skipped)
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, 111);
    }
}
