package org.jerkar.tool;

import java.io.File;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyExclusions;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkPublisher;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolutionParameters;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.depmanagement.JkVersionProvider;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.tooling.JkCodeWriterForBuildClass;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Template build definition class providing support for managing dependencies
 * and multi-projects.
 *
 * @author Jerome Angibaud
 */
public class JkBuildDependencySupport extends JkBuild {

    /**
     * Default path for the non managed dependencies. This path is relative to
     * {@link #baseDir()}.
     */
    protected static final String STD_LIB_PATH = "build/libs";

    // A cache for dependency resolver
    private JkDependencyResolver cachedResolver;

    // A cache for artifact publisher
    private JkPublisher cachedPublisher;

    /** Dependency and publish repositories */
    @JkDoc("Dependency and publish repositories")
    protected JkOptionRepos repo = new JkOptionRepos();

    /** Version to inject to this build. If 'null' or blank than the version will be the one returned by #version() */
    @JkDoc("Version to inject to this build. If 'null' or blank than the version will be the one returned by #version()")
    protected String version = null;

    /**
     * Constructs a {@link JkBuildDependencySupport}
     */
    protected JkBuildDependencySupport() {
    }

    /**
     * Override this method to define version programmatically. This is not
     * necessary the effective version that will be used in end. Indeed version
     * injected via Jerkar options takes precedence on this one.
     *
     * @see JkBuildDependencySupport#effectiveVersion()
     */
    protected JkVersion version() {
        return null;
    }

    /**
     * Returns the effective version for this project. This value is used to
     * name and publish artifacts. It may take format as
     * <code>1.0-SNAPSHOT</code>, <code>trunk-SNAPSHOT</code>,
     * <code>1.2.3-rc1</code>, <code>1.2.3</code>.
     *
     * To get the effective version, this method looks in the following order :
     * <ul>
     * <li>The version injected by option (with command line argment
     * -version=2.1 for example)</li>
     * <li>The version returned by the {@link #version()} method</li>
     * <li>The the hard-coded <code>trunk-SNAPSHOT</code> value</li>
     * </ul>
     *
     */
    public final JkVersion effectiveVersion() {
        if (!JkUtilsString.isBlank(version)) {
            return JkVersion.ofName(version);
        }
        return JkUtilsObject.firstNonNull(version(), JkVersion.ofName("trunk-SNAPSHOT"));
    }

    /**
     * Returns identifier for this project. This identifier is used to name
     * generated artifacts and by dependency manager.
     */
    public JkModuleId moduleId() {
        return JkModuleId.of(baseDir().root().getName());
    }

    /**
     * Returns moduleId along its version
     */
    protected final JkVersionedModule versionedModule() {
        return JkVersionedModule.of(moduleId(), effectiveVersion());
    }

    /**
     * Returns the download repositories where to retrieve artifacts. It has
     * only a meaning in case of using managed dependencies.
     */
    protected JkRepos downloadRepositories() {
        return reposOfOptions("download").andIfEmpty(JkRepo.mavenCentral());
    }

    /**
     * Returns the repositories where are published artifacts.
     */
    protected JkPublishRepos publishRepositories() {

        // Find best defaults
        if (repo.publish.url == null && repo.release.url == null) {
            JkLog.info("No url specified for publish and release repo : use defaults.");
            if (repo.publish.username != null && repo.publish.password != null) {
                JkLog.info("Credential specifified for publish repo : use OSSRH repos.");
                return JkPublishRepos.ossrh(repo.publish.username, repo.publish.password, pgp());
            } else {
                final JkRepo repo = JkRepo.mavenLocal();
                JkLog.info("No credential specifified for publish repo : use local filesystem repo."
                        + repo.url());
                return JkPublishRepos.of(repo.asPublishRepo());
            }
        }

        // One of release or publish url has been specified
        final JkRepo defaultDownloadRepo = this.repo.download.toRepo();
        final JkRepo defaultPublishRepo = this.repo.publish.toRepo();
        final JkRepo defaultPublishReleaseRepo = this.repo.release.toRepo();
        final JkRepo publishRepo = JkRepo.firstNonNull(defaultPublishRepo, defaultDownloadRepo);
        final JkRepo releaseRepo = JkRepo.firstNonNull(defaultPublishReleaseRepo, publishRepo);

        return JkPublishRepos.of(publishRepo.asPublishSnapshotRepo()).and(
                releaseRepo.asPublishReleaseRepo());
    }

