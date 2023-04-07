package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.HelpDisplayer.ItemContainer;
import dev.jeka.core.tool.HelpDisplayer.RenderItem;

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

    static BeanDescription renderItem(Class<? extends JkBean> beanClass) {
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
            stringBuilder.append(margin + "\nMethods\n");
            List<RenderItem> items = methods.stream().map(BeanDescription::renderItem).collect(Collectors.toList());
            ItemContainer container = new ItemContainer(items);
            container.render().forEach(line -> stringBuilder.append(margin + "  " + line + "\n"));
        }
        if (!properties.isEmpty()) {
            stringBuilder.append(margin + "\nProperties\n");
            List<RenderItem> items = properties.stream().map(BeanDescription::renderItem).collect(Collectors.toList());
            ItemContainer container = new ItemContainer(items);
            container.render().forEach(line -> stringBuilder.append(margin + "  " + line + "\n"));
        }
        return stringBuilder.toString();
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

    static RenderItem renderItem(BeanMethod beanMethod) {
        String name = beanMethod.name;
        List<String> descLines = JkUtilsString.isBlank(beanMethod.description)
                ? Collections.singletonList("No description available.")
                : RenderItem.split(beanMethod.description);
        return new RenderItem(name, descLines);
    }

    static RenderItem renderItem(BeanField beanField) {
        String name = beanField.name;
        List<String> descLines = JkUtilsString.isBlank(beanField.description)
                ? Collections.singletonList("No description available.")
                : RenderItem.split(beanField.description);
        LinkedList result = new LinkedList(descLines);
        String last = (String) result.pollLast();
        last = last + "  [" + beanField.type() + "  default=" + beanField.defaultValue + "]";
        result.add(last);
        return new RenderItem(name, result);
    }




}
