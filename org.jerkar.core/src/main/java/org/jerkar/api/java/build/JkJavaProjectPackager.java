package org.jerkar.api.java.build;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates different Java archive a project may produce (binary, doc, source, fat jar...)
 */
@Deprecated // Experimental !!!!
public class JkJavaProjectPackager implements Cloneable {

    /**
     * A filter to exclude signature files to jar
     */
    public static final JkPathFilter EXCLUDE_SIGNATURE_FILTER = JkPathFilter.exclude("meta-inf/*.rsa", "meta-inf/*.dsa", "meta-inf/*.sf").caseSensitive(false);

    /**
     * Creates a {@link JkJavaProjectPackager} for the specified java build.
     */
    public static JkJavaProjectPackager of(JkJavaProject project) {
        return new JkJavaProjectPackager(project);
    }

    private final JkJavaProject project;

    // -------------------- extra content --------------------------

    private JkManifest manifest;

    private JkFileTreeSet extraFilesInJar = JkFileTreeSet.empty();

    // ---------------------- options -------------------------------

    private Set<String> checkSums;

    private boolean doJar = true;

    private boolean doTest = false;

    private boolean doSources = true;

    private boolean doFatJar = false;

    private String fatJarSuffix = "fat";

    private JkPathFilter fatJarEntryFilter = EXCLUDE_SIGNATURE_FILTER;

    private JkPgp pgp = null;

    private JkJavaProjectPackager(JkJavaProject project) {
        this.project = project;
        final File manifestFile = new File(project.structure().classDir(), "META-INF/MANIFEST.MF");
        if (manifestFile.exists()) {
            this.manifest = JkManifest.of(manifestFile);
        }
        this.checkSums = new HashSet<String>();
    }

    /**
     * The jar file that will be generated for the main artifact.
     */
    public File jarFile() {
        final String suffix = !JkUtilsString.isBlank(fatJarSuffix) ? "" :  "-original";
        return new File(project.structure().outputDir(), project.artifactName() + suffix + ".jar");
    }

    /**
     * The jar file that will be generated the jar for the specified classifier.
     */
    public File jarFile(String classifier) {
        return new File(project.structure().outputDir(), project.artifactName() + "-" + classifier + ".jar");
    }

    /**
     * The jar containing the source files.
     */
    public File jarSourceFile() {
        return new File(project.structure().outputDir(), project.artifactName() + "-sources.jar");
    }

    /**
     * The jar containing the test classes.
     */
    public File jarTestFile() {
        return new File(project.structure().outputDir(), project.artifactName() + "-test.jar");
    }

    /**
     * The jar containing the test source files.
     */
    public File jarTestSourceFile() {
        return new File(project.structure().outputDir(), project.artifactName() + "-test-sources.jar");
    }

    /**
     * The jar standing for the fat jar (aka uber jar)
     */
    public File fatJarFile() {
        final String suffix = JkUtilsString.isBlank(fatJarSuffix) ? "" :  "-" + fatJarSuffix;
        return new File(project.structure().outputDir(), project.artifactName() + suffix + ".jar");
    }

    /**
     * The jar containing the javadoc
     */
    public File javadocFile() {
        return new File(project.structure().outputDir(), project.artifactName() + "-javadoc.jar");
    }

