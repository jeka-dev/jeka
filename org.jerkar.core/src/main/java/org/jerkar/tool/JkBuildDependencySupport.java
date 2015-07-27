package org.jerkar.tool;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkPublisher;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolutionParameters;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Template build definition class providing support for managing dependencies and multi-projects.
 * 
 * @author Jerome Angibaud
 */
public class JkBuildDependencySupport extends JkBuild {

	private static final ThreadLocal<Map<SubProjectRef, JkBuild>> SUB_PROJECT_CONTEXT =
			new ThreadLocal<Map<SubProjectRef, JkBuild>>();


	/**
	 * Default path for the non managed dependencies. This path is relative to {@link #baseDir()}.
	 */
	protected static final String STD_LIB_PATH = "build/libs";

	// A cache for dependency resolver
	private JkDependencyResolver cachedResolver;

	// A cache for artifact publisher
	private JkPublisher cachedPublisher;

	@JkDoc("Dependency and publish repositories")
	protected JkOptionRepos repo = new JkOptionRepos();

	@JkDoc("Version to inject to this build. If 'null' or blank than the version will be the one returned by #version()" )
	protected String version = null;

	private final JkSlaveBuilds annotatedJkProjectSlaves;

	// all slaves of this build
	private JkSlaveBuilds slaves;

	public JkBuildDependencySupport() {
		final List<JkBuild> subBuilds = populateJkProjectAnnotatedFields();
		this.annotatedJkProjectSlaves = JkSlaveBuilds.of(this.baseDir().root(), subBuilds);
	}

	/**
	 * The current version for this project. It has to be understood as the 'release version',
	 * as this version will be used to publish artifacts. <br/>
	 * It may take format as <code>1.0-SNAPSHOT</code>, <code>trunk-SNAPSHOT</code>, <code>1.2.3-rc1</code>, <code>1.2.3</code>, ...
	 * This may be injected using the 'version' option, otherwise it takes the value returned by {@link #version()}
	 * If not, it takes the result from {@link #version()}
	 */
	public final JkVersion actualVersion() {
		if (JkUtilsString.isBlank(this.version)) {
			return version();
		}
		return JkVersion.ofName(version);
	}

	/**
	 * Returns the version returned by {@link JkBuildDependencySupport#actualVersion()} when not forced.
	 * 
	 * @see #actualVersion()
	 */
	protected JkVersion version() {
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
	 * Returns moduleId along its version
	 */
	protected final JkVersionedModule versionedModule() {
		return JkVersionedModule.of(moduleId(), actualVersion());
	}

	/**
	 * Returns the download repositories where to retrieve artifacts. It has only a meaning in case of using
	 * managed dependencies.
	 */
	protected JkRepos downloadRepositories() {
		JkUtilsAssert.notNull(this.repo.download.url, "repo.download.url must not be null.");
		return JkRepos.of(JkRepo.of(this.repo.download.url)
				.withOptionalCredentials(this.repo.download.username, this.repo.download.password));
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
				final File file = new File(JkLocator.jerkarUserHome(), "maven-publish-dir");
				JkLog.info("No credential specifified for publish repo : use local filesystem repo." + file.getAbsolutePath());
				return JkPublishRepos.maven(file);
			}
		}

		// One of release or publish url has been specified
		final JkRepo defaultDownloadRepo = JkRepo.ofOptional(repo.download.url, repo.download.username, repo.download.password);
		final JkRepo defaultPublishRepo = JkRepo.ofOptional(repo.publish.url, repo.publish.username, repo.publish.password);
		final JkRepo defaultPublishReleaseRepo = JkRepo.ofOptional(repo.release.url, repo.release.username, repo.release.password);

		final JkRepo publishRepo = JkRepo.firstNonNull(defaultPublishRepo, defaultDownloadRepo);
		final JkRepo releaseRepo = JkRepo.firstNonNull(defaultPublishReleaseRepo, publishRepo);

