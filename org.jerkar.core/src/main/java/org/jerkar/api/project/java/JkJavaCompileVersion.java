package org.jerkar.api.project.java;

import org.jerkar.api.java.JkJavaVersion;

public final class JkJavaCompileVersion {

    public static final JkJavaCompileVersion V6 = of(JkJavaVersion.V6);

    public static final JkJavaCompileVersion V7 = of(JkJavaVersion.V7);

    public static final JkJavaCompileVersion V8 = of(JkJavaVersion.V8);

    public static final JkJavaCompileVersion V9 = of(JkJavaVersion.V9);

    private static JkJavaCompileVersion of(JkJavaVersion source, JkJavaVersion target) {
        return new JkJavaCompileVersion(source, target);
    }

    private static JkJavaCompileVersion of(JkJavaVersion version) {
        return of(version, version);
    }



    private final JkJavaVersion source;

    private final JkJavaVersion target;

    private JkJavaCompileVersion(JkJavaVersion source, JkJavaVersion target) {
        this.source = source;
        this.target = target;
    }

    public JkJavaVersion source() {
        return source;
    }

    public JkJavaVersion target() {
        return target;
    }
}
