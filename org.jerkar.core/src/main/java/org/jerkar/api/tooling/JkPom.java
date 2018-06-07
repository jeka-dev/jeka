package org.jerkar.api.tooling;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.depmanagement.JkDepExclude;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyExclusions;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.depmanagement.JkVersionProvider;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Wraps a POM file (Ideally an effective POM file) and provides convenient methods to extract
 * information jump.
 *
 * @author Jerome Angibaud
 */
public final class JkPom {

    private final Document pomDoc;

    private JkPom(Document pomDoc) {
        super();
        this.pomDoc = pomDoc;
    }

    /**
     * Creates a {@link JkPom} jump a POM file, ideally an effective POM file.
     */
    public static JkPom of(Path file) {
        final Document document = JkUtilsXml.documentFrom(file);
        return new JkPom(document);
    }

    private Element dependenciesElement() {
        return JkUtilsXml.directChild(projectEl(), "dependencies");
    }

    private Element dependencyManagementEl() {
        return JkUtilsXml.directChild(projectEl(), "dependencyManagement");
    }

    private Element repositoriesEl() {
        return JkUtilsXml.directChild(projectEl(), "repositories");
    }

    private Element projectEl() {
        return pomDoc.getDocumentElement();
    }

    /**
     * The groupId for this POM.
     */
    public String groupId() {
        return JkUtilsXml.directChildText(projectEl(), "groupId");
    }

    /**
     * The artifzctId for this POM.
     */
    public String artifactId() {
        return JkUtilsXml.directChildText(projectEl(), "artifactId");
    }

    /**
     * The projectVersion for this POM.
     */
    public String version() {
        return JkUtilsXml.directChildText(projectEl(), "version");
    }

    /**
     * The dependencies declared in this POM.
     */
    public JkDependencies dependencies() {
        return dependencies(dependenciesElement());
    }

