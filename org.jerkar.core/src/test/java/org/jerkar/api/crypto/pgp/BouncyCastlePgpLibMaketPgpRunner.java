package org.jerkar.api.crypto.pgp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Jerkar uses BouncyCastle to perform PGP task. The bouncyCastle is embedded in
 * the Jerkar jar (to not pollute classpath and ease distribution) so want to
 * make as small as possible.
 * 
 * To achieve this, we run the needed methods in a classloader keeping track of
 * the really used class then we create a new bouncyCastle lib without the
 * unnecessary classes.
 * 
 * @author Jerome Angibaud
 */
@SuppressWarnings("javadoc")
public class BouncyCastlePgpLibMaketPgpRunner {

    private static final String PGPUTILS_CLASS_NAME = "org.jerkar.api.crypto.pgp.PgpUtils";

    public static void main(String[] args) throws Exception {
        final URL jar = JkPgp.class.getResource("bouncycastle-all-152.jar");
        final Set<String> classNames = new HashSet<>();
        final Class<?> PGPUTILS_CLASS = JkClassLoader.current().sibling(jar)
                .printingSearchedClasses(classNames).load(PGPUTILS_CLASS_NAME);
        testSignAndVerify(PGPUTILS_CLASS);

        final List<String> list = new ArrayList<>(classNames);
        Collections.sort(list);
        JkLog.info(list);
        removeUnusedClass(new File(jar.getFile()), new File("bouncycastle-pgp-152.jar"), classNames);
    }

    private static void testSignAndVerify(Class<?> pgpClass) {
        final File pubFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("pubring.gpg"));
        final File secringFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("secring.gpg"));
        final File signatureFile = JkUtilsFile.createFileIfNotExist(new File(
                "build/output/test-out/signature-runner.asm"));
        final File sampleFile = JkUtilsFile.fromUrl(JkPgpTest.class
                .getResource("sampleFileToSign.txt"));

        sign(sampleFile, signatureFile, "jerkar", secringFile, pgpClass);
        verify(sampleFile, signatureFile, pubFile, pgpClass);
        try {
            sign(sampleFile, signatureFile, "bad password", secringFile, pgpClass);
        } catch (final RuntimeException re) {
            // fail silently
        }

    }

    public void testSignWithBadSignature(Class<?> pgpClass) {
        final File pubFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("pubring.gpg"));
        final File secringFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("secring.gpg"));
        final JkPgp pgp = JkPgp.of(pubFile, secringFile, "basPassword");
        final File signatureFile = JkUtilsFile.createFileIfNotExist(new File(
                "build/output/test-out/signature-fake.asm"));
        final File sampleFile = JkUtilsFile.fromUrl(JkPgpTest.class
                .getResource("sampleFileToSign.txt"));
        pgp.sign(sampleFile, signatureFile);
    }

    private static void sign(File fileToSign, File output, String password, File secRing,
            Class<?> clazz) {
        final char[] pass;
        if (password == null) {
            pass = new char[0];
        } else {
            pass = password.toCharArray();
        }
        JkUtilsAssert.isTrue(secRing != null,
                "You must supply a secret ring file (as secring.gpg) to sign files");
        JkUtilsReflect.invokeStaticMethod(clazz, "sign", fileToSign, secRing, output, pass, true);
    }

    private static void verify(File fileToVerify, File signature, File pubRing, Class<?> clazz) {
        JkUtilsReflect.invokeStaticMethod(clazz, "verify", fileToVerify, pubRing, signature);
    }

    private static void removeUnusedClass(File jar, File outputJar, Set<String> classes)
            throws Exception {
        final ZipInputStream zin = new ZipInputStream(new FileInputStream(jar));
        final ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputJar));
        final byte[] buf = new byte[1024];
        ZipEntry entry = zin.getNextEntry();
        int removecount = 0;
        while (entry != null) {
            final String name = entry.getName();
            System.out.print(name + " : ");
            if (name.endsWith(".class")) {
                final String className = JkUtilsString.substringBeforeLast(name.replace('/', '.'),
                        ".class");
                if (!classes.contains(className)) {
                    removecount++;
                    System.out.println("removed");
                    entry = zin.getNextEntry();
                    continue;
                }
            }
            zout.putNextEntry(new ZipEntry(name));
            int len;
            while ((len = zin.read(buf)) > 0) {
                zout.write(buf, 0, len);
            }
            System.out.println("kept");
            entry = zin.getNextEntry();
        }
        System.out.println("removed entry count :" + removecount);
        zin.close();
        zout.close();
    }

}
