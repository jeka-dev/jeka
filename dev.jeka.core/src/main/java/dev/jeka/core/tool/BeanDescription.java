package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Description of methods and fields exposed by a KBean.
 *
 * @author Jerome Angibaud
 */
final class BeanDescription {

    private final List<BeanMethod> beanMethods;

    private final List<BeanField> beanFields;

    private final Class<? extends JkBean> beanClass;

    private BeanDescription(Class<? extends JkBean> beanClass, List<BeanMethod> beanMethods,
                      List<BeanField> beanFields) {
        super();
        this.beanClass = beanClass;
        this.beanMethods = Collections.unmodifiableList(beanMethods);
        this.beanFields = Collections.unmodifiableList(beanFields);
    }

    static BeanDescription of(Class<? extends JkBean> beanClass) {
        final List<BeanMethod> methods = new LinkedList<>();
        for (final Method method : executableMethods(beanClass)) {
            methods.add(BeanMethod.of(method));
        }
        Collections.sort(methods);
        final List<BeanField> options = new LinkedList<>();
        for (final NameAndField nameAndField : fields(beanClass, "", true, null)) {
            options.add(BeanField.of(beanClass, nameAndField.field,
                    nameAndField.name, nameAndField.rootClass));
        }
        Collections.sort(options);
        return new BeanDescription(beanClass, methods, options);
    }

    List<BeanMethod> beanMethods() {
        return beanMethods;
    }