    /**
     * Returns the resolved dependencies for the given scope. Depending on the
     * passed options, it may be augmented with extra-libs mentioned in options
     * <code>extraXxxxPath</code>.
     */
    public final JkPath depsFor(JkScope... scopes) {
        return dependencyResolver().get(scopes);
    }

    /**
     * Returns the dependencies of this module. By default it uses unmanaged
     * dependencies stored locally in the project as described by
     * {@link #implicitDependencies()} method. If you want to use managed
     * dependencies, you must override this method.
     */
    private JkDependencies effectiveDependencies() {
        JkDependencies deps = dependencies().withDefaultScope(this.defaultScope())
                .resolvedWith(versionProvider()).withExclusions(dependencyExclusions());
        final JkScope defaultcope = this.defaultScope();
        if (defaultcope != null) {
            deps = deps.withDefaultScope(defaultcope);
        }
        return JkBuildPlugin.applyDependencies(plugins.getActives(),
                implicitDependencies().and(deps));
    }

    /**
     * On {@link #asDependency(File...)} method, you may have declared
     * dependency without mentioning the version (unspecified version) or
     * specifying a dynamic one (as 1.4+). The {@link #dependencyResolver()}
     * will use the version provided by this method in order to replace
     * unspecified or dynamic versions.
     */
    protected JkVersionProvider versionProvider() {
        return JkVersionProvider.empty();
    }

    /**
     * Specify transitive dependencies to exclude when using certain
     * dependencies.
     */
    protected JkDependencyExclusions dependencyExclusions() {
        return JkDependencyExclusions.builder().build();
    }

    /**
     * Returns the dependencies of this project. By default it is empty.
     */
    protected JkDependencies dependencies() {
        return JkDependencies.of();
    }

    /**
     * The scope that will be used when a dependency has been declared without
     * scope. It can be returns <code>null</code>, meaning that when no scope is
     * mentioned then the dependency is always available.
     */
    protected JkScope defaultScope() {
        return null;
    }

    /**
     * Returns the dependencies that does not need to be explicitly declared.
     * For example, it can include all jar file located under
     * <code>build/libs</code> directory.
     * <p>
     * Normally you don't need to override this method.
     */
    protected JkDependencies implicitDependencies() {
        return JkDependencies.builder().build();
    }

    /**
     * Returns the dependency resolver for this build.
     */
    public final JkDependencyResolver dependencyResolver() {
        if (cachedResolver == null) {
            JkLog.startln("Setting dependency resolver ");
            cachedResolver = JkBuildPlugin.applyDependencyResolver(plugins.getActives(),
                    createDependencyResolver());
            JkLog.done("Resolver set " + cachedResolver);
        }
        return cachedResolver;
    }

    /**
     * Returns the base dependency resolver.
     */
    private JkDependencyResolver createDependencyResolver() {
        final JkDependencies dependencies = effectiveDependencies().and(implicitDependencies());
        if (dependencies.containsModules()) {
            return JkDependencyResolver.managed(downloadRepositories(), dependencies)
                    .withModuleHolder(versionedModule())
                    .withParams(JkResolutionParameters.of().withDefault(scopeMapping()));
        }
        return JkDependencyResolver.unmanaged(dependencies);
    }

    /**
     * Returns the scope mapping used by the underlying dependency manager.
     */
    protected JkScopeMapping scopeMapping() {
        return JkScopeMapping.empty();
    }

    /**
     * Returns the publisher used to actually publish artifacts.
     */
    protected JkPublisher publisher() {
        if (cachedPublisher == null) {
            cachedPublisher = JkPublisher.of(publishRepositories(), this.ouputDir().root());
        }
        return cachedPublisher;
    }

    @Override
    protected JkScaffolder scaffolder() {
        final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
        codeWriter.extendedClass = "JkBuildDependencySupport";
        codeWriter.dependencies = JkDependencies.builder().build();
        codeWriter.imports.clear();
        codeWriter.imports.addAll(JkCodeWriterForBuildClass.importsForJkDependencyBuildSupport());
        return super.scaffolder().buildClassWriter(codeWriter);
    }


