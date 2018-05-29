package org.jerkar.tool;

public abstract class JkPlugin2 {

    protected final JkBuild build;

    protected JkPlugin2(JkBuild build) {
        this.build = build;
    }

    protected void setupBuild() {
    }

    protected void doDefault() {
    }
}