    List<BeanField> beanFields() {
        return beanFields;
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

    private static List<NameAndField> fields(Class<?> clazz, String prefix, boolean root, Class<?> rClass) {
        final List<NameAndField> result = new LinkedList<>();
        for (final Field field : FieldInjector.getPropertyFields(clazz)) {
            final Class<?> rootClass = root ? field.getDeclaringClass() : rClass;
            if (!hasSubOption(field)) {
                result.add(new NameAndField(prefix + field.getName(), field, rootClass));
            } else {
                final List<NameAndField> subOpts = fields(field.getType(), prefix + field.getName() + ".", false,
                        rootClass);
                result.addAll(subOpts);
            }
        }
        return result;
    }

    private static boolean hasSubOption(Field field) {
        return !JkUtilsReflect.getAllDeclaredFields(field.getType(), JkDoc.class).isEmpty();
    }

    String description() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Class<? extends JkBean> buildClass : this.beanClassHierarchy()) {
            stringBuilder.append(description(buildClass, "", true, false));
        }
        return stringBuilder.toString();
    }

    String flatDescription(String prefix) {
        return description(beanClass, prefix, false, true);
    }

    private String description(Class<?> beanClass, String prefix, boolean withHeader, boolean includeHierarchy) {
        List<BeanMethod> methods = includeHierarchy ? this.beanMethods : this.methodsOf(beanClass);
        List<BeanField> properties = includeHierarchy ? this.beanFields : this.optionsOf(beanClass);
        if (methods.isEmpty() && properties.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (withHeader) {
            stringBuilder.append("\nFrom " + beanClass.getName() + " :\n");
        }
        String margin = withHeader ? "  " : "";
        if (!methods.isEmpty()) {
            stringBuilder.append(margin + "METHODS\n");
            int maxSize = 0;
            for (BeanMethod method : methods) {
                final String displayedMethodName = prefix + method.name;
                if (displayedMethodName.length() > maxSize) {
                    maxSize = displayedMethodName.length();
                }
            }
            for (BeanMethod methodDef : methods) {
                final String displayedMethodName =
                        JkUtilsString.padEnd(prefix + methodDef.name, maxSize + 1, ' ');
                if (methodDef.description == null) {
                    stringBuilder.append( margin + "  " + displayedMethodName + " : No description available.\n");
                } else {
                    stringBuilder.append(margin).append("  ").append(displayedMethodName)
                            .append(" : ").append(methodDef.description.replace("\n", " "))
                            .append("\n");
                }
            }
        }
        if (!properties.isEmpty()) {
            stringBuilder.append(margin + "PROPERTIES\n");
            int maxSize = 0;
            for (BeanField property : properties) {
                final String displayName = prefix + property.name ;
                if (displayName.length() > maxSize) {
                    maxSize = displayName.length();
                }
            }
            for (BeanField property : properties) {
                String displayName =
                        JkUtilsString.padEnd(prefix + property.name + "=", maxSize + 1, ' ');
                stringBuilder.append(property.description(displayName, margin));
            }
        }
        return stringBuilder.toString();
    }

    Map<String, String> optionValues(JkBean bean) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (final BeanField optionDef : this.beanFields) {
            final String name = optionDef.name;
            final Object value = BeanField.value(bean, name);
            result.put(name, JkUtilsObject.toString(value));
        }
        return result;
    }

    Element toElement(Document document) {
        final Element runEl = document.createElement("run");
        final Element methodsEl = document.createElement("methods");
        runEl.appendChild(methodsEl);
        for (final BeanMethod beanMethod : this.beanMethods) {
            final Element methodEl = beanMethod.toXmlElement(document);
            methodsEl.appendChild(methodEl);
        }
        final Element optionsEl = document.createElement("options");
        runEl.appendChild(optionsEl);
        for (final BeanField beanField : this.beanFields) {
            final Element optionEl = beanField.toElement(document);
            optionsEl.appendChild(optionEl);
        }
        return runEl;
    }

    private List<Class<? extends JkBean>> beanClassHierarchy() {
        List<Class<? extends JkBean>> result = new ArrayList<>();
        Class<?> current = beanClass;
        while (JkBean.class.isAssignableFrom(current)) {
            result.add((Class<? extends JkBean>) current);
            current = current.getSuperclass();
        }
        return result;
    }

    private List<BeanMethod> methodsOf(Class<?> runClass) {
        return this.beanMethods.stream().filter(beanMethod -> runClass.equals(beanMethod.declaringClass))
                .collect(Collectors.toList());
    }

    private List<BeanField> optionsOf(Class<?> runClass) {
        return this.beanFields.stream().filter(beanField -> runClass.equals(beanField.rootDeclaringClass))
                .collect(Collectors.toList());
    }

    /**
     * Definition of method in a given class that can be called by Jeka.
     *
     * @author Jerome Angibaud
     */
     static final class BeanMethod implements Comparable<BeanMethod> {

        private final String name;

        private final String description;

        private final Class<?> declaringClass;

        private BeanMethod(String name, String description, Class<?> declaringClass) {
            super();
            this.name = name;
            this.description = description;
            this.declaringClass = declaringClass;
        }

        static BeanMethod of(Method method) {
            final JkDoc jkDoc = JkUtilsReflect.getInheritedAnnotation(method, JkDoc.class);
            final String descr = jkDoc != null ? String.join("\n", jkDoc.value()) : null;
            return new BeanMethod(method.getName(), descr, method.getDeclaringClass());
        }

        @Override
        public int compareTo(BeanMethod other) {
            if (this.declaringClass.equals(other.declaringClass)) {
                return this.name.compareTo(other.name);
            }
            if (this.declaringClass.isAssignableFrom(other.declaringClass)) {
                return -1;
            }
            return 1;
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
     * Definition for Jeka class option. Jeka class options are fields belonging to a
     * Jeka class.
     *
     * @author Jerome Angibaud
     */
     static final class BeanField implements Comparable<BeanField> {

        private final String name;

        private final String description;

        private final Object bean;

        private final Object defaultValue;

        private final Class<?> rootDeclaringClass;

        private final Class<?> type;

        private final String injectedPropertyName;

        private BeanField(String name, String description, Object bean, Object defaultValue,
                          Class<?> type, Class<?> declaringClass, String injectedPropertyName) {
            super();
            this.name = name;
            this.description = description;
            this.bean = bean;
            this.defaultValue = defaultValue;
            this.type = type;
            this.rootDeclaringClass = declaringClass;
            this.injectedPropertyName = injectedPropertyName;
        }

        static BeanField of(Class<? extends JkBean> beanClass, Field field, String name, Class<?> rootDeclaringClass) {
            final JkDoc opt = field.getAnnotation(JkDoc.class);
            final String descr = opt != null ? String.join("\n", opt.value()) : null;
            final Class<?> type = field.getType();
            Object instance = JkUtilsReflect.newInstance(beanClass);
            final Object defaultValue = value(instance, name);
            final JkInjectProperty injectProperty = field.getAnnotation(JkInjectProperty.class);
            final String propertyName = injectProperty != null ? injectProperty.value() : null;
            return new BeanField(name, descr, instance, defaultValue, type, rootDeclaringClass, propertyName);
        }

        private static Object value(Object runInstance, String optName) {
            if (!optName.contains(".")) {
                return JkUtilsReflect.getFieldValue(runInstance, optName);
            }
            final String first = JkUtilsString.substringBeforeFirst(optName, ".");
            Object firstObject = JkUtilsReflect.getFieldValue(runInstance, first);
            if (firstObject == null) {
                final Class<?> firstClass = JkUtilsReflect.getField(runInstance.getClass(), first).getType();
                firstObject = JkUtilsReflect.newInstance(firstClass);
            }
            final String last = JkUtilsString.substringAfterFirst(optName, ".");
            return value(firstObject, last);
        }

        @Override
        public int compareTo(BeanField other) {
            if (this.bean.getClass().equals(other.bean.getClass())) {
                return this.name.compareTo(other.name);
            }
            if (this.bean.getClass().isAssignableFrom(other.bean.getClass())) {
                return -1;
            }
            return 1;
        }

        String shortDescription() {
            return name + " = " + defaultValue;
        }

        String description(String displayName, String margin) {
            String desc = description != null ? description : "No description available.";
            String oneLineDesc = desc.replace("\n", " ");
            String envPart = injectedPropertyName == null ? "" : ", property: " + injectedPropertyName;
            return String.format("%s  %s  : %s (type: %s, default: %s%s)\n",
                    margin, displayName, oneLineDesc, type(), defaultValue, envPart);
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
        final String name;
        final Field field;
        final Class<?> rootClass; // for nested fields, we need the class declaring

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
