package org.jerkar.api.function;

/**
 * A container for runnable.
 */
public class JkRunnables implements Runnable {

    private Runnable runnable;

    public static JkRunnables of(Runnable runnable) {
        return new JkRunnables(runnable);
    }

    private JkRunnables(Runnable runnable) {
        this.runnable = runnable;
    }

    public JkRunnables set(Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    public JkRunnables chain(Runnable runnable) {
        this.runnable = () -> {this.runnable.run(); runnable.run();};
        return this;
    }

    public JkRunnables clear() {
        this.runnable = () -> {};
        return this;
    }

    @Override
    public void run() {
        runnable.run();
    }




}
