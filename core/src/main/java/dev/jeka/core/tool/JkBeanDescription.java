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

package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.builtins.app.AppKBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.setup.SetupKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.git.GitKBean;
import dev.jeka.core.tool.builtins.tooling.ide.EclipseKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/*
 * This describes the methods and fields exposed by a KBean,
 * which are obtained through reflection
 *
 * @author Jerome Angibaud
 */
public final class JkBeanDescription {

    public static final List<Class<? extends KBean>> STANDARD_KBEAN_CLASSES = JkUtilsIterable.listOf(
            SetupKBean.class,
            AppKBean.class,
            BaseKBean.class,
            ProjectKBean.class,
            MavenKBean.class,
            GitKBean.class,
            DockerKBean.class,
            NativeKBean.class,
            IntellijKBean.class,
            EclipseKBean.class
    );

    private static final Map<Class<? extends KBean>, JkBeanDescription> CACHE = new HashMap<>();

    final Class<? extends KBean> kbeanClass;

    public final String synopsisHeader;

    public final String synopsisDetail;

    public final List<BeanMethod> beanMethods;

    public final List<BeanField> beanFields;

    public final String initDescription;

    final boolean includeDefaultValues;

    final List<InitMethodInfo> preInitInfos;

    final List<InitMethodInfo> postInitInfos;

    final String docUlr;

    private JkBeanDescription(
            Class<? extends KBean> kbeanClass,
            String synopsisHeader,
            String synopsisDetail,
            List<BeanMethod> beanMethods,
            List<BeanField> beanFields,
            String initDescription,
            boolean includeDefaultValues,
            List<InitMethodInfo> preInitInfos,
            List<InitMethodInfo> postInitInfos,
            String docUlr) {

        super();
        this.kbeanClass = kbeanClass;
        this.synopsisHeader = synopsisHeader;
        this.synopsisDetail = synopsisDetail;
        this.beanMethods = Collections.unmodifiableList(beanMethods);
        this.beanFields = Collections.unmodifiableList(beanFields);
        this.initDescription = initDescription;
        this.includeDefaultValues = includeDefaultValues;
        this.preInitInfos = Collections.unmodifiableList(preInitInfos);
        this.postInitInfos = Collections.unmodifiableList(postInitInfos);
        this.docUlr = docUlr;
    }

    public static JkBeanDescription of(Class<? extends KBean> kbeanClass) {
        if (CACHE.containsKey(kbeanClass)) {
            return CACHE.get(kbeanClass);
        }

        final List<BeanMethod> methods = new LinkedList<>();
        for (final Method method : executableMethods(kbeanClass)) {
            methods.add(BeanMethod.of(method));
        }
        Collections.sort(methods);
        final List<BeanField> beanFields = new LinkedList<>();
        List<NameAndField> nameAndFields =  nameAndFields(kbeanClass, "", true, null);
        for (final NameAndField nameAndField : nameAndFields) {
            final BeanField beanField = BeanField.of(kbeanClass, nameAndField);
            beanFields.add(beanField);
        }
        Collections.sort(beanFields);
        String initDescription = getInitDescription(kbeanClass);

        // Grab header + description from content of @JkDoc
        final JkDoc jkDoc = kbeanClass.getAnnotation(JkDoc.class);
        final String header;
        final String detail;
        final String fullDesc = jkDoc == null ? "" : jkDoc.value();
        String[] lines = fullDesc.split("\n");
        if (lines.length == 0) {
            header = "";
            detail = "";
        } else {
            header = lines[0];
            if (lines.length > 1) {
                detail = JkUtilsString.substringAfterFirst(fullDesc, "\n");
            } else {
                detail = "";
            }
        }
        List<InitMethodInfo> preInitInfos = InitMethodInfo.preInitMethodsOf(kbeanClass);
        List<InitMethodInfo> postInitInfos = InitMethodInfo.postInitMethodsOf(kbeanClass);

        JkDocUrl jkDocUrl = kbeanClass.getAnnotation(JkDocUrl.class);
        String docUlr = jkDocUrl == null ? null : jkDocUrl.value();

        return new JkBeanDescription(kbeanClass, header, detail, methods, beanFields, initDescription, false,
                preInitInfos, postInitInfos, docUlr);
    }

