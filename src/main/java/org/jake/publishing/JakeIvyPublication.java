package org.jake.publishing;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jake.depmanagement.JakeScope;
import org.jake.publishing.JakeIvyPublication.Artifact;

public class JakeIvyPublication implements Iterable<Artifact> {

	public static JakeIvyPublication of(JakeScope jakeScope, File file, String type) {
		return new JakeIvyPublication(new HashSet<JakeIvyPublication.Artifact>(), null, null).and(jakeScope, file, type);
	}

	public static JakeIvyPublication of(JakeScope jakeScope, File file) {
		return new JakeIvyPublication(new HashSet<JakeIvyPublication.Artifact>(), null, null).and(jakeScope, file);
	}

	private final Set<Artifact> artifacts;

	public final Status status;

	public final String branch;

	private JakeIvyPublication(Set<Artifact> artifacts, Status status, String branch) {
		super();
		this.artifacts = artifacts;
		this.status = status;
		this.branch = branch;
	}

	public JakeIvyPublication and(JakeScope jakeScope, File file, String type) {
		final Set<Artifact> artifacts = new HashSet<JakeIvyPublication.Artifact>(this.artifacts);
		artifacts.add(new Artifact(file, type, jakeScope));
		return new JakeIvyPublication(artifacts, this.status, this.branch);
	}

	public JakeIvyPublication and(JakeScope jakeScope, File file) {
		return and(jakeScope, file, null);
	}

	@Override
	public Iterator<Artifact> iterator() {
		return this.artifacts.iterator();
	}

	public JakeIvyPublication status(Status status) {
		return new JakeIvyPublication(this.artifacts, status, this.branch);
	}

	public JakeIvyPublication branch(String branch) {
		return new JakeIvyPublication(this.artifacts, this.status, branch);
	}

	public static class Artifact {

		private Artifact(File file, String type, JakeScope jakeScope) {
			super();
			this.file = file;
			this.type = type;
			this.jakeScope = jakeScope;
		}

		public final File file;

		public final String type;

		public final JakeScope jakeScope;

	}

	public static class Status {

		public static final Status INTEGRATION = new Status("integration", true);

		public static final Status MILESTONE = new Status("milestone", false);

		public static final Status RELEASE = new Status("release", false);

		public static final Status of(String name) {
			return new Status(name, false);
		}

		public static final Status ofIntegration(String name) {
			return new Status(name, true);
		}

		private final String name;

		private final boolean integration;

		private Status(String name, boolean integration) {
			super();
			this.name = name;
			this.integration = integration;
		}

		public String name() {
			return name;
		}

		public boolean integration() {
			return integration;
		}

	}

}
