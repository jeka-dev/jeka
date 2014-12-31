package org.jake.publishing;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jake.depmanagement.JakeScope;
import org.jake.publishing.JakeIvyPublication.Artifact;

public class JakeIvyPublication implements Iterable<Artifact> {

	public static JakeIvyPublication of(JakeScope jakeScope, File file, String type) {
		return new JakeIvyPublication(new HashSet<JakeIvyPublication.Artifact>()).and(jakeScope, file, type);
	}

	public static JakeIvyPublication of(JakeScope jakeScope, File file) {
		return new JakeIvyPublication(new HashSet<JakeIvyPublication.Artifact>()).and(jakeScope, file);
	}

	private final Set<Artifact> artifacts;

	private JakeIvyPublication(Set<Artifact> artifacts) {
		super();
		this.artifacts = artifacts;
	}

	public JakeIvyPublication and(JakeScope jakeScope, File file, String type) {
		final Set<Artifact> artifacts = new HashSet<JakeIvyPublication.Artifact>(this.artifacts);
		artifacts.add(new Artifact(file, type, jakeScope));
		return new JakeIvyPublication(artifacts);
	}

	public JakeIvyPublication and(JakeScope jakeScope, File file) {
		return and(jakeScope, file, null);
	}

	@Override
	public Iterator<Artifact> iterator() {
		return this.artifacts.iterator();
	}

	public static class Artifact {

		public Artifact(File file, String type, JakeScope jakeScope) {
			super();
			this.file = file;
			this.type = type;
			this.jakeScope = jakeScope;
		}

		public final File file;

		public final String type;

		public final JakeScope jakeScope;

	}

}