    /**
     * Generates markdown content describing the bean, including its fields, methods,
     * and initialization details. The generated content is formatted as a markdown table.
     *
     * @return A formatted string in markdown representing the bean's information
     *         including its fields, methods, and initialization description if available.
     */
    public MdContent toMdContent() {
        StringBuilder header = new StringBuilder();
        header.append(this.synopsisHeader).append("\n\n");
        header.append(this.synopsisDetail);

        StringBuilder body = new StringBuilder();
        body.append(this.synopsisHeader).append("\n\n");
        body.append(this.synopsisDetail).append("\n");

        if (!preInitInfos.isEmpty()) {
            body.append("\n\n");
            body.append("**This KBean pre-initializes the following KBeans:**\n\n");
            body.append("|Pre-initialized KBean  |Description  |\n");
            body.append("|-------|-------------|\n");
            this.preInitInfos.forEach(info -> body.append(preInitContent(info)));
        }

        if (!JkUtilsString.isBlank(this.initDescription)) {
            body.append("\n\n");
            body.append("|KBean Initialisation  |\n");
            body.append("|--------|\n");
            body.append(String.format("|%s |%n", oneLiner(this.initDescription)));
        }

        if (!postInitInfos.isEmpty()) {
            body.append("\n\n");
            body.append("**This KBean post-initializes the following KBeans:**\n\n");
            body.append("|Post-initialised KBean   |Description  |\n");
            body.append("|-------|-------------|\n");
            this.postInitInfos.forEach(info -> body.append(preInitContent(info)));
        }

        if (!beanFields.isEmpty()) {
            body.append("\n\n");
            body.append("**This KBean exposes the following fields:**\n\n");
            body.append("|Field  |Description  |\n");
            body.append("|-------|-------------|\n");
            this.beanFields.forEach(field -> body.append(fieldContent(field)));
        }

        if (!beanMethods.isEmpty()) {
            body.append("\n\n");
            body.append("**This KBean exposes the following methods:**\n\n");
            body.append("|Method  |Description  |\n");
            body.append("|--------|-------------|\n");
            this.beanMethods.forEach(method -> body.append(methodContent(method)));
        }
        return new MdContent(header.toString(), body.toString());
    }

    private static String fieldContent(JkBeanDescription.BeanField beanField) {
        String typeName = JkUtilsString.removePackagePrefix(beanField.type.getName());
        if (beanField.type.isEnum()) {
            typeName = "enum:" + typeName;
        }
        return String.format("|%s [%s] |%s |%n",
                beanField.name ,
                typeName,
                oneLiner(beanField.description));
    }

    private static String methodContent(JkBeanDescription.BeanMethod beanMethod) {
        return String.format("|%s |%s |%n",
                beanMethod.name,
                oneLiner(beanMethod.description));
    }

    private static String preInitContent(InitMethodInfo preInitInfo) {
        return String.format("|%s |%s |%n",
                preInitInfo.targetKBean.getSimpleName(),
                preInitInfo.description == null ? "Undocumented." : oneLiner(preInitInfo.description));
    }

    private static String oneLiner(String original) {
        if (original == null) {
            return "";
        }
        String result = original.replaceAll("\\n", "<br/>").replaceAll("%n", "<br/>");
        if (!result.endsWith(".")) {
            result = result + ".";
        }
        return result;
    }

