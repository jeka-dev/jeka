package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plugin instances are owned by a <code>JkClass</code> instance. The relationship is bidirectional :
 * <code>JkClass</code> instances can invoke plugin methods and vice-versa.<p>
 *
 * Therefore, plugins can interact with (or load) other plugins from the owning <code>JkClass</code> instance
 * (which is a quite common pattern).
 */
public abstract class JkBean {

    private static final String JKBEAN_CLASS_SIMPLE_NAME = JkBean.class.getSimpleName();

    private static final String CLASS_SUFIX = JkBean.class.getSimpleName();

    private JkRuntime runtime;

    private final JkImportedJkBeans importedJkBeans;  // KBeans from other projects

    /*
     * Plugin instances are likely to be configured by the owning <code>JkClass</code> instance, before options
     * are injected.
     * If a plugin needs to initialize state before options are injected, you have to do it in the
     * constructor.
     */
    JkBean(JkRuntime runtime) {
        this.runtime = runtime;
        this.importedJkBeans = new JkImportedJkBeans(this);
    }

    protected JkBean() {
        this(JkRuntime.getCurrentContextBaseDir());
    }

    @JkDoc("Displays help about this plugin.")
    public void help() {
        HelpDisplayer.helpPlugin(this);
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
        return computeShortName(this.getClass());
    }

    @Override
    public String toString() {
        return this.getClass().getName();
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

    static String computeShortName(Class<?> jkBeanClass) {
        final String className = jkBeanClass.getSimpleName();
        if (! className.endsWith(CLASS_SUFIX) || className.equals(CLASS_SUFIX)) {
            throw new IllegalStateException(String.format("Plugin class " + className + " not properly named. Name should be formatted as " +
                    "'Xxxx%s' where xxxx is the name of the KBean (uncapitalized).", CLASS_SUFIX, className));
        }
        final String prefix = JkUtilsString.substringBeforeLast(className, CLASS_SUFIX);
        return JkUtilsString.uncapitalize(prefix);
    }

    static boolean nameMatches(String className, String nameCandidate) {
        if (nameCandidate.equals(className)) {
            return true;
        }
        String classSimpleName = className.contains(".") ? JkUtilsString.substringBeforeLast(className, ".")
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

    static String nickName(Class<? extends JkBean> beanClass) {
        String uncapitalizedClassSimpleName = JkUtilsString.uncapitalize(beanClass.getSimpleName());
        if (uncapitalizedClassSimpleName.endsWith(JKBEAN_CLASS_SIMPLE_NAME)
                && !beanClass.getSimpleName().equals(JKBEAN_CLASS_SIMPLE_NAME)) {
            return JkUtilsString.substringBeforeLast(uncapitalizedClassSimpleName, JKBEAN_CLASS_SIMPLE_NAME);
        }
        return uncapitalizedClassSimpleName;
    }

}
