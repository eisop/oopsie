import io.github.eisop.opsc.OpsChecker;
import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Run the OPSC tests. */
public class OpscTest extends CheckerFrameworkPerDirectoryTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/chinook";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    private static final String DB_URL_MYSQL = "jdbc:mysql://localhost:3307/Chinook?generateSimpleParameterMetadata=true";
    private static final String DB_USER_MYSQL = "root";
    private static final String DB_PASSWORD_MYSQL = "chinook";

    public OpscTest(List<File> testFiles) {
        // set checker options for db connection
        super(
                testFiles,
                OpsChecker.class,
                "opsc",
                "-AnonNullStringsConcatenation=true",
                "-AdbUrl=" + DB_URL_MYSQL,
                "-AdbUser=" + DB_USER_MYSQL,
                "-AdbPassword=" + DB_PASSWORD_MYSQL);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"opsc"};
    }
}
