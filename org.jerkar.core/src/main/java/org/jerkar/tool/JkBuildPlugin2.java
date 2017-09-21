package org.jerkar.tool;

// Experimental
public abstract class JkBuildPlugin2<T extends JkBuild> {

    private final T build;

    protected JkBuildPlugin2(T build) {
        this.build = build;
    }

    protected final T build() {
        return build;
    }

    /**
     * Modify the enclosed build instance ( {@link #build()}) in accordance of what this plugin is supposed to do.
     */
    protected abstract void apply();

}