		return JkPublishRepos.of(publishRepo.asPublishSnapshotRepo())
				.and(releaseRepo.asPublishReleaseRepo());
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
			return JkDependencyResolver.managed(downloadRepositories(), dependencies, versionedModule(),
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
			cachedPublisher = JkPublisher.of(publishRepositories(), this.ouputDir().root());
		}
		return cachedPublisher;
	}

	protected JkDependencies extraCommandLineDeps() {
		return JkDependencies.builder().build();
	}

	@Override
	protected JkScaffolder scaffolder() {
		final URL template = JkBuildDependencySupport.class.getResource("DepSupportBuild.java_sample");
		return JkScaffolder.of(this, template).withExtraAction(new Runnable() {

			@Override
			public void run() {
				final File spec = baseDir(JkConstants.BUILD_DEF_DIR);
				final String packageName = moduleId().group().replace('.', '/');
				new File(spec, packageName).mkdirs();
			}
		})
		.withExtendedClass(JkBuildDependencySupport.class);
	}

	/**
	 * Returns slave builds (potentially on other projects).
	 */
	public final JkSlaveBuilds slaves() {
		if (slaves == null) {
			slaves = this.annotatedJkProjectSlaves.and(projectBuildDependencies(this.effectiveDependencies()));
		}
		return slaves;

	}

	/**
	 * Returns all build included in these dependencies.
	 * The builds are coming from {@link BuildDependency}.
	 */
	private static List<JkBuild> projectBuildDependencies(JkDependencies dependencies) {
		final List<JkBuild> result = new LinkedList<JkBuild>();
		for (final JkScopedDependency scopedDependency : dependencies) {
			if (scopedDependency.dependency() instanceof BuildDependency) {
				final BuildDependency projectDependency = (BuildDependency) scopedDependency.dependency();
				result.add(projectDependency.projectBuild());
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<JkBuild> populateJkProjectAnnotatedFields() {
		final List<JkBuild> result = new LinkedList<JkBuild>();
		final List<Field> fields = JkUtilsReflect.getAllDeclaredField(this.getClass(), JkProject.class);
		for (final Field field : fields) {
			final JkProject jkProject = field.getAnnotation(JkProject.class);
			final JkBuildDependencySupport subBuild = relativeProject(this, (Class<? extends JkBuildDependencySupport>) field.getType(),
					jkProject.value());
			JkUtilsReflect.setFieldValue(this, field, subBuild);
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

	/**
	 * Creates an instance of <code>JkBuild</code> for the given project and build class.
	 * The instance field annotated with <code>JkOption</code> are populated as usual.
	 */
	@SuppressWarnings("unchecked")
	private final <T extends JkBuild> T relativeProjectBuild(Class<T> clazz, String relativePath) {
		final File projectDir = this.baseDir(relativePath);
		final SubProjectRef projectRef = new SubProjectRef(projectDir, clazz);
		Map<SubProjectRef, JkBuild> map = SUB_PROJECT_CONTEXT.get();
		if (map == null) {
			map = new HashMap<JkBuildDependencySupport.SubProjectRef, JkBuild>();
			SUB_PROJECT_CONTEXT.set(map);
		}
		final T cachedResult = (T) SUB_PROJECT_CONTEXT.get().get(projectRef);
		if (cachedResult != null) {
			return cachedResult;
		}
		final Project project = new Project(projectDir);
		final T result = project.getBuild(clazz);
		JkOptions.populateFields(result);
		SUB_PROJECT_CONTEXT.get().put(projectRef, result);
		return result;
	}



	public static final class JkOptionRepos {

		@JkDoc("Maven or Ivy repository to download dependency artifacts.")
		public final JkOptionRepo download = new JkOptionRepo();

		@JkDoc("Maven or Ivy repositories to publish artifacts.")
		public final JkOptionRepo publish = new JkOptionRepo();

		@JkDoc({"Maven or Ivy repositories to publish released artifacts.",
		"If this repo is not null, then Jerkar will try to publish snapshot in the publish repo and release in this one."})
		public final JkOptionRepo release = new JkOptionRepo();

		public JkOptionRepos() {
			download.url = JkRepo.MAVEN_CENTRAL_URL.toExternalForm();
			publish.url = JkRepo.MAVEN_OSSRH_PUSH_SNAPSHOT_AND_PULL.toExternalForm();
			release.url = JkRepo.MAVEN_OSSRH_PUSH_RELEASE.toExternalForm();
		}

	}

	public static final class JkOptionRepo {

		@JkDoc({"Url of the repository : Prefix the Url with 'ivy:' if it is an Ivy repostory."})
		public String url;

		@JkDoc({"Usename to connect to repository (if needed).",
		"Null or blank means that the repository will be accessed in an anonymous way."})
		public String username;

		@JkDoc({"Password to connect to the repository (if needed)."})
		public String password;

	}

	public JkPgp pgp() {
		return JkPgp.of(JkOptions.getAll());
	}

	private static class SubProjectRef {

		final String canonicalFileName;

		final Class<?> clazz;

		SubProjectRef(File projectDir, Class<?> clazz) {
			super();
			this.canonicalFileName = JkUtilsFile.canonicalPath(projectDir);
			this.clazz = clazz;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((canonicalFileName == null) ? 0 : canonicalFileName
							.hashCode());
			result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SubProjectRef other = (SubProjectRef) obj;
			if (canonicalFileName == null) {
				if (other.canonicalFileName != null) {
					return false;
				}
			} else if (!canonicalFileName.equals(other.canonicalFileName)) {
				return false;
			}
			if (clazz == null) {
				if (other.clazz != null) {
					return false;
				}
			} else if (!clazz.equals(other.clazz)) {
				return false;
			}
			return true;
		}




	}

}
