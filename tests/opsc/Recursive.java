// DOESN'T WORK ON java8 BRANCH
// TODO find out why

//import java.sql.*;
//
//class Recursive {
//
//    Connection conn;
//
//    public Recursive() throws SQLException {
//        conn =
//                DriverManager.getConnection(
//                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
//    }
//
//    void selectAllCorrect() throws SQLException {
//        // WITH RECURSIVE query linking playlists according to their tracks
//        String sql =
//                """
//                WITH RECURSIVE playlist_track (playlistid, trackid, level) AS (
//                    SELECT playlistid, trackid, 1 AS level
//                    FROM playlisttrack
//                    WHERE playlistid = ?
//                    UNION ALL
//                    SELECT pt.playlistid, pt.trackid, pt.level + 1
//                    FROM playlist_track pt
//                    INNER JOIN playlist_track ppt ON ppt.trackid = pt.trackid
//                    WHERE pt.playlistid = 1
//                )
//                SELECT * FROM playlist_track
//                WHERE playlistid = ?;
//                """;
//
//        PreparedStatement stmt = conn.prepareStatement(sql);
//        stmt.setInt(1, 111);
//    }
//}
