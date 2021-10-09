package build

import dev.jeka.core.tool.JkInit

fun main(args: Array<String>){
    JkInit.instanceOf(BuildKT::class.java).intellij.iml();
}
