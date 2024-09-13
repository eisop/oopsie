package io.github.eisop.opsc.log;

import com.sun.source.tree.CompilationUnitTree;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TypeSystemError;

public class OpsLogger implements Closeable {

    private final CSVPrinter statementsCsvPrinter;
    private final CSVPrinter bindingsCsvPrinter;

    private final String projectRoot;

    public OpsLogger(Path statementsPath, Path bindingsPath, String projectRoot)
            throws IOException {
        CSVFormat statementsCsvFormat =
                CSVFormat.DEFAULT
                        .builder()
                        .setHeader(OpsStatementLogEntry.STATEMENT_COLUMNS)
                        .build();
        CSVFormat bindingsCsvFormat =
                CSVFormat.DEFAULT.builder().setHeader(OpsBindingLogEntry.BINDING_COLUMNS).build();
        statementsCsvPrinter =
                new CSVPrinter(
                        Files.newBufferedWriter(statementsPath, StandardCharsets.UTF_8),
                        statementsCsvFormat);
        bindingsCsvPrinter =
                new CSVPrinter(
                        Files.newBufferedWriter(bindingsPath, StandardCharsets.UTF_8),
                        bindingsCsvFormat);
        this.projectRoot = projectRoot;
    }

    @Override
    public void close() throws IOException {
        statementsCsvPrinter.close(true);
        bindingsCsvPrinter.close(true);
    }

    public void supportedStatement(
            CompilationUnitTree tree,
            long start,
            String statementString,
            Integer numberOfParameters,
            boolean isPreparedStatement) {
        statementEntry(
                OpsLogEntryKind.SUPPORTED_PREPARED_STATEMENT,
                tree,
                start,
                null,
                statementString,
                numberOfParameters,
                isPreparedStatement);
    }

    public void unsupportedPreparedStatement(
            CompilationUnitTree tree,
            long location,
            String details,
            String statementString,
            boolean isPreparedStatement) {
        String sourceFileName = null;
        String line = null;
        String column = null;
        if (tree != null) {
            sourceFileName = sanitizeFileName(tree.getSourceFile().getName());
            line = lineNumberFromLocation(tree, location);
            column = columnNumberFromLocation(tree, location);
        }
        statementLogEntry(
                new OpsStatementLogEntry(
                        OpsLogEntryKind.UNSUPPORTED_PREPARED_STATEMENT,
                        sourceFileName,
                        line,
                        column,
                        details,
                        statementString,
                        null,
                        isPreparedStatement));
    }

    public void entryRelatedToStatement(
            OpsLogEntryKind kind,
            CompilationUnitTree warningTree,
            String warningLine,
            String warningColumn,
            String statementFile,
            String statementLine,
            String statementColumn,
            String key,
            String details) {
        bindingLogEntry(
                new OpsBindingLogEntry(
                        kind,
                        sanitizeFileName(warningTree.getSourceFile().getName()),
                        warningLine,
                        warningColumn,
                        statementFile,
                        statementLine,
                        statementColumn,
                        key,
                        details));
    }

    public void errorRelatedToStatement(
            CompilationUnitTree errorTree,
            long errorLocation,
            String statementFile,
            String statementLine,
            String statementColumn,
            String key,
            String details) {
        entryRelatedToStatement(
                OpsLogEntryKind.ERROR,
                errorTree,
                lineNumberFromLocation(errorTree, errorLocation),
                columnNumberFromLocation(errorTree, errorLocation),
                statementFile,
                statementLine,
                statementColumn,
                key,
                details);
    }

    public void ok(
            CompilationUnitTree tree,
            long location,
            String statementFile,
            String statementLine,
            String statementColumn,
            String key) {
        entryRelatedToStatement(
                OpsLogEntryKind.OK,
                tree,
                lineNumberFromLocation(tree, location),
                columnNumberFromLocation(tree, location),
                statementFile,
                statementLine,
                statementColumn,
                key,
                null);
    }

    public void statementEntry(
            OpsLogEntryKind kind,
            @Nullable CompilationUnitTree tree,
            long location,
            String details,
            String statementString,
            Integer numberOfParameters,
            boolean isPreparedStatement) {
        String sourceFileName = null;
        String line = null;
        String column = null;
        if (tree != null) {
            sourceFileName = sanitizeFileName(tree.getSourceFile().getName());
            line = lineNumberFromLocation(tree, location);
            column = columnNumberFromLocation(tree, location);
        }
        statementLogEntry(
                new OpsStatementLogEntry(
                        kind,
                        sourceFileName,
                        line,
                        column,
                        details,
                        statementString,
                        numberOfParameters,
                        isPreparedStatement));
    }

    public void simpleStatementEntry(
            OpsLogEntryKind kind,
            @Nullable CompilationUnitTree tree,
            long location,
            String details,
            boolean isPreparedStatement) {
        statementEntry(kind, tree, location, details, null, null, isPreparedStatement);
    }

    private void statementLogEntry(OpsStatementLogEntry entry) {
        try {
            statementsCsvPrinter.printRecord(entry.values());
            statementsCsvPrinter.flush();
        } catch (IOException e) {
            throw new TypeSystemError("Unable to write to log: %s", e.getMessage());
        }
    }

    private void bindingLogEntry(OpsBindingLogEntry entry) {
        try {
            bindingsCsvPrinter.printRecord(entry.values());
            bindingsCsvPrinter.flush();
        } catch (IOException e) {
            throw new TypeSystemError("Unable to write to log: %s", e.getMessage());
        }
    }

    public String sanitizeFileName(String name) {
        // remove projectRoot prefix
        return name.startsWith(projectRoot) ? name.substring(projectRoot.length()) : name;
    }

    private String lineNumberFromLocation(CompilationUnitTree tree, long loc) {
        return String.valueOf(tree.getLineMap().getLineNumber(loc));
    }

    private String columnNumberFromLocation(CompilationUnitTree tree, long loc) {
        return String.valueOf(tree.getLineMap().getColumnNumber(loc));
    }
}
