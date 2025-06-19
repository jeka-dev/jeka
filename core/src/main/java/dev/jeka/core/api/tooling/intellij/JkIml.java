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

package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.marshalling.xml.JkDomElement;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Object model for IntelliJ iml files.
 */
public final class JkIml {

    private Path moduleDir = Paths.get("");

    // when generating iml for a sub 'jeka-src' module, we need to relativize paths to $MODULE_DIR$/.. instead
    // of $MODULE_DIR$
    private boolean isForJekaSrc;

    public final Component component = new Component();

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

    public JkIml setIsModuleBaseJekaSrc(boolean isForJekaSrc) {
        this.isForJekaSrc = isForJekaSrc;
        return this;
    }

    public enum Scope {
        COMPILE, RUNTIME, TEST, PROVIDED
    }


    public class Component {

        private String output =".idea/output/production";

        private String outputTest = ".idea/output/test";

        private boolean excludeOutput = true;

        private String jdkName;

        private Content content = new Content();

        private List<OrderEntry> orderEntries = new LinkedList<>();

        public Content getContent() {
            return content;
        }

        public Component setOutput(String output) {
            this.output = output;
            return this;
        }

        public Component setOutputTest(String outputTest) {
            this.outputTest = outputTest;
            return this;
        }

        public Component setExcludeOutput(boolean excludeOutput) {
            this.excludeOutput = excludeOutput;
            return this;
        }

        public Component setJdkName(String jdkName) {
            this.jdkName = jdkName;
            return this;
        }

