package org.jerkar.depmanagement;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.utils.JkUtilsIterable;

public final class JkRepos implements Iterable<JkRepo> {

	public static JkRepos of(JkRepo ...jakeRepositories) {
		return new JkRepos(Arrays.asList(jakeRepositories));
	}

	public static JkRepos maven(String ... urls) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final String url : urls) {
			list.add(JkRepo.maven(url));
		}
		return new JkRepos(list);
	}

	public static JkRepos maven(File ...files) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final File file : files) {
			list.add(JkRepo.maven(file));
		}
		return new JkRepos(list);
	}

	public static JkRepos ivy(String ... urls) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final String url : urls) {
			list.add(JkRepo.ivy(url));
		}
		return new JkRepos(list);
	}

	public static JkRepos ivy(File ... files) {
		final List<JkRepo> list = new LinkedList<JkRepo>();
		for (final File file : files) {
			list.add(JkRepo.ivy(file));
		}
		return new JkRepos(list);
	}


	public static JkRepos mavenCentral() {
		return new JkRepos(JkUtilsIterable.listOf(JkRepo.mavenCentral()));
	}

	public static JkRepos mavenJCenter() {
		return new JkRepos(JkUtilsIterable.listOf(JkRepo.mavenJCenter()));
	}

	private final List<JkRepo> repos;

	private JkRepos(List<JkRepo> repos) {
		super();
		this.repos = Collections.unmodifiableList(repos);
	}

	public JkRepos and(Iterable<JkRepo> jakeRepos) {
		return and(JkUtilsIterable.toArray(jakeRepos, JkRepo.class));
	}

	public JkRepos and(JkRepo ...jakeRepoArray) {
		final List<JkRepo> list = new LinkedList<JkRepo>(this.repos);
		list.addAll(Arrays.asList(jakeRepoArray));
		return new JkRepos(list);
	}

	public JkRepos andMaven(String ...urls) {
		final List<JkRepo> list = new LinkedList<JkRepo>(this.repos);
		list.addAll(maven(urls).repos);
		return new JkRepos(list);
	}

	public JkRepos andIvy(File ...files) {
		final List<JkRepo> list = new LinkedList<JkRepo>(this.repos);
		list.addAll(JkRepos.ivy(files).repos);
		return new JkRepos(list);
	}

	public JkRepos andMaven(File ...files) {
		final List<JkRepo> list = new LinkedList<JkRepo>(this.repos);
		list.addAll(maven(files).repos);
		return new JkRepos(list);
	}

	public JkRepos andMavenCentral() {
		final List<JkRepo> list = new LinkedList<JkRepo>(this.repos);
		list.add(JkRepo.mavenCentral());
		return new JkRepos(list);
	}

	public JkRepos andMavenJCenter() {
		final List<JkRepo> list = new LinkedList<JkRepo>(this.repos);
		list.add(JkRepo.mavenJCenter());
		return new JkRepos(list);
	}

	public boolean isEmpty() {
		return this.repos.isEmpty();
	}

	@Override
	public Iterator<JkRepo> iterator() {
		return repos.iterator();
	}

	@Override
	public String toString() {
		return repos.toString();
	}

}
