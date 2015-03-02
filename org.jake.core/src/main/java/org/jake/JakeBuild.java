package org.jake;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeDependency;
import org.jake.depmanagement.JakeDependencyResolver;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepo.MavenRepository;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeResolutionParameters;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeVersion;
import org.jake.depmanagement.JakeVersionedModule;
import org.jake.depmanagement.ivy.JakeIvy;
import org.jake.publishing.JakePublishRepos;
import org.jake.publishing.JakePublisher;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;
import org.jake.utils.JakeUtilsTime;

/**
 * Base class defining commons tasks and utilities
 * necessary for building any kind of project, regardless involved technologies.
 * 
 * @author Jerome Angibaud
 */
public class JakeBuild {

	/**
	 * Default path for the non managed dependencies. This path is relative to {@link #baseDir()}.
	 */
	protected static final String STD_LIB_PATH = "build/libs";

	// A cache for dependency resolver
	private JakeDependencyResolver cachedResolver;

	private Locator locator = Locator.ofDir(JakeUtilsFile.workingDir());

	private final Date buildTime = JakeUtilsTime.now();

	// A cache for artifact publisher
	private JakePublisher cachedPublisher;

	@JakeOption({"Maven or Ivy repositories to download dependency artifacts.",
	"Prefix the Url with 'ivy:' if it is an Ivy repostory."})
	private final String downloadRepoUrl = MavenRepository.MAVEN_CENTRAL_URL.toString();

	@JakeOption({"Usename to connect to the download repository (if needed).",
	"Null or blank means that the upload repository will be accessed in an anonymous way."})
	private final String dowloadRepoUsername = null;

	@JakeOption({"Password to connect to the download repository (if needed)."})
	private final String downloadRepoPassword = null;

	@JakeOption({"Specify the publish repository if it is different than the download one.",
	"Prefix the Url with 'ivy:' if it is an Ivy repository."})
	private final String publishRepoUrl = null;

	@JakeOption({"Usename to connect to the publish repository (if needed).",
	"Null or blank means that the upload repository will be accessed in an anonymous way."})
	private final String publishRepoUsername = null;

	@JakeOption({"Password to connect to the publish repository (if needed)."})
	private final String publishRepoPassword = null;

	@JakeOption("Specify the publish repository for releases if it is different than the one for snapshots.")
	private final String publishRepoReleaseUrl = null;

	@JakeOption("Usename to connect to the publish release repository (if needed).")
	private final String publishRepoReleaseUsername = null;

	@JakeOption("Password to connect to the publish release repository (if needed).")
	private final String publishRepoReleasePassword = null;

	@JakeOption("Version to inject to this build. If 'null' or blank than the version will be the one returned by #defaultVersion()" )
	private String forcedVersion = null;

