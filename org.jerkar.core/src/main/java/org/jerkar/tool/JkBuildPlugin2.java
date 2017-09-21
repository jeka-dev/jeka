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

    protected abstract void apply();

}
