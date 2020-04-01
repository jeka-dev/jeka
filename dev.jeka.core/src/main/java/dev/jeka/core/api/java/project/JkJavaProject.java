package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkFileSystemLocalizable;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

/**
 * Container for a Java project with classic characteristic :
 * <ul>
 *     <li>Contains Java source files to be compiled</li>
 *     <li>All Java sources file (prod + test) are wrote against the same Java version and encoding</li>
 *     <li>JkJavaProject may contain unit tests</li>
 *     <li>It can depends on any accepted dependencies (Maven module, other project, files on fs, ...)</li>
 *
 *     <li>It produces a bin jar, a source jar and a javadoc jar</li>
 *     <li>It can produce any other artifact files (fat-jar, test jar, doc, ...)</li>
 *     <li>It can be identified as a Maven module (means it can provide a group, artifact id, version) in order to be published/reused</li>
 *     <li>It can be published on any Maven/Ivy repository, including Maven central</li>
 *
 *     <li>Part of the sources/resources may be generated</li>
 *     <li>By default, passing test suite is required to produce artifact.</li>
 * </ul>
 *
 * Beside, java projects are highly extensible so you can add build tasks or alter existing ones. This
 * is done using {@link #getMaker()} object. For example you can easily add test cover or SonarQube analysis.
 *
 * It provides cache mechanism in order compile or unit test phases are executed once when generating
 * several artifact files so be aware of clean it if you want to replay some tasks with different settings.
 *
 * @See JkJavaProjectMaker
 */
public class JkJavaProject implements JkJavaIdeSupportSupplier, JkFileSystemLocalizable, Supplier<JkArtifactProducer> {

    private JkProjectSourceLayout sourceLayout;

    private final JkJavaProjectMaker maker;

    private final JkDependencyManagement<JkJavaProject> dependencyManagement;

    private JkJavaProject(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        this.maker = new JkJavaProjectMaker(this);
        this.dependencyManagement = JkDependencyManagement.of(this);
    }

    public static JkJavaProject of(JkProjectSourceLayout layout) {
        return new JkJavaProject(layout);
    }

    public static JkJavaProject ofMavenLayout(Path baseDir) {
        return JkJavaProject.of(JkProjectSourceLayout.ofMavenStyle().withBaseDir(baseDir));
    }

    public static JkJavaProject ofMavenLayout(String baseDir) {
        return ofMavenLayout(Paths.get(baseDir));
    }

    public static JkJavaProject ofSimpleLayout(Path baseDir) {
        return JkJavaProject.of(JkProjectSourceLayout.ofSimpleStyle().withBaseDir(baseDir));
    }

    public static JkJavaProject ofSimpleLayout(String baseDir) {
        return ofSimpleLayout(Paths.get(baseDir));
    }


    // ---------------------------- Getters / setters --------------------------------------------

    @Override
    public Path getBaseDir() {
        return this.getSourceLayout().getBaseDir();
    }

    public JkProjectSourceLayout getSourceLayout() {
        return sourceLayout;
    }

    public JkJavaProjectMaker getMaker() {
        return maker;
    }

    public JkJavaProject setSourceLayout(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        return this;
    }

    public JkDependencyManagement<JkJavaProject> getDependencyManagement() {
        return dependencyManagement;
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + this.sourceLayout.getBaseDir().getFileName();
    }

    @Override
    public JkArtifactProducer get() {
        return getMaker();
    }

    public String getInfo() {
        return new StringBuilder("Project Location : " + this.getBaseDir() + "\n")
                .append("Published Module & version : " + this.maker.getSteps().getPublishing().getVersionedModule() + "\n")
                .append(this.sourceLayout.getInfo()).append("\n")
                .append("Java Source Version : " + this.maker.getSteps().getCompilation().getComputedCompileSpec().getSourceVersion() + "\n")
                .append("Source Encoding : " + this.maker.getSteps().getCompilation().getComputedCompileSpec().getEncoding() + "\n")
                .append("Source file count : " + this.sourceLayout.getSources().count(Integer.MAX_VALUE, false) + "\n")
                .append("Download Repositories : " + this.dependencyManagement.getResolver().getRepos() + "\n")
                .append("Publish repositories : " + this.maker.getSteps().getPublishing().getPublishRepos()  + "\n")
                .append("Declared Dependencies : " + this.dependencyManagement.getDependencies().toList().size() + " elements.\n")
                .append("Defined Artifacts : " + this.get().getArtifactIds())
                .toString();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return JkJavaIdeSupport.ofDefault()
                .withDependencies(this.dependencyManagement.getDependencies())
                .withDependencyResolver(this.dependencyManagement.resolver)
                .withSourceLayout(this.sourceLayout)
                .withSourceVersion(this.maker.getSteps().getCompilation().getComputedCompileSpec().getSourceVersion());
    }

    public static class JkDependencyManagement<T> {

        private final Map<Set<JkScope>, JkResolveResult> dependencyCache = new HashMap<>();

        private final JkDependencyResolver<JkDependencyManagement> resolver;

        private boolean failOnDependencyResolutionError = true;

        /**
         * For parent chaining
         */
        public final T __;

        private JkDependencySet dependencies = JkDependencySet.of();

        private JkDependencyManagement(T __) {
            this.__ = __;
            resolver = JkDependencyResolver.of(this);
            resolver.addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral());
        }

        private static <T> JkDependencyManagement<T> of(T parent) {
           return new JkDependencyManagement(parent);
        }

        public JkDependencySet getDependencies() {
            return this.dependencies;
        }

        public JkDependencyManagement<T> removeDependencies() {
            dependencyCache.clear();
            this.dependencies = JkDependencySet.of();
            return this;
        }

        public JkDependencyManagement<T> addDependencies(JkDependencySet dependencies) {
            dependencyCache.clear();;
            this.dependencies = this.dependencies.and(dependencies);
            return this;
        }

        public JkDependencyResolver<JkDependencyManagement> getResolver() {
            return resolver;
        }

        /**
         * If <code>true</code> this object will throw a JkException whenever a dependency resolution occurs. Otherwise
         * just log a warn message. <code>false</code> by default.
         */
        public JkDependencyManagement<T> setFailOnDependencyResolutionError(boolean fail) {
            this.failOnDependencyResolutionError = fail;
            return this;
        }

        // ------------

        public JkDependencyManagement<T> cleanCache() {
            dependencyCache.clear();
            return this;
        }

        /**
         * Returns dependencies declared for this project. Dependencies declared without specifying
         * scope are defaulted to scope {@link JkJavaDepScopes#COMPILE_AND_RUNTIME}
         */
        public JkDependencySet getScopeDefaultedDependencies() {
            return dependencies.withDefaultScopes(JkJavaDepScopes.COMPILE_AND_RUNTIME);
        }

        /**
         * Returns lib paths standing for the resolution of this project dependencies for the specified dependency scopes.
         */
        public JkResolveResult fetchDependencies(JkScope... scopes) {
            final Set<JkScope> scopeSet = new HashSet<>(Arrays.asList(scopes));
            return dependencyCache.computeIfAbsent(scopeSet,
                    scopes1 -> {
                        JkResolveResult resolveResult =
                                resolver.resolve(getScopeDefaultedDependencies(), scopes);
                        JkResolveResult.JkErrorReport report = resolveResult.getErrorReport();
                        if (report.hasErrors()) {
                            if (failOnDependencyResolutionError) {
                                throw new JkException(report.toString());
                            }
                            JkLog.warn(report.toString());
                        }
                        return resolveResult;
                    });
        }

    }
}