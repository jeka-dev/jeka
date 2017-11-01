package org.jerkar.tool;

/**
 * Plugin for {@link JkBuild}.
 *
 * A Jerkar Plugin is a piece of code that can be hooked to {@link JkBuild} instance at runtime.<br/>
 *
 * A plugin defines two kinds of hook :
 * <ul>
 *     <li>code that modify the {@link JkBuild} instance on which this plugin is applied (code located in {@link #apply(JkBuild)} method)</li>
 *     <li>code that is exposed as a JkBuild method (code located in any public method taking a #JkBuild as its single argument)</code></li>
 * </ul>
 *
 * To hook a plugin at runtime, you have to invoke Jerkar with argument <i>pluginName#</i> followed by the method name. For
 * exemple command line <code>jerkar sonarQube#run</code> will invoke the <code>run(JkBuild)</code> method of plugin class JkBuildPluginSonarQube.<br/>
 * For <code>apply</code> method, one can use <code>jerkar myPlugin#</code> short-hand instead of <code>jerkar myPlugin#apply</code>.
 * <p>
 * All plugin class must be named as <code>JkPluginXxxxxx</code> where xxxxxx stands for the plugin name. This
 * is necessary in order to find plugin class from its name accept the classpath.
 * <p>
 * A plugin can be configured at runtime by injecting data in its instance fields by passing argument <i>pluginName#</i>
 * followed by the field name. For example <code>jerkar eclise#generatefiles -eclipse#useVarPath=true</code>.
 *
 * <h5>Configuration</h5>
 * One may need to configure a plugin in the build class itself accept order to no mention the configuration of the
 * command line each time it is invoked. For such build class writer has to instantiate and configure programmatically
 * in the build class then register it by invoking JkBuild#register(JkPlugin) method.
 *
 * <h5>Documentation</h5>
 * It's highly recommended to annotate plugin class, fields and methods with @{@link JkDoc} annotation in order to
 * provide self documentation.
 *
 * <h5>When to write plugins or not ?</h5>
 * It makes sense to write plugins for code that will be invoked only time to time. For example for generating IDE
 * metadata files, launch a sugarQube analysis or perform test coverage measures.<br/>
 * If a feature is supposed to be permanently used to produce artifacts (as source code generation) it is senseless
 * as it has to be present programmatically in build class.
 *
 */
public interface JkPlugin {

    /**
     * Modify the specified instance in accordance with what this plugin is supposed to do.
     */
    void apply(JkBuild build);

}
