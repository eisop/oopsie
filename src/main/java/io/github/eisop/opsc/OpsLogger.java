package io.github.eisop.opsc;

import com.sun.source.tree.CompilationUnitTree;

/** TODO Write to csv */
public class OpsLogger {

    // TODO Keep sql string?
    public void newPreparedStatement(CompilationUnitTree tree, long start, String sql) {
        System.out.println(
                "New prepared statement at " + tree.getSourceFile().getName() + ":" + start);
    }

    public void warningRelatedToStatement(
            CompilationUnitTree warningTree,
            long warningStart,
            CompilationUnitTree statementTree,
            long statementStart,
            String kind,
            String details) {
        System.out.println(
                "Warning at "
                        + warningTree.getSourceFile().getName()
                        + ":"
                        + warningStart
                        + " related to statement at "
                        + statementTree.getSourceFile().getName()
                        + ":"
                        + statementStart
                        + ": "
                        + kind
                        + ": "
                        + details);
    }

    public void errorRelatedToStatement(
            CompilationUnitTree errorTree,
            long errorStart,
            CompilationUnitTree statementTree,
            long statementStart,
            String kind,
            String details) {
        System.out.println(
                "Error at "
                        + errorTree.getSourceFile().getName()
                        + ":"
                        + errorStart
                        + " related to statement at "
                        + statementTree.getSourceFile().getName()
                        + ":"
                        + statementStart
                        + ": "
                        + kind
                        + ": "
                        + details);
    }
}
