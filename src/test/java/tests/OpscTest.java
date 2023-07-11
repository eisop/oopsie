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

    public OpscTest(List<File> testFiles) {
        // set checker options for db connection
        super(
                testFiles,
                OpsChecker.class,
                "opsc",
                "-AdbUrl=" + DB_URL,
                "-AdbUser=" + DB_USER,
                "-AdbPassword=" + DB_PASSWORD);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"opsc"};
    }
}
