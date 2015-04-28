package org.jerkar.publishing;

import java.util.List;

/**
 * These are extra information required to publish on Maven central.
 * 
 * @author Jerome Angibaud
 *
 */
public class JkMavenPublicationInfo {

	public final String projectName;

	public final String description;

	public final String url;

	public final JkScmInfo scm;

	public final List<JkDeveloperInfo> devs;

	public final String licenseName;

	public final String licenseUrl;

	public


	public class JkScmInfo {

		public final String connection;

		public final String developerConnection;

		public final String url;

		public JkScmInfo(String connection, String developerConnection,
				String url) {
			super();
			this.connection = connection;
			this.developerConnection = developerConnection;
			this.url = url;
		}

	}

	public class JkDeveloperInfo {
		public final String name;
		public final String email;
		public final String organisation;
		public final String organisationUrl;
		public JkDeveloperInfo(String name, String email, String organisation,
				String organisationUrl) {
			super();
			this.name = name;
			this.email = email;
			this.organisation = organisation;
			this.organisationUrl = organisationUrl;
		}

	}





}
