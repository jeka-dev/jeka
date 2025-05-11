/*
 * Copyright 2014-2025  the original author or authors.
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

import java.math.BigDecimal;
import java.util.*;

/**
 * A type-safe container that maps String keys to values while maintaining natural key ordering,
 * with special handling for both numeric and non-numeric keys.
 *
 * <h3>Overview</h3>
 * <p>This container is designed for use as {@code public final} fields in {@code KBeans} classes.
 * The values can be configured through properties.</p>
 *
 * <h3>Key Features</h3>
 * <ul>
 *   <li>Type-safe value storage</li>
 *   <li>Natural ordering of keys</li>
 *   <li>Special handling for numeric keys</li>
 *   <li>Property-based configuration</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * class MyKBean extends KBean {
 *
 *     public final JkMultiValue<String> phones = JkMultiValue.of(String.class);
 *
 *     public final JkMultiValue<Address> addresses = JkMultiValue.of(Address.class);
 *
 *     public static class Address {
 *         @JkDoc("The street name and number")
 *         public String street;
 *         public int zip;
 *     }
 * }
 * }</pre>
 *
 * <h3>Property Configuration</h3>
 * <pre>{@code
 * # Properties file example
 * @phones.gsm=0856432321
 * @phones.home=0123456789
 *
 * @script.addresses.main.street=Buckingham Street 45
 * @script.addresses.main.zip=21050
 *
 * @script.addresses.secondary.street=Rue boyard 32
 * @script.addresses.secondary.zip=14540
 * }</pre>
 *
 * @param <T> The type of values stored in this container
 */
public final class JkMultiValue<T> {

    static final String KEY_TEXT = "[key]";

    private final TreeMap<String, T> values;

    private final Class<T> type;

    private JkMultiValue(Class<T> type) {
        this.type = type;
        this.values = new TreeMap<>(new NumericAwareComparator());
    }

    public static <T> JkMultiValue<T> of(Class<T> type) {
        return new JkMultiValue<T>(type);
    }

    public T get(String name) {
        return values.get(name);
    }

    public List<String> getKeys() {
        return new ArrayList<>(values.keySet());
    }

    public List<T> getValues() {
        return new ArrayList<>(values.values());
    }

    public List<Map.Entry<String, T>> getEntries() {
        return new ArrayList<>(values.entrySet());
    }

    public void put(String key, T value) {
        values.put(key, value);
    }

    public Class<T> getType() {
        return type;
    }

    void putAny(String key, Object value) {
        values.put(key, (T) value);
    }

    static boolean propNameMatches(String propDescName, String actualPropName) {
        if (propDescName.equals(actualPropName)) {
            return true;
        }
        String[] candidateParts = actualPropName.split("\\.");

        // Hande case where the key is empty, like in 'phones.=098989'
        if (actualPropName.endsWith(".")) {
            String[] newArray = new String[candidateParts.length + 1];
            System.arraycopy(candidateParts, 0, newArray, 0, candidateParts.length);
            newArray[newArray.length - 1] = "";
            candidateParts = newArray;
        }
        String[] nameParts = propDescName.split("\\.");
        if (candidateParts.length != nameParts.length) {
            return false;
        }
        for (int i = 0; i < nameParts.length; i++) {
            String namePart = nameParts[i];
            if (namePart.equals(KEY_TEXT)) {
                continue;
            }
            if (!namePart.equals(candidateParts[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    private static class NumericAwareComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            // Check if both strings represent numbers (including decimals)
            if (isNumeric(s1) && isNumeric(s2)) {
                try {
                    // Try parsing as BigDecimal to handle both integers and decimals
                    BigDecimal n1 = new BigDecimal(s1);
                    BigDecimal n2 = new BigDecimal(s2);
                    return n1.compareTo(n2);
                } catch (NumberFormatException e) {
                    // Fallback to string comparison
                    return s1.compareTo(s2);
                }
            }
            // Default to natural string comparison
            return s1.compareTo(s2);
        }

        private boolean isNumeric(String str) {
            if (str == null || str.isEmpty()) {
                return false;
            }
            // Regex for numbers (including decimals and negative numbers)
            return str.matches("-?\\d*\\.?\\d+");
        }
    }

}
