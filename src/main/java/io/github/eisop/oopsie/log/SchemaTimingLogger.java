package io.github.eisop.oopsie.log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jspecify.annotations.Nullable;

/** Lightweight file logger for schema timing and DB access counters. */
public class SchemaTimingLogger {

    private static final String CALCITE_LOG_FILE = "calcite_schema_timing.csv";
    private static final String JDBC_LOG_FILE = "jdbc_schema_timing.csv";
    private static final int MAX_DETAIL_LENGTH = 200;
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Map<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();
    private static final String[] CSV_HEADER = {
        "timestamp", "event", "class", "operation", "count", "duration_ms", "success", "details"
    };

    private final Path logDir;

    public SchemaTimingLogger(Path logDir) {
        this.logDir = logDir;
    }

    public void logMethodTiming(
            String owner, String operation, long nanos, boolean success, @Nullable String details) {
        log("METHOD", owner, operation, nanos, success, details);
    }

    private synchronized void log(
            String eventType,
            String owner,
            String operation,
            long nanos,
            boolean success,
            @Nullable String details) {
        String key = owner + ":" + operation + ":" + eventType;
        long count = COUNTERS.computeIfAbsent(key, unused -> new AtomicLong(0)).incrementAndGet();
        double millis = nanos / 1_000_000.0;
        String timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(TIMESTAMP_FORMAT);
        String safeDetails = sanitize(details);
        String logFile = logFileForOwner(owner);

        try {
            File file = new File(logFile);
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Path path = file.toPath();
            boolean writeHeader = !Files.exists(path) || Files.size(path) == 0L;
            CSVFormat format =
                    writeHeader
                            ? CSVFormat.DEFAULT.builder().setHeader(CSV_HEADER).get()
                            : CSVFormat.DEFAULT;

            try (CSVPrinter printer =
                    new CSVPrinter(
                            Files.newBufferedWriter(
                                    path,
                                    StandardCharsets.UTF_8,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.APPEND),
                            format)) {
                printer.printRecord(
                        timestamp,
                        eventType,
                        owner,
                        operation,
                        count,
                        millis,
                        success,
                        safeDetails);
                printer.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to write schema timing log: " + e.getMessage());
        }
    }

    private String logFileForOwner(String owner) {
        if ("CalciteSchemaInfo".equals(owner)) {
            return logDir.resolve(CALCITE_LOG_FILE).toString();
        }
        if ("JDBCSchemaInfo".equals(owner)) {
            return logDir.resolve(JDBC_LOG_FILE).toString();
        }
        return "oopsielog/schema_timing_unknown.csv";
    }

    private String sanitize(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String clean = raw.replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.length() <= MAX_DETAIL_LENGTH) {
            return clean;
        }
        return clean.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}
