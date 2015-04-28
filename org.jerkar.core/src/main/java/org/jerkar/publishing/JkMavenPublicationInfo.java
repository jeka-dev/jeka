package org.jerkar.publishing;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * These are extra information required to publish on Maven central.
 * 
 * @author Jerome Angibaud
 */
public class JkMavenPublicationInfo {

	@SuppressWarnings("unchecked")
	public static JkMavenPublicationInfo of(String projectName, String description, String url) {
		final JkProjectInfo project = new JkProjectInfo(projectName, description, url);
		return new JkMavenPublicationInfo(project, null, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}

	public final JkProjectInfo project;

	public final JkScmInfo scm;

	public final List<JkDeveloperInfo> devs;

	public final List<JkLicenseInfo> licenses;

	private JkMavenPublicationInfo(JkProjectInfo project, JkScmInfo scm,
			List<JkDeveloperInfo> devs, List<JkLicenseInfo> licenses) {
		super();
		this.project = project;
		this.scm = scm;
		this.devs = devs;
		this.licenses = licenses;
	}

	public JkMavenPublicationInfo andDeveloper(JkDeveloperInfo developerInfo) {
		final List<JkDeveloperInfo> list = new LinkedList<JkMavenPublicationInfo.JkDeveloperInfo>(this.devs);
		list.add(developerInfo);
		return new JkMavenPublicationInfo(this.project, this.scm, Collections.unmodifiableList(list), this.licenses);
	}

	public JkMavenPublicationInfo andDeveloper(String name, String email, String organisation, String organisationUrl) {
		return andDeveloper(new JkDeveloperInfo(name, email, organisation, organisationUrl));
	}

	public JkMavenPublicationInfo withGitHubDeveloper(String name, String email) {
		return andDeveloper(name, email, "GitHub", "https://github.com");
	}

	public JkMavenPublicationInfo withScm(String connection, String developerConnection, String url) {
		final JkScmInfo scmInfo = new JkScmInfo(connection, developerConnection, url);
		return new JkMavenPublicationInfo(this.project, scmInfo, this.devs, this.licenses);
	}

	public JkMavenPublicationInfo withScm(String connectionUrl) {
		return withScm(connectionUrl, connectionUrl, connectionUrl);
	}

	public JkMavenPublicationInfo andLicense(String name, String url) {
		final List<JkLicenseInfo> list = new LinkedList<JkLicenseInfo>(this.licenses);
		list.add(new JkLicenseInfo(name, url));
		return new JkMavenPublicationInfo(this.project, this.scm, this.devs, Collections.unmodifiableList(list));
	}

	public JkMavenPublicationInfo andApache2License() {
		return andLicense("Apache License V2.0", "http://www.apache.org/licenses/LICENSE-2.0.html");
	}

	public JkMavenPublicationInfo andGpl3License() {
		return andLicense("GNU General public license V3", "https://www.gnu.org/copyleft/gpl.html");
	}

	public JkMavenPublicationInfo andMitLicense() {
		return andLicense("MIT License", "http://opensource.org/licenses/MIT");
	}


	public static final class JkScmInfo {

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

	public static final class JkDeveloperInfo {
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

	public static final class JkLicenseInfo {
		public final String name;
		public final String url;
		public JkLicenseInfo(String name, String url) {
			super();
			this.name = name;
			this.url = url;
		}
	}

	public static final class JkProjectInfo {

		public final String name;

		public final String description;

		public final String url;

		public JkProjectInfo(String name, String description, String url) {
			super();
			this.name = name;
			this.description = description;
			this.url = url;
		}

	}

}