    static JkBeanDescription ofWithDefaultValues(Class<? extends KBean> kbeanClass, JkRunbase runbase) {
        if (CACHE.containsKey(kbeanClass)) {
            return CACHE.get(kbeanClass);
        }

        final List<BeanMethod> methods = new LinkedList<>();
        for (final Method method : executableMethods(kbeanClass)) {
            methods.add(BeanMethod.of(method));
        }
        Collections.sort(methods);
        final List<BeanField> beanFields = new LinkedList<>();
        List<NameAndField> nameAndFields =  nameAndFields(kbeanClass, "", true, null);
        for (final NameAndField nameAndField : nameAndFields) {
            beanFields.add(BeanField.ofWithDefaultValues(kbeanClass, nameAndField, runbase));
        }
        Collections.sort(beanFields);
        String initDescription = getInitDescription(kbeanClass);

        // Grab header + description from content of @JkDoc
        final JkDoc jkDoc = kbeanClass.getAnnotation(JkDoc.class);
        final String header;
        final String detail;
        final String fullDesc = jkDoc == null ? "" : jkDoc.value();
        String[] lines = fullDesc.split("\n");
        if (lines.length == 0) {
            header = "";
            detail = "";
        } else {
            header = lines[0];
            if (lines.length > 1) {
                detail = JkUtilsString.substringAfterFirst(fullDesc, "\n");
            } else {
                detail = "";
            }
        }
        List<InitMethodInfo> preInitInfos = InitMethodInfo.preInitMethodsOf(kbeanClass);
        List<InitMethodInfo> postInitInfos = InitMethodInfo.preInitMethodsOf(kbeanClass);
        JkDocUrl jkDocUrl = kbeanClass.getAnnotation(JkDocUrl.class);
        String docUlr = jkDocUrl == null ? null : jkDocUrl.value();

        JkBeanDescription result = new JkBeanDescription(kbeanClass, header, detail, methods, beanFields,
                initDescription, true, preInitInfos, postInitInfos, docUlr);
        CACHE.put(kbeanClass, result);
        return result;
    }

     boolean isContainingField(String fieldName) {
        return this.beanFields.stream()
                .anyMatch(beanField -> beanField.matchName(fieldName));
    }

    private static List<Method> executableMethods(Class<?> clazz) {
        final List<Method> result = new LinkedList<>();
        for (final Method method : clazz.getMethods()) {
            final JkDoc jkDoc = method.getAnnotation(JkDoc.class);
            if (jkDoc != null && jkDoc.hide()) {
                continue;
            }
            final int modifier = method.getModifiers();
            if (method.getReturnType().equals(void.class) && method.getParameterTypes().length == 0
                    && !JkUtilsReflect.isMethodPublicIn(Object.class, method.getName())
                    && !Modifier.isAbstract(modifier) && !Modifier.isStatic(modifier)) {
                result.add(method);
            }

        }
        return result;
    }

    private static List<NameAndField> nameAndFields(Class<?> clazz, String prefix, boolean root, Class<?> rClass) {
        final List<NameAndField> result = new LinkedList<>();
        for (final Field field : getPropertyFields(clazz)) {
            final JkDoc jkDoc = field.getAnnotation(JkDoc.class);
            if (jkDoc != null && jkDoc.hide()) {
                continue;
            }
            final Class<?> rootClass = root ? field.getDeclaringClass() : rClass;

            Class<?> fieldType = field.getType();
            if (isTerminal(fieldType)) {  // optimization to avoid costly discoveries
                result.add(NameAndField.of(prefix + field.getName(), field, rootClass));

            } else if (JkMultiValue.class.equals(fieldType)) {
                Class<?> multiValueType = BeanUtils.getValueType(field);
                String multiPrefix = prefix + field.getName() + "." + JkMultiValue.KEY_TEXT;

                if (isTerminal(multiValueType)) {
                    String descr = BeanField.getDescr(field);
                    result.add(NameAndField.of(multiPrefix, multiValueType, descr, rootClass));
                } else {
                    final List<NameAndField> subOpts = nameAndFields(
                            multiValueType,
                            multiPrefix + ".",
                            false,
                            rootClass);
                    result.addAll(subOpts);
                }

            } else {
                final List<NameAndField> subOpts = nameAndFields(field.getType(), prefix + field.getName() + ".", false,
                        rootClass);
                result.addAll(subOpts);
            }
        }
        return result.stream()
                .filter(nameAndField -> nameAndField.field == null
                        || !Modifier.isFinal(nameAndField.field.getModifiers()))
                .collect(Collectors.toList());
    }

    // For nested props, JkDoc must be present on the class or one of its fields.
    private static boolean isTerminal(Class<?> fieldType) {
        if (fieldType.isEnum()) {
            return true;
        }
        if (JkMultiValue.class.equals(fieldType)) {
            return false;
        }
        return !fieldType.isAnnotationPresent(JkDoc.class) &&
                JkUtilsReflect.getDeclaredFieldsWithAnnotation(fieldType, JkDoc.class).isEmpty();
    }

