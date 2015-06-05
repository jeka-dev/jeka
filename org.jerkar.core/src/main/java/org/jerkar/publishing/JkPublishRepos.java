package org.jerkar.publishing;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkVersionedModule;
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
public final class JkPublishRepos implements Iterable<Map.Entry<JkPublishFilter, JkRepo>> {

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
		final List<FilteredRepo> list = JkUtilsIterable.listOf(new FilteredRepo(repo, filter, requirePgpSign));
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

	private final List<FilteredRepo> repos;

	private JkPublishRepos(List<FilteredRepo> repos) {
		super();
		this.repos = repos;
	}

	public JkPublishRepos and(JkPublishFilter filter, JkRepo repo, boolean requirePgpSign) {
		final List<FilteredRepo> list = new LinkedList<FilteredRepo>(this.repos);
		list.add(new FilteredRepo(repo, filter, requirePgpSign));
		return new JkPublishRepos(list);
	}


	public JkPublishRepos and(JkPublishRepos other) {
		@SuppressWarnings("unchecked")
		final List<FilteredRepo> list = JkUtilsIterable.concatLists(this.repos, other.repos);
		return new JkPublishRepos(list);
	}

	public Map.Entry<JkPublishFilter, JkRepo> getRepoHavingUrl(String url) {
		for (final FilteredRepo repo : this.repos) {
			if (url.equals(repo.jkRepo.url().toExternalForm())) {
				return repo.entry();
			}
		}
		return null;
	}

	private static List<FilteredRepo> toPublishRepo(Iterable<JkRepo> repos, JkPublishFilter filter, boolean requirePgpSign) {
		final List<FilteredRepo> result = new LinkedList<FilteredRepo>();
		for (final JkRepo repo : repos) {
			result.add(new FilteredRepo(repo, filter, requirePgpSign));
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
	public Iterator<Map.Entry<JkPublishFilter, JkRepo>> iterator() {
		final List<Map.Entry<JkPublishFilter, JkRepo>> list = new LinkedList<Map.Entry<JkPublishFilter,JkRepo>>();
		for (final FilteredRepo filteredRepo : this.repos) {
			list.add(filteredRepo.entry());
		}
		return list.iterator();
	}

	/**
	 * Returns <code>true</code> if it is required to sign the artifacts with PGP in order to
	 * publish the specified version and module.
	 */
	public boolean requirePgpSignature(JkVersionedModule versionedModule) {
		for (final FilteredRepo filteredRepo : this.repos) {
			if (filteredRepo.filter.accept(versionedModule) && filteredRepo.requirePgpSign) {
				return true;
			}
		}
		return false;
	}

	private static final class FilteredRepo {

		private final JkRepo jkRepo;

		private final JkPublishFilter filter;

		private final boolean requirePgpSign;

		public FilteredRepo(JkRepo jkRepo, JkPublishFilter filter, boolean requirePgpSign) {
			super();
			this.jkRepo = jkRepo;
			this.filter = filter;
			this.requirePgpSign = requirePgpSign;
		}

		public Map.Entry<JkPublishFilter, JkRepo> entry() {
			return new Map.Entry<JkPublishFilter, JkRepo>() {

				@Override
				public JkRepo setValue(JkRepo value) {
					return null;
				}

				@Override
				public JkRepo getValue() {
					return jkRepo;
				}

				@Override
				public JkPublishFilter getKey() {
					return filter;
				}
			};
		}

	}



}
