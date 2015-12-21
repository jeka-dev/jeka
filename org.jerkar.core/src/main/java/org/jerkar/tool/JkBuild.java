package org.jerkar.tool;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * Base class defining commons tasks and utilities necessary for building any
 * kind of project, regardless involved technologies.
 *
 * @author Jerome Angibaud
 */
public class JkBuild {

    private static final ThreadLocal<Map<SubProjectRef, JkBuild>> SUB_PROJECT_CONTEXT = new ThreadLocal<Map<SubProjectRef, JkBuild>>();

    private static final ThreadLocal<File> BASE_DIR_CONTEXT = new ThreadLocal<File>();

    static void baseDirContext(File baseDir) {
        BASE_DIR_CONTEXT.set(baseDir);
    }

    private final File baseDir;

    private final Date buildTime = JkUtilsTime.now();

    /** attached plugin instances to this build */
    protected final JkBuildPlugins plugins = new JkBuildPlugins(this);

    private JkDependencyResolver buildDefDependencyResolver;

    private final JkSlaveBuilds annotatedJkProjectSlaves;

    /**
     * Constructs a {@link JkBuild}
     */
    protected JkBuild() {
        final File baseDirContext = BASE_DIR_CONTEXT.get();
        this.baseDir = JkUtilsObject.firstNonNull(baseDirContext, JkUtilsFile.workingDir());
        final List<JkBuild> subBuilds = populateJkProjectAnnotatedFields();
        this.annotatedJkProjectSlaves = JkSlaveBuilds.of(this.baseDir().root(), subBuilds);
    }

    void setBuildDefDependencyResolver(JkDependencyResolver scriptDependencyResolver) {
        this.buildDefDependencyResolver = scriptDependencyResolver;
    }

    /**
     * Returns the dependency resolver used to compile/run scripts of this
     * project.
     */
    public JkDependencyResolver buildDefDependencyResolver() {
        return this.buildDefDependencyResolver;
    }

    /**
     * Returns the classes accepted as template for plugins. If you override it,
     * do not forget to add the ones from the super class.
     */
    protected List<Class<Object>> pluginTemplateClasses() {
        return Collections.emptyList();
    }

    /**
     * Set the plugins to activate for this build. This method should be invoked
     * after the {@link #setBaseDir(File)} method, so plugins can be configured
     * using the proper base dir.
     */
    protected void setPlugins(Iterable<?> plugins) {
        // Do nothing as no plugin extension as been defined at this level.
    }

    /**
     * Returns the time-stamp this build has been initiated. Default is the time
     * stamp (formatted as 'yyyyMMdd-HHmmss') this build has been instantiated.
     */
    public String buildTimestamp() {
        return JkUtilsTime.iso(buildTime);
    }

    /**
     * Returns the time the build was started.
     */
    protected Date buildTime() {
        return (Date) buildTime.clone();
    }

    /**
     * Returns the specified relative path to this project as a {@link JkPath} instance.
     */
    protected final JkPath toPath(String pathAsString) {
        if (pathAsString == null) {
            return JkPath.of();
        }
        return JkPath.of(baseDir().root(), pathAsString);
    }

    /**
     * Returns the base directory for this project. All file/directory path are
     * resolved from this directory.
     */
    public final JkFileTree baseDir() {
        return JkFileTree.of(baseDir);
    }


    /**
     * Invokes the specified method in this build.
     */
    private final void invoke(String methodName, File fromDir) {
        final Method method;
        try {
            method = this.getClass().getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            JkLog.warn("No zero-arg method '" + methodName + "' found in class '" + this.getClass()
                    + "'. Skip.");
            JkLog.warnStream().flush();
            return;
        }
        final String context;
        if (fromDir != null) {
            final String path = JkUtilsFile.getRelativePath(fromDir, this.baseDir).replace(
                    File.separator, "/");
            context = " from project " + path + ", class " + this.getClass().getName();
        } else {
            context = "";
        }
        JkLog.infoUnderlined("Method : " + methodName + context);
        final long time = System.nanoTime();
        try {
            JkUtilsReflect.invoke(this, method);
            JkLog.info("Method " + methodName + " success in "
                    + JkUtilsTime.durationInSeconds(time) + " seconds.");
        } catch (final RuntimeException e) {
            JkLog.info("Method " + methodName + " failed in " + JkUtilsTime.durationInSeconds(time)
                    + " seconds.");
            throw e;
        }
    }

