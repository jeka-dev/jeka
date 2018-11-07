package org.jerkar.api.utils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

/**
 * Utility class for dealing with {@link Iterable}.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsIterable {

    /**
     * Returns a list form the the specified {@link Iterable}.
     * If the specified {@link Iterable} is yet a {@link List} than it
     * is returned as is, otherwise a brand new {@link List} is created.
     */
    public static <T> List<T> listOf(Iterable<T> it) {
        if (it instanceof List) {
            return (List<T>) it;
        }
        final List<T> result = new LinkedList<>();
        for (final T t : it) {
            result.add(t);

        }
        return result;
    }

    /**
     * @see #listOf(Iterable)
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> listOf(T... items) {
        return Arrays.asList(items);
    }

    /**
     * @see #listOf(Iterable)
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> listOfItems(T item1, T item2, T... others) {
        ArrayList<T> result = new ArrayList<>(others.length + 2);
        result.set(0, item1);
        result.set(1, item2);
        for (int i = 0; i < others.length; i++) {
            result.set(i + 2, others[i]);
        }
        return result;
    }

    /**
     * Returns a duplicate free list of the given items
     */
    public static <T> List<T> listWithoutDuplicateOf(Iterable<T> items) {
        final Set<T> set = new HashSet<>();
        final LinkedList<T> result = new LinkedList<>();
        for (final T item : items) {
            if (!set.contains(item)) {
                set.add(item);
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Adds items to the specified recipient list. Only item not already present in the recipient list are added.
     */
    public static <T> void addAllWithoutDuplicate(Collection<T> recipientList, Iterable<? extends T> items) {
        for (final T item : items) {
            if (!recipientList.contains(item)) {
                recipientList.add(item);
            }
        }
    }

    /**
     * Creates an array of the specified class and populate it with items of the specified {@link Iterable}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayOf(Iterable<T> it, Class<T> clazz) {
        final List<T> list = listOf(it);
        final T[] result = (T[]) Array.newInstance(clazz, list.size());
        int i = 0;
        for (final T t : it) {
            result[i] = t;
            i++;
        }
        return result;
    }

    /**
     * Creates a set of specified items.
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> setOf(T... items) {
        final HashSet<T> result = new HashSet<>();
        result.addAll(Arrays.asList(items));
        return result;
    }

    /**
     * Creates a set of specified items.
     */
    public static <T> Set<T> setOf(Iterable<T> items) {
        final HashSet<T> result = new HashSet<>();
        for (final T item : items) {
            result.add(item);
        }
        return result;
    }

    /**
     * Creates a map of specified key/value. Key of should be declared in sequence as
     * <code>mapOf(key1, value1, key2, value2,...)</code>
     */
    @SuppressWarnings("unchecked")
    public static <T, U> Map<T, U> mapOf(T key, U value, Object... others) {
        final Map<T, U> result = new HashMap<>();
        result.put(key, value);
        for (int i = 0; i < others.length; i = i + 2) {
            final T otherKey = (T) others[i];
            final U otherValue = (U) others[i + 1];
            result.put(otherKey, otherValue);
        }
        return result;
    }

    /**
     * Creates a map of specified key/value. Key of should be declared in sequence as
     * <code>mapOf(key1, value1, key2, value2,...)</code>
     */
    @SuppressWarnings("unchecked")
    public static <T, U> Map<T, U> mapOfAny(Object... others) {
        final Map<T, U> result = new LinkedHashMap<>();
        for (int i = 0; i < others.length; i = i + 2) {
            final T otherKey = (T) others[i];
            final U otherValue = (U) others[i + 1];
            result.put(otherKey, otherValue);
        }
        return result;
    }

    private static <T> List<Iterable<T>> removeEmptyIt(Iterable<Iterable<T>> iterables) {
        final List<Iterable<T>> result = new LinkedList<>();
        for (final Iterable<T> iterable : iterables) {
            if (iterable.iterator().hasNext()) {
                result.add(iterable);
            }
        }
        return result;
    }

    /**
     * Convenient method to put several entry in a map having the same value at
     * once.
     */
    public static <K, V> void putMultiEntry(Map<K, V> map, Iterable<K> keys, V value) {
        for (final K key : keys) {
            map.put(key, value);
        }
    }

    /**
     * Returns a list that is a concatenation of the specified lists.
     * The result is not backed by specified {@link Iterable}.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> concatLists(Iterable<? extends T>... lists) {
        final List<T> result = new LinkedList<>();
        for (final Iterable<? extends T> list : lists) {
            for (final T item : list) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Creates a {@link Map} and populates it with specified properties.
     */
    public static Map<String, String> propertiesToMap(Properties properties) {
        final Map<String, String> result = new HashMap<>();
        for (final Object propKey : properties.keySet()) {
            result.put(propKey.toString(), properties.getProperty(propKey.toString()));
        }
        return result;
    }

    /**
     * Returns a string representation of the specified map constructed as
     * [key0]=[value0], [key1]=[value1], ...
     */
    @SuppressWarnings("unchecked")
    public static String toString(Map<?, ?> map) {
        final StringBuilder builder = new StringBuilder();
        for (final Iterator<?> it = map.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<Object, Object> entry = (Entry<Object, Object>) it.next();
            builder.append(entry.getKey()).append("=");
            builder.append(JkUtilsObject.toString(entry.getValue()));
            if (it.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    /**
     * Returns a multi-line string representation of the specified map
     * constructed as [key0]=[value0], [key1]=[value1], ...
     */
    @SuppressWarnings("unchecked")
    public static List<String> toStrings(Map<?, ?> map) {
        final List<String> result = new LinkedList<>();
        for (final Object name : map.entrySet()) {
            final Map.Entry<Object, Object> entry = (Entry<Object, Object>) name;
            result.add(entry.getKey() + "=" + JkUtilsObject.toString(entry.getValue()));
        }
        return result;
    }

    /**
     * Concats two arrays
     */
    public static <T> T[] concat(T[] a, T[] b) {
        final int aLen = a.length;
        final int bLen = b.length;

        @SuppressWarnings("unchecked")
        final T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

}
