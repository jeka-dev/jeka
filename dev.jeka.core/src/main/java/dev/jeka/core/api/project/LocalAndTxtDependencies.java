package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


class LocalAndTxtDependencies {

    // Equals to Maven 'compile' scope or Gradle 'implementation' configuration
    private final JkDependencySet regular;

    // Equals to Maven 'provided' scope
    private final JkDependencySet compileOnly;

    // Equals to Maven 'runtime' scope
    private final JkDependencySet runtimeOnly;

    // Equals to Maven 'test' scope
    private final JkDependencySet test;

    private LocalAndTxtDependencies(JkDependencySet regular,
                                    JkDependencySet compileOnly,
                                    JkDependencySet runtimeOnly,
                                    JkDependencySet test) {
        this.regular = regular;
        this.compileOnly = compileOnly;
        this.runtimeOnly = runtimeOnly;
        this.test = test;
    }

    static LocalAndTxtDependencies of() {
        return new LocalAndTxtDependencies(JkDependencySet.of(), JkDependencySet.of(),
                JkDependencySet.of(), JkDependencySet.of());
    }

    /**
     * Creates a {@link JkDependencySet} based on jars located under the specified directory. Jars are
     * supposed to lie in a directory structure standing for the different scopes they are intended.
     * So jars needed for compilation are supposed to be in <code>baseDir/compile</code>, jar needed for
     * test are supposed to be in <code>baseDir/test</code> and so on.
     */
    public static LocalAndTxtDependencies ofLocal(Path baseDir) {
        return Parser.parseFileStructure(baseDir);
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static LocalAndTxtDependencies ofTextDescriptionIfExist(Path path) {
        if (Files.notExists(path)) {
            return LocalAndTxtDependencies.of();
        }
        return ofTextDescription(JkUtilsPath.toUrl(path));
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static LocalAndTxtDependencies ofTextDescription(URL url) {
        return ofTextDescription(JkUtilsIO.read(url));
    }

    /**
     * Creates a {@link LocalAndTxtDependencies} from a flat file formatted as :
     * <pre>
     * == REGULAR ==
     * org.springframework.boot:spring-boot-starter-thymeleaf
     * org.springframework.boot:spring-boot-starter-data-jpa
     *
     * == COMPILE_ONLY ==
     * org.projectlombok:lombok:1.16.16
     *
     * == RUNTIME_ONLY ==
     * com.h2database:h2
     * org.liquibase:liquibase-core
     * com.oracle:ojdbc6:12.1.0
     *
     * == TEST ==
     * org.springframework.boot:spring-boot-starter-test
     * org.seleniumhq.selenium:selenium-chrome-driver:3.4.0
     * org.fluentlenium:fluentlenium-assertj:3.2.0
     * org.fluentlenium:fluentlenium-junit:3.2.0
     * </pre>
     *
     */
    public static LocalAndTxtDependencies ofTextDescription(String description) {
        return Parser.parseTxt(description);
    }

    public JkDependencySet getRegular() {
        return regular;
    }

    public JkDependencySet getCompileOnly() {
        return compileOnly;
    }

    public JkDependencySet getRuntimeOnly() {
        return runtimeOnly;
    }

    public JkDependencySet getTest() {
        return test;
    }


    public LocalAndTxtDependencies and(LocalAndTxtDependencies other) {
        return new LocalAndTxtDependencies(
                regular.and(other.regular),
                compileOnly.and(other.compileOnly),
                runtimeOnly.and(other.runtimeOnly),
                test.and(other.test)
        );
    }

    private static class Parser {

        private static final String REGULAR = "regular";

        private static final String COMPILE = "compile_only";

        private static final String RUNTIME = "runtime_only";

        private static final String TEST = "test";

        private static final List<String> KNOWN_QUALIFIER = JkUtilsIterable.listOf(COMPILE, REGULAR,
                RUNTIME, TEST);

        private static LocalAndTxtDependencies parseFileStructure(Path baseDir) {
            final JkPathTree libDir = JkPathTree.of(baseDir);
            if (!libDir.exists()) {
                return LocalAndTxtDependencies.of();
            }
            JkDependencySet regular = JkDependencySet.of()
                    .andFiles(libDir.andMatching(true, REGULAR + "/*.jar").getFiles());
            JkDependencySet compileOnly = JkDependencySet.of()
                    .andFiles(libDir.andMatching(true, COMPILE + "/*.jar").getFiles());
            JkDependencySet runtimeOnly = JkDependencySet.of()
                    .andFiles(libDir.andMatching(true, "*.jar", RUNTIME + "/*.jar").getFiles());
            JkDependencySet test = JkDependencySet.of()
                    .andFiles(libDir.andMatching(true, "*.jar", TEST + "/*.jar").getFiles());
            return new LocalAndTxtDependencies(regular, compileOnly, runtimeOnly, test);
        }

        static LocalAndTxtDependencies parseTxt(String description) {
            final String[] lines = description.split(System.lineSeparator());
            JkDependencySet regular = JkDependencySet.of();
            JkDependencySet compileOnly = JkDependencySet.of();
            JkDependencySet runtimeOnly = JkDependencySet.of();
            JkDependencySet test = JkDependencySet.of();

            String currentQualifier = REGULAR;
            for (final String line : lines) {
                if (line.trim().startsWith("#")) {
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (line.startsWith("==")) {
                    currentQualifier = readQualifier(line);
                    continue;
                }
                final JkCoordinateDependency dependency = JkCoordinateDependency.of(line.trim());
                if (REGULAR.equals(currentQualifier) ) {
                    regular = plus(regular, dependency);
                } else if (COMPILE.equals(currentQualifier)) {
                    compileOnly = plus(compileOnly, dependency);
                } else if (RUNTIME.equals(currentQualifier)) {
                    runtimeOnly = plus(runtimeOnly, dependency);
                } else if (TEST.equals(currentQualifier)) {
                    test = plus(test, dependency);
                }
            }
            return new LocalAndTxtDependencies(regular, compileOnly, runtimeOnly, test);
        }

        private static JkDependencySet plus(JkDependencySet dependencySet,
                                            JkCoordinateDependency dependency) {
            JkCoordinate coordinate = dependency.getCoordinate();
            JkCoordinate.JkArtifactSpecification spec = coordinate.getArtifactSpecification();
            if (spec.getClassifier() == null && "pom".equals(spec.getType())) {
                JkVersionProvider versionProvider = dependencySet.getVersionProvider().andBom(coordinate);
                return dependencySet.withVersionProvider(versionProvider);
            }
            return dependencySet.and(dependency);
        }

        private static String readQualifier(String line) {
            String payload = JkUtilsString.substringAfterFirst(line,"==").trim();
            if (payload.contains("=")) {
                payload = JkUtilsString.substringBeforeFirst(payload, "=").toLowerCase().trim();
            }
            if (KNOWN_QUALIFIER.contains(payload)) {
                return payload;
            }
            return REGULAR;
        }

    }

}
