package io.github.eisop.opsc;

import io.github.eisop.opsc.log.OpsLogger;
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
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;
import org.jspecify.annotations.Nullable;

/** The main checker class for the Optional Prepared Statement Checker (OPSC). */
@SupportedOptions({"dbUrl", "dbUser", "dbPassword", "enableSqlStringHeuristic", "opsLogDir"})
public class OpsChecker extends BaseTypeChecker {

    private static final String LOG_FILE_NAME_PATTERN = "yyyyMMdd-HHmmss'-opslog'";

    protected @Nullable OpsLogger logger;

    protected String projectRoot = "";

    protected TypeMapping typeMapping;

    public OpsChecker() {
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
                "Could not determine the path to the project root. Please provide a log directory using -AopsLogDir.");
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new OpsVisitor(this);
    }

    @Override
    protected boolean shouldAddShutdownHook() {
        return true;
    }

    @Override
    protected void shutdownHook() {
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

    @EnsuresNonNull("logger")
    private void initLogger() {
        String logDir;
        if (hasOption("opsLogDir")) {
            logDir = getOption("opsLogDir");
        } else {
            projectRoot = getProjectRoot();
            logDir = Paths.get(projectRoot, "opslog/").toString();
        }

        if (logDir == null) {
            throw new UserError(
                    "Unable to determine the log directory. Please provide it with -AopsLogDir.");
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
                            + ". Consider choosing an alternative directory using -AopsLogDir",
                    e);
        }

        report(
                null,
                new DiagMessage(Diagnostic.Kind.NOTE, "Storing OPSC logs in " + timeStampedLogDir));

        try {
            logger =
                    new OpsLogger(
                            timeStampedLogDir.resolve("statements.csv"),
                            timeStampedLogDir.resolve("bindings.csv"),
                            projectRoot);
        } catch (IOException e) {
            throw new UserError(
                    "Could not create logger. Check the path provided with -AopsLogDir", e);
        }
    }

    protected OpsLogger getLogger() {
        if (logger != null) {
            return logger;
        }

        initLogger();

        return logger;
    }

    public TypeMapping getTypeMapping() {
        return typeMapping;
    }
}
