package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;

/**
 * Standard options for packaging java projects.
 */
public class JkJavaPackOptions {

    /** Comma separated list of algorithm to use to produce checksums (ex : 'sha-1,md5'). */
    @JkDoc("Comma separated list of algorithms to use to produce checksums (e.g. 'sha-1,md5,sha-256').")
    public String checksums;

    /** When true, javadoc is created and packed in a jar file.*/
    @JkDoc("If true, javadoc jar is added in the list of artifact to produce/publish.")
    public boolean javadoc;

    /** When true, sources are packed in a jar file.*/
    @JkDoc("If true, sources jar is added in the list of artifact to produce/publish.")
    public boolean sources = true;

    /** When true, tests classes and sources are packed in jars.*/
    @JkDoc("If true, tests jar is added in the list of artifact to produce/publish.")
    public boolean tests;

    @JkDoc("If true, test-sources jar is added in the list of artifact to produce/publish.")
    public boolean testSources;

    /**
     * Returns the checksums algorithms to checksum artifact files.
     */
    public String[] checksums() {
        return JkUtilsString.splitTrimmed(checksums, ",");
    }


}
