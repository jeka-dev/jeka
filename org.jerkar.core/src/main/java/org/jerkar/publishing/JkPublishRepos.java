package org.jerkar.publishing;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.publishing.JkPublishRepos.JkPublishRepo;
import org.jerkar.utils.JkUtilsIterable;

public final class JkPublishRepos implements Iterable<JkPublishRepo>{


	public interface JkPublishFilter {

		boolean accept(JkVersionedModule versionedModule);

	}

	public static JkPublishRepos ofSnapshotAndRelease(JkRepo snapshot, JkRepo optionalRelease) {
		return JkPublishRepos.of(ACCEPT_SNAPSHOT_ONLY, snapshot).and(ACCEPT_RELEASE_ONLY, optionalRelease);
	}



	public static JkPublishRepos of(JkPublishFilter filter, JkRepo ... repo) {
		final List<JkRepo> list = Arrays.asList(repo);
		return new JkPublishRepos(toPublishRepo(list, filter));
	}

	public static JkPublishRepos ossrh(String userName, String password) {
		return JkPublishRepos.ofSnapshotAndRelease(
				JkRepo.mavenOssrhPushSnapshotPullAll(userName, password),
				JkRepo.mavenOssrhPushRelease(userName, password));
	}

	public static JkPublishRepos maven(JkPublishFilter filter, String ... urls) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final String url : urls) {
			list.add(JkRepo.maven(url));
		}
		return new JkPublishRepos(toPublishRepo(list, filter));
	}

	public static JkPublishRepos maven(JkPublishFilter filter, File ... files) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final File file : files) {
			list.add(JkRepo.maven(file));
		}
		return new JkPublishRepos(toPublishRepo(list, filter));
	}


	public static JkPublishRepos ivy(JkPublishFilter filter, File ... files) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final File file : files) {
			list.add(JkRepo.ivy(file));
		}
		return new JkPublishRepos(toPublishRepo(list, filter));
	}

	public static JkPublishRepos ivy(JkPublishFilter filter, String ... urls) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final String url : urls) {
			list.add(JkRepo.ivy(url));
		}
		return new JkPublishRepos(toPublishRepo(list, filter));
	}

	public static JkPublishRepos of(JkRepo ... repos) {
		return of(ACCEPT_ALL, repos);
	}

	public static JkPublishRepos maven(String ... urls) {
		return maven(ACCEPT_ALL, urls);
	}

	public static JkPublishRepos maven(File ... files) {
		return maven(ACCEPT_ALL, files);
	}

	public static JkPublishRepos ivy(File ... files) {
		return ivy(ACCEPT_ALL, files);
	}

	public static JkPublishRepos ivy(String ... urls) {
		return ivy(ACCEPT_ALL, urls);
	}

	private final List<JkPublishRepo> repos;

	private JkPublishRepos(List<JkPublishRepo> repos) {
		super();
		this.repos = repos;
	}

	public JkPublishRepos and(JkPublishFilter filter, Iterable<JkRepo> repos) {
		final List<JkPublishRepo> list = new LinkedList<JkPublishRepos.JkPublishRepo>(this.repos);
		list.addAll(toPublishRepo(repos, filter));
		return new JkPublishRepos(list);
	}

	public JkPublishRepos and(JkPublishFilter filter, JkRepo ... repos) {
		return and(JkPublishRepos.of(filter, repos));
	}

	public JkPublishRepos andMaven(JkPublishFilter filter, String ... urls) {
		return and(JkPublishRepos.maven(filter, urls));
	}

	public JkPublishRepos andMaven(JkPublishFilter filter, File ... files) {
		return and(JkPublishRepos.maven(filter, files));
	}

	public JkPublishRepos andIvy(JkPublishFilter filter, String ... urls) {
		return and(JkPublishRepos.ivy(filter, urls));
	}

	public JkPublishRepos andIvy(JkPublishFilter filter, File ... files) {
		return and(JkPublishRepos.ivy(filter, files));
	}

	public JkPublishRepos and(JkPublishRepos other) {
		@SuppressWarnings("unchecked")
		final List<JkPublishRepo> list = JkUtilsIterable.concatLists(this.repos, other.repos);
		return new JkPublishRepos(list);
	}

	public JkPublishRepo getRepoHavingUrl(String url) {
		for (final JkPublishRepo repo : this) {
			if (url.equals(repo.repo().url().toExternalForm())) {
				return repo;
			}
		}
		return null;
	}

	public static final class JkPublishRepo {

		private final JkRepo jkRepo;

		private final JkPublishFilter filter;

		private JkPublishRepo(JkRepo jkRepo, JkPublishFilter filter) {
			super();
			this.jkRepo = jkRepo;
			this.filter = filter;
		}

		public JkRepo repo() {
			return jkRepo;
		}

		public JkPublishFilter filter() {
			return filter;
		}

	}

	private static List<JkPublishRepo> toPublishRepo(Iterable<JkRepo> repos, JkPublishFilter filter) {
		final List<JkPublishRepo> result = new LinkedList<JkPublishRepos.JkPublishRepo>();
		for (final JkRepo repo : repos) {

			result.add(new JkPublishRepo(repo, filter));
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



}
