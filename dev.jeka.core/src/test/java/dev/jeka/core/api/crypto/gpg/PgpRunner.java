package dev.jeka.core.api.crypto.gpg;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PgpRunner {

    private static final String PGPUTILS_CLASS_NAME = "dev.jeka.core.api.crypto.pgp.embedded.bc.PgpUtils";

    public static void main(String[] args) throws Exception {
        System.out.println("ttt".startsWith(""));
        final Path sampleFile = Paths.get(JkGpgTest.class.getResource("sampleFileToSign.txt").toURI());
        JkGpgSigner.ofDefaultGnuPg("", "").sign(sampleFile);
    }

}
