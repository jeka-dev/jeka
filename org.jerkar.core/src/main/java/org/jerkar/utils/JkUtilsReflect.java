package org.jerkar.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class JkUtilsReflect {



	public static void setAccessibleIfNeeded(Field field) {
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(Object object, Field field) {
		setAccessibleIfNeeded(field);
		try {
			return (T) field.get(object);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setFieldValue(Object object, Field field, Object value) {
		try {
			setAccessibleIfNeeded(field);
			field.set(object, value);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

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
			throw new RuntimeException(e);
		}
	}


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


	public static <T> T newInstance(Class<T> clazz) {
		try {
			final Constructor<T> constructor = clazz.getDeclaredConstructor();
			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
			return constructor.newInstance();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T invoke(Object target, String methodName) {
		try {
			final Method method = target.getClass().getMethod(methodName);
			return (T) method.invoke(target);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T invoke(Object target, Method method) {
		try {
			return (T) method.invoke(target);
		} catch(final InvocationTargetException e) {
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


	@SuppressWarnings("unchecked")
	public static <V> V invoke(Object target, Method method, Object... params) {
		try {
			if (!method.isAccessible()) {
				method.setAccessible(true);
			}
			final Class<?>[] paramTypes = new Class<?>[params.length];
			for (int i = 0; i<params.length; i++) {
				paramTypes[i] = params[i].getClass();
			}
			return (V) method.invoke(target, params);
		} catch (final Exception e) {
			throw new RuntimeException("Error while invoking " + method + " with params "
					+ Arrays.toString(params), e);
		}
	}

	public static Method getMethod(Class<?> clazz, String name, Class<?> ...argTypes) {
		try {
			return clazz.getMethod(name, argTypes);
		} catch (final SecurityException e) {
			throw new RuntimeException(e);
		} catch (final NoSuchMethodException e) {
			throw new IllegalStateException("No method " + name + " with argTypes "
					+ toString(argTypes) + " found on class " +clazz.getName());
		}
	}

	public static Method getMethodOrNull(Class<?> clazz, String name, Class<?> ...argTypes) {
		try {
			return clazz.getMethod(name, argTypes);
		} catch (final SecurityException e) {
			throw new RuntimeException(e);
		} catch (final NoSuchMethodException e) {
			return null;
		}
	}

	public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?> ...argTypes) {
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

	public static String toString(Class<?>...classes) {
		return "[" + JkUtilsString.toString(Arrays.asList(classes), ", ") + "]";
	}


	public static boolean isMethodPublicIn(Class<?> clazz, String method, Class<?> ...args) {
		try {
			clazz.getMethod(method, args);
		} catch (final NoSuchMethodException e) {
			return false;
		}
		return true;
	}

	public static <T extends Annotation> T getInheritedAnnotation(Method method, Class<T> annotationClass) {
		final T result = method.getAnnotation(annotationClass);
		if (result != null) {
			return result;
		}
		final Class<?> methodSuperClass = method.getDeclaringClass().getSuperclass();
		if (methodSuperClass != null) {
			final Method superMethod = getMethodOrNull(methodSuperClass, method.getName(), method.getParameterTypes());
			if (superMethod == null) {
				return null;
			}
			return getInheritedAnnotation(superMethod, annotationClass);
		}
		return null;
	}

	/**
	 * Returns all fields declared in the class passed as argument or in its super classes annotated with
	 * the supplied annotation.
	 */
	public static List<Field> getAllDeclaredField(Class<?> clazz, Class<? extends Annotation> annotationClass) {
		final List<Field> result = new LinkedList<Field>();
		for (final Field field : clazz.getDeclaredFields()) {
			final Object option = field.getAnnotation(annotationClass);
			if (option != null) {
				result.add(field);
			}
		}
		final Class<?> superClass = clazz.getSuperclass();
		if (superClass != null) {
			result.addAll(getAllDeclaredField(superClass, annotationClass));
		}
		return result;
	}

	/**
	 * Returns all fields declared in the class passed as argument or in its super classes.
	 */
	public static List<Field> getAllDeclaredField(Class<?> clazz, boolean includeSuperClass) {
		final List<Field> result = new LinkedList<Field>();
		for (final Field field : clazz.getDeclaredFields()) {
			result.add(field);
		}
		final Class<?> superClass = clazz.getSuperclass();
		if (superClass != null && includeSuperClass) {
			result.addAll(getAllDeclaredField(superClass, true));
		}
		return result;
	}


	@SuppressWarnings("unchecked")
	public static <T> T invokeStaticMethod(Class<?> clazz, String methodName, Object ...args) {
		return (T) invokeMethod(null, clazz, methodName, args);
	}

	@SuppressWarnings("unchecked")
	public static <T> T invokeInstanceMethod(Object target, String methodName, Object ...args) {
		return (T) invokeMethod(target, null, methodName, args);
	}

	private static Object invokeMethod(Object target, Class<?> clazz, String methodName, Object ...args) {
		final boolean staticMethod = target == null;
		final Class<?> effectiveClass = clazz == null ? target.getClass() : clazz;
		final String className = effectiveClass.getName();
		final Set<Method> canditates = new HashSet<Method>(Arrays.asList(effectiveClass.getMethods()));
		canditates.addAll(Arrays.asList(effectiveClass.getDeclaredMethods()));
		final Class<?> types[] = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			final Object arg = args[i];
			types[i] = arg.getClass();
		}
		final Set<Method> result = findMethodsCompatibleWith(true, canditates, methodName, types);
		if (result.isEmpty()) {
			throw new IllegalArgumentException("No " + (staticMethod ? "static" : "instance") + " method found on class "
					+ className + " for method " + methodName + " and param types " + Arrays.toString(types));
		} else if (result.size() > 1) {
			throw new IllegalArgumentException("Several "+  (staticMethod ? "static" : "instance")
					+ " methods match on class " + className + " for method " + methodName + " and param types " + Arrays.toString(types)
					+ ". You should use method #invoke(Method, Object[] args) instead." );
		}
		final Method method = result.iterator().next();
		return invoke(target, method, args);
	}



	private static Set<Method> findMethodsCompatibleWith(boolean staticMethod, Set<Method> methods,
			String methodName, Class<?>[] argTypes) {
		final Set<Method> list = new HashSet<Method>(methods);
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
			if (Modifier.isAbstract(method.getModifiers()) || Modifier.isStatic(method.getModifiers()) != staticMethod) {
				it.remove();
				continue;
			}
			if (!isMethodArgCompatible(method, argTypes)) {
				it.remove();
				continue;
			}

		}
		return list;
	}

	private static boolean isMethodArgCompatible(Method method, Class<?>... argTypes) {
		for (int i =0; i<argTypes.length; i++) {
			if (!isCompatible(method.getParameterTypes()[i], argTypes[i])) {
				return false;
			}
		}
		return true;
	}

	private static boolean isCompatible(Class<?> type0, Class<?> type1) {
		if (type0.getName().equals(type1.getName())) {
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

	private static final Map<Class<?>,Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<Class<?>,Class<?>>();

	static{
		PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class );
		PRIMITIVE_TO_WRAPPER.put(long.class, Long.class );
		PRIMITIVE_TO_WRAPPER.put(double.class, Double.class );
		PRIMITIVE_TO_WRAPPER.put(float.class, Float.class );
		PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class );
		PRIMITIVE_TO_WRAPPER.put(char.class, Character.class );
		PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class );
		PRIMITIVE_TO_WRAPPER.put(void.class, Void.TYPE );
		PRIMITIVE_TO_WRAPPER.put(short.class, Short.TYPE );
	}

}
