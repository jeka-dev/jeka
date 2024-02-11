package dev.jeka.core.api.crypto;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.utils.JkUtilsIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Functional interface for signing data.
 */
@FunctionalInterface
public interface JkSigner extends JkFileSigner {

    /**
     * Signs the data in the specified input stream and writes the signature to the output stream.
     *
     * @param streamToSign the input stream containing the data to be signed
     * @param signatureString the output stream to write the signature string to
     */
    void sign(InputStream streamToSign, OutputStream signatureString);

    /**
     * @see JkFileSigner#sign(Path, Path)
     */
    @Override
    default void sign(Path fileToSgn, Path targetSignatureFile) {
        JkPathFile.of(targetSignatureFile).deleteIfExist().createIfNotExist();
        try(InputStream is = JkUtilsIO.inputStream(fileToSgn.toFile());
            OutputStream os = JkUtilsIO.outputStream(targetSignatureFile.toFile(), false)) {

            sign(is, os);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
