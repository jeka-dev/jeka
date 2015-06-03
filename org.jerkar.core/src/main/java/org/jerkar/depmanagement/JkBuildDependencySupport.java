package org.jerkar.depmanagement;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.JkBuild;
import org.jerkar.JkBuildPlugin;
import org.jerkar.JkConstants;
import org.jerkar.JkLocator;
import org.jerkar.JkLog;
import org.jerkar.JkOption;
import org.jerkar.JkProject;
import org.jerkar.JkScaffolder;
import org.jerkar.depmanagement.JkRepo.JkMavenRepository;
import org.jerkar.file.JkPath;
import org.jerkar.internal.ivy.JkIvyPublisher;
import org.jerkar.internal.ivy.JkIvyResolver;
import org.jerkar.publishing.JkPublishRepos;
import org.jerkar.publishing.JkPublisher;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsString;

/**
 * Template build definition class providing support for managing dependencies and multi-projects.
 * 
 * @author Jerome Angibaud
 */
public class JkBuildDependencySupport extends JkBuild {

	/**
	 * Default path for the non managed dependencies. This path is relative to {@link #baseDir()}.
	 */
	protected static final String STD_LIB_PATH = "build/libs";

	// A cache for dependency resolver
	private JkDependencyResolver cachedResolver;

	// A cache for artifact publisher
	private JkPublisher cachedPublisher;

	@JkOption("Dependency and publish repositories")
	protected JkOptionRepos repo = new JkOptionRepos();

	@JkOption("Version to inject to this build. If 'null' or blank than the version will be the one returned by #defaultVersion()" )
	protected String forcedVersion = null;

	private final JkMultiProjectDependencies explicitMultiProjectDependencies;

	/**
	 * Other builds (projects) this build depend of.
	 */
	private JkMultiProjectDependencies multiProjectDependencies;

	public JkBuildDependencySupport() {
		final List<JkBuildDependencySupport> subBuilds = populateMultiProjectBuildField(this);
		this.explicitMultiProjectDependencies = JkMultiProjectDependencies.of(this, subBuilds);
	}

	/**
	 * The current version for this project. It has to be understood as the 'release version',
	 * as this version will be used to publish artifacts. <br/>
	 * it may take format as <code>1.0-SNAPSHOT</code>, <code>trunk-SNAPSHOT</code>, <code>1.2.3-rc1</code>, <code>1.2.3</code>, ...
	 * This may be injected using the 'version' option, otherwise it takes the value returned by {@link #defaultVersion()}
	 * If not, it takes the result from {@link #defaultVersion()}
	 */
	public final JkVersion version() {
		if (JkUtilsString.isBlank(this.forcedVersion)) {
			return defaultVersion();
		}
		return JkVersion.ofName(forcedVersion);
	}

	/**
	 * Returns the version returned by {@link JkBuildDependencySupport#version()} when not forced.
	 * 
	 * @see #version()
	 */
	protected JkVersion defaultVersion() {
		return JkVersion.fromOptionalResourceOrExplicit(getClass(), "1.0-SNAPSHOT");

	}

	/**
	 * Returns identifier for this project.
	 * This identifier is used to name generated artifacts and by dependency manager.
	 */
	public JkModuleId moduleId() {
		return JkModuleId.of(baseDir().root().getName());
	}

	/**
	 * Returns
	 */
	protected final JkVersionedModule module() {
		return JkVersionedModule.of(moduleId(), version());
	}

	/**
	 * Returns the parameterized JkIvyResolver instance to use when dealing with managed dependencies.
	 * If you don't use managed dependencies, this method is never invoked.
	 */
	protected JkIvyResolver jkIvyResolver() {
		return JkIvyResolver.of(publishRepositories(), downloadRepositories());
	}

	/**
	 * Returns the parameterized JkIvyPublisher instance to use when dealing with publication.
	 * If you don't publish artifacts, this method is never invoked.
	 */
	protected JkIvyPublisher jkIvyPublisher() {
		return JkIvyPublisher.of(publishRepositories());
	}

	/**
	 * Returns the download repositories where to retrieve artifacts. It has only a meaning in case of using
	 * managed dependencies.
	 */
	protected JkRepos downloadRepositories() {
		return JkRepos.of(JkRepo.of(this.repo.download.url)
				.withOptionalCredentials(this.repo.download.username, this.repo.download.password));
	}

	/**
	 * Returns the repositories where are published artifacts.
	 */
	protected JkPublishRepos publishRepositories() {
		final JkRepo defaultDownloadRepo = JkRepo.ofOptional(repo.download.url, repo.download.username, repo.download.password);
		final JkRepo defaultPublishRepo = JkRepo.ofOptional(repo.publish.url, repo.publish.username, repo.publish.password);
		final JkRepo defaultPublishReleaseRepo = JkRepo.ofOptional(repo.release.url, repo.release.username, repo.release.password);

		final JkRepo publishRepo = JkRepo.firstNonNull(defaultPublishRepo, defaultDownloadRepo);
		final JkRepo releaseRepo = JkRepo.firstNonNull(defaultPublishReleaseRepo, publishRepo);

		return JkPublishRepos.ofSnapshotAndRelease(publishRepo, releaseRepo);
	}

