package org.jerkar.publishing;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.publishing.JkPublishRepos.JakePublishRepo;
import org.jerkar.utils.JkUtilsIterable;

public final class JkPublishRepos implements Iterable<JakePublishRepo>{

	public interface JakePublishFilter {

		boolean accept(JkVersionedModule versionedModule);

	}

	public static JkPublishRepos ofSnapshotAndRelease(JkRepo snapshot, JkRepo optionalRelease) {
		return JkPublishRepos.of(ACCEPT_SNAPSHOT_ONLY, snapshot).and(ACCEPT_RELEASE_ONLY, optionalRelease);
	}

	public static JkPublishRepos of(JakePublishFilter filter, JkRepo ... repo) {
		final List<JkRepo> list = Arrays.asList(repo);
		return new JkPublishRepos(toPublishRepo(list, filter));
	}

	public static JkPublishRepos maven(JakePublishFilter filter, String ... urls) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final String url : urls) {
			list.add(JkRepo.maven(url));
		}
		return new JkPublishRepos(toPublishRepo(list, filter));
	}

	public static JkPublishRepos maven(JakePublishFilter filter, File ... files) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final File file : files) {
			list.add(JkRepo.maven(file));
		}
		return new JkPublishRepos(toPublishRepo(list, filter));
	}


	public static JkPublishRepos ivy(JakePublishFilter filter, File ... files) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final File file : files) {
			list.add(JkRepo.ivy(file));
		}
		return new JkPublishRepos(toPublishRepo(list, filter));
	}

	public static JkPublishRepos ivy(JakePublishFilter filter, String ... urls) {
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

	private final List<JakePublishRepo> repos;

	private JkPublishRepos(List<JakePublishRepo> repos) {
		super();
		this.repos = repos;
	}

	public JkPublishRepos and(JakePublishFilter filter, Iterable<JkRepo> repos) {
		final List<JakePublishRepo> list = new LinkedList<JkPublishRepos.JakePublishRepo>(this.repos);
		list.addAll(toPublishRepo(repos, filter));
		return new JkPublishRepos(list);
	}

	public JkPublishRepos and(JakePublishFilter filter, JkRepo ... repos) {
		return and(JkPublishRepos.of(filter, repos));
	}

	public JkPublishRepos andMaven(JakePublishFilter filter, String ... urls) {
		return and(JkPublishRepos.maven(filter, urls));
	}

	public JkPublishRepos andMaven(JakePublishFilter filter, File ... files) {
		return and(JkPublishRepos.maven(filter, files));
	}

	public JkPublishRepos andIvy(JakePublishFilter filter, String ... urls) {
		return and(JkPublishRepos.ivy(filter, urls));
	}

	public JkPublishRepos andIvy(JakePublishFilter filter, File ... files) {
		return and(JkPublishRepos.ivy(filter, files));
	}

	public JkPublishRepos and(JkPublishRepos other) {
		@SuppressWarnings("unchecked")
		final List<JakePublishRepo> list = JkUtilsIterable.concatLists(this.repos, other.repos);
		return new JkPublishRepos(list);
	}

	public JakePublishRepo getRepoHavingUrl(String url) {
		for (final JakePublishRepo repo : this) {
			if (url.equals(repo.repo().url().toExternalForm())) {
				return repo;
			}
		}
		return null;
	}

	public static final class JakePublishRepo {

		private final JkRepo jakeRepo;

		private final JakePublishFilter filter;

		private JakePublishRepo(JkRepo jakeRepo, JakePublishFilter filter) {
			super();
			this.jakeRepo = jakeRepo;
			this.filter = filter;
		}

		public JkRepo repo() {
			return jakeRepo;
		}

		public JakePublishFilter filter() {
			return filter;
		}

	}

	private static List<JakePublishRepo> toPublishRepo(Iterable<JkRepo> repos, JakePublishFilter filter) {
		final List<JakePublishRepo> result = new LinkedList<JkPublishRepos.JakePublishRepo>();
		for (final JkRepo repo : repos) {

			result.add(new JakePublishRepo(repo, filter));
		}
		return result;
	}


	public static final JakePublishFilter ACCEPT_ALL= new JakePublishFilter() {

		@Override
		public boolean accept(JkVersionedModule versionedModule) {
			return true;
		}

	};

	public static final JakePublishFilter ACCEPT_SNAPSHOT_ONLY= new JakePublishFilter() {

		@Override
		public boolean accept(JkVersionedModule versionedModule) {
			return versionedModule.version().isSnapshot();
		}

	};

	public static final JakePublishFilter ACCEPT_RELEASE_ONLY= new JakePublishFilter() {

		@Override
		public boolean accept(JkVersionedModule versionedModule) {
			return !versionedModule.version().isSnapshot();
		}

	};

	@Override
	public Iterator<JakePublishRepo> iterator() {
		return this.repos.iterator();
	}



}
