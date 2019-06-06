package org.jerkar.api.crypto.pgp;

import org.jerkar.api.java.JkUrlClassLoader;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsString;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
        final Class<?> PGPUTILS_CLASS = JkUrlClassLoader.ofCurrent().getSibling(jar)
                .getSiblingPrintingSearchedClasses(classNames).toJkClassLoader().load(PGPUTILS_CLASS_NAME);
        testSignAndVerify(PGPUTILS_CLASS);

        final List<String> list = new ArrayList<>(classNames);
        Collections.sort(list);
        System.out.println(list);
        removeUnusedClass(new File(jar.getFile()), new File("bouncycastle-pgp-152.jar"), classNames);
    }

    private static void testSignAndVerify(Class<?> pgpClass) throws Exception {
        final Path pubFile = Paths.get(JkPgpTest.class.getResource("pubring.gpg").toURI());
        final Path secringFile = Paths.get(JkPgpTest.class.getResource("secring.gpg").toURI());
        final Path signatureFile = Paths.get("jerkar/output/test-out/signature-runner.asm");
        Files.createDirectories(signatureFile.getParent());
        if (!Files.exists(signatureFile)) {
            Files.createFile(signatureFile);
        }
        final Path sampleFile = Paths.get(JkPgpTest.class.getResource("sampleFileToSign.txt").toURI());

        sign(sampleFile, signatureFile, "jerkar", secringFile, pgpClass);
        verify(sampleFile, signatureFile, pubFile, pgpClass);
        try {
            sign(sampleFile, signatureFile, "bad password", secringFile, pgpClass);
        } catch (final RuntimeException re) {
            // fail silently
        }

    }

    @Test(expected = IllegalStateException.class)
    public void testSignWithBadSignature() throws Exception {
        final Path pubFile = Paths.get(JkPgpTest.class.getResource("pubring.gpg").toURI());
        final Path secringFile = Paths.get(JkPgpTest.class.getResource("secring.gpg").toURI());
        final JkPgp pgp = JkPgp.of(pubFile, secringFile, "badPassword");
        final Path signatureFile = Paths.get(
                "jerkar/output/test-out/signature-fake.asm").toAbsolutePath();
        final Path sampleFile = Paths.get(JkPgpTest.class.getResource("sampleFileToSign.txt").toURI());
        pgp.sign(sampleFile, "", signatureFile);
    }

    private static void sign(Path fileToSign, Path output, String password, Path secRing,
            Class<?> clazz) throws Exception {
        final char[] pass;
        if (password == null) {
            pass = new char[0];
        } else {
            pass = password.toCharArray();
        }
        JkUtilsAssert.isTrue(secRing != null,
                "You must supply a secret ring file (as secring.gpg) to sign files");
        Method method = clazz.getMethod("sign", Path.class, Path.class, Path.class, char[].class, boolean.class);
        method.setAccessible(true);
        method.invoke(null, fileToSign, secRing, output, pass, true);
    }

    private static void verify(Path fileToVerify, Path signature, Path pubRing, Class<?> clazz) throws Exception {
        Method method = clazz.getMethod("verify", Path.class, Path.class, Path.class);
        method.setAccessible(true);
        method.invoke(null, fileToVerify, pubRing, signature);

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