    /**
     * Produces all the artifact files.
     */
    public void pack() {
        if (manifest != null && !manifest.isEmpty()) {
            manifest.writeToStandardLocation(project.structure().classDir());
        }
        if (doJar && !JkUtilsFile.isEmpty(project.structure().classDir(), false)) {
            JkFileTreeSet.of(project.structure().classDir()).and(extraFilesInJar)
                    .zip().to(jarFile())
                    .md5If(checkSums.contains("MD5"))
                    .sha1If(checkSums.contains("SHA-1"));
        }
        final JkFileTreeSet sourceAndResources = project.structure().sources().and(project.structure().resources());
        if (doSources && sourceAndResources.countFiles(false) > 0) {
            project.structure().sources().and(project.structure().resources()).and(extraFilesInJar).zip().to(jarSourceFile());
        }
        if (doTest) {
            JkFileTreeSet.of(project.structure().testClassDir()).and(extraFilesInJar).zip().to(jarTestFile());
        }
        if (doTest && doSources && !project.structure().testSources().files(false).isEmpty()) {
            project.structure().testSources().and(project.structure().testResources()).and(extraFilesInJar).zip().to(jarTestSourceFile());
        }
        if (doFatJar) {
            JkFileTreeSet.of(project.structure().classDir()).and(extraFilesInJar)
                    .zip().merge(project.depResolver().resolver().get(JkJavaBuild.RUNTIME))
                    .to(fatJarFile(), fatJarEntryFilter).md5If(checkSums.contains("MD5"))
                    .sha1If(checkSums.contains("SHA-1"));
        }
        if (pgp != null) {
            pgp.sign(jarFile(), jarSourceFile(), jarTestFile(), jarTestSourceFile(), fatJarFile(),
                    javadocFile());
            JkLog.done();
        }
    }

    public void deleteArtifacts() {
        JkUtilsFile.deleteIfExist(jarFile());
        JkUtilsFile.deleteIfExist(new File(jarFile().getAbsolutePath() + ".sha1"));
        JkUtilsFile.deleteIfExist(new File(jarFile().getAbsolutePath() + ".md5"));
        JkUtilsFile.deleteIfExist(jarSourceFile());
        JkUtilsFile.deleteIfExist(jarTestFile());
        JkUtilsFile.deleteIfExist(jarTestSourceFile());
        JkUtilsFile.deleteIfExist(fatJarFile());
        JkUtilsFile.deleteIfExist(new File(fatJarFile().getAbsolutePath() + ".sha1"));
        JkUtilsFile.deleteIfExist(new File(fatJarFile().getAbsolutePath() + ".md5"));
        File[] sigFiles = JkPgp.drySignatureFiles(jarFile(), jarSourceFile(), jarTestFile(), jarTestSourceFile(), fatJarFile(),
                javadocFile());
        for (File file : sigFiles) {
            JkUtilsFile.deleteIfExist(file);
        }
    }

    @Override
    public JkJavaProjectPackager clone() {
        try {
            JkJavaProjectPackager result = (JkJavaProjectPackager) super.clone();
            result.checkSums = new HashSet<String>(this.checkSums);
            return result;
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------- setters -----------------------


    public JkJavaProjectPackager setManifest(JkManifest manifest) {
        this.manifest = manifest;
        return this;
    }

    public JkJavaProjectPackager setDoJar(boolean doJar) {
        this.doJar = doJar;
        return this;
    }

    public JkJavaProjectPackager setDoTest(boolean doTest) {
        this.doTest = doTest;
        return this;
    }

    public JkJavaProjectPackager setDoSources(boolean doSources) {
        this.doSources = doSources;
        return this;
    }

    public JkJavaProjectPackager setDoFatJar(boolean doFatJar) {
        this.doFatJar = doFatJar;
        return this;
    }

    public JkJavaProjectPackager setFatJarSuffix(String fatJarSuffix) {
        this.fatJarSuffix = fatJarSuffix;
        return this;
    }

    public JkJavaProjectPackager setFatJarEntryFilter(JkPathFilter fatJarEntryFilter) {
        this.fatJarEntryFilter = fatJarEntryFilter;
        return this;
    }

    public JkJavaProjectPackager setPgp(JkPgp pgp) {
        this.pgp = pgp;
        return this;
    }

    public JkJavaProjectPackager setExtraFilesInJar(JkFileTreeSet extraFilesInJar) {
        this.extraFilesInJar = extraFilesInJar;
        return this;
    }

    public JkJavaProjectPackager addCheckSum(String ... checksums) {
        for (String checksum : checksums) {
            this.checkSums.add(checksum.toUpperCase().trim());
        }
        return this;
    }
}
