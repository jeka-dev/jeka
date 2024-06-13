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

import java.util.function.Consumer;

/**
 * A mutable container for {@link Consumer}. From this object you can replace the underlying {@link Runnable} or
 * chain it with other ones.
 * @param <T> The type to be consumed by the consumers.
 */
public class JkConsumers<T> implements Consumer<T> {

    private Consumer<T> consumer;

    private JkConsumers(Consumer<T> consumer) {
        this.consumer= consumer;
    }

    /**
     * Creates a {@link JkConsumers} delegating to the single specified {@link Consumer}.
     */
    public static <T> JkConsumers<T> of() {
        return new JkConsumers<T>( o -> {});
    }

    /**
     * Set the specified {@link Consumer} as the unique underlying element for this container.
     */
    public JkConsumers<T> set(Consumer<T> consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * Chains this underlying {@link Consumer} with the specified one. The specified element will
     * be executed at the end.
     */
    public JkConsumers<T> add(Consumer<T> appendedConsumer) {
        consumer = consumer.andThen(appendedConsumer);
        return this;
    }

    /**
     * Chains this underlying {@link Consumer} with the specified one. The specified element will
     * be executed at the beginning.
     */
    public JkConsumers<T> prepend(Consumer<T> appendedConsumer) {
        consumer = appendedConsumer.andThen(consumer);
        return this;
    }


    @Override
    public void accept(T t) {
        consumer.accept(t);
    }
}
