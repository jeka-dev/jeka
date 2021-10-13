package build

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId
import dev.jeka.core.api.java.JkJavaProcess
import dev.jeka.core.api.java.JkJavaVersion
import dev.jeka.core.api.kotlin.JkKotlinModules
import dev.jeka.core.tool.JkClass
import dev.jeka.core.tool.JkInit

class Build : JkClass() {

    val kotlin = getPlugin(JkPluginKotlin::class.java)

    override fun setup() {
        kotlin.project.simpleFacade()
                .setJavaVersion(JkJavaVersion.V8)
                .setCompileDependencies { deps -> deps
                        .and("com.google.guava:guava:30.0-jre")
                }
                .setTestDependencies {deps -> deps
                        .and(JkKotlinModules.TEST_JUNIT5)
                }
        kotlin.project.construction.manifest.addMainClass("dev.jeka.example.MainKt")
        kotlin.java.pack.javadoc = false;
        kotlin.java.pack.sources = false;
        kotlin.generateFatJar();
    }

    fun cleanPack() {
        clean(); kotlin.java.pack();
    }

    object CleanPack {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args)
            build.cleanPack()
        }
    }

    object PrintDeps  {
        @JvmStatic fun main(args: Array<String>){
            JkInit.instanceOf(Build::class.java, *args).kotlin.java.showDependencies()
        }
    }

    object RunJar {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args)
            val allDepsArtifactId = JkArtifactId.of("all-deps", "jar");
            val jar = build.kotlin.project.publication.artifactProducer.getArtifactPath(allDepsArtifactId)
            JkJavaProcess.ofJavaJar(jar, null).exec("1", "2" , "3", "4")
        }
    }

    object CleanCompile {
        @JvmStatic fun main(args: Array<String>) {
            val build = JkInit.instanceOf(Build::class.java, *args);
            build.kotlin.java.compile()
        }
    }

}