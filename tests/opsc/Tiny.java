import io.github.eisop.opsc.qual.Sql;

class Tiny {
    // :: error: (assignment.type.incompatible)
    @Sql String s = "dummy";
}