    private static List<Field> getPropertyFields(Class<?> clazz) {
        return JkUtilsReflect.getDeclaredFieldsWithAnnotation(clazz,true).stream()
                .filter(KBean::isPropertyField)
                .collect(Collectors.toList());
    }

    private static String getInitDescription(Class<?> clazz) {
        Method method = JkUtilsReflect.getDeclaredMethod(clazz, "init");
        if (method == null || method.getAnnotation(JkDoc.class) == null) {
            return null;
        }
        return method.getAnnotation(JkDoc.class).value();
    }

    /**
     * Definition of method in a given class that can be called by Jeka.
     *
     * @author Jerome Angibaud
     */
    public static final class BeanMethod implements Comparable<BeanMethod> {

        public final String name;

        public final String description;

        private final Class<?> declaringClass;

        private BeanMethod(String name, String description, Class<?> declaringClass) {
            super();
            this.name = name;
            this.description = description;
            this.declaringClass = declaringClass;
        }

        static BeanMethod of(Method method) {
            final JkDoc jkDoc = JkUtilsReflect.getInheritedAnnotation(method, JkDoc.class);
            final String descr;
            if (jkDoc != null) {
                descr = String.join("\n", jkDoc.value());
            } else {
                descr = null;
            }
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
    public static final class BeanField implements Comparable<BeanField> {

        final String name;

        final String description;

        private final Object bean;

        final Object defaultValue;

        final Class<?> type;

        final String injectedPropertyName;

        private BeanField(
                String name,
                String description,
                Object bean,
                Object defaultValue,
                Class<?> type,
                String injectedPropertyName) {

            super();
            this.name = name;
            this.description = description;
            this.bean = bean;
            this.defaultValue = defaultValue;
            this.type = type;
            this.injectedPropertyName = injectedPropertyName;
        }

        boolean containsMultiValue() {
            return name.contains(JkMultiValue.KEY_TEXT);
        }

        private static BeanField of(Class<? extends KBean> beanClass, NameAndField nameAndField) {
            if (nameAndField.field == null) {
                return of(beanClass, null, null, null, nameAndField.type, nameAndField.name);
            }
            Field field = nameAndField.field;
            return of(beanClass,
                    field.getAnnotation(JkDoc.class),
                    field.getAnnotation(JkPropValue.class),
                    field.getAnnotation(JkInjectProperty.class),
                    field.getType(),
                    nameAndField.name);
        }

        private static BeanField of(
                Class<? extends KBean> beanClass,
                JkDoc jkDoc,
                JkPropValue propValue,
                JkInjectProperty injectProperty,
                Class<?> type,
                String name) {


            final String descr = getDescr(jkDoc);
            String propertyName = null;
            if (propValue != null) {
                propertyName = propValue.value();
            } else {
                propertyName = injectProperty != null ? injectProperty.value() : null;
            }
            return new BeanField(
                    name,
                    descr,
                    null,
                    null,
                    type,
                    propertyName);
        }

        private static String getDescr(Field field) {
            JkDoc jkDoc = field.getAnnotation(JkDoc.class);
            if (jkDoc == null) {
                return null;
            }
            return getDescr(jkDoc);
        }

        private static String getDescr(JkDoc jkDoc) {
            final String descr;
            if (jkDoc != null) {
                descr = String.join("\n", jkDoc.value());
            } else {
                descr = null;
            }
            return descr;
        }

        private static BeanField ofWithDefaultValues(
                Class<? extends KBean> beanClass,
                NameAndField nameAndField,
                JkRunbase runbase) {

            final String descr = nameAndField.descr;
            final Class<?> type;
            final JkPropValue injectedPropValue;
            if (nameAndField.field != null) {
                type = nameAndField.field.getType();
                injectedPropValue = nameAndField.field.getAnnotation(JkPropValue.class);

            } else {
                type = nameAndField.type;
                injectedPropValue = null;
            }

            Object instance = runbase.load(beanClass);
            Object defaultValue = nameAndField.field == null ? null : value(instance, nameAndField.name);
            String propertyName = null;
            if (injectedPropValue != null) {
                propertyName = injectedPropValue.value();
            }
            return new BeanField(
                    nameAndField.name,
                    descr,
                    instance,
                    defaultValue,
                    type,
                    propertyName);
        }

        private static Object value(Object runInstance, String optName) {
            if (!optName.contains(".")) {
                return JkUtilsReflect.getFieldValue(runInstance, optName);
            }
            final String first = JkUtilsString.substringBeforeFirst(optName, ".");
            final Object firstObject;
            if (runInstance instanceof JkMultiValue) {
                JkMultiValue<?> multiValue = (JkMultiValue<?>) runInstance;
                Class<?> multiValueType = multiValue.getType();
                firstObject = JkUtilsReflect.newInstance(multiValueType);
            } else {
                Object candidate = JkUtilsReflect.getFieldValue(runInstance, first);
                if (candidate == null) {
                    final Class<?> firstClass = JkUtilsReflect.getField(runInstance.getClass(), first).getType();
                    candidate = JkUtilsReflect.newInstance(firstClass);
                }
                firstObject = candidate;
            }
            final String last = JkUtilsString.substringAfterFirst(optName, ".");
            return value(firstObject, last);
        }

        @Override
        public int compareTo(BeanField other) {
            if (this.bean == null || other.bean == null) { // maybe null if we don't compute default values
                return 0;
            }
            if (this.bean.getClass().equals(other.bean.getClass())) {
                return this.name.compareTo(other.name);
            }
            if (this.bean.getClass().isAssignableFrom(other.bean.getClass())) {
                return -1;
            }
            return 1;
        }

        boolean matchName(String candidate) {
            return JkMultiValue.propNameMatches(name, candidate);
        }

        BeanField withName(String newName) {
            return new BeanField(newName, description, bean, defaultValue, type, injectedPropertyName);
        }
    }

    private static class NameAndField {
        final String name;
        final Field field;  // either field nor type are set, not both
        final Class<?> type;
        final String descr;
        final Class<?> rootClass; // for nested fields, we need the class declaring

        // the asScopedDependency object

        private NameAndField(String name, Field field, Class<?> type, String descr, Class<?> rootClass) {
            super();
            this.name = name;
            this.field = field;
            this.type = type;
            this.descr = descr;
            this.rootClass = rootClass;
        }

        static NameAndField of(String name, Field field, Class<?> rootClass) {
            return new NameAndField(name, field, null, BeanField.getDescr(field), rootClass);
        }

        static NameAndField of(String name, Class<?> type, String descr, Class<?> rootClass) {
            return new NameAndField(name, null, type, descr, rootClass);
        }

        @Override
        public String toString() {
            return name + ", to " + rootClass.getName();
        }

    }

    public static class InitMethodInfo {

        public final Method declaringMethod;

        public final Class<?> targetKBean;

        public final String description;

        private InitMethodInfo(Method method, Class<?> targetKBean, String description) {
            super();
            this.declaringMethod = method;
            this.targetKBean = targetKBean;
            this.description = description;
        }

        static List<InitMethodInfo> preInitMethodsOf(Class<?> declaringClass) {
            return Arrays.stream(declaringClass.getDeclaredMethods())
                    .filter(method -> method.getAnnotation(JkPreInit.class) != null)
                    .map(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        Class<?> kbeanClass = parameterTypes.length == 0 ? null : parameterTypes[0];
                        JkDoc jkDoc = method.getAnnotation(JkDoc.class);
                        String description = jkDoc != null ? jkDoc.value() : null;
                        return new InitMethodInfo(method, kbeanClass, description);
                    })
                    .collect(Collectors.toList());
        }

        static List<InitMethodInfo> postInitMethodsOf(Class<?> declaringClass) {
            return Arrays.stream(declaringClass.getDeclaredMethods())
                    .filter(method -> method.getAnnotation(JkPostInit.class) != null)
                    .map(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        Class<?> kbeanClass = parameterTypes.length == 0 ? null : parameterTypes[0];
                        JkDoc jkDoc = method.getAnnotation(JkDoc.class);
                        String description = jkDoc != null ? jkDoc.value() : null;
                        return new InitMethodInfo(method, kbeanClass, description);
                    })
                    .collect(Collectors.toList());
        }

    }

    public static class MdContent {

        public final String header;

        public final String body;

        public MdContent(String header, String body) {
            this.header = header;
            this.body = body;
        }

        @Override
        public String toString() {
            return header + "\n" + body;
        }
    }

}
