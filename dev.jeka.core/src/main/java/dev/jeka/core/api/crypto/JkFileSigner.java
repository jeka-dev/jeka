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
