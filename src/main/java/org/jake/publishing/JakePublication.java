package org.jake.publishing;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jake.depmanagement.JakeScope;

public class JakePublication {

	public static JakePublication of(JakeScope jakeScope, File file, String type) {
		return new JakePublication(new HashSet<JakePublication.PublishedAtrifact>()).and(jakeScope, file, type);
	}

	private final Set<PublishedAtrifact> artifacts;

	private JakePublication(Set<PublishedAtrifact> artifacts) {
		super();
		this.artifacts = artifacts;
	}

	public JakePublication and(JakeScope jakeScope, File file, String type) {
		final Set<PublishedAtrifact> artifacts = new HashSet<JakePublication.PublishedAtrifact>(this.artifacts);
		artifacts.add(new PublishedAtrifact(file, type, jakeScope));
		return new JakePublication(artifacts);
	}

	private static class PublishedAtrifact {

		public PublishedAtrifact(File file, String type, JakeScope jakeScope) {
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
