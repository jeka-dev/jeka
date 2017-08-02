package org.jerkar.api.java.build;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.utils.JkUtilsFile;


import java.io.File;


public class JkJavaProject  {

    public static JkJavaProject of(File baseDir) {
        return new JkJavaProject(JkJavaProjectStructure.classic(baseDir));
    }

    public static JkJavaProject ofCurrentWorkingDir() {
        return of(new File("."));
    }

    public static JkJavaProject of(String relatedPath) {
        return of(new File(relatedPath));
    }

    private JkVersionedModule module;

    private String explicitArtifactName;

    private JkJavaProjectStructure structure;

    private JkJavaProjectDepResolver javaDeps;

    private JkJavaCompiler baseCompiler = JkJavaCompiler.base();

    private JkJavaProjectPackager packager;

    private JkJavaProjectPublisher publisher;

    private JkJavadocMaker javadocMaker;

    private JkJavaProject(JkJavaProjectStructure structure) {
        this.structure = structure;
        this.javaDeps = JkJavaProjectDepResolver.of();
        this.packager = defaultPackager();
        this.publisher = defaulJavaPublier();
        this.javadocMaker = defaultJavadocMaker();
    }

    // ------------------------------------------ setters ----------------

    public JkJavaProject setBaseCompiler(JkJavaCompiler baseCompiler) {
        this.baseCompiler = baseCompiler;
        return this;
    }


    // ---------------------------- views -------------------------

    public JkJavaProjectStructure structure() {
        return structure;
    }

    public JkJavaProjectDepResolver depResolver() {
        return javaDeps;
    }

    public String artifactName() {
        if (explicitArtifactName != null) {
            return explicitArtifactName;
        }
        if (module != null) {
            return module.moduleId().name();
        }
        return JkModuleId.of(JkUtilsFile.canonicalFile(structure.baseDir()).getName()).name();
    }

    public JkVersionedModule module() {
        return this.module;
    }

    // --------------------------- doers -----------------------------


    private static JkDependencyResolver defaultResolver(JkVersionedModule versionedModule) {
        return JkDependencyResolver.managed(JkRepos.mavenCentral(), JkDependencies.of())
                        .withModuleHolder(versionedModule)
                        .withParams(JkResolutionParameters.of().withDefault(JkJavaDepScopes.DEFAULT_SCOPE_MAPPING));
    }



    private JkJavaProjectPackager defaultPackager() {
        return JkJavaProjectPackager.of(this);
    }

    private JkJavaProjectPublisher defaulJavaPublier() {
        return JkJavaProjectPublisher.of(this);
    }

    private JkJavadocMaker defaultJavadocMaker() {
        String name = this.artifactName() + "-javadoc";
        return JkJavadocMaker.of(structure.sources(), new File(structure.outputDir(), name),
                new File(structure.outputDir(), name +  ".jar")).withClasspath(
                javaDeps.resolver().get(JkJavaDepScopes.SCOPES_FOR_COMPILATION));
    }

    public final JkJavaCompiler baseCompiler() {
        return this.baseCompiler;
    }

    /**
     * Returns the compiler used to compile production code.
     */
    private final JkJavaCompiler productionCompiler() {
        return baseCompiler.withOutputDir(structure.classDir())
                .andSources(structure.sources())
                .withClasspath(javaDeps.resolver().get(JkJavaDepScopes.SCOPES_FOR_COMPILATION));
    }

    /**
     * Returns the compiler used to compile unit tests.
     */
    private final JkJavaCompiler testCompiler() {
        return baseCompiler.withOutputDir(structure.testClassDir())
                .andSources(structure.testSources())
                .withClasspath(this.javaDeps.resolver().get(JkJavaDepScopes.SCOPES_FOR_TEST).andHead(structure.classDir()));
    }

    /**
     * Returns the object used to process unit tests.
     */
    public final JkUnit tester() {
        final JkClasspath classpath = JkClasspath.of(this.structure.testClassDir(), this.structure.classDir()).and(
                this.javaDeps.resolver().get(JkJavaDepScopes.SCOPES_FOR_TEST));
        final File junitReport = new File(this.structure.testReportDir(), "junit");
        return JkUnit.of(classpath).withReportDir(junitReport).withClassesToTest(this.structure.testClassDir());
    }

    public final JkJavaProjectPackager packager() {
        return this.packager;
    }

    public final JkJavadocMaker javadocMaker() {
        return this.javadocMaker;
    }

    // ---------------------------- build methods -------------------

    public void clean() {
        this.structure.deleteOutputDirs();
        this.packager.deleteArtifacts();
    }

    /** Generates sources and resources, compiles production sources and process production resources to the class directory. */
    public void compile() {
        productionCompiler().compile();
        JkResourceProcessor.of(structure.resources()).generateTo(structure.classDir());
    }

    /** Compiles and runs all unit tests. */
    public void test() {
        testCompiler().compile();
        JkResourceProcessor.of(structure.testResources()).generateTo(structure.testClassDir());
        tester().run();
    }

    /**
     * Produces jars. Suppose that the project has been compiled first.
     */
    public void pack() {
        this.packager.pack();
    }

}
