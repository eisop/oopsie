package io.github.eisop.opsc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.javacutil.TypeSystemError;

public class TypeMapping {

    private final List<CSVRecord> records;

    public TypeMapping(URL configFilePath) {
        try {
            Reader reader =
                    new InputStreamReader(configFilePath.openStream(), StandardCharsets.UTF_8);
            records = CSVFormat.DEFAULT.parse(reader).getRecords();
        } catch (IOException e) {
            throw new TypeSystemError("Could not load type mapping configuration");
        }
    }

    public OpsCheckResult checkCall(String method, String jdbcType) {
        for (CSVRecord record : records) {
            if (record.get(0).equals(method) && record.get(1).equals(jdbcType)) {
                OpsCheckResultKind kind = OpsCheckResultKind.valueOf(record.get(2));
                return new OpsCheckResult(kind, record.get(3));
            }
        }
        return new OpsCheckResult(OpsCheckResultKind.ERROR, "incompatibleTypes");
    }

    public Set<String> getSetMethodNames() {
        Set<String> names = new HashSet<>();
        for (CSVRecord record : records) {
            if (record.get(0).startsWith("set")) {
                names.add(record.get(0));
            }
        }
        return names;
    }

    public Set<String> getGetMethodNames() {
        Set<String> names = new HashSet<>();
        for (CSVRecord record : records) {
            if (record.get(0).startsWith("get")) {
                names.add(record.get(0));
            }
        }
        return names;
    }
}
