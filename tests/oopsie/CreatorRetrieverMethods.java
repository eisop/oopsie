import io.github.eisop.oopsie.qual.*;
import java.sql.*;

class CreatorRetrieverMethods {

    Connection conn;

    public CreatorRetrieverMethods() throws SQLException {
        conn =
                DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/chinook", "postgres", "postgres");
    }

    void ps() throws SQLException {
        @Sql(in = {"DECIMAL"})
        PreparedStatement ps1 = prepareStatement1("SELECT * FROM Invoice WHERE Total > ?");
        @Sql(in = {"DECIMAL"})
        PreparedStatement ps2 = prepareStatement2(3, "SELECT * FROM Invoice WHERE Total > ?");
        @Sql(in = {"DECIMAL"})
        PreparedStatement ps3 =
                prepareStatement3("Invoice Retrieval", "SELECT * FROM Invoice WHERE Total > ?");
    }

    @CreatesSqlStatement
    PreparedStatement prepareStatement1(String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    @CreatesSqlStatement(statementStringParameter = 1)
    PreparedStatement prepareStatement2(int priority, String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    @CreatesSqlStatement(statementStringParameter = 1)
    PreparedStatement prepareStatement3(String label, String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }
}
