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

package dev.jeka.core.api.function;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A mutable container for {@link Consumer}. From this object you can replace the underlying {@link Runnable} or
 * chain it with other ones.
 * @param <T> The type to be consumed by the consumers.
 */
public class JkConsumers<T> implements Consumer<T> {

    private final LinkedList<Entry<Consumer<T>>> entries = new LinkedList<>();

    private JkConsumers() {
    }

    /**
     * Creates a {@link JkConsumers} delegating to the single specified {@link Consumer}.
     */
    public static <T> JkConsumers<T> of() {
        return new JkConsumers<T>();
    }

    /**
     * Appends the specified consumer to the consumer chain.
     */
    public JkConsumers<T> append(Consumer<T> consumer) {
        return append(consumer.toString(), consumer);
    }

    /**
     * Appends the specified consumer, with the specified name, to the consumer chain.
     */
    public JkConsumers<T> append(String name, Consumer<T> consumer) {
        this.entries.add(new Entry<>(name, consumer, null));
        return this;
    }

    /**
     * Appends the specified {@code JkConsumers} instance to the current chain of consumers.
     * The consumers from the provided {@code other} instance are added to the end of the existing chain.
     */
    public JkConsumers<T> append(JkConsumers other) {
        this.entries.addAll(other.entries);
        return this;
    }

    /**
     * Chains this underlying {@link Consumer} with the specified one. The specified element will
     * be executed at the beginning.
     */
    public JkConsumers<T> prepend(Consumer<T> consumer) {
        this.entries.add(0, new Entry<>(consumer.toString(), consumer, null));
        return this;
    }

    /**
     * Returns the name of the {@link Runnable}s, in the order of execution chain.
     */
    public List<String> getConsumerNames() {
        return Entry.sort(entries).stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());
    }

    @Override
    public void accept(T t) {
        entries.forEach(entry -> entry.runnable.accept(t));
    }

    @Override
    public String toString() {
        return getConsumerNames().toString();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Map<String, Consumer<T>> toMap() {
        LinkedHashMap<String, Consumer<T>> map = new LinkedHashMap<>();
        for (Entry<Consumer<T>> entry : entries) {
            map.put(entry.name, entry.runnable);
        }
        return map;
    }
}
