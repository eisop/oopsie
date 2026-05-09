package io.github.eisop.oopsie;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;

public class TypeMapping {

    private final List<CSVRecord> records;

    private final Set<String> setMethodNames;
    private final Set<String> getMethodNames;

    public TypeMapping(URL configFilePath) {
        try {
            Reader reader =
                    new InputStreamReader(configFilePath.openStream(), StandardCharsets.UTF_8);
            CSVFormat format = CSVFormat.DEFAULT.builder().setCommentMarker('#').build();
            records = format.parse(reader).getRecords();
        } catch (IOException e) {
            throw new TypeSystemError("Could not load type mapping configuration");
        }

        setMethodNames = new HashSet<>();
        getMethodNames = new HashSet<>();
        for (CSVRecord record : records) {
            if (record.get(0).startsWith("set")) {
                setMethodNames.add(record.get(0));
            } else if (record.get(0).startsWith("get")) {
                getMethodNames.add(record.get(0));
            }
        }
    }

    public OopsieCheckResult checkCall(String method, String annotatedType) {
        OopsieType oopsieType = OopsieType.fromAnnotationString(annotatedType);
        String jdbcType = oopsieType.columnDataType();
        for (CSVRecord record : records) {
            if (record.get(0).equals(method) && record.get(1).equalsIgnoreCase(jdbcType)) {
                OopsieCheckResultKind kind = OopsieCheckResultKind.valueOf(record.get(2));
                return new OopsieCheckResult(kind, record.get(3));
            }
        }
        return new OopsieCheckResult(OopsieCheckResultKind.ERROR, "incompatibleTypes");
    }

    protected List<ExecutableElement> getSetterMethods(ProcessingEnvironment processingEnv) {
        List<ExecutableElement> setters = new ArrayList<>();
        for (String name : setMethodNames) {
            setters.addAll(
                    TreeUtils.getMethods("java.sql.PreparedStatement", name, 2, processingEnv));
        }
        return setters;
    }

    protected List<ExecutableElement> getGetterByIndexMethods(ProcessingEnvironment processingEnv) {
        return getMethodNames.stream()
                .map(name -> TreeUtils.getMethod("java.sql.ResultSet", name, processingEnv, "int"))
                .collect(Collectors.toList());
    }

    protected List<ExecutableElement> getGetterByNameMethods(ProcessingEnvironment processingEnv) {
        return getMethodNames.stream()
                .map(
                        name ->
                                TreeUtils.getMethod(
                                        "java.sql.ResultSet",
                                        name,
                                        processingEnv,
                                        "java.lang.String"))
                .collect(Collectors.toList());
    }
}
