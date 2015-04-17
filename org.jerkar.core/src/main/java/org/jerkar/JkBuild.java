package org.jerkar;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkDependencyResolver;
import org.jerkar.depmanagement.JkModuleId;
import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkRepo.MavenRepository;
import org.jerkar.depmanagement.JkRepos;
import org.jerkar.depmanagement.JkResolutionParameters;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.depmanagement.ivy.JkIvy;
import org.jerkar.publishing.JkPublishRepos;
import org.jerkar.publishing.JkPublisher;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsString;
import org.jerkar.utils.JkUtilsTime;

/**
 * Base class defining commons tasks and utilities
 * necessary for building any kind of project, regardless involved technologies.
 * 
 * @author Jerome Angibaud
 */
public class JkBuild {

	/**
	 * Default path for the non managed dependencies. This path is relative to {@link #baseDir()}.
	 */
	protected static final String STD_LIB_PATH = "build/libs";

	// A cache for dependency resolver
	private JkDependencyResolver cachedResolver;

	private File baseDir = JkUtilsFile.workingDir();

	private final Date buildTime = JkUtilsTime.now();

	// A cache for artifact publisher
	private JkPublisher cachedPublisher;

	protected final JkBuildPlugins plugins = new JkBuildPlugins(this);

	@JkOption({"Maven or Ivy repositories to download dependency artifacts.",
	"Prefix the Url with 'ivy:' if it is an Ivy repostory."})
	private final String downloadRepoUrl = MavenRepository.MAVEN_CENTRAL_URL.toString();

	@JkOption({"Usename to connect to the download repository (if needed).",
	"Null or blank means that the upload repository will be accessed in an anonymous way."})
	private final String dowloadRepoUsername = null;

	@JkOption({"Password to connect to the download repository (if needed)."})
	private final String downloadRepoPassword = null;

	@JkOption({"Specify the publish repository if it is different than the download one.",
	"Prefix the Url with 'ivy:' if it is an Ivy repository."})
	private final String publishRepoUrl = null;

	@JkOption({"Usename to connect to the publish repository (if needed).",
	"Null or blank means that the upload repository will be accessed in an anonymous way."})
	private final String publishRepoUsername = null;

	@JkOption({"Password to connect to the publish repository (if needed)."})
	private final String publishRepoPassword = null;

	@JkOption("Specify the publish repository for releases if it is different than the one for snapshots.")
	private final String publishRepoReleaseUrl = null;

	@JkOption("Usename to connect to the publish release repository (if needed).")
	private final String publishRepoReleaseUsername = null;

	@JkOption("Password to connect to the publish release repository (if needed).")
	private final String publishRepoReleasePassword = null;

	@JkOption("Version to inject to this build. If 'null' or blank than the version will be the one returned by #defaultVersion()" )
	private final String forcedVersion = null;