	/**
	 * Returns the resolved dependencies for the given scope. Depending on the passed
	 * options, it may be augmented with extra-libs mentioned in options <code>extraXxxxPath</code>.
	 */
	public final JkPath depsFor(JkScope ...scopes) {
		return dependencyResolver().get(scopes);
	}

	/**
	 * Returns the dependencies of this module. By default it uses unmanaged dependencies stored
	 * locally in the project as described by {@link #implicitDependencies()} method.
	 * If you want to use managed dependencies, you must override this method.
	 */
	private JkDependencies effectiveDependencies() {
		return JkBuildPlugin.applyDependencies(plugins.getActives(),
				implicitDependencies().and(dependencies().withDefaultScope(this.defaultScope())));
	}

	protected JkDependencies dependencies() {
		return JkDependencies.on();
	}

	/**
	 * The scope that will be used when a dependency has been declared without scope.
	 */
	protected JkScope defaultScope() {
		return JkScope.BUILD;
	}

	/**
	 * Returns the dependencies that does not need to be explicitly declared.
	 * For example, it can include all jar file located under <code>build/libs</code> directory.
	 * <p>Normally you don't need to override this method.
	 */
	protected JkDependencies implicitDependencies() {
		return JkDependencies.builder().build();
	}

	public final JkDependencyResolver dependencyResolver() {
		if (cachedResolver == null) {
			JkLog.startln("Setting dependency resolver ");
			cachedResolver = JkBuildPlugin.applyDependencyResolver(plugins.getActives()
					, createDependencyResolver());
			JkLog.done("Resolver set " + cachedResolver);
		}
		return cachedResolver;
	}

	/**
	 * Returns the base dependency resolver.
	 */
	private JkDependencyResolver createDependencyResolver() {
		final JkDependencies dependencies = effectiveDependencies().and(extraCommandLineDeps());
		if (dependencies.containsExternalModule()) {
			return JkDependencyResolver.managed(jkIvyResolver(), dependencies, module(),
					JkResolutionParameters.of(scopeMapping()));
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
			cachedPublisher = JkPublisher.usingIvy(jkIvyPublisher());
		}
		return cachedPublisher;
	}

	protected JkDependencies extraCommandLineDeps() {
		return JkDependencies.builder().build();
	}

	@Override
	protected JkScaffolder scaffolder() {
		return JkScaffolder.of(this).withExtraAction(new Runnable() {

			@Override
			public void run() {
				final File spec = baseDir(JkConstants.BUILD_DEF_DIR);
				final String packageName = moduleId().group().replace('.', '/');
				new File(spec, packageName).mkdirs();
			}
		})
		.withExtendedClass(JkBuild.class);

	}

	/**
	 * Returns dependencies on other projects
	 */
	public final JkMultiProjectDependencies multiProjectDependencies() {
		if (multiProjectDependencies == null) {
			multiProjectDependencies = this.explicitMultiProjectDependencies.and(this.effectiveDependencies().projectDependencies());
		}
		return multiProjectDependencies;

	}

	@SuppressWarnings("unchecked")
	private static List<JkBuildDependencySupport> populateMultiProjectBuildField(JkBuildDependencySupport mainBuild) {
		final List<JkBuildDependencySupport> result = new LinkedList<JkBuildDependencySupport>();
		final List<Field> fields = JkUtilsReflect.getAllDeclaredField(mainBuild.getClass(), JkProject.class);
		for (final Field field : fields) {
			final JkProject jkProject = field.getAnnotation(JkProject.class);
			final JkBuildDependencySupport subBuild = relativeProject(mainBuild, (Class<? extends JkBuildDependencySupport>) field.getType(),
					jkProject.value());
			JkUtilsReflect.setFieldValue(mainBuild, field, subBuild);
			result.add(subBuild);
		}
		return result;
	}

	public JkBuild relativeProject(String relativePath) {
		return relativeProject(this, null, relativePath);
	}

	private static final JkBuildDependencySupport relativeProject(JkBuildDependencySupport mainBuild, Class<? extends JkBuildDependencySupport> clazz, String relativePath) {
		final JkBuildDependencySupport build = mainBuild.relativeProjectBuild(clazz, relativePath);

		build.init();
		return build;
	}

	public static final class JkOptionRepos {

		@JkOption("Maven or Ivy repository to download dependency artifacts.")
		private final JkOptionRepo download = new JkOptionRepo();

		@JkOption("Maven or Ivy repositories to publish artifacts.")
		private final JkOptionRepo publish;

		@JkOption({"Maven or Ivy repositories to publish released artifacts.",
		"If this repo is not null, then Jerkar will try to publish snapshot in the publish repo and release in this one."})
		private final JkOptionRepo release;

		public JkOptionRepos() {
			publish = new JkOptionRepo();
			final File defaultPublishDir = new File(JkLocator.jerkarUserHome(), "publish");
			defaultPublishDir.mkdirs();
			publish.url = JkUtilsFile.toUrl(defaultPublishDir).toExternalForm();
			release = publish;
		}
	}

	public static final class JkOptionRepo {

		@JkOption({"Url of the repository : Prefix the Url with 'ivy:' if it is an Ivy repostory."})
		private String url = JkMavenRepository.MAVEN_CENTRAL_URL.toString();

		@JkOption({"Usename to connect to repository (if needed).",
		"Null or blank means that the repository will be accessed in an anonymous way."})
		private final String username = null;

		@JkOption({"Password to connect to the repository (if needed)."})
		private final String password = null;

	}

}
