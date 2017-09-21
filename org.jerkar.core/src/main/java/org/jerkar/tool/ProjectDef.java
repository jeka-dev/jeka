package org.jerkar.tool;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.system.JkLog;
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

    private final List<Class<?>> buildClassNames;

    private ProjectDef(List<Class<?>> buildClassNames) {
        super();
        this.buildClassNames = Collections.unmodifiableList(buildClassNames);
    }

    /**
     * Creates a project definition by giving its asScopedDependency directory.
     */
    public static ProjectDef of(File rootDir) {
        final BuildResolver buildResolver = new BuildResolver(rootDir);
        final List<Class<?>> classDefs = new LinkedList<>();
        classDefs.addAll(buildResolver.resolveBuildClasses());
        return new ProjectDef(classDefs);
    }

    public void logAvailableBuildClasses() {
        int i = 0;
        for (final Class<?> classDef : this.buildClassNames) {
            final String defaultMessage = (i == 0) ? " (default)" : "";
            final JkDoc jkDoc = classDef.getAnnotation(JkDoc.class);
            final String desc;
            if (jkDoc != null) {
                desc = JkUtilsString.join(jkDoc.value(), "\n");
            } else {
                desc = "No description available";
            }
            JkLog.info(classDef.getName() + defaultMessage + " : " + desc);
            i++;
        }
    }

    /**
     * Defines methods and options available on a given build class.
     *
     * @author Jerome Angibaud
     */
    static final class ProjectBuildClassDef {

        private final List<JkProjectBuildMethodDef> methodDefs;

        private final List<JkProjectBuildOptionDef> optionDefs;

        private ProjectBuildClassDef(List<JkProjectBuildMethodDef> methodDefs,
                List<JkProjectBuildOptionDef> optionDefs) {
            super();
            this.methodDefs = Collections.unmodifiableList(methodDefs);
            this.optionDefs = Collections.unmodifiableList(optionDefs);
        }

        List<JkProjectBuildMethodDef> methodDefinitions() {
            return methodDefs;
        }

        static ProjectBuildClassDef of(Object build) {
            final Class<?> clazz = build.getClass();
            final List<JkProjectBuildMethodDef> methods = new LinkedList<>();
            for (final Method method : executableMethods(clazz)) {
                methods.add(JkProjectBuildMethodDef.of(method));
            }
            Collections.sort(methods);
            final List<JkProjectBuildOptionDef> options = new LinkedList<>();
            for (final NameAndField nameAndField : options(clazz, "", true, null)) {
                options.add(JkProjectBuildOptionDef.of(build, nameAndField.field, nameAndField.rootClass,
                        nameAndField.name));
            }
            Collections.sort(options);
            return new ProjectBuildClassDef(methods, options);
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

        void log(boolean displayFromClass) {
            log(displayFromClass, "");
        }

        void log(boolean displayFromClass, String methodPrefix) {
            JkLog.nextLine();
            JkLog.infoHeaded("Methods               ");
            if (this.methodDefs.isEmpty()) {
                JkLog.info("None");
            }
            Class<?> currentClass = Object.class;
            for (final JkProjectBuildMethodDef methodDef : this.methodDefs) {
                JkLog.nextLine();
                if (!methodDef.declaringClass.equals(currentClass) && displayFromClass) {
                    JkLog.infoUnderlined("From " + methodDef.declaringClass.getName());
                }
                currentClass = methodDef.declaringClass;
                String displayedMethodName = methodPrefix + methodDef.name;
                if (methodDef.description == null) {
                    JkLog.info(displayedMethodName + " : No description available.");
                } else if (!methodDef.description.contains("\n")) {
                    JkLog.info(displayedMethodName+ " : " + methodDef.description);
                } else {
                    JkLog.info(displayedMethodName + " : ");
                    JkLog.info(toLines(methodDef.description));
                }
            }
            JkLog.nextLine();
            JkLog.infoHeaded("Options               ");
            if (this.optionDefs.isEmpty()) {
                JkLog.info("None");
            }
            currentClass = Object.class;
            for (final JkProjectBuildOptionDef optionDef : this.optionDefs) {
                JkLog.nextLine();
                if (!optionDef.jkBuild.getClass().equals(currentClass) && displayFromClass) {
                    JkLog.infoUnderlined("From " + optionDef.jkBuild.getClass().getName());
                }
                currentClass = optionDef.jkBuild.getClass();
                optionDef.log(methodPrefix);
            }

        }

        private static List<String> toLines(String string) {
            if (string == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(JkUtilsString.split(string, "\n"));
        }

        Map<String, String> optionValues(JkBuild build) {
            final Map<String, String> result = new LinkedHashMap<>();
            for (final JkProjectBuildOptionDef optionDef : this.optionDefs) {
                final String name = optionDef.name;
                final Object value = JkProjectBuildOptionDef.value(build, name);
                result.put(name, JkUtilsObject.toString(value));
            }
            return result;
        }

        Element toElement(Document document) {
            final Element buildEl = document.createElement("build");
            final Element methodsEl = document.createElement("methods");
            buildEl.appendChild(methodsEl);
            for (final JkProjectBuildMethodDef buildMethodDef : this.methodDefs) {
                final Element methodEl = buildMethodDef.toXmlElement(document);
                methodsEl.appendChild(methodEl);
            }
            final Element optionsEl = document.createElement("options");
            buildEl.appendChild(optionsEl);
            for (final JkProjectBuildOptionDef buildOptionDef : this.optionDefs) {
                final Element optionEl = buildOptionDef.toElement(document);
                optionsEl.appendChild(optionEl);
            }
            return buildEl;
        }
    }

    /**
     * Definition of method in a given class that can be called by Jerkar.
     *
     * @author Jerome Angibaud
     */
    public static final class JkProjectBuildMethodDef implements Comparable<JkProjectBuildMethodDef> {

        private final String name;

        private final String description;

        private final Class<?> declaringClass;

        private JkProjectBuildMethodDef(String name, String description, Class<?> declaringClass) {
            super();
            this.name = name;
            this.description = description;
            this.declaringClass = declaringClass;
        }

        static JkProjectBuildMethodDef of(Method method) {
            final JkDoc jkDoc = JkUtilsReflect.getInheritedAnnotation(method, JkDoc.class);
            final String descr = jkDoc != null ? JkUtilsString.join(jkDoc.value(), "\n") : null;
            return new JkProjectBuildMethodDef(method.getName(), descr, method.getDeclaringClass());
        }

        @Override
        public int compareTo(JkProjectBuildMethodDef other) {
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
    public static final class JkProjectBuildOptionDef implements Comparable<JkProjectBuildOptionDef> {

        private final String name;

        private final String description;

        private final Object jkBuild;

        private final Object defaultValue;

        private final Class<?> type;

        private JkProjectBuildOptionDef(String name, String description, Object jkBuild, Object defaultValue,
                Class<?> type) {
            super();
            this.name = name;
            this.description = description;
            this.jkBuild = jkBuild;
            this.defaultValue = defaultValue;
            this.type = type;
        }

        static JkProjectBuildOptionDef of(Object instance, Field field, Class<?> declaringClass, String name) {
            final JkDoc opt = field.getAnnotation(JkDoc.class);
            final String descr = opt != null ? JkUtilsString.join(opt.value(), "\n") : null;
            final Class<?> type = field.getType();
            final Object defaultValue = value(instance, name);
            return new JkProjectBuildOptionDef(name, descr, instance, defaultValue, type);
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
        public int compareTo(JkProjectBuildOptionDef other) {
            if (this.jkBuild.getClass().equals(other.jkBuild.getClass())) {
                return this.name.compareTo(other.name);
            }
            if (this.jkBuild.getClass().isAssignableFrom(other.jkBuild.getClass())) {
                return -1;
            }
            return 1;
        }

        void log(String optionPrefix) {
            String displayedOptionName = optionPrefix + this.name;
            if (this.description == null) {
                JkLog.info(displayedOptionName + " : No description available.");
            } else if (!this.description.contains("\n")) {
                JkLog.info(displayedOptionName + " : " + this.description);
            } else {
                JkLog.info(displayedOptionName + " : ");
                JkLog.info(ProjectBuildClassDef.toLines(this.description));
            }
            JkLog.info("Type : " + this.type());
            JkLog.info("Default value : " + this.defaultValue);
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
