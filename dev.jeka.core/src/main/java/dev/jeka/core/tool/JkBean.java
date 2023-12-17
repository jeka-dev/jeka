package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
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

    private final JkRuntime runtime;

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

    private JkBean() {
        this(JkRuntime.getCurrentContextBaseDir());
    }

    protected void init() {

    }

    @JkDoc("Displays help about this KBean")
    public void help() {
        HelpDisplayer.helpJkBean(this);
    }

    public Path getBaseDir() {
        return runtime.getProjectBaseDir();
    }

    public String getBaseDirName() {
        String result = getBaseDir().getFileName().toString();
        return result.isEmpty() ? getBaseDir().toAbsolutePath().getFileName().toString() : result;
    }

    /**
     * Returns the output directory where all the final and intermediate artifacts are generated.
     */
    public Path getOutputDir() {
        return getBaseDir().resolve(JkConstants.OUTPUT_PATH);
    }

    static boolean nameMatches(String className, String nameCandidate) {
        if (nameCandidate == null) {
            return false;
        }
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

    public JkImportedJkBeans getImportedBeans() {
        return importedJkBeans;
    }

    public JkRuntime getRuntime() {
        return runtime;
    }


    /**
     * Use {@link #load(Class)} instead.
     */
    @Deprecated
    public <T extends JkBean> T getBean(Class<T> beanClass) {
        return this.load(beanClass);
    }

    /**
     * Instantiates the specified KBean into the current runtime, if it is not already present. <p>
     * As KBeans are singleton within a runtime, this method has no effect if the bean is already loaded.
     * @param beanClass The class of the KBean to load.
     * @return This object for call chaining.
     * @see JkRuntime#load(Class)
     */
    public <T extends JkBean> T load(Class<T> beanClass) {
        return runtime.load(beanClass);
    }



    final String shortName() {
        return name(this.getClass());
    }

    @Override
    public String toString() {
        return shortName() + " in project '" + JkUtilsPath.friendlyName(this.runtime.getProjectBaseDir()) + "'";
    }

    /**
     * Cleans output directory.
     */
    protected void cleanOutput() {
        Path output = getOutputDir();
        JkLog.info("Clean output directory " + output.toAbsolutePath().normalize());
        if (Files.exists(output)) {
            JkPathTree.of(output).deleteContent();
        }
    }

    static String name(String fullyQualifiedClassName) {
        final String className = fullyQualifiedClassName.contains(".")
                ? JkUtilsString.substringAfterLast(fullyQualifiedClassName, ".")
                : fullyQualifiedClassName;
        if (!className.endsWith(CLASS_SUFFIX) || className.equals(CLASS_SUFFIX)) {
            return JkUtilsString.uncapitalize(className);
        }
        final String prefix = JkUtilsString.substringBeforeLast(className, CLASS_SUFFIX);
        return JkUtilsString.uncapitalize(prefix);
    }

    static String name(Class<?> jkBeanClass) {
        return name(jkBeanClass.getName());
    }

    final boolean isMatchingName(String candidateName) {
        return nameMatches(this.getClass().getName(), candidateName);
    }


}

