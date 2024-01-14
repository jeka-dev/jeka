package dev.jeka.core.api.crypto;

import java.io.UncheckedIOException;
import java.nio.file.Path;

@FunctionalInterface
public interface JkFileSigner {

    /**
     * Signs the specified file and writes the signature to the target signature file.
     *
     * @param fileToSgn the path of the file to be signed
     * @param targetSignatureFile the path of the signature file to be created
     * @throws UncheckedIOException if an I/O error occurs while signing the file
     */
    void sign(Path fileToSgn, Path targetSignatureFile);

    /**
     * Signs the specified file producing a signature file at standard location.
     *
     * @param fileToSign the file to be signed
     * @return the path of the signature file
     */
    default Path sign(Path fileToSign) {
        Path result = fileToSign.getParent().resolve(fileToSign.getFileName().toString() + ".asc");
        sign(fileToSign, result);
        return result;
    }
}
