package dev.jeka.core.tool.builtins.base;

import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkInjectClasspath;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class JkBaseScaffold extends JkScaffold {

    public static final String BUILD_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/_dev/Build.java";

    public static final String TEST_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/_dev/test/MyTest.java";

    public static final String APP_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/app/App.java";

    public static final String SCRIPT_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/Script.java";

    public enum Kind {
        JEKA_SCRIPT, APP
    }

    public static final List<String> devDeps = new LinkedList<>();

    public static final List<String> deps = new LinkedList<>();

    private boolean includeJunit = true;

    private final BaseKBean.BaseScaffoldOptions selfScaffoldOption;

    private final JkConsumers<JkBaseScaffold> customizers = JkConsumers.of();

    private JkBaseScaffold(Path baseDir, BaseKBean.BaseScaffoldOptions scaffoldOptions) {
        super(baseDir);
        this.selfScaffoldOption = scaffoldOptions;
    }

    /**
     * Adds a customizer to this instance, that will be executed at {@link #run()} execution.
     */
    public JkBaseScaffold addCustomizer(Consumer<JkBaseScaffold> customizer) {
        this.customizers.add(customizer);
        return this;
    }

    /**
     * Creates an instance initialized with the base directory and scaffold options..
     */
    public static JkBaseScaffold of(Path baseDir, BaseKBean.BaseScaffoldOptions scaffoldOptions) {
        return new JkBaseScaffold(baseDir, scaffoldOptions);
    }

    /**
     * Creates an instance of JkBaseScaffold initialized with the given BaseKBean.
     */
    public static JkBaseScaffold of(BaseKBean baseKBean) {
        return of(baseKBean.getBaseDir(), baseKBean.scaffold);
    }

    /**
     * Sets the flag to include Junit dependencies in the scaffold.
     * Initial value is <code>true</code>.
     */
    public JkBaseScaffold setIncludeJunit(boolean includeJunit) {
        this.includeJunit = includeJunit;
        return this;
    }

    @Override
    public void run() {
        configureScaffold();
        customizers.accept(this);
        super.run();
    }

    /**
     * Converts a list of dependencies into a string representation of {@link JkInjectClasspath} annotations.
     *
     * @param deps The list of dependencies to convert.
     * @return The string representation of JkInject annotations.
     */
    public static String toJkInject(List<String> deps) {
        List<String> injects = deps.stream()
                .map(dep -> "@" + JkInjectClasspath.class.getSimpleName() + "(\"" + dep + "\")")
                .collect(Collectors.toList());
        return String.join("\n", injects);
    }

    private List<String> junitDeps() {
        if (!includeJunit) {
            return Collections.emptyList();
        }
        String moduleId = "org.junit.jupiter:junit-jupiter";
        String lastVersion = findLatestVersion(moduleId, "5.9.1");
        return Collections.singletonList(moduleId + ":" + lastVersion);
    }

    private String code(String snippetName, List<String> ...deps) {
        String baseCode = readResource(JkBaseScaffold.class, snippetName);
        List<String> allDeps = JkUtilsIterable.concatLists(deps);
        String injectCode = toJkInject(allDeps);
        return baseCode.replace("${inject}", injectCode);
    }

    private void configureScaffold() {
        if (selfScaffoldOption.kind == Kind.APP) {
            addFileEntry(BUILD_CLASS_PATH, code("Build.snippet", junitDeps(), devDeps));
            addFileEntry(TEST_CLASS_PATH, code("MyTest.snippet"));
            addFileEntry(APP_CLASS_PATH, code("App.snippet", deps));

        } else {
            addFileEntry(SCRIPT_CLASS_PATH, code("Script.snippet", deps, devDeps));
        }
    }


}
