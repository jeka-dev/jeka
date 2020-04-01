package dev.jeka.core.api.function;

/**
 * A mutable container for {@link Runnable}. From this object you can replace the underlying {@link Runnable} or
 * chain it with other ones.
 */
public class JkRunnables<T> implements Runnable {

    private Runnable runnable;

    /**
     * For parent chaining
     */
    public final T _;

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
        return new JkRunnables(null, runnable);
    }

    /**
     * Creates a {@link JkRunnables} delegating to a no-op runnable.
     */
    public static JkRunnables<Void> noOp() {
        return of(() -> {});
    }

    /**
     * Same as {@link #noOp()} but providing parent chaining
     */
    public static <T> JkRunnables<T> noOp(T parent) {
        return of(parent, () -> {});
    }

    private JkRunnables(T parent, Runnable runnable) {
        this.runnable = runnable;
        this._ = parent;
    }

    /**
     * Set the specified {@link Runnable} as the unique underlying {@link Runnable} for this container.
     */
    public JkRunnables<T> set(Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    /**
     * Chains this underlying {@link Runnable} with the specified one. The specified runnable will
     * be executed at the end.
     */
    public JkRunnables<T> append(Runnable chainedRunnable) {
        final Runnable oldRunnable = this.runnable;
        this.runnable = () -> {oldRunnable.run(); chainedRunnable.run();};
        return this;
    }

    /**
     * Chains this specified {@link Runnable} with the underlying one. The specified runnable will
     * be executed first.
     */
    public JkRunnables<T> prepend(Runnable chainedRunnable) {
        final Runnable oldRunnable = this.runnable;
        this.runnable = () -> {oldRunnable.run(); chainedRunnable.run();};
        return this;
    }

    @Override
    public void run() {
        runnable.run();
    }

}
