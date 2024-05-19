package io.github.eisop.opsc;

import io.github.eisop.opsc.log.OpsLogger;
import java.io.IOException;
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

    private static final String LOG_FILE_NAME_PATTERN = "yyyyMMdd-HHmmss'-opslog.csv'";

    protected OpsLogger logger;

    @Override
    public void typeProcessingOver() {
        try {
            logger.close();
        } catch (IOException e) {
            throw new TypeSystemError("Could not close logger: ", e.getMessage());
        }
        super.typeProcessingOver();
    }

    @Override
    public void initChecker() {
        String logDir = hasOption("opsLogDir") ? getOption("opsLogDir") : ".";
        String logFileName =
                DateTimeFormatter.ofPattern(LOG_FILE_NAME_PATTERN)
                        .format(LocalDateTime.now(ZoneId.systemDefault()));
        Path logPath = Paths.get(logDir, logFileName);
        try {
            logger = new OpsLogger(logPath);
        } catch (IOException e) {
            throw new UserError(
                    "Could not create logger. Check the path provided with -AopsLogDir", e);
        }

        System.out.println("Logging to " + logPath.toAbsolutePath());

        super.initChecker();
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