	@JakeOption({
		"Mention if you want to add extra lib in your build path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the build path cto compile and run Jake scripts.",
	"Example : -extraCompilePath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private final String extraJakePath = null;

	protected JakeBuild() {
	}

	/**
	 * Defines this project base directory.
	 */
	public void setBaseDir(File baseDir) {
		this.locator = Locator.ofDir(baseDir);
	}

	/**
	 * Returns the classes accepted as template for plugins.
	 * If you override it, do not forget to add the ones from the super class.
	 */
	protected List<Class<Object>> pluginTemplateClasses() {
		return Collections.emptyList();
	}

	/**
	 * Set the plugins to activate for this build.
	 * This method should be invoked after the {@link #setBaseDir(File)} method, so
	 * plugins can be configured using the proper base dir.
	 */
	protected void setPlugins(Iterable<?> plugins) {
		// Do nothing as no plugin extension as been defined at this level.
	}

	/**
	 * The current version for this project. This may be injected using the 'version' option.
	 * If not, it takes the result from {@link #defaultVersion()}
	 */
	public final JakeVersion version() {
		if (JakeUtilsString.isBlank(this.forcedVersion)) {
			return defaultVersion();
		}
		return JakeVersion.named(forcedVersion);
	}

	/**
	 * The current version for this project. Might look like "0.6.3", "0.1-SNAPSHOT" or "20141220170532".
	 * Default is the time stamp (formatted as 'yyyyMMdd-HHmmss') this build has been instantiated.
	 */
	public JakeVersion defaultVersion() {
		return JakeVersion.named(JakeUtilsTime.timestampSec(buildTime));
	}

	/**
	 * The project name. This is likely to used in produced artifacts.
	 */
	public String projectName() {
		final String projectDirName = baseDir().root().getName();
		return projectDirName.contains(".") ? JakeUtilsString.substringAfterLast(projectDirName, ".") : projectDirName;
	}

	/**
	 * The project group name. This is likely to used in produced artifacts.
	 */
	public String groupName() {
		final String projectDirName = baseDir().root().getName();
		return projectDirName.contains(".") ? JakeUtilsString.substringBeforeLast(projectDirName, ".") : projectDirName;
	}

	/**
	 * By default, this method returns the concatenation of the project group and project name. It is likely to
	 * be used as produced artifacts file names.
	 */
	public String projectFullName() {
		if (groupName() == null || groupName().equals(projectName())) {
			return projectName();
		}
		return groupName()+ "." + projectName();
	}

	protected final JakeVersionedModule module() {
		return JakeVersionedModule.of(JakeModuleId.of(groupName(), projectName()), version());
	}

	/**
	 * Returns the parameterized JakeIvy instance to use when dealing with managed dependencies.
	 * If you don't use managed dependencies, this method is never invoked.
	 */
	protected JakeIvy jakeIvy() {
		return JakeIvy.of(publishRepositories(), downloadRepositories());
	}

	/**
	 * Returns the download repositories where to retrieve artifacts. It has only a meaning in case of using
	 * managed dependencies.
	 */
	protected JakeRepos downloadRepositories() {
		return JakeRepos.of(JakeRepo.of(this.downloadRepoUrl)
				.withOptionalCredentials(this.dowloadRepoUsername, this.downloadRepoPassword));
	}

	/**
	 * Returns the repositories where are published artifacts.
	 */
	protected JakePublishRepos publishRepositories() {
		final JakeRepo defaultDownloadRepo = JakeRepo.ofOptional(downloadRepoUrl, dowloadRepoUsername, downloadRepoPassword);
		final JakeRepo defaultPublishRepo = JakeRepo.ofOptional(publishRepoUrl, publishRepoUsername, publishRepoPassword);
		final JakeRepo defaultPublishReleaseRepo = JakeRepo.ofOptional(publishRepoReleaseUrl, publishRepoReleaseUsername, publishRepoReleasePassword);

		final JakeRepo publishRepo = JakeRepo.firstNonNull(defaultPublishRepo, defaultDownloadRepo);
		final JakeRepo releaseRepo = JakeRepo.firstNonNull(defaultPublishReleaseRepo, publishRepo);

		return JakePublishRepos.ofSnapshotAndRelease(publishRepo, releaseRepo);
	}

	protected Date buildTime() {
		return (Date) buildTime.clone();
	}

	/**
	 * Returns the resolved dependencies for the given scope. Depending on the passed
	 * options, it may be augmented with extra-libs mentioned in options <code>extraXxxxPath</code>.
	 */
	public final JakePath depsFor(JakeScope ...scopes) {
		return dependencyResolver().get(scopes);
	}

	/**
	 * Returns the dependencies of this module. By default it uses unmanaged dependencies stored
	 * locally in the project as described by {@link #localDependencies()} method.
	 * If you want to use managed dependencies, you must override this method.
	 */
	protected JakeDependencies dependencies() {
		return localDependencies();
	}

	/**
	 * Returns the dependencies located locally to the project.
	 */
	protected JakeDependencies localDependencies() {
		final JakeDir libDir = JakeDir.of(baseDir(STD_LIB_PATH));
		if (!libDir.root().exists()) {
			return JakeDependencies.builder().build();
		}
		return JakeDependencies.builder()
				.usingDefaultScopes(Project.JAKE_SCOPE)
				.on(JakeDependency.of(libDir.include("*.jar", "jake/*.jar"))).build();
	}

	public final JakeDependencyResolver dependencyResolver() {
		if (cachedResolver == null) {
			JakeLog.startln("Setting dependency resolver ");
			cachedResolver = createDependencyResolver();
			JakeLog.done("Resolver set " + cachedResolver);
		}
		return cachedResolver;
	}

	/**
	 * Returns the base dependency resolver.
	 */
	protected JakeDependencyResolver createDependencyResolver() {
		final JakeDependencies dependencies = dependencies().and(extraCommandLineDeps());
		if (dependencies.containsExternalModule()) {
			return JakeDependencyResolver.managed(jakeIvy(), dependencies, module(),
					JakeResolutionParameters.of(scopeMapping()));
		}
		return JakeDependencyResolver.unmanaged(dependencies);
	}

	protected JakeScopeMapping scopeMapping() {
		return JakeScopeMapping.of(Project.JAKE_SCOPE).to("default(*)");
	}

	protected JakePublisher publisher() {
		if (cachedPublisher == null) {
			cachedPublisher = JakePublisher.usingIvy(jakeIvy());
		}
		return cachedPublisher;
	}

	protected JakeDependencies extraCommandLineDeps() {
		return JakeDependencies.builder()
				.usingDefaultScopes(Project.JAKE_SCOPE).onFiles(toPath(extraJakePath))
				.build();
	}

	protected final JakeClasspath toPath(String pathAsString) {
		if (pathAsString == null) {
			return JakeClasspath.of();
		}
		return JakeClasspath.of(JakeUtilsFile.toPath(pathAsString, ";", baseDir().root()));
	}



	/**
	 * Returns the base directory for this project. All file/directory path are
	 * resolved from this directory.
	 */
	public final JakeDir baseDir() {
		return JakeDir.of(locator.baseDir());
	}

	/**
	 * Return a file located at the specified path relative to the base directory.
	 */
	public final File baseDir(String relativePath) {
		if (relativePath.isEmpty()) {
			return baseDir().root();
		}
		return baseDir().file(relativePath);
	}

	/**
	 * The output directory where all the final and intermediate
	 * artifacts are generated.
	 */
	public JakeDir ouputDir() {
		return baseDir().sub("build/output").createIfNotExist();
	}

	/**
	 * Returns the file located at the specified path relative to the output directory.
	 */
	public File ouputDir(String relativePath) {
		return ouputDir().file(relativePath);
	}

	// ------------ Operations ------------

	@JakeDoc("Clean the output directory.")
	public void clean() {
		JakeLog.start("Cleaning output directory " + ouputDir().root().getPath() );
		JakeUtilsFile.deleteDirContent(ouputDir().root());
		JakeLog.done();
	}

	@JakeDoc("Conventional method standing for the default operations to perform.")
	public void base() {
		clean();
	}

	@JakeDoc("Display all available methods defined in this build.")
	public void help() {
		HelpDisplayer.help(this);
	}

	@JakeDoc("Display details on all available plugins.")
	public void helpPlugins() {
		HelpDisplayer.helpPlugins(this);
	}

	public final <T extends JakeBuild> T relativeProject(Class<T> clazz, String relativePath) {
		final T build = JakeUtilsReflect.newInstance(clazz);
		build.locator = Locator.ofProjectRealive(this, relativePath);
		build.forcedVersion = this.version().name();
		return build;
	}

	private static class Locator {

		private final File dir;

		private final JakeBuild relateProject;

		private final String relativePath;

		private Locator(File dir, JakeBuild relateProject, String relativePath) {
			super();
			this.dir = dir;
			this.relateProject = relateProject;
			this.relativePath = relativePath;
		}

		public static Locator ofDir(File dir) {
			return new Locator(dir, null, null);
		}

		public static Locator ofProjectRealive(JakeBuild build, String relativePath) {
			return new Locator(null, build, relativePath);
		}

		public File baseDir() {
			if (dir != null) {
				return dir;
			}
			return this.relateProject.baseDir(relativePath);
		}

	}


}
