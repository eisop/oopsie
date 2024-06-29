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

    private static final String[] COLUMNS = {
        "kind",
        "file",
        "location",
        "relatedStatementFile",
        "relatedStatementLocation",
        "key",
        "details"
    };

    private final CSVPrinter csvPrinter;

    private final String projectRoot;

    public OpsLogger(Path path, String projectRoot) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(COLUMNS).build();
        csvPrinter =
                new CSVPrinter(Files.newBufferedWriter(path, StandardCharsets.UTF_8), csvFormat);
        this.projectRoot = projectRoot;
    }

    @Override
    public void close() throws IOException {
        csvPrinter.close();
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
        logEntry(
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
        logEntry(
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
        logEntry(new OpsLogEntry(kind, sourceFileName, location, null, null, key, null));
    }

    private void logEntry(OpsLogEntry entry) {
        try {
            csvPrinter.printRecord(entry.values());
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
