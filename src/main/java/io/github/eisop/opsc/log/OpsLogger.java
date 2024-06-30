package io.github.eisop.opsc.log;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
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

    private static final String[] STATEMENT_COLUMNS = {"kind", "file", "location", "details"};

    private static final String[] BINDING_COLUMNS = {
        "kind",
        "file",
        "location",
        "relatedStatementFile",
        "relatedStatementLocation",
        "key",
        "details"
    };

    private final CSVPrinter statementsCsvPrinter;
    private final CSVPrinter bindingsCsvPrinter;

    private final String projectRoot;

    public OpsLogger(Path statementsPath, Path bindingsPath, String projectRoot)
            throws IOException {
        CSVFormat statementsCsvFormat =
                CSVFormat.DEFAULT.builder().setHeader(STATEMENT_COLUMNS).build();
        CSVFormat bindingsCsvFormat =
                CSVFormat.DEFAULT.builder().setHeader(BINDING_COLUMNS).build();
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
        statementsCsvPrinter.close();
    }

    public void supportedPreparedStatement(CompilationUnitTree tree, long start) {
        supportedPreparedStatement(tree, start, null);
    }

    public void supportedPreparedStatement(CompilationUnitTree tree, long start, String key) {
        simpleEntry(OpsLogEntryKind.SUPPORTED_PREPARED_STATEMENT, tree, start, key);
    }

    public void unsupportedPreparedStatement(
            CompilationUnitTree tree, long start, String key, String details) {
        String sourceFileName = null;
        String location = null;
        if (tree != null) {
            sourceFileName = sanitizeFileName(tree.getSourceFile().getName());
            location = String.valueOf(start);
        }
        statementLogEntry(
                new OpsLogEntry(
                        OpsLogEntryKind.UNSUPPORTED_PREPARED_STATEMENT,
                        sourceFileName,
                        location,
                        null,
                        null,
                        key,
                        details));
    }

    public void unsupportedPreparedStatement(CompilationUnitTree tree, long start) {
        unsupportedPreparedStatement(tree, start, null);
    }

    public void unsupportedPreparedStatement(CompilationUnitTree tree, long start, String key) {
        simpleEntry(OpsLogEntryKind.UNSUPPORTED_PREPARED_STATEMENT, tree, start, key);
    }

    public void entryRelatedToStatement(
            OpsLogEntryKind kind,
            CompilationUnitTree warningTree,
            String warningLocation,
            String statementFile,
            String statementLocation,
            String key,
            String details) {
        bindingLogEntry(
                new OpsLogEntry(
                        kind,
                        sanitizeFileName(warningTree.getSourceFile().getName()),
                        String.valueOf(warningLocation),
                        statementFile,
                        statementLocation,
                        key,
                        details));
    }

    public void warningRelatedToStatement(
            CompilationUnitTree warningTree,
            long warningStart,
            String statementFile,
            String statementStart,
            String key,
            String details) {
        entryRelatedToStatement(
                OpsLogEntryKind.WARNING,
                warningTree,
                lineMappedLocation(warningTree, warningStart),
                statementFile,
                statementStart,
                key,
                details);
    }

    public void errorRelatedToStatement(
            CompilationUnitTree errorTree,
            long errorStart,
            String statementFile,
            String statementStart,
            String key,
            String details) {
        entryRelatedToStatement(
                OpsLogEntryKind.ERROR,
                errorTree,
                lineMappedLocation(errorTree, errorStart),
                statementFile,
                statementStart,
                key,
                details);
    }

    public void simpleEntry(
            OpsLogEntryKind kind, @Nullable CompilationUnitTree tree, long start, String key) {
        String sourceFileName = null;
        String location = null;
        if (tree != null) {
            sourceFileName = sanitizeFileName(tree.getSourceFile().getName());
            location = lineMappedLocation(tree, start);
        }
        statementLogEntry(new OpsLogEntry(kind, sourceFileName, location, null, null, key, null));
    }

    private void statementLogEntry(OpsLogEntry entry) {
        try {
            statementsCsvPrinter.printRecord(entry.statementValues());
        } catch (IOException e) {
            throw new TypeSystemError("Unable to write to log: %s", e.getMessage());
        }
    }

    private void bindingLogEntry(OpsLogEntry entry) {
        try {
            bindingsCsvPrinter.printRecord(entry.bindingValues());
        } catch (IOException e) {
            throw new TypeSystemError("Unable to write to log: %s", e.getMessage());
        }
    }

    private String sanitizeFileName(String name) {
        // todo is there a way to get the (qualified) class name instead?
        // remove projectRoot prefix
        return name.startsWith(projectRoot) ? name.substring(projectRoot.length()) : name;
    }

    private String lineMappedLocation(CompilationUnitTree tree, long loc) {
        LineMap lineMap = tree.getLineMap();
        return lineMap.getLineNumber(loc) + ":" + lineMap.getColumnNumber(loc);
    }
}
