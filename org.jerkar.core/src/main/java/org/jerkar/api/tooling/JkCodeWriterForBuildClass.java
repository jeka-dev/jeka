package org.jerkar.api.tooling;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jerkar.api.depmanagement.JkDepExclude;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyExclusions;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkVersionProvider;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildDependencySupport;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Provides facilities to create code source for Build class. This is mainly
 * intended for scaffolding or migration purpose.
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class JkCodeWriterForBuildClass {

    private static final String LINE_JUMP = "\n";

    private final Writer writer = new Writer();

    /** The package name of the generated code */
    public String packageName;

    /** The class name of the generated code */
    public String className = "Build";

    /** The extended class. Can be null */
    public String extendedClass = "JkBuild";

    /**
     * The list of package/class to import.
     * It can me expressed as "my.pack.MyClass" or "my.pack.*".
     */
    public final List<String> imports = importsForJkBuild();

    /**
     * The list of package/class to import.
     * It can me expressed as "my.pack.MyClass" or "my.pack.*".
     */
    public final List<String> staticImports = new LinkedList<String>();

    /**
     * The list of module/files to add to JkImport class annotation
     */
    public final List<String> jkImports = new LinkedList<String>();

    private final Map<String, String> groupVersionVariableMap = new HashMap<String, String>();

    private final Map<String, String> publicFieldString = new HashMap<String, String>();

    /**
     * The moduleId declared in the generated build class. Can be null.
     */
    public JkModuleId moduleId;

    /**
     * The version declared in the generated build class. Can be null.
     */
    public String version;

    /**
     * The dependencies declared in the generated build class. Set it to <code>null</code> to
     * not generate #dependencies method.
     */
    public JkDependencies dependencies;

    /**
     * The repositories declared in the generated build class.
     */
    public JkRepos repos;

    /**
     * The version provider declared in the the generated build class.
     */
    public JkVersionProvider versionProvider;

    /**
     * The version dependency exclusions declared in the generated build class.
     */
    public JkDependencyExclusions dependencyExclusions;

    /** JkExtraPacking method code to be generated */
    public List<String> extraMethods = new LinkedList<String>();



    /**
     * When generating versionProvider method, if you want that the all the
     * moduleId for a given group map to a variable (instead of the version
     * literal), add an entry to this map as
     * <code>groupVersionVariableMap.put("my.group", "myGroupVersion")</code>
     * where "myGroupVersion" is a a field declared in
     * {@link #publicFieldString}
     */
    public void addGroupVersionVariable(String group, String version) {
        final String variableName = toVersionVariableName(group);
        groupVersionVariableMap.put(group, variableName);
        publicFieldString.put(variableName, version);
    }

    static String toVersionVariableName(String group) {
        final String[] items = group.split("\\.");
        final StringBuilder builder = new StringBuilder();
        builder.append(items[0].toLowerCase());
        for (int i = 1; i < items.length; i++) {
            builder.append(JkUtilsString.capitalize(items[i].toLowerCase()));
        }
        return builder.append("Version").toString();
    }

    /**
     * Returns the java code portion that declares imports for a basic
     * build class extending {@link JkJavaBuild}
     */
    public static List<String> importsForJkJavaBuild() {
        final List<String> imports = new LinkedList<String>();
        imports.add("org.jerkar.api.depmanagement.*");
        imports.add("org.jerkar.tool.builtins.javabuild.JkJavaBuild");
        return imports;
    }

    /**
     * Returns the java code portion that declares imports for a basic
     * build class extending {@link JkBuild}
     */
    public static List<String> importsForJkBuild() {
        final List<String> imports = new LinkedList<String>();
        imports.add("org.jerkar.tool.JkBuild");
        return imports;
    }

    /**
     * Returns the java code portion that declares imports for a basic
     * build class extending {@link JkBuildDependencySupport}
     */
    public static List<String> importsForJkDependencyBuildSupport() {
        final List<String> imports = new LinkedList<String>();
        imports.add("org.jerkar.api.depmanagement.*");
        imports.add("org.jerkar.tool.JkDependencyBuildSupport");
        return imports;
    }

    /**
     * Returns the entire class code except the last "}".
     */
    public String wholeClass() {
        final StringBuilder builder = new StringBuilder();
        if (!JkUtilsString.isBlank(packageName)) {
            builder.append(writer.packageDeclaration(packageName)).append(LINE_JUMP);
        }
        builder.append(writer.imports(imports)).append(LINE_JUMP);
        if (!staticImports.isEmpty()) {
            builder.append(writer.staticImports(staticImports)).append(LINE_JUMP);
        }
        builder.append(writer.classDeclaration(this.jkImports, className, extendedClass)).append(LINE_JUMP);
        if (!this.publicFieldString.isEmpty()) {
            builder.append(writer.publicStringFields(publicFieldString)).append(LINE_JUMP);
        }

        if (moduleId != null) {
            builder.append(writer.moduleId(moduleId)).append(LINE_JUMP).append(LINE_JUMP);
        }
        if (version != null) {
            builder.append(writer.version(version)).append(LINE_JUMP).append(LINE_JUMP);
        }
        if (dependencies != null) {
            builder.append(writer.dependencies(dependencies)).append(LINE_JUMP);
        }

        if (repos != null) {
            builder.append(writer.downloadRepositories(repos));
            builder.append(LINE_JUMP);
        }
        if (versionProvider != null && !versionProvider.isEmpty()) {
            builder.append(writer.versionProvider(versionProvider, groupVersionVariableMap))
            .append(LINE_JUMP);
        }
        if (dependencyExclusions != null && !dependencyExclusions.isEmpty()) {
            builder.append(writer.dependencyExclusions(dependencyExclusions)).append(LINE_JUMP);
        }
        for (final String extraMethod : extraMethods) {
            builder.append(extraMethod);
            builder.append(LINE_JUMP);
        }
        return builder.toString();
    }

    /**
     * Returns the last "}" that close the class.
     * @return
     */
    public String endClass() {
        return writer.endClass();
    }

    /**
     * Returns the entire class code.
     */
    @Override
    public String toString() {
        return wholeClass() + endClass();
    }

    private static class Writer {

        public String publicStringFields(Map<String, String> nameToValue) {
            final SortedMap<String, String> sortedMap = new TreeMap<String, String>();
            sortedMap.putAll(nameToValue);
            final StringBuilder builder = new StringBuilder();
            for (final String constantName : sortedMap.keySet()) {
                builder.append("    public String ").append(constantName).append(" = \"")
                .append(sortedMap.get(constantName)).append("\";\n\n");
            }
            return builder.toString();
        }

        public String packageDeclaration(String packageName) {
            if (JkUtilsString.isBlank(packageName)) {
                return "";
            }
            return "package " + packageName + ";";
        }

        public String imports(List<String> imports) {
            final List<String> list = new LinkedList<String>(imports);
            Collections.sort(list);
            final StringBuilder builder = new StringBuilder();
            for (final String item : list) {
                builder.append("import ").append(item).append(";\n");
            }
            return builder.toString();
        }

        public String staticImports(List<String> imports) {
            final List<String> list = new LinkedList<String>(imports);
            Collections.sort(list);
            final StringBuilder builder = new StringBuilder();
            for (final String item : list) {
                builder.append("import static ").append(item).append(";\n");
            }
            return builder.toString();
        }

        public String classDeclaration(List<String> jkImports, String className, String extendedClass) {
            final StringBuilder builder = new StringBuilder().append("/**\n")
                    .append(" * @formatter:off\n").append(" */\n");
            if (!jkImports.isEmpty()) {
                builder.append(jkImportCode(jkImports)).append("\n");
            }
            builder.append("class " + className + " extends " + extendedClass)
            .append(" {").append("\n");
            return builder.toString();
        }

        public String endClass() {
            return "}";
        }

        String jkImportCode(List<String> imports) {
            final StringBuilder builder = new StringBuilder();
            builder.append("@JkImport({");
            for(final String item : imports) {
                builder.append("\"").append(item).append("\", ");
            }
            builder.delete(builder.length()-2, builder.length());
            builder.append("})");
            return builder.toString();
        }

        public String moduleId(JkModuleId moduleId) {
            return new StringBuilder()
            .append("    @Override\n")
            .append("    public JkModuleId moduleId() {\n")
            .append("        return JkModuleId.of(\"" + moduleId.group() + "\", \""
                    + moduleId.name() + "\");\n").append("    }").toString();
        }

        public String version(String version) {
            return new StringBuilder().append("    @Override\n")
                    .append("    public JkVersion version() {\n")
                    .append("        return JkVersion.ofName(\"" + version + "\");\n")
                    .append("    }").toString();
        }

        public String dependencies(JkDependencies dependencies) {
            final StringBuilder builder = new StringBuilder().append("    @Override\n")
                    .append("    public JkDependencies dependencies() {\n")
                    .append("        return ").append(dependencies.toJavaCode(8)).append("\n    }")
                    .append("\n");
            return builder.toString();
        }

        public String downloadRepositories(JkRepos repos) {
            if (repos.isEmpty()) {
                return null;
            }
            final StringBuilder builder = new StringBuilder().append("    @Override\n")
                    .append("    public JkRepos downloadRepositories() {\n")
                    .append("        return JkRepos.maven(");
            for (final JkRepo repo : repos) {
                builder.append("\"").append(repo.url().toString()).append("\", ");
            }
            builder.delete(builder.length() - 2, builder.length());
            builder.append(");\n");
            builder.append("    }");
            return builder.toString();
        }

        public String versionProvider(JkVersionProvider versionProvider,
                Map<String, String> groupVersionConstants) {
            if (versionProvider.moduleIds().isEmpty()) {
                return null;
            }
            final StringBuilder builder = new StringBuilder().append("    @Override\n")
                    .append("    public JkVersionProvider versionProvider() {\n")
                    .append("        return JkVersionProvider.of()");
            final List<JkModuleId> moduleIds = JkUtilsIterable.listOf(versionProvider.moduleIds());
            Collections.sort(moduleIds, JkModuleId.GROUP_NAME_COMPARATOR);
            for (final JkModuleId moduleId : moduleIds) {
                builder.append("\n            .and(\"").append(moduleId.groupAndName())
                .append("\", ");
                final String constant = groupVersionConstants.get(moduleId.group());
                if (constant != null) {
                    builder.append(constant);
                } else {
                    builder.append("\"").append(versionProvider.versionOf(moduleId)).append("\"");
                }
                builder.append(")");
            }
            builder.append(";\n");
            builder.append("    }");
            return builder.toString();
        }

        public String dependencyExclusions(JkDependencyExclusions exclusions) {
            if (exclusions.isEmpty()) {
                return null;
            }
            final StringBuilder builder = new StringBuilder().append("    @Override\n")
                    .append("    public JkDependencyExclusions dependencyExclusions() {\n")
                    .append("        return JkDependencyExclusions.builder()");
            final List<JkModuleId> moduleIds = JkUtilsIterable.listOf(exclusions.moduleIds());
            Collections.sort(moduleIds, JkModuleId.GROUP_NAME_COMPARATOR);
            for (final JkModuleId moduleId : moduleIds) {
                builder.append("\n            .on(\"").append(moduleId.groupAndName()).append("\"");
                for (final JkDepExclude depExclude : exclusions.get(moduleId)) {
                    builder.append(", \"").append(depExclude.moduleId().groupAndName())
                    .append("\"");
                }
                builder.append(")");
            }
            builder.append(".build();\n");
            builder.append("    }");
            return builder.toString();
        }

    }

}
