import dev.jeka.core.api.java.JkJavaVersion
import dev.jeka.core.api.kotlin.JkKotlinCompiler
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec
import dev.jeka.core.tool.JkClass
import dev.jeka.core.tool.builtins.intellij.JkPluginIntellij
import dev.jeka.core.tool.builtins.java.JkPluginJava
import dev.jeka.core.tool.builtins.scaffold.JkPluginScaffold

class BuildKT : JkClass() {

    val java = getPlugin(JkPluginJava::class.java)

    val scaffold = getPlugin(JkPluginScaffold::class.java)

    val intellij = getPlugin(JkPluginIntellij::class.java)

    override fun setup() {
        java.project.simpleFacade()
                .setJavaVersion(JkJavaVersion.V8)
                .setCompileDependencies { deps -> deps
                        .and("com.google.guava:guava:21.0")
                }
                .setTestDependencies {deps -> deps
                        .and("org.junit.jupiter:junit-jupiter:5.8.1")
                }
        java.project.construction.compilation.preCompileActions.append(this::compileKotlin)
    }

    fun compileKotlin() {
        val layout = java.project.construction.compilation.layout
        val compileSpec = JkKotlinJvmCompileSpec.of()
                .setOutputDir(layout.outputDir.resolve("classes"))
                .setTargetVersion(java.project.construction.javaVersion)
                .addSources(java.project.baseDir.resolve("src/main/java"))
        JkKotlinCompiler.ofKotlinHome().compile(compileSpec)
    }

    fun cleanPack() {
        clean(); java.pack();
    }

}