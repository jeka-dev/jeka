package org.jerkar.tool.builtins.java;

import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.JkDoc;

/**
 * Standard options for packaging java projects.
 */
public class JkJavaPackOptions {

    /** When true, the produced artifacts are signed with PGP */
    @JkDoc("When true, the produced artifacts are signed with PGP.")
    public boolean signWithPgp;

    /** When true, tests classes and sources are packed in jars.*/
    @JkDoc("When true, tests classes and sources are packed in jars.")
    public Boolean tests;

    /** Comma separated list ofMany algorithm to use to produce checksums (ex : 'sha-1,md5'). */
    @JkDoc("Comma separated list ofMany algorithm to use to produce checksums (ex : 'sha-1,md5').")
    public String checksums;

    /** When true, javadoc is created and packed in a jar file.*/
    @JkDoc("When true, javadoc is created and packed in a jar file.")
    public Boolean javadoc;

    /**
     * Returns the checksums algorithms to checksum artifact files.
     */
    public String[] checksums() {
        return JkUtilsString.splitTrimed(checksums, ",");
    }


}
