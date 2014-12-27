package org.jake.publishing;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jake.depmanagement.JakeScope;

public class Publish {

	private final Set<PublishedAtrifact> artifacts = new HashSet<Publish.PublishedAtrifact>();



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