    /**
     * The map groupId:ArtifactId -> projectVersion provideded by the <code>dependencyManagement</code>
     * section of this POM.
     */
    public JkVersionProvider versionProvider() {
        final List<JkVersionedModule> versionedModules = new LinkedList<>();
        if (dependencyManagementEl() == null) {
            return JkVersionProvider.empty();
        }
        final Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl(),
                "dependencies");
        final JkDependencies dependencies = dependencies(dependenciesEl);
        for (final JkScopedDependency scopedDependency : dependencies) {
            final JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency
                    .dependency();
            final JkVersionedModule versionedModule = JkVersionedModule.of(
                    moduleDependency.moduleId(),
                    JkVersion.name(moduleDependency.versionRange().definition()));
            versionedModules.add(versionedModule);
        }
        return JkVersionProvider.of(versionedModules);
    }

    /**
     * The {@link JkDependencyExclusions} instance provided by the <code>dependencyManagement</code>
     * section of this POM.
     */
    public JkDependencyExclusions dependencyExclusion() {
        final JkDependencyExclusions.Builder builder = JkDependencyExclusions.builder();
        if (dependencyManagementEl() == null) {
            return builder.build();
        }
        final Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl(),
                "dependencies");
        final JkDependencies dependencies = dependencies(dependenciesEl);
        for (final JkScopedDependency scopedDependency : dependencies) {
            final JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency
                    .dependency();
            if (!moduleDependency.excludes().isEmpty()) {
                builder.on(moduleDependency.moduleId(), moduleDependency.excludes());
            }
        }
        return builder.build();
    }

    /**
     * Repositories declared in this POM.
     */
    public JkRepos repos() {
        final List<String> urls = new LinkedList<>();
        if (repositoriesEl() == null) {
            return JkRepos.empty();
        }
        for (final Element repositoryEl : JkUtilsXml.directChildren(repositoriesEl(), "repository")) {
            urls.add(JkUtilsXml.directChildText(repositoryEl, "url"));
        }
        return JkRepos.of(JkUtilsIterable.arrayOf(urls, String.class));
    }

    private JkDependencies dependencies(Element dependenciesEl) {
        final JkDependencies.Builder builder = JkDependencies.builder();
        for (final Element dependencyEl : JkUtilsXml.directChildren(dependenciesEl, "dependency")) {
            builder.on(jkDependency(dependencyEl));
        }
        return builder.build();
    }

    private JkScopedDependency jkDependency(Element mvnDependency) {
        final String groupId = JkUtilsXml.directChildText(mvnDependency, "groupId");
        final String artifactId = JkUtilsXml.directChildText(mvnDependency, "artifactId");
        final String version = JkUtilsXml.directChildText(mvnDependency, "version");
        JkModuleDependency moduleDependency = JkModuleDependency.of(groupId, artifactId, version);
        final String type = JkUtilsXml.directChildText(mvnDependency, "type");
        if (type != null) {
            moduleDependency = moduleDependency.ext(type);
        }
        final String classifier = JkUtilsXml.directChildText(mvnDependency, "classifier");
        if (classifier != null) {
            moduleDependency = moduleDependency.classifier(classifier);
        }
        final Element exclusionsEl = JkUtilsXml.directChild(mvnDependency, "exclusions");
        if (exclusionsEl != null) {
            for (final Element exclusionElement : JkUtilsXml.directChildren(exclusionsEl,
                    "exclusion")) {
                moduleDependency = moduleDependency.andExclude(jkDepExclude(exclusionElement));
            }
        }
        final String scope = JkUtilsXml.directChildText(mvnDependency, "scope");
        final JkScope jkScope = (scope == null) ? JkScope.of("compile") : JkScope.of(scope);
        return JkScopedDependency.of(moduleDependency, jkScope);
    }

    private JkDepExclude jkDepExclude(Element exclusionEl) {
        final String groupId = JkUtilsXml.directChildText(exclusionEl, "groupId");
        final String artifactId = JkUtilsXml.directChildText(exclusionEl, "artifactId");
        return JkDepExclude.of(groupId, artifactId);

    }

    /**
     * The Jerkar build class source equivalent to this POM.
     */
    public String jerkarSourceCode(JkPathTree baseDir) {
        final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
        codeWriter.moduleId = JkModuleId.of(groupId(), artifactId());
        codeWriter.dependencies = dependencies();
        codeWriter.dependencyExclusions = dependencyExclusion();
        codeWriter.extendedClass = "JkJavaProjectBuild";
        codeWriter.imports.clear();
        codeWriter.imports.addAll(JkCodeWriterForBuildClass.importsForJkJavaBuild());
        codeWriter.staticImports.addAll(JkCodeWriterForBuildClass.staticImportsForJkJavaBuild());
        codeWriter.repos = null;
        codeWriter.version = version();
        codeWriter.versionProvider = versionProvider();
        if (baseDir.goTo("src/main/resources").exists()) {
            codeWriter.imports.add(JkPathTreeSet.class.getName());
            codeWriter.extraMethods.add(
                    "    // If you move your resources to src/main/java (collocated with java classes code), \n" +
                            "    // you can remove this method. \n" +
                            "    @Override\n" +
                            "    public JkPathTreeSet resources() {\n" +
                            "        return baseDirAsTree().jump(\"src/main/resources\").asSet();\n" +
                    "    }");
        }
        if (baseDir.goTo("src/test/resources").exists()) {
            codeWriter.imports.add(JkPathTreeSet.class.getName());
            codeWriter.extraMethods.add(
                    "    // If you move your test resources to src/test/java (collocated with java classes code), \n" +
                            "    // you can remove this method.\n" +
                            "    @Override\n" +
                            "    public JkPathTreeSet unitTestResources() {\n" +
                            "        return baseDirAsTree().jump(\"src/test/resources\").asSet();\n" +
                    "    }");
        }
        final VersionConstanter constanter = VersionConstanter.of(codeWriter.versionProvider);
        for (final Map.Entry<String, String> entry : constanter.groupToVersion().entrySet()) {
            codeWriter.addGroupVersionVariable(entry.getKey(), entry.getValue());
        }
        return codeWriter.wholeClass() + codeWriter.endClass();
    }

}
