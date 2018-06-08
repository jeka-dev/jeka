package org.jerkar.tool;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import org.jerkar.api.system.JkEvent;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Defines build classes defined on a given project.
 *
 * @author Jerome Angibaud
 */
final class ProjectDef {

    private final List<Class<?>> buildClasses;

    private ProjectDef(List<Class<?>> buildClasses) {
        super();
        this.buildClasses = Collections.unmodifiableList(buildClasses);
    }

    public void logAvailableBuildClasses() {
        int i = 0;
        for (final Class<?> classDef : this.buildClasses) {
            final String defaultMessage = (i == 0) ? " (default)" : "";
            final JkDoc jkDoc = classDef.getAnnotation(JkDoc.class);
            final String desc;
            if (jkDoc != null) {
                desc = JkUtilsString.join(jkDoc.value(), "\n");
            } else {
                desc = "No description available";
            }
            JkEvent.info(this,classDef.getName() + defaultMessage + " : " + desc);
            i++;
        }
    }

    /**
     * Defines methods and options available on a given build class.
     *
     * @author Jerome Angibaud
     */
    static final class BuildClassDef {

        private final List<BuildMethodDef> methodDefs;

        private final List<BuildOptionDef> optionDefs;

        private final Object buildOrPlugin;

        private BuildClassDef(Object buildOrPlugin, List<BuildMethodDef> methodDefs,
                              List<BuildOptionDef> optionDefs) {
            super();
            this.buildOrPlugin = buildOrPlugin;
            this.methodDefs = Collections.unmodifiableList(methodDefs);
            this.optionDefs = Collections.unmodifiableList(optionDefs);
        }

        List<BuildMethodDef> methodDefinitions() {
            return methodDefs;
        }

        static BuildClassDef of(Object build) {
            final Class<?> clazz = build.getClass();
            final List<BuildMethodDef> methods = new LinkedList<>();
            for (final Method method : executableMethods(clazz)) {
                methods.add(BuildMethodDef.of(method));
            }
            Collections.sort(methods);
            final List<BuildOptionDef> options = new LinkedList<>();
            for (final NameAndField nameAndField : options(clazz, "", true, null)) {
                options.add(BuildOptionDef.of(build, nameAndField.field,
                        nameAndField.name, nameAndField.rootClass));
            }
            Collections.sort(options);
            return new BuildClassDef(build, methods, options);
        }

        private static List<Method> executableMethods(Class<?> clazz) {
            final List<Method> result = new LinkedList<>();
            for (final Method method : clazz.getMethods()) {
                final int modifier = method.getModifiers();
                if (method.getReturnType().equals(void.class) && method.getParameterTypes().length == 0
                        && !JkUtilsReflect.isMethodPublicIn(Object.class, method.getName())
                        && !Modifier.isAbstract(modifier) && !Modifier.isStatic(modifier)) {
                    result.add(method);
                }

            }
            return result;
        }

        private static List<NameAndField> options(Class<?> clazz, String prefix, boolean root, Class<?> rClass) {
            final List<NameAndField> result = new LinkedList<>();
            for (final Field field : JkUtilsReflect.getAllDeclaredField(clazz, JkDoc.class)) {
                final Class<?> rootClass = root ? field.getDeclaringClass() : rClass;
                if (!hasSubOption(field)) {
                    result.add(new NameAndField(prefix + field.getName(), field, rootClass));
                } else {
                    final List<NameAndField> subOpts = options(field.getType(), prefix + field.getName() + ".", false,
                            rootClass);
                    result.addAll(subOpts);
                }
            }
            return result;
        }

        private static boolean hasSubOption(Field field) {
            return !JkUtilsReflect.getAllDeclaredField(field.getType(), JkDoc.class).isEmpty();
        }

