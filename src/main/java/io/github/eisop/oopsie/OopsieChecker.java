package io.github.eisop.oopsie;

import io.github.eisop.oopsie.log.OopsieLogger;
import io.github.eisop.oopsie.log.SchemaTimingLogger;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import javax.annotation.processing.SupportedOptions;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;
import org.jspecify.annotations.Nullable;

/** The main checker class for the Oopsie checker. */
@SupportedOptions({"dbUrl", "dbUser", "dbPassword", "enableSqlStringHeuristic", "oopsieLogDir"})
public class OopsieChecker extends BaseTypeChecker {

    private static final String LOG_FILE_NAME_PATTERN = "yyyyMMdd-HHmmss'-oopsielog'";

    private @Nullable OopsieLogger logger;
    private @Nullable SchemaTimingLogger schemaLogger;

    protected String projectRoot = "";

    protected TypeMapping typeMapping;

    public OopsieChecker() {
        // Load the type mapping file from resources and initialize the type mapping
        URL typeMappingPath = getClass().getResource("/type_mapping.csv");
        if (typeMappingPath == null) {
            throw new TypeSystemError("Could not load type mapping configuration");
        }

        typeMapping = new TypeMapping(typeMappingPath);
    }

    @Override
    public void typeProcessingOver() {
        super.typeProcessingOver();
    }

    private String getProjectRoot() {
        try {
            // Create a dummy resource to find the output directory
            FileObject resource =
                    getProcessingEnvironment()
                            .getFiler()
                            .createResource(
                                    StandardLocation.CLASS_OUTPUT,
                                    "",
                                    "dummy" + System.currentTimeMillis());
            Path projectPath = Paths.get(resource.toUri()).getParent();
            resource.delete();

            // Traverse up to find the project root, marked by build.gradle, pom.xml, or .git
            while (projectPath != null) {
                if (Files.exists(projectPath.resolve("build.gradle"))
                        || Files.exists(projectPath.resolve("build.gradle.kts"))
                        || Files.exists(projectPath.resolve("pom.xml"))
                        || Files.exists(projectPath.resolve(".git"))) {
                    return projectPath.toString();
                }
                projectPath = projectPath.getParent();
            }
        } catch (IOException e) {
            throw new TypeSystemError("Could not determine project root: ", e.getMessage());
        }

        throw new TypeSystemError(
                "Could not determine the path to the project root. Please provide a log directory using -AoopsieLogDir.");
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new OopsieVisitor(this);
    }

    @Override
    protected boolean shouldAddShutdownHook() {
        return true;
    }

    @Override
    protected void shutdownHook() {
        ((OopsieAnnotatedTypeFactory) getTypeFactory()).shutdown();
        try {
            if (logger != null) {
                logger.close();
            }
        } catch (IOException e) {
            throw new TypeSystemError("Could not close logger: ", e.getMessage());
        }
        super.shutdownHook();
    }

    @Override
    protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);

        return checkers;
    }

    @EnsuresNonNull({"logger", "schemaLogger"})
    private void initLogger() {
        String logDir;
        if (hasOption("oopsieLogDir")) {
            logDir = getOption("oopsieLogDir");
        } else {
            projectRoot = getProjectRoot();
            logDir = Paths.get(projectRoot, "oopsielog/").toString();
        }

        if (logDir == null) {
            throw new UserError(
                    "Unable to determine the log directory. Please provide it with -AoopsieLogDir.");
        }

        String timeStamp =
                DateTimeFormatter.ofPattern(LOG_FILE_NAME_PATTERN)
                        .format(LocalDateTime.now(ZoneId.systemDefault()));
        Path timeStampedLogDir = Paths.get(logDir, timeStamp);

        try {
            Files.createDirectories(timeStampedLogDir);
        } catch (IOException e) {
            throw new UserError(
                    "Could not create log directory: "
                            + logDir
                            + ". Consider choosing an alternative directory using -AoopsieLogDir",
                    e);
        }
        System.out.println("Storing oopsie logs in " + timeStampedLogDir);

        try {
            logger =
                    new OopsieLogger(
                            timeStampedLogDir.resolve("statements.csv"),
                            timeStampedLogDir.resolve("bindings.csv"),
                            projectRoot);
            schemaLogger = new SchemaTimingLogger(timeStampedLogDir);
        } catch (IOException e) {
            throw new UserError(
                    "Could not create logger. Check the path provided with -AoopsieLogDir", e);
        }
    }

    protected OopsieLogger getLogger() {
        if (logger != null) {
            return logger;
        }

        initLogger();

        return logger;
    }

    protected SchemaTimingLogger getSchemaLogger() {
        if (schemaLogger != null) {
            return schemaLogger;
        }
        initLogger();
        return schemaLogger;
    }

    public TypeMapping getTypeMapping() {
        return typeMapping;
    }
}
