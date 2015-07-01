package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.crypto.pgp.JkPgp;

/**
 * Stands for a repository for deploying artifacts.
 * 
 * @author Jerome Angibaud
 */
public final class JkPublishRepo implements Serializable {

	private static final long serialVersionUID = 1L;

	private final JkRepo jkRepo;

	private final JkPublishFilter filter;

	private final JkPgp requirePgpSign;

	// SHA-1, SHA-256, MD5
	private final Set<String> digesterAlgorithms;

	private final boolean uniqueSnapshot;


	public static JkPublishRepo of(JkRepo jkRepo, JkPublishFilter filter) {
		return new JkPublishRepo(jkRepo, filter, null, new HashSet<String>(), false);
	}

	public static JkPublishRepo of(JkRepo jkRepo) {
		return new JkPublishRepo(jkRepo, JkPublishFilter.ACCEPT_ALL, null, new HashSet<String>(), false);
	}

	public static JkPublishRepo ofSnapshot(JkRepo jkRepo) {
		return new JkPublishRepo(jkRepo, JkPublishFilter.ACCEPT_SNAPSHOT_ONLY, null, new HashSet<String>(), false);
	}

	public static JkPublishRepo ofRelease(JkRepo jkRepo) {
		return new JkPublishRepo(jkRepo, JkPublishFilter.ACCEPT_RELEASE_ONLY, null, new HashSet<String>(), false);
	}

	private JkPublishRepo(JkRepo jkRepo, JkPublishFilter filter, JkPgp requirePgpSign, Set<String> digesters, boolean uniqueSnapshot) {
		super();
		this.jkRepo = jkRepo;
		this.filter = filter;
		this.requirePgpSign = requirePgpSign;
		this.digesterAlgorithms = Collections.unmodifiableSet(digesters);
		this.uniqueSnapshot = uniqueSnapshot;
	}

	public JkRepo repo() {
		return jkRepo;
	}

	public JkPublishFilter filter() {
		return filter;
	}


	public JkPgp requirePgpSign() {
		return requirePgpSign;
	}

	public Set<String> digesterAlgorithms() {
		return digesterAlgorithms;
	}

	/**
	 * When <code>true</code> the snapshot version are replaced with a timestamped version (ala Maven 3)
	 * 
	 * @see http://stackoverflow.com/questions/4275466/how-do-you-deal-with-maven-3-timestamped-snapshots-efficiently
	 */
	public boolean uniqueSnapshot() {
		return uniqueSnapshot;
	}


	public JkPublishRepo withSigner(JkPgp signer) {
		return new JkPublishRepo(jkRepo, filter, signer, digesterAlgorithms, uniqueSnapshot);
	}

	/**
	 * Returns a {@link JkPublishRepo} but adding the specified digester algorithms.
	 * When adding a digester algorith (as MD5 or SHA-1), all the published artifact will be
	 * hashed with the specified algorithms and the hashes will be published along the artifacts.
	 * @param algorithms algorithms name as accepted by {@link MessageDigest} constructor.
	 */
	public JkPublishRepo andDigesters(String... algorithms) {
		final HashSet<String> set = new HashSet<String>(this.digesterAlgorithms);
		set.addAll(Arrays.asList(algorithms));
		return new JkPublishRepo(jkRepo, filter, requirePgpSign, set, uniqueSnapshot);
	}

	public JkPublishRepo andMd5Digesters() {
		return andDigesters("MD5");
	}

	public JkPublishRepo andSha1Digesters() {
		return andDigesters("SHA-1");
	}

	public JkPublishRepo withUniqueSnapshot(boolean uniqueSnapShot) {
		return new JkPublishRepo(jkRepo, filter, requirePgpSign, this.digesterAlgorithms, uniqueSnapShot);
	}

}