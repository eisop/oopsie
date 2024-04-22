package io.github.eisop.opsc.log;

import com.sun.source.tree.CompilationUnitTree;

/** TODO Write to csv */
public class OpsLogger {

    public void supportedPreparedStatement(CompilationUnitTree tree, long start) {
        supportedPreparedStatement(tree, start, null);
    }

    public void supportedPreparedStatement(CompilationUnitTree tree, long start, String details) {
        simpleEntry(OpsLogEntryKind.SUPPORTED_PREPARED_STATEMENT, tree, start, details);
    }

    public void unsupportedPreparedStatement(CompilationUnitTree tree, long start) {
        unsupportedPreparedStatement(tree, start, null);
    }

    public void unsupportedPreparedStatement(CompilationUnitTree tree, long start, String details) {
        simpleEntry(OpsLogEntryKind.UNSUPPORTED_PREPARED_STATEMENT, tree, start, details);
    }

    public void entryRelatedToStatement(
            OpsLogEntryKind kind,
            CompilationUnitTree warningTree,
            long warningStart,
            CompilationUnitTree statementTree,
            long statementStart,
            String key,
            String details) {
        logEntry(
                new OpsLogEntry(
                        kind,
                        warningTree.getSourceFile().getName(),
                        warningStart,
                        statementTree.getSourceFile().getName(),
                        statementStart,
                        key,
                        details));
    }

    public void warningRelatedToStatement(
            CompilationUnitTree warningTree,
            long warningStart,
            CompilationUnitTree statementTree,
            long statementStart,
            String key,
            String details) {
        entryRelatedToStatement(
                OpsLogEntryKind.WARNING,
                warningTree,
                warningStart,
                statementTree,
                statementStart,
                key,
                details);
    }

    public void errorRelatedToStatement(
            CompilationUnitTree errorTree,
            long errorStart,
            CompilationUnitTree statementTree,
            long statementStart,
            String key,
            String details) {
        entryRelatedToStatement(
                OpsLogEntryKind.ERROR,
                errorTree,
                errorStart,
                statementTree,
                statementStart,
                key,
                details);
    }

    public void simpleEntry(OpsLogEntryKind kind, CompilationUnitTree tree, long start, String details) {
        logEntry(
                new OpsLogEntry(
                        kind, tree.getSourceFile().getName(), start, null, null, null, details));
    }

    private void logEntry(OpsLogEntry entry) {
        System.out.println(entry.csv());
    }
}
