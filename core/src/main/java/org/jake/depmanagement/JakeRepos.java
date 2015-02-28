package org.jake.depmanagement;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jake.utils.JakeUtilsIterable;

public final class JakeRepos implements Iterable<JakeRepo> {

	public static JakeRepos of(JakeRepo ...jakeRepositories) {
		return new JakeRepos(Arrays.asList(jakeRepositories));
	}

	public static JakeRepos maven(String ... urls) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>();
		for (final String url : urls) {
			list.add(JakeRepo.maven(url));
		}
		return new JakeRepos(list);
	}

	public static JakeRepos maven(File ...files) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>();
		for (final File file : files) {
			list.add(JakeRepo.maven(file));
		}
		return new JakeRepos(list);
	}

	public static JakeRepos ivy(String ... urls) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>();
		for (final String url : urls) {
			list.add(JakeRepo.ivy(url));
		}
		return new JakeRepos(list);
	}

	public static JakeRepos ivy(File ... files) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>();
		for (final File file : files) {
			list.add(JakeRepo.ivy(file));
		}
		return new JakeRepos(list);
	}


	public static JakeRepos mavenCentral() {
		return new JakeRepos(JakeUtilsIterable.listOf(JakeRepo.mavenCentral()));
	}

	public static JakeRepos mavenJCenter() {
		return new JakeRepos(JakeUtilsIterable.listOf(JakeRepo.mavenJCenter()));
	}

	private final List<JakeRepo> repos;

	private JakeRepos(List<JakeRepo> repos) {
		super();
		this.repos = Collections.unmodifiableList(repos);
	}

	public JakeRepos and(Iterable<JakeRepo> jakeRepos) {
		return and(JakeUtilsIterable.toArray(jakeRepos, JakeRepo.class));
	}

	public JakeRepos and(JakeRepo ...jakeRepoArray) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>(this.repos);
		list.addAll(Arrays.asList(jakeRepoArray));
		return new JakeRepos(list);
	}

	public JakeRepos andMaven(String ...urls) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>(this.repos);
		list.addAll(maven(urls).repos);
		return new JakeRepos(list);
	}

	public JakeRepos andIvy(File ...files) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>(this.repos);
		list.addAll(JakeRepos.ivy(files).repos);
		return new JakeRepos(list);
	}

	public JakeRepos andMaven(File ...files) {
		final List<JakeRepo> list = new LinkedList<JakeRepo>(this.repos);
		list.addAll(maven(files).repos);
		return new JakeRepos(list);
	}

	public JakeRepos andMavenCentral() {
		final List<JakeRepo> list = new LinkedList<JakeRepo>(this.repos);
		list.add(JakeRepo.mavenCentral());
		return new JakeRepos(list);
	}

	public JakeRepos andMavenJCenter() {
		final List<JakeRepo> list = new LinkedList<JakeRepo>(this.repos);
		list.add(JakeRepo.mavenJCenter());
		return new JakeRepos(list);
	}

	public boolean isEmpty() {
		return this.repos.isEmpty();
	}

	@Override
	public Iterator<JakeRepo> iterator() {
		return repos.iterator();
	}

	@Override
	public String toString() {
		return repos.toString();
	}

}
