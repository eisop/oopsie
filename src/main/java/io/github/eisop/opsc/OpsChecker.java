package io.github.eisop.opsc;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import io.github.eisop.opsc.log.OpsLogger;
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
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;

/** The main checker class for the Optional Prepared Statement Checker (OPSC). */
@SupportedOptions({"dbUrl", "dbUser", "dbPassword", "enableSqlStringHeuristic", "opsLogDir"})
public class OpsChecker extends BaseTypeChecker {

    private static final String LOG_FILE_NAME_PATTERN = "yyyyMMdd-HHmmss'-opslog'";

    protected OpsLogger logger;

    protected String projectRoot;

    @Override
    public void typeProcessingOver() {
        super.typeProcessingOver();
    }

    @Override
    public void initChecker() {
        projectRoot = getProjectRoot();

        String logDir =
                hasOption("opsLogDir")
                        ? getOption("opsLogDir")
                        : Paths.get(projectRoot, "opslog/").toString();

        System.out.println("projectRoot = " + projectRoot);
        System.out.println("logDir = " + logDir);

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

        System.out.println("Logging in " + timeStampedLogDir.toAbsolutePath());

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
        return new OpsVisitor(this);
    }

    @Override
    protected boolean shouldAddShutdownHook() {
        return true;
    }

    @Override
    protected void shutdownHook() {
        try {
            logger.close();
        } catch (IOException e) {
            throw new TypeSystemError("Could not close logger: ", e.getMessage());
        }
        super.shutdownHook();
    }

    @Override
    protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);

        return checkers;
    }

    protected OpsLogger getLogger() {
        return logger;
    }
}
