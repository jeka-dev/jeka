package org.jerkar.publishing;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.publishing.JkPublishRepos.JkPublishRepo;
import org.jerkar.utils.JkUtilsIterable;

/**
 * Set of repository to publish to. When publishing you may want deploy your artifact on to a repository or another
 * according some criteria.<br/>
 * For example, you would like to publish snapshot on a repository and release to another one, so each
 * repository registered in JkPublishRepos is associated with a filter that determine if it accepts or not
 * the versionned module to publish.
 * 
 * @author Jerome Angibaud
 */
public final class JkPublishRepos implements Iterable<JkPublishRepo> {

	/**
	 * Creates a JkPublishRepos that publish snaphots on to a specified repository and release on
	 * another one. You can specify if the repositories require to sign published artifacts.
	 */
	public static JkPublishRepos ofSnapshotAndRelease(JkRepo snapshot, boolean snapshotRequirePgpSign, JkRepo optionalRelease, boolean releaseRequirePgpSign) {
		return JkPublishRepos.of(ACCEPT_SNAPSHOT_ONLY, snapshot, snapshotRequirePgpSign).and(ACCEPT_RELEASE_ONLY, optionalRelease, releaseRequirePgpSign);
	}

	/**
	 * Creates a JkPublishRepos that publish snaphots on to a specified repository and release on
	 * another one. The specified repositories does not require to sign artifacts
	 */
	public static JkPublishRepos ofSnapshotAndRelease(JkRepo snapshot, JkRepo optionalRelease) {
		return JkPublishRepos.of(ACCEPT_SNAPSHOT_ONLY, snapshot, false).and(ACCEPT_RELEASE_ONLY, optionalRelease, false);
	}

	/**
	 * Creates a JkPublishRepos that publish on the specified repositories when versionedModule matches
	 * the specified filter.
	 */
	public static JkPublishRepos of(JkPublishFilter filter, JkRepo repo, boolean requirePgpSign) {
		final List<JkPublishRepo> list = JkUtilsIterable.listOf(new JkPublishRepo(repo, filter, requirePgpSign));
		return new JkPublishRepos(list);
	}

	/**
	 * Creates a JkPublishRepos tailored for <a href="http://central.sonatype.org/">OSSRH</a>
	 */
	public static JkPublishRepos ossrh(String userName, String password) {
		return JkPublishRepos.ofSnapshotAndRelease(
				JkRepo.mavenOssrhPushSnapshotPullAll(userName, password), true,
				JkRepo.mavenOssrhPushRelease(userName, password), true);
	}

	public static JkPublishRepos maven(JkPublishFilter filter, String url, boolean requirePgpSign) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		list.add(JkRepo.maven(url));
		return new JkPublishRepos(toPublishRepo(list, filter, requirePgpSign));
	}

	public static JkPublishRepos maven(String url) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		list.add(JkRepo.maven(url));
		return new JkPublishRepos(toPublishRepo(list, ACCEPT_ALL, false));
	}

	public static JkPublishRepos maven(File file) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		list.add(JkRepo.maven(file));
		return new JkPublishRepos(toPublishRepo(list, ACCEPT_ALL, false));
	}


	public static JkPublishRepos ivy(File file) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		list.add(JkRepo.ivy(file));
		return new JkPublishRepos(toPublishRepo(list, ACCEPT_ALL, false));
	}

	public static JkPublishRepos ivy(String url) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		list.add(JkRepo.ivy(url));
		return new JkPublishRepos(toPublishRepo(list, ACCEPT_ALL, false));
	}


	public static JkPublishRepos ivy(JkPublishFilter filter, String url, boolean requirePgpSign) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		list.add(JkRepo.ivy(url));
		return new JkPublishRepos(toPublishRepo(list, filter, requirePgpSign));
	}

	private final List<JkPublishRepo> repos;

	private JkPublishRepos(List<JkPublishRepo> repos) {
		super();
		this.repos = repos;
	}

	public JkPublishRepos and(JkPublishFilter filter, JkRepo repo, boolean requirePgpSign) {
		final List<JkPublishRepo> list = new LinkedList<JkPublishRepo>(this.repos);
		list.add(new JkPublishRepo(repo, filter, requirePgpSign));
		return new JkPublishRepos(list);
	}


	public JkPublishRepos and(JkPublishRepos other) {
		@SuppressWarnings("unchecked")
		final List<JkPublishRepo> list = JkUtilsIterable.concatLists(this.repos, other.repos);
		return new JkPublishRepos(list);
	}

	public JkPublishRepo getRepoHavingUrl(String url) {
		for (final JkPublishRepo repo : this.repos) {
			if (url.equals(repo.jkRepo.url().toExternalForm())) {
				return repo;
			}
		}
		return null;
	}

	private static List<JkPublishRepo> toPublishRepo(Iterable<JkRepo> repos, JkPublishFilter filter, boolean requirePgpSign) {
		final List<JkPublishRepo> result = new LinkedList<JkPublishRepo>();
		for (final JkRepo repo : repos) {
			result.add(new JkPublishRepo(repo, filter, requirePgpSign));
		}
		return result;
	}


	public static final JkPublishFilter ACCEPT_ALL= new JkPublishFilter() {

		@Override
		public boolean accept(JkVersionedModule versionedModule) {
			return true;
		}

	};

	public static final JkPublishFilter ACCEPT_SNAPSHOT_ONLY= new JkPublishFilter() {

		@Override
		public boolean accept(JkVersionedModule versionedModule) {
			return versionedModule.version().isSnapshot();
		}

	};

	public static final JkPublishFilter ACCEPT_RELEASE_ONLY= new JkPublishFilter() {

		@Override
		public boolean accept(JkVersionedModule versionedModule) {
			return !versionedModule.version().isSnapshot();
		}

	};

	@Override
	public Iterator<JkPublishRepo> iterator() {
		return this.repos.iterator();
	}

	/**
	 * Returns <code>true</code> if it is required to sign the artifacts with PGP in order to
	 * publish the specified version and module.
	 */
	public boolean requirePgpSignature(JkVersionedModule versionedModule) {
		for (final JkPublishRepo jkPublishRepo : this.repos) {
			if (jkPublishRepo.filter.accept(versionedModule) && jkPublishRepo.requirePgpSign) {
				return true;
			}
		}
		return false;
	}

	public static final class JkPublishRepo {

		private final JkRepo jkRepo;

		private final JkPublishFilter filter;

		private final boolean requirePgpSign;

		public JkPublishRepo(JkRepo jkRepo, JkPublishFilter filter, boolean requirePgpSign) {
			super();
			this.jkRepo = jkRepo;
			this.filter = filter;
			this.requirePgpSign = requirePgpSign;
		}

		public JkRepo repo() {
			return jkRepo;
		}

		public JkPublishFilter filter() {
			return filter;
		}

		public boolean requirePgpSign() {
			return requirePgpSign;
		}



	}



}
