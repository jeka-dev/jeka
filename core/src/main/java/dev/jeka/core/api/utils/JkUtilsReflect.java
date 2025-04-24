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

package dev.jeka.core.api.utils;

import dev.jeka.core.api.java.JkClassLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Utility class for dealing with reflection
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsReflect {

    /**
     * Sets the specified field to accessible if not already done.
     */
    public static void setAccessibleIfNeeded(Field field) {
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    /**
     * Returns a method from the target class that has the same name and same
     * argument types then the specified one. Argument types are compared on
     * name base to handle different classloaders.
     */
    public static Method methodWithSameNameAndArgType(Method original, Class<?> targetClass) {
        for (final Method method : targetClass.getMethods()) {
            if (!method.getName().equals(original.getName())) {
                continue;
            }
            final Class<?>[] originalTypes = method.getParameterTypes();
            final Class<?>[] types = method.getParameterTypes();
            if (types.length == 0 && originalTypes.length == 0) {
                return method;
            }
            if (types.length != originalTypes.length) {
                continue;
            }
            boolean found = false;
            for (int i = 0; i < originalTypes.length; i++) {
                if (!originalTypes[i].getName().equals(types[i].getName())) {  //NOSONAR
                    break;
                }
                found = true;
            }
            if (found) {
                return method;
            }
        }
        throw new IllegalArgumentException("No method " + original + " found on class "
                + targetClass.getName());
    }

    public static List<Method> methodsHavingName(Class clazz, String name) {
        List<Method> result = new LinkedList<>();
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                result.add(method);
            }
        }
        return result;
    }



    /**
     * Returns an array of the types of specified objects.
     */
    public static Class<?>[] getTypesOf(Object[] params) {
        final Class<?>[] result = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = params[i] == null ? null : params[i].getClass();
        }
        return result;
    }

    /**
     * Same as {@link Field#get(Object)} but throwing only unchecked exceptions.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object object, Field field) {
        setAccessibleIfNeeded(field);
        try {
            return (T) field.get(object);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the specified value for the specified field and object. Field is marked as
     * 'accessible' if not already done.
     */
    public static void setFieldValue(Object object, Field field, Object value) {
        try {
            setAccessibleIfNeeded(field);
            field.set(object, value);
        } catch (final Exception e) {
            Class<?> valueType = value == null ? Void.class : value.getClass();
            Class<?> fieldType = field.getType();
            StringBuilder msg = new StringBuilder();
            msg.append(String.format("Can not set object of type %s on field %s", valueType, field));

            // Classloader issue: same class name but coming from distinct classloader
            if(fieldType.getName().equals(valueType.getName())) {
                ClassLoader fieldClassLoader = fieldType.getClassLoader();
                ClassLoader valueClassLoader = valueType.getClassLoader();
                msg.append("\nTry to assign a value of compatible type but coming from distinct classloader\n");
                msg.append("Field: ").append(field).append("\n");
                msg.append("Value: ").append(value).append("\n");
                msg.append("Field classloader: ").append(JkClassLoader.of(fieldClassLoader)).append("\n\n");
                msg.append("Value classloader: ").append(JkClassLoader.of(valueClassLoader)).append("\n");
            }
            throw new RuntimeException(msg.toString(), e);
        }
    }

    /**
     * Gets the value of the field having the specified name on a given object.
     */
    public static Object getFieldValue(Object object, String fieldName) {
        try {
            final Field field = getField(object.getClass(), fieldName);
            if (field == null) {
                throw new IllegalArgumentException("No field '" + fieldName + "' found in "
                        + object.getClass().getName() + " or its super classes");
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(object);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Gets the field having the specified name on the specified object.
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        final Field[] fields = clazz.getDeclaredFields();
        final Field field = getField(fields, fieldName);
        if (field == null) {
            final Class<?> superClass = clazz.getSuperclass();
            if (Object.class.equals(superClass)) {
                return null;
            }
            return getField(superClass, fieldName);
        }
        return field;
    }

    private static Field getField(Field[] fields, String fieldName) {
        for (final Field field : fields) {
            if (fieldName.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * Instantiates the given class.
     */
    public static <T> T newInstance(Class<T> clazz) {
        try {
            final Constructor<T> constructor = clazz.getDeclaredConstructor();
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            throw JkUtilsThrowable.unchecked(targetException, targetException.getMessage());
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Instantiates the given class using a constructor with specified single argument.
     */
    public static <T> T newInstance(Class<T> clazz, Class<?> parameterType, Object parameter) {
        try {
            final Constructor<T> constructor = clazz.getDeclaredConstructor(parameterType);
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance(parameter);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            } else {
                throw new RuntimeException(e.getTargetException());
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("No constructor found with parameter of type " + parameterType.getName() + " on class " + clazz, e);
        }
    }

    /**
     * Invokes the specified method on the given object.
     */
    public static <T> T invoke(Object target, String methodName) {
        final Method method;
        try {
            method = target.getClass().getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("No method " + methodName + " found on class " + target.getClass().getName());
        }
        return invoke(target, method);
    }

    /**
     * Invokes the specified method on the given object.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object target, Method method) {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return (T) method.invoke(target);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            } else {
                throw new RuntimeException(e.getTargetException());
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invokes the specified method on the given object.
     */
    @SuppressWarnings("unchecked")
    public static <V> V invoke(Object target, Method method, Object... params) {
        try {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return (V) method.invoke(target, params);
        } catch (final InvocationTargetException e) {
            final Throwable targetEx = e.getTargetException();
            if (targetEx instanceof Error) {
                throw (Error) targetEx;
            }
            throw JkUtilsThrowable.unchecked((Exception) targetEx);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Type mismatch. Expecting "
                    + Arrays.toString(method.getParameterTypes()) + " but got "
                    + Arrays.toString(params), e);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e, "Error while invoking " + method + " with params "
                    + Arrays.toString(params));
        }
    }

    private static String[] detailedTypesOf(Object... params) {
        final String[] result = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = params[i] == null ? null : params[i].getClass().getName() + ":"
                    + params[i].getClass().getClassLoader();
        }
        return result;
    }

    private static String[] classloaderOf(Class<?>... params) {
        final String[] result = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            final ClassLoader classLoader = params[i].getClassLoader();
            result[i] = params[i] == null ? null : params[i].getName() + ":"
                    + classLoader.toString();
        }
        return result;
    }

    /**
     * Same as {@link Class#getMethod(String, Class...)} but throwing only
     * unchecked exceptions.
     */
    public static Method getMethod(Class<?> clazz, String name, Class<?>... argTypes) {
        try {
            return clazz.getMethod(name, argTypes);
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("No method " + name + " with argTypes "
                    + toString(argTypes) + " found on class " + clazz.getName());
        }
    }

    /**
     * Returns the method of the given name and argument type on the specified
     * class. If none, this method returns <code>null</code>.
     */
    public static Method getMethodOrNull(Class<?> clazz, String name, Class<?>... argTypes) {
        try {
            return clazz.getMethod(name, argTypes);
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns the method of the given name and argument type on the specified
     * class.
     */
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... argTypes) {
        try {
            final Method method = clazz.getDeclaredMethod(name, argTypes);
            method.setAccessible(true);
            return method;
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Find a method of the given name and argument type on the specified class or its parent classes.
     */
    public static Method findMethodMethodDeclaration(Class<?> clazz, String name, Class<?>... argTypes) {
        try {
            return clazz.getDeclaredMethod(name, argTypes);
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchMethodException e) {
            if (clazz.equals(Object.class)) {
                return null;
            }
            return findMethodMethodDeclaration(clazz.getSuperclass(), name, argTypes);
        }
    }

    private static String toString(Class<?>... classes) {
        return "[" + JkUtilsString.join(Arrays.asList(classes), ", ") + "]";
    }

    /**
     * Returns whether the specified method signature is declared in the
     * specified class.
     */
    public static boolean isMethodPublicIn(Class<?> clazz, String method, Class<?>... args) {
        try {
            clazz.getMethod(method, args);
        } catch (final NoSuchMethodException e) {
            return false;
        }
        return true;
    }




    /**
     * Returns the annotation declared on a given method. If no annotation is
     * declared on the method, then annotation is searched in parent classes.
     */
    public static <T extends Annotation> T getInheritedAnnotation(Method method,
            Class<T> annotationClass) {
        final T result = method.getAnnotation(annotationClass);
        if (result != null) {
            return result;
        }
        final Class<?> methodSuperClass = method.getDeclaringClass().getSuperclass();
        if (methodSuperClass != null) {
            final Method superMethod = getMethodOrNull(methodSuperClass, method.getName(),
                    method.getParameterTypes());
            if (superMethod == null) {
                return null;
            }
            return getInheritedAnnotation(superMethod, annotationClass);
        }
        return null;
    }

    /**
     * Returns the annotation declared on a given method. If no annotation is
     * declared on the method, then annotation is searched in parent classes.
     */
    public static <T extends Annotation> T getInheritedAnnotation(Class<?> clazz,
                                                                  Class<T> annotationClass, String methodName,
                                                                  Class<?> ... argTypes) {
        Method method = getDeclaredMethod(clazz, methodName, argTypes);
        if (method == null) {
            if (clazz.equals(Object.class)) {
                return null;
            }
            return getInheritedAnnotation(clazz.getSuperclass(), annotationClass, methodName, argTypes);
        }
        return getInheritedAnnotation(method, annotationClass);
    }

    /**
     * Returns all fields declared in the class passed as argument or its
     * super classes annotated with the supplied annotation.
     */
    public static List<Field> getDeclaredFieldsWithAnnotation(Class<?> clazz,
                                                              Class<? extends Annotation> annotationClass) {
        final List<Field> result = new LinkedList<>();
        for (final Field field : clazz.getDeclaredFields()) {
            final Object annotation = field.getAnnotation(annotationClass);
            if (annotation != null) {
                result.add(field);
            }
        }
        final Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            result.addAll(getDeclaredFieldsWithAnnotation(superClass, annotationClass));
        }
        return result;
    }

    /**
     * Retrieves a list of methods declared in the specified class that are annotated
     * with a given annotation.
     *
     * @param clazz the class to search for methods
     * @param annotationClass the annotation type to look for on the methods
     * @return a list of methods declared in the specified class that are annotated
     *         with the provided annotation
     */
    public static List<Method> getDeclaredMethodsWithAnnotation(Class<?> clazz,
                                                              Class<? extends Annotation> annotationClass) {
        final List<Method> result = new LinkedList<>();
        for (final Method method : clazz.getDeclaredMethods()) {
            final Object annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
                result.add(method);
            }
        }
        return result;
    }

    /**
     * Returns all fields declared in the class passed as argument or and its super classes.
     */
    public static List<Field> getDeclaredFieldsWithAnnotation(Class<?> clazz, boolean includeSuperClass) {
        final List<Field> result = new LinkedList<>();
        Collections.addAll(result, clazz.getDeclaredFields());
        final Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && includeSuperClass) {
            result.addAll(getDeclaredFieldsWithAnnotation(superClass, true));
        }
        return result;
    }

    /**
     * Invokes a static method with the specified arguments
     *
     * @param clazz
     *            The class the method is invoked on.
     * @param methodName
     *            The method name to invoke
     * @param args
     *            The argument values the method is invoked with.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return (T) invokeMethod(null, clazz, methodName, args);
    }

    /**
     * Invokes an instance method with the specified arguments
     *
     * @param instance
     *            The instance the method is invoked on.
     * @param methodName
     *            The method name to invoke
     * @param args
     *            The argument values the method is invoked with.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeInstanceMethod(Object instance, String methodName, Object... args) {
        return (T) invokeMethod(instance, null, methodName, args);
    }

    /**
     * Returns a string representation of the given object.
     */
    public static String toString(Object object) {
        if (object == null) {
            return "[null]";
        }
        StringBuilder b = new StringBuilder("[" + object.getClass().getName() + "    ");
        for (Field f : object.getClass().getDeclaredFields()) {
            b.append(f.getName() + "=" + getFieldValue(object, f.getName()) + " ");
        }
        b.append(']');
        return b.toString();
    }

    private static Object invokeMethod(Object target, Class<?> clazz, String methodName,
            Object... args) {
        final boolean staticMethod = target == null;
        final Class<?> effectiveClass = clazz == null ? target.getClass() : clazz;
        final String className = effectiveClass.getName();
        final Set<Method> candidates = new HashSet<>(Arrays.asList(effectiveClass
                .getMethods()));
        candidates.addAll(Arrays.asList(effectiveClass.getDeclaredMethods()));
        final Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            final Object arg = args[i];
            types[i] = arg == null ? null : arg.getClass();
        }
        final Set<Method> result = findMethodsCompatibleWith(staticMethod, candidates, methodName,
                types);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No " + (staticMethod ? "static" : "instance")
                    + " method found on class " + className + " for method " + methodName
                    + " and param types " + Arrays.toString(types));
        } else if (result.size() > 1) {
            throw new IllegalArgumentException("Several " + (staticMethod ? "static" : "instance")
                    + " methods match on class " + className + " for method " + methodName
                    + " and param types " + Arrays.toString(types)
                    + ". You should use method #invoke(Method, Object[] args) instead.");
        }
        final Method method = result.iterator().next();
        return invoke(target, method, args);
    }

    private static Set<Method> findMethodsCompatibleWith(boolean staticMethod, Set<Method> methods,
            String methodName, Class<?>[] argTypes) {
        final Set<Method> list = new HashSet<>(methods);
        for (final Iterator<Method> it = list.iterator(); it.hasNext();) {
            final Method method = it.next();
            if (!methodName.equals(method.getName())) {
                it.remove();
                continue;
            }
            if (argTypes.length != method.getParameterTypes().length) {
                it.remove();
                continue;
            }
            if (Modifier.isAbstract(method.getModifiers())
                    || Modifier.isStatic(method.getModifiers()) != staticMethod) {
                it.remove();
                continue;
            }
            if (!isMethodArgCompatible(method, argTypes)) {
                it.remove();
            }

        }
        return list;
    }

    private static boolean isMethodArgCompatible(Method method, Class<?>... argTypes) {
        for (int i = 0; i < argTypes.length; i++) {
            if (!isCompatible(method.getParameterTypes()[i], argTypes[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCompatible(Class<?> type0, Class<?> type1) {
        if (type1 == null) {
            return true;
        }
        if (type0.getName().equals(type1.getName())) {  //NOSONAR
            return true;
        }
        if (type0.isPrimitive()) {
            return isCompatible(PRIMITIVE_TO_WRAPPER.get(type0), type1);
        }
        if (type1.isPrimitive()) {
            return isCompatible(type0, PRIMITIVE_TO_WRAPPER.get(type1));
        }
        return type0.isAssignableFrom(type1);
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();

    static {
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(void.class, Void.TYPE);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.TYPE);
    }

    /**
     * Create a Dynamic proxy with the specified interface, delagating call to the
     * target object.
     */
    public static <T> T createReflectionProxy(Class<T> interfaze, Object target) {
        return (T) Proxy.newProxyInstance(
                interfaze.getClassLoader(),
                new Class[] {interfaze},
                new JkUtilsReflect.ReflectionInvocationHandler(target)
        );
    }

    /**
     * Proxy Invocation handler, that delegate to target instance via reflection
     */
    public static class ReflectionInvocationHandler implements InvocationHandler {

        private final Object targetInstance;

        public ReflectionInvocationHandler(Object targetInstance) {
            this.targetInstance = targetInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            try {
                Method targetMethod = targetInstance.getClass().getMethod(method.getName(), method.getParameterTypes());
                return targetMethod.invoke(targetInstance, args);
            } catch (final IllegalArgumentException | NoSuchMethodException | IllegalAccessException |
                           InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
