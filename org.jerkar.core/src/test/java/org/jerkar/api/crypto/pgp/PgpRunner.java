package org.jerkar.api.crypto.pgp;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PgpRunner {

    private static final String PGPUTILS_CLASS_NAME = "org.jerkar.api.crypto.pgp.PgpUtils";

    public static void main(String[] args) throws Exception {
        final Path sampleFile = Paths.get(JkPgpTest.class.getResource("sampleFileToSign.txt").toURI());
        JkPgp.ofDefaultGnuPg().withSecretRingPassword("").sign(sampleFile);
    }

}
