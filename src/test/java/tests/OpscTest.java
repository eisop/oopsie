import io.github.eisop.opsc.OpsChecker;
import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Run the OPSC tests. */
public class OpscTest extends CheckerFrameworkPerDirectoryTest {
    public OpscTest(List<File> testFiles) {
        super(testFiles, OpsChecker.class, "opsc");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"opsc"};
    }
}
