package dev.jeka.core.tool.builtins.self;

import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkInjectClasspath;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class JkSelfScaffold extends JkScaffold {

    protected static final String BUILD_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/_dev/Build.java";

    protected static final String TEST_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/_dev/test/MyTest.java";

    protected static final String APP_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/app/App.java";

    protected static final String SCRIPT_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/Script.java";

    public enum Kind {
        JEKA_SCRIPT, APP
    }

    public static final List<String> devDeps = new LinkedList<>();

    public static final List<String> deps = new LinkedList<>();

    protected boolean includeJunit = true;

    private final SelfKBean.SelfScaffoldOptions selfScaffoldOption;

    public JkSelfScaffold(SelfKBean selfKBean) {
        this(selfKBean.getBaseDir(), selfKBean.scaffold);
    }

    protected JkSelfScaffold(Path baseDir, SelfKBean.SelfScaffoldOptions scaffoldOptions) {
        super(baseDir);
        this.selfScaffoldOption = scaffoldOptions;
    }

    @Override
    public void run() {
        configureScaffold();
        super.run();
    }

    protected void configureScaffold() {
        if (selfScaffoldOption.kind == Kind.APP) {
            addFileEntry(BUILD_CLASS_PATH, code("Build.snippet", junitDeps(), devDeps));
            addFileEntry(TEST_CLASS_PATH, code("MyTest.snippet"));
            addFileEntry(APP_CLASS_PATH, code("App.snippet", deps));

        } else {
            addFileEntry(SCRIPT_CLASS_PATH, code("Script.snippet", deps, devDeps));
        }
    }

    private String code(String snippetName, List<String> ...deps) {
        String baseCode = readResource(JkSelfScaffold.class, snippetName);
        List<String> allDeps = JkUtilsIterable.concatLists(deps);
        String injectCode = toJkInject(allDeps);
        return baseCode.replace("${inject}", injectCode);
    }

    private static String toJkInject(List<String> deps) {
        List<String> injects = deps.stream()
                .map(dep -> "@" + JkInjectClasspath.class.getSimpleName() + "(\"" + dep + "\")")
                .collect(Collectors.toList());
        return String.join("\n", injects);
    }

    protected List<String> junitDeps() {
        if (!includeJunit) {
            return Collections.emptyList();
        }
        String moduleId = "org.junit.jupiter:junit-jupiter";
        String lastVersion = findLatestVersion(moduleId, "5.9.1");
        return Collections.singletonList(moduleId + ":" + lastVersion);
    }


}
