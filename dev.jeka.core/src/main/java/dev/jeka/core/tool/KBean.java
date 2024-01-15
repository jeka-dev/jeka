package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for KBean. User code is not supposed to instantiate KBeans using 'new' but usinng
 * {@link JkRunbase#load(java.lang.Class)}.
 */
public abstract class KBean {

    private static final String JKBEAN_CLASS_SIMPLE_NAME = KBean.class.getSimpleName();

    private static final String CLASS_SUFFIX = KBean.class.getSimpleName();

    private final JkRunbase runbase;

    private final JkImportedKBeans importedKBeans;  // KBeans from other projects

    private KBean(JkRunbase runbase) {
        this.runbase = runbase;
        this.importedKBeans = new JkImportedKBeans(this);

        // This way KBeans are registered in the order they have been requested for instantiation,
        // and not the order they have finished to be instantiated.
        this.runbase.putKBean(this.getClass(), this);
    }

    /**
     * Use {@link #init()} instead !!
     *
     * If you put some code here, the public instance fields are not yet injected with values from command-line or
     * properties <p>
     */
    protected KBean() {
        this(JkRunbase.getCurrentContextBaseDir());
    }

    /**
     * This method is called by JeKa engine, right after public fields from command-line or properties have been injected.<p>
     * Put your initialization/configuration code here.
     */
    protected void init() {
    }

    @JkDoc("Displays help about this KBean")
    public void help() {
        HelpDisplayer.helpJkBean(this);
    }

    /**
     * Returns the base directory of the project. In single projects, base dir = working dir.
     * When working in multi-project (aka multi-module project), the base dir will be
     * the sub-project base directory.
     */
    public Path getBaseDir() {
        return runbase.getBaseDir();
    }

    /**
     * Returns the name of the folder which stands for the project base directory.
     */
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

    /**
     * Refer to the KBeans coming from other sub-runbase, that has been imported in this KBean.
     */
    public JkImportedKBeans getImportedKBeans() {
        return importedKBeans;
    }

    /**
     * Returns the {@link JkRunbase} where this KBean has been instantiated.
     */
    public JkRunbase getRunbase() {
        return runbase;
    }

    /**
     * Instantiates the specified KBean into the current runbase, if it is not already present. <p>
     * As KBeans are singleton within a runbase, this method has no effect if the bean is already loaded.
     * @param beanClass The class of the KBean to load.
     * @return This object for call chaining.
     * @see JkRunbase#load(Class)
     */
    public <T extends KBean> T load(Class<T> beanClass) {
        return runbase.load(beanClass);
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
            return uncapitalizedClassSimpleName.equals(nameCandidate + KBean.class.getSimpleName());
        }
        return false;
    }

    final String shortName() {
        return name(this.getClass());
    }

    @Override
    public String toString() {
        return "KBean '" + shortName() + "' [from project '" + JkUtilsPath.friendlyName(this.runbase.getBaseDir()) + "']";
    }

    /**
     * Cleans output directory.
     */
    public KBean cleanOutput() {
        Path output = getOutputDir();
        JkLog.info("Clean output directory " + output.toAbsolutePath().normalize());
        if (Files.exists(output)) {
            JkPathTree.of(output).deleteContent();
        }
        return this;
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

    static String name(Class<?> kbeanClass) {
        return name(kbeanClass.getName());
    }

    final boolean isMatchingName(String candidateName) {
        return nameMatches(this.getClass().getName(), candidateName);
    }

    static boolean isPropertyField(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        if (Modifier.isPublic(field.getModifiers())) {
            return true;
        }
        return field.getAnnotation(JkDoc.class) != null;
    }

}

