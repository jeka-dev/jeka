package dev.jeka.core.api.function;

import java.util.LinkedList;
import java.util.List;

/**
 * A mutable container for {@link Runnable}. From this object you can replace the underlying {@link Runnable} or
 * chain it with other ones.
 */
public class JkRunnables<T> implements Runnable {

    private List<Runnable> runnables;

    /**
     * For parent chaining
     */
    public final T __;

    /**
     * Creates a {@link JkRunnables} delegating to the single specified {@link Runnable}.
     */
    public static JkRunnables<Void> of(Runnable runnable) {
        return of(null, runnable);
    }

    /**
     * Same as {@link #of(Runnable)} but providing a parent chaining.
     */
    public static <T> JkRunnables<T> of(T parent, Runnable runnable) {
        List<Runnable> runnables = new LinkedList<>();
        runnables.add(runnable);
        return new JkRunnables(parent, runnables);
    }

    /**
     * Creates a {@link JkRunnables} delegating to a no-op runnable.
     */
    public static JkRunnables<Void> of() {
        return ofParent(null);
    }

    /**
     * Same as {@link #of()} but providing parent chaining
     */
    public static <T> JkRunnables<T> ofParent(T parent) {
        return new JkRunnables<T>(parent, new LinkedList<>());
    }

    private JkRunnables(T parent, List<Runnable> runnables) {
        this.runnables = runnables;
        this.__ = parent;
    }

    /**
     * Set the specified {@link Runnable} as the unique underlying {@link Runnable} for this container.
     */
    public JkRunnables<T> set(Runnable runnable) {
        List<Runnable> runnables = new LinkedList<>();
        runnables.add(runnable);
        return this;
    }

    /**
     * Chains this underlying {@link Runnable} with the specified one. The specified runnable will
     * be executed at the end.
     */
    public JkRunnables<T> append(Runnable chainedRunnable) {
        this.runnables.add(chainedRunnable);
        return this;
    }

    /**
     * Chains this specified {@link Runnable} with the underlying one. The specified runnable will
     * be executed first.
     */
    public JkRunnables<T> prepend(Runnable chainedRunnable) {
        this.runnables.add(0, chainedRunnable);
        return this;
    }

    @Override
    public void run() {
        runnables.forEach(Runnable::run);
    }

}