    /**
     * Executes the specified methods given the fromDir as working directory.
     */
    public void execute(Iterable<JkModelMethod> methods, File fromDir) {
        for (final JkModelMethod method : methods) {
            this.invoke(method, fromDir);
        }
    }

    /**
     * Returns a file located at the specified path relative to the base
     * directory.
     */
    public final File file(String relativePath) {
        if (relativePath.isEmpty()) {
            return baseDir().root();
        }
        return baseDir().file(relativePath);
    }

    /**
     * The output directory where all the final and intermediate artifacts are
     * generated.
     */
    public JkFileTree ouputDir() {
        return baseDir().from(JkConstants.BUILD_OUTPUT_PATH).createIfNotExist();
    }

    /**
     * Returns the file located at the specified path relative to the output
     * directory.
     */
    public File ouputDir(String relativePath) {
        return ouputDir().file(relativePath);
    }

    // ------------ Jerkar methods ------------



    /**
     * Creates the project structure (mainly project folder layout, build class code and IDE metadata) at the root
     * of the current project.
     */
    @JkDoc("Create the project structure")
    public final void scaffold() {
        scaffolder().run();
        JkBuildPlugin.applyScaffold(this.plugins.getActives());
    }

    /**
     * Returns the scaffolder object in charge of doing the scaffolding for this build.
     * Override this method if you write a template class that need to do custom action for scaffolding.
     */
    protected JkScaffolder scaffolder() {
        return new JkScaffolder(this);
    }

    /** Clean the output directory. */
    @JkDoc("Clean the output directory.")
    public void clean() {
        JkLog.start("Cleaning output directory " + ouputDir().root().getPath());
        ouputDir().exclude(JkConstants.BUILD_DEF_BIN_DIR_NAME + "/**").deleteAll();
        JkLog.done();
    }

    /** Conventional method standing for the default operations to perform. */
    @JkDoc("Conventional method standing for the default operations to perform.")
    public void doDefault() {
        clean();
    }

    /** Run checks to verify the package is valid and meets quality criteria. */
    @JkDoc("Run checks to verify the package is valid and meets quality criteria.")
    public void verify() {
        JkBuildPlugin.applyVerify(this.plugins.getActives());
    }

    /** Display all available methods defined in this build. */
    @JkDoc("Display all available methods defined in this build.")
    public void help() {
        HelpDisplayer.help(this);
    }

    /** Display details on all available plugins. */
    @JkDoc("Display details on all available plugins.")
    public void helpPlugins() {
        HelpDisplayer.helpPlugins();
    }

    /**
     * Invokes the specified method in this build but from the w
     *
     * @param jkModelMethod
     * @param from
     */
    private void invoke(JkModelMethod jkModelMethod, File fromDir) {
        if (jkModelMethod.isMethodPlugin()) {
            this.plugins.invoke(jkModelMethod.pluginClass(), jkModelMethod.name());
        } else {
            this.invoke(jkModelMethod.name(), fromDir);
        }
    }

    /**
     * Returns plugins attached to this build and extending the specified class.
     */
    public <T extends JkBuildPlugin> T pluginOf(Class<T> pluginClass) {
        return this.plugins.findInstanceOf(pluginClass);
    }

    @Override
    public String toString() {
        return this.baseDir.getPath();
    }

    /**
     * Returns a {@link JkComputedDependency} on this project and specified
     * files. The 'doDefault' method will be invoked to compute the dependee
     * files.
     */
    public JkComputedDependency asDependency(Iterable<File> files) {
        return BuildDependency.of(this, JkUtilsIterable.setOf(files));
    }

