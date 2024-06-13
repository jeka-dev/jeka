/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