        String description(String prefix, boolean withHeader) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Class<? extends JkBuild> buildClass : this.buildClassHierarchy()) {
                stringBuilder.append(description(buildClass, prefix, withHeader));
            }
            if (this.buildOrPlugin instanceof JkBuild) {
                JkBuild build = (JkBuild) this.buildOrPlugin;
                if (build.plugins != null) {
                    for (JkPlugin plugin : build.plugins.all()) {
                        stringBuilder.append(BuildClassDef.of(plugin).description(plugin.name() + "#", withHeader));
                    }
                }
            }
            return stringBuilder.toString();
        }

        private String description(Class<?> buildClass, String prefix, boolean withHeader) {
            List<BuildMethodDef> methods = this.methodsOf(buildClass);
            List<BuildOptionDef> options = this.optionsOf(buildClass);
            if (methods.isEmpty() && options.isEmpty()) {
                return "";
            }
            String classWord = JkBuild.class.isAssignableFrom(buildClass) ? "class" : "plugin";
            StringBuilder stringBuilder = new StringBuilder();
            if (withHeader) {
                stringBuilder.append("\nFrom " + classWord + " " + buildClass.getName() + " :\n");
            }
            String margin = withHeader ? "  " : "";
            if (!methods.isEmpty()) {
                stringBuilder.append(margin + "Methods :\n");
                for (BuildMethodDef methodDef : this.methodsOf(buildClass)) {
                    final String displayedMethodName = prefix + methodDef.name;
                    if (methodDef.description == null) {
                        stringBuilder.append( margin + "  " + displayedMethodName + " : No description available.\n");
                    } else {
                        stringBuilder.append(margin).append("  ").append(displayedMethodName)
                                .append(" : ").append(methodDef.description.replace("\n", " "))
                                .append("\n");
                    }
                }
            }
            if (!options.isEmpty()) {
                stringBuilder.append(margin + "Options :\n");
                for (BuildOptionDef optionDef : this.optionsOf(buildClass)) {
                    stringBuilder.append(optionDef.description(prefix, margin));
                }
            }
            return stringBuilder.toString();
        }



        private static List<String> toLines(String string) {
            if (string == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(JkUtilsString.split(string, "\n"));
        }

        Map<String, String> optionValues(JkBuild build) {
            final Map<String, String> result = new LinkedHashMap<>();
            for (final BuildOptionDef optionDef : this.optionDefs) {
                final String name = optionDef.name;
                final Object value = BuildOptionDef.value(build, name);
                result.put(name, JkUtilsObject.toString(value));
            }
            return result;
        }

        Element toElement(Document document) {
            final Element buildEl = document.createElement("build");
            final Element methodsEl = document.createElement("methods");
            buildEl.appendChild(methodsEl);
            for (final BuildMethodDef buildMethodDef : this.methodDefs) {
                final Element methodEl = buildMethodDef.toXmlElement(document);
                methodsEl.appendChild(methodEl);
            }
            final Element optionsEl = document.createElement("options");
            buildEl.appendChild(optionsEl);
            for (final BuildOptionDef buildOptionDef : this.optionDefs) {
                final Element optionEl = buildOptionDef.toElement(document);
                optionsEl.appendChild(optionEl);
            }
            return buildEl;
        }

        private List<Class<? extends JkBuild>> buildClassHierarchy() {
            List<Class<? extends JkBuild>> result = new ArrayList<>();
            Class<?> current = this.buildOrPlugin.getClass();
            while (JkBuild.class.isAssignableFrom(current) || JkPlugin.class.isAssignableFrom(current)) {
                result.add((Class<? extends JkBuild>) current);
                current = current.getSuperclass();
            }
            return result;
        }

        private List<BuildMethodDef> methodsOf(Class<?> buildClass) {
            return this.methodDefs.stream().filter(buildMethodDef -> buildClass.equals(buildMethodDef.declaringClass))
                    .collect(Collectors.toList());
        }

        private List<BuildOptionDef> optionsOf(Class<?> buildClass) {
            return this.optionDefs.stream().filter(buildOptionDef -> buildClass.equals(buildOptionDef.rootDeclaringClass))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Definition of method in a given class that can be called by Jerkar.
     *
     * @author Jerome Angibaud
     */
     static final class BuildMethodDef implements Comparable<BuildMethodDef> {

        private final String name;

        private final String description;

        private final Class<?> declaringClass;

        private BuildMethodDef(String name, String description, Class<?> declaringClass) {
            super();
            this.name = name;
            this.description = description;
            this.declaringClass = declaringClass;
        }

        static BuildMethodDef of(Method method) {
            final JkDoc jkDoc = JkUtilsReflect.getInheritedAnnotation(method, JkDoc.class);
            final String descr = jkDoc != null ? JkUtilsString.join(jkDoc.value(), "\n") : null;
            return new BuildMethodDef(method.getName(), descr, method.getDeclaringClass());
        }

        @Override
        public int compareTo(BuildMethodDef other) {
            if (this.declaringClass.equals(other.declaringClass)) {
                return this.name.compareTo(other.name);
            }
            if (this.declaringClass.isAssignableFrom(other.declaringClass)) {
                return -1;
            }
            return 1;
        }

        String serialize() {
            return name + '|' + description + '|' + declaringClass.getName();
        }

        Element toXmlElement(Document document) {
            final Element methodEl = document.createElement("method");
            final Element nameEl = document.createElement("name");
            nameEl.setTextContent(this.name);
            final Element descriptionEl = document.createElement("description");
            if (description != null) {
                descriptionEl.appendChild(document.createCDATASection(description));
            }
            final Element classEl = document.createElement("declaringClass");
            classEl.setTextContent(declaringClass.getName());
            methodEl.appendChild(nameEl);
            methodEl.appendChild(descriptionEl);
            methodEl.appendChild(classEl);
            return methodEl;
        }

    }

    /**
     * Definition for build option. Build options are fields belonging to a
     * build class.
     *
     * @author Jerome Angibaud
     */
     static final class BuildOptionDef implements Comparable<BuildOptionDef> {

        private final String name;

        private final String description;

        private final Object build;

        private final Object defaultValue;

        private final Class<?> rootDeclaringClass;

        private final Class<?> type;

        private BuildOptionDef(String name, String description, Object jkBuild, Object defaultValue,
                               Class<?> type, Class<?> declaringClass) {
            super();
            this.name = name;
            this.description = description;
            this.build = jkBuild;
            this.defaultValue = defaultValue;
            this.type = type;
            this.rootDeclaringClass = declaringClass;
        }

        static BuildOptionDef of(Object instance, Field field, String name, Class<?> rootDeclaringClass) {
            final JkDoc opt = field.getAnnotation(JkDoc.class);
            final String descr = opt != null ? JkUtilsString.join(opt.value(), "\n") : null;
            final Class<?> type = field.getType();
            final Object defaultValue = value(instance, name);
            return new BuildOptionDef(name, descr, instance, defaultValue, type, rootDeclaringClass);
        }

        private static Object value(Object buildinstance, String optName) {
            if (!optName.contains(".")) {
                return JkUtilsReflect.getFieldValue(buildinstance, optName);
            }
            final String first = JkUtilsString.substringBeforeFirst(optName, ".");
            Object firstObject = JkUtilsReflect.getFieldValue(buildinstance, first);
            if (firstObject == null) {
                final Class<?> firstClass = JkUtilsReflect.getField(buildinstance.getClass(), first).getType();
                firstObject = JkUtilsReflect.newInstance(firstClass);
            }
            final String last = JkUtilsString.substringAfterFirst(optName, ".");
            return value(firstObject, last);
        }

        @Override
        public int compareTo(BuildOptionDef other) {
            if (this.build.getClass().equals(other.build.getClass())) {
                return this.name.compareTo(other.name);
            }
            if (this.build.getClass().isAssignableFrom(other.build.getClass())) {
                return -1;
            }
            return 1;
        }


        String description(String prefix, String margin) {
            StringBuilder builder = new StringBuilder();
            builder.append(prefix).append(name).append(" (").append(type()).append( ", default : ").append(defaultValue)
                    .append(") : ").append(description.replace("\n", " "));
            return builder.toString();
        }

        private String type() {
            if (type.isEnum()) {
                return "Enum of " + enumValues(type);
            }
            return this.type.getSimpleName();
        }

        private static String enumValues(Class<?> enumClass) {
            final Object[] values = enumClass.getEnumConstants();
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                result.append(values[i].toString());
                if (i + 1 < values.length) {
                    result.append(", ");
                }
            }
            return result.toString();
        }

        String defaultValue() {
            return this.defaultValue == null ? "null" : this.defaultValue.toString();
        }

        Element toElement(Document document) {
            final Element optionEl = document.createElement("option");
            final Element nameEl = document.createElement("name");
            nameEl.setTextContent(this.name);
            final Element descriptionEl = document.createElement("description");
            if (description != null) {
                descriptionEl.appendChild(document.createCDATASection(this.description));
            }
            final Element typeEl = document.createElement("type");
            typeEl.setTextContent(this.type());
            final Element defaultValueEl = document.createElement("defaultValue");
            defaultValueEl.appendChild(document.createCDATASection(this.defaultValue()));
            optionEl.appendChild(nameEl);
            optionEl.appendChild(descriptionEl);
            optionEl.appendChild(typeEl);
            optionEl.appendChild(defaultValueEl);
            return optionEl;
        }

    }

    private static class NameAndField {
        String name;
        Field field;
        Class<?> rootClass; // for nested fields, we need the class declaring

        // the asScopedDependency object

        NameAndField(String name, Field field, Class<?> rootClass) {
            super();
            this.name = name;
            this.field = field;
            this.rootClass = rootClass;
        }

        @Override
        public String toString() {
            return name + ", to " + rootClass.getName();
        }

    }

}