    /**
     * Options for multi-purpose repositories.
     */
    public static final class JkOptionRepos {

        /** Maven or Ivy repository to download dependency artifacts. */
        @JkDoc("Maven or Ivy repository to download dependency artifacts.")
        public final JkOptionRepo download = new JkOptionRepo();

        /** Maven or Ivy repositories to publish artifacts. */
        @JkDoc("Maven or Ivy repositories to publish artifacts.")
        public final JkOptionRepo publish = new JkOptionRepo();

        /**
         * Maven or Ivy repositories to publish released artifacts.
         * If this repository is not null, then Jerkar will try to publish snapshot in the publish repo and release in this one.
         */
        @JkDoc({
            "Maven or Ivy repositories to publish released artifacts.",
        "If this repo is not null, then Jerkar will try to publish snapshot in the publish repo and release in this one." })
        public final JkOptionRepo release = new JkOptionRepo();

        /**
         * Constructs a {@link JkOptionRepo} populated with default values
         */
        public JkOptionRepos() {
            download.url = JkRepo.MAVEN_CENTRAL_URL.toExternalForm();
            publish.url = JkRepo.MAVEN_OSSRH_DOWNLOAD_AND_DEPLOY_SNAPSHOT.toExternalForm();
            release.url = JkRepo.MAVEN_OSSRH_DEPLOY_RELEASE.toExternalForm();
        }

    }

    /** Option for a single repository */
    public static final class JkOptionRepo {

        /** Url of the repository : Prefix the Url with 'ivy:' if it is an Ivy repostory. */
        @JkDoc({ "Url of the repository : Prefix the Url with 'ivy:' if it is an Ivy repostory." })
        public String url;

        /**
         * User name to connect to repository (if needed).
         * Null or blank means that the repository will be accessed in an anonymous way.
         */
        @JkDoc({ "User name to connect to repository (if needed).",
        "Null or blank means that the repository will be accessed in an anonymous way." })
        public String username;

        /** Password to connect to the repository (if needed). */
        @JkDoc({ "Password to connect to the repository (if needed)." })
        public String password;

        /** Returns the repo configured here or null if the url is null */
        public JkRepo toRepo() {
            if (url == null) {
                return null;
            }
            return JkRepo.of(url).withOptionalCredentials(username, password);
        }

    }

    /**
     * Returns the PGP signer used to sign produced artifacts.
     */
    public JkPgp pgp() {
        return JkPgp.of(JkOptions.getAll());
    }

    /**
     * Creates {@link JkRepo} form Jerkar options. the specified repository name
     * will be turned to <code>repo.[repoName].url</code>,
     * <code>repo.[repoName].username</code> and
     * <code>repo.[repoName].password</code> options for creating according
     * repository.
     */
    public static JkRepo repoOfOptions(String repoName) {
        final String optionName = "repo." + repoName + "." + "url";
        final String url = JkOptions.get(optionName);
        if (JkUtilsString.isBlank(url)) {
            return null;
        }
        final String username = JkOptions.get("repo." + repoName + ".username");
        final String password = JkOptions.get("repo." + repoName + ".password");
        return JkRepo.of(url.trim()).withOptionalCredentials(username, password);
    }

    /**
     * Creates {@link JkRepos} form Jerkar options. the specified repository
     * name will be turned to <code>repo.[repoName].url</code>,
     * <code>repo.[repoName].username</code> and
     * <code>repo.[repoName].password</code> options for creating according
     * repository.
     *
     * You can specify severals url by using coma separation in
     * <code>repo.[repoName].url</code> option value. but the credential will
     * remain the same for all returned repositories.
     */
    public static JkRepos reposOfOptions(String repoName) {
        final String urls = JkOptions.get("repo." + repoName + "." + "url");
        JkRepos result = JkRepos.of();
        if (JkUtilsString.isBlank(urls)) {
            return result;
        }
        final String username = JkOptions.get("repo." + repoName + ".username");
        final String password = JkOptions.get("repo." + repoName + ".password");
        for (final String url : urls.split(",")) {
            result = result.and(JkRepo.of(url.trim()).withOptionalCredentials(username, password));
        }
        return result;
    }

}
