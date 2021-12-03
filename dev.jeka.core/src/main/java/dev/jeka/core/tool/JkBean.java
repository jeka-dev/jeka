package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for KBean. User code is not supposed to instantiate KBeans using 'new' but usinng
 * {@link JkRuntime#getBean(Class)}.
 */
public abstract class JkBean {

    private static final String JKBEAN_CLASS_SIMPLE_NAME = JkBean.class.getSimpleName();

    private static final String CLASS_SUFFIX = JkBean.class.getSimpleName();

    private JkRuntime runtime;

    private final JkImportedJkBeans importedJkBeans;  // KBeans from other projects

    /*
     * Plugin instances are likely to be configured by the owning <code>JkClass</code> instance, before options
     * are injected.
     * If a plugin needs to initialize state before options are injected, you have to do it in the
     * constructor.
     */
    private JkBean(JkRuntime runtime) {
        this.runtime = runtime;
        this.importedJkBeans = new JkImportedJkBeans(this);
    }

    protected JkBean() {
        this(JkRuntime.getCurrentContextBaseDir());
    }

    @JkDoc("Displays help about this plugin.")
    public void help() {
        HelpDisplayer.helpJkBean(this);
    }

    public Path getBaseDir() {
        return runtime.getProjectBaseDir();
    }

    /**
     * Returns the output directory where all the final and intermediate artifacts are generated.
     */
    public Path getOutputDir() {
        return getBaseDir().resolve(JkConstants.OUTPUT_PATH);
    }

    /**
     * This method is invoked once field values have been injected.
     * Configuration of other KBean is supposed to be implemented here.
     */
    protected void init() throws Exception {
    }

    protected void postInit() throws Exception {
    }

    final String shortName() {
        return name(this.getClass());
    }

    /**
     * Cleans the output directory.
     */
    @JkDoc("Cleans the output directory.")
    public void clean() {
        Path output = getOutputDir();
        JkLog.info("Clean output directory " + output);
        if (Files.exists(output)) {
            JkPathTree.of(output).deleteContent();
        }
    }

    public JkImportedJkBeans getImportedJkBeans() {
        return importedJkBeans;
    }

    public JkRuntime getRuntime() {
        return runtime;
    }

    static String name(Class<?> jkBeanClass) {
        final String className = jkBeanClass.getSimpleName();
        if (!className.endsWith(CLASS_SUFFIX) || className.equals(CLASS_SUFFIX)) {
            return JkUtilsString.uncapitalize(className);
        }
        final String prefix = JkUtilsString.substringBeforeLast(className, CLASS_SUFFIX);
        return JkUtilsString.uncapitalize(prefix);
    }

    static boolean nameMatches(String className, String nameCandidate) {
        if (nameCandidate.equals(className)) {
            return true;
        }
        String classSimpleName = className.contains(".") ? JkUtilsString.substringAfterLast(className, ".")
                : className;
        String uncapitalizedClassSimpleName = JkUtilsString.uncapitalize(classSimpleName);
        if (JkUtilsString.uncapitalize(nameCandidate).equals(uncapitalizedClassSimpleName)) {
            return true;
        }
        if (className.endsWith(JKBEAN_CLASS_SIMPLE_NAME)) {
            return uncapitalizedClassSimpleName.equals(nameCandidate + JkBean.class.getSimpleName());
        }
        return false;
    }

    @Override
    public String toString() {
        return shortName() + " in project '" + this.runtime.getProjectBaseDir() + "'";
    }
}
