package org.jake.publishing;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeVersionedModule;
import org.jake.publishing.JakePublishRepos.JakePublishRepo;
import org.jake.utils.JakeUtilsIterable;

public final class JakePublishRepos implements Iterable<JakePublishRepo>{

	interface JakePublishFilter {

		boolean accept(JakeVersionedModule versionedModule);

	}

	public static JakePublishRepos ofSnapshotAndRelease(JakeRepo snapshot, JakeRepo optionalRelease) {
		return JakePublishRepos.of(ACCEPT_SNAPSHOT_ONLY, snapshot).and(ACCEPT_RELEASE_ONLY, optionalRelease);
	}

	public static JakePublishRepos of(JakePublishFilter filter, JakeRepo ... repo) {
		final List<JakeRepo> list = Arrays.asList(repo);
		return new JakePublishRepos(toPublishRepo(list, filter));
	}

	public static JakePublishRepos maven(JakePublishFilter filter, String ... urls) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>();
		for (final String url : urls) {
			list.add(JakeRepo.maven(url));
		}
		return new JakePublishRepos(toPublishRepo(list, filter));
	}

	public static JakePublishRepos maven(JakePublishFilter filter, File ... files) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>();
		for (final File file : files) {
			list.add(JakeRepo.maven(file));
		}
		return new JakePublishRepos(toPublishRepo(list, filter));
	}


	public static JakePublishRepos ivy(JakePublishFilter filter, File ... files) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>();
		for (final File file : files) {
			list.add(JakeRepo.ivy(file));
		}
		return new JakePublishRepos(toPublishRepo(list, filter));
	}

	public static JakePublishRepos ivy(JakePublishFilter filter, String ... urls) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>();
		for (final String url : urls) {
			list.add(JakeRepo.ivy(url));
		}
		return new JakePublishRepos(toPublishRepo(list, filter));
	}

	public static JakePublishRepos of(JakeRepo ... repos) {
		return of(ACCEPT_ALL, repos);
	}

	public static JakePublishRepos maven(String ... urls) {
		return maven(ACCEPT_ALL, urls);
	}

	public static JakePublishRepos maven(File ... files) {
		return maven(ACCEPT_ALL, files);
	}

	public static JakePublishRepos ivy(File ... files) {
		return ivy(ACCEPT_ALL, files);
	}

	public static JakePublishRepos ivy(String ... urls) {
		return ivy(ACCEPT_ALL, urls);
	}

	private final List<JakePublishRepo> repos;

	private JakePublishRepos(List<JakePublishRepo> repos) {
		super();
		this.repos = repos;
	}

	public JakePublishRepos and(JakePublishFilter filter, Iterable<JakeRepo> repos) {
		final List<JakePublishRepo> list = new LinkedList<JakePublishRepos.JakePublishRepo>(this.repos);
		list.addAll(toPublishRepo(repos, filter));
		return new JakePublishRepos(list);
	}

	public JakePublishRepos and(JakePublishFilter filter, JakeRepo ... repos) {
		return and(JakePublishRepos.of(filter, repos));
	}

	public JakePublishRepos andMaven(JakePublishFilter filter, String ... urls) {
		return and(JakePublishRepos.maven(filter, urls));
	}

	public JakePublishRepos andMaven(JakePublishFilter filter, File ... files) {
		return and(JakePublishRepos.maven(filter, files));
	}

	public JakePublishRepos andIvy(JakePublishFilter filter, String ... urls) {
		return and(JakePublishRepos.ivy(filter, urls));
	}

	public JakePublishRepos andIvy(JakePublishFilter filter, File ... files) {
		return and(JakePublishRepos.ivy(filter, files));
	}

	public JakePublishRepos and(JakePublishRepos other) {
		@SuppressWarnings("unchecked")
		final List<JakePublishRepo> list = JakeUtilsIterable.concatLists(this.repos, other.repos);
		return new JakePublishRepos(list);
	}

	public static final class JakePublishRepo {

		private final JakeRepo jakeRepo;

		private final JakePublishFilter filter;

		private JakePublishRepo(JakeRepo jakeRepo, JakePublishFilter filter) {
			super();
			this.jakeRepo = jakeRepo;
			this.filter = filter;
		}

		public JakeRepo repo() {
			return jakeRepo;
		}

		public JakePublishFilter filter() {
			return filter;
		}

	}

	private static List<JakePublishRepo> toPublishRepo(Iterable<JakeRepo> repos, JakePublishFilter filter) {
		final List<JakePublishRepo> result = new LinkedList<JakePublishRepos.JakePublishRepo>();
		for (final JakeRepo repo : repos) {

			result.add(new JakePublishRepo(repo, filter));
		}
		return result;
	}


	public static final JakePublishFilter ACCEPT_ALL= new JakePublishFilter() {

		@Override
		public boolean accept(JakeVersionedModule versionedModule) {
			return true;
		}

	};

	public static final JakePublishFilter ACCEPT_SNAPSHOT_ONLY= new JakePublishFilter() {

		@Override
		public boolean accept(JakeVersionedModule versionedModule) {
			return versionedModule.version().isSnapshot();
		}

	};

	public static final JakePublishFilter ACCEPT_RELEASE_ONLY= new JakePublishFilter() {

		@Override
		public boolean accept(JakeVersionedModule versionedModule) {
			return !versionedModule.version().isSnapshot();
		}

	};

	@Override
	public Iterator<JakePublishRepo> iterator() {
		return this.repos.iterator();
	}



}
