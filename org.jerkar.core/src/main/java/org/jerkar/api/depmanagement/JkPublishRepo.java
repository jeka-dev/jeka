package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.system.JkLocator;

/**
 * Stands for a repository for deploying artifacts.
 * 
 * @author Jerome Angibaud
 */
public final class JkPublishRepo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final JkRepo jkRepo;

    private final JkPublishFilter filter;

    private final JkPgp pgpSigner;

    // SHA-1, SHA-256, MD5
    private final Set<String> checksumAlgorithms;

    private final boolean uniqueSnapshot;

    /**
     * Creates a {@link JkPublishRepo} for publishing on the specified {@link JkRepo} when
     * the specified {@link JkPublishRepo} agree. If the specified filter do not agree to publish
     * a given {@link JkVersionedModule}, a publish of the disagreed module on this {@link JkPublishRepo}
     * will result in an no operation (doing nothing).
     */
    public static JkPublishRepo of(JkRepo jkRepo, JkPublishFilter filter) {
        return new JkPublishRepo(jkRepo, filter, null, new HashSet<>(), false);
    }

    /**
     * Creates a {@link JkPublishRepo} for publishing always on the specified {@link JkRepo}.
     */
    public static JkPublishRepo of(JkRepo jkRepo) {
        return new JkPublishRepo(jkRepo, JkPublishFilter.ACCEPT_ALL, null, new HashSet<>(),
                false);
    }

    /**
     * Creates a repository for publishing locally under <code></code>[USER HOME]/.jerkar/publish</code> folder.
     */
    public static JkPublishRepo local() {
        final File file = new File(JkLocator.jerkarUserHome(), "maven-publish-dir");
        return JkRepo.maven(file).asPublishRepo();
    }

    /**
     * Creates a {@link JkPublishRepo} for publishing snapshot version on the specified {@link JkRepo}.
     * Release versions are not publishable on this {@link JkPublishRepos}
     */
    public static JkPublishRepo ofSnapshot(JkRepo jkRepo) {
        return new JkPublishRepo(jkRepo, JkPublishFilter.ACCEPT_SNAPSHOT_ONLY, null,
                new HashSet<>(), false);
    }

    /**
     * Creates a {@link JkPublishRepo} for publishing non-snapshot version on the specified {@link JkRepo}.
     * Snapshot versions are not publishable on this {@link JkPublishRepos}
     */
    public static JkPublishRepo ofRelease(JkRepo jkRepo) {
        return new JkPublishRepo(jkRepo, JkPublishFilter.ACCEPT_RELEASE_ONLY, null,
                new HashSet<>(), false);
    }

    private JkPublishRepo(JkRepo jkRepo, JkPublishFilter filter, JkPgp requirePgpSign,
            Set<String> digesters, boolean uniqueSnapshot) {
        super();
        this.jkRepo = jkRepo;
        this.filter = filter;
        this.pgpSigner = requirePgpSign;
        this.checksumAlgorithms = Collections.unmodifiableSet(digesters);
        this.uniqueSnapshot = uniqueSnapshot;
    }

    /**
     * Returns the underlying {@link JkRepo}.
     */
    public JkRepo repo() {
        return jkRepo;
    }

    /**
     * Returns the filter used for this {@link JkPublishRepo}.
     * Only modules accepted by this filter will pb published on this repo.
     */
    public JkPublishFilter filter() {
        return filter;
    }

    /**
     * If this repository requires a artifact PGP signature in order to be published, this method
     * returns the the {@link JkPgp} signer for signing artifacts to be published.
     * If no no PGP signer has been defined, this method returns <code>null</code>.
     */
    public JkPgp pgpSigner() {
        return pgpSigner;
    }

    /**
     * Returns the algorithms used ("sha-1" or "md5") for check summing published artifacts.
     * If this, repository does not produces checksum, this methods returns an empty set.
     */
    public Set<String> checksumAlgorithms() {
        return checksumAlgorithms;
    }

    /**
     * When <code>true</code> the snapshot version are replaced with a
     * timestamped version (ala Maven 3) See
     * http://stackoverflow.com/questions/4275466/how-do-you-deal-with-maven-3-
     * timestamped-snapshots-efficiently
     */
    public boolean uniqueSnapshot() {
        return uniqueSnapshot;
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but with the specified Pgp signer.
     * All artifacts published on the returned repository will be automatically signed with the specified
     * Pgp signer, if this one is not <code>null<code>.
     */
    public JkPublishRepo withSigner(JkPgp signer) {
        return new JkPublishRepo(jkRepo, filter, signer, checksumAlgorithms, uniqueSnapshot);
    }

    /*
     * Returns a {@link JkPublishRepo} but adding the specified digester
     * algorithms. When adding a digester algorith (as MD5 or SHA-1), all the
     * published artifact will be hashed with the specified algorithms and the
     * hashes will be published along the artifacts.
     * 
     * @param algorithms algorithms name as accepted by {@link MessageDigest}
     * constructor.
     */
    private JkPublishRepo andChecksums(String... algorithms) {
        final HashSet<String> set = new HashSet<>(this.checksumAlgorithms);
        set.addAll(Arrays.asList(algorithms));
        return new JkPublishRepo(jkRepo, filter, pgpSigner, set, uniqueSnapshot);
    }

    /**
     * Returns a {@link JkPublishRepo} but adding the MD5 checksum algorithm.
     * When adding a checksum algorith (as MD5 or SHA-1), all the published
     * artifact will be hashed with the specified algorithms and the hashes will
     * be published along the artifacts.
     */
    public JkPublishRepo andMd5Checksum() {
        return andChecksums("MD5");
    }

    /**
     * Same as {@link #andMd5Checksum()} but with SHA-1 algo
     */
    public JkPublishRepo andSha1Checksum() {
        return andChecksums("SHA-1");
    }

    /**
     * Returns a {@link JkPublishRepo} but with sha-1 and md5 check summers.
     * All published artifact will be check-summed with sha1 and md5.
     */
    public JkPublishRepo andSha1Md5Checksums() {
        return andSha1Checksum().andMd5Checksum();
    }

    /**
     * Returns a {@link JkPublishRepos} made of this {@link JkPublishRepo} and the specified one.
     */
    public JkPublishRepos and(JkPublishRepo repo) {
        return JkPublishRepos.of(this).and(repo);
    }

    /**
     * Returns a {@link JkPublishRepos} made of this {@link JkPublishRepo} and the specified {@link JkRepo}
     * for artifact having a non-snapshot version.
     */
    public JkPublishRepos andRelease(JkRepo repo) {
        return and(JkPublishRepo.ofRelease(repo));
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but with the specified <i>uniqueSnapshot</i>
     * property. When this property is <code>true</code>n artifact deployed on this repository are timestamped so
     * several artifact from the same snapshot version can coexist in the this repository. <br/>
     * This is the default behavior for artifact deployed with Maven 3, while this is not the case with Maven 2.
     */
    public JkPublishRepo withUniqueSnapshot(boolean uniqueSnapShot) {
        return new JkPublishRepo(jkRepo, filter, pgpSigner, this.checksumAlgorithms,
                uniqueSnapShot);
    }

}