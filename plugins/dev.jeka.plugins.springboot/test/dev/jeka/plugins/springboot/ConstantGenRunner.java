package org.jerkar.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.List;

class ConstantGenRunner {

    public static void main(String[] args) {

        String version = "cloud";
        String prefix = "SPRING-CLOUD-";

        List<String> lines = JkUtilsIO
                .readAsLines(ConstantGenRunner.class.getResourceAsStream("modules-" + version + ".txt"));
        for (String line : lines) {
            String afterAnd = JkUtilsString.substringAfterFirst(line, ".and(\"");
            String groupAndName = JkUtilsString.substringBeforeFirst(afterAnd, "\",");
            JkModuleId moduleId = JkModuleId.of(groupAndName);
            String name = moduleId.getName().toUpperCase();
            name = JkUtilsString.substringAfterFirst(name, prefix);
            name = name.replace('-', '_');
            System.out.println("public static final JkModuleId " + name + " = module(\"" + moduleId.getName() + "\");");
            System.out.println();
        }
        System.out.println("--------------------------------------------------------");

        for (String line : lines) {
            String afterAnd = JkUtilsString.substringAfterFirst(line, ".and(\"");
            String groupAndName = JkUtilsString.substringBeforeFirst(afterAnd, "\",");
            JkModuleId moduleId = JkModuleId.of(groupAndName);
            String name = moduleId.getName().toUpperCase();
            name = JkUtilsString.substringAfterFirst(name, prefix);
            name = name.replace('-', '_');
            System.out.println("                .and(" + name + ", " + version + ")");
        }

    }

}
