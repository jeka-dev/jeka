package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.tool.JkDoc;

/**
 * Standard options for packaging java projects.
 */
public class JkJavaPackOptions {

    /** When true, javadoc is created and packed in a jar file.*/
    @JkDoc("If true, javadoc jar is added in the list of artifact to produce/publish.")
    public boolean javadoc = true;

    /** When true, sources are packed in a jar file.*/
    @JkDoc("If true, sources jar is added in the list of artifact to produce/publish.")
    public boolean sources = true;

}
