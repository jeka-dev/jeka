package org.jerkar.api.utils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

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
        final List<T> result = new LinkedList<T>();
        for (final T t : it) {
            result.add(t);

        }
        return result;
    }

    /**
     * @see #listOf(Iterable)
     */
    public static <T> List<T> listOf(T... items) {
        return Arrays.asList(items);
    }

    /**
     * Returns a duplicate free list of the given items
     */
    public static <T> List<T> listWithoutDuplicateOf(Iterable<T> items) {
        final Set<T> set = new HashSet<T>();
        final LinkedList<T> result = new LinkedList<T>();
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
    public static <T> void addAllWithoutDplicate(List<T> recipientList, Iterable<? extends T> items) {
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
    public static <T> Set<T> setOf(T... items) {
        final HashSet<T> result = new HashSet<T>();
        result.addAll(Arrays.asList(items));
        return result;
    }

    /**
     * Creates a set of specified items.
     */
    public static <T> Set<T> setOf(Iterable<T> items) {
        final HashSet<T> result = new HashSet<T>();
        for (final T item : items) {
            result.add(item);
        }
        return result;
    }

    /**
     * Creates a set of specified items.
     */
    public static <T> Set<T> linkedHashSetOf(Iterable<T> items) {
        final LinkedHashSet<T> result = new LinkedHashSet<T>();
        for (final T item : items) {
            result.add(item);
        }
        return result;
    }

    /**
     * Creates a map of specified key/value. Key value should be declared in sequence as
     * <code>mapOf(key1, value1, key2, value2,...)</code>
     */
    @SuppressWarnings("unchecked")
    public static <T, U> Map<T, U> mapOf(T key, U value, Object... others) {
        final Map<T, U> result = new HashMap<T, U>();
        result.put(key, value);
        for (int i = 0; i < others.length; i = i + 2) {
            final T otherKey = (T) others[i];
            final U otherValue = (U) others[i + 1];
            result.put(otherKey, otherValue);
        }
        return result;
    }

    /**
     * Creates a map of specified key/value. Key value should be declared in sequence as
     * <code>mapOf(key1, value1, key2, value2,...)</code>
     */
    @SuppressWarnings("unchecked")
    public static <T, U> Map<T, U> mapOfAny(Object... others) {
        final Map<T, U> result = new LinkedHashMap<T, U>();
        for (int i = 0; i < others.length; i = i + 2) {
            final T otherKey = (T) others[i];
            final U otherValue = (U) others[i + 1];
            result.put(otherKey, otherValue);
        }
        return result;
    }

    /**
     * Returns an {@link Iterable} iterating on items of specified iterables.
     * Result is backed by the specified iterables.
     */
    public static <T> Iterable<T> chain(Iterable<T>... iterables) {
        return chainAll(Arrays.asList(iterables));
    }


    /**
     * @see JkUtilsIterable#chain(Iterable...)
     */
    public static <T> Iterable<T> chainAll(Iterable<Iterable<T>> iterables) {
        final List<Iterable<T>> effectiveIterables = removeEmptyIt(iterables);
        if (effectiveIterables.isEmpty()) {
            return Collections.emptyList();
        }
        if (effectiveIterables.size() == 1) {
            return effectiveIterables.get(0);
        }
        return new ChainedIterable<T>(effectiveIterables);
    }

    private static <T> List<Iterable<T>> removeEmptyIt(Iterable<Iterable<T>> iterables) {
        final List<Iterable<T>> result = new LinkedList<Iterable<T>>();
        for (final Iterable<T> iterable : iterables) {
            if (iterable.iterator().hasNext()) {
                result.add(iterable);
            }
        }
        return result;
    }

    private static final class ChainedIterable<T> implements Iterable<T> {

        private final Iterable<Iterable<T>> iterables;

        public ChainedIterable(Iterable<Iterable<T>> iterables) {
            super();
            this.iterables = iterables;
        }

        @Override
        public Iterator<T> iterator() {
            final List<Iterator<T>> iterators = new LinkedList<Iterator<T>>();
            for (final Iterable<T> iterable : iterables) {
                iterators.add(iterable.iterator());
            }
            return new ChainedIterator<T>(iterators);
        }

    }

    private static final class ChainedIterator<T> implements Iterator<T> {

        private final Iterator<Iterator<T>> iterators;

        private Iterator<T> current;

        public ChainedIterator(Iterable<Iterator<T>> iterables) {
            super();
            this.iterators = iterables.iterator();
            current = iterators.next();
        }

        @Override
        public boolean hasNext() {
            final boolean currentNext = current.hasNext();
            if (currentNext) {
                return true;
            }
            while (iterators.hasNext()) {
                current = iterators.next();
                if (current.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public T next() {
            if (current.hasNext()) {
                return current.next();
            }
            while (iterators.hasNext()) {
                current = iterators.next();
                if (current.hasNext()) {
                    return current.next();
                }
            }
            return current.next();
        }

        @Override
        public void remove() {
            current.remove();
        }

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
    public static <T> List<T> concatLists(Iterable<? extends T>... lists) {
        final List<T> result = new LinkedList<T>();
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
        final Map<String, String> result = new HashMap<String, String>();
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
        final List<String> result = new LinkedList<String>();
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

    /**
     * Returns difference between two collections. The result consists in an array of two {@link Set}.
     * The first one contains the element present in A and absent in B.
     * The second one contains elements in B but absent in A.
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T>[] diff(Collection<T> a, Collection<T> b) {
        final HashSet<T> first = new HashSet<T>();
        final HashSet<T> second = new HashSet<T>();
        for (final T item : a) {
            if (! b.contains(item)) {
                first.add(item);
            }
        }
        for (final T item : b) {
            if (! a.contains(item)) {
                second.add(item);
            }
        }
        return new HashSet[] {first, second};
    }

}
