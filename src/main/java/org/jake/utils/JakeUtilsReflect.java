package org.jake.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JakeUtilsReflect {

	public static void setFieldValueIgnoreAbsent(Object object, String fieldName, Object value) {
		try {
			final Field field = getField(object.getClass(), fieldName);
			if (field == null) {
				return;
			}
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(object, value);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Constructor<T> getNoArgConstructor(Class<T> clazz) {
		for (final Constructor<?> constructor : clazz.getConstructors()) {
			if (constructor.getParameterTypes().length == 0) {
				return (Constructor<T>) constructor;
			}
		}
		return null;
	}

	public static void setAccessibleIfNeeded(Field field) {
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(Object object, Field field) {
		try {
			return (T) field.get(object);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setFieldValue(Object object, Field field, Object value) {
		try {
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

	public static boolean isWrapperClass(Class<?> clazz) {
		return (clazz == Integer.class || clazz == Long.class || clazz == Boolean.class
				|| clazz == Short.class || clazz == Double.class || clazz == Float.class
				|| clazz == Byte.class || clazz == Boolean.class);
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

	public static LinkedHashMap<String, Field> retainsPropertyFields(Field[] fields) {
		final LinkedHashMap<String, Field> result = new LinkedHashMap<String, Field>();
		for (final Field field : fields) {
			final int modifier = field.getModifiers();
			if (!Modifier.isStatic(modifier) && !Modifier.isTransient(modifier)) {
				result.put(field.getName(), field);
			}
		}
		return result;
	}




	public static Map<String, Method> retainsPropertyMethod(Method[] methods) {
		final Map<String, Method> result = new LinkedHashMap<String, Method>();
		for (final Method method : methods) {
			final int modif = method.getModifiers();
			if (!Modifier.isAbstract(modif) && Modifier.isPublic(modif)
					&& !Modifier.isStatic(modif) && method.getParameterTypes().length <= 1) {
				result.put(method.getName(), method);
			}
		}
		return result;
	}

	public static Map<String, Method> retainsPropertyMethod(Class<?> clazz, Class<?> cap) {
		final Map<String, Method> map = retainsPropertyMethod(clazz.getDeclaredMethods());
		if (cap.equals(clazz.getSuperclass())) {
			return map;
		} else {
			map.putAll(retainsPropertyMethod(clazz.getSuperclass(), cap));
			return map;
		}
	}

	@SuppressWarnings("unchecked")
	public static <V> V invoke(Object target, Method method, Object... params) {
		try {
			return (V) method.invoke(target, params);
		} catch (final Exception e) {
			throw new RuntimeException("Error while invoking " + method + " with params "
					+ Arrays.toString(params), e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <V> V invokeStatic(Method method, Object... params) {
		try {
			return (V) method.invoke(null, params);
		} catch (final Exception e) {
			throw new RuntimeException("Error while invoking " + method + " with params "
					+ Arrays.toString(params), e);
		}
	}



	public static Class<?> getClosestParent(Class<?> target, Set<Class<?>> others) {
		Class<?> result = null;
		for (final Class<?> candidate : others) {
			if (target.equals(candidate)) {
				return candidate;
			}
			if (candidate.isAssignableFrom(target)) {
				if (result == null || result.isAssignableFrom(candidate)) {
					result = candidate;
				}
			}
		}
		return result;
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
		return "[" + JakeUtilsIterable.toString(Arrays.asList(classes), ", ") + "]";
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






}
