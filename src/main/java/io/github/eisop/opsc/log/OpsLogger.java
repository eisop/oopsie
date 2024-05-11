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

/** TODO Write to csv */
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

    public OpsLogger(Path path) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(COLUMNS).build();
        csvPrinter =
                new CSVPrinter(Files.newBufferedWriter(path, StandardCharsets.UTF_8), csvFormat);
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

    public void unsupportedPreparedStatement(CompilationUnitTree tree, long start) {
        unsupportedPreparedStatement(tree, start, null);
    }

    public void unsupportedPreparedStatement(CompilationUnitTree tree, long start, String key) {
        simpleEntry(OpsLogEntryKind.UNSUPPORTED_PREPARED_STATEMENT, tree, start, key);
    }

    public void entryRelatedToStatement(
            OpsLogEntryKind kind,
            CompilationUnitTree warningTree,
            long warningStart,
            String statementFile,
            String statementStart,
            String key,
            String details) {
        logEntry(
                new OpsLogEntry(
                        kind,
                        warningTree.getSourceFile().getName(),
                        String.valueOf(warningStart),
                        statementFile,
                        statementStart,
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
                warningStart,
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
                errorStart,
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
            sourceFileName = tree.getSourceFile().getName();
            location = String.valueOf(start);
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
}
