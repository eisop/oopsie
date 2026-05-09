package io.github.eisop.oopsie;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import io.github.eisop.oopsie.log.OopsieLogger;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import javax.annotation.processing.SupportedOptions;

import io.github.eisop.oopsie.log.SchemaTimingLogger;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;

/**
 * The main checker class for the Oopsie checker.
 */
@SupportedOptions({"dbUrl", "dbUser", "dbPassword", "enableSqlStringHeuristic", "oopsieLogDir"})
public class OopsieChecker extends BaseTypeChecker {

    private static final String LOG_FILE_NAME_PATTERN = "yyyyMMdd-HHmmss'-oopsielog'";

    private OopsieLogger logger;
    private SchemaTimingLogger schemaLogger;

    protected String projectRoot;

    protected TypeMapping typeMapping;

    @Override
    public void typeProcessingOver() {
        super.typeProcessingOver();
    }

    @Override
    public void initChecker() {
        projectRoot = getProjectRoot();

        String logDir =
                hasOption("oopsieLogDir")
                        ? getOption("oopsieLogDir")
                        : Paths.get(projectRoot, "oopsielog/").toString();

        System.out.println("projectRoot = " + projectRoot);
        System.out.println("logDir = " + logDir);

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

        System.out.println("Logging in " + timeStampedLogDir.toAbsolutePath());

        // Load the type mapping file from resources and initialize the type mapping
        URL typeMappingPath = getClass().getResource("/type_mapping.csv");
        if (typeMappingPath == null) {
            throw new TypeSystemError("Could not load type mapping configuration");
        }

        typeMapping = new TypeMapping(typeMappingPath);

        super.initChecker();
    }

    private String getProjectRoot() {
        JavacProcessingEnvironment javacProcessingEnvironment =
                (JavacProcessingEnvironment) getProcessingEnvironment();
        URLClassLoader processorClassLoader =
                (URLClassLoader) javacProcessingEnvironment.getProcessorClassLoader();

        URL url = null;
        for (URL u : processorClassLoader.getURLs()) {
            String path = u.getFile();
            if (path.matches(".*[/\\\\]build[/\\\\].*")
                    && !path.matches(".*\\.(gradle|\\.m2)/.*")) {
                url = u;
                break;
            }
        }

        if (url == null) {
            return "";
        }

        // remove the last part of the path starting from "[/\\]build[/\\]" to get the root path
        return url.getFile().replaceFirst("[/\\\\]build[/\\\\].*", "");
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
            logger.close();
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

    protected OopsieLogger getLogger() {
        return logger;
    }

    protected SchemaTimingLogger getSchemaLogger() {
        return schemaLogger;
    }

    public TypeMapping getTypeMapping() {
        return typeMapping;
    }
}
