package tests;

import io.github.eisop.oopsie.OopsieChecker;
import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Run the Oopsie tests. */
public class OopsieTest extends CheckerFrameworkPerDirectoryTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/chinook";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    public OopsieTest(List<File> testFiles) {
        // set checker options for db connection
        super(
                testFiles,
                OopsieChecker.class,
                "oopsie",
                "-AdbUrl=" + DB_URL,
                "-AdbUser=" + DB_USER,
                "-AdbPassword=" + DB_PASSWORD,
                "-AenableSqlStringHeuristic=false",
                "-AnonNullStringsConcatenation=true");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"oopsie"};
    }
}
