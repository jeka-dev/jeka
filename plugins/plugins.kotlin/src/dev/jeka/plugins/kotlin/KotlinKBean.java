/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.plugins.kotlin;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@JkDoc("Add Kotlin compilation capability to `project` KBean.")
@JkDocUrl("https://github.com/jeka-dev/jeka/tree/master/plugins/dev.jeka.plugins.kotlin")
public class KotlinKBean extends KBean {

    public enum Preset {
        SPRING
    }

    private static final String DEFAULT_VERSION = "2.1.21";

    @JkDoc("Overrides the Kotlin version for compiling and running defined in 'jeka.kotlin.version' property.")
    public String kotlinVersion;

    @JkDoc("Location of Kotlin sources")
    public String sourceDir = "src/main/kotlin";

    @JkDoc("Location of Kotlin sources for tests")
    public String testSourceDir = "src/test/kotlin";

    @JkDoc("If true, includes standard lib for compiling")
    public boolean includeStdlib = true;

    @JkDoc("If true, the project KBean will be automatically configured to use Kotlin.")
    public boolean configureProject = true;

    @JkDoc("Suggest preconfigured settings.")
    public JkMultiValue<Preset> preset = JkMultiValue.of(Preset.class);

    @JkDoc("Kotlin compiler plugin setups")
    @JkSuggest({"0", "1", "2"})
    public final JkMultiValue<PluginOption> pluginOption = JkMultiValue.of(PluginOption.class);

    @JkDoc("Compiler plugins. Can be a Maven coordinate or a JAR path. Version is optional if it's a coordinate.")
    @JkDepSuggest(hint = "org.jetbrains.kotlin:")
    @JkSuggest({"0", "1", "2"})
    public final JkMultiValue<String> plugin = JkMultiValue.of(String.class);

    private JkKotlinJvm kotlinJvmProject;

    @JkDoc("Displays info about Kotlin configuration")
    public void info() {
        System.out.println("Kotlin version        : " + getKotlinVersion());
        System.out.println("Kotlin source dir     : " + Optional.ofNullable(getKotlinJvm().kotlinSourceDir())
                .orElse(Paths.get(sourceDir)));
        System.out.println("Kotlin test source dir: " + Optional.ofNullable(getKotlinJvm().getKotlinTestSourceDir())
                .orElse(Paths.get(testSourceDir)));
    }

    @JkDoc("Adds Kotlin source compilation and Kotlin standard library to dependencies")
    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        if (configureProject) {
            getKotlinJvm().configureProject(projectKBean.project, getBaseDir().resolve(sourceDir),
                    getBaseDir().resolve(testSourceDir));
        }
        handlePresets();
        handlePlugins();
        handlePluginOptions();

    }

    /**
     * Retrieves the Kotlin JVM project. If the project has already been created, it is returned.
     * Otherwise, it creates a new Kotlin JVM project using the projectKBean found in runbase..
     * If no projectKean is found in the runbase, an IllegalStateException is thrown.
     *
     * @throws IllegalStateException if no projectKean is found in the runbase
     */
    public JkKotlinJvm getKotlinJvm() {
        if (kotlinJvmProject != null) {
            return kotlinJvmProject;
        }
        JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(getRunbase().getDependencyResolver().getRepos(),
                getKotlinVersion());
        kotlinJvmProject = JkKotlinJvm.of(kotlinCompiler).setAddStdlib(this.includeStdlib);
        return kotlinJvmProject;
    }

    private String getKotlinVersion() {
        String result = kotlinVersion;
        if (result == null) {
            result = Optional.ofNullable(getRunbase().getProperties()
                    .getTrimmedNonBlank(JkKotlinCompiler.KOTLIN_VERSION_OPTION)).orElse(DEFAULT_VERSION);
        }
        return result;
    }

    private void handlePresets() {
        if (preset.getValues().contains(Preset.SPRING)) {
            getKotlinJvm().getKotlinCompiler()
                    .addPlugin(JkKotlinCompiler.ALLOPEN_PLUGIN_COORDINATES)
                    .addPluginOption(JkKotlinCompiler.ALLOPEN_PLUGIN_ID, "preset", "spring");
        }
    }

    private void handlePlugins() {
        for (String plugin : plugin.getValues()) {
            if (JkCoordinate.isCoordinateDescription(plugin)) {
                getKotlinJvm().getKotlinCompiler().addPlugin(plugin.trim());
            } else {
                Path path = Paths.get(plugin.trim());
                if (path.isAbsolute()) {
                    getKotlinJvm().getKotlinCompiler().addPlugin(path);
                } else  {
                    getKotlinJvm().getKotlinCompiler().addPlugin(getBaseDir().resolve(path));
                }
            }
        }
    }

    private void handlePluginOptions() {
        for (PluginOption pluginOption : pluginOption.getValues()) {
            getKotlinJvm().getKotlinCompiler().addPluginOption(pluginOption.pluginId,
                    pluginOption.key, pluginOption.value);
        }
    }

    public static class PluginOption {

        @JkSuggest({
                JkKotlinCompiler.ALLOPEN_PLUGIN_ID,
                JkKotlinCompiler.NOARG_PLUGIN_ID,
                JkKotlinCompiler.SERIALIZATION_PLUGIN_ID})
        @JkDoc("The compiler plugin id we want to setup")
        public String pluginId;

        @JkDoc("Property key to setup")
        public String key;

        @JkDoc("Property value to setup")
        public String value;
    }

}