        public Component setContent(Content content) {
            this.content = content;
            return this;
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

        /**
         * Replaces the specified library with the specified module. The library is specified
         * by the end of its path. For example, '-foo.bar'  will replace 'mylibs/core-foo.jar'
         * by the specified module. Only the first matching lib is replaced.
         */
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

        /**
         * Sets the <i>scope</i> and <i>exported</i> attribute to the specified module.
         * @param moduleName The module to set attributes on.
         * @param scope If null, scope remains unchanged.
         * @param exported If null, scope remains unchanged.
         * @return This object for  chaining.
         */
        public Component setModuleAttributes(String moduleName, Scope scope, Boolean exported) {
            orderEntries.forEach(orderEntry -> {
                        if (orderEntry instanceof ModuleOrderEntry) {
                            ModuleOrderEntry entry = (ModuleOrderEntry) orderEntry;
                            if (entry.moduleName.equals(moduleName)) {
                                if (scope != null) {
                                    entry.scope = scope;
                                }
                                if (exported != null) {
                                    entry.exported = exported;
                                }
                            }
                        }

                    });
            return this;
        }

        void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            boolean inheritedJdk = JkUtilsString.isBlank(jdkName) || "inheritedJdk".equals(jdkName);
            parent.add("component")
                .attr("name", "NewModuleRootManager")
                .attr("inherit-compileRunner-output", "false")
                    .applyIf(output != null, el -> el.add("output")
                            .attr("url", pathUrlResolver.ideaPath(false, moduleDir.resolve(output))))
                    .applyIf(outputTest != null, el -> el.add("output-test")
                            .attr("url", pathUrlResolver.ideaPath(false, moduleDir.resolve(outputTest))))
                    .applyIf(excludeOutput, el -> el.add("exclude-output"))
                    .apply(el -> content.append(el, pathUrlResolver))
                    .add("orderEntry")
                    .applyIf(inheritedJdk, e -> e.attr("type", "inheritedJdk"))
                    .applyIf(!inheritedJdk, e -> e.attr("type", "jdk").attr("jdkType", "JavaSDK").attr("jdkName", jdkName)).__
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

        public String excludePattern;

        public String url = "file://$MODULE_DIR$";

        public Content addSourceFolder(String path, boolean test, String type) {
            return addSourceFolder(moduleDir.resolve(path), test, type);
        }

        public Content addSourceFolder(Path path, boolean test, String type) {
            for (ListIterator<SourceFolder> it = sourceFolders.listIterator(); it.hasNext();) {
                SourceFolder sourceFolder = it.next();
                if (sourceFolder.getUrl().equals(path)) {
                    it.remove();
                    it.add(SourceFolder.of(path, test, type));
                    return this;
                }
            }
            sourceFolders.add(SourceFolder.of(path, test, type));
            return this;
        }

        public Content addExcludeFolder(String path) {
            excludeFolders.add(new ExcludeFolder(moduleDir.resolve(path)));
            return this;
        }

        /**
         * Adds and parameters standard JeKa folders
         */
        public Content addJekaStandards() {
            return this
                    .addSourceFolder("jeka-src", true, null)
                    .addExcludeFolder("jeka-output")
                    .addExcludeFolder(".jeka-work")
                    .addExcludeFolder(".idea/output");
        }

        void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            JkDomElement el = parent.add("content").attr("url", url);
            sourceFolders.forEach(sourceFolder -> sourceFolder.append(el, pathUrlResolver));
            excludeFolders.forEach(excludeFolder -> excludeFolder.append(el, pathUrlResolver));
            if (!JkUtilsString.isBlank(excludePattern)) {
                parent.add("excludePattern").attr("pattern", excludePattern);
            }
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

        public String getType() {
            return type;
        }

        void append(JkDomElement parent, PathUrlResolver pathUrlResolver) {
            JkDomElement el = parent.add("sourceFolder")
                    .attr("url", pathUrlResolver.ideaPath(true, path))
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
            JkDomElement el = parent.add("excludeFolder").attr("url", pathUrlResolver.ideaPath(true, path));
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
                        .add("root").attr("url", pathUrlResolver.ideaPath(true, classes)).__.__
                    .add("JAVADOC")
                        .applyIf(javadoc != null, docEl -> docEl.add("root")
                                .attr("url", pathUrlResolver.ideaPath(true, javadoc)))
                    .__
                    .add("SOURCES")
                        .applyIf(sources != null, srcEl -> srcEl.add("root")
                                .attr("url", pathUrlResolver.ideaPath(true, sources)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModuleLibraryOrderEntry that = (ModuleLibraryOrderEntry) o;

            return classes.equals(that.classes);
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

            return moduleName.equals(that.moduleName);
        }

        @Override
        public int hashCode() {
            return moduleName.hashCode();
        }
    }

    public JkDomDocument toDoc() {
        JkDomDocument doc = JkDomDocument.of("module");
        doc.root().attr("type", "JAVA_MODULE").attr("version", "4")
                .apply(el -> component.append(el, this.pathUrlResolver));
        return doc;
    }

    public class PathUrlResolver {

        private final Map<String, Path> substitutes = new LinkedHashMap<>();

        void setPathSubstitute(Path jekaCacheDir) {
            substitutes.put("MODULE_DIR", JkIml.this.moduleDir);
            substitutes.put("JEKA_CACHE_DIR", jekaCacheDir);
        }

        String ideaPath(boolean takeJekaSrcInAccount, Path file) {
            boolean jarFile = file.getFileName().toString().toLowerCase().endsWith(".jar");
            String type = jarFile ? "jar" : "file";
            String result = type + "://" + substitutedVarPath(takeJekaSrcInAccount, file).toString()
                    .replace('\\', '/');
            if (jarFile) {
                result = result + "!/";
            }
            return result;
        }

        private Path substitutedVarPath(boolean takeJekaSrcInAccount, Path original) {
            if (!original.isAbsolute()) {
                Path moduleDirRelativePath = moduleDir.toAbsolutePath().normalize()
                        .relativize(original.toAbsolutePath().normalize());
                String rootDirString = (isForJekaSrc && takeJekaSrcInAccount) ? "$MODULE_DIR$/.." : "$MODULE_DIR$";
                return Paths.get(rootDirString).resolve(moduleDirRelativePath);
            }
            Path normalized  = original.normalize();
            return substitutes.entrySet().stream()
                    .filter(pathStringEntry -> pathStringEntry.getValue() != null)
                    .filter(pathStringEntry -> normalized.startsWith(pathStringEntry.getValue().toAbsolutePath()))
                    .findFirst()
                    .map(entry -> {
                        String entryKey = "$" + entry.getKey() + "$";
                        if (isForJekaSrc && takeJekaSrcInAccount && "MODULE_DIR".equals(entry.getKey())) {
                            entryKey = entryKey + "/..";
                        }
                        return Paths.get(entryKey)
                                .resolve(entry.getValue().toAbsolutePath().relativize(normalized));
                    })
                    .orElse(normalized);
        }
    }

}
