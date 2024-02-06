import io.github.eisop.opsc.qual.Sql;
import java.sql.*;

class Value {

    void conditionalIntInteger(int a, Integer b) {
        // unexpected type error
        // :: error: (conditional.type.incompatible)
        int max = a > b ? a : b;
    }

}
