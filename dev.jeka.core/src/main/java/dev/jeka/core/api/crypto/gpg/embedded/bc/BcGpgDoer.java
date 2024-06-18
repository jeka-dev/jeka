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

package dev.jeka.core.api.crypto.gpg.embedded.bc;

import dev.jeka.core.api.crypto.gpg.JkInternalGpgDoer;
import dev.jeka.core.api.utils.JkUtilsThrowable;
import org.bouncycastle.bcpg.*;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.*;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

final class BcGpgDoer implements JkInternalGpgDoer {

    private static final int HASH_ALGO = HashAlgorithmTags.SHA1;

    // Accessed through reflection
    static BcGpgDoer of() {
        return new BcGpgDoer();
    }

    public boolean verify(Path fileToVerify, Path signatureFile, Path publicRingPath) {
        try (final InputStream streamToVerify = Files.newInputStream(fileToVerify);
             final InputStream signatureStream = Files.newInputStream(signatureFile);
             final InputStream pubringStream = Files.newInputStream(publicRingPath)) {
            return verify(streamToVerify, signatureStream, pubringStream);
        } catch (final IOException | PGPException e) {
            throw JkUtilsThrowable.unchecked(e);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Error with one of this file : signatureFile = "
                    + signatureFile);
        }
    }

    static boolean verify(InputStream streamToVerify, InputStream signatureStream,
            InputStream keyInputStream) throws IOException, PGPException {

        final InputStream sigInputStream = PGPUtil.getDecoderStream(new BufferedInputStream(
                signatureStream));

        final KeyFingerPrintCalculator fingerPrintCalculator = new JcaKeyFingerprintCalculator();
        final PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(sigInputStream,
                fingerPrintCalculator);
        final PGPSignatureList signatureList;
        final Object gpgObject = pgpObjectFactory.nextObject();
        if (gpgObject == null) {
            throw new IllegalArgumentException("no PGP signature found in " + sigInputStream);
        }
        if (gpgObject instanceof PGPCompressedData) {
            final PGPCompressedData compressedData = (PGPCompressedData) gpgObject;
            final PGPObjectFactory compressedPgpObjectFactory = new PGPObjectFactory(
                    compressedData.getDataStream(), fingerPrintCalculator);
            signatureList = (PGPSignatureList) compressedPgpObjectFactory.nextObject();
        } else {
            signatureList = (PGPSignatureList) gpgObject;
        }

        final PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(keyInputStream), fingerPrintCalculator);
        final InputStream bufferedStream = new BufferedInputStream(streamToVerify);
        final PGPSignature signature = signatureList.get(0);
        final PGPPublicKey publicKey = pgpPubRingCollection.getPublicKey(signature.getKeyID());

        final PGPContentVerifierBuilderProvider builderProvider = new BcPGPContentVerifierBuilderProvider();
        signature.init(builderProvider, publicKey);
        int character;
        while ((character = bufferedStream.read()) >= 0) {
            signature.update((byte) character);
        }
        return signature.verify();
    }

    @Override
    public void sign(InputStream streamToSign, OutputStream signatureStream, Path secretRingPath, String keyName, char[] pass, boolean armor) {
        try (final InputStream keyRing = Files.newInputStream(secretRingPath)) {
            sign(streamToSign, keyRing, keyName, signatureStream, pass, armor);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void sign(InputStream streamToSign, OutputStream signatureStream,
                     String asciiKey, String keyPassphrase, boolean armorSignature) {

    }

    static void sign(InputStream toSign, InputStream keyRing, String keyName, OutputStream out, char[] pass,
            boolean armorSignature) {
        if (armorSignature) {
            out = new ArmoredOutputStream(out);
        }
        final PGPSecretKey pgpSecretKey = readSecretKey(keyRing, keyName);

        doSign(toSign, out, pass, pgpSecretKey);
    }

    private static void doSign(InputStream toSign, OutputStream out, char[] pass, PGPSecretKey pgpSecretKey) {
        final PGPDigestCalculatorProvider pgpDigestCalculatorProvider = new BcPGPDigestCalculatorProvider();
        final PBESecretKeyDecryptor secretKeyDecryptor = new BcPBESecretKeyDecryptorBuilder(
                pgpDigestCalculatorProvider).build(pass);
        try {
            final PGPPrivateKey pgpPrivKey = pgpSecretKey.extractPrivateKey(secretKeyDecryptor);
            final int secretKeyAlgo = pgpSecretKey.getPublicKey().getAlgorithm();
            final PGPContentSignerBuilder signerBuilder = new BcPGPContentSignerBuilder(secretKeyAlgo, HASH_ALGO);
            final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(signerBuilder);
            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);
            final BCPGOutputStream bcpgOut = new BCPGOutputStream(out);
            final InputStream fileInputStream = new BufferedInputStream(toSign);
            int ch;
            while ((ch = fileInputStream.read()) >= 0) {
                signatureGenerator.update((byte) ch);
            }
            fileInputStream.close();
            signatureGenerator.generate().encode(bcpgOut);
            out.close();
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        } catch (final PGPException e) {
            if (e.getMessage().equals("checksum mismatch at 0 of 20")) {
                String passMessage = pass.length == 0 ? "empty" : "'" + pass[0] + "***********'";
                throw new IllegalStateException("Secret key password is probably wrong. Was " + passMessage, e);
            }
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private static PGPSecretKey readSecretKey(InputStream keyRingIs, String prefix) {
        for (final PGPSecretKeyRing keyRing : extractSecrectKeyRings(keyRingIs)) {
            final Iterator<PGPSecretKey> keyIter = keyRing.getSecretKeys();
            while (keyIter.hasNext()) {
                final PGPSecretKey key = keyIter.next();
                if (key.isSigningKey()) {
                    String keyname = key.getUserIDs().hasNext() ? key.getUserIDs().next().toString() : "";
                    if (keyname.startsWith(prefix)) {
                        return key;
                    }
                }
            }
        }
        throw new IllegalStateException("Can't find signing key in key ring having name starting with " + prefix);
    }

    private static List<PGPSecretKeyRing> extractSecrectKeyRings(InputStream inputStream) {
        InputStream decodedInput;
        try {
            decodedInput = PGPUtil.getDecoderStream(inputStream);
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        final KeyFingerPrintCalculator fingerPrintCalculator = new JcaKeyFingerprintCalculator();
        final InnerPGPObjectFactory pgpFact = new InnerPGPObjectFactory(decodedInput,
                fingerPrintCalculator);
        PGPSecretKeyRing secKeyRing;
        final List<PGPSecretKeyRing> result = new LinkedList<>();
        while ((secKeyRing = pgpFact.nextSecretKey()) != null) {
            result.add(secKeyRing);
        }
        return result;
    }

    private static class InnerPGPObjectFactory {

        private final BCPGInputStream in;
        private final KeyFingerPrintCalculator fingerPrintCalculator;

        InnerPGPObjectFactory(InputStream in, KeyFingerPrintCalculator fingerPrintCalculator) {
            this.in = new BCPGInputStream(in);
            this.fingerPrintCalculator = fingerPrintCalculator;
        }

        PGPSecretKeyRing nextSecretKey() {
            int tag;
            try {
                tag = in.nextPacketTag();
            } catch (final IOException e1) {
                throw JkUtilsThrowable.unchecked(e1);
            }
            if (tag == -1) {
                return null;
            } else if (tag == PacketTags.SECRET_KEY) {
                try {
                    return new PGPSecretKeyRing(in, fingerPrintCalculator);
                } catch (final PGPException e) {
                    throw JkUtilsThrowable.unchecked(e, "Can't create secret key object.");
                } catch (final IOException e) {
                    throw JkUtilsThrowable.unchecked(e);
                }
            } else {
                throw new IllegalStateException(
                        "Provided PGP file does not contain only secret key."
                                + " Was expecting a file containing secret key only. ");
            }
        }
    }

    private BcGpgDoer() {
        // Do nothing
    }

    private static void signAscKey(InputStream toSign, OutputStream out, String ascKey, String keyPassphrase) {
        throw new IllegalStateException("not implemented");
    }

}
