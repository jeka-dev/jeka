package dev.jeka.core.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * These are extra information required to publish on Maven central.
 *
 * @author Jerome Angibaud
 */
public final class JkPomMetadata<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * For parent chaining
     */
    public final T __;

    /** Info relative to project */
    private final JkProjectInfo<JkPomMetadata<T>> projectInfo;

    /** Info relative to source control management */
    private final JkScmInfo<JkPomMetadata<T>> scm;

    /** Developer list involved in the project */
    private final List<JkDeveloperInfo> developers;

    /** License list for the project, generaly there is a single license */
    private final List<JkLicenseInfo> licenses;

    private JkPomMetadata(T parent) {
        this.__ = parent;
        this.projectInfo = new JkProjectInfo(this);
        this.scm = new JkScmInfo(this);
        this.developers = new LinkedList<>();
        this.licenses = new LinkedList<>();
    }

    /**
     * Creates an empty publication info.
     */
    public static JkPomMetadata<Void> of() {
        return new JkPomMetadata(null);
    }

    /**
     * Creates an empty publication info.
     */
    public static <T> JkPomMetadata<T> ofParent(T parent) {
        return new JkPomMetadata(parent);
    }

    /**
     * Adds a developer pthis this publication.
     */
    public JkPomMetadata<T> addDeveloper(JkDeveloperInfo developerInfo) {
        this.developers.add(developerInfo);
        return this;
    }

    /**
     * @see #addDeveloper(JkDeveloperInfo)
     */
    public JkPomMetadata<T> addDeveloper(String name, String email, String organisation,
                                         String organisationUrl) {
        return addDeveloper(new JkDeveloperInfo(name, email, organisation, organisationUrl));
    }

    /**
     * @see #addDeveloper(JkDeveloperInfo)
     */
    public JkPomMetadata<T> addGithubDeveloper(String name, String email) {
        return addDeveloper(new JkDeveloperInfo(name, email, "Github", "https://github.com"));
    }

    /**
     * Returns the {@link JkScmInfo} for this publication
     */
    public JkScmInfo<JkPomMetadata<T>> getScm() {
        return scm;

    }

    /**
     * Adds license information about this publication
     */
    public JkPomMetadata<T> addLicense(JkLicenseInfo licenseInfo) {
        this.licenses.add(licenseInfo);
        return this;
    }

    /**
     * @see #addLicense(JkLicenseInfo)
     */
    public JkPomMetadata<T> addLicense(String name, String url) {
        this.licenses.add(JkLicenseInfo.of(name, url));
        return this;
    }

    /**
     * R@see #addLicense(JkLicenseInfo)
     */
    public JkPomMetadata<T> addApache2License() {
        return addLicense("Apache License V2.0", "http://www.apache.org/licenses/LICENSE-2.0.html");
    }

    /**
     * @see #addLicense(JkLicenseInfo)
     */
    public JkPomMetadata<T> addGpl3License() {
        return addLicense("GNU General public license V3", "https://www.gnu.org/copyleft/gpl.html");
    }

    /**
     *  @see #addLicense(JkLicenseInfo)
     */
    public JkPomMetadata<T> addMitLicense() {
        return addLicense("MIT License", "http://opensource.org/licenses/MIT");
    }

    public JkProjectInfo<JkPomMetadata<T>> getProjectInfo() {
        return projectInfo;
    }

    public List<JkDeveloperInfo> getDevelopers() {
        return Collections.unmodifiableList(developers);
    }

    public List<JkLicenseInfo> getLicenses() {
        return Collections.unmodifiableList(licenses);
    }

    /**
     * Information about source control ofSystem managing the project sources.
     */
    public static final class JkScmInfo<T> implements Serializable {

        private static final long serialVersionUID = 1L;

        public final T __;

        private String connection;

        private String developerConnection;

        private String url;

        /**
         * Constructs a {@link JkScmInfo} specifying its connection, developerConnection and url.
         */
        JkScmInfo(T parent) {
            this.__ = parent;
        }

        /** Read-only connection string to project sources */
        public JkScmInfo<T> setConnection(String connection) {
            this.connection = connection;
            return this;
        }

        public String getConnection() {
            return connection;
        }

        public String getDeveloperConnection() {
            return developerConnection;
        }

        /** Read-write connection string to project sources */
        public JkScmInfo<T> setDeveloperConnection(String developerConnection) {
            this.developerConnection = developerConnection;
            return this;
        }

        /** Read-write connection string to project sources */
        public String getUrl() {
            return url;
        }

        /** Publicly browsable repository url */
        public JkScmInfo<T> setUrl(String url) {
            this.url = url;
            return this;
        }

    }

    /**
     * Information about a developer.
     */
    public static final class JkDeveloperInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Name of developer */
        private final String name;

        /** Email we can contact the developer */
        private final String email;

        /** Organization name the developer belong to */
        private final String organisation;

        /** Organization url the developer belong to */
        private final String organisationUrl;

        /**
         * Constructs a {@link JkDeveloperInfo} specifying its name, email, organization value
         * and organization url.
         */
         JkDeveloperInfo(String name, String email, String organisation,
                String organisationUrl) {
            super();
            this.name = name;
            this.email = email;
            this.organisation = organisation;
            this.organisationUrl = organisationUrl;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getOrganisation() {
            return organisation;
        }

        public String getOrganisationUrl() {
            return organisationUrl;
        }
    }

    /**
     * Information about a license.
     */
    public static final class JkLicenseInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Name of the license (as Apache 2) */
        private final String name;

        /** An url pointing on the license terms */
        private final String url;

        /**
         * Constructs a {@link JkLicenseInfo} specifying its name and URL.
         */
        private JkLicenseInfo(String name, String url) {
            super();
            this.name = name;
            this.url = url;
        }

        public static JkLicenseInfo of(String name, String url) {
            return new JkLicenseInfo(name, url);
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }

    /**
     * Information about a project.
     */
    public static final class JkProjectInfo<T> {

        public final T __;

        /**
         * Public name of the project.
         */
        private String name;

        /**
         * Description for the project.
         */
        private String description;

        /**
         * Url site of the project.
         */
        private String url;

        /**
         * Constructs a {@link JkProjectInfo} specifying its name, description and url.
         */
        private JkProjectInfo(T __) {
            this.__ = __;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getUrl() {
            return url;
        }

        public JkProjectInfo<T> setName(String name) {
            this.name = name;
            return this;
        }

        public JkProjectInfo<T> setDescription(String description) {
            this.description = description;
            return this;
        }

        public JkProjectInfo<T> setUrl(String url) {
            this.url = url;
            return this;
        }
    }

}
