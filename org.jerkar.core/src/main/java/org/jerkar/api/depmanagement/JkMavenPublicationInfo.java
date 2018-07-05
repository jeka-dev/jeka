package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * These are extra information required to publish on Maven central.
 * 
 * @author Jerome Angibaud
 */
public final  class JkMavenPublicationInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a publication infoString specifying the name of the project, its description and its
     * site url.
     */
    @SuppressWarnings("unchecked")
    public static JkMavenPublicationInfo of(String projectName, String description, String url) {
        final JkProjectInfo project = new JkProjectInfo(projectName, description, url);
        return new JkMavenPublicationInfo(project, null, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST);
    }

    /** Info relative to project */
    public final JkProjectInfo project;

    /** Info relative to source control management */
    public final JkScmInfo scm;

    /** Developer list involved in the project */
    public final List<JkDeveloperInfo> devs;

    /** License list for the project, generaly there is a single license */
    public final List<JkLicenseInfo> licenses;

    private JkMavenPublicationInfo(JkProjectInfo project, JkScmInfo scm,
            List<JkDeveloperInfo> devs, List<JkLicenseInfo> licenses) {
        super();
        this.project = project;
        this.scm = scm;
        this.devs = devs;
        this.licenses = licenses;
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but adding the specified
     * developer.
     */
    public JkMavenPublicationInfo andDeveloper(JkDeveloperInfo developerInfo) {
        final List<JkDeveloperInfo> list = new LinkedList<>(
                this.devs);
        list.add(developerInfo);
        return new JkMavenPublicationInfo(this.project, this.scm,
                Collections.unmodifiableList(list), this.licenses);
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but adding the specified
     * developer.
     */
    public JkMavenPublicationInfo andDeveloper(String name, String email, String organisation,
            String organisationUrl) {
        return andDeveloper(new JkDeveloperInfo(name, email, organisation, organisationUrl));
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but adding the specified
     * Github developer.
     */
    public JkMavenPublicationInfo andGitHubDeveloper(String name, String email) {
        return andDeveloper(name, email, "GitHub", "https://github.com");
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but with the specified SCM infoString.
     */
    public JkMavenPublicationInfo withScm(String connection, String developerConnection, String url) {
        final JkScmInfo scmInfo = new JkScmInfo(connection, developerConnection, url);
        return new JkMavenPublicationInfo(this.project, scmInfo, this.devs, this.licenses);
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but with the specified SCM infoString.
     */
    public JkMavenPublicationInfo withScm(String connectionUrl) {
        return withScm(connectionUrl, connectionUrl, connectionUrl);
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but adding the specifyied license.
     */
    public JkMavenPublicationInfo andLicense(String name, String url) {
        final List<JkLicenseInfo> list = new LinkedList<>(this.licenses);
        list.add(new JkLicenseInfo(name, url));
        return new JkMavenPublicationInfo(this.project, this.scm, this.devs,
                Collections.unmodifiableList(list));
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but adding Apache2 license
     */
    public JkMavenPublicationInfo andApache2License() {
        return andLicense("Apache License V2.0", "http://www.apache.org/licenses/LICENSE-2.0.html");
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but adding GPL3 license.
     */
    public JkMavenPublicationInfo andGpl3License() {
        return andLicense("GNU General public license V3", "https://www.gnu.org/copyleft/gpl.html");
    }

    /**
     * Returns a {@link JkMavenPublicationInfo} identical to this one but adding MIT license.
     */
    public JkMavenPublicationInfo andMitLicense() {
        return andLicense("MIT License", "http://opensource.org/licenses/MIT");
    }

    /**
     * Information about source control system managing the project sources.
     */
    public static final class JkScmInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Read-only connection string to project sources */
        public final String connection;

        /** Read-write connection string to project sources */
        public final String developerConnection;

        /** Publicly browsable repository url */
        public final String url;

        /**
         * Constructs a {@link JkScmInfo} specifying its connection, developerConnection and url.
         */
        public JkScmInfo(String connection, String developerConnection, String url) {
            super();
            this.connection = connection;
            this.developerConnection = developerConnection;
            this.url = url;
        }

    }

    /**
     * Information about a developer.
     */
    public static final class JkDeveloperInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Name of developer */
        public final String name;

        /** Email we can contact the developer */
        public final String email;

        /** Organization name the developer belong to */
        public final String organisation;

        /** Organization url the developer belong to */
        public final String organisationUrl;

        /**
         * Constructs a {@link JkDeveloperInfo} specifying its name, email, organization value
         * and organization url.
         */
        public JkDeveloperInfo(String name, String email, String organisation,
                String organisationUrl) {
            super();
            this.name = name;
            this.email = email;
            this.organisation = organisation;
            this.organisationUrl = organisationUrl;
        }

    }

    /**
     * Information about a license.
     */
    public static final class JkLicenseInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Name of the license (as Apache 2) */
        public final String name;

        /** An url pointing on the license terms */
        public final String url;

        /**
         * Constructs a {@link JkLicenseInfo} specifying its name and URL.
         */
        public JkLicenseInfo(String name, String url) {
            super();
            this.name = name;
            this.url = url;
        }
    }

    /**
     * Information about a project.
     */
    public static final class JkProjectInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Public name of the project.
         */
        public final String name;

        /**
         * Description for the project.
         */
        public final String description;

        /**
         * Url site of the project.
         */
        public final String url;

        /**
         * Constructs a {@link JkProjectInfo} specifying its name, description and url.
         */
        public JkProjectInfo(String name, String description, String url) {
            super();
            this.name = name;
            this.description = description;
            this.url = url;
        }

    }

}
