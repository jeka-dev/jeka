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
import org.jerkar.api.utils.JkUtilsAssert;

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

    @JkDoc("Dependency and publish repositories")
    protected JkOptionRepos repo = new JkOptionRepos();

    @JkDoc("Version to inject to this build. If 'null' or blank than the version will be the one returned by #version()")
    protected String version = null;

    protected JkBuildDependencySupport() {
    }

    /**
     * The current version for this project. It has to be understood as the
     * 'release version', as this version will be used to publish artifacts.
     * <br/>
     * It may take format as <code>1.0-SNAPSHOT</code>,
     * <code>trunk-SNAPSHOT</code>, <code>1.2.3-rc1</code>, <code>1.2.3</code>,
     * ... This may be injected using the 'version' option, otherwise it takes
     * the value returned by {@link #version()} If not, it takes the result from
     * {@link #version()}
     */
    public JkVersion version() {
	return JkVersion.firstNonNull(version, "1.0-SNAPSHOT");
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
	return JkVersionedModule.of(moduleId(), version());
    }

    /**
     * Returns the download repositories where to retrieve artifacts. It has
     * only a meaning in case of using managed dependencies.
     */
    protected JkRepos downloadRepositories() {
	JkUtilsAssert.notNull(this.repo.download.url, "repo.download.url must not be null.");
	return JkRepos.of(JkRepo.of(this.repo.download.url).withOptionalCredentials(this.repo.download.username,
		this.repo.download.password));
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
	final JkRepo defaultDownloadRepo = JkRepo.ofOptional(repo.download.url, repo.download.username,
		repo.download.password);
	final JkRepo defaultPublishRepo = JkRepo.ofOptional(repo.publish.url, repo.publish.username,
		repo.publish.password);
	final JkRepo defaultPublishReleaseRepo = JkRepo.ofOptional(repo.release.url, repo.release.username,
		repo.release.password);

	final JkRepo publishRepo = JkRepo.firstNonNull(defaultPublishRepo, defaultDownloadRepo);
	final JkRepo releaseRepo = JkRepo.firstNonNull(defaultPublishReleaseRepo, publishRepo);

	return JkPublishRepos.of(publishRepo.asPublishSnapshotRepo()).and(releaseRepo.asPublishReleaseRepo());
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
	JkDependencies deps = dependencies()
		.withDefaultScope(this.defaultScope())
		.resolvedWith(versionProvider())
		.withExclusions(dependencyExclusions());
	final JkScope defaultcope = this.defaultScope();
	if (defaultcope != null) {
	    deps = deps.withDefaultScope(defaultcope);
	}
	return JkBuildPlugin.applyDependencies(plugins.getActives(),
		implicitDependencies().and(deps));
    }

    /**
     * On {@link #asDependency(File...)} method, you may have declared dependency without mentioning the version (unspecified version)
     * or specifying a dynamic one (as 1.4+).
     * The {@link #dependencyResolver()} will use the version provided by this method in order to replace unspecified or dynamic versions.
     */
    protected JkVersionProvider versionProvider() {
	return JkVersionProvider.empty();
    }


    /**
     * Specify transitive dependencies to exclude when using certain dependencies.
     */
    protected JkDependencyExclusions dependencyExclusions() {
	return JkDependencyExclusions.builder().build();
    }

    protected JkDependencies dependencies() {
	return JkDependencies.of();
    }

    /**
     * The scope that will be used when a dependency has been declared without
     * scope. It can be returns <code>null</code>, meaning that when no scope is mentioned
     * then the dependency is always available.
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
	    cachedResolver = JkBuildPlugin.applyDependencyResolver(plugins.getActives(), createDependencyResolver());
	    JkLog.done("Resolver set " + cachedResolver);
	}
	return cachedResolver;
    }

    /**
     * Returns the base dependency resolver.
     */
    private JkDependencyResolver createDependencyResolver() {
	final JkDependencies dependencies = effectiveDependencies().and(extraCommandLineDeps());
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

    protected JkPublisher publisher() {
	if (cachedPublisher == null) {
	    cachedPublisher = JkPublisher.of(publishRepositories(), this.ouputDir().root());
	}
	return cachedPublisher;
    }

    protected JkDependencies extraCommandLineDeps() {
	return JkDependencies.builder().build();
    }

    @Override
    protected String scaffoldedBuildClassCode() {
	final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
	codeWriter.extendedClass = "JkBuildDependencySupport";
	codeWriter.dependencies = JkDependencies.builder().build();
	codeWriter.imports.clear();
	codeWriter.imports.addAll(JkCodeWriterForBuildClass.importsFoJkDependencyBuildSupport());
	return codeWriter.wholeClass() + codeWriter.endClass();
    }

    public static final class JkOptionRepos {

	@JkDoc("Maven or Ivy repository to download dependency artifacts.")
	public final JkOptionRepo download = new JkOptionRepo();

	@JkDoc("Maven or Ivy repositories to publish artifacts.")
	public final JkOptionRepo publish = new JkOptionRepo();

	@JkDoc({ "Maven or Ivy repositories to publish released artifacts.",
	"If this repo is not null, then Jerkar will try to publish snapshot in the publish repo and release in this one." })
	public final JkOptionRepo release = new JkOptionRepo();

	public JkOptionRepos() {
	    download.url = JkRepo.MAVEN_CENTRAL_URL.toExternalForm();
	    publish.url = JkRepo.MAVEN_OSSRH_PUSH_SNAPSHOT_AND_PULL.toExternalForm();
	    release.url = JkRepo.MAVEN_OSSRH_PUSH_RELEASE.toExternalForm();
	}

    }

    public static final class JkOptionRepo {

	@JkDoc({ "Url of the repository : Prefix the Url with 'ivy:' if it is an Ivy repostory." })
	public String url;

	@JkDoc({ "Usename to connect to repository (if needed).",
	"Null or blank means that the repository will be accessed in an anonymous way." })
	public String username;

	@JkDoc({ "Password to connect to the repository (if needed)." })
	public String password;

    }

    public JkPgp pgp() {
	return JkPgp.of(JkOptions.getAll());
    }

}
