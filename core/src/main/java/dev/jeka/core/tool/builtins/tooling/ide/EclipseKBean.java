/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.tool.builtins.tooling.ide;


import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.eclipse.JkEclipseClasspathGenerator;
import dev.jeka.core.api.tooling.eclipse.JkEclipseProjectGenerator;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@JkDoc("Manages Eclipse metadata files.")
@JkDocUrl("https://jeka-dev.github.io/jeka/reference/kbeans-eclipse/")
public final class EclipseKBean extends KBean {

    @JkDoc("If true, .classpath will include javadoc reference for declared dependencies.")
    boolean javadoc = false;

    /** If not null, this value will be used as the JRE container path when generating .classpath file.*/
    @JkDoc("If not null, this value will be used as the JRE container path in .classpath.")
    public String jreContainer = null;

    /** Flag to set whether 'all' task should use absolute paths instead of classpath variables */
    @JkDoc("If true, dependency paths will be expressed relatively to Eclipse path variables instead of absolute paths.")
    public boolean useVarPath = true;

    private final Map<JkDependency, Properties> attributes = new HashMap<>();

    private final Map<JkDependency, Properties> accessRules = new HashMap<>();

    // ------------------------- setters ----------------------------

    /** Set the JRE container to the Eclipse Standard VM type with the desired name. */
    public void setStandardJREContainer(String jreName) {
        jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + jreName;
    }

    // ------------------------ plugin methods ----------------------


    /**
     * @deprecated Use {@link #sync()} instead.
     */
    @JkDoc(value = "Deprecated: use 'sync' instead.")
    @Deprecated
    public void files() {
        sync();
    }

    @JkDoc("Generates Eclipse files (.classpath and .project) in the current directory. The files reflect project " +
            "dependencies and source layout.")
    public void sync() {
        final Path dotProject = getBaseDir().resolve(".project");
        JkIdeSupport projectIde = IdeSupport.getProjectIde(getRunbase());
        if (projectIde != null) {
            final List<Path> importedRunProjects = new LinkedList<>();
            /*
            for (final KBean importedKBean : getImportedKBeans().get(false)) {
                importedRunProjects.add(importedKBean.getBaseDir());
            }
            */
            final JkEclipseClasspathGenerator classpathGenerator =
                    JkEclipseClasspathGenerator.of(projectIde);

            classpathGenerator.setJekaSrcDependencies(getRunbase().getDependencyResolver(), IdeSupport.classpathAsDependencySet());
            classpathGenerator.setIncludeJavadoc(this.javadoc);
            classpathGenerator.setJreContainer(this.jreContainer);
            classpathGenerator.setImportedProjects(importedRunProjects);
            classpathGenerator.setUsePathVariables(this.useVarPath);
            this.attributes.forEach(classpathGenerator::addAttributes);
            this.accessRules.forEach(classpathGenerator::addAccessRules);
            final String result = classpathGenerator.generate();
            final Path dotClasspath = getBaseDir().resolve(".classpath");
            JkUtilsPath.write(dotClasspath, result.getBytes(StandardCharsets.UTF_8));
            JkLog.info("File " + dotClasspath + " generated.");

            if (!Files.exists(dotProject)) {
                JkEclipseProjectGenerator.ofJavaNature(getBaseDir().getFileName().toString())
                        .writeTo(dotProject);
                JkLog.info("File " + dotProject + " generated.");
            }
        } else {
            if (!Files.exists(dotProject)) {
                JkEclipseProjectGenerator.ofSimpleNature(getBaseDir().getFileName().toString())
                        .writeTo(dotProject);
                JkLog.info("File " + dotProject + " generated.");
            }
        }
    }

    @JkDoc("Generates Eclipse files (.project and .classpath) on all sub-folders of the current directory. Only sub-folders having a jeka-src directory are taken in account. See eclipse#files.")
    public void all() {
        final Iterable<Path> folders = JkPathTree.of(getBaseDir())
                .andMatching(true,"**/" + JkConstants.JEKA_SRC_DIR, JkConstants.JEKA_SRC_DIR)
                .andMatching(false,"**/" + JkConstants.OUTPUT_PATH + "/**")
                .getFiles();
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.startTask("Generate Eclipse files on " + projectFolder);
            Main.exec(projectFolder, "eclipse#files");
            JkLog.endTask();
        }
    }


    // -------------------------- Setters

    /**
     * For the specified dependency, specify a child attribute tag to add to the mapping classpathentry tag.
     * @param dependency The dependency paired to the classpathentry we want generate `<attributes></attributes>` children
     *                   for. It can be a {@link dev.jeka.core.api.depmanagement.JkCoordinateDependency} or a
     *                   {@link dev.jeka.core.api.depmanagement.JkFileSystemDependency}.
     *                   If it is a module dependency, it can be a direct or transitive dependency and only group:name
     *                   is relevant.
     */
    public EclipseKBean addAttribute(JkDependency dependency, String name, String value) {
        this.attributes.putIfAbsent(dependency, new Properties());
        this.attributes.get(dependency).put(name, value);
        return this;
    }

    /**
     * For the specified dependency, specify a child accessrule tag to add to the mapping classpathentry tag.
     * @param dependency The dependency paired to the classpathentry we want generate `<attributes></attributes>` children
     *                   for. It can be a {@link dev.jeka.core.api.depmanagement.JkCoordinateDependency} or a
     *                   {@link dev.jeka.core.api.depmanagement.JkFileSystemDependency}.
     *                   If it is a module dependency, it can be a direct or transitive dependency and only group:name
     *                   is relevant.
     */
    public EclipseKBean addAccessRule(JkDependency dependency, String kind, String pattern) {
        this.accessRules.putIfAbsent(dependency, new Properties());
        this.accessRules.get(dependency).put(kind, pattern);
        return this;
    }

}
