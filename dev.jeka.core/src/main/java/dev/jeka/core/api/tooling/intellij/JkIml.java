package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.marshalling.JkDomDocument;
import dev.jeka.core.api.marshalling.JkDomElement;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Object model for IntelliJ iml files.
 */
public final class JkIml {

    private Path moduleDir = Paths.get("");

    private Component component = new Component();

    final PathUrlResolver pathUrlResolver = new PathUrlResolver();

    private JkIml() {
    }

    public static JkIml of() {
        return new JkIml();
    }

    public JkIml setModuleDir(Path path) {
        this.moduleDir = path;
        return this;
    }

    public Component getComponent() {
        return component;
    }

    public enum Scope {
        COMPILE, RUNTIME, TEST, PROVIDED
    }

    public class Component {

        private String output =".idea/output/production";

        private String outputTest = ".idea/output/test";

        private boolean excludeOutput = true;

        private Content content = new Content();

        private List<OrderEntry> orderEntries = new LinkedList<>();

        public Content getContent() {
            return content;
        }

        public List<OrderEntry> getOrderEntries() {
            return orderEntries;
        }

        public Component addModuleLibraryOrderEntry(Path path, Scope scope) {
            ModuleLibraryOrderEntry moduleLibraryOrderEntry = ModuleLibraryOrderEntry.of()
                    .setClasses(path).setExported(true).setScope(scope);
            orderEntries.add(moduleLibraryOrderEntry);
            return this;
        }

        public Component addModuleOrderEntry(String moduleName, Scope scope) {
            ModuleOrderEntry moduleOrderEntry = ModuleOrderEntry.of()
                    .setModuleName(moduleName).setExported(true).setScope(scope);
            orderEntries.add(moduleOrderEntry);
            return this;
        }

        public Component replaceLibByModule(String libPathEndsWithFilter, String moduleName) {
            List<OrderEntry> result = orderEntries.stream()
                    .map(orderEntry -> {
                        if (orderEntry instanceof ModuleLibraryOrderEntry) {
                            ModuleLibraryOrderEntry entry = (ModuleLibraryOrderEntry) orderEntry;
                            if (entry.classes.endsWith(libPathEndsWithFilter)) {
                                return new ModuleOrderEntry()
                                        .setModuleName(moduleName)
                                        .setExported(entry.exported)
                                        .setScope(entry.scope);
                            }
                        }
                        return orderEntry;
                    })
                    .collect(Collectors.toList());
            this.orderEntries = result;
            return this;
        }

