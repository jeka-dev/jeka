package dev.jeka.core.api.function;

import java.util.function.Consumer;

/**
 * A mutable container for {@link Consumer}. From this object you can replace the underlying {@link Runnable} or
 * chain it with other ones.
 * @param <T> The type to be consumed by the consumers.
 * @param <P> The type of the instance holding this <code>JkConsumers</code>. Only used for
 *           parent chaining purpose.
 */
public class JkConsumers<T, P> implements Consumer<T> {

    private Consumer<T> consumer;

    /**
     * For parent chaining
     */
    public final P __;

    private JkConsumers(P parent, Consumer<T> consumer) {
        this.consumer= consumer;
        this.__ = parent;
    }

    /**
     * Creates a {@link JkConsumers} delegating to the single specified {@link Consumer}.
     */
    public static <T> JkConsumers<T, Void> of() {
        return ofParent(null);
    }

    /**
     * Same as {@link #of()} but providing a parent chaining.
     */
    public static <T, P> JkConsumers<T, P> ofParent(P parent) {
        return new JkConsumers<T, P>(parent, o -> {});
    }

    /**
     * Set the specified {@link Consumer} as the unique underlying element for this container.
     */
    public JkConsumers<T, P> set(Consumer<T> consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * Chains this underlying {@link Consumer} with the specified one. The specified element will
     * be executed at the end.
     */
    public JkConsumers<T, P> append(Consumer<T> appendedConsumer) {
        consumer = consumer.andThen(appendedConsumer);
        return this;
    }

    /**
     * Chains this underlying {@link Consumer} with the specified one. The specified element will
     * be executed at the beginning.
     */
    public JkConsumers<T, P> prepend(Consumer<T> appendedConsumer) {
        consumer = appendedConsumer.andThen(consumer);
        return this;
    }


    @Override
    public void accept(T t) {
        consumer.accept(t);
    }
}
