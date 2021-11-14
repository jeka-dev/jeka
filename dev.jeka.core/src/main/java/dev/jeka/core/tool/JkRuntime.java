package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class JkRuntime {

    static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    private final static Map<Path, JkRuntime> RUNTIMES = new HashMap<>();

    private final Path projectBaseDir;

    private JkDependencyResolver dependencyResolver;

    private JkBeanRegistry jkBeanRegistry = new JkBeanRegistry(Environment.commandLine.getPluginOptions());

    private JkRuntime(Path projectBaseDir) {
        this.projectBaseDir = projectBaseDir;
    }

    static JkRuntime of(Path projectBaseDir) {
        return RUNTIMES.computeIfAbsent(projectBaseDir, path -> new JkRuntime(path));
    }

    static JkRuntime ofContextBaseDir() {
        return of(getBaseDirContext());
    }

    static void setBaseDirContext(Path baseDir) {
        JkUtilsAssert.argument(baseDir == null || Files.exists(baseDir),"Project " + baseDir + " not found.");
        BASE_DIR_CONTEXT.set(baseDir);
    }

    private static Path getBaseDirContext() {
        return Optional.ofNullable(BASE_DIR_CONTEXT.get()).orElseGet(() -> {
            setBaseDirContext(Paths.get(""));
            return BASE_DIR_CONTEXT.get();
        });
    }

    public Path getProjectBaseDir() {
        return projectBaseDir;
    }

    public JkBeanRegistry getBeanRegistry() {
        return jkBeanRegistry;
    }

    public JkRepoSet getDownloadRepos() {
        return dependencyResolver.getRepos();
    }

    void setDependencyResolver(JkDependencyResolver resolverArg) {
        dependencyResolver = resolverArg;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    void init() {
        for (JkBean bean : getBeanRegistry().getAll()) {
            try {
                bean.init();
            } catch (Exception e) {
                throw JkUtilsThrowable.unchecked(e);
            }
        }
        List<JkBean> beans = getBeanRegistry().getAll();
        Collections.reverse(beans);
        for (JkBean bean : beans) {
            try {
                bean.postInit();
            } catch (Exception e) {
                throw JkUtilsThrowable.unchecked(e);
            }
        }
    }

    void initialise(JkBean jkBean) throws Exception {
        jkBean.init();

        // initialise imported projects after setup to let a chance master Jeka class
        // to modify imported Jeka classes in the setup method.
        for (JkBean importedJkBean : jkBean.getImportedJkBeans().get(false)) {
            initialise(importedJkBean);
        }

        for (JkBean registeredBean : getBeanRegistry().getAll()) {
            List<BeanDescription.BeanField> defs = BeanDescription.of(registeredBean.getClass()).beanFields();
            JkLog.startTask("Activating KBean " + registeredBean.shortName() + " with options "
                    + HelpDisplayer.optionValues(defs));
            try {
                registeredBean.postInit();
            } catch (RuntimeException e) {
                JkLog.error("KBean " + registeredBean.shortName() + " has caused build instantiation failure.");
                throw e;
            }
            JkLog.endTask();
        }
        JkRuntime.setBaseDirContext(null);
    }




    <T extends JkBean> T ofUninitialized(Class<T> JkBeanClass) {
        final T jkBean = JkUtilsReflect.newInstance(JkBeanClass);
        //final JkClass jkClassInstance = jkBean;

        // Inject options & environment variables
        JkOptions.populateFields(jkBean, JkOptions.readSystemAndUserOptions());
        JkOptions.populateFields(jkBean, JkOptions.readFromProjectOptionsProperties(jkBean.getBaseDir()));
        FieldInjector.injectEnv(jkBean);
        Set<String> unusedCmdOptions = JkOptions.populateFields(jkBean, Environment.commandLine.getCommandOptions());
        unusedCmdOptions.forEach(key -> JkLog.warn("Option '" + key
                + "' from command line does not match with any field of class " + jkBean.getClass().getName()));

        // Load plugins declared in command line and inject options
        getBeanRegistry().loadCommandLinePlugins();
        List<JkBean> plugins = getBeanRegistry().getAll();
        for (JkBean plugin : plugins) {
            if (!getBeanRegistry().getAll().contains(plugin)) {
                getBeanRegistry().injectOptions(plugin);
            }
        }
        return jkBean;
    }

    /**
     * Creates a instance of the specified Jeka class (extending JkClass), including option injection, plugin loading
     * and plugin activation.
     */
    <T extends JkBean> T of(Class<T> jkClass) {
        String currentPath = getBaseDirContext().equals(Paths.get("")) ? "." : getBaseDirContext().toString();
        JkLog.startTask("Instantiating Jeka class " + jkClass.getName() + " at " + currentPath);
        final T jkBean = ofUninitialized(jkClass);
        try {
            initialise(jkBean);
        } catch (Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        JkLog.endTask();
        return jkBean;
    }
}
