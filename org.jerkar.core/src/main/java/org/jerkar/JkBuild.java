package org.jerkar;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jerkar.annotation.JkDoc;
import org.jerkar.annotation.JkOption;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkDependencyResolver;
import org.jerkar.file.JkFileTree;
import org.jerkar.file.JkPath;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsTime;

/**
 * Base class defining commons tasks and utilities
 * necessary for building any kind of project, regardless involved technologies.
 * 
 * @author Jerome Angibaud
 */
public class JkBuild {


	private File baseDir = JkUtilsFile.workingDir();

	private final Date buildTime = JkUtilsTime.now();

	protected final JkBuildPlugins plugins = new JkBuildPlugins(this);

	@JkOption({
		"Mention if you want to add extra lib in your build path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the build path cto compile and run Jerkar scripts.",
	"Example : -extraCompilePath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	protected String extraJerkarPath = null;

	private JkDependencyResolver scriptDependencyResolver;

	protected JkBuild() {
		JkOptions.populateFields(this);  // The option are also populated here so it's effective even when called from a main method

	}

	void setScriptDependencyResolver(JkDependencyResolver scriptDependencyResolver) {
		this.scriptDependencyResolver = scriptDependencyResolver;
	}

	/**
	 * Returns the dependency resolver used to compile/run scripts of this project.
	 */
	public JkDependencyResolver scriptDependencyResolver() {
		return this.scriptDependencyResolver;
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
	 * Returns the time-stamp this build has been initiated.
	 * Default is the time stamp (formatted as 'yyyyMMdd-HHmmss') this build has been instantiated.
	 */
	public String buildTimestamp() {
		return JkUtilsTime.timestampSec(buildTime);
	}

	protected Date buildTime() {
		return (Date) buildTime.clone();
	}

	protected JkScaffolder scaffolder() {
		return JkScaffolder.of(this).withExtraAction(new Runnable() {

			@Override
			public void run() {
				final File spec = baseDir(JkConstants.BUILD_DEF_DIR);
				spec.mkdirs();
			}
		})
		.withExtendedClass(JkBuild.class);
	}

	protected JkDependencies extraCommandLineDeps() {
		return JkDependencies.builder().build();
	}

	protected final JkPath toPath(String pathAsString) {
		if (pathAsString == null) {
			return JkPath.of();
		}
		return JkPath.of(baseDir().root(), pathAsString);
	}

	/**
	 * Returns the base directory for this project. All file/directory path are
	 * resolved from this directory.
	 */
	public final JkFileTree baseDir() {
		return JkFileTree.of(baseDir);
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
	private final void invoke(String methodName, File fromDir) {
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
		if (fromDir != null) {
			final String path  = JkUtilsFile.getRelativePath(fromDir, this.baseDir)
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
	 * Executes the specified methods given the fromDir as working directory.
	 */
	public void execute(Iterable<JkModelMethod> methods, File fromDir) {
		for (final JkModelMethod method : methods) {
			this.invoke(method, fromDir);
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
	public JkFileTree ouputDir() {
		return baseDir().from(JkConstants.BUILD_OUTPUT_PATH).createIfNotExist();
	}

	/**
	 * Returns the file located at the specified path relative to the output directory.
	 */
	public File ouputDir(String relativePath) {
		return ouputDir().file(relativePath);
	}

	// ------------ Jerkar methods ------------

	@JkDoc("Create the project structure")
	public final void scaffold() {
		JkScaffolder jkScaffolder = this.scaffolder();
		jkScaffolder = JkBuildPlugin.enhanceScafforld(this.plugins.getActives(), jkScaffolder);
		jkScaffolder.process();
	}

	@JkDoc("Clean the output directory.")
	public void clean() {
		JkLog.start("Cleaning output directory " + ouputDir().root().getPath() );
		ouputDir().exclude(JkConstants.BUILD_DEF_BIN_DIR_NAME + "/**").deleteAll();
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

	/**
	 * Invokes the specified method in this build but from the w
	 * @param jkModelMethod
	 * @param from
	 */
	private void invoke(JkModelMethod jkModelMethod, File fromDir) {
		if (jkModelMethod.isMethodPlugin()) {
			this.plugins.invoke(jkModelMethod.pluginClass(), jkModelMethod.name());
		} else {
			this.invoke(jkModelMethod.name(), fromDir);
		}
	}

	public <T extends JkBuildPlugin> T pluginOf(Class<T> pluginClass) {
		return this.plugins.findInstanceOf(pluginClass);
	}

	/**
	 * Creates an instance of <code>JkBuild</code> for the given project and build class.
	 * The instance field annotated with <code>JkOption</code> are populated as usual.
	 */
	protected final <T extends JkBuild> T relativeProjectBuild(Class<T> clazz, String relativePath) {
		final File projectDir = this.baseDir(relativePath);
		final Project project = new Project(projectDir);
		final T result = project.getBuild(clazz);
		JkOptions.populateFields(result);
		return result;
	}


}
