package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * Set of repositories to publish to. When publishing you may want deploy your
 * artifact on to a repository or another according some criteria.<br/>
 * For example, you would like to publish snapshot on a repository and release
 * to another one, so each repository registered in JkPublishRepos is associated
 * with a filter that determine if it accepts or not the versioned module to
 * publish.
 *
 * @author Jerome Angibaud
 */
public final class JkPublishRepos implements Iterable<JkPublishRepo>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a JkPublishRepos that publish on the specified repositories.
     */
    public static JkPublishRepos of(JkPublishRepo publishRepo) {
        final List<JkPublishRepo> list = JkUtilsIterable.listOf(publishRepo);
        return new JkPublishRepos(list);
    }

    /**
     * Creates a JkPublishRepos tailored for <a
     * href="http://central.sonatype.org/">OSSRH</a>
     */
    public static JkPublishRepos ossrh(String userName, String password, JkPgp pgp) {
        final JkPublishRepo snapshot = JkPublishRepo
                .ofSnapshot(JkRepo.mavenOssrhDownloadAndDeploySnapshot(userName, password))
                .andSha1Md5Checksums().withUniqueSnapshot(true); // ignored as
        // unique
        // timestamped
        // snapshot not yet supported
        final JkPublishRepo release = JkPublishRepo
                .ofRelease(JkRepo.mavenOssrhDeployRelease(userName, password)).withSigner(pgp)
                .andSha1Md5Checksums();
        return JkPublishRepos.of(snapshot).and(release);
    }

    /**
     * Returns a Maven repository for publishing located at the specified URL.
     */
    public static JkPublishRepos maven(String url) {
        return new JkPublishRepos(JkUtilsIterable.listOf(JkPublishRepo.of(JkRepo.maven(url))));
    }

    /**
     * Returns a Maven repository for publishing located at the specified file.
     */
    public static JkPublishRepos maven(File file) {
        return new JkPublishRepos(JkUtilsIterable.listOf(JkPublishRepo.of(JkRepo.maven(file))));
    }

    /**
     * Returns an Ivy repository for publishing located at the specified file.
     */
    public static JkPublishRepos ivy(File file) {
        return new JkPublishRepos(JkUtilsIterable.listOf(JkPublishRepo.of(JkRepo.ivy(file))));
    }

    /**
     * Returns an Ivy repository for publishing located at the specified URL/
     */
    public static JkPublishRepos ivy(String url) {
        return new JkPublishRepos(JkUtilsIterable.listOf(JkPublishRepo.of(JkRepo.ivy(url))));
    }

    private final List<JkPublishRepo> publishRepos;

    private JkPublishRepos(List<JkPublishRepo> repos) {
        super();
        this.publishRepos = repos;
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but with the specified Pgp elements
     * necessary to sign artifacts.
     */
    public JkPublishRepos withSigner(JkPgp pgp) {
        final List<JkPublishRepo> list = new LinkedList<JkPublishRepo>();
        for (final JkPublishRepo publishRepo : this.publishRepos) {
            list.add(publishRepo.withSigner(pgp));
        }
        return new JkPublishRepos(list);
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but producing a SHA1 check
     * sum for all published artifacts and signature.
     */
    public JkPublishRepos withSha1Checksum() {
        final List<JkPublishRepo> list = new LinkedList<JkPublishRepo>();
        for (final JkPublishRepo publishRepo : this.publishRepos) {
            list.add(publishRepo.andSha1Checksum());
        }
        return new JkPublishRepos(list);
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but producing a MD5 check
     * sum for all published artifacts and signature.
     */
    public JkPublishRepos withMd5Checksum() {
        final List<JkPublishRepo> list = new LinkedList<JkPublishRepo>();
        for (final JkPublishRepo publishRepo : this.publishRepos) {
            list.add(publishRepo.andMd5Checksum());
        }
        return new JkPublishRepos(list);
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but producing a SHA1 and MD5 check
     * sums for all published artifacts and signature.
     */
    public JkPublishRepos withMd5AndSha1Checksum() {
        final List<JkPublishRepo> list = new LinkedList<JkPublishRepo>();
        for (final JkPublishRepo publishRepo : this.publishRepos) {
            list.add(publishRepo.andSha1Md5Checksums());
        }
        return new JkPublishRepos(list);
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but with the specified value
     * for <i>unique snapshot</i> property.<br/>
     * When <i>unique snapshot</i> is <code>true</code>, the published artifact versioned with a Snapshot
     * version, are timestamped so several 'version' on a given snapshot can coexist in the repository.
     * It is the default behavior for Maven 3 while it was the opposit in Maven 2.
     *
     */
    public JkPublishRepos withUniqueSnapshot(boolean uniqueSnapshot) {
        final List<JkPublishRepo> list = new LinkedList<JkPublishRepo>();
        for (final JkPublishRepo publishRepo : this.publishRepos) {
            list.add(publishRepo.withUniqueSnapshot(uniqueSnapshot));
        }
        return new JkPublishRepos(list);
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but with the additional specified publish repositories.
     */
    public JkPublishRepos and(JkPublishRepos others) {
        @SuppressWarnings("unchecked")
        final List<JkPublishRepo> list = JkUtilsIterable.concatLists(this.publishRepos,
                others.publishRepos);
        return new JkPublishRepos(list);
    }

    /**
     * Returns a {@link JkPublishRepo} identical to this one but with the additional specified publish repository.
     */
    public JkPublishRepos and(JkPublishRepo other) {
        final List<JkPublishRepo> list = new LinkedList<JkPublishRepo>(this.publishRepos);
        list.add(other);
        return new JkPublishRepos(list);
    }

    /**
     * Return the individual repository from this set having the specified url.
     * Returns <code>null</code> if no such repository found.
     */
    public JkPublishRepo getRepoHavingUrl(String url) {
        for (final JkPublishRepo repo : this.publishRepos) {
            if (url.equals(repo.repo().url().toExternalForm())) {
                return repo;
            }
        }
        return null;
    }

    @Override
    public Iterator<JkPublishRepo> iterator() {
        return this.publishRepos.iterator();
    }

    /**
     * Returns <code>true</code> if it is required to sign the artifacts with
     * PGP in order to publish the specified version and module.
     */
    public boolean requirePgpSignature(JkVersionedModule versionedModule) {
        for (final JkPublishRepo jkPublishRepo : this.publishRepos) {
            if (jkPublishRepo.filter().accept(versionedModule)
                    && jkPublishRepo.pgpSigner() != null) {
                return true;
            }
        }
        return false;
    }

}
