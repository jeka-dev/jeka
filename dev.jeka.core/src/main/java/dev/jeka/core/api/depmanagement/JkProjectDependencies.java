package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A set of dependencies commonly used in Java projects.
 */
public class JkProjectDependencies {

    private static final String COMPILE_AND_RUNTIME = "compile+runtime";

    private static final String COMPILE = "compile";

    private static final String RUNTIME = "runtime";

    private static final String TEST = "test";

    private static final List<String> KNOWN_QUALIFIER = JkUtilsIterable.listOf(COMPILE, COMPILE_AND_RUNTIME,
            RUNTIME, TEST);


    // All necessary dependencies for compiling
    private final JkDependencySet compileDeps;

    // All necessary dependencies for the runtime
    private final JkDependencySet runtimeDeps;

    // Only additional dependencies for testing
    private final JkDependencySet testDeps;

    private JkProjectDependencies(JkDependencySet compileDeps, JkDependencySet runtimeDeps, JkDependencySet testDeps) {
        this.compileDeps = compileDeps;
        this.runtimeDeps = runtimeDeps;
        this.testDeps = testDeps;
    }

    public static JkProjectDependencies of(JkDependencySet compile, JkDependencySet runtime, JkDependencySet test) {
        return new JkProjectDependencies(compile, runtime, test);
    }

    static JkProjectDependencies of() {
        return of(JkDependencySet.of(), JkDependencySet.of(), JkDependencySet.of());
    }

    /**
     * Creates a {@link JkDependencySet} based on jars located under the specified directory. Jars are
     * supposed to lie in a directory structure standing for the different scopes they are intended.
     * So jars needed for compilation are supposed to be in <code>baseDir/compile</code>, jar needed for
     * test are supposed to be in <code>baseDir/test</code> and so on.
     */
    public static JkProjectDependencies ofLocal(Path baseDir) {
        final JkPathTree libDir = JkPathTree.of(baseDir);
        if (!libDir.exists()) {
            return JkProjectDependencies.of();
        }
        JkDependencySet compile = JkDependencySet.of()
                .andFiles(libDir.andMatching(true, "*.jar", COMPILE + "/*.jar").getFiles())
                .andFiles(libDir.andMatching(true, COMPILE_AND_RUNTIME + "/*.jar").getFiles());
        JkDependencySet runtime = JkDependencySet.of()
                .andFiles(libDir.andMatching(true, "*.jar", RUNTIME + "/*.jar").getFiles())
                .andFiles(libDir.andMatching(true, COMPILE_AND_RUNTIME + "/*.jar").getFiles());
        JkDependencySet test = JkDependencySet.of()
                .andFiles(libDir.andMatching(true, "*.jar", TEST + "/*.jar").getFiles());
        return of(compile, runtime, test);
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static JkProjectDependencies ofTextDescription(Path path) {
        return ofTextDescription(JkUtilsPath.toUrl(path));
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static JkProjectDependencies ofTextDescriptionIfExist(Path path) {
        if (Files.notExists(path)) {
            return JkProjectDependencies.of();
        }
        return ofTextDescription(JkUtilsPath.toUrl(path));
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static JkProjectDependencies ofTextDescription(URL url) {
        return ofTextDescription(JkUtilsIO.read(url));
    }

    /**
     * Creates a {@link JkProjectDependencies} from a flat file formatted as :
     * <pre>
     * - COMPILE+RUNTIME
     * org.springframework.boot:spring-boot-starter-thymeleaf
     * org.springframework.boot:spring-boot-starter-data-jpa
     *
     * - COMPILE
     * org.projectlombok:lombok:1.16.16
     *
     * - RUNTIME
     * com.h2database:h2
     * org.liquibase:liquibase-core
     * com.oracle:ojdbc6:12.1.0
     *
     * - TEST
     * org.springframework.boot:spring-boot-starter-test
     * org.seleniumhq.selenium:selenium-chrome-driver:3.4.0
     * org.fluentlenium:fluentlenium-assertj:3.2.0
     * org.fluentlenium:fluentlenium-junit:3.2.0
     * </pre>
     */
    public static JkProjectDependencies ofTextDescription(String description) {
        final String[] lines = description.split(System.lineSeparator());
        JkDependencySet compile = JkDependencySet.of();
        JkDependencySet runtime = JkDependencySet.of();
        JkDependencySet test = JkDependencySet.of();

       String currentQualifier = COMPILE_AND_RUNTIME;
        for (final String line : lines) {
            if (line.trim().startsWith("#")) {
                continue;
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            if (line.startsWith("-")) {
                currentQualifier = readQualifier(line);
                continue;
            }
            final JkModuleDependency dependency = JkModuleDependency.of(line.trim());
            if (COMPILE_AND_RUNTIME.equals(currentQualifier) || COMPILE.equals(currentQualifier)) {
                compile = compile.and(dependency);
            }
            if (COMPILE_AND_RUNTIME.equals(currentQualifier) || RUNTIME.equals(currentQualifier)) {
                runtime = runtime.and(dependency);
            } else if (TEST.equals(currentQualifier)) {
                test = test.and(dependency);
            }
        }
        return JkProjectDependencies.of(compile, runtime, test);
    }

    private static String readQualifier(String line) {
        final String payload = JkUtilsString.substringAfterFirst(line,"-").trim().toLowerCase();
        if (KNOWN_QUALIFIER.contains(payload)) {
            return payload;
        }
        return COMPILE_AND_RUNTIME;
    }


    public JkDependencySet getCompileDeps() {
        return compileDeps;
    }

    public JkDependencySet getRuntimeDeps() {
        return runtimeDeps;
    }

    public JkDependencySet getTestDeps() {
        return testDeps;
    }

    public JkProjectDependencies and(JkProjectDependencies other) {
        return of(compileDeps.and(other.compileDeps), runtimeDeps.and(other.runtimeDeps), testDeps.and(other.testDeps));
    }

}
