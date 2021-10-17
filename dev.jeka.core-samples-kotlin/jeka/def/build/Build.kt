package build

import dev.jeka.core.api.depmanagement.JkPopularModules
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId
import dev.jeka.core.api.java.JkJavaProcess
import dev.jeka.core.api.java.JkJavaVersion
import dev.jeka.core.api.kotlin.JkKotlinModules
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
        kotlin.jvmProject().simpleFacade()
                .setJavaVersion(JkJavaVersion.V8)
                .setCompileDependencies { deps -> deps
                    .and("io.ktor:ktor-serialization:$ktorVersion")
                    .and("io.ktor:ktor-server-core:$ktorVersion")
                    .and("io.ktor:ktor-server-netty:$ktorVersion")
                    .and("ch.qos.logback:logback-classic:$logbackVersion")
                    .and("org.litote.kmongo:kmongo-coroutine-serialization:$kmongoVersion")
                }
                .setTestDependencies {deps -> deps
                    //.and(JkPopularModules.JUNIT_5.version("5.8.1"))
                    .and(JkKotlinModules.TEST_JUNIT5)
                }
        kotlin.jvmProject().construction.manifest.addMainClass("dev.jeka.example.MainKt")
        kotlin.jvmProject().publication.includeJavadocAndSources(false);
        kotlin.generateFatJar();
    }


    object CleanPack {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args)
            build.clean();
            build.kotlin.jvmProject().publication.pack();
        }
    }

    object PrintDeps  {
        @JvmStatic fun main(args: Array<String>){
            JkInit.instanceOf(Build::class.java, *args).getPlugin(JkPluginJava::class.java).showDependencies()
        }
    }

    object RunJar {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args)
            val allDepsArtifactId = JkArtifactId.of("all-deps", "jar");
            val jar = build.kotlin.jvmProject().publication.artifactProducer.getArtifactPath(allDepsArtifactId)
            JkJavaProcess.ofJavaJar(jar, null).exec("1", "2" , "3", "4")
        }
    }

    object CleanCompile {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args);
            build.kotlin.jvmProject().construction.compilation.run();
        }
    }

}