    /**
     * Returns a {@link JkComputedDependency} on this project and specified
     * files. The 'doDefault' method will be invoked to compute the dependee
     * files.
     */
    public JkComputedDependency asDependency(File... files) {
        return BuildDependency.of(this, files);
    }

    /**
     * Returns a {@link JkComputedDependency} on this project and specified
     * files and methods to execute.
     */
    public JkComputedDependency asDependency(String methods, File... files) {
        return BuildDependency.of(this, methods, files);
    }

    /**
     * Returns slave builds (potentially on other projects).
     */
    public final JkSlaveBuilds slaves() {
        return this.annotatedJkProjectSlaves;
    }

    /**
     * Returns the slave project declared with annotation <code>JkProject</code> in this build.
     * @return
     */
    protected final JkSlaveBuilds annotatedJkProjectSlaves() {
        return this.annotatedJkProjectSlaves;
    }

    @SuppressWarnings("unchecked")
    private List<JkBuild> populateJkProjectAnnotatedFields() {
        final List<JkBuild> result = new LinkedList<JkBuild>();
        final List<Field> fields = JkUtilsReflect.getAllDeclaredField(this.getClass(),
                JkProject.class);
        for (final Field field : fields) {
            final JkProject jkProject = field.getAnnotation(JkProject.class);
            final JkBuild subBuild = relativeProject(this,
                    (Class<? extends JkBuild>) field.getType(), jkProject.value());
            JkUtilsReflect.setFieldValue(this, field, subBuild);
            result.add(subBuild);
        }
        return result;
    }

    /**
     * Returns the build of the specified slave project. Slave projects are expressed with relative path to this project.
     */
    public JkBuild relativeProject(String relativePath) {
        return relativeProject(this, null, relativePath);
    }

    private static final JkBuild relativeProject(JkBuild mainBuild, Class<? extends JkBuild> clazz,
            String relativePath) {
        final JkBuild build = mainBuild.relativeProjectBuild(clazz, relativePath);
        return build;
    }

    /**
     * Creates an instance of <code>JkBuild</code> for the given project and
     * build class. The instance field annotated with <code>JkOption</code> are
     * populated as usual.
     */
    @SuppressWarnings("unchecked")
    private final <T extends JkBuild> T relativeProjectBuild(Class<T> clazz, String relativePath) {
        final File projectDir = this.file(relativePath);
        final SubProjectRef projectRef = new SubProjectRef(projectDir, clazz);
        Map<SubProjectRef, JkBuild> map = SUB_PROJECT_CONTEXT.get();
        if (map == null) {
            map = new HashMap<SubProjectRef, JkBuild>();
            SUB_PROJECT_CONTEXT.set(map);
        }
        final T cachedResult = (T) SUB_PROJECT_CONTEXT.get().get(projectRef);
        if (cachedResult != null) {
            return cachedResult;
        }
        final Project project = new Project(projectDir);
        final T result = project.getBuild(clazz);
        JkOptions.populateFields(result);
        SUB_PROJECT_CONTEXT.get().put(projectRef, result);
        return result;
    }

    private static class SubProjectRef {

        final String canonicalFileName;

        final Class<?> clazz;

        SubProjectRef(File projectDir, Class<?> clazz) {
            super();
            this.canonicalFileName = JkUtilsFile.canonicalPath(projectDir);
            this.clazz = clazz;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((canonicalFileName == null) ? 0 : canonicalFileName.hashCode());
            result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SubProjectRef other = (SubProjectRef) obj;
            if (canonicalFileName == null) {
                if (other.canonicalFileName != null) {
                    return false;
                }
            } else if (!canonicalFileName.equals(other.canonicalFileName)) {
                return false;
            }
            if (clazz == null) {
                if (other.clazz != null) {
                    return false;
                }
            } else if (!clazz.equals(other.clazz)) {
                return false;
            }
            return true;
        }

    }

}
