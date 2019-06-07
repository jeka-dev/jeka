package org.jerkar.api.crypto.pgp;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.PGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.jerkar.api.file.JkPathFile;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsThrowable;

final class PgpUtils {

    private static final int HASH_ALGO = PGPUtil.SHA1;

    public static boolean verify(Path fileToVerify, Path pubringFile, Path signatureFile) {

        try (final InputStream streamToVerify = Files.newInputStream(fileToVerify);
             final InputStream signatureStream = Files.newInputStream(signatureFile);
             final InputStream pubringStream = Files.newInputStream(pubringFile)) {
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

    public static void sign(Path fileToSign, Path secringFile, String keyName, Path signatureFile, char[] pass,
                            boolean armor) {
        JkUtilsAssert.isTrue(Files.exists(fileToSign), fileToSign + " not found.");
        JkUtilsAssert.isTrue(Files.exists(secringFile), secringFile + " not found.");
        JkPathFile.of(signatureFile).createIfNotExist();
        try (final InputStream toSign = Files.newInputStream(fileToSign);
             final InputStream keyRing = Files.newInputStream(secringFile);
             final OutputStream out = Files.newOutputStream(signatureFile)) {
            sign(toSign, keyRing, keyName, out, pass, armor);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void sign(InputStream toSign, InputStream keyRing, String keyName, OutputStream out, char[] pass,
            boolean armor) {
        if (armor) {
            out = new ArmoredOutputStream(out);
        }
        final PGPSecretKey pgpSecretKey = readSecretKey(keyRing, keyName);
        try {
            final PGPDigestCalculatorProvider pgpDigestCalculatorProvider = new BcPGPDigestCalculatorProvider();
            final PBESecretKeyDecryptor secretKeyDecryptor = new BcPBESecretKeyDecryptorBuilder(
                    pgpDigestCalculatorProvider).build(pass);
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
                throw new IllegalStateException("Secret key password is probably wrong.", e);
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
        throw new IllegalArgumentException("Can't find a signing key in keyring having a name starting with " + prefix);
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

        public InnerPGPObjectFactory(InputStream in, KeyFingerPrintCalculator fingerPrintCalculator) {
            this.in = new BCPGInputStream(in);
            this.fingerPrintCalculator = fingerPrintCalculator;
        }

        public PGPSecretKeyRing nextSecretKey() {
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
                throw new IllegalArgumentException(
                        "Provided PGP file does not contain only secret key."
                                + " Was expecting a file containing secret key only. ");
            }
        }

    }

    private PgpUtils() {
        // Do nothing
    }

}
