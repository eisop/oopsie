import io.github.eisop.opsc.OpsChecker;
import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Run the OPSC tests. */
public class OpscTest extends CheckerFrameworkPerDirectoryTest {

    private static final String DB_URL = "jdbc:sqlite:tests/db/Chinook.db";
    private static final String DB_USER = null;
    private static final String DB_PASSWORD = null;

    public OpscTest(List<File> testFiles) {
        // set checker options for db connection
        super(testFiles, OpsChecker.class, "opsc",
                "-AdbUrl=" + DB_URL, "-AdbUser=" + DB_USER, "-AdbPassword=" + DB_PASSWORD);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"opsc"};
    }
}
