package build

import dev.jeka.core.api.depmanagement.JkDependencySet
import dev.jeka.core.api.java.JkJavaProcess
import dev.jeka.core.api.java.JkJavaVersion
import dev.jeka.core.api.kotlin.JkKotlinModules
import dev.jeka.core.api.kotlin.JkKotlinModules.COMPILER_PLUGIN_KOTLINX_SERIALIZATION
import dev.jeka.core.tool.JkClass
import dev.jeka.core.tool.JkInit
import dev.jeka.core.tool.builtins.java.JkPluginJava

class Build : JkClass() {

    val kotlin = getPlugin(JkPluginKotlin::class.java)

    val serializationVersion = "1.2.1"
    val ktorVersion = "1.6.1"
    val logbackVersion = "1.2.3"
    val kmongoVersion = "4.2.7"
    val reactWrappersVersion = "17.0.2-pre.214-kotlin-1.5.20"

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
        JkJavaProcess.ofJavaJar(jar, null).exec()
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

    object CleanCompile {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args);
            build.kotlin.jvm().project.construction.compilation.run();
        }
    }

}