	@JkOption({
		"Mention if you want to add extra lib in your build path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the build path cto compile and run Jerkar scripts.",
	"Example : -extraCompilePath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private final String extraJerkarPath = null;

	private final JkBuildDependencies explicitBuildDependencies;

	/**
	 * Other builds (projects) this build depend of.
	 */
	private JkBuildDependencies buildDependencies;

	protected JkBuild() {
		final List<JkBuild> subBuilds = populateProjectBuildField(this);
		this.explicitBuildDependencies = JkBuildDependencies.of(this, subBuilds);
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
	 * The current version for this project. It has to be understood as the 'release version',
	 * as this version will be used to publish artifacts. <br/>
	 * it may take format as <code>1.0-SNAPSHOT</code>, <code>trunk-SNAPSHOT</code>, <code>1.2.3-rc1</code>, <code>1.2.3</code>, ...
	 * This may be injected using the 'version' option, otherwise it takes the value returned by {@link #releaseVersion()}
	 * If not, it takes the result from {@link #releaseVersion()}
	 */
	public final JkVersion version() {
		if (JkUtilsString.isBlank(this.forcedVersion)) {
			return releaseVersion();
		}
		return JkVersion.named(forcedVersion);
	}

	/**
	 * Returns the release version to use for publishing when this one is not enforced by the 'version' option.
	 * 
	 * @see #version()
	 */
	protected JkVersion releaseVersion() {
		return JkVersion.named("1.0-SNAPSHOT");
	}

	/**
	 * Returns the time-stamp this build has been initiated.
	 * Default is the time stamp (formatted as 'yyyyMMdd-HHmmss') this build has been instantiated.
	 */
	public String buildTimestamp() {
		return JkUtilsTime.timestampSec(buildTime);
	}

	/**
	 * The project name. This is likely to used in produced artifacts.
	 */
	public String projectName() {
		final String projectDirName = baseDir().root().getName();
		return projectDirName.contains(".") ? JkUtilsString.substringAfterLast(projectDirName, ".") : projectDirName;
	}

	/**
	 * The project group name. This is likely to used in produced artifacts.
	 */
	public String groupName() {
		final String projectDirName = baseDir().root().getName();
		return projectDirName.contains(".") ? JkUtilsString.substringBeforeLast(projectDirName, ".") : projectDirName;
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

	protected final JkVersionedModule module() {
		return JkVersionedModule.of(JkModuleId.of(groupName(), projectName()), version());
	}

	/**
	 * Returns the parameterized JkIvy instance to use when dealing with managed dependencies.
	 * If you don't use managed dependencies, this method is never invoked.
	 */
	protected JkIvy jkIvy() {
		return JkIvy.of(publishRepositories(), downloadRepositories());
	}

	/**
	 * Returns the download repositories where to retrieve artifacts. It has only a meaning in case of using
	 * managed dependencies.
	 */
	protected JkRepos downloadRepositories() {
		return JkRepos.of(JkRepo.of(this.downloadRepoUrl)
				.withOptionalCredentials(this.dowloadRepoUsername, this.downloadRepoPassword));
	}

	/**
	 * Returns the repositories where are published artifacts.
	 */
	protected JkPublishRepos publishRepositories() {
		final JkRepo defaultDownloadRepo = JkRepo.ofOptional(downloadRepoUrl, dowloadRepoUsername, downloadRepoPassword);
		final JkRepo defaultPublishRepo = JkRepo.ofOptional(publishRepoUrl, publishRepoUsername, publishRepoPassword);
		final JkRepo defaultPublishReleaseRepo = JkRepo.ofOptional(publishRepoReleaseUrl, publishRepoReleaseUsername, publishRepoReleasePassword);

		final JkRepo publishRepo = JkRepo.firstNonNull(defaultPublishRepo, defaultDownloadRepo);
		final JkRepo releaseRepo = JkRepo.firstNonNull(defaultPublishReleaseRepo, publishRepo);

		return JkPublishRepos.ofSnapshotAndRelease(publishRepo, releaseRepo);
	}

	protected Date buildTime() {
		return (Date) buildTime.clone();
	}

	/**
	 * Returns the resolved dependencies for the given scope. Depending on the passed
	 * options, it may be augmented with extra-libs mentioned in options <code>extraXxxxPath</code>.
	 */
	public final JkPath depsFor(JkScope ...scopes) {
		return dependencyResolver().get(scopes);
	}

	/**
	 * Returns the builds this build references.
	 */
	public final JkBuildDependencies buildDependencies() {
		if (buildDependencies == null) {
			buildDependencies = this.explicitBuildDependencies.and(this.dependencies().buildDependencies());
		}
		return buildDependencies;

	}

	/**
	 * Returns the dependencies of this module. By default it uses unmanaged dependencies stored
	 * locally in the project as described by {@link #localDependencies()} method.
	 * If you want to use managed dependencies, you must override this method.
	 */
	protected JkDependencies dependencies() {
		return JkBuildPlugin.applyDependencies(plugins.getActives(), localDependencies());
	}

	/**
	 * Returns the dependencies located locally to the project.
	 */
	protected JkDependencies localDependencies() {
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
		final JkDependencies dependencies = dependencies().and(extraCommandLineDeps());
		if (dependencies.containsExternalModule()) {
			return JkDependencyResolver.managed(jkIvy(), dependencies, module(),
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
			cachedPublisher = JkPublisher.usingIvy(jkIvy());
		}
		return cachedPublisher;
	}

	protected JkDependencies extraCommandLineDeps() {
		return JkDependencies.builder().build();
	}

	protected final JkClasspath toPath(String pathAsString) {
		if (pathAsString == null) {
			return JkClasspath.of();
		}
		return JkClasspath.of(JkUtilsFile.toPath(pathAsString, ";", baseDir().root()));
	}



	/**
	 * Returns the base directory for this project. All file/directory path are
	 * resolved from this directory.
	 */
	public final JkDir baseDir() {
		return JkDir.of(baseDir);
	}

	void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	/**
	 * This method is invoked right after the base directory of this builds had been set, so
	 * you can initialize fields safely here.
	 */
	protected void init() {
	}

	/**
	 * Invokes the specified method in this build.
	 */
	private final void invoke(String methodName, JkBuild from) {
		final Method method;
		try {
			method = this.getClass().getMethod(methodName);
		} catch (final NoSuchMethodException e) {
			JkLog.warn("No zero-arg method '" + methodName
					+ "' found in class '" + this.getClass()  + "'. Skip.");
			JkLog.warnStream().flush();
			return;
		}
		final String context;
		if (from != null) {
			final String path  = JkUtilsFile.getRelativePath(from.baseDir().root(), this.baseDir)
					.replace(File.separator, "/");
			context = " from project " + path + ", class " + this.getClass().getName();
		} else {
			context = "";
		}
		JkLog.startUnderlined("Method : " + methodName + context);
		try {
			JkUtilsReflect.invoke(this, method);
			JkLog.done("Method " + methodName + " success.");
		} catch (final RuntimeException e) {
			JkLog.done("Method " + methodName + " failed.");
			throw e;
		}
	}

	/**
	 * Executes specified method for this build.
	 * 
	 * @param from If the method is invoked from an external build (multi-projects)
	 * then you can specify the build from where it is launched in order to have proper logging.
	 * Can be <code>null</code>.
	 */
	final void execute(Iterable<BuildMethod> methods, JkBuild from) {
		for (final BuildMethod method : methods) {
			this.invoke(method, from);
		}
	}

	/**
	 * Returns a file located at the specified path relative to the base directory.
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
	public JkDir ouputDir() {
		return baseDir().sub(JkBuildResolver.BUILD_OUTPUT_PATH).createIfNotExist();
	}

	/**
	 * Returns the file located at the specified path relative to the output directory.
	 */
	public File ouputDir(String relativePath) {
		return ouputDir().file(relativePath);
	}

	// ------------ Operations ------------

	@JkDoc("Create the project structure")
	public void scaffold() {
		final File spec = this.baseDir(JkBuildResolver.BUILD_SOURCE_DIR);
		spec.mkdirs();
		final String packageName = this.groupName().replace('.', '/');
		new File(spec, packageName).mkdirs();
		JkBuildPlugin.applyScafforld(this.plugins.getActives());
	}

	@JkDoc("Clean the output directory.")
	public void clean() {
		JkLog.start("Cleaning output directory " + ouputDir().root().getPath() );
		ouputDir().exclude(JkBuildResolver.BUILD_BIN_DIR_NAME + "/**").deleteAll();
		JkLog.done();
	}

	@JkDoc("Conventional method standing for the default operations to perform.")
	public void doDefault() {
		clean();
	}

	@JkDoc("Run checks to verify the package is valid and meets quality criteria.")
	public void verify() {
		JkBuildPlugin.applyVerify(this.plugins.getActives());
	}

	@JkDoc("Display all available methods defined in this build.")
	public void help() {
		HelpDisplayer.help(this);
	}

	@JkDoc("Display details on all available plugins.")
	public void helpPlugins() {
		HelpDisplayer.helpPlugins(this);
	}

	public JkBuild relativeProject(String relativePath) {
		return relativeProject(this, null, relativePath);
	}

	private void invoke(BuildMethod buildMethod, JkBuild from) {
		if (buildMethod.isMethodPlugin()) {
			this.plugins.invoke(buildMethod.pluginClass, buildMethod.methodName);
		} else {
			this.invoke(buildMethod.methodName, from);
		}
	}

	private static final JkBuild relativeProject(JkBuild mainBuild, Class<? extends JkBuild> clazz, String relativePath) {
		final File projectDir = mainBuild.baseDir(relativePath);
		final Project project = new Project(projectDir);
		final JkBuild build = project.getBuild(clazz);
		JkOptions.populateFields(build);
		build.init();
		return build;
	}

	@SuppressWarnings("unchecked")
	private static List<JkBuild> populateProjectBuildField(JkBuild mainBuild) {
		final List<JkBuild> result = new LinkedList<JkBuild>();
		final List<Field> fields = JkUtilsReflect.getAllDeclaredField(mainBuild.getClass(), JkProject.class);
		for (final Field field : fields) {
			final JkProject jkProject = field.getAnnotation(JkProject.class);
			final JkBuild subBuild = relativeProject(mainBuild, (Class<? extends JkBuild>) field.getType(),
					jkProject.value());
			JkUtilsReflect.setFieldValue(mainBuild, field, subBuild);
			result.add(subBuild);
		}
		return result;
	}

	public <T extends JkBuildPlugin> T pluginOf(Class<T> pluginClass) {
		return this.plugins.findInstanceOf(pluginClass);
	}

}
