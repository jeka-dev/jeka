package build

import build.common.JkNodeJs
import build.common.JkPluginKotlin
import dev.jeka.core.api.depmanagement.JkDependencySet
import dev.jeka.core.api.java.JkJavaRunner
import dev.jeka.core.api.java.JkJavaVersion
import dev.jeka.core.api.kotlin.JkKotlinModules
import dev.jeka.core.api.kotlin.JkKotlinModules.COMPILER_PLUGIN_KOTLINX_SERIALIZATION
import dev.jeka.core.api.utils.JkUtilsIO
import dev.jeka.core.api.utils.JkUtilsPath
import dev.jeka.core.api.utils.JkUtilsString
import dev.jeka.core.tool.JkClass
import dev.jeka.core.tool.JkDoc
import dev.jeka.core.tool.JkInit
import dev.jeka.core.tool.builtins.java.JkPluginJava
import java.awt.Desktop
import kotlin.io.path.Path

class Build : JkClass() {

    val kotlin = getPlugin(JkPluginKotlin::class.java)

    val serializationVersion = "1.2.1"
    val ktorVersion = "1.6.1"
    val logbackVersion = "1.2.3"
    val kmongoVersion = "4.2.7"
    val reactWrappersVersion = "17.0.2-pre.214-kotlin-1.5.20"

    @JkDoc("Version of nodeJs to use")
    var nodeJsVersion = "14.18.1"

    var nodejsArgs = ""

    override fun setup() {
        val jvmProject = kotlin.jvm().project
        jvmProject.simpleFacade()
                .setJvmTargetVersion(JkJavaVersion.V8)
                .setCompileDependencies { deps -> deps
                    .and("io.ktor:ktor-serialization:$ktorVersion")
                    .and("io.ktor:ktor-server-core:$ktorVersion")
                    .and("io.ktor:ktor-server-netty:$ktorVersion")
                    .and("ch.qos.logback:logback-classic:$logbackVersion")
                    .and("org.litote.kmongo:kmongo-coroutine-serialization:$kmongoVersion")
                }
                .setTestDependencies {deps -> deps
                    .and(JkKotlinModules.TEST_JUNIT5)
                }
        jvmProject.construction.manifest.addMainClass("ServerKt")
        jvmProject.publication.includeJavadocAndSources(false);
        kotlin.jvm()
            .useFatJarForMainArtifact()
            .kotlinCompiler
                .addPlugin("$COMPILER_PLUGIN_KOTLINX_SERIALIZATION:${kotlin.kotlinVersion}")
        kotlin.common()
            .setTestDir(null)
            .setCompileDependencies(JkDependencySet.of()
                .and("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                .and("io.ktor:ktor-client-core:$ktorVersion")
            )
    }

    fun cleanPack() {
        clean(); kotlin.jvm().project.publication.pack();
    }

    fun run() {;
        val jar = kotlin.jvm().project.publication.artifactProducer.mainArtifactPath
        JkJavaRunner.runInSeparateClassloader(jar);
        //JkJavaProcess.ofJavaJar(jar, null).exec()
    }

    fun open() {
        Desktop.getDesktop().browse(JkUtilsIO.toUrl("http:localhost:9090").toURI());
    }

    fun npm() {
        val sitePath = baseDir.resolve("jeka/.work/localsite");
        JkUtilsPath.createDirectories(sitePath);
        JkNodeJs.of(nodeJsVersion)
            .setWorkingDir(sitePath)
            .exec("npm", *JkUtilsString.translateCommandline(this.nodejsArgs))

    }

    fun npx() {
        val sitePath = baseDir.resolve("jeka/.work/localsite");
        JkUtilsPath.createDirectories(sitePath);
        JkNodeJs.of(nodeJsVersion)
            .setWorkingDir(sitePath)
            .exec("npx", *JkUtilsString.translateCommandline(this.nodejsArgs))
    }

    object CleanPack {
        @JvmStatic fun main(args: Array<String>) {
            JkInit.instanceOf(Build::class.java, *args).cleanPack();
        }
    }

    object PrintDeps  {
        @JvmStatic fun main(args: Array<String>){
            JkInit.instanceOf(Build::class.java, *args).getPlugin(JkPluginJava::class.java).showDependencies()
        }
    }

    object RunJar {
        @JvmStatic fun main(args: Array<String>) {
            JkInit.instanceOf(Build::class.java, *args).run()
        }
    }

    object Open  {
        @JvmStatic fun main(args: Array<String>){
            JkInit.instanceOf(Build::class.java, *args).open()
        }
    }

    object CleanCompile {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args);
            build.kotlin.jvm().project.construction.compilation.run();
        }
    }

    object Npm {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args);
            build.npm()
        }
    }


}