        void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            parent.add("component")
                .attr("name", "NewModuleRootManager")
                .attr("inherit-compileRunner-output", "false")
                    .applyIf(output != null, el -> el.add("output")
                            .attr("url", pathUrlResolver.ideaPath(moduleDir.resolve(output))))
                    .applyIf(outputTest != null, el -> el.add("output-test")
                            .attr("url", pathUrlResolver.ideaPath(moduleDir.resolve(outputTest))))
                    .applyIf(excludeOutput, el -> el.add("exclude-output"))
                    .apply(el -> content.append(el, pathUrlResolver))
                    .add("orderEntry").attr("type", "inheritedJdk").__
                    .add("orderEntry")
                        .attr("forTests", "false")
                        .attr("type", "sourceFolder").__
                    .apply(el -> orderEntries.stream()
                            .distinct().forEach(orderEntry -> orderEntry.append(el, pathUrlResolver)));
        }
    }

    public class Content {

        private final List<SourceFolder> sourceFolders = new LinkedList<>();

        private final List<ExcludeFolder> excludeFolders = new LinkedList<>();

        public List<SourceFolder> getSourceFolders() {
            return sourceFolders;
        }

        public List<ExcludeFolder> getExcludeFolders() {
            return excludeFolders;
        }

        public Content addSourceFolder(String path, boolean test, String type) {
            return addSourceFolder(moduleDir.resolve(path), test, type);
        }

        public Content addSourceFolder(Path path, boolean test, String type) {
            sourceFolders.add(SourceFolder.of(path, test, type));
            return this;
        }

        public Content addExcludeFolder(String path) {
            excludeFolders.add(new ExcludeFolder(moduleDir.resolve(path)));
            return this;
        }

        public Content addJekaStandards() {
            return this
                    .addSourceFolder("jeka/def", true, null)
                    .addExcludeFolder("jeka/output")
                    .addExcludeFolder("jeka/.work")
                    .addExcludeFolder(".idea/output");
        }

        void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            JkDomElement el = parent.add("content").attr("url", "file://$MODULE_DIR$");
            sourceFolders.forEach(sourceFolder -> sourceFolder.append(el, pathUrlResolver));
            excludeFolders.forEach(excludeFolder -> excludeFolder.append(el, pathUrlResolver));
        }
    }

    public static class SourceFolder {

        private final Path path;

        private final boolean isTest;

        private final String type;

        private SourceFolder(Path path, boolean isTest, String type) {
            this.path = path;
            this.isTest = isTest;
            this.type = type;
        }

        public static SourceFolder of(Path path, boolean isTest, String type) {
            return new SourceFolder(path, isTest, type);
        }

        public static SourceFolder of(Path path, boolean isTest) {
            return of(path, isTest, null);
        }

        public Path getUrl() {
            return path;
        }

        public boolean isTest() {
            return isTest;
        }

        void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            JkDomElement el = parent.add("sourceFolder")
                    .attr("url", pathUrlResolver.ideaPath(path))
                    .attr("type", type);
            if (isTest) {
                el.attr("isTestSource", "true");
            }
        }
    }

    public static class ExcludeFolder {

        private final Path path;

        public ExcludeFolder(Path path) {
            this.path = path;
        }

        public Path getPath() {
            return path;
        }

        void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            JkDomElement el = parent.add("excludeFolder").attr("url", pathUrlResolver.ideaPath(path));
        }
    }

    interface OrderEntry {

        default void enrichAttribute(JkDomElement el, Scope scope, boolean exported) {
            if (scope != null) {
                el.attr("scope", scope.name());
            }
            if (exported) {
                el.attr("exported", "");
            }
        }

        void append(JkDomElement parent, PathUrlResolver pathUrlResolver);
    }

    public static class ModuleLibraryOrderEntry implements OrderEntry {

        private Path classes;

        private Path sources;

        private Path javadoc;

        private Scope scope;

        private boolean exported;

        private ModuleLibraryOrderEntry() {
        }

        public static ModuleLibraryOrderEntry of() {
            return new ModuleLibraryOrderEntry();
        }

        ModuleLibraryOrderEntry copy() {
            ModuleLibraryOrderEntry copy = new ModuleLibraryOrderEntry();
            copy.classes = classes;
            copy.sources = sources;
            copy.javadoc = javadoc;
            copy.scope = scope;
            copy.exported = exported;
            return copy;
        }

        public Path getClasses() {
            return classes;
        }

        public ModuleLibraryOrderEntry setClasses(Path classes) {
            this.classes = classes;
            return this;
        }

        public Path getSources() {
            return sources;
        }

        public ModuleLibraryOrderEntry setSources(Path sources) {
            this.sources = sources;
            return this;
        }

        public Path getJavadoc() {
            return javadoc;
        }

        public ModuleLibraryOrderEntry setJavadoc(Path javadoc) {
            this.javadoc = javadoc;
            return this;
        }

        public Scope getScope() {
            return scope;
        }

        public ModuleLibraryOrderEntry setScope(Scope scope) {
            this.scope = scope;
            return this;
        }

        public boolean isExported() {
            return exported;
        }

        public ModuleLibraryOrderEntry setExported(boolean exported) {
            this.exported = exported;
            return this;
        }

        public void assertValid() {
            JkUtilsAssert.state(classes !=  null, "classesUrl must not be null");
        }

        public void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            assertValid();
            JkDomElement el = parent.add("orderEntry");
            enrichAttribute(el, scope, exported);
            el.attr("type", "module-library");
            el.add("library")
                    .add("CLASSES")
                        .add("root").attr("url", pathUrlResolver.ideaPath(classes)).__.__
                    .add("JAVADOC")
                        .applyIf(javadoc != null, docEl -> docEl.add("root")
                                .attr("url", pathUrlResolver.ideaPath(javadoc)))
                    .__
                    .add("SOURCES")
                        .applyIf(sources != null, srcEl -> srcEl.add("root")
                                .attr("url", pathUrlResolver.ideaPath(sources)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModuleLibraryOrderEntry that = (ModuleLibraryOrderEntry) o;

            if (!classes.equals(that.classes)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return classes.hashCode();
        }
    }

    public static class ModuleOrderEntry implements OrderEntry {

        private String moduleName;

        private Scope scope;

        private boolean exported;

        private ModuleOrderEntry() {
        }

        public static ModuleOrderEntry of() {
            return new ModuleOrderEntry();
        }

        public String getModuleName() {
            return moduleName;
        }

        public ModuleOrderEntry setModuleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Scope getScope() {
            return scope;
        }

        public ModuleOrderEntry setScope(Scope scope) {
            this.scope = scope;
            return this;
        }

        public boolean isExported() {
            return exported;
        }

        public ModuleOrderEntry setExported(boolean exported) {
            this.exported = exported;
            return this;
        }

        public void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            JkDomElement el = parent.add("orderEntry")
                    .attr("type", "module")
                    .attr("module-name", moduleName);
            enrichAttribute(el, scope, exported);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModuleOrderEntry that = (ModuleOrderEntry) o;

            if (!moduleName.equals(that.moduleName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return moduleName.hashCode();
        }
    }

    private static String url(String relPath) {
        return "file://$MODULE_DIR$/" + relPath;
    }

    public JkDomDocument toDoc() {
        JkDomDocument doc = JkDomDocument.of("module");
        doc.root().attr("type", "JAVA_MODULE").attr("version", "4")
                .apply(el -> component.append(el, this.pathUrlResolver));
        return doc;
    }

    public class PathUrlResolver {

        private Map<String, Path> substitutes = new LinkedHashMap<>();

        void setPathSubstitute(Path jekaCacheDir) {
            substitutes.put("MODULE_DIR", JkIml.this.moduleDir);
            substitutes.put("JEKA_CACHE_DIR", jekaCacheDir);
        }

        String ideaPath(Path file) {
            boolean jarFile = file.getFileName().toString().toLowerCase().endsWith(".jar");
            String type = jarFile ? "jar" : "file";
            String result = type + "://" + substitutedVarPath(file).toString()
                    .replace('\\', '/');
            if (jarFile) {
                result = result + "!/";
            }
            return result;
        }

        private Path substitutedVarPath(Path original) {
            if (!original.isAbsolute()) {
                Path moduleDirRelativePath = moduleDir.toAbsolutePath().normalize()
                        .relativize(original.toAbsolutePath().normalize());
                return Paths.get("$MODULE_DIR$").resolve(moduleDirRelativePath);
            }
            Path normalized  = original.normalize();
            return substitutes.entrySet().stream()
                    .filter(pathStringEntry -> pathStringEntry.getValue() != null)
                    .filter(pathStringEntry -> normalized.startsWith(pathStringEntry.getValue()))
                    .findFirst()
                    .map(entry -> Paths.get("$" + entry.getKey() + "$").resolve(entry.getValue().relativize(normalized)))
                    .orElse(normalized);
        }
    }

}
