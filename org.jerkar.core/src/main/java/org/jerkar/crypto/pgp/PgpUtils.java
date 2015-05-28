package org.jerkar.crypto.pgp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIO;

final class PgpUtils {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public static boolean verify(File fileToVerify, File pubringFile, File signatureFile) {
		final InputStream streamToVerify = JkUtilsIO.inputStream(fileToVerify);
		final InputStream signatureStream = JkUtilsIO.inputStream(signatureFile);
		final InputStream pubringStream = JkUtilsIO.inputStream(pubringFile);
		try {
			return verify(streamToVerify, signatureStream, pubringStream);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} catch (final PGPException e) {
			throw new RuntimeException(e);
		} finally {
			JkUtilsIO.closeQuietly(streamToVerify, signatureStream, pubringStream);
		}
	}

	public static boolean verify(InputStream streamToVerify,
			InputStream signatureStream, InputStream keyInputStream) throws IOException, PGPException {

		final InputStream sigInputStream = PGPUtil.getDecoderStream(new BufferedInputStream(signatureStream));

		final KeyFingerPrintCalculator fingerPrintCalculator = new JcaKeyFingerprintCalculator();
		final PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(sigInputStream, fingerPrintCalculator);
		final PGPSignatureList signatureList;
		final Object gpgObject = pgpObjectFactory.nextObject();
		if (gpgObject instanceof PGPCompressedData) {
			final PGPCompressedData compressedData = (PGPCompressedData) gpgObject;
			final PGPObjectFactory compressedPgpObjectFactory = new PGPObjectFactory(compressedData.getDataStream(), fingerPrintCalculator);
			signatureList = (PGPSignatureList) compressedPgpObjectFactory.nextObject();
		} else {
			signatureList = (PGPSignatureList) gpgObject;
		}

		final PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(
				PGPUtil.getDecoderStream(keyInputStream), fingerPrintCalculator);
		final InputStream bufferedStream = new BufferedInputStream(streamToVerify);
		final PGPSignature signature = signatureList.get(0);
		final PGPPublicKey publicKey = pgpPubRingCollection.getPublicKey(signature.getKeyID());
		signature.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), publicKey);
		int character;
		while ((character = bufferedStream.read()) >= 0) {
			signature.update((byte) character);
		}
		return signature.verify();
	}

	public static void sign(File fileToSign, File secringFile,
			File signatureFile, char[] pass, boolean armor) {
		JkUtilsFile.assertAllExist(fileToSign, secringFile);
		final InputStream toSign = JkUtilsIO.inputStream(fileToSign);
		final InputStream keyRing = JkUtilsIO.inputStream(secringFile);
		final FileOutputStream out = JkUtilsIO.outputStream(signatureFile);
		sign(toSign, keyRing, out, pass, armor);
		JkUtilsIO.closeQuietly(toSign);
		JkUtilsIO.closeQuietly(keyRing);
		JkUtilsIO.closeQuietly(out);
	}

	public static void sign(InputStream toSign, InputStream keyRing,
			OutputStream out, char[] pass, boolean armor) {

		if (armor) {
			out = new ArmoredOutputStream(out);
		}
		final PGPSecretKey pgpSecretKey = readFirstSecretKey(keyRing);
		try {
			final PGPPrivateKey pgpPrivKey = pgpSecretKey
					.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
					.setProvider("BC").build(pass));
			final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
					new JcaPGPContentSignerBuilder(pgpSecretKey.getPublicKey()
							.getAlgorithm(), PGPUtil.SHA1).setProvider("BC"));
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
			throw new RuntimeException(e);
		} catch (final PGPException e) {
			throw new RuntimeException(e);
		}
	}

	private static PGPSecretKey readFirstSecretKey(InputStream keyRingIs) {
		for (final PGPSecretKeyRing keyRing : extractSecrectKeyRings(keyRingIs)) {
			final Iterator<PGPSecretKey> keyIter = keyRing.getSecretKeys();
			while (keyIter.hasNext()) {
				final PGPSecretKey key = keyIter.next();
				if (key.isSigningKey()) {
					return key;
				}
			}
		}
		throw new IllegalArgumentException(
				"Can't find signing key in key ring.");
	}

	private static List<PGPSecretKeyRing> extractSecrectKeyRings(
			InputStream inputStream) {

		InputStream decodedInput;
		try {
			decodedInput = PGPUtil.getDecoderStream(inputStream);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		final KeyFingerPrintCalculator fingerPrintCalculator = new JcaKeyFingerprintCalculator();
		final InnerPGPObjectFactory pgpFact = new InnerPGPObjectFactory(
				decodedInput, fingerPrintCalculator);
		PGPSecretKeyRing secKeyRing;
		final List<PGPSecretKeyRing> result = new LinkedList<PGPSecretKeyRing>();
		while ((secKeyRing = pgpFact.nextSecretKey()) != null) {
			final PGPSecretKeyRing pgpSecret = secKeyRing;
			result.add(pgpSecret);
		}
		return result;
	}

	private static class InnerPGPObjectFactory {

		private final BCPGInputStream in;
		private final KeyFingerPrintCalculator fingerPrintCalculator;

		public InnerPGPObjectFactory(InputStream in,
				KeyFingerPrintCalculator fingerPrintCalculator) {
			this.in = new BCPGInputStream(in);
			this.fingerPrintCalculator = fingerPrintCalculator;
		}

		public PGPSecretKeyRing nextSecretKey() {
			int tag;
			try {
				tag = in.nextPacketTag();
			} catch (final IOException e1) {
				throw new RuntimeException(e1);
			}
			if (tag == -1) {
				return null;
			} else if (tag == PacketTags.SECRET_KEY) {
				try {
					return new PGPSecretKeyRing(in, fingerPrintCalculator);
				} catch (final PGPException e) {
					throw new RuntimeException(
							"can't create secret key object: " + e);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				throw new IllegalArgumentException(
						"Provided PGP file does not contain only secret key."
								+ " Was expecting a file containing secret key only. ");
			}
		}

	}

}
