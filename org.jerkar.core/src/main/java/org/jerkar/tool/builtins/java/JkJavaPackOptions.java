package org.jerkar.tool.builtins.java;

import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.JkDoc;

/**
 * Standard options for packaging java projects.
 */
public class JkJavaPackOptions {

    /** When true, the produced artifacts are signed with PGP */
    @JkDoc("If true, the produced artifacts are signed with PGP.")
    public boolean signWithPgp;

    /** Comma separated list of algorithm to use to produce checksums (ex : 'sha-1,md5'). */
    @JkDoc("Comma separated list of algorithms to use to produce checksums (ex : 'sha-1,md5,sha-256').")
    public String checksums;

    /** When true, javadoc is created and packed in a jar file.*/
    @JkDoc("If true, javadoc jar is added in the list of artifact to produce.")
    public boolean javadoc;

    /** When true, sources are packed in a jar file.*/
    @JkDoc("If true, sources jar is added in the list of artifact to produce.")
    public boolean sources;

    /** When true, tests classes and sources are packed in jars.*/
    @JkDoc("If true, tests jar is added in the list of artifact to produce.")
    public boolean tests;

    /**
     * Returns the checksums algorithms to checksum artifact files.
     */
    public String[] checksums() {
        return JkUtilsString.splitTrimed(checksums, ",");
    